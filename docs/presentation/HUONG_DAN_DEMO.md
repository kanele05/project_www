# Hướng dẫn demo: Spring AI, Vector Database và User Tracking

Tài liệu đi kèm bài thuyết trình `SpringAI_VectorDB_UserTracking.pptx` (Phần 2, slide 16 đến 33).
Thời lượng demo trực tiếp khoảng **10 đến 15 phút**. Mọi đường dẫn dưới đây tính từ thư mục gốc dự án
`C:\Users\Khangg\Downloads\www\project_www`.

> **Đọc trước khi lên lớp.** Code đã được kiểm bằng 73 kiểm thử tự động và chạy cả ứng dụng trên H2 với
> máy chủ giả giao thức OpenAI. **Chưa ai chạy với SQL Server thật và Gemini thật trong môi trường làm bài**, và
> giao diện chưa được nhìn bằng mắt trên trình duyệt. Hãy **chạy thử toàn bộ kịch bản một lần trước giờ trình bày**
> (mục 8) và chụp sẵn ảnh dự phòng (mục 7).

---

## 1. Chuẩn bị một lần (làm trước ngày trình bày)

| Việc | Lệnh / thao tác |
|---|---|
| Lấy mã mới | `git pull origin claude/amazing-lamport-4v22ru` (hoặc nhánh đã gộp) |
| Tải thư viện Spring AI (cần mạng, **không dùng `-o`**) | `./mvnw -DskipTests compile` |
| Tạo bảng `user_events` trong CSDL đang có | mục 3 |
| Lấy khoá Gemini miễn phí | mục 2 |
| Chạy kiểm thử tự động (không cần SQL Server, không cần khoá) | `./mvnw test` phải báo `Tests run: 73, Failures: 0` |

Biến môi trường chung (như `CLAUDE.md`):

```bash
export JAVA_HOME="/c/Users/Khangg/.jdks/corretto-23.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export SQLCMD="/c/Program Files/Microsoft SQL Server/Client SDK/ODBC/180/Tools/Binn/SQLCMD.EXE"
```

---

## 2. Cấu hình nhà cung cấp mô hình

### 2.1. Gemini (đường chính, miễn phí)

1. Vào <https://aistudio.google.com/apikey>, đăng nhập tài khoản Google, bấm **Create API key**.
2. Chép khoá (dạng `AIza...`). **Không dán khoá vào chat, vào mã nguồn hay vào file cấu hình đưa lên GitHub** (kho công khai).
3. Đặt biến môi trường `GEMINI_API_KEY`:

   **Git Bash** (chỉ có hiệu lực trong cửa sổ đó):
   ```bash
   export GEMINI_API_KEY="AIza...khoá-của-bạn"
   ./mvnw spring-boot:run
   ```

   **PowerShell**:
   ```powershell
   $env:GEMINI_API_KEY = "AIza...khoá-của-bạn"
   .\mvnw.cmd spring-boot:run
   ```

   **IntelliJ** (cách user đang dùng): *Run > Edit Configurations > `TourBookingApplication` > Modify options >
   Environment variables* rồi thêm `GEMINI_API_KEY=AIza...`. Để trống thì AI tắt (không sao, xem mục 6).

4. Kiểm tra nhanh khoá hợp lệ (Git Bash): `curl -s https://generativelanguage.googleapis.com/v1beta/openai/models -H "Authorization: Bearer $GEMINI_API_KEY" | head -c 300`.
   Có danh sách mô hình là khoá dùng được; lỗi 400/403 là khoá sai hoặc chưa bật.

Mặc định (không cần đặt thêm): chat `gemini-2.5-flash`, embedding `gemini-embedding-001` (3072 chiều),
địa chỉ `https://generativelanguage.googleapis.com`, đường dẫn `/v1beta/openai/...`.

**Các biến tuỳ chọn:**

