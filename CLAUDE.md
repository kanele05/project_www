# TourBooking — quy ước cho agent

Đồ án môn **Lập trình WWW (Java)** — IUH. Spring Boot 3.5.16 + Thymeleaf + **MariaDB** (từ Phase 16;
trước đó là SQL Server — các gotcha riêng của SQL Server trong PLAN.md chỉ còn giá trị lịch sử).
**Đọc [PLAN.md](PLAN.md) trước khi làm bất cứ việc gì** — nó có tiến độ từng phase, domain model,
và **86 gotcha** (phần lớn là lỗi đã thực sự vấp phải, không phải phỏng đoán).

## Chạy

```bash
export JAVA_HOME="/c/Users/Khangg/.jdks/corretto-23.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export MARIADB="/c/Program Files/MariaDB 12.2/bin/mariadb.exe"   # MariaDB 12.2, dịch vụ ở cổng 3306
./mvnw -o spring-boot:run        # http://localhost:8081  (8080 bị XAMPP chiếm)
```

Dựng CSDL lần đầu: `01_create_database.sql` chạy bằng **root** (user tự chạy, agent không có mật khẩu root),
rồi `02_schema.sql`, `03_seed_data.sql` bằng `tourapp` — lệnh đầy đủ ở PLAN.md mục "Môi trường".
Đổi CSDL đích bằng biến `DB_URL` (mặc định `jdbc:mariadb://localhost:3306/TourBookingDB`).

Tính năng AI (tuỳ chọn, mặc định **tắt** khi không có khoá — app vẫn chạy đủ chức năng):

```bash
export GEMINI_API_KEY="..."        # khoá miễn phí ở https://aistudio.google.com/apikey ; thiếu/rỗng = AI tắt
# AI_CHAT_MODEL đổi mô hình chat (vd. gemini-3.5-flash-lite). Chat đi qua SDK Gemini gốc (giữ thought_signature, gotcha #80);
# chat bằng OpenAI/Ollama: AI_CHAT_PROVIDER=openai + AI_BASE_URL, AI_CHAT_PATH, AI_EMBEDDINGS_PATH, ... (xem PLAN.md Phase 15)
bash ./mvnw test                   # 86 kiểm thử, không cần MariaDB (H2 chế độ MariaDB + máy chủ Gemini giả có ép luật thought_signature)
```

CSDL đã có sẵn muốn có bảng theo dõi hành vi: chạy `database/05_user_events.sql` (idempotent). `data/` (chỉ mục vector) bị `.gitignore`.

Tài khoản mẫu, mật khẩu đều `123456`: `admin@tourbooking.vn` (ADMIN) · `an.nguyen@gmail.com` ·
`binh.tran@gmail.com` · `cuong.le@gmail.com`.

## Bảy quy tắc dễ vi phạm nhất

1. **`ddl-auto: validate`** — sửa entity thì **phải** sửa cả `database/02_schema.sql` rồi chạy lại,
   không thì app không khởi động.
2. **Enum mới phải có `AttributeConverter` riêng**, cấm `@Enumerated` — nếu không Hibernate sinh
   ràng buộc `CHECK` mà đề bài cấm. Cũng **cấm `columnDefinition` kiểu chuỗi** trên entity.
3. **Câu chữ mới vào cả ba file** `messages.properties`, `messages_vi.properties`,
   `messages_en.properties` (hiện 850 khoá, khớp nhau — lệnh đối chiếu ở cuối PLAN.md).
4. **Đổi màu thì sửa token ở mục 1 của `static/css/app.css`**, đừng rải mã màu vào template.
5. **Sửa `resources/` xong phải `./mvnw -o process-resources`** — `spring-boot:run` không tự nạp lại
   template/CSS/messages.
6. **Đừng tin mã 200**: lỗi Thymeleaf giữa chừng vẫn trả 200 với HTML cắt cụt. Kiểm bằng
   `grep -c "</html>"`.
7. **Kiểm thử có tiếng Việt thì truyền qua file**, đừng qua tham số dòng lệnh (Git Bash trên Windows
   làm hỏng non-ASCII).

## Quy ước mã nguồn

- Quy tắc nghiệp vụ **nằm ở tầng service bằng Java**, không dùng CHECK / Function / Stored Procedure
  / Trigger — đề bài cấm. Khoá ngoại chỉ là lớp bảo vệ cuối cùng, không phải nơi báo lỗi cho người
  dùng. Chặn xoá thì ném `BusinessRuleException` mang **message key**, và gợi ý phương án thay thế.
- `open-in-view: false` là **cố ý**. Cần hiện quan hệ LAZY ra template thì thêm `LEFT JOIN FETCH`
  vào truy vấn, đừng bật lại.
- Lombok trên entity chỉ được dùng `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`.
  **Cấm `@Data`/`@ToString`/`@EqualsAndHashCode`** (quan hệ hai chiều → `StackOverflowError`).
- Cấu hình Spring nằm ở `application.properties` (môn học yêu cầu), **không dùng YAML**. Loader đọc `.properties` theo
  ISO-8859-1: giá trị có dấu phải viết `\uXXXX`, comment thì tuỳ ý (gotcha #79).
- Mọi POST kết thúc bằng `redirect:` + flash attribute (PRG).
- Ghi chú nội bộ trong template dùng `<!--/* ... */-->` để không lọt ra HTML.

## Giao diện

Bảng màu **"Ngọc bích & Đất nung"** (user chọn, đừng tự đổi) — token ở mục 1 của `app.css`.
Phông Playfair Display + Be Vietnam Pro (chọn có chủ đích cho dấu tiếng Việt).

Có sẵn hai bộ skill trong `.claude/skills/`: **taste-skill** (12 skill, đáng chú ý `soft-skill`,
`redesign-skill`) và **impeccable** (`reference/craft-floor.md` là sàn chất lượng).
⚠️ Launcher `impeccable/scripts/impeccable` **tải một binary về rồi chạy** — dùng đường dự phòng
"đọc thẳng file reference" trừ khi user cho phép.

**Một hiệu ứng trang trí không bao giờ được phép làm mất nội dung.** Mọi luật animation phải nằm sau
lớp `.anim-ready` do JS gắn, và reveal khi cuộn phải có hẹn giờ bảo hiểm (xem gotcha #29, #33).
