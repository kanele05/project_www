package vn.edu.iuh.fit.tourbooking.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Máy chủ giả của Gemini để kiểm thử cả chuỗi Spring AI mà không cần mạng hay khoá thật. Nói ba giao thức:
//  - API gốc  POST /v1beta/models/{model}:generateContent   (đường chat thật của ứng dụng, qua Google GenAI SDK)
//  - OpenAI-compat  POST /v1beta/openai/chat/completions     (đường cũ gây ra lỗi gốc; giữ để tái hiện lỗi)
//  - OpenAI-compat  POST /v1beta/openai/embeddings           (nhúng vector, vẫn dùng thật)
// Hai đường chat đều ÉP LUẬT của Gemini 3: trong lượt hiện tại, mỗi tin nhắn của mô hình có function call phải mang lại
// đúng chữ ký (thought signature) mà máy chủ giả đã phát ra, nếu không trả HTTP 400 với đúng câu chữ thật:
// "Function call is missing a thought_signature in functionCall parts. ..."
public class FakeGeminiServer {

    public static final String NO_TOOLS_ANSWER = "Mình chưa tra cứu được số liệu lúc này, bạn thử trang tìm kiếm nhé.";
    private static final int DIMS = 256;
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicInteger signatureCounter = new AtomicInteger();
    private final Set<String> issuedSignatures = ConcurrentHashMap.newKeySet();

    public final AtomicInteger embeddingRequests = new AtomicInteger();
    public final AtomicInteger chatRequests = new AtomicInteger();
    public final AtomicInteger nativeChatRequests = new AtomicInteger();
    public final AtomicInteger signaturesIssued = new AtomicInteger();
    public final AtomicInteger signedFollowUpsAccepted = new AtomicInteger();
    public final AtomicInteger signatureRejections = new AtomicInteger();
    public final List<String> chatBodies = new CopyOnWriteArrayList<>();
    public volatile String lastAuthorization;
    public volatile String lastApiKeyHeader;
    public volatile String lastChatBody;
    public volatile String lastChatPath;
    // != 0: MỌI yêu cầu chat trả mã này (mô phỏng nhà cung cấp hỏng hoặc hết hạn mức).
    public volatile int chatFailureStatus = 0;
    // true: yêu cầu chat nào có khai báo công cụ cũng bị từ chối 400 kiểu "thiếu thought_signature" (mô phỏng Gemini 3 khi chữ ký bị mất); yêu cầu không công cụ thì được.
    public volatile boolean rejectToolRequests = false;
    // false: tắt việc ép chữ ký (chỉ để chứng minh chính phép thử có khả năng bắt lỗi).
    public volatile boolean enforceSignatures = true;
    // > 0: số yêu cầu chat đầu tiên trả 429 (kiểm thử thử lại).
    public final AtomicInteger transientFailures = new AtomicInteger();