| Biến | Dùng khi | Ví dụ |
|---|---|---|
| `AI_CHAT_MODEL` | bị giới hạn tốc độ, muốn hạn mức cao hơn | `gemini-2.5-flash-lite` |
| `AI_SIMILARITY_THRESHOLD` | tìm ngữ nghĩa ra quá ít hoặc quá nhiều kết quả | `0.4` (ít chặt hơn) hoặc `0.6` |
| `AI_ENABLED` | ép tắt AI dù có khoá (demo chế độ dự phòng) | `false` |
| `AI_VECTOR_STORE_PATH` | đổi chỗ lưu chỉ mục | `data/vector-store.json` |

**Lưu ý gói miễn phí:**

- Giới hạn số yêu cầu mỗi phút và mỗi ngày (con số thay đổi theo thời gian, xem trang *Rate limits* trong AI Studio). Chatbot dùng tool có thể
  gọi mô hình 2 đến 3 lần cho một câu hỏi, nên đừng gõ dồn dập. Ứng dụng tự giới hạn 8 tin nhắn mỗi phút mỗi phiên và tự thử lại khi gặp 429.
- Lần đầu lập chỉ mục 20 tour cần 20 lần gọi embedding, mỗi lần cách nhau 0,7 giây (khoảng 15 giây, chạy nền). **Lập sẵn trước giờ demo.**
  Những lần khởi động sau nạp thẳng từ `data/vector-store.json`, không gọi lại API.
- **Quyền riêng tư:** ở gói miễn phí, Google có thể dùng nội dung gửi lên để cải thiện sản phẩm. **Không đưa dữ liệu khách thật vào lúc demo**
  (dùng tài khoản mẫu `an.nguyen@gmail.com`, nội dung câu hỏi chỉ về tour).

### 2.2. Đổi nhà cung cấp (một dòng)

- **OpenAI:** `AI_BASE_URL=https://api.openai.com`, `AI_CHAT_PATH=/v1/chat/completions`, `AI_EMBEDDINGS_PATH=/v1/embeddings`,
  `AI_CHAT_MODEL=gpt-4o-mini`, `AI_EMBEDDING_MODEL=text-embedding-3-small`, và `GEMINI_API_KEY=<khoá OpenAI>` (tên biến không đổi, chỉ là nơi chứa khoá).
- **Ollama chạy máy mình:** `AI_BASE_URL=http://localhost:11434`, hai đường dẫn như OpenAI, `AI_CHAT_MODEL=qwen2.5`, `AI_EMBEDDING_MODEL=bge-m3`,
  `GEMINI_API_KEY=ollama` (khoá giả để bật AI). Mỗi lần **đổi mô hình nhúng hoặc nhà cung cấp**, ứng dụng tự nhận ra và lập lại chỉ mục.

---

## 3. Tạo bảng `user_events` trong CSDL đang có

CSDL dựng mới từ `02_schema.sql` đã có bảng này. CSDL cũ thì chạy script idempotent (chạy lại nhiều lần vẫn an toàn):

```bash
"$SQLCMD" -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' -d TourBookingDB -C -f 65001 -i database/05_user_events.sql
```

PowerShell:

```powershell
& "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\180\Tools\Binn\SQLCMD.EXE" -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' -d TourBookingDB -C -f 65001 -i database\05_user_events.sql
```

Đầu ra phải có `Đã tạo bảng user_events.` (hoặc `đã tồn tại, bỏ qua`) và **không có chữ `Msg`**. Vì app chạy `ddl-auto: validate`, **thiếu bảng này app không khởi động**.

---

## 4. Chạy ứng dụng và kiểm tra trước giờ demo

1. Chạy app (mục 2.1). Trong log tìm dòng `Đã lập chỉ mục 20 tour` (lần đầu) hoặc `Đã nạp 20 tour từ data/vector-store.json`.
2. Đăng nhập `admin@tourbooking.vn` / `123456` > menu **Trợ lý AI** (`/admin/ai`): phải hiện *Đang bật*, khoá API *Đã có*, số tour đã lập chỉ mục = **20**.
   Nếu thấy lỗi gần nhất (429, 401...) xem mục 6.
3. Thử nhanh: `/tours?q=biển yên tĩnh cho gia đình dưới 5 triệu&ai=true` phải hiện thông báo xanh *Kết quả được AI xếp theo độ phù hợp*.
4. Mở khung chat góc phải dưới, hỏi một câu bất kỳ.
5. Xoá dữ liệu hành vi cũ nếu muốn thống kê sạch: `DELETE FROM user_events;` (chạy bằng `sqlcmd -Q`).

