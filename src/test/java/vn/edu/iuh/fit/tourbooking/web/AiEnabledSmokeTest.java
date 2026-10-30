package vn.edu.iuh.fit.tourbooking.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.iuh.fit.tourbooking.ai.TourVectorService;
import vn.edu.iuh.fit.tourbooking.entity.EventType;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.repository.UserEventRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Chạy cả ứng dụng thật trên H2 (chế độ SQL Server) + máy chủ OpenAI giả: AI BẬT, kiểm trang, API, theo dõi, thống kê, chatbot.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "h2"})
class AiEnabledSmokeTest {

    static final FakeGeminiServer FAKE = new FakeGeminiServer();
    static Path storeDir;

    // Dùng system property để biến có mặt từ sớm (EnvironmentPostProcessor đọc môi trường trước khi nạp ngữ cảnh).
    static {
        try {
            FAKE.start();
            storeDir = java.nio.file.Files.createTempDirectory("vec");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        System.setProperty("GEMINI_API_KEY", "AIza-fake-key");
        System.setProperty("AI_BASE_URL", FAKE.baseUrl());
        System.setProperty("AI_VECTOR_STORE_PATH", storeDir.resolve("vector-store.json").toString());
        System.setProperty("AI_EMBEDDING_MIN_INTERVAL_MS", "0");
        System.setProperty("AI_SIMILARITY_THRESHOLD", "0.05");
    }

    @AfterAll
    static void stop() {
        FAKE.stop();
        for (String key : new String[]{"GEMINI_API_KEY", "AI_BASE_URL", "AI_VECTOR_STORE_PATH", "AI_EMBEDDING_MIN_INTERVAL_MS", "AI_SIMILARITY_THRESHOLD"}) {
            System.clearProperty(key);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired TourVectorService vectorService;
    @Autowired UserEventRepository events;
    @Autowired UserRepository users;

    private static final String BROWSER = "Mozilla/5.0 (Windows NT 10.0) Chrome/126.0";

    private void waitForIndex() throws InterruptedException {
        for (int i = 0; i < 100 && vectorService.status().indexedCount() < 20; i++) {
            Thread.sleep(100);
        }
        assertThat(vectorService.status().indexedCount()).isEqualTo(20);
    }

    @Test
    void whole_ai_and_tracking_flow_works_end_to_end() throws Exception {
        waitForIndex();
        int issuedBefore = FAKE.signaturesIssued.get();
        int acceptedBefore = FAKE.signedFollowUpsAccepted.get();
        int rejectedBefore = FAKE.signatureRejections.get();
        assertThat(FAKE.lastAuthorization).isEqualTo("Bearer AIza-fake-key");
        assertThat(storeDir.resolve("vector-store.json")).exists();

        // Trang chủ: khối gợi ý (khởi đầu lạnh -> phổ biến) và khung chat bật.
        String home = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(home).contains("</html>").contains("id=\"goi-y\"").contains("id=\"aiChatForm\"");

        // Tìm bằng AI: xếp theo ngữ nghĩa, giá lọc từ câu.
        String search = mvc.perform(get("/tours").param("q", "biển đảo lặn san hô dưới 5 triệu").param("ai", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(search).contains("</html>").contains("xếp theo độ phù hợp");
        assertThat(search).contains("Nha Trang").doesNotContain("Phú Quốc - Đảo Ngọc");

        // REST ngữ nghĩa.
        String json = mvc.perform(get("/api/tours/semantic").param("q", "núi rừng săn mây"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json).contains("\"mode\":\"SEMANTIC\"").contains("Sa Pa");

        // Chi tiết tour: tour tương tự bằng vector + hook theo dõi.
        String detail = mvc.perform(get("/tours/6")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("</html>").contains("data-track-view=\"6\"").contains("id=\"similar-tours\"")
                .contains("do AI so sánh ý nghĩa");

        // Theo dõi: VIEW + CLICK + SEARCH + TIME_ON_PAGE, userId do server gắn.
        MockHttpSession session = new MockHttpSession();
        long before = events.count();
        for (String body : new String[]{
                "{\"type\":\"VIEW\",\"tourId\":6,\"pageUrl\":\"/tours/6/sa-pa\"}",
                "{\"type\":\"CLICK\",\"tourId\":8,\"pageUrl\":\"/tours\"}",
                "{\"type\":\"SEARCH\",\"keyword\":\"Biển yên tĩnh\",\"pageUrl\":\"/tours\"}",
                "{\"type\":\"TIME_ON_PAGE\",\"tourId\":6,\"durationMs\":42000}"}) {
            mvc.perform(post("/api/tracking").session(session).with(csrf()).header("User-Agent", BROWSER)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isAccepted());
        }
        assertThat(events.count()).isEqualTo(before + 4);

        // Sai: thiếu trường, loại lạ -> 400; bot -> 202 nhưng không ghi; thiếu CSRF -> 403.
        mvc.perform(post("/api/tracking").session(session).with(csrf()).header("User-Agent", BROWSER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"VIEW\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/tracking").session(session).with(csrf()).header("User-Agent", BROWSER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"HACK\",\"tourId\":1}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/tracking").session(session).with(csrf()).header("User-Agent", "Googlebot/2.1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"VIEW\",\"tourId\":1}")).andExpect(status().isAccepted());
        assertThat(events.count()).isEqualTo(before + 4);
        mvc.perform(post("/api/tracking").session(session).header("User-Agent", BROWSER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"VIEW\",\"tourId\":1}")).andExpect(status().is4xxClientError());

        // userId KHÔNG lấy từ client: gửi userId giả, đăng nhập là người khác.
        User an = users.findByEmail("an.nguyen@gmail.com").orElseThrow();
        mvc.perform(post("/api/tracking").with(csrf()).with(user(new CustomUserDetails(an))).header("User-Agent", BROWSER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"VIEW\",\"tourId\":2,\"userId\":999}"))
                .andExpect(status().isAccepted());
        assertThat(events.findAll()).filteredOn(e -> e.getType() == EventType.VIEW && e.getTourId() == 2L)
                .extracting("userId").containsExactly(an.getId());

        // Thêm giỏ qua REST -> ADD_TO_CART ghi ở server.
        long carts = events.findAll().stream().filter(e -> e.getType() == EventType.ADD_TO_CART).count();
        mvc.perform(post("/api/cart/items").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tourId\":6,\"numAdults\":1,\"numChildren\":0}"))
                .andExpect(status().isOk());
        assertThat(events.findAll().stream().filter(e -> e.getType() == EventType.ADD_TO_CART).count()).isEqualTo(carts + 1);

        // Gợi ý cá nhân theo phiên vừa xem Sa Pa.
        String homeAfter = mvc.perform(get("/").session(session)).andReturn().getResponse().getContentAsString();
        assertThat(homeAfter).contains("dựa trên những tour bạn vừa xem");

        // Chatbot: vòng tool-calling thật (searchTours) rồi addToCart vào giỏ của phiên.
        MockHttpSession chat = new MockHttpSession();
        String reply = mvc.perform(post("/api/chat").session(chat).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Có tour biển nào dưới 6 triệu?\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(reply).contains("Kết quả công cụ").contains("Nha Trang");
        mvc.perform(post("/api/chat").session(chat).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"ok, thêm vào giỏ 2 người lớn\"}"))
                .andExpect(status().isOk());
        String cart = mvc.perform(get("/api/cart").session(chat)).andReturn().getResponse().getContentAsString();
        assertThat(cart).contains("\"numAdults\":2");
        assertThat(FAKE.lastChatBody).contains("searchTours").contains("addToCart").contains("getDepartures");
        // Chat đi qua API gốc của Gemini (Google GenAI SDK), khoá nằm ở header x-goog-api-key; máy chủ giả ÉP luật chữ ký:
        // mỗi vòng gọi công cụ (2 lần: searchTours, addToCart) phát một chữ ký và lần gửi tiếp theo phải trả lại đúng chữ ký đó.
        assertThat(FAKE.lastChatPath).startsWith("/v1beta/models/").endsWith(":generateContent");
        assertThat(FAKE.lastApiKeyHeader).isEqualTo("AIza-fake-key");
        assertThat(FAKE.signaturesIssued.get() - issuedBefore).isEqualTo(2);
        assertThat(FAKE.signedFollowUpsAccepted.get() - acceptedBefore).isEqualTo(2);
        assertThat(FAKE.signatureRejections.get() - rejectedBefore).isZero();

        // Quản trị: thống kê hành vi + trang AI + lập chỉ mục lại (PRG).
        User admin = users.findByEmail("admin@tourbooking.vn").orElseThrow();
        String analytics = mvc.perform(get("/admin/analytics").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(analytics).contains("</html>").contains("Phễu chuyển đổi").contains("Sa Pa").contains("biển yên tĩnh");
        mvc.perform(get("/admin/analytics").param("from", "2020-01-01").param("to", "2020-01-31")
                .with(user(new CustomUserDetails(admin)))).andExpect(status().isOk());
        mvc.perform(get("/admin/analytics").with(user(new CustomUserDetails(an)))).andExpect(status().isForbidden());

        String aiPage = mvc.perform(get("/admin/ai").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(aiPage).contains("</html>").contains("Đang bật");
        MvcResult reindex = mvc.perform(post("/admin/ai/reindex").with(csrf()).with(user(new CustomUserDetails(admin))))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(reindex.getResponse().getRedirectedUrl()).isEqualTo("/admin/ai");
        mvc.perform(post("/admin/ai/reindex").with(csrf()).with(user(new CustomUserDetails(an))))
                .andExpect(status().isForbidden());
    }

    @Test
    void provider_failure_gives_a_friendly_503_and_the_site_keeps_working() throws Exception {
        waitForIndex();
        FAKE.chatFailureStatus = 500;
        try {
            MvcResult result = mvc.perform(post("/api/chat").session(new MockHttpSession()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"xin chào\"}"))
                    .andExpect(status().isServiceUnavailable()).andReturn();
            assertThat(result.getResponse().getContentAsString()).contains("tạm thời chưa phản hồi")
                    .doesNotContain("AIza").doesNotContain("Exception");
        } finally {
            FAKE.chatFailureStatus = 0;
        }
        mvc.perform(get("/tours")).andExpect(status().isOk());
    }

    private MvcResult chat(String message) throws Exception {
        return mvc.perform(post("/api/chat").session(new MockHttpSession()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"" + message + "\"}")).andReturn();
    }

    @Test
    void a_tool_round_rejected_with_4xx_falls_back_to_a_rag_only_answer() throws Exception {
        waitForIndex();
        FAKE.rejectToolRequests = true;
        int signatureRejectionsBefore = FAKE.signatureRejections.get();
        int before = FAKE.chatRequests.get();
        try {
            MvcResult result = chat("Có tour biển nào dưới 6 triệu?");
            // Người dùng nhận câu trả lời hữu ích (200), không phải lỗi.
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            assertThat(result.getResponse().getContentAsString()).contains(FakeGeminiServer.NO_TOOLS_ANSWER);
            // Đúng hai yêu cầu: lần có công cụ bị 400, lần thử lại KHÔNG có công cụ.
            assertThat(FAKE.chatRequests.get() - before).isEqualTo(2);
            assertThat(FAKE.signatureRejections.get() - signatureRejectionsBefore).isEqualTo(1);
            String first = FAKE.chatBodies.get(FAKE.chatBodies.size() - 2);
            String retry = FAKE.chatBodies.get(FAKE.chatBodies.size() - 1);
            assertThat(first).contains("functionDeclarations");
            assertThat(retry).doesNotContain("functionDeclarations");
            // Bản dự phòng dùng system prompt riêng (mời khách dùng trang tìm kiếm) và lịch sử không bị ghi trùng tin nhắn của khách.
            assertThat(retry).contains("/tours");
            assertThat(retry.split("Có tour biển nào", -1).length - 1).isEqualTo(1);
        } finally {
            FAKE.rejectToolRequests = false;
        }
    }

    @Test
    void errors_that_dropping_tools_cannot_fix_are_not_retried() throws Exception {
        waitForIndex();
        try {
            // 401/403/429 và 5xx: bỏ công cụ không cứu được -> đúng 1 yêu cầu rồi 503 thân thiện.
            for (int status : new int[]{401, 403, 500}) {
                FAKE.chatFailureStatus = status;
                int before = FAKE.chatRequests.get();
                assertThat(chat("xin chào").getResponse().getStatus()).as("status %d", status).isEqualTo(503);
                assertThat(FAKE.chatRequests.get() - before).as("requests for %d", status).isEqualTo(1);
            }
            // Nhà cung cấp luôn từ chối 400 (cả lần thử lại): vẫn là 503 thân thiện, không lộ chi tiết, đúng 2 yêu cầu.
            FAKE.chatFailureStatus = 400;
            int before = FAKE.chatRequests.get();
            MvcResult result = chat("xin chào");
            assertThat(result.getResponse().getStatus()).isEqualTo(503);
            assertThat(result.getResponse().getContentAsString()).contains("tạm thời chưa phản hồi")
                    .doesNotContain("AIza").doesNotContain("thought_signature");
            assertThat(FAKE.chatRequests.get() - before).isEqualTo(2);
        } finally {
            FAKE.chatFailureStatus = 0;
        }
    }
}
