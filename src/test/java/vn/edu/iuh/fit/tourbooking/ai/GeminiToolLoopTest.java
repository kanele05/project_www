package vn.edu.iuh.fit.tourbooking.ai;

import com.google.genai.Client;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import vn.edu.iuh.fit.tourbooking.web.FakeGeminiServer;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Kiểm thử mức mô hình (không dựng Spring): vòng tool-calling của Gemini với luật thought_signature được ÉP bởi máy chủ giả.
//  - đường OpenAI-compat của Spring AI 1.1.8 làm rơi chữ ký -> bị 400 đúng như lỗi thật (bằng chứng nguyên nhân gốc);
//  - đường API gốc (Google GenAI SDK) trả lại chữ ký -> vòng gọi công cụ thành công;
//  - thử lại 429.
class GeminiToolLoopTest {

    static final FakeGeminiServer FAKE = new FakeGeminiServer();

    @BeforeAll
    static void start() throws IOException {
        FAKE.start();
    }

    @AfterAll
    static void stop() {
        FAKE.stop();
    }

    @BeforeEach
    void reset() {
        FAKE.enforceSignatures = true;
        FAKE.transientFailures.set(0);
        FAKE.chatFailureStatus = 0;
        FAKE.rejectToolRequests = false;
    }

    static class SearchTool {
        int calls;

        @Tool(description = "Tìm tour theo từ khoá")
        String searchTours(@ToolParam(description = "từ khoá") String query,
                           @ToolParam(description = "giá tối đa", required = false) Long maxPriceVnd) {
            calls++;
            return "{\"tours\":[\"Nha Trang\"]}";
        }
    }

    private ChatClient nativeClient(SearchTool tool, String model, double retryDelaySeconds) {
        Client client = GenAiClientFactory.create("AIza-fake-key", FAKE.baseUrl(), 10_000, retryDelaySeconds);
        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .defaultOptions(GoogleGenAiChatOptions.builder().model(model).build())
                .build();
        return ChatClient.builder(chatModel).defaultTools(tool).build();
    }

    private ChatClient openAiCompatClient(SearchTool tool) {
        OpenAiApi api = OpenAiApi.builder().baseUrl(FAKE.baseUrl()).apiKey("AIza-fake-key")
                .completionsPath("/v1beta/openai/chat/completions").build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder().openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model("gemini-3.5-flash-lite").build()).build();
        return ChatClient.builder(chatModel).defaultTools(tool).build();
    }

    @Test
    void root_cause_openai_compat_path_drops_the_thought_signature_and_gemini_rejects_the_follow_up() {
        var tool = new SearchTool();
        int rejectedBefore = FAKE.signatureRejections.get();
        assertThatThrownBy(() -> openAiCompatClient(tool).prompt().user("Có tour biển nào?").call().content())
                .isInstanceOf(NonTransientAiException.class)
                .hasMessageStartingWith("400 - ")
                .hasMessageContaining("missing a thought_signature")
                .hasMessageContaining("default_api:searchTours");
        assertThat(tool.calls).isEqualTo(1);
        assertThat(FAKE.signatureRejections.get() - rejectedBefore).isEqualTo(1);
        // Yêu cầu thứ hai thật sự đã gửi lại tool_call nhưng không có extra_content (Spring AI bỏ trường lạ).
        assertThat(FAKE.lastChatBody).contains("tool_calls").doesNotContain("thought_signature");
    }

    @Test
    void control_the_same_openai_compat_scenario_passes_when_the_fake_does_not_enforce_signatures() {
        FAKE.enforceSignatures = false;
        var tool = new SearchTool();
        assertThat(openAiCompatClient(tool).prompt().user("Có tour biển nào?").call().content())
                .contains("Kết quả công cụ");
    }

    @Test
    void native_sdk_path_sends_the_signature_back_so_the_tool_loop_passes_for_gemini_2_5_and_3() {
        for (String model : new String[]{"gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-3.5-flash-lite"}) {
            var tool = new SearchTool();
            int issued = FAKE.signaturesIssued.get();
            int accepted = FAKE.signedFollowUpsAccepted.get();
            int rejected = FAKE.signatureRejections.get();

            String answer = nativeClient(tool, model, 0.05).prompt().user("Có tour biển nào?").call().content();

            assertThat(answer).as(model).contains("Kết quả công cụ").contains("Nha Trang");
            assertThat(tool.calls).as(model).isEqualTo(1);
            assertThat(FAKE.signaturesIssued.get() - issued).as(model).isEqualTo(1);
            assertThat(FAKE.signedFollowUpsAccepted.get() - accepted).as(model).isEqualTo(1);
            assertThat(FAKE.signatureRejections.get() - rejected).as(model).isZero();
            assertThat(FAKE.lastChatPath).contains("/models/" + model + ":generateContent");
            // Chữ ký thật sự nằm trong thân yêu cầu gửi lại.
            assertThat(FAKE.lastChatBody).contains("\"thoughtSignature\"").contains("functionResponse");
        }
    }

    @Test
    void quota_errors_429_are_retried_by_the_sdk_with_backoff() {
        FAKE.transientFailures.set(2);
        int before = FAKE.chatRequests.get();
        assertThat(nativeClient(new SearchTool(), "gemini-2.5-flash", 0.05).prompt().user("xin chào").call().content())
                .isNotBlank();
        assertThat(FAKE.chatRequests.get() - before).isEqualTo(3);
    }

    @Test
    void persistent_429_gives_up_after_three_attempts() {
        FAKE.transientFailures.set(100);
        int before = FAKE.chatRequests.get();
        assertThatThrownBy(() -> nativeClient(new SearchTool(), "gemini-2.5-flash", 0.05).prompt().user("xin chào").call().content())
                .hasStackTraceContaining("429");
        assertThat(FAKE.chatRequests.get() - before).isEqualTo(3);
    }
}