---

## 5. Kịch bản demo từng bước (khoảng 13 phút)

Mở sẵn 2 cửa sổ trình duyệt: **(A)** khách chưa đăng nhập (cửa sổ ẩn danh), **(B)** admin đã đăng nhập. Slide dùng để chuyển giữa các bước là số slide trong cột 2.

| # | Thời gian | Slide | Thao tác trên màn hình | Nói gì |
|---|---|---|---|---|
| 0 | 1' | 16, 17 | Chỉ slide kiến trúc | "Website đặt tour vốn dùng SQL Server và giỏ trong session. Nhóm thêm ba thứ: tìm theo ý, trợ lý chat, và ghi hành vi để thống kê. SQL Server vẫn là nguồn gốc, kho vector chỉ là chỉ mục phụ lưu ra tệp." |
| 1 | 1' | 18, 19, 20 | Cửa sổ B, `/admin/ai` | "Dùng một starter OpenAI trỏ sang Gemini miễn phí nên đổi nhà cung cấp chỉ cần đổi biến môi trường. Đây là Spring AI 1.1.8 vì bản 2.0 của Chương 7 nhắm tới Spring Boot 4. 20 tour đã được nhúng thành vector và lưu ra tệp, khởi động lại không phải gọi API nữa." |
| 2 | 2' | 21, 22 | Cửa sổ A, **Tất cả tour**, bật **Tìm bằng AI**, gõ câu 1 rồi câu 2 (bảng dưới) | "Ô tìm cũ chỉ khớp chuỗi ký tự. Giờ tôi mô tả bằng lời, hệ thống tách cụm 'dưới 5 triệu' thành bộ lọc giá rồi so ý nghĩa với vector của từng tour." Tắt công tắc, gõ lại cùng câu: "Cùng câu mà tìm từ khoá thì không ra gì." |
| 3 | 1' | 21 | Mở **Nha Trang** (hoặc **Sa Pa**), kéo xuống khối **Tour tương tự** | "Vector của tour đang xem được so với các tour còn lại, đây là tính năng 'khách xem X cũng quan tâm Y'." |
| 4 | 3' | 23, 24 | Mở khung chat, hội thoại mẫu (bảng dưới) | "Đây là Product Assistant của Chương 7. Mô hình không chạm cơ sở dữ liệu: nó chỉ yêu cầu ứng dụng chạy tool `searchTours`, `getDepartures`, `addToCart`. Mọi kiểm tra còn chỗ, tối đa 30 khách vẫn do `CartService` làm. Chatbot **không có** công cụ thanh toán." Sau đó bấm biểu tượng giỏ để chứng minh giỏ đã có tour. |
| 5 | 1' | 25 đến 29 | Quay lại vài trang tour, bấm 3 đến 4 thẻ tour khác nhau, ở lại mỗi trang vài giây, đổi sang tab khác rồi quay lại; gõ một từ khoá vào ô tìm | "Mỗi lần xem, bấm, tìm kiếm, rời trang, tracker.js gửi một sự kiện bằng `fetch keepalive` kèm CSRF. Máy chủ tự lấy userId và sessionId, băm mã phiên, lọc bot, giới hạn tốc độ. Còn thêm giỏ thì máy chủ tự ghi, nên không đếm khi hết chỗ." |
| 6 | 2' | 30, 31 | Cửa sổ B, **Hành vi người dùng** (`/admin/analytics`), bấm *7 ngày* | "Phễu: bao nhiêu phiên có xem tour, bao nhiêu phiên thêm giỏ, bao nhiêu đơn thật từ bảng đơn hàng. Top xem, top bấm, từ khoá tìm kiếm, thời gian xem trung bình. Tất cả bằng JPQL, không thủ tục lưu trữ." |
| 7 | 1' | 32 | Cửa sổ A, về **Trang chủ**, chỉ khối **Có thể bạn thích** | "Gợi ý lấy các tour vừa xem làm hạt giống, tìm tour giống bằng vector rồi xếp lại theo 0,7 độ giống cộng 0,3 độ phổ biến. Khách mới chưa có lịch sử thì thấy tour phổ biến." |
| 8 | 1' | 34 | Chuyển về slide kết luận | "Tóm lại: một API thống nhất của Spring AI, tìm kiếm và gợi ý theo ngữ nghĩa, và tracking biến hành vi thành số liệu. Khi tắt AI, website vẫn chạy đủ." |

