package vn.edu.iuh.fit.tourbooking.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

// Máy chủ giả nói giao thức OpenAI (đúng đường dẫn tương thích của Gemini) để kiểm thử cả chuỗi Spring AI mà không cần mạng hay khoá thật.
public class FakeOpenAiServer {

    private static final int DIMS = 256;
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    public final AtomicInteger embeddingRequests = new AtomicInteger();
    public final AtomicInteger chatRequests = new AtomicInteger();
    public volatile String lastAuthorization;
    public volatile String lastChatBody;
    public volatile int chatFailureStatus = 0;

    public FakeOpenAiServer start() throws IOException {
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
            chatRequests.incrementAndGet();
            lastAuthorization = ex.getRequestHeaders().getFirst("Authorization");
            String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastChatBody = raw;
            if (chatFailureStatus != 0) {
                respond(ex, chatFailureStatus, "{\"error\":{\"message\":\"quota\"}}");
                return;
            }
            respond(ex, 200, chatAnswer(mapper.readTree(raw)).toString());
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

    // Kịch bản: hỏi "biển" -> gọi searchTours; có kết quả công cụ -> trả lời chứa kết quả đó; nhắn "thêm vào giỏ" -> gọi addToCart.
    private ObjectNode chatAnswer(JsonNode request) {
        JsonNode messages = request.get("messages");
        JsonNode last = messages.get(messages.size() - 1);
        String role = last.get("role").asText();

        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");
        String finish = "stop";

        if ("tool".equals(role)) {
            message.put("content", "Kết quả công cụ: " + last.get("content").asText());
        } else {
            String user = flatten(last.get("content")).toLowerCase(Locale.ROOT);
            if (user.contains("them vao gio")) {
                message.putNull("content");
                message.set("tool_calls", toolCall("addToCart", "{\"departureId\":1,\"adults\":2,\"children\":0}"));
                finish = "tool_calls";
            } else if (user.contains("bien")) {
                message.putNull("content");
                message.set("tool_calls", toolCall("searchTours", "{\"query\":\"biển đảo\",\"maxPriceVnd\":6000000}"));
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

    private ArrayNode toolCall(String name, String arguments) {
        ObjectNode function = mapper.createObjectNode();
        function.put("name", name);
        function.put("arguments", arguments);
        ObjectNode call = mapper.createObjectNode();
        call.put("id", "call_" + name);
        call.put("type", "function");
        call.set("function", function);
        return mapper.createArrayNode().add(call);
    }

    private String flatten(JsonNode content) {
        String text = content.isArray() ? content.get(0).path("text").asText() : content.asText();
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd');
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

    private void respond(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