    public FakeGeminiServer start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/openai/embeddings", ex -> {
            embeddingRequests.incrementAndGet();
            lastAuthorization = ex.getRequestHeaders().getFirst("Authorization");
            JsonNode req = mapper.readTree(ex.getRequestBody());
            ArrayNode data = mapper.createArrayNode();
            JsonNode input = req.get("input");
            int index = 0;
            if (input.isArray()) {
                for (JsonNode text : input) {
                    data.add(embedding(text.asText(), index++));
                }
            } else {
                data.add(embedding(input.asText(), 0));
            }
            ObjectNode body = mapper.createObjectNode();
            body.put("object", "list");
            body.set("data", data);
            body.put("model", "fake-embedding");
            body.set("usage", mapper.createObjectNode().put("prompt_tokens", 1).put("total_tokens", 1));
            respond(ex, 200, body.toString());
        });
        server.createContext("/v1beta/openai/chat/completions", ex -> {
            String raw = beginChat(ex, false);
            if (raw == null) {
                return;
            }
            JsonNode request = mapper.readTree(raw);
            String violation = openAiSignatureViolation(request);
            if (violation != null) {
                signatureRejections.incrementAndGet();
                // Đúng hình dạng thật của endpoint tương thích: mảng bọc ngoài.
                respond(ex, 400, "[{\"error\":{\"code\":400,\"message\":" + mapper.writeValueAsString(violation)
                        + ",\"status\":\"INVALID_ARGUMENT\"}}]");
                return;
            }
            respond(ex, 200, openAiAnswer(request).toString());
        });
        server.createContext("/v1beta/models/", ex -> {
            nativeChatRequests.incrementAndGet();
            String raw = beginChat(ex, true);
            if (raw == null) {
                return;
            }
            JsonNode request = mapper.readTree(raw);
            String violation = nativeSignatureViolation(request);
            if (violation != null) {
                signatureRejections.incrementAndGet();
                respond(ex, 400, "{\"error\":{\"code\":400,\"message\":" + mapper.writeValueAsString(violation)
                        + ",\"status\":\"INVALID_ARGUMENT\"}}");
                return;
            }
            respond(ex, 200, nativeAnswer(request).toString());
        });
        server.start();
        return this;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    // Chung cho hai đường chat: đếm, ghi lại, và áp các chế độ lỗi. Trả null nếu đã trả lời lỗi.
    private String beginChat(HttpExchange ex, boolean nativeApi) throws IOException {
        chatRequests.incrementAndGet();
        if (!nativeApi) {
            lastAuthorization = ex.getRequestHeaders().getFirst("Authorization");
        }
        lastApiKeyHeader = ex.getRequestHeaders().getFirst("x-goog-api-key");
        lastChatPath = ex.getRequestURI().getPath();
        String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        lastChatBody = raw;
        chatBodies.add(raw);
        if (transientFailures.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
            respond(ex, 429, "{\"error\":{\"code\":429,\"message\":\"Resource has been exhausted\",\"status\":\"RESOURCE_EXHAUSTED\"}}");
            return null;
        }
        if (chatFailureStatus != 0) {
            respond(ex, chatFailureStatus, "{\"error\":{\"code\":" + chatFailureStatus + ",\"message\":\"quota\"}}");
            return null;
        }
        if (rejectToolRequests && hasTools(raw)) {
            signatureRejections.incrementAndGet();
            String message = missingSignatureMessage("searchTours", 3);
            String body = "{\"error\":{\"code\":400,\"message\":" + mapper.writeValueAsString(message)
                    + ",\"status\":\"INVALID_ARGUMENT\"}}";
            respond(ex, 400, nativeApi ? body : "[" + body + "]");
            return null;
        }
        return raw;
    }

    private boolean hasTools(String raw) throws IOException {
        JsonNode tools = mapper.readTree(raw).path("tools");
        return tools.isArray() && !tools.isEmpty();
    }

    private static String missingSignatureMessage(String function, int position) {
        return "Function call is missing a thought_signature in functionCall parts. This is required for tools to work "
                + "correctly, and missing thought_signature may lead to degraded model performance. Additional data, "
                + "function call `default_api:" + function + "` , position " + position + ". Please refer to "
                + "https://ai.google.dev/gemini-api/docs/thought-signatures for more details.";
    }

    private String newSignature() {
        String signature = Base64.getEncoder().encodeToString(
                ("sig-" + signatureCounter.incrementAndGet()).getBytes(StandardCharsets.UTF_8));
        issuedSignatures.add(signature);
        signaturesIssued.incrementAndGet();
        return signature;
    }

    // ---------------------------------------------------------------- API gốc (generateContent)

    // Lượt hiện tại bắt đầu ở tin nhắn "user" cuối cùng có chữ (không phải chỉ functionResponse).
    private String nativeSignatureViolation(JsonNode request) {
        if (!enforceSignatures) {
            return null;
        }
        JsonNode contents = request.path("contents");
        int turnStart = 0;
        for (int i = 0; i < contents.size(); i++) {
            JsonNode c = contents.get(i);
            if ("user".equals(c.path("role").asText("user")) && hasTextPart(c)) {
                turnStart = i;
            }
        }
        boolean sawSignedCall = false;
        for (int i = turnStart + 1; i < contents.size(); i++) {
            JsonNode c = contents.get(i);
            if (!"model".equals(c.path("role").asText())) {
                continue;
            }
            boolean first = true;
            for (JsonNode part : c.path("parts")) {
                if (!part.has("functionCall")) {
                    continue;
                }
                if (first) {
                    String sig = part.path("thoughtSignature").asText("");
                    if (sig.isEmpty() || !issuedSignatures.contains(sig)) {
                        return missingSignatureMessage(part.path("functionCall").path("name").asText(), i);
                    }
                    sawSignedCall = true;
                }
                first = false;
            }
        }
        if (sawSignedCall) {
            signedFollowUpsAccepted.incrementAndGet();
        }
        return null;
    }

    private static boolean hasTextPart(JsonNode content) {
        for (JsonNode part : content.path("parts")) {
            if (part.hasNonNull("text")) {
                return true;
            }
        }
        return false;
    }

    // Kịch bản: hỏi "biển" -> gọi searchTours; có kết quả công cụ -> trả lời chứa kết quả đó; nhắn "thêm vào giỏ" -> gọi addToCart.
    private ObjectNode nativeAnswer(JsonNode request) {
        JsonNode contents = request.path("contents");
        JsonNode last = contents.get(contents.size() - 1);
        JsonNode lastPart = last.path("parts").get(last.path("parts").size() - 1);

        boolean toolsDeclared = request.path("tools").isArray() && !request.path("tools").isEmpty();
        ArrayNode parts = mapper.createArrayNode();
        if (!toolsDeclared) {
            // Không khai báo công cụ thì mô hình thật không thể gọi hàm: chỉ trả lời bằng chữ.
            parts.add(mapper.createObjectNode().put("text", NO_TOOLS_ANSWER));
        } else if (lastPart.has("functionResponse")) {
            parts.add(mapper.createObjectNode().put("text",
                    "Kết quả công cụ: " + lastPart.path("functionResponse").path("response").toString()));
        } else {
            String user = plain(lastPart.path("text").asText());
            if (user.contains("them vao gio")) {
                parts.add(nativeCall("addToCart", "{\"departureId\":1,\"adults\":2,\"children\":0}"));
            } else if (user.contains("bien")) {
                parts.add(nativeCall("searchTours", "{\"query\":\"biển đảo\",\"maxPriceVnd\":6000000}"));
            } else {
                parts.add(mapper.createObjectNode().put("text", "Xin chào, mình là trợ lý tour."));
            }
        }
        ObjectNode content = mapper.createObjectNode().put("role", "model");
        content.set("parts", parts);
        ObjectNode candidate = mapper.createObjectNode();
        candidate.set("content", content);
        candidate.put("finishReason", "STOP");
        candidate.put("index", 0);
        ObjectNode body = mapper.createObjectNode();
        body.set("candidates", mapper.createArrayNode().add(candidate));
        body.set("usageMetadata", mapper.createObjectNode()
                .put("promptTokenCount", 1).put("candidatesTokenCount", 1).put("totalTokenCount", 2));
        body.put("modelVersion", "fake-gemini");
        return body;
    }

    private ObjectNode nativeCall(String name, String argsJson) {
        try {
            ObjectNode functionCall = mapper.createObjectNode();
            functionCall.put("name", name);
            functionCall.set("args", mapper.readTree(argsJson));
            ObjectNode part = mapper.createObjectNode();
            part.set("functionCall", functionCall);
            part.put("thoughtSignature", newSignature());
            return part;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- Endpoint tương thích OpenAI

    private String openAiSignatureViolation(JsonNode request) {
        if (!enforceSignatures) {
            return null;
        }
        JsonNode messages = request.path("messages");
        int turnStart = 0;
        for (int i = 0; i < messages.size(); i++) {
            if ("user".equals(messages.get(i).path("role").asText())) {
                turnStart = i;
            }
        }
        for (int i = turnStart + 1; i < messages.size(); i++) {
            JsonNode m = messages.get(i);
            if (!"assistant".equals(m.path("role").asText()) || !m.path("tool_calls").isArray()) {
                continue;
            }
            JsonNode first = m.path("tool_calls").get(0);
            String sig = first.path("extra_content").path("google").path("thought_signature").asText("");
            if (sig.isEmpty() || !issuedSignatures.contains(sig)) {
                return missingSignatureMessage(first.path("function").path("name").asText(), i);
            }
        }
        return null;
    }

    private ObjectNode openAiAnswer(JsonNode request) {
        JsonNode messages = request.get("messages");
        JsonNode last = messages.get(messages.size() - 1);
        String role = last.get("role").asText();

        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");
        String finish = "stop";

        if ("tool".equals(role)) {
            message.put("content", "Kết quả công cụ: " + last.get("content").asText());
        } else {
            String user = plain(last.get("content").isArray()
                    ? last.get("content").get(0).path("text").asText() : last.get("content").asText());
            if (user.contains("them vao gio")) {
                message.putNull("content");
                message.set("tool_calls", openAiToolCall("addToCart", "{\"departureId\":1,\"adults\":2,\"children\":0}"));
                finish = "tool_calls";
            } else if (user.contains("bien")) {
                message.putNull("content");
                message.set("tool_calls", openAiToolCall("searchTours", "{\"query\":\"biển đảo\",\"maxPriceVnd\":6000000}"));
                finish = "tool_calls";
            } else {
                message.put("content", "Xin chào, mình là trợ lý tour.");
            }
        }

        ObjectNode choice = mapper.createObjectNode();
        choice.put("index", 0);
        choice.set("message", message);
        choice.put("finish_reason", finish);

        ObjectNode body = mapper.createObjectNode();
        body.put("id", "chatcmpl-fake");
        body.put("object", "chat.completion");
        body.put("created", 1);
        body.put("model", "fake-chat");
        body.set("choices", mapper.createArrayNode().add(choice));
        body.set("usage", mapper.createObjectNode().put("prompt_tokens", 1).put("completion_tokens", 1).put("total_tokens", 2));
        return body;
    }

    private ArrayNode openAiToolCall(String name, String arguments) {
        ObjectNode function = mapper.createObjectNode();
        function.put("name", name);
        function.put("arguments", arguments);
        ObjectNode call = mapper.createObjectNode();
        call.put("id", "call_" + name);
        call.put("type", "function");
        call.set("function", function);
        // Đúng như Gemini thật: chữ ký nằm ở extra_content.google.thought_signature của tool_call.
        call.set("extra_content", mapper.createObjectNode().set("google",
                mapper.createObjectNode().put("thought_signature", newSignature())));
        return mapper.createArrayNode().add(call);
    }

    // ---------------------------------------------------------------- Dùng chung

    private String plain(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd').toLowerCase(Locale.ROOT);
    }

    private ObjectNode embedding(String text, int index) {
        float[] v = new float[DIMS];
        String plain = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd');
        for (String word : plain.split("[^a-z0-9]+")) {
            if (word.length() > 1) {
                v[Math.floorMod(word.hashCode(), DIMS)] += 1f;
            }
        }
        ArrayNode values = mapper.createArrayNode();
        for (float f : v) {
            values.add(f);
        }
        ObjectNode node = mapper.createObjectNode();
        node.put("object", "embedding");
        node.put("index", index);
        node.set("embedding", values);
        return node;
    }

    private void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