**Câu tìm kiếm nên dùng với dữ liệu mẫu** (kết quả cụ thể phụ thuộc mô hình, **hãy thử trước và chọn câu nào ra đẹp**):

| Câu gõ (bật Tìm bằng AI) | Thường ra |
|---|---|
| `biển yên tĩnh cho gia đình dưới 5 triệu` | Nha Trang 4,59 triệu, Hạ Long 3,89 triệu (Phú Quốc 5,99 triệu bị loại bởi bộ lọc giá) |
| `săn mây trên núi cao` | Sa Pa, Hà Giang |
| `hành hương cầu bình an` | Yên Tử (Quảng Ninh), Tây Ninh Núi Bà Đen, Côn Đảo |
| `nghỉ dưỡng resort sát biển` | Mũi Né, Phú Quốc, Nha Trang |
| `trải nghiệm miệt vườn chợ nổi` | Miền Tây, Cát Tiên |
| `đi nước ngoài ngắm hoa anh đào từ 20 triệu` | Nhật Bản (tour 28,9 triệu, nhờ bộ lọc giá tối thiểu) |

**Hội thoại chatbot mẫu:**

1. `Có tour biển nào dưới 6 triệu không?` > bot gọi `searchTours` rồi liệt kê vài tour kèm giá và liên kết.
2. `Cho mình xem lịch khởi hành của Nha Trang` > bot gọi `getDepartures` (có ngày, số chỗ, giá).
3. `Thêm lịch gần nhất cho 2 người lớn` > bot hỏi xác nhận, bạn đáp `đồng ý` > bot gọi `addToCart`. Số trên biểu tượng giỏ tăng.
4. `Bạn thanh toán giúp mình luôn nhé` > bot từ chối và hướng dẫn vào **Giỏ hàng** (chứng minh giới hạn an toàn).

**Mẹo để số liệu thống kê có hình ảnh đẹp:** trước giờ demo đặt thử 1 đến 2 đơn bằng `an.nguyen@gmail.com`; cột *Đơn* và bước *Đơn đặt tour* của phễu lấy từ đơn thật
trong khoảng ngày đã chọn (đơn đã huỷ không tính).

---

## 6. Kế hoạch dự phòng khi AI hoặc mạng hỏng

| Tình huống | Dấu hiệu | Cách xử lý |
|---|---|---|
| **Không có khoá / để trống** | Khung chat báo *AI chưa được cấu hình*; `/admin/ai` báo *Đang tắt* | **Đây vẫn là một phần demo hợp lệ.** Nói: "Hệ thống thiết kế để chạy đủ khi AI không khả dụng": bật *Tìm bằng AI* thì công tắc bị khoá và tìm lùi về từ khoá; *Tour tương tự* lùi về cùng danh mục; *Có thể bạn thích* lùi về tour phổ biến; tracking và thống kê vẫn chạy. |
| **Mạng đứt khi chạy** | Chat trả *Trợ lý AI tạm thời chưa phản hồi*, tìm AI hiện thông báo vàng | Website không sập. Chuyển sang cửa sổ ảnh chụp/video (mục 7) cho phần chatbot và tìm ngữ nghĩa; tracking và thống kê chạy offline bình thường. |
| **Lỗi 429 (hết hạn mức)** | `/admin/ai` hiện lỗi gần nhất chứa `429` | Đợi 1 phút. Hoặc dừng app, đặt `AI_CHAT_MODEL=gemini-2.5-flash-lite` rồi chạy lại. Đừng bấm *Lập chỉ mục lại* lúc đang demo. |
| **Khoá sai (401/403)** | Chat báo chưa phản hồi, log có `401` | Kiểm tra lại biến (không có dấu cách, không thừa dấu nháy), thử `curl` ở mục 2.1. |
| **Tìm ngữ nghĩa trả quá ít hoặc quá nhiều** | Luôn hiện thông báo vàng (lùi về từ khoá) hoặc ra cả 20 tour | Chỉnh `AI_SIMILARITY_THRESHOLD` (hạ xuống `0.4` nếu ít, nâng lên `0.6` nếu nhiều), chạy lại, không cần lập chỉ mục lại. |
| **Muốn ép chế độ dự phòng để so sánh** | | `AI_ENABLED=false` rồi chạy lại. |
| **Chỉ mục hỏng** | Số tour đã lập chỉ mục = 0 | Xoá `data/vector-store.json` và `data/vector-store.json.meta.json`, chạy lại (hoặc *Lập chỉ mục lại* ở `/admin/ai`). |
| **Không bật được máy chủ SQL** | App không khởi động | Chạy bằng cơ sở dữ liệu đã dựng sẵn trên máy khác, hoặc trình chiếu ảnh chụp. Slide vẫn đủ để giải thích kiến trúc. |

---

## 7. Ảnh chụp dự phòng cần chụp trước (1440px, dữ liệu thật)

1. `/tours` với kết quả *Tìm bằng AI* cho câu `biển yên tĩnh cho gia đình dưới 5 triệu`.
2. Trang chi tiết tour có khối *Tour tương tự*.
3. Khung chat với hội thoại mục 5 (hiện cả tour được thêm vào giỏ).
4. Trang `/admin/analytics` sau khi đã tạo vài sự kiện.
5. Trang `/admin/ai`.
6. Khung chat ở trạng thái *AI chưa được cấu hình* (bỏ khoá) để minh hoạ chế độ dự phòng.

Có thể quay thêm một video 1 đến 2 phút toàn bộ kịch bản. Báo cáo Word có chỗ chờ chèn ảnh (Hình 5.2 và 5.3 ở Chương 5).

---

## 8. Chạy thử toàn bộ kịch bản (checklist trước giờ trình bày, 20 phút)

- [ ] `./mvnw test` xanh (73 kiểm thử).
- [ ] Đã chạy `05_user_events.sql`, không có `Msg`.
- [ ] `/admin/ai` báo 20 tour, khoá *Đã có*.
- [ ] Cả 6 câu tìm kiếm mẫu: chọn ra 2 câu cho kết quả đẹp; ghi lại.
- [ ] Hội thoại chatbot đủ 4 câu chạy được; giỏ có tour sau câu 3.
- [ ] `/admin/analytics` hiện số liệu sau khi duyệt vài tour (cần tắt chế độ chặn theo dõi của trình duyệt: tính năng *Do Not Track* làm tracker.js không gửi gì).
- [ ] Trang chủ hiện *Có thể bạn thích* dựa trên tour vừa xem.
- [ ] Đã chụp ảnh dự phòng (mục 7).
- [ ] Khoá API **không** nằm trong file nào của dự án, không có trong màn hình chia sẻ.

---

## 9. Lý thuyết (Phần 1) ứng với mã nguồn (Phần 2)

| Khái niệm | Slide lý thuyết | Lớp / tệp trong dự án |
|---|---|---|
| ChatClient, system prompt | 5 | `config/AiConfig.java` (bean `tourAssistant`), `ai/AiChatService.java` |
| Advisor + Chat Memory | 8 | `MessageChatMemoryAdvisor`, bean `ChatMemory` trong `AiConfig` |
| RAG với QuestionAnswerAdvisor | 8 | `AiConfig` (QuestionAnswerAdvisor trên `TourVectorStore`) |
| Tool Calling (`@Tool`, `ToolContext`) | 7 | `ai/TourTools.java` |
| Embedding, cosine | 9 | `gemini-embedding-001` qua `EmbeddingModel`; `ai/ThrottledEmbeddingModel.java` |
| Document, metadata, id | 10 | `ai/TourDocumentFactory.java` |
| VectorStore / SimpleVectorStore | 10 | `ai/TourVectorStore.java`, `ai/TourVectorService.java`, tệp `data/vector-store.json` |
| similaritySearch, topK, threshold, filter | 10 | `TourVectorService.search()`, `ai/PriceHintParser.java` |
| Tìm kiếm ngữ nghĩa trong dự án | 11, 22 | `ai/SemanticSearchService.java`, `TourController`, `TourApiController#semantic` |
| Sản phẩm tương tự, gợi ý | 11, 32 | `service/RecommendationService.java` |
| Cấu hình, khoá API, công tắc | 12, 19 | `config/AiProperties.java`, `AiEnabledCondition`, `AiEnvironmentPostProcessor`, `application.yml` |
| Mock ChatModel khi kiểm thử | 12, 34 | `src/test/.../ai/AiChatServiceTest.java`, `web/FakeOpenAiServer.java` |
| Các chỉ số tracking, phễu | 13 | `service/BehaviorStatsService.java`, `UserEventRepository` |
| Event delegation, Page Visibility, keepalive | 14 | `static/js/tracker.js` |
| Quyền riêng tư, không tin client, rate limit | 15 | `service/TrackingService.java`, `dto/form/TrackingRequest.java`, `util/SlidingWindowRateLimiter.java` |
| Bảng sự kiện | 25, 26 | `database/02_schema.sql`, `05_user_events.sql`, `entity/UserEvent.java`, `EventType`, `EventTypeConverter` |
| API nhận sự kiện | 27 | `controller/api/TrackingApiController.java`, `SecurityConfig` |
| Trang thống kê | 30, 31 | `controller/admin/AdminAnalyticsController.java`, `templates/admin/analytics.html` |
| Khung chat | 24 | `templates/fragments/chat-widget.html`, `static/js/chat.js`, `controller/api/ChatApiController.java` |
| Trang quản trị AI, lập chỉ mục lại | 21 | `controller/admin/AdminAiController.java`, `templates/admin/ai.html` |

---

## 10. Câu hỏi giảng viên có thể hỏi

- **Vì sao không dùng Spring AI 2.0.2 như bài giảng?** 2.0.x dùng Jackson 3 và nhắm tới Spring Boot 4; dự án chạy Boot 3.5.16, nên chọn bản 1.x mới nhất (1.1.8). Các khái niệm (ChatClient, advisor, tool, VectorStore) giống nhau.
- **Vì sao không dùng PgVector như slide gốc?** Môi trường đồ án không có PostgreSQL, SQL Server không nằm trong danh sách store của Chương 7. `SimpleVectorStore` lưu tệp đủ cho vài trăm tour; đổi sang PgVector chỉ cần đổi bean, các lớp nghiệp vụ dùng giao diện `VectorStore`.
- **AI có chạm vào CSDL không?** Không. Mô hình chỉ yêu cầu chạy tool, ứng dụng chạy và kiểm tra bằng `CartService`, `TourService`. Chatbot không có công cụ thanh toán.
- **Dữ liệu khách có gửi cho Google không?** Chỉ nội dung câu hỏi và mô tả tour. Gói miễn phí có thể được Google dùng để cải thiện sản phẩm; vì vậy không dùng dữ liệu thật khi demo.
- **Tracking có theo dõi cá nhân không?** Mã phiên được băm SHA-256, userId do máy chủ lấy từ phiên đăng nhập, từ khoá và đường dẫn bị cắt; dữ liệu xoá sau 180 ngày; tôn trọng Do Not Track. Còn thiếu biểu ngữ xin đồng ý (nói rõ là hạn chế).
- **Tại sao ADD_TO_CART ghi ở server?** Để không đếm cả những lần thêm giỏ thất bại (hết chỗ), và để chatbot thêm giỏ cũng được tính.
- **Nếu AI sập thì sao?** Website không sập: tìm lùi về từ khoá, chat báo thân thiện, gợi ý lùi về cùng danh mục. Có kiểm thử tự động cho các nhánh này.
- **Độ chính xác của tìm ngữ nghĩa ra sao?** Phụ thuộc mô hình nhúng và ngưỡng giống nhau (`AI_SIMILARITY_THRESHOLD`); ngưỡng mặc định 0,5 là ước lượng cần chỉnh theo dữ liệu thật.
