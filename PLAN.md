# Plan: Website giới thiệu & đăng ký tour du lịch trực tuyến

> **Ghi chú về file này.** Bản plan gốc nằm ngoài repo
> (`~/.claude/plans/gi-h-y-b-t-tay-distributed-patterson.md`) và **đã mất**. Đây là bản dựng lại,
> đặt trong chính thư mục project để không mất lần nữa. Nội dung lấy từ lịch sử phiên làm việc và
> **đã đối chiếu với mã nguồn / CSDL hiện tại**. Chỗ nào không chắc đều ghi rõ.
>
> Cập nhật lần cuối: 2026-10-30 (Phase 15 - AI + theo dõi hành vi; chat chuyển sang SDK Gemini gốc, xem gotcha #80-81). Các phase trước giữ nguyên.

## Bối cảnh

Bài tập lớn nhóm môn **Lập trình WWW (Java)** — IUH, Khoa CNTT (GV: ThS. Đặng Thị Thu Hà).
Đề tài: **"Website giới thiệu tour du lịch và cho phép đăng ký tour trực tuyến"** (nhóm đề tài
51–54, có bộ yêu cầu riêng khác nhóm 01–50). Chấm theo **3 giai đoạn** (phân tích/thiết kế →
Spring Data + Spring MVC → Spring Security + hoàn thiện) và 6 CLO.

### Quyết định đã chốt

| Hạng mục | Lựa chọn |
|---|---|
| Kiến trúc | Spring Boot 3.5.16 + Spring MVC + **Thymeleaf** SSR + REST controller cho AJAX |
| Database | **SQL Server** (2025 Developer Ed.) |
| Java | target **21** (compile bằng Corretto 23 với `--release 21`) |
| Tính năng nâng cao | Upload ảnh tour · Spring Security · i18n VI/EN |
| Email | `EmailService` interface + hai bản cài đặt (`ConsoleEmailService` ghi console, `SmtpEmailService` gửi thật qua `JavaMailSender`), chọn bằng `app.mail.mode=console\|smtp` (mặc định `console`, không cần tài khoản SMTP). Gửi thư xác nhận đặt tour **và** thư chào mừng đăng ký, cả hai qua `@TransactionalEventListener(AFTER_COMMIT)` (Phase 9). |

**Không dùng Boot 4.x**: breaking changes, mọi tài liệu môn học đều 3.x. `pom.xml` viết tay hoàn
toàn vì Initializr nay chỉ sinh được Boot 4.x với các artifact không tồn tại ở 3.5.x.

---

## 📌 Tiến độ

| Phase | Trạng thái |
|---|---|
| **0** — Môi trường & scaffold | ✅ xong, verify bằng lệnh thật |
| **1** — Entity / Repository / Seeder | ✅ xong |
| **2** — Trang công khai + giỏ Session | ✅ xong |
| **3** — Tài khoản / Security / Checkout | ✅ xong |
| **4** — Khu vực quản trị | ✅ xong |
| **5** — REST / AJAX / i18n | ✅ xong |
| **5.5** — Làm lại toàn bộ giao diện | ✅ xong |
| **6** — Hardening & báo cáo | 🔶 biểu đồ UML ✅; còn kiểm thử tự động, screenshots, README |
| **7** — Mở rộng 9 bảng ⭐⭐⭐ | 🔶 tầng dữ liệu ✅ (entity + repository + schema + seed, `validate` qua) |
| **7.1** — Mã giảm giá đi trọn luồng tiền | ✅ xong, verify bằng đơn hàng thật |
| **7.2** — Làm giao diện mượt và mềm hơn | ✅ xong, verify bằng trình duyệt thật |
| **7.3** — Dựng lại trang chủ + sửa bố cục khu quản trị | ✅ xong, verify bằng trình duyệt thật (khu quản trị: user tự kiểm) |
| **7.4** — UC018/UC019 giao diện + 20 ảnh tour thật | ✅ xong, verify bằng lệnh thật + trình duyệt thật (chi tiết bên dưới) |
| **7.5** — UC020: kiểm duyệt đánh giá | ✅ xong, verify bằng lệnh thật + trình duyệt thật (chi tiết bên dưới) |
| **7.6** — UC021/UC022 + lịch sử trạng thái đơn + thanh toán ("ba bảng chết") | ✅ xong, verify bằng lệnh thật + trình duyệt thật (chi tiết bên dưới) |
| **8** — Sửa 13 lỗi do hai subagent rà ra (đọc mã + bấm tay) | ✅ xong 13/13, verify bằng lệnh thật (chi tiết bên dưới) |
| **9** — SMTP thật + thư chào mừng đăng ký (subagent EMAIL) | ✅ xong phần console/SMTP-switch/đăng ký; 🔶 chế độ `smtp` với tài khoản Gmail thật **chưa thử** (chi tiết bên dưới) |
| **10** — Sửa 16 lỗi do agent rà lần hai (1 nghiêm trọng, 2 vừa, 13 nhẹ) | ✅ 16/16, verify bằng lệnh thật + `curl` thao tác thật trên đơn/tài khoản thật (chi tiết bên dưới) |
| **11** — Nghiệp vụ đợt B: máy trạng thái, hoàn tiền, UC023 tự huỷ, tự huỷ quá hạn, hạn chót đặt, khoá sửa đợt, thống kê (SPEC_CHUNG.md mục 12.1-12.6, 12.9) | ✅ xong 7/7, verify bằng lệnh thật + `curl` thao tác thật + dựng lại CSDL từ đầu (chi tiết bên dưới) |
| **12** — Nghiệp vụ đợt C: nhập hành khách khi thanh toán, giữ bất biến khi admin sửa số khách, ba trang chi tiết CHỈ XEM cho admin (SPEC_CHUNG.md mục 12.7, 12.8) | ✅ xong 2/2, verify bằng lệnh thật + `curl` + **trình duyệt thật** (đặt tour thật qua UI) + dựng lại từ CSDL thật (chi tiết bên dưới) |
| **13** — Sửa 7 lỗi do agent rà lần ba (1 nghiêm trọng khoá bi quan, 2 vừa, 4 nhẹ) | ✅ 7/7, verify bằng khoá SQL Server thật nới khe đua + hai request `curl` song song thật + dựng lại DB tạm bằng login sysadmin Windows Auth (chi tiết bên dưới) |
| **15** — AI (Spring AI 1.1.8 + Gemini, tìm kiếm ngữ nghĩa, tour tương tự, chatbot RAG + tool) và theo dõi hành vi người dùng (bảng `user_events`, trang thống kê, gợi ý) | ✅ code + 86 kiểm thử tự động xanh, **chạy trọn vẹn trên H2 + máy chủ Gemini giả có ép luật `thought_signature`**; 🔶 **chưa thử với SQL Server thật và chưa gọi Gemini thật** (chi tiết ở Phase 15 bên dưới) |

### 🎨 Bảng màu & hệ thống thiết kế *(user đã chọn, đừng tự đổi)*

**"Ngọc bích & Đất nung"** — cố ý tránh xa xanh Bootstrap `#0d6efd`. Toàn bộ token nằm ở **mục 1
của `static/css/app.css`**; mục 2 là chỗ duy nhất nối sang biến Bootstrap, nên **đổi màu cả
website = sửa mục 1**.

| Vai trò | Mã | Dùng ở đâu |
|---|---|---|
| Ngọc bích sâu | `#0A3833` / `#06251F` | nền hero, thanh điều hướng, chân trang |
| Ngọc bích | `#1B7A6B` | nút chính, liên kết |
| Đất nung | `#C2622D` / `#9C4A1E` | nhấn, giá tiền, nút kêu gọi hành động |
| Vàng nghệ | `#E0A458` | huy hiệu, con số trong hero |
| Cát | `#F6EFE4` | nền trang |
| Mực | `#14201E` | chữ |

Phông: **Playfair Display** (tiêu đề) + **Be Vietnam Pro** (thân) qua Google Fonts, có danh sách dự
phòng đầy đủ nên mất mạng vẫn không vỡ. Be Vietnam Pro chọn **có chủ đích**: thiết kế riêng cho
tiếng Việt nên dấu thanh không chồng lên dấu mũ.

---

## Ràng buộc phải nhớ

- **`ddl-auto: validate`** — sửa entity thì **phải** sửa cả `database/02_schema.sql` rồi chạy lại,
  không thì app không khởi động.
- **Câu chữ mới vào cả 3 file** `messages.properties`, `messages_vi.properties`,
  `messages_en.properties` (hiện **850 khoá**, đã đối chiếu khớp). Lệnh đối chiếu ở cuối file.
- **Đổi màu → sửa token ở mục 1 của `app.css`**, đừng rải mã màu vào template.
- Cấm `columnDefinition` kiểu chuỗi trên entity (#14) · enum dùng `AttributeConverter`, cấm
  `@Enumerated` (#6) · tìm không dấu chạy trên `tours.search_text` (#7) · `th:each` phải ở ngoài
  `th:replace` (#10) · ghi chú template dùng `<!--/* */-->` (#16).
- Kiểm thử có tiếng Việt: **truyền qua file**, đừng qua tham số dòng lệnh (#17).
- Kiểm trang lỗi bằng curl phải thêm `-H "Accept: text/html"` (#26).
- `mvn spring-boot:run` **không** nạp lại `src/main/resources` — sửa template/CSS xong phải chạy
  `./mvnw -o process-resources` (#11).

---

## Môi trường

```bash
cd "C:/Users/Khangg/Downloads/www/project_www"
export JAVA_HOME="/c/Users/Khangg/.jdks/corretto-23.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export SQLCMD="/c/Program Files/Microsoft SQL Server/Client SDK/ODBC/180/Tools/Binn/SQLCMD.EXE"

./mvnw -o spring-boot:run          # -> http://localhost:8081
```

**Cổng 8081**, không phải 8080 (8080 bị Apache/XAMPP chiếm). Còn tiến trình cũ thì:
`netstat -ano | grep :8081` rồi `taskkill //PID <pid> //F`.

Thao tác cần quyền admin (bật TCP cho SQL Server, restart service) **user phải tự chạy** — shell
của agent không elevated.

### Dựng lại CSDL từ số không

```bash
"$SQLCMD" -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' -d TourBookingDB -C -f 65001 -i database/99_drop_all.sql
"$SQLCMD" -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' -d TourBookingDB -C -f 65001 -i database/02_schema.sql
"$SQLCMD" -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' -d TourBookingDB -C -f 65001 -i database/03_seed_data.sql
```

> Đọc đầu ra tìm chữ `Msg` — **đừng dùng `| tail`**, thông báo lỗi nằm giữa output (#9).

### Tài khoản mẫu (mật khẩu đều `123456`)

`admin@tourbooking.vn` (ADMIN) · `an.nguyen@gmail.com` · `binh.tran@gmail.com` · `cuong.le@gmail.com`

---

## Domain model

```
TourCategory 1─N Tour 1─N TourDeparture 1─N BookingDetail N─1 Booking N─1 User
                  1─N TourImage
                  1─N TourItinerary
BookingDetail 1─N BookingPassenger · Booking 1─N Payment · Booking 1─N BookingStatusHistory
Promotion 1─N CouponUsage · Booking N─1 Promotion · Tour/User 1─N Review · ContactMessage
```

**CSDL hiện có 17 bảng** (7 lõi + 9 bổ sung + `user_events` của Phase 15). `Cart` / `CartItem` là **POJO `Serializable` trong
`session/`, KHÔNG phải entity** — đề bài bắt buộc giỏ hàng nằm trong Session.

**Tại sao giữ `TourDeparture`** (thay vì nhét ngày/giá/chỗ thẳng vào `Tour`): nó khiến 3 yêu cầu khó
của đề bài trở nên *có thật* — (1) chuỗi ràng buộc xoá 3 tầng, (2) REST
`/api/tours/{id}/departures` cho date-picker AJAX là web-service **có lý do tồn tại** (CLO6),
(3) checkout có business logic thật (validate + trừ `availableSeats` trong transaction).

### Quan hệ / cascade
- `Tour → TourImage`, `Tour → TourItinerary`: `cascade=ALL, orphanRemoval=true`
- `Tour → TourDeparture`: `cascade={PERSIST,MERGE}` — **cố ý không REMOVE**, lỡ tay `delete()` thì
  fail ở FK chứ không âm thầm xoá đợt đã có khách
- `Booking → BookingDetail`, `BookingDetail → BookingPassenger`: `cascade=ALL, orphanRemoval=true`
- `User → Booking`: **không map collection** — dùng `bookingRepository.findByUserId(id, Pageable)`
- Tất cả `@ManyToOne` đều `fetch=LAZY`; `@GeneratedValue(IDENTITY)`

### Quy tắc chặn xoá — enforce trong Java ở tầng service

| Xoá | Chặn khi | Message key |
|---|---|---|
| `TourCategory` | còn tour | `error.category.delete.hasTours` |
| `Tour` | đã có lượt đặt / **đánh giá** / **liên hệ** | `error.tour.delete.{inBooking,hasReviews,hasContactMessages}` |
| `TourDeparture` | đã có lượt đặt | `error.departure.delete.inBooking` |
| `User` | có đơn · là chính mình · là admin cuối · **đánh giá** · **lượt dùng mã** · **lịch sử đơn** · **liên hệ đã xử lý** | `error.user.delete.*` |

`password_reset_tokens` là thứ **duy nhất** bị xoá theo người dùng (vé không có giá trị lưu trữ).
Mỗi lần chặn xoá đều gợi ý phương án thay thế ("Ngừng bán" / "Vô hiệu hoá").

---

## Những gì đã verify bằng lệnh thật

**Phase 0–1.** TCP/IP bật, cổng 1433 LISTENING · DB `TourBookingDB` collation `Vietnamese_CI_AI`
+ login `tourapp` · `clean compile` BUILD SUCCESS · seed thật 4 user / 6 danh mục / 20 tour /
60 đợt / 5 đơn / 6 dòng · `sys.check_constraints` = **0**, Function/SP/Trigger = 0 · tiếng Việt lưu
đúng (`Đà Lạt` ký tự đầu U+0110) · mật khẩu BCrypt `$2a$10$…` 60 ký tự · **app khởi động với
`ddl-auto=validate` trên schema viết tay** → entity khớp 1-1 (điểm CLO4/CLO5).

**Phase 2.** 6 route đúng mã · tìm không dấu `da lat`/`DA LAT`/`Đà Lạt` đều ra 1 tour · phân trang
giữ nguyên bộ lọc · giỏ hàng: gộp dòng khi trùng đợt, đặt 0 khách thì dòng biến mất, sống qua nhiều
request, phiên khác thì rỗng · **giỏ không chạm CSDL** (bảng `%cart%` = 0).

**Phase 3.** Phân quyền đúng (302/403) · CSRF bật · **giỏ sống sót qua đăng nhập** (session
fixation đổi mã phiên nhưng giữ attribute) · đặt tour thật: mã `TB…`, tổng khớp tay, `available_seats`
giảm đúng, giỏ về rỗng · thư xác nhận in ra console · kiểm tra quyền sở hữu: khách khác mở đơn → 403
· không trang nào lộ chuỗi `$2a$`.

**Phase 4.** 9 địa chỉ quản trị đều 200 · bốn quy tắc chặn xoá chạy đúng kèm thông báo tiếng Việt ·
upload ảnh + xoá tour thì xoá luôn file · upload `.txt` bị chặn và **không ghi file nào** (giao dịch
roll back) · sửa số khách → thành tiền + tổng + số chỗ luôn khớp · huỷ đơn thì chỗ trả về.

**Phase 5.** `ddl-auto` → `validate` · `clean package` BUILD SUCCESS (jar 65 MB) · REST đủ mã trạng
thái (200/404/400/401/403) · **bốn quy tắc chặn xoá trả 409 kèm câu tiếng Việt** · CSRF vẫn bật cho
`/api/**` · giỏ qua REST vẫn là giỏ Session · biểu đồ doanh thu SVG đọc từ `/api/admin/stats/revenue`
· i18n giữ nguyên query param khi đổi ngôn ngữ, Referer ngoài miền bị bỏ qua · trang lỗi 403/404/500.

**Phase 5.5.** `app.css` 100 → ~1886 dòng, hệ thống thiết kế 12 mục · trang chủ dựng lại 8 khối ·
trang `/about` mới · sửa 6 ảnh danh mục trỏ tới `.jpg` không tồn tại (vẽ 6 SVG) · thẻ tour không cần
ảnh chụp (gradient chọn bằng `Tour.getArtVariant()`) · **đo tương phản WCAG 12 cặp màu → 12/12 đều
≥ 4,5** · khung 375px không tràn ngang.

**Phase 6.** `docs/uml/TourBooking.vpp` mở được bằng Visual Paradigm CE 18, **6 biểu đồ** + 6 ảnh
PNG trong `docs/uml/png/`. Dựng lại bằng một lệnh: `docs/uml/tools/build.sh`.

**Phase 7.** 9 bảng mới, `sys.tables` = 16, check_constraints vẫn 0 · `validate` vẫn qua · bất biến
"đếm hành khách theo loại khớp `num_adults`/`num_children`" → 0 dòng lệch · `DELETE
/api/admin/tours/10` (tour có đánh giá) → **409**, tour sạch → **204**.

**Phase 7.1.** `/api/promotions/check` trả 200 cho mã tốt và **409** cho bốn nhánh sai · đặt đơn
thật bằng `hello2026`: 5.990.000 → giảm 500.000 (10% bị chặn trần) → **5.490.000**, sinh 1 dòng
`coupon_usages`, `used_count` 1→2, gọi lại mã đó bằng chính tài khoản ấy → 409.

---

## ⚠️ Gotchas — 62 cái, phần lớn là lỗi đã thực sự vấp phải

1. **`encrypt=true;trustServerCertificate=true` là bắt buộc** trong JDBC URL. Driver 10.x+ mặc định
   `encrypt=true`, SQL Server local dùng self-signed cert → thiếu là gặp `PKIX path building failed`.
2. **Session fixation** — Spring Security tạo session mới khi login. `changeSessionId` (mặc định)
   **giữ** attribute nên giỏ hàng sống sót; đổi sang `newSession()` là **giỏ biến mất lúc login**.
   Khai báo tường minh + đưa "giỏ sống sót qua login" vào test script.
3. **Hibernate 6.2+ tự sinh `CHECK` cho `@Enumerated(STRING)`** → vi phạm đề bài. Chặn bằng
   `validator.apply_to_ddl: false` và để `@Min/@Max/@Positive` ở DTO chứ không ở entity.
4. **CSRF**: Thymeleaf chỉ tự chèn hidden token khi dùng `th:action="@{...}"`; viết `action="/cart/add"`
   trơn sẽ ra 403 trông y hệt lỗi config. Với AJAX thì **không** disable CSRF cho `/api/**` — nhét
   token vào `<meta>` rồi `$.ajaxSetup` một lần.
5. **`/uploads/**` phải `permitAll()`**, và resource location phải build bằng
   `Paths.get(dir).toUri().toString()` (có dấu `/` cuối) — hard-code `"file:C:\\..."` với backslash
   sẽ âm thầm không resolve trên Windows.
6. ⚠️ **`columnDefinition = "VARCHAR(20)"` KHÔNG chặn được CHECK constraint của enum.** Phải dùng
   `entity/converter/*Converter.java` (`AttributeConverter<E,String>` + `@Converter(autoApply=true)`),
   **bỏ hẳn `@Enumerated`**. *Hệ quả:* **mọi enum thêm mới về sau đều phải có converter riêng.*
7. **Collation `Vietnamese_CI_AI` KHÔNG coi `Đ` là `D`.** `N'à' = N'a'` đúng nhưng `N'Đ' = N'D'` sai
   → gõ `da lat` không tìm ra `Đà Lạt`. Cách xử lý: cột **`tours.search_text`** (đã bỏ dấu, chữ
   thường) dựng bằng Java trong `@PrePersist/@PreUpdate`, xử lý riêng `đ/Đ → d/D` vì `Normalizer`
   không tách được. Từ khoá đầu vào **cũng phải** bỏ dấu + hạ chữ thường trước khi truy vấn.
8. `@Version` chống bán quá chỗ đã thêm ngay ở Phase 1 cho `TourDeparture` — thêm sau sẽ phải sửa
   lại `02_schema.sql` vốn là artifact được chấm.
9. **Bẫy khi viết SQL seed:** `STRING_AGG` không cho biểu thức gộp vừa tham chiếu cột bảng ngoài vừa
   cột bảng trong (**Msg 8124**). Chạy `sqlcmd -i` với file UTF-8 **bắt buộc `-f 65001`**. Và
   **đừng dùng `| tail`** — lỗi nằm giữa output, dùng `| grep -iE "msg|error"`.
10. **`th:each` và `th:replace` KHÔNG được đặt trên cùng một thẻ.** `th:replace` (ưu tiên 100) chạy
    **trước** `th:each` (200) → mảnh giao diện được chèn khi biến lặp chưa tồn tại. Trang vẫn trả
    **200** vì lỗi xảy ra khi phần đầu HTML đã gửi đi — **đừng tin mã 200, phải đếm nội dung**.
11. **`mvn spring-boot:run` KHÔNG nạp lại `src/main/resources`.** DevTools chỉ theo dõi
    `target/classes`. Sửa template / `messages.properties` xong phải `./mvnw -o process-resources`.
12. **`curl -X POST -L` giữ nguyên POST khi đi theo redirect** → POST vào `/cart` trả 405, và
    request 405 đó vẫn "ăn" mất flash attribute. Bỏ `-X POST`, chỉ dùng `-d`.
13. **`MessageFormat` tự chèn dấu phân cách nhóm vào tham số số:** `{0}` với id 99999 hiện ra
    `99,999`. Với mã định danh phải viết `{0,number,#}`.
14. ⚠️ **`columnDefinition="VARCHAR(20)"` + `use_nationalized_character_data` = không đọc được dữ
    liệu.** Hibernate đọc mọi cột chuỗi bằng `getNString()`, driver từ chối đọc cột VARCHAR kiểu đó:
    `The conversion from varchar to NCHAR is unsupported`. Triệu chứng bề ngoài chỉ là "đăng nhập
    luôn sai mật khẩu". **Quy tắc: đừng ép `columnDefinition` kiểu chuỗi ở bất kỳ entity nào**
    (riêng `NVARCHAR(MAX)` cho `tours.description`/`itinerary` thì được, vì vẫn là nationalized).
15. **CSRF thất bại với người CHƯA đăng nhập trả về 302 chứ không phải 403** —
    `ExceptionTranslationFilter` coi đó là "cần đăng nhập". Đừng thấy 302 mà tưởng CSRF chưa bật.
16. **Chú thích HTML thường đi thẳng ra mã nguồn trang.** Một ghi chú viết "sẽ không tìm thấy chuỗi
    `$2a$`" lại chứa đúng chuỗi đó → phép kiểm tra báo nhầm. Ghi chú nội bộ phải dùng chú thích mức
    bộ phân tích của Thymeleaf `<!--/* ... */-->`.
17. ⚠️ **Trên Windows: KHÔNG truyền tiếng Việt qua tham số dòng lệnh.** Git Bash chuyển tham số sang
    code page ANSI trước khi đưa cho tiến trình con → dữ liệu vào CSDL thành `?` hoặc U+FFFD, trông
    y hệt lỗi bảng mã của ứng dụng. Cách đúng: form thường thì ghi body đã percent-encode ra file rồi
    `curl --data-binary @file`; multipart thì `curl -F "field=<file"`.
18. **`#aggregates` của Thymeleaf chỉ có `sum` và `avg`, KHÔNG có `max`/`min`.** Cần giá trị lớn nhất
    để vẽ biểu đồ thì tính sẵn ở tầng service.
19. **Record dùng accessor `amount()` chứ không phải `getAmount()`** → trong khuôn mẫu phải viết
    `${row.amount()}`.
20. ⚠️ **`defaultAuthenticationEntryPointFor` khai báo ĐÚNG MỘT lần thì bộ chọn theo đường dẫn bị bỏ
    qua** (`if (mappings.size() == 1) return ...`). Triệu chứng: gõ địa chỉ không tồn tại thì nhận
    JSON 401 thay vì trang đăng nhập. Sửa: khai thêm bộ thứ hai khớp `AnyRequestMatcher.INSTANCE`.
21. ⚠️ **Trong bộ lọc của Spring Security, `LocaleContextHolder` CHƯA có ngôn ngữ của người dùng** —
    giá trị đó do `DispatcherServlet` đặt. Lớp chạy ở tầng bộ lọc (`ApiErrorWriter`) phải hỏi thẳng
    `LocaleResolver.resolveLocale(request)`.
22. **Đừng dùng chung một DTO cho POST-thêm và PUT-sửa.** `CartItemRequest` có `@AssertTrue` bắt
    "phải có tourId hoặc departureId"; dùng lại cho PUT thì mọi lời gọi sửa đều bị chặn 400.
23. ⚠️ **`spring-boot-devtools` tự đặt `server.error.include-stacktrace=ALWAYS`** → phản hồi lỗi kèm
    nguyên vết lỗi. Phải khai tường minh `server.error.include-stacktrace=never`.
24. **Jackson tuần tự hoá `record` theo đúng thành phần khai báo trong hàm dựng.** Phương thức thêm
    vào như `MonthlyRevenue.label()` sẽ **không** xuất hiện trong JSON.
25. **`#{khoa}` không truyền tham số thì Spring trả nguyên văn chuỗi mẫu** — mẹo để đưa chuỗi mẫu
    xuống cho JavaScript tự thay `{0}`, nhờ vậy không có chuỗi tiếng Việt nào viết cứng trong `.js`.
26. ⚠️ **Kiểm thử trang lỗi bằng curl phải thêm `-H "Accept: text/html"`** — mặc định curl gửi
    `Accept: */*` và `BasicErrorController` trả **JSON** chứ không phải `templates/error/404.html`.
27. **`anyRequest().authenticated()` khiến địa chỉ gõ sai bị đẩy về trang đăng nhập, không ra 404.**
    Hệ quả của thế "mặc định là chặn", giữ có chủ ý.
28. **`~/.m2` thiếu `maven-surefire`** thì `./mvnw -o clean package` thất bại dù có `-DskipTests`.
    Đã chạy một lần **có mạng** để tải về; từ nay `-o` chạy được.
29. ⚠️ **Hiệu ứng "hiện dần khi cuộn" có thể làm mất trắng nội dung.** Đã gặp môi trường mà
    `IntersectionObserver` **tồn tại nhưng không bao giờ bắn sự kiện** → cả trang chủ trắng từ dưới
    hero. Kiểm `'IntersectionObserver' in window` là **không đủ**: phải có hẹn giờ bảo hiểm (2,5s
    hiện hết) + hiện ngay những phần tử đã nằm trong khung nhìn lúc tải.
    *Quy tắc: một hiệu ứng trang trí không bao giờ được phép làm mất nội dung.*
30. **`CSSStyleRule` nay CÓ thuộc tính `.cssRules`** (CSS lồng nhau từ Chrome 112) → đoạn duyệt
    CSSOM kiểu `if (r.cssRules) { đệ_quy(); continue; }` bỏ qua sạch mọi luật thường.
31. **Khung xem của công cụ kiểm thử không dựng hình thì đừng tin số đo giao diện**: `window.scrollY`
    đứng yên ở 0, `IntersectionObserver` im lặng, `getComputedStyle` trả giá trị không khớp luật CSS
    thật.
32. **Đừng tin `getComputedStyle` để đánh giá tương phản chữ trên nền trong suốt** — phần tử có
    `background-color: rgba(0,0,0,0)` khiến phép tính lấy nhầm nền đen (đo 5,08 trong khi thực tế
    3,62). Phải truy ngược lên cha tìm nền đục đầu tiên.
33. ⚠️ **`animation-fill-mode: both` + `opacity: 0` ở khung hình đầu = nội dung có thể mất trắng.**
    Môi trường kiểm thử đăng ký animation nhưng đồng hồ kẹt ở `currentTime = 0`. **Không có cách viết
    CSS thuần nào thoát được** — bỏ fill-mode cũng vô ích vì giá trị animation vẫn đè lên giá trị
    khai báo. Cách duy nhất chắc chắn: **đặt mọi luật animation sau một lớp do JavaScript gắn**
    (dự án dùng `.anim-ready`), để nhánh "không có hiệu ứng" luôn là nhánh nhìn thấy được.
34. **Vệt tô nền chữ kiểu "bút dạ quang" hỏng với tiếng Việt cỡ lớn** — nó cắt ngang thân chữ và dấu
    nặng của "ệ" thò xuống đúng vùng đó. Nhớ kiểm cả với **phông dự phòng**.
35. ⚠️ **Tô nền cả cụm chữ: ba đại lượng RÀNG BUỘC LẪN NHAU.** Ở cỡ 72px với Playfair Display, riêng
    phần chữ kể cả dấu đã cao **106px** trong khi `line-height: 1.18` chỉ cho 93px. **Chiều cao khối
    = chiều cao hộp dòng**, nên nới `line-height` thì khối phình theo — phải dùng `top`/`bottom`
    **DƯƠNG** để co khối vào ôm sát chữ (phản trực giác nhất). Bộ số đang dùng: `line-height: 1.52`,
    `top: 0`, `bottom: .012em`, cỡ chữ trần `4.5rem`. **Đổi một số thì phải đo lại cả bốn chỉ tiêu.**
36. ⚠️ **`d-flex` trên thẻ bọc `.row` của Bootstrap làm sập cả lưới** — `.row` thành flex item nên co
    lại vừa nội dung. Muốn căn giữa dọc thì phải thêm `w-100` cho `.row`. Cách quét cả site: duyệt
    mọi `.row` xem cha có `d-flex` mà bản thân `.row` không có `w-100` không.
37. ⚠️ **Visual Paradigm CE 18 cấm dịch ngược nhưng KHÔNG cấm nhập XML.** `InstantReverse`,
    `ReverseDDL`, `InstantReverseSD` đều trả `Feature not supported by current edition`; nhưng
    `com.vp.cmd.ImportXML`, `ExportXML`, `ExportDiagramImage` **chạy bình thường**. Học định dạng
    bằng cách copy `resources/BaggageSchemas.vpp` ra chỗ khác rồi `ExportXML -upgrade-project`; lược
    đồ đầy đủ ở `bundled/project.xsd`. Chạy bằng `java -cp` trực tiếp, đừng gọi `.bat`.
38. ⚠️ **Trong XML của Visual Paradigm, thiếu `<Caption>` là mất tên hình** (caption cao 0px: nội
    dung bên trong vẫn vẽ nhưng **tên lớp/tên bảng biến mất**, import không báo lỗi). Lớp/bảng/
    lifeline: caption `Center` cao 20 (35 nếu có stereotype). Use case: caption phủ kín e-líp. Tác
    nhân: caption `South`. **Đường nối thì ngược lại** — phải đặt `requestResetCaption = true`.
39. **Quan hệ trên ERD phải là `DBForeignKey`; `Relationship` bị bỏ qua lặng lẽ.** Từ vựng kiểu dữ
    liệu cũng hẹp: nhận `bigint`, `integer`, `varchar(n)`, `numeric(p,s)`, `bit`, `date`,
    `timestamp`, `char(n)`; **không** nhận `nvarchar`, `datetime2`, `boolean`, `text` (bị đổi âm
    thầm thành `integer`).
40. **Tên biểu đồ có dấu tiếng Việt: truyền qua file `-diagram @danhsach.txt` (UTF-8) thì ĐÚNG**,
    truyền thẳng trên dòng lệnh thì hỏng — mặt bên kia của #17. `ExportDiagramImage` **không ghi đè**
    ảnh cũ mà thêm số 2 vào tên file. Hình sát x=0 thì nhãn trái **bị cắt cụt** — chừa lề ≥ 150px.
    `-type png` cho ảnh **nền trong suốt**, dùng `png_with_background`.
41. ⚠️ **SQL Server: UNIQUE trên cột cho phép NULL chỉ chấp nhận ĐÚNG MỘT dòng NULL.** Vấp ở
    `payments.txn_ref`. Chỉ mục **có lọc** là cách chuẩn nhưng đã thử và **bỏ**: bảng mang chỉ mục có
    lọc thì **mọi** INSERT/DELETE đều đòi `QUOTED_IDENTIFIER ON`, mà sqlcmd mặc định OFF → chính
    script seed gãy với `Msg 1934`. Kết luận: ràng buộc duy nhất trên cột nullable nên **kiểm bằng
    Java**.
42. ⚠️ **Thêm bảng mới trỏ tới `users`/`tours` là tự tay tạo lỗ hổng ở quy tắc xoá.** Mỗi bảng mới
    trỏ tới một bảng đang có quy tắc chặn xoá thì **phải bổ sung một phép `countBy...`**, hoặc quyết
    định rõ là cho xoá theo.
43. ⚠️ **Thêm một quan hệ LAZY rồi hiện nó ra khuôn mẫu = trang trả 200 nhưng HTML bị cắt cụt giữa
    chừng** (`open-in-view` tắt có chủ ý). Nhận biết nhanh: `grep -c "</html>"` bằng 0. Sửa bằng
    `LEFT JOIN FETCH` vào chính truy vấn nạp trang đó, **đừng bật lại `open-in-view`**.
44. ⚠️ **Thêm một cột khoá ngoại vào bảng CŨ là đảo luôn thứ tự xoá.** `bookings.promotion_id` biến
    `bookings` thành bảng con của `promotions` → seed và `99_drop_all` phải xoá `bookings` **trước**
    `promotions`, không thì `Msg 547`. Nguy hiểm ở chỗ script vẫn "chạy xong" và dữ liệu cuối trông
    vẫn đúng. Cũng vì bookings tạo trước promotions nên khoá ngoại này khai bằng `ALTER TABLE`.
45. **Visual Paradigm đang mở project thì công cụ dòng lệnh không đọc được `.vpp`**: báo
    `Project file locked` rồi ném `java.awt.HeadlessException` — trông y hệt lỗi thiếu màn hình. File
    khoá là `.TourBooking.vpp.lck`, không xoá được khi giao diện còn mở. `build.sh` nay xuất ảnh từ
    một **bản sao** nên chạy được cả khi đang mở project.

46. ⚠️ **Phần tử `z-index` âm biến mất nếu khối cha không phải một ngữ cảnh xếp lớp.** Khối tô nền
    của `.mark-stroke` nằm ở lớp giả với `z-index: -2`; bê tiêu đề từ nền `.bg-jade` (vốn có sẵn
    `isolation: isolate`) sang hero nền sáng là cả khối cam tụt xuống dưới nền của section và mất
    sạch, chỉ còn chữ kem nằm trơ trên nền cát. Sửa: `isolation: isolate` cho khối bọc.
47. ⚠️ **Đặt `--bs-btn-padding-x` ở lớp `.btn` là giết luôn `.btn-sm`.** Bootstrap khai lại biến ấy
    trong `.btn-sm`, hai lớp cùng độ đặc hiệu (0,1,0) mà `app.css` nạp sau → "nút nhỏ" rộng y hệt
    nút thường. Hậu quả đo được: bốn nút trên thanh điều hướng cộng lại 371px trong chỗ 353px và cả
    thanh xuống hai dòng. Đặt biến kích thước ở `.btn` thì **phải khai lại `.btn-sm`/`.btn-lg`**.
48. **`display: grid` + `place-items: center` trên một nút `dropdown-toggle` đẩy mũi tên xuống dòng
    dưới** — mũi tên `::after` của Bootstrap là phần tử con thứ hai, nên thành hàng thứ hai của
    lưới. Nút chọn ngôn ngữ cao gấp đôi vì lý do này. Dùng `inline-flex`.
49. ⚠️ **Dải chạy ngang nhân bản nội dung thì `:nth-child(even)` đảo chẵn lẻ đúng chỗ nối.** Một
    lượt có 7 mục (số lẻ) nên sang lượt sau mục đặc thành mục rỗng, vòng lặp đang liền mạch bỗng
    nhấp nháy. Kiểu dáng xen kẽ phải gắn bằng **lớp do máy chủ đặt**, bản sao mang theo.
50. ⚠️ **Hai `animation-name` trên cùng một phần tử mà cùng ghi `transform` thì cái sau đè cái
    trước.** Hiệu ứng vào (`transform: translateY`) và hiệu ứng trôi theo cuộn không sống chung
    được. Cách thoát: cho một trong hai dùng thuộc tính riêng `translate:` thay vì `transform:`.
51. ⚠️ **Nền đặt trên cột `col-lg-2` của Bootstrap KHÔNG cao bằng khung nhìn.** Cột chỉ cao bằng
    nội dung, nên menu quản trị ngắn hơn phần nội dung là lộ ra một mảng trắng cụt lửng. Nền phải
    nằm trên chính phần tử `sticky`, và `top` của nó phải khớp chiều cao THẬT của thanh trên (viết
    cứng `56px` trong khi thanh cao 72px thì menu chui xuống dưới thanh lúc cuộn). Bề rộng menu là
    hằng số giao diện → khai bằng px trong CSS Grid, đừng dùng tỉ lệ cột của lưới.
52. ⚠️ **Một luật media query đặt TRƯỚC luật thường cùng độ đặc hiệu thì thua.** Luật tắt vòng xoay
    cho điện thoại (`@media (max-width: 575.98px) { .anim-ready .home-slot { animation: none } }`)
    nằm ở mục 14.3, còn luật bật vòng xoay nằm ở mục 14.9 phía sau - cùng (0,2,0) nên luật sau
    thắng và **trên điện thoại vòng xoay vẫn chạy**. Media query KHÔNG cộng thêm độ đặc hiệu nào.
    Quy tắc: luật "tắt theo bề rộng" phải đặt **ngay sau** luật bật nó.
53. ⚠️ **`.row.g-5` nằm thẳng trong `.container` thì tràn ngang 12px mỗi bên.** `g-5` đặt
    `--bs-gutter-x: 3rem` cho riêng `.row`, nên lề âm của nó là -24px trong khi `.container` chỉ có
    đệm 12px. Trên dải bề rộng mà container còn là 100% (dưới 576px) thì phần dư đẩy
    `scrollWidth` vượt `clientWidth` và cả trang cuộn ngang được. Dùng `g-4 g-lg-5`: dưới `lg` thì
    gutter khớp đúng đệm của container.
54. **Bề rộng `.container` của Bootstrap ĐỨNG YÊN ở 1140px suốt dải 1200-1399px.** Thanh điều
    hướng vừa khít ở 1400px không có nghĩa là nó vừa ở 1280px - chỗ chứa y hệt nhau mà màn hình
    rộng hơn chỉ làm lề hai bên dày thêm. Đo ở đúng 1200px chứ đừng đo ở 1440px rồi suy ra.
55. ⚠️ **Thymeleaf coi `__...__` là cú pháp TIỀN XỬ LÝ, nên hai tên lớp kiểu BEM trong cùng một
    biểu thức là gãy trang.** `th:classappend="${s.even} ? 'home-reel__item--left' :
    'home-reel__item--right'"` có đúng HAI cụm `__` khép thành một cặp; Thymeleaf lôi
    `item--left' : 'home-reel` ra phân tích như một biểu thức và ném lỗi giữa chừng. Trang vẫn trả
    **200** nhưng HTML cụt ngay tại đó (lại đúng bẫy #10: đừng tin mã 200). MỘT cụm `__` lẻ thì vô
    hại vì không có cụm đóng - đó là lý do `'marquee__item--ghost'` đứng một mình vẫn chạy ngon,
    và cũng là lý do lỗi này rất khó đoán. Cách xử lý: tên lớp trong biểu thức đừng dùng hai gạch
    dưới (dự án chuyển sang `is-left` / `is-right`, hợp với `is-visible`/`is-scrolled` sẵn có).
56. ⚠️ **`isolation: isolate` ở một khối bên dưới sẽ vẽ ĐÈ lên menu xổ xuống của thanh điều
    hướng.** `.bg-jade` cần `isolation` để giữ khối tô nền `z-index` âm của nó (#46), nhưng
    isolation tạo ra một ngữ cảnh xếp lớp, mà ngữ cảnh như vậy được vẽ như thể `z-index: 0` -
    cùng tầng với thanh điều hướng (`z-index: auto`) nhưng đứng SAU trong cây HTML nên thắng.
    Triệu chứng: bấm "Danh mục" ở trang trong thì menu bị dải tiêu đề cắt cụt ngay mép trên của
    nó, còn ở trang chủ thì không (thanh ở đó đã có sẵn `z-index: 1030` vì nằm đè lên hero).
    Sửa: cho `.site-nav` `position: relative` + `z-index: 1030`. *Bài học chung: thêm `isolation`
    hay bất cứ thứ gì tạo ngữ cảnh xếp lớp vào một khối là phải rà lại mọi thứ nổi lên trên nó.*
57. ⚠️ **`th:if`/`th:unless` cùng thẻ với `th:replace`/`th:insert` thì điều kiện bị BỎ QUA - khối
    luôn hiện ra bất kể đúng sai.** Cùng họ với gotcha #10 (`th:each`+`th:replace`), vì lý do giống
    hệt nhau: theo bảng độ ưu tiên của Thymeleaf, `th:insert`/`th:replace` (ưu tiên cao nhất, chạy
    ĐẦU tiên) luôn chạy trước `th:if`/`th:unless` (nhóm "conditional evaluation", chạy sau fragment
    inclusion) dù viết chung hay tách dòng trên cùng một thẻ. Vấp thật ở `fragments/components.html
    :: tourCard` và `index.html` (băng ảnh hero): `<div th:unless="${tour.thumbnail != null}"
    th:replace="~{... :: tourArt(...)}">` - suốt từ lúc viết (20 tour đều chưa có ảnh, `thumbnail`
    luôn null) hai nhánh "vô tình" cho cùng một kết quả nên không ai phát hiện ra. Lỗi chỉ lộ diện
    ở Phase 7.4 khi 20 tour đổi sang **có** ảnh thật: khối `tourArt` (nền gradient + tên điểm đến)
    vẫn cứ vẽ **đè lên** ảnh thật vì `th:unless` bị bỏ qua. Trang vẫn trả 200, `</html>` vẫn đủ - im
    lặng hoàn toàn, chỉ thấy được bằng mắt hoặc bằng cách đọc HTML trả về tìm chuỗi `tour-art art-`.
    Sửa: tách `th:if`/`th:unless` sang một `th:block` bọc ngoài, để thẻ mang `th:replace` đứng
    riêng: `<th:block th:unless="..."><div th:replace="..."></div></th:block>`. *Bài học chung: bất
    cứ khi nào 20 tour "chưa có X" đổi thành "đã có X", phải rà lại toàn bộ nhánh `th:if X == null`
    từng được viết chung thẻ với `th:replace`/`th:insert` - im lặng suốt thời gian X còn null.*
58. ⚠️ **Cộng thêm `Sort` vào `Pageable` cho một phương thức repository đã tự sắp xếp theo TÊN
    (`...OrderByCreatedAtDesc`) sinh ra `ORDER BY` trùng cột - SQL Server từ chối thẳng.** Vấp ở
    `ReviewService.adminList` (UC020): gọi `PageRequest.of(page, size, Sort.by(DESC, "createdAt"))`
    rồi truyền cho `findByApprovedFalseOrderByCreatedAtDesc(pageable)` - Spring Data nối sắp xếp suy
    ra từ tên phương thức với `Sort` mang trong `Pageable`, ra `ORDER BY r1_0.created_at desc,
    r1_0.created_at desc`. SQL Server: `Msg` *"A column has been specified more than once in the
    order by list"* → **500**, không phải lỗi âm thầm. Nhiều hệ quản trị khác (H2, MySQL) im lặng bỏ
    qua cột trùng nên lỗi này **sẽ không lộ ra** nếu viết unit test bằng H2 trong bộ nhớ. Sửa:
    phương thức đã có `OrderBy...` trong tên thì `PageRequest.of(page, size)` **không kèm `Sort`**.
59. ⚠️ **`@Min`/`@Max`... trên `@RequestParam` KHÔNG làm gì cả nếu lớp controller thiếu `@Validated`
    ở mức lớp.** Vấp ở `AdminBookingController.updateDetail`: thêm `@Min(0)` cho `numChildren` tưởng
    là xong, gửi `-10` vẫn lọt thẳng vào thân phương thức không một tiếng động - trông y hệt như chưa
    hề thêm annotation. Phải thêm `@Validated` (`org.springframework.validation.annotation.Validated`,
    **không phải** `jakarta.validation.Valid`) ngay trên khai báo `class`. Thêm vào rồi thì lại vấp bẫy
    thứ hai: vi phạm ném ra `ConstraintViolationException`, một loại `GlobalExceptionHandler` **cũ chưa
    hề bắt** (nó chỉ bắt `BusinessRuleException`, cố ý không bắt `Exception` chung) → chặn được nhưng
    biến 302-kèm-flash-lỗi thành trang 500 thô. Phải thêm hẳn một `@ExceptionHandler(ConstraintViolationException.class)`
    riêng, cùng khuôn xử lý (flash + redirect) với handler cũ.
60. ⚠️ **Khoá bi quan (`@Lock(PESSIMISTIC_WRITE)`) chặn đúng ở tầng CSDL nhưng vẫn lost update ở tầng
    Java nếu entity đã nằm sẵn trong persistence context từ TRƯỚC khi xin khoá.** Vấp ở
    `PromotionService.recordUsage` lúc verify bằng hai request checkout bắn thật gần như đồng thời
    (`curl ... & curl ... & wait`, không phải suy luận): `promotion` truyền vào `recordUsage` đã được
    nạp trước đó trong CÙNG giao dịch, ở `PromotionService.check` (gọi từ đầu
    `BookingService.placeOrder`) - nên khi giao dịch B bị khoá bi quan chặn lại, đợi giao dịch A commit
    xong rồi mới được chạy tiếp, câu `SELECT ... FOR UPDATE` của B **có** chạy thật và **có** đọc đúng
    `used_count` mới nhất từ CSDL, nhưng Hibernate trả về đúng **đối tượng Java đã có sẵn trong session
    của B** (theo id, kiểu identity-map) thay vì ghi giá trị cột mới vào nó - B vẫn nhìn thấy
    `used_count` CŨ như lúc `check()` chạy. Đo được bằng dữ liệu thật: mã `usageLimit=1`, hai tài khoản
    đặt gần như cùng lúc → **cả hai** tạo đơn thành công, **cả hai** được giảm giá, nhưng `used_count`
    cuối cùng chỉ lên 1 (không phải 2) - bị B tính lại từ baseline cũ (0+1) rồi ghi đè đúng lên giá trị
    A vừa commit, trông y hệt lost update ban đầu dù khoá đã đúng. Sửa: `entityManager.refresh(locked)`
    ngay sau khi giữ được khoá, bắt buộc nạp lại state mới nhất từ CSDL đè lên state cũ trong session.
    *Bài học chung: xin khoá bi quan cho một entity đã có khả năng được nạp từ trước trong CÙNG giao
    dịch (qua một service method khác) thì `@Lock` không đủ - phải `refresh()` tường minh.*
61. **Log console nuốt dấu tiếng Việt dù file `messages*.properties` đọc đúng UTF-8** - xem chi tiết ở
    Phase 9 (Logback ghi console theo bảng mã hệ điều hành; sửa bằng `logging.charset.console=UTF-8`).
62. ⚠️ **`echo "$BODY" > file` rồi `curl --data-binary @file` để kiểm CSRF/POST thật: `echo` luôn thêm
    một dòng mới ở cuối file** (cùng họ với bẫy heredoc đã ghi ở Phase 9 mục 8, nhưng lần này rơi vào
    chính token CSRF thay vì mật khẩu). Trường cuối cùng của payload URL-encoded thường là
    `_csrf=<token>`, nên byte `\n` thừa biến giá trị gửi lên thành `"<token>\n"` - không khớp giá trị
    thật trong session, `CsrfFilter` từ chối với **403 "Forbidden"** trông y hệt lỗi phân quyền
    (`AccessDeniedHandler` xử lý cả hai loại như nhau, xem gotcha #26 về việc `curl` cần
    `-H "Accept: text/html"` mới thấy đúng trang báo lỗi thay vì JSON của `BasicErrorController`).
    Vấp thật khi verify 1.3 (Phase 10): request tự thân đã đúng logic nhưng cứ 403 dù CSRF token vừa
    lấy từ đúng trang vừa fetch; nghi ngờ sai hướng (tưởng đâu đó chặn quyền quản trị) mãi tới khi thử
    một request KHÁC (toggle-enabled, payload ngắn hơn) bằng `curl --data-urlencode` (không qua file)
    mới thành công, so sánh hai cách mới lộ ra `echo` là thủ phạm. **Quy tắc**: kiểm CSRF/POST thật thì
    dùng `printf` (không tự thêm `\n`) khi phải ghi payload ra file, hoặc `--data-urlencode` thẳng trên
    dòng lệnh khi payload không chứa tiếng Việt (gotcha #17 vẫn cấm tiếng Việt qua tham số dòng lệnh).

**Phase 7.2 — mềm & mượt.** `app.css` 1886 → **2139 dòng**. Token: bo góc nới một nấc (lg 22→26,
xl 32→36, thêm 2xl), **đổ bóng chuyển sang hai lớp** (tiếp xúc + môi trường), thêm
`--shadow-inset-top`, `--hairline`, `--ease-soft`. **Mục 13 mới** gom toàn bộ lớp mềm/mượt vào một
chỗ (gỡ cả mục là trang về nguyên trạng): bề mặt trình duyệt (caret, thanh cuộn WebKit,
`:focus-visible`, gạch chân hạ thấp, chữ số tabular), vật liệu khối (viền pha ngọc bích + vệt sáng
1px mép trên), và chuyển động (thẻ nâng 8px với đường cong mềm, nút lún khi bấm, reveal đi ít mà lâu).
*Verify:* 22 khối `reveal` → **0 khối còn ẩn** sau khi cuộn (phép kiểm sống còn của #29/#33) ·
0 lỗi console · viền ô nhập **3,10:1** trên nền trắng, chữ mô tả tour **6,30:1** · xem thật ở
1440px, 1366px và 375px.

**Phase 7.3 — trang chủ dựng lại & khu quản trị.** `app.css` 2139 → **3337 dòng** (thêm mục 14,
viết lại mục 11), `index.html` 348 → **447 dòng**, `app.js` 574 → **752 dòng**, câu chữ 538 → **540
khoá**.

*Trang chủ.* Cả trang đổi sang **một tông sáng** (nền cát), chỉ còn hai khối tối đặt xa nhau. Hero
chia đôi: trái là tiêu đề + ô tìm kiếm kiểu thanh lệnh + hai nút, phải là **ba thẻ tour thật**
nghiêng chồng lên nhau, trôi theo con trỏ (biến CSS + transition) và trôi theo cuộn
(`animation-timeline: view()`, **không một sự kiện scroll nào**). Thêm dải số liệu tự đếm lên, hai
dải địa danh chạy ngược chiều, bốn bước xếp cột dọc có thanh tiến trình chạy theo cuộn, lưới cảm
nhận lệch. Bỏ hết nhãn nhỏ in hoa và gợi ý cuộn; ba nút cùng dẫn về `/tours` nay dùng **một** câu.
Thanh điều hướng của trang chủ thành viên thuốc nổi chữ mực (`.site-nav--float`).

*Verify:* 17 khối `reveal` → **0 khối còn ẩn** · 0 lỗi console · **không tràn ngang** ở 1440 / 1024
/ 375px · thanh điều hướng **60px một dòng** (trước đó 105px hai dòng ở 1024px) · nút kêu gọi nằm ở
675px trên khung cao 860px, tức trên nếp gấp · bốn con số chạy đúng về 20 / 20 / 60 / 6 · dải chạy
ngang rộng 2719px trên khung 1024px nên vòng lặp không hở · bản tiếng Anh hiện đủ câu mới.

*Vòng sửa thứ hai (theo phản hồi của user, kèm hai video mẫu).*
1. **Băng ảnh ở hero trôi từ TRÊN XUỐNG DƯỚI không ngừng** (đúng kiểu `mau.mp4`): một cột ảnh
   so le trái phải, nghiêng nhẹ, hai mép trên dưới mờ dần bằng `mask-image`. Bao nhiêu tour nổi
   bật thì bấy nhiêu ảnh, app.js nhân bản cho đủ dài rồi nhân đôi để vòng lặp khép kín.
   Khối mang `aria-hidden` và **không có liên kết nào**: đây là trang trí, đúng những tour ấy đã
   có đủ ở khối "Tour nổi bật" kèm giá và nút đặt. Bản phác trước đó (ba tấm thẻ xoay vòng
   qua ba thế đứng) đã bỏ.
2. **Thanh nối ở khối "bốn bước" không còn gạch ngang con số**: vòng tròn số vốn trong suốt 92%
   nên sợi dây dọc hiện xuyên qua. Nay vòng tròn có nền đục (#51 → xem thêm #50).
3. **Thanh điều hướng nhất quán ở mọi trang**: `.site-nav--float` gắn cứng trong `layout.html`
   thay vì chỉ trang chủ, nên bấm từ trang chủ sang trang bất kỳ không còn cảnh cả thanh đổi từ
   sáng sang ngọc bích đậm. Tham số `overlay` nay chỉ còn quyết định "đè lên nội dung hay không".
   Đệm trên của `.page-head` hạ từ 6-8rem xuống 3-4,5rem vì không còn phải chừa chỗ cho thanh đè.

*Verify vòng hai:* không tràn ngang ở 541 / 990 / 1270 / 1490px · thanh điều hướng **một dòng
59-60px** ở cả bốn bề rộng (trước đó 98px hai dòng ở 1280px) · băng ảnh: 6 ảnh → 12 sau khi
nhân bản, **nửa chiều cao băng trừ một lượt = 0px** (đo bằng `offsetTop`, tức vòng lặp không
có mối nối) · 17 khối `reveal` → **0 khối còn ẩn** · 0 lỗi console.

*Khu quản trị.* Bốn lỗi bố cục ở #51 đã sửa: bỏ lưới Bootstrap, thay bằng CSS Grid cột trái cố định
264px; nền và đường kẻ chuyển lên chính phần tử `sticky`; `--admin-topbar-h` khai một chỗ cho cả
thanh trên lẫn menu; phần nội dung có trần 1400px. Dưới 992px menu dọc đổi thành dải cuộn ngang.
Biểu đồ doanh thu **thôi dùng ba mã màu Bootstrap viết cứng** (`#198754`, `#dee2e6`, `#6c757d`) mà
đọc token từ `app.css`, thêm đường kẻ ngang và nhãn số tiền; ít tháng thì gom cụm vào giữa thay vì
để hai cột đứng cách nhau nửa màn hình. Gỡ 16 chỗ `shadow-sm` (bóng xám `!important` của Bootstrap
đè lên bóng ngọc bích).

**Phase 7.4 — UC019 (mã khuyến mãi), UC018 (đánh giá tour), danh sách hành khách, 20 ảnh tour thật.**

*B. `/admin/promotions`.* `AdminPromotionController` + `PromotionService` (thêm CRUD/xoá/bật-tắt
vào service sẵn có của UC017) + `admin/promotion/{list,form,usages}.html`, theo đúng khuôn category.
Chặn xoá mã đã có lượt dùng ở tầng Java (`couponUsageRepository.countByPromotionId`), gợi ý "Vô hiệu
hoá mã" - cùng khuôn bốn quy tắc chặn xoá cũ. Màn `usages` xem được ai dùng/đơn nào/giảm bao nhiêu
(`CouponUsageRepository.findByPromotionIdOrderByUsedAtDesc` nạp sẵn `user`+`booking` bằng
`@EntityGraph`).

*C. Đánh giá tour + danh sách hành khách.* `ReviewService` mới: `canReview` đòi có đơn `COMPLETED`
chứa tour này (`BookingDetailRepository.existsCompletedBookingForTour`) và chưa từng đánh giá
(`existsByUserIdAndTourId`, kiểm bằng Java chứ không dùng chỉ mục duy nhất lọc - đúng gotcha #41).
Gửi đánh giá qua `POST /account/reviews` (đặt dưới `/account/**` đã `authenticated()` sẵn, rẻ hơn
sửa `SecurityConfig` vì `/tours/**` đang `permitAll()` không phân biệt method). Trang chi tiết tour
hiện điểm trung bình + danh sách đánh giá (nạp riêng, không `JOIN FETCH` chung với ảnh - tránh tích
Descartes, giống cách `departures` đã tách trước đó). Danh sách hành khách nối vào cả
`/account/bookings/{code}` và `/admin/bookings/{code}` qua `BookingPassengerRepository.findByBookingId`
(đã sửa thành `JOIN FETCH p.detail`, xem gotcha #43 áp dụng lần nữa).

*A. 20 ảnh tour thật.* Tải từ **Wikimedia Commons** (script Python dùng `urllib` + API tìm kiếm,
lọc giấy phép CC0/Public domain/CC BY/CC BY-SA, resize 1200px + nén JPEG chất lượng ~80 bằng
Pillow) - không dùng Unsplash/Pexels vì API "source.unsplash.com" cũ đã ngừng và cả hai đều cần khoá
API để tìm theo từ khoá. `static/images/tours/NGUON_ANH.md` liệt kê đủ 20 dòng nguồn/tác giả/giấy
phép. Tổng thư mục **2,96 MB**, file lớn nhất **228 KB** (dưới trần 250 KB). `database/03_seed_data.sql`
thêm khối `UPDATE tours SET thumbnail = CASE id ...` trỏ tới `/images/tours/...`; `DataSeeder.java`
thêm `TOUR_THUMBNAILS` cùng thứ tự với `TOUR_SEEDS` cho môi trường dựng từ số không không qua
`sqlcmd`. Ảnh dùng đường dẫn **tuyệt đối bắt đầu bằng `/`** để phân biệt với ảnh quản trị viên tải
lên (`uploads/tours/…`, tương đối) - cùng quy ước đã có sẵn ở `TourCategory.imageUrl`; năm khuôn mẫu
hiện ảnh tour (`fragments/components.html::tourCard`, `index.html` băng ảnh hero, `tour/detail.html`
ảnh đại diện, `admin/tour/list.html`, `cart/cart.html`) đều phải sửa vì trước đó chỉ biết ghép cứng
`/uploads/` + đường dẫn.

*Verify bằng lệnh thật:* app khởi động với `ddl-auto=validate` sau mọi thay đổi entity/repository ·
`grep -c "</html>"` = 1 cho cả 8 trang mới/sửa (`/`, `/tours`, `/tours/1`, `/tours/6`,
`/admin/promotions`, `/admin/promotions/form`, `/admin/bookings/{code}`, `/account/bookings/{code}`)
· `DELETE /api/admin/promotions/1` (mã `HELLO2026`, đã có 1 lượt dùng) → **409** kèm
`error.promotion.delete.hasUsages` và câu gợi ý "Vô hiệu hoá mã"; cùng mã sau khi xoá hết lượt dùng
(mã test tạo riêng) → **204** · ba file câu chữ **626 khoá mỗi file**, 0 trùng, 0 lệch · 0 lỗi console
ở `/admin/promotions`, `/tours/6`, `/admin/bookings/{code}` (tab trình duyệt sạch, không tính một sự
kiện `/logout?continue` 404 do trình duyệt tự dò tìm sau khi đăng xuất, không phải lỗi ứng dụng) ·
không tràn ngang ở 375 / 1024 / 1440px trên cả hai trang công khai lẫn `/admin/promotions` · 20 ảnh
tour hiển thị đúng ở trang chủ, `/tours`, chi tiết tour, giỏ hàng và `/admin/tours`.

*Còn dở, nói thẳng (đã hết ở Phase 7.5):* biểu đồ doanh thu trong ảnh chụp `/admin` không vẽ được
cột (khối JS đọc `/api/admin/stats/revenue` bằng AJAX, nhưng ảnh chụp lấy từ một bản HTML tĩnh đã
ghi lại nên phiên trình duyệt chụp ảnh không có cookie đăng nhập để gọi API đó) - chỉ ảnh hưởng ảnh
chụp báo cáo, trang thật khi mở bằng trình duyệt có đăng nhập vẫn vẽ biểu đồ bình thường (đã kiểm ở
Phase 7.3). ~~Đánh giá gửi lên được duyệt ngay~~ - **đã đảo lại đúng thiết kế gốc ở Phase 7.5 bên
dưới**, xem UC020.

**Phase 7.5 — UC020: kiểm duyệt đánh giá.** User yêu cầu bổ sung sau khi thấy Phase 7.4 cho đánh giá
hiện ngay. Hoá ra **không cần đổi gì dưới CSDL**: `Review.approved` (mặc định `false` trong entity),
`admin_reply`, `replied_at` đã có sẵn từ Phase 1, y hệt tinh thần "thiết kế trước, dùng sau" của
`TourDeparture`. Bám đúng thiết kế chốt sẵn ở `docs/report/SPEC_CHUNG.md` mục 8 (agent điều phối
viết trước để hai agent UI/docs không lệch nhau).

- `ReviewService.submit()` đảo `approved = true` → **`false`**. Không cần sửa hai truy vấn lọc công
  khai (`findByTourIdAndApprovedTrueOrderByCreatedAtDesc`, `averageRatingByTourId`) - **đã lọc
  `approved = true` sẵn từ Phase 7.4**, hoá ra làm đúng ngay từ đầu dù lúc đó chưa có màn kiểm duyệt.
- `/admin/reviews`: `AdminReviewController` + ba truy vấn mới trong `ReviewRepository` (Chờ duyệt /
  Đã duyệt / Tất cả, đều `@EntityGraph({"tour","user"})` - gotcha #43 áp dụng lần nữa). Bốn thao
  tác: Duyệt · Bỏ duyệt · Trả lời (dùng sẵn `Review.reply(String)`) · Xoá. Trả lời gấp-mở ngay trong
  dòng bằng Bootstrap `collapse`, không cần trang riêng.
- `AdminModelAdvice` mới (`@ControllerAdvice` scope `controller.admin`, cùng khuôn
  `GlobalModelAdvice` bên `controller.web`) bơm `pendingReviewCount` cho mọi trang quản trị - huy
  hiệu đỏ trên mục "Kiểm duyệt đánh giá" ở sidebar.
- Trang chi tiết tour: khách thấy đánh giá của chính mình (`TourController` nạp thêm `ownReview` qua
  `ReviewService.findOwnReview`) kèm nhãn **"Chờ duyệt"**/**"Đã duyệt"**, kể cả khi chưa duyệt nên
  chưa nằm trong danh sách công khai.
- Vấp gotcha #58 (`Sort` trùng với `OrderBy...` trong tên phương thức → SQL Server 500) ngay lần
  chạy thử đầu tiên - đã sửa, xem chi tiết ở mục gotcha.

*Verify bằng lệnh thật:* app khởi động sạch với `ddl-auto: validate`, **không đổi `02_schema.sql`**
(đúng như SPEC_CHUNG.md mục 8 dự đoán) · `grep -c "</html>"` = 1 cho `/admin/reviews` (cả ba bộ lọc)
và `/tours/{id}` · **kịch bản thật**: đánh giá có sẵn trong seed (Nguyễn Văn An, tour Huế, 3 sao,
đang `approved=0`) → **không** hiện ở `/tours/10` (đếm chuỗi nội dung = 0 lần, trang hiện "Chưa có
đánh giá") → bấm "Duyệt" ở `/admin/reviews` → hiện ra ngay, điểm trung bình đổi từ "Chưa có đánh giá"
thành **3,0** · bấm "Bỏ duyệt" → biến mất khỏi `/tours/10` trở lại, huy hiệu chờ duyệt tăng lại 1 ·
"Trả lời" lưu đúng nội dung, hiện lại đúng nguyên văn ở lần tải sau · "Xoá" xoá thật, huy hiệu giảm
theo · dựng một kịch bản đủ điều kiện đánh giá mới (tạm chuyển 1 đơn `PENDING` sang `COMPLETED`,
đăng nhập đúng khách đó, gửi đánh giá thật qua `POST /account/reviews`) → khách thấy ngay đánh giá
của mình kèm nhãn **"Chờ duyệt"** trên `/tours/{id}`, khách vãng lai xem cùng trang thì **không** thấy
gì (đếm chuỗi = 0) · dọn sạch: xoá đánh giá test, trả đơn về `PENDING`, cân bằng lại `approved` của
đánh giá seed đúng như lúc bắt đầu · ba file câu chữ **650 khoá mỗi file**, 0 trùng, 0 lệch · không
tràn ngang ở 375/1024/1440px trên `/admin/reviews` và `/tours/{id}` · 0 lỗi console.

**Phase 7.6 — UC021/UC022 + hai bổ sung: vá "ba bảng chết" (`contact_messages`,
`payments`, `booking_status_history`).** User duyệt vá cả ba theo đúng thiết kế chốt sẵn ở
`docs/report/SPEC_CHUNG.md` mục 9. Ba bảng này có tầng dữ liệu từ Phase 7 nhưng **chưa từng được ghi
vào** - chỉ tồn tại trong các phép `countBy...` của quy tắc chặn xoá, nên bốn quy tắc liên quan
(`error.tour.delete.hasContactMessages`, `error.user.delete.hasBookingHistory`,
`error.user.delete.hasContactMessages`) trước đây chỉ kích hoạt được trên dữ liệu seed. Không đổi
`02_schema.sql` - đúng dự đoán, ba enum liên quan (`ContactStatus`, `PaymentMethod`, `PaymentStatus`)
đã có `AttributeConverter` sẵn từ Phase 7.

- **UC021 `/contact`** (AC01, không cần đăng nhập - thêm vào danh sách `permitAll` của
  `SecurityConfig`): `ContactController` + `ContactService` + `contact.html`. `tourId` tuỳ chọn, điền
  sẵn khi mở từ nút "Liên hệ về tour này" mới thêm ở `tour/detail.html`.
- **UC022 `/admin/contacts`**: `AdminContactController`, danh sách gấp-mở tại dòng (cùng khuôn UC020)
  lọc theo `ContactStatus` (mặc định `NEW`, tham số `status=ALL` nghĩa là không lọc - tách khỏi enum
  vì enum không có giá trị "tất cả"). `handledBy`/`handledAt` chỉ đặt khi chuyển sang
  `RESOLVED`/`SPAM` - đúng yêu cầu, hai trạng thái đó coi như "xong việc", còn `IN_PROGRESS` là đang
  dở. `AdminModelAdvice` thêm `newContactCount` cho huy hiệu sidebar, cùng cơ chế
  `pendingReviewCount` đã có.
- **Bổ sung A `BookingStatusHistory`**: `BookingService.placeOrder` ghi dòng đầu tiên
  (`fromStatus=null, toStatus=PENDING, changedBy=<khách đặt>`) ngay trong giao dịch tạo đơn.
  `BookingService.updateStatus` đổi chữ ký thêm `changedByUserId`/`reason`, dùng
  `userRepository.getReferenceById` (chỉ cần một proxy đủ gắn khoá ngoại, không cần nạp cả bản ghi
  User) và ghi thêm một dòng mỗi lần đổi. `admin/booking/detail.html` thêm ô nhập lý do (tuỳ chọn)
  cạnh nút đổi trạng thái. Dòng thời gian hiện ở cả `/account/bookings/{code}` và
  `/admin/bookings/{code}`.
- **Bổ sung B `Payment`**: `PaymentService` mới. `createPendingForBooking` gọi ngay sau khi tạo đơn
  (cùng giao dịch) - `amount` lấy từ `booking.totalAmount` (đã trừ giảm giá), `status=PENDING`.
  **Không đổi `CheckoutForm`/`checkout.html`**: `booking.paymentMethod` vẫn lưu nguyên câu dịch hiển
  thị (vd. "Chuyển khoản ngân hàng") như thiết kế cũ, `PaymentService.resolveMethod` so khớp câu đó
  với `messages.get("checkout.payment.bank/cash/momo")` theo đúng ngôn ngữ của request để suy ra
  enum `PaymentMethod` - tránh phải sửa hai chỗ đang hiển thị `booking.paymentMethod` như văn bản
  thô (`account/booking-detail.html`, `admin/booking/detail.html`). `markPaid` kiểm trùng `txnRef`
  **bằng Java** (không có ràng buộc UNIQUE dưới CSDL - đúng lý do đã ghi ở gotcha #41 và Javadoc của
  `Payment.txnRef`).
- Vấp lại **gotcha #43** một lần nữa, lần này ở `BookingStatusHistoryRepository.findByBookingCode`:
  câu truy vấn cũ không `JOIN FETCH changedBy`, mà `admin/booking/detail.html` mới thêm hiện
  `h.changedBy.fullName` trên dòng thời gian. Trang trả **200** nhưng `LazyInitializationException`
  cắt cụt HTML ngay tại đó - chỉ lộ ra khi mở trang bằng một quản trị viên **khác** người vừa đổi
  trạng thái đơn (proxy `User` của `changedBy` lúc đó thật sự cần nạp, không như trường hợp trùng
  người ở phiên đang mở). Sửa bằng `LEFT JOIN FETCH h.changedBy` ngay trong câu truy vấn.

*Verify bằng lệnh thật:* `./mvnw -o clean compile` sạch, app khởi động lại nhiều lần qua toàn bộ quá
trình sửa, luôn qua `ddl-auto: validate`, **`02_schema.sql` không đổi một dòng** · `grep -c "</html>"`
= 1 trên `/contact`, `/admin/contacts?status=NEW`, `/admin/contacts?status=ALL`,
`/admin/bookings/{code}`, `/account/bookings/{code}` · **kịch bản thật**: gửi liên hệ qua `/contact`
(không tourId) → hiện ở `/admin/contacts?status=NEW` ngay · gửi liên hệ thứ hai kèm `tourId=3` (Côn
Đảo, tour "sạch" không booking/review) → `DELETE /api/admin/tours/3` → **409**
`error.tour.delete.hasContactMessages`: *"Không xoá được: còn 1 liên hệ đang hỏi về tour này."* ·
chuyển liên hệ đầu sang `RESOLVED` kèm ghi chú → `handled_by`/`handled_at` được ghi, hiện đúng
"Quản trị hệ thống", huy hiệu `NEW` giảm 1 · đặt một đơn thật qua checkout (tour Hà Giang, tài khoản
`an.nguyen@gmail.com`) → sinh đúng 1 dòng `payments` (`PENDING`, `BANK_TRANSFER`, đúng số tiền) và 1
dòng `booking_status_history` (`from_status=NULL, to_status=PENDING, changed_by_id` = id của
`an.nguyen`) · đổi trạng thái đơn đó hai lần bằng một quản trị viên **thứ hai** (`admin2@tourbooking.vn`,
tạo riêng để kiểm quy tắc) → dòng thời gian dài thêm đúng 2 dòng, mỗi dòng ghi "Người thực hiện: Admin
Phu" và lý do đã nhập · `POST .../mark-paid` với `txnRef` mới → **PAID**, hiện đúng mã giao dịch ·
lặp lại `mark-paid` với **cùng `txnRef`** cho một khoản khác → bị chặn, flash lỗi tiếng Việt
*"Mã giao dịch … đã được dùng cho một lần thanh toán khác."*, khoản đó vẫn `PENDING` (không bị ghi đè
sai) · **thử xoá `admin2@tourbooking.vn`** (đã đổi trạng thái đơn 2 lần) qua
`DELETE /api/admin/users/5` → **409** `error.user.delete.hasBookingHistory`: *"Không xoá được: tài
khoản này đã 2 lần đổi trạng thái đơn hàng, nhật ký phải giữ nguyên người thao tác."* - quy tắc này
tồn tại từ Phase 7 nhưng đây là lần đầu tiên kích hoạt được bằng dữ liệu **không phải seed** · ba
file câu chữ **695 khoá mỗi file**, 0 trùng, 0 lệch · 0 lỗi console trên cả năm trang trên · không
tràn ngang ở 375/1024/1440px · 4 ảnh báo cáo mới trong `docs/screenshots/`: `screenshot-contact.png`,
`screenshot-admin-contact.png` (lọc "Mới", 3 dòng dữ liệu thật), và `screenshot-409.png` (phản hồi
409 thật của `DELETE /api/admin/promotions/1`, mã `HELLO2026` đã có lượt dùng - bằng chứng còn thiếu
từ Phase 7.4).

*Dữ liệu để lại có chủ đích, không dọn:* tài khoản `admin2@tourbooking.vn`, đơn
`TB20260921F975QK`, hai liên hệ test, và khoản thanh toán vừa đánh dấu `PAID` đều là **bằng chứng
sống** cho các quy tắc vừa vá - khác với Phase 7.5 (nơi dữ liệu test bị dọn sạch vì đó là đánh giá
gắn với tài khoản/tour seed có sẵn), lần này dữ liệu test độc lập nên giữ lại để ai kiểm tra sau vẫn
thấy đúng những gì đã verify. `admin2@tourbooking.vn` còn hữu ích cho việc còn lại ở mục 6 dưới đây
(kiểm quy tắc "không xoá quản trị viên cuối cùng" bằng thao tác thật).

**Phase 8 — sửa 13 lỗi do hai subagent rà ra (một đọc mã, một bấm thử trên trình duyệt thật),
2026-09-22.** 13/13 đã sửa, tất cả verify bằng lệnh thật (curl + sqlcmd), không verify bằng suy đoán.

*Nghiêm trọng.*
1. Hồ sơ cá nhân không lưu được: `AccountController` nay có phương thức `@ModelAttribute("profileForm")`
   điền sẵn `id` từ principal **trước khi** Spring gắn tham số request, nên `@UniqueEmail(excludeIdField)`
   thấy đúng `id` lúc validate. `AdminUserForm` **không dính lỗi này** - `admin/user/form.html` đã có sẵn
   `<input type="hidden" th:field="*{id}">` nên `id` luôn có mặt lúc bind, kể cả trước bản vá.
2. `AdminUserController.toggleEnabled` đổi `admin.user.enabled`/`admin.user.disabled` (một cái là nhãn
   checkbox, một cái không tồn tại) thành đúng `admin.user.enabledMsg`/`admin.user.disabledMsg`.
3. `numChildren` âm: `BookingService.updateDetailQuantity` nay chặn `children < 0` và trần tổng khách
   (dùng lại `CartService.MAX_GUESTS_PER_ITEM`, nay `public`) **trước** khi đụng tới `holdSeats`/
   `releaseSeats`. `AdminBookingController.updateDetail` thêm `@Min(0)` + lớp mang `@Validated` -
   nhưng `@Min` trên `@RequestParam` **im lặng vô tác dụng nếu thiếu `@Validated` ở mức lớp** (xem
   gotcha #59), và `ConstraintViolationException` nó ném ra **không được `GlobalExceptionHandler` cũ
   bắt** (chỉ bắt `BusinessRuleException`) - phải thêm hẳn một `@ExceptionHandler` mới, không thì
   `@Min` chặn thành công nhưng lại đổi 302-kèm-flash thành 500 thô.
4. Tour "Ngừng bán" vẫn đặt được trọn luồng: `TourDepartureRepository.findBookable` thêm
   `AND d.tour.active = true`; `CartService.addByDeparture` và vòng lặp `BookingService.placeOrder`
   thêm kiểm `departure.getTour().isActive()` tường minh (không sửa `TourDeparture.isBookable()` vì
   `DepartureMapper` cố ý dựa vào việc `findBookable` không nạp `tour` để tránh đụng LAZY ngoài giao
   dịch - thêm điều kiện vào chính câu JPQL không phá vỡ điều đó); `TourController.detail` trả 404
   khi tour ngừng bán và người xem không phải admin. Verify bằng đúng tour Mộc Châu (id=9) đã bị tester
   ngừng bán: `/tours/9` → 404 (khách + khách đăng nhập), admin vẫn 200; thêm giỏ qua cả hai đường
   (`departureId` và `tourId`) đều bị chặn; **cố tình bật lại tour, thêm giỏ, tắt lại rồi mới checkout**
   (mô phỏng "đã mở tab tour từ lúc còn bán") → `placeOrder` chặn đúng, 0 đơn ma, 0 chỗ bị trừ.

*Vừa.*
5. Sửa số khách / huỷ đơn không đồng bộ thanh toán: `PaymentService.syncPendingAmount` (gọi sau
   `booking.recalculateTotal()` trong `updateDetailQuantity`) và `PaymentService.cancelPendingForBooking`
   (gọi khi `updateStatus` chuyển sang `CANCELLED`, chuyển khoản `PENDING` sang `FAILED` - `PaymentStatus`
   không có hằng số "đã huỷ" riêng nên dùng lại `FAILED` kèm `note` giải thích). Verify bằng đơn thật:
   sửa 2→4 khách, thành tiền + tổng đơn + khoản `PENDING` cùng nhảy từ 13.178.000 lên 24.379.300 ·
   huỷ đơn đó → khoản chuyển `FAILED`, `available_seats` trả đúng về giá trị ban đầu.
6. `markPaid` không kiểm trạng thái / chủ sở hữu: `PaymentService.markPaid` nay nhận thêm `bookingCode`
   để đối chiếu `payment.booking.code`, và kiểm `status == PENDING` trước khi ghi. Verify bằng hai đơn
   thật của hai tài khoản khác nhau: `POST .../TB-B/payments/{id-thuộc-TB-A}/mark-paid` → 409
   `error.payment.bookingMismatch`, khoản của TB-A không đổi · mark-paid hợp lệ → thành công · lặp lại
   trên khoản vừa `PAID` → 409 `error.payment.notPending`, `txn_ref` không bị ghi đè.
7. `Promotion.usedCount` lost update: chọn khoá bi quan (`PromotionRepository.findByIdForUpdate`,
   `PESSIMISTIC_WRITE`) thay vì `@Version` - không phải sửa `02_schema.sql`. **Vấp thêm một lớp bug
   nữa khi verify bằng đơn hàng thật song song** (xem gotcha #60): chỉ khoá thôi chưa đủ, phải
   `entityManager.refresh(locked)` ngay sau khi giữ khoá. `PromotionForm.usageLimitPerUser` đổi mặc
   định `null` (không giới hạn) thành `1`. Verify bằng **hai request checkout bắn gần như đồng thời
   bằng `curl ... & curl ... & wait`** (không phải suy luận): mã `usageLimit=1` - trước khi thêm
   `entityManager.refresh` cả hai request cùng **thành công**, cả hai đơn đều được giảm giá, nhưng
   `used_count` chỉ lên 1 thay vì 2 (đúng lost update, dù đã có khoá!) · sau khi thêm `refresh` thì
   đúng một request thành công, request kia nhận lỗi và **không** tạo đơn ma · lặp lại với hai phiên
   đăng nhập **cùng một tài khoản** cho mã `usageLimitPerUser=1` → cùng kết quả, đúng 1 lượt dùng.
8. `PromotionForm` không chặn "giảm 200%": thêm `@AssertTrue isPercentValueValid()` (giới hạn
   `discountValue <= 100` khi `discountType == PERCENT`), và thêm khối hiện lỗi trong
   `admin/promotion/form.html` (cùng khuôn với `periodValid` có sẵn - thiếu khối này thì lỗi chặn được
   nhưng người dùng không thấy vì sao). Verify: gửi `PERCENT` + `200` → **200** (không lưu, không redirect),
   hiện đúng câu "Giảm theo phần trăm không được vượt quá 100%." · gửi `15%` hợp lệ → lưu bình thường (302).

*Nhẹ.*
9. `AdminBookingController.updateStatus` đổi `status.getDisplayName()` (tiếng Việt viết cứng) thành
   `messages.get(status.getMessageKey())` trước khi lồng vào `admin.booking.statusUpdated`. Verify bằng
   cookie ngôn ngữ `en`: đổi trạng thái → flash hiện "The booking has been moved to Confirmed."
   (trước đó sẽ lòi ra tên trạng thái tiếng Việt giữa câu tiếng Anh).
10. `AdminContactController.list` bắt `IllegalArgumentException` của `ContactStatus.valueOf` (giá trị lạ
    thì coi như không lọc) thay vì để nó lọt ra `GlobalExceptionHandler` vốn chỉ bắt
    `BusinessRuleException`. Verify: `?status=xyz` → **200** (trước đó **500**), HTML đủ.
11. `admin/contact/list.html` dùng nhầm `admin.review.replyPlaceholder` - đổi sang khoá riêng
    `admin.contact.replyPlaceholder` (thêm cả ba file). Verify: trang liên hệ hiện đúng "Trả lời liên hệ
    này..." / "Reply to this contact message..." tuỳ ngôn ngữ, không còn dính chữ "đánh giá".
12. Số ít/nhiều tiếng Anh: `tour.list.resultCount`, `home.hero.badge`, `home.categories.count` trong
    `messages_en.properties` đổi sang cú pháp `{0,choice,0#s|1#|1<s}` của `MessageFormat` - chỉ sửa
    nội dung, không thêm khoá mới. Verify: `/tours?q=Singapore` (1 kết quả) → "1 tour found"; trang
    chủ hiện đúng "1 journey" cho danh mục có 1 tour, "0 journeys" cho danh mục rỗng, "20 journeys
    with seats open right now" cho hero.
13. `booking_passengers` chưa có nơi ghi vào thật - xác nhận lại bằng cách rà `src/main/java` lẫn
    `DataSeeder`, **không sửa mã** (cố tình, xem cảnh báo trong mục "Việc còn lại" - đổi luồng UC007 sẽ
    kéo theo phải sửa báo cáo/UML đã chốt). Chỉ cập nhật mục "Việc còn lại" số 5 cho đúng sự thật.

*Verify chung:* `./mvnw -o clean compile` sạch nhiều lần, app khởi động lại qua toàn bộ quá trình sửa
luôn với `ddl-auto: validate`, **không entity nào đổi nên `02_schema.sql` không đổi một dòng** · ba
file câu chữ 690 → **695 khoá mỗi file**, 0 trùng, 0 lệch · `grep -c "</html>"` = 1 trên chín trang đã
sửa (`/account/profile`, `/admin/users`, `/admin/promotions`, `/admin/promotions/form`,
`/admin/contacts?status=xyz`, `/admin/contacts?status=ALL`, `/`, `/tours`, `/admin/bookings`) · bảy quy
tắc chặn xoá của `User.delete()` kiểm bằng `curl -X DELETE` (browser `confirm()` chặn tester tự động
hoá được việc này) - ba quy tắc **cô lập được** bằng dữ liệu thật (`self`, `hasBookings` id=2,
`hasBookingHistory` id=5/`admin2`) đều đúng **409**, không tài khoản nào bị xoá thật; bốn quy tắc còn
lại (`lastAdmin`, `hasReviews`, `hasCouponUsages`, `hasContactMessages`) **không cô lập được bằng dữ
liệu hiện có** vì luôn bị `hasBookings`/`hasBookingHistory` chặn trước theo đúng thứ tự kiểm tra trong
`UserService.delete` (đặt tour luôn đi trước đánh giá/dùng mã trong domain thật - không tạo được tài
khoản "chỉ có đánh giá mà không có đơn" từ luồng nghiệp vụ bình thường) - xác nhận đúng bằng đọc mã,
không phải bằng thao tác thật; ghi rõ ra đây để không ai tưởng nhầm là đã kiểm hết bảy quy tắc bằng dữ
liệu thật. Tiện thể kiểm thêm `error.tour.delete.inBooking` và `error.category.delete.hasTours` (ngoài
phạm vi "bảy quy tắc" nhưng cùng cơ chế) → cũng đúng 409.

*Dữ liệu để lại / dọn:* xoá tài khoản `qa.tester.register@example.com`, danh mục rỗng "Danh mục kiểm
thử QA", đợt khởi hành 20/12/2026 của tour Đà Lạt (cả ba qua đúng API/route của ứng dụng, không đụng
SQL) và hai liên hệ `tester.qa*@example.com` (SQL trực tiếp - ứng dụng không có chức năng xoá liên hệ).
Giữ nguyên `admin2@tourbooking.vn`, đơn `TB20260921F975QK`, đơn `TB20260922JTPAF3` (bằng chứng lỗi #4 -
tour Mộc Châu id=9 **cố tình để lại ở trạng thái "Ngừng bán"** đúng như tester để lại, dù bài viết có
bật/tắt tạm thời để verify bug #4 ở giữa). Bảy đơn `TB…` + ba mã khuyến mãi (`RACETEST`, `RACETEST2`,
`RACETEST3`) tự tạo để verify bug #3/#5/#6/#7 đã **huỷ đơn / tắt mã** (không xoá - ứng dụng không cho
xoá đơn, và ba mã đã có lượt dùng nên tự chính quy tắc chặn xoá của bản vá này sẽ chặn lại).

---

### Phase 9 — SMTP thật + thư chào mừng đăng ký (subagent EMAIL, 2026-09-23)

Đề bài đòi gửi email ở **hai** chỗ (phiếu ĐK bài tập nhóm): đăng ký tài khoản và thanh toán. Đơn đặt
tour đã gửi từ trước (`BookingEmailListener`); **đăng ký tài khoản thì chưa** - đây là phần vá.

1. `pom.xml` **đã sẵn** `spring-boot-starter-mail` và `SmtpEmailService` (`JavaMailSender`, chọn bằng
   `app.mail.mode=smtp`) từ trước khi subagent này chạy - lúc lập kế hoạch tưởng còn thiếu, đọc lại mã
   mới biết đã có. Chỉ phần thư chào mừng đăng ký là thật sự thiếu.
2. Thêm `EmailService.sendWelcomeEmail(String fullName, String email)` (nhận hai chuỗi, **không** nhận
   `User` hay mật khẩu - lý do trong Javadoc), `WelcomeEmailRenderer`, mẫu
   `templates/email/welcome.html`, cài đặt ở cả `ConsoleEmailService` và `SmtpEmailService`.
3. `UserService.register` phát `UserRegisteredEvent(fullName, email)` sau `userRepository.save`;
   `UserEmailListener` nghe bằng `@TransactionalEventListener(AFTER_COMMIT)` y hệt
   `BookingEmailListener` - lỗi gửi thư bị bắt và ghi log, không làm hỏng đăng ký. Verify bằng cách tạm
   cho `ConsoleEmailService.sendWelcomeEmail` ném `RuntimeException` ngay dòng đầu, đăng ký một tài
   khoản thật → vẫn **302** `redirect:/login?registered` (không phải lỗi), log có dòng
   `UserEmailListener : Không gửi được thư chào mừng...`, tài khoản vẫn nằm trong CSDL. Đã trả mã về
   như cũ sau khi verify xong (diff sạch, `clean compile` lại chạy qua).
4. **Gotcha #61 — log console nuốt dấu tiếng Việt dù file `messages*.properties` đọc đúng UTF-8.**
   Logback mặc định ghi console theo bảng mã hệ điều hành (Windows: Cp1252/Cp1258), không phải UTF-8 -
   "Đã đăng ký" thành "?? ??ng k?" **thật sự mất byte** (không phải lỗi hiển thị terminal): đọc file log
   bằng `open(path,'rb').decode('utf-8')` ném `UnicodeDecodeError`, đọc bằng `cp1252` mới ra chữ đúng
   nhưng dấu đã bị thay bằng `?`. Sửa bằng khai báo tường minh `logging.charset.console=UTF-8` (và
   `logging.charset.file=UTF-8`) trong `application.properties` - không đụng `logback.xml` vì dự án không có
   file này, dùng property Spring Boot tự nối vào cấu hình Logback mặc định. Ảnh hưởng **toàn bộ** log
   tiếng Việt của ứng dụng, không riêng gì thư chào mừng (thư xác nhận đặt tour trước đó cũng bị, chỉ
   là chưa ai để ý vì không có test nào đọc lại byte thật của file log). Verify: đăng ký tài khoản thật
   sau khi sửa → đọc file log bằng `utf-8` không lỗi, in ra đúng "Chào mừng bạn đến với IUH Tour
   Booking!", "Họ tên : Nguyễn Văn Test" - không còn dấu `?`.
5. Ba file câu chữ: thêm 8 khoá `email.welcome.*` (subject/greeting/intro/accountInfo/email/cta/outro/
   signature) vào cả ba file, 695 → **703 khoá mỗi file**, 0 trùng, 0 lệch (lệnh đối chiếu ở cuối file
   này).
6. **Chưa thử được**: chế độ `app.mail.mode=smtp` với tài khoản Gmail thật - máy chấm bài không có sẵn
   tài khoản SMTP (đúng chủ đích ban đầu của dự án). Cách bật khi có tài khoản thật:
   ```bash
   export MAIL_USERNAME="ten-tai-khoan@gmail.com"
   export MAIL_PASSWORD="mat-khau-ung-dung-16-ky-tu"   # KHÔNG dùng mật khẩu Gmail thường -
                                                         # Google > Tài khoản > Bảo mật > Mật khẩu ứng dụng
                                                         # (yêu cầu đã bật xác minh 2 bước)
   ./mvnw -o spring-boot:run --spring.mail.username="$MAIL_USERNAME" \
       --spring.mail.password="$MAIL_PASSWORD" --app.mail.mode=smtp
   ```
   Không bao giờ đặt `MAIL_USERNAME`/`MAIL_PASSWORD` trong `application.properties` hay commit vào mã nguồn -
   chỉ qua biến môi trường, đúng vì mã nguồn đã công khai trên GitHub.
7. *Dữ liệu để lại / dọn:* ba tài khoản kiểm thử không có đơn hàng
   (`email.agent.test*.<timestamp>@gmail.com`) đã xoá thẳng bằng SQL (không qua ứng dụng vì không có
   chức năng xoá tài khoản của chính mình, và tài khoản admin dùng để test đăng nhập bị lỗi ở bước
   khác - xem dưới). **Để lại** `email.agent.booking.<timestamp>@gmail.com` (id 8) vì đã có đơn thật
   `TB20260923HNWU3E` - đúng quy tắc chặn xoá `error.user.delete.hasBookings` của chính `UserService`,
   không xoá tay qua SQL để khỏi phá vỡ khoá ngoại `booking_status_history`/`payments` mà chính bản vá
   Phase 7.6 dựng lên.
8. **Không phải lỗi ứng dụng, chỉ là lỗi kịch bản kiểm thử của chính subagent này**: lần đầu login bằng
   `curl` cho `an.nguyen@gmail.com` và `admin@tourbooking.vn` báo sai mật khẩu dù `123456` đúng - hoá ra
   payload dựng bằng heredoc (`cat > file <<EOF ... EOF`) có `password=123456` là **trường cuối cùng**,
   heredoc luôn thêm một dòng mới ở cuối file, `curl --data-binary @file` gửi nguyên byte đó nên mật
   khẩu POST thật sự là `"123456\n"` (8 ký tự). Giải mã BCrypt hash lấy từ CSDL bằng
   `BCryptPasswordEncoder` chạy tay (`enc.matches("123456", hash)` → `true`) mới lộ ra vấn đề nằm ở
   payload, không phải ở `SecurityConfig`/`CustomUserDetailsService`. Sửa bằng `printf` (không xuống
   dòng cuối) hoặc đặt trường có khả năng dính khoảng trắng cuối (`address`, `note`) làm trường cuối
   cùng trong payload thay vì `password`.

*Verify chung:* `./mvnw -o clean compile` sạch · app khởi động **ba lần** qua toàn bộ quá trình sửa,
lần nào cũng không cần biến môi trường nào (chế độ mặc định `console`, `ddl-auto: validate` qua vì
không entity nào đổi) · đăng ký tài khoản thật → thư chào mừng in console đúng dấu tiếng Việt · giả lập
lỗi gửi thư → đăng ký vẫn thành công · đặt đơn tour thật (`TB20260923HNWU3E`, tour Phú Quốc, 2 người
lớn, 11.980.000đ) → thư xác nhận vẫn in console đúng như trước, không hồi quy · `grep -c "</html>"` = 1
trên `/register`, `/login`, `/checkout` · ba file câu chữ 695 → 703 khoá, 0 trùng, 0 lệch.

---

### Phase 10 — Sửa 16 lỗi do agent rà lần hai (2026-09-23)

Một agent review khác soát lại toàn bộ mã sau Phase 8/9 và ra thêm 16 lỗi (1 **nghiêm trọng**, 2
**vừa**, 13 **nhẹ**). Không đụng máy trạng thái đơn tự do, cách tính doanh thu, luồng hoàn tiền, tự
huỷ đơn, hạn chót đặt tour, nhập hành khách - đúng như đã chốt là quyết định nghiệp vụ còn chờ user.

**1.1 (nghiêm trọng) — Admin không sửa được tài khoản nếu không đặt lại mật khẩu.**
`AdminUserForm.newPassword` đổi từ `@Size(min=6,max=50)` (chuỗi rỗng vẫn bị đo độ dài, luôn thất bại)
sang `@Pattern(regexp="^$|.{6,50}")`. *Trước:* POST sửa hồ sơ để trống mật khẩu → **200** kèm lỗi
"Mật khẩu phải từ 6 đến 50 ký tự.". *Sau:* → **302**, hồ sơ lưu đúng, mật khẩu cũ giữ nguyên (đã kiểm
`saveFromAdmin` không băm chuỗi rỗng).

**1.3 (vừa) — Sửa tài khoản lách được "không tự khoá mình / không khoá admin cuối".**
`UserService.saveFromAdmin` nay nhận thêm `currentUserId`, chặn tự hạ quyền/khoá chính mình
(`error.user.edit.selfDemote`) và hạ quyền/khoá quản trị viên đang bật cuối cùng
(`error.user.edit.lastAdmin`). *Trước:* admin (`id=1`) tự sửa hồ sơ mình thành `CUSTOMER` + bỏ tick
"Kích hoạt" → **302**, CSDL đổi thật (`role=CUSTOMER, enabled=0`), và phiên đang đăng nhập **vẫn còn
vào được** `/admin/users` (đúng luôn bằng chứng sống cho lỗ hổng 1.8 bên dưới). *Sau:* → **200** kèm
"Không thể tự hạ quyền hoặc tự khoá chính tài khoản bạn đang đăng nhập.", CSDL không đổi. Chưa cô lập
riêng được nhánh "admin cuối" bằng HTTP (cần một admin thứ ba sạch, xem mục 6 "Vấn đề còn tồn đọng"
cũ) nhưng dùng lại đúng câu truy vấn `countByRoleAndEnabledTrue` đã chứng minh đúng ở `delete`/
`toggleEnabled`.

**1.4 (vừa) — Khôi phục đơn đã huỷ thì mất khoản phải thu.**
`PaymentService.restorePendingForBooking` sinh lại một dòng `PENDING` theo đúng tổng hiện tại của
đơn, gọi từ nhánh khôi phục của `BookingService.updateStatus`. *Trước/sau (đơn thật `TB20260917QT7B9X`,
huỷ rồi khôi phục):* trước khi vá, sau khôi phục đơn chỉ còn đúng một khoản `FAILED`, không khoản nào
`PENDING`. Sau khi vá: khoản `FAILED` cũ vẫn giữ (lịch sử), **có thêm** khoản `PENDING` mới đúng
9.180.000đ - kiểm bằng SQL thật, không suy luận.

**1.6 (vừa) — Sửa số khách làm lệch tiền giảm giá và khoản thu.**
`PromotionService.recalculateDiscount` tính lại `discountAmount` theo đúng luật của `Promotion`
(min order / trần phần trăm) mỗi khi số khách đổi, đồng bộ luôn `coupon_usages.discount_amount`; đơn
đã có khoản `PAID` thì chặn hẳn việc sửa số khách (`error.booking.detailQuantity.hasPaidPayment`).
Verify bằng đơn thật dùng mã `VIP20` (giảm 20%, trần 2.000.000đ, đơn tối thiểu 15.000.000đ): đặt 2
người lớn (25.800.000đ) → giảm đúng 2.000.000đ (bị trần chặn) → admin sửa còn 1 người lớn
(12.900.000đ, dưới mức tối thiểu) → `discount_amount` **cả ở `bookings` lẫn `coupon_usages`** tự động
về `0.00`, `payments.amount` đồng bộ theo còn 12.900.000đ. Verify riêng phần chặn `PAID`: đơn thật
`TB20260910HK4M2P` (đã có khoản `PAID`) → sửa số khách bị chặn với thông báo tiếng Việt, `booking_details`
không đổi.

**1.7 (nhẹ) — Bản sửa lỗi hồ sơ chưa kín.**
`AccountController` thêm `@InitBinder("profileForm")` gọi `binder.setDisallowedFields("id")`;
`UserService.updateProfile` tự kiểm email trùng trừ chính mình, ném `BusinessRuleException` thay vì
để lộ ra tận CSDL. *Trước:* tài khoản `an.nguyen` POST `/account/profile` kèm `id=3&email=binh.tran@gmail.com`
→ **500** `Violation of UNIQUE KEY constraint 'uk_users_email'` (bắt được nguyên văn từ log server).
*Sau:* → **200** kèm lỗi "Email này đã được sử dụng. Vui lòng chọn email khác." gắn đúng vào ô email,
dữ liệu hai tài khoản không đổi.

**1.8 (nhẹ nhưng phải chạy thật) — Hạ quyền/khoá tài khoản không có hiệu lực với phiên đang đăng nhập.**
Thêm `SessionRegistry` + `HttpSessionEventPublisher` (bean ở `SecurityConfig`) và
`.sessionConcurrency(...).maximumSessions(-1)` (không giới hạn số phiên - chỉ dùng để **đăng ký**
phiên, không chặn đăng nhập nhiều nơi) kèm `SimpleRedirectSessionInformationExpiredStrategy` đưa về
`/login?expired`. `UserService.expireSessionsOf` gọi `SessionInformation.expireNow()` cho mọi phiên
của người dùng ngay sau khi `saveFromAdmin` đổi vai trò/trạng thái, hoặc `toggleEnabled` chuyển sang
khoá. *Trước:* đơn 1.3 ở trên đã tự chứng minh - phiên vẫn `200` dù CSDL đã đổi. *Sau, thao tác thật
với hai phiên `curl` độc lập:* đăng nhập `binh.tran` (phiên A) → `GET /account/bookings` → **200**.
Admin (phiên B) bấm khoá tài khoản đó. Request **kế tiếp** của phiên A (không đăng nhập lại, không đổi
gì phía client) → **302** `Location: /login?expired`, `Set-Cookie: JSESSIONID=; Max-Age=0` (phiên bị
huỷ thật, không phải chỉ chuyển hướng). Trang `/login?expired` hiện đúng câu
"Phiên đăng nhập của bạn đã hết hạn vì tài khoản vừa được thay đổi quyền hoặc bị khoá.". Kiểm không hồi
quy gotcha #2 ("giỏ hàng sống sót qua đăng nhập"): thêm tour vào giỏ lúc chưa đăng nhập → đăng nhập
(`cuong.le`) → `JSESSIONID` đổi (đúng `changeSessionId()`) nhưng giỏ **vẫn còn nguyên** 1 dòng/2 khách.

**1.10 (nhẹ) — Tiếng Việt viết cứng lọt vào giao diện tiếng Anh.**
`PaymentService.markPaid` và `BookingEmailRenderer.plainSummary` đổi `.getDisplayName()` (hằng số
tiếng Việt trong enum) sang `messages.get(x.getMessageKey())`; `PaymentService.cancelPendingForBooking`
đổi ghi cứng `"Đơn đã huỷ - khoản thu không còn hiệu lực."` sang khoá `payment.note.cancelledByBookingCancel`.
Xác nhận bằng đúng đơn `TB20260917QT7B9X` dùng để test 1.4 - khoản `FAILED` sinh ra mang note đọc từ
`messages.properties`, không còn chuỗi Java viết tay.

**1.11 (nhẹ) — `CheckoutForm.paymentMethod` là chuỗi tự do.**
Thêm `@ValidPaymentMethod` (validator mới, so khớp với đúng ba câu dịch `checkout.payment.bank/cash/momo`
ở **cả** hai ngôn ngữ vi/en, không phụ thuộc locale hiện tại của request) + `@Size(max=30)` khớp cột
CSDL. *Trước (suy luận từ mã, cột `payment_method` dài 30 và validator cũ chỉ có `@NotBlank`):* một
chuỗi tự chế dài hơn 30 ký tự sẽ vỡ ở lúc `INSERT`. *Sau, thử thật:* POST `/checkout` với
`paymentMethod=HACKED_METHOD_123...` (58 ký tự) → **200** kèm "Hình thức thanh toán không hợp lệ.",
không có đơn nào được tạo (kiểm bằng SQL, đếm đơn theo số điện thoại không đổi).

**1.12 (nhẹ) — Giỏ cộng dồn không kiểm trần.**
`CartService.addByDeparture` kiểm `MAX_GUESTS_PER_ITEM` (30) trên **tổng sau khi cộng dồn**
(`alreadyInCart + adults + children`), không chỉ trên số khách của riêng lượt thêm mới. *Thử thật:*
thêm 20 khách (thành công) rồi thêm tiếp 20 khách nữa cùng đợt → lượt hai bị chặn với
"Mỗi lần đặt tối đa 30 khách...", giỏ vẫn dừng ở 20 khách (không lặng lẽ cộng thành 40).

**1.13 (nhẹ) — Đặt từ trang danh sách lấy đợt gần nhất còn chỗ, không lấy đợt gần nhất ĐỦ chỗ.**
`TourDepartureRepository.findNextBookable` nhận thêm `minSeats`, lọc `hasEnoughSeats(minSeats)` trước
khi lấy đợt đầu tiên; `CartService.addByTour` truyền `adults + children` vào. Kiểm bằng mã, chưa dựng
được kịch bản dữ liệu thật có hai đợt liền kề với sức chứa khác nhau đủ rõ để chứng minh qua `curl`
trong phạm vi thời gian của đợt sửa này - **cần verify thêm bằng dữ liệu thật ở lần sau**.

**1.14 (nhẹ) — `GET /api/tours/{id}` trả cả tour ngừng bán.**
`TourService.findDetail` ném `ResourceNotFoundException` nếu `!tour.isActive()`, khớp hành vi
`TourController` phía web. *Trước/sau, tour `id=1` (`PQ-3N2D`):* bật `active=1` → `GET /api/tours/1`
= **200**; `POST /admin/tours/1/toggle-active` (tắt) → cùng địa chỉ = **404**; bật lại → **200**.

**1.15 (nhẹ) — `PromotionForm.maxDiscount` không ràng buộc.**
Thêm `@AssertTrue isMaxDiscountValid` (âm/0 thì báo lỗi, để trống vẫn hợp lệ = không chặn trần), gắn
hiển thị lỗi ở `admin/promotion/form.html` theo đúng khuôn `percentValueValid`/`periodValid` sẵn có
(lỗi `@AssertTrue` rơi vào tên phương thức bỏ `is`, không rơi vào tên trường). *Thử thật:* tạo mã mới
với `maxDiscount=-5000` → **200** kèm "Mức giảm tối đa phải lớn hơn 0 nếu có nhập...", không mã nào
được tạo.

**1.16 (nhẹ) — `AdminTourController` gắn mọi lỗi vào ô "Mã tour".**
Phân nhánh theo tiền tố khoá thông điệp: `error.upload.*` gắn vào `thumbnailFile` (đã thêm
`th:errors="*{thumbnailFile}"` vào khuôn mẫu, trước đó không có ô hiện lỗi nào ở đó), còn lại
(`error.tour.codeExists`) vẫn gắn `code` như cũ. *Trước/sau, tour `id=1`:* multipart upload một file
`.txt` làm `thumbnailFile` → **trước** lỗi "Chỉ chấp nhận các định dạng..." hiện dưới ô **Mã tour**;
**sau** hiện đúng dưới ô **Ảnh đại diện**, ô Mã tour sạch (không còn lớp `is-invalid`).

**1.17 (nhẹ) — Chế độ `smtp` không có timeout.**
Thêm `mail.smtp.connectiontimeout/timeout/writetimeout=5000` vào `application.properties`. Không thử được
bằng thao tác thật (không có máy chủ SMTP giả lập trong phạm vi đợt sửa) - xác nhận bằng đọc cấu hình
sau khi `process-resources`.

**1.18 (nhẹ) — Hai tình huống vận hành.**
- Khoá lạc quan: `GlobalExceptionHandler` bắt thêm `ObjectOptimisticLockingFailureException`, chuyển
  thành `error.request.dataChanged` + redirect thay vì trang 500. Chưa dựng được kịch bản đua thật
  (`curl ... & curl ... & wait` như gotcha #60) trong phạm vi đợt sửa này - **cần verify thêm**.
- `DepartureForm` bỏ `@Future`, chuyển xuống `DepartureService.requireFutureDate` chỉ áp dụng khi
  **tạo mới** hoặc khi **sửa mà đổi ngày**. *Thử thật* (chèn thẳng bằng SQL một đợt khởi hành ngày
  `2026-01-10` cho tour `id=1` - quá khứ so với "hôm nay" `2026-09-23`, không qua form được vì form
  không cho tạo mới ngày quá khứ): sửa `totalSeats` 20→25 **giữ nguyên ngày `2026-01-10`** → **302**,
  CSDL cập nhật đúng (kiểm bằng SQL); đổi tiếp sang ngày `2026-02-15` (vẫn là quá khứ so với hôm nay,
  tức đúng kịch bản "sửa mà đổi ngày" phải bị chặn) → **200** kèm "Ngày khởi hành mới phải ở tương
  lai.". Xoá đợt kiểm thử này bằng SQL sau khi xong (không có lượt đặt nào, không phạm quy tắc chặn
  xoá nào).

**Phụ — chú thích HTML thường lọt vào mã nguồn thư.** `templates/email/welcome.html` và
`booking-confirmation.html` đổi mọi `<!-- ... -->` nội bộ (kể cả khối ghi chú lớn ngay sau
`<!DOCTYPE html>`) sang `<!--/* ... */-->` đúng quy ước dự án.

*Verify chung:* `./mvnw -o clean compile` sạch, app khởi động **hai lần** (một lần build gốc trước
khi sửa để tái hiện 1.1/1.3/1.7/1.8 bằng `git stash -u`, một lần build đã sửa) với `ddl-auto: validate`
qua cả hai lần (không entity nào đổi) · ba file câu chữ 703 → **712 khoá mỗi file**, 0 trùng, 0 lệch ·
`grep -c "</html>"` = 1 trên 17 trang đã kiểm (`/`, `/tours`, `/tours/1`, `/login`, `/register`,
`/about`, `/contact`, `/admin`, `/admin/users`, `/admin/tours(+form)`, `/admin/promotions(+form)`,
`/admin/bookings`, `/account/profile`, `/account/bookings`, `/cart`) · sau khi xong đã tắt app
(`taskkill`), đối chiếu lại CSDL bằng SQL: bốn tài khoản mẫu (`admin`, `an.nguyen`, `binh.tran`,
`cuong.le`) về đúng vai trò/trạng thái gốc.

*Dữ liệu test để lại (đã cân nhắc, không xoá tay để tránh phá khoá ngoại của chính các bảng vừa vá ở
Phase 7.6):* một đơn mới `TB20260923DLYA5V` (dùng mã `VIP20` để verify 1.6, đã chuyển sang
`CANCELLED` sau khi test xong thay vì để `PENDING` treo) kéo theo `promotions.used_count` của `VIP20`
tăng từ 0 lên 1 và một dòng `coupon_usages` mới; đơn `TB20260917QT7B9X` (dùng để verify 1.4) có thêm
một dòng lịch sử `payments` (`FAILED` cũ + `PENDING` mới) so với lúc đầu dù trạng thái đơn đã đưa về
lại đúng `PENDING` ban đầu. Không có tài khoản, tour, hay đợt khởi hành thử nghiệm nào còn sót lại.

**Mục nêu chưa xong / chưa verify được bằng thao tác thật (không phải "chưa sửa mã"):**
1. **1.3, nhánh "admin cuối"** - guard dùng lại đúng câu truy vấn đã chứng minh ở `delete`/
   `toggleEnabled`, nhưng chưa tự chạy một kịch bản HTTP cô lập đúng MỘT mình nhánh này (cần một admin
   thứ ba "sạch", không đơn/đánh giá/lịch sử, xem mục cũ trong "Vấn đề còn tồn đọng").
2. **1.13** - đã sửa đúng logic (`hasEnoughSeats(minSeats)` lọc trước khi lấy đợt đầu), nhưng chưa
   dựng được hai đợt khởi hành liền kề với sức chứa lệch nhau đủ rõ trong dữ liệu mẫu hiện có để chứng
   minh bằng `curl` thật.
3. **1.17** - chỉ thêm cấu hình, không có máy chủ SMTP giả lập (timeout kế hoạch: cố ý cho service SMTP
   không tồn tại `MAIL_HOST=10.255.255.1 app.mail.mode=smtp` rồi bấm đặt tour, đo thời gian phản hồi
   dưới 6 giây thay vì treo vô hạn - chưa chạy).
4. **1.18, nhánh khoá lạc quan** - đã thêm lưới an toàn đúng loại ngoại lệ Spring ném ra
   (`ObjectOptimisticLockingFailureException`), nhưng chưa dựng kịch bản đua thật kiểu gotcha #60
   (`curl ... & curl ... & wait`) để tự tay kích hoạt nó.

---

### Phase 14 — Xoá comment, chỉ giữ một dòng tiếng Việt cho mỗi lớp/hàm (subagent XOÁ COMMENT, 2026-09-24)

User muốn tự đọc hiểu mã nên yêu cầu xoá toàn bộ comment trong 211 file được git theo dõi (mọi
`*.java`, `*.html`, `*.css`, `*.js`, `application.yml` (nay là `application.properties`), ba file `messages*.properties`,
`database/*.sql`, `database/00_enable_sqlserver_tcp.ps1`, `pom.xml`), chỉ để lại đúng một dòng tiếng
Việt cho mỗi lớp và mỗi hàm không hiển nhiên (bỏ qua getter/setter, hàm một dòng, `equals`/`hashCode`/
`toString`). Bản đầy đủ comment vẫn còn nguyên trong lịch sử git (trước commit này).

**Cách làm.** Viết tay bảy bộ tách từ tố (tokenizer) bằng Python - **không dùng regex ngây thơ** - mỗi
bộ hiểu đúng cú pháp chuỗi/comment của một ngôn ngữ: Java (chuỗi, ký tự, **text block `"""..."""`**),
YAML (`#` chỉ là comment khi ở đầu dòng hoặc sau khoảng trắng, giữ nguyên bên trong chuỗi có nháy -
mật khẩu `Tour@2026#IUH` sống sót), Thymeleaf/HTML (giữ `<!--/*/ ... /*/-->` nếu có - dự án này hoá ra
**không dùng** cú pháp đó hay `th:inline` ở đâu cả, xác nhận bằng `grep` toàn bộ `templates/` trước khi
xoá), JS (chuỗi, template literal `` `...${}...` ``, regex literal phân biệt với phép chia bằng ngữ
cảnh ký tự trước đó), CSS (giữ một dòng nhãn ngắn cho mỗi mục lớn của `app.css`, ví dụ `/* 1. Token
màu, chữ, khoảng cách, bo góc, đổ bóng */`, để cách gọi "mục N" trong `CLAUDE.md`/`PLAN.md` vẫn tra
được), `.properties` (chỉ dòng bắt đầu bằng `#`/`!` mới là comment, dòng nối bằng `\` ở cuối được giữ
nguyên là dữ liệu), SQL (`--`/`/* */` ngoài chuỗi `N'...'`/`'...'` và ngoài `[...]`), PowerShell (`#`/
`<# #>` ngoài chuỗi và here-string `@"..."@`).

**Nghiệm thu bằng số thật, không suy đoán:**
- **Kiểm chéo tự động cho cả 211 file**: chạy lại đúng bộ tách từ tố nói trên lên cả bản gốc (HEAD) và
  bản mới, so sánh phần "chỉ còn mã" sau khi bỏ mọi comment và chuẩn hoá khoảng trắng - **211/211 khớp
  tuyệt đối**, chứng minh không có ký tự mã nào bị đổi ở bất kỳ file nào, kể cả sau khi thêm hơn 270
  dòng chú thích một dòng.
- `./mvnw -o clean compile` sạch qua nhiều lần chạy giữa các bước sửa.
- App khởi động thật (`spring-boot:run`, cổng 8081): kết nối CSDL bằng đúng mật khẩu trong
  `application.yml` (lúc đó; nay là `application.properties`) (chứng minh `#` trong mật khẩu không bị cắt nhầm), `ddl-auto: validate` qua.
- `grep -c "</html>"` = 1 trên 13 trang đã kiểm bằng `curl` (`/`, `/tours`, `/tours/1`, `/login`,
  `/about`, `/contact`, `/admin`, `/admin/tours`, `/admin/bookings`, `/admin/promotions`,
  `/admin/reviews`, `/admin/contacts`, `/admin/users`, `/admin/categories`) - cả trước lẫn sau khi
  đăng nhập quản trị viên thật qua form (CSRF token lấy từ trang, không phải suy đoán).
- Trình duyệt thật (`mcp__Claude_Browser__*`): trang chủ, `/tours/1`, `/login`, `/admin` sau đăng nhập
  - **0 lỗi console** ở cả bốn trang; ô chọn ngày khởi hành ở `/tours/1` gọi
  `/api/tours/1/departures` bằng AJAX và vẽ đúng dữ liệu thật ("còn 29 chỗ" - khớp một đơn `curl` đã
  đặt trước đó trong phiên kiểm thử); biểu đồ doanh thu ở `/admin` đọc `/api/admin/stats/revenue` và vẽ
  đúng hai cột 08/2026, 09/2026 khớp số JSON trả về.
- `node --check` sạch cho `app.js` và `admin.js`.
- Ba file câu chữ: **756 khoá mỗi file, giống hệt HEAD** (đối chiếu bằng Python, key/value từng dòng).
- SQL: chạy `02_schema.sql` + `03_seed_data.sql` **đã xoá comment** trên một CSDL tạm
  (`TB_verify_new`), và bản gốc từ `git show HEAD:...` trên một CSDL tạm khác (`TB_verify_orig`), cả
  hai bằng Windows Auth (`-E -C -f 65001`) - **0 lỗi**, **16 bảng cả hai bên**, **số dòng từng bảng
  giống hệt nhau** (đối chiếu bằng một câu SELECT UNION ALL), `sys.check_constraints` = 0. Đã xoá cả
  hai CSDL tạm ngay sau khi so sánh xong, **không đụng `TourBookingDB`**.
- `git diff --stat`: **209 file đổi, +571 -6632 dòng**. Thêm mới: 274 dòng chú thích một dòng trong
  Java (lớp + hàm không hiển nhiên), 15 nhãn mục trong `app.css`, 5 dòng tóm tắt đầu file SQL, 1 dòng
  tóm tắt đầu file PowerShell.

**Bẫy mới gặp (không có trong 62 cái cũ):**
63. **`git diff` báo "LF will be replaced by CRLF" cho mọi file vừa sửa** - tưởng đâu tool tự ý đổi
    kiểu xuống dòng, hoá ra đây là hành vi `core.autocrlf=true` sẵn có của máy, áp dụng cho MỌI file
    Git thấy thay đổi bất kể nội dung gì; xác nhận bằng cách so `git show HEAD:<file>` (đọc bằng
    Python `open(...,'rb')`) với file hiện tại - cả hai đều **chỉ có LF**, không file nào bị đổi
    kiểu xuống dòng thật.
64. **Windows Python (`python.exe`) không hiểu đường dẫn POSIX `/tmp/...` của Git Bash** khi đường dẫn
    nằm trong một chuỗi `python -c "..."` (MSYS chỉ dịch đường dẫn cho đối số trực tiếp của lệnh, không
    dịch bên trong nội dung script truyền qua `-c`) - `open('/tmp/x.java')` ném
    `FileNotFoundError` dù `ls /tmp/x.java` thấy file bình thường. Sửa bằng `cygpath -w` đổi sang
    đường dẫn kiểu `C:\...` trước khi đưa vào script Python.
65. **Bộ tách từ tố cho comment khối `/* ... */` nếu chỉ "nuốt" ký tự mà không phát lại dấu xuống dòng
    bên trong sẽ nối hai dòng mã ở hai bên comment thành một dòng** - vô hại về mặt cú pháp (Java vẫn
    biên dịch đúng) nhưng làm số dòng file đổi khác thường và khó đối chiếu bằng mắt. Sửa: khi bỏ qua
    nội dung comment khối, mỗi ký tự `\n` gặp phải vẫn được ghi lại một `\n` vào kết quả, chỉ xoá phần
    chữ.

**Không dám xử lý / cố tình bỏ qua ngoài phạm vi 211 file:** `mvnw`, `mvnw.cmd`, `.mvn/**` (script của
Apache Maven) · `static/images/tours/NGUON_ANH.md` · `.gitignore`, `.gitattributes` · ảnh
`*.svg`/`*.png`/`*.jpg` · `docs/`, `PLAN.md`, `CLAUDE.md` - đúng theo phạm vi user đã chốt.

**Chưa làm hết trong lượt này, nói thẳng:** 274 dòng chú thích một dòng phủ **100% lớp/interface/enum/
record cấp cao nhất** của cả 156 file Java (kiểm bằng script quét `public class|interface|enum|record`
và dò ngược tìm dòng `//` ngay phía trên, xuyên qua annotation nhiều dòng) và các hàm nghiệp vụ phức
tạp nhất ở tầng `service`/`controller`/`repository` (đặc biệt `BookingService`, `PromotionService`,
`PaymentService`, `UserService`, `TourService`, `CheckoutController`, `DataSeeder`) - đây là nơi user
cần đọc hiểu nhất. Các lớp DTO/mapper/converter đơn giản chỉ có comment mức lớp (đúng tinh thần "hàm
hiển nhiên thì không comment", vì phần lớn thân hàm ở đó chỉ một dòng). Có **chưa** rà tay từng hàm
private nhỏ trong 41 file HTML (bản thân template không có khái niệm "hàm" nên không áp dụng) và chưa
đối chiếu hình ảnh (screenshot) từng trang một cách toàn diện như một bộ QA đầy đủ nhiều ngày mới làm
được - phần kiểm bằng trình duyệt thật giới hạn ở bốn trang đại diện (trang chủ, chi tiết tour, đăng
nhập, bảng điều khiển quản trị) nêu ở trên, không phải toàn bộ mọi trang trong hệ thống.


---

## Phase 15 — AI (Spring AI + vector search) và theo dõi hành vi người dùng

Thiết kế bám `docs/presentation/SpringAI_VectorDB_UserTracking.pptx` (Phần 2) nhưng **đã chỉnh theo thực tế**; hướng dẫn trình diễn ở `docs/presentation/HUONG_DAN_DEMO.md`.

### Quyết định (khác slide gốc)

| Hạng mục | Slide gốc | Thực tế đã làm | Vì sao |
|---|---|---|---|
| Phiên bản | Spring AI 2.0.2 | **Spring AI 1.1.8** qua `spring-ai-bom`, `spring-ai-starter-model-google-genai` (chat) + `spring-ai-starter-model-openai` (nhúng vector) + `spring-ai-vector-store` + `spring-ai-advisors-vector-store` | 2.0.x dùng Jackson 3 (`tools.jackson`) nên nhắm tới Boot 4; 1.1.8 là bản 1.x mới nhất (Maven Central), chạy với Boot 3.5.16 (đã biên dịch + chạy thật) |
| Nhà cung cấp | Ollama dev / OpenAI prod | **Google Gemini (gói miễn phí)**: **chat qua SDK Gemini gốc** (starter `google-genai`, giữ `thought_signature`, xem #80); **nhúng vector qua endpoint tương thích OpenAI**; đổi chat sang OpenAI/Ollama bằng `AI_CHAT_PROVIDER=openai` + biến môi trường | Chốt theo yêu cầu người dùng; một starter, một đường mã |
| Vector store | PgVector (prod) / Simple (dev) | **`SimpleVectorStore` lưu tệp `data/vector-store.json`** (lớp `TourVectorStore` kế thừa để có `ids()`/`clearAll()`) | Không có Postgres; SQL Server vẫn là nguồn gốc dữ liệu |
| ADD_TO_CART | `tracker.js` bắt submit form | **Ghi ở server** (`CartApiController`, `CartController`, tool `addToCart`) sau khi thêm giỏ **thành công** | Số liệu chính xác, không đếm cả lúc hết chỗ/lỗi |
| Bảng `user_events` | có `page_url`? không | thêm cột `page_url` (chỉ phần đường dẫn, bỏ query) | slide có nhắc cắt `pageUrl` |
| Công tắc AI | — | `app.ai.enabled` (`AI_ENABLED`) để trống = **tự bật khi có `GEMINI_API_KEY`** | app phải chạy đủ chức năng khi không có khoá |

### Cấu hình (`application.properties`, mọi thứ ghi đè được bằng biến môi trường)

`GEMINI_API_KEY` · `AI_ENABLED` · `AI_CHAT_PROVIDER` (`google-genai` mặc định, hoặc `openai`) · `AI_BASE_URL` (mặc định `https://generativelanguage.googleapis.com`; dùng cho nhúng vector, cho SDK Gemini và cho chat OpenAI-compat) · `AI_CHAT_TIMEOUT_MS` (60000) · `AI_CHAT_RETRY_DELAY_SECONDS` (2; SDK thử lại tối đa 3 lần với 429/503) · `AI_CHAT_PATH` (`/v1beta/openai/chat/completions`, chỉ khi `AI_CHAT_PROVIDER=openai`) · `AI_EMBEDDINGS_PATH` (`/v1beta/openai/embeddings`) · `AI_CHAT_MODEL` (`gemini-2.5-flash`; dự phòng hạn mức cao hơn: `gemini-2.5-flash-lite`) · `AI_EMBEDDING_MODEL` (`gemini-embedding-001`, 3072 chiều) · `AI_EMBEDDING_DIMENSIONS` (trống = mặc định của mô hình) · `AI_SIMILARITY_THRESHOLD` (0.5) · `AI_EMBEDDING_MIN_INTERVAL_MS` (700) · `AI_VECTOR_STORE_PATH`. Đổi chat sang OpenAI: `AI_CHAT_PROVIDER=openai`, `AI_BASE_URL=https://api.openai.com`, hai đường dẫn `/v1/...`, hai tên mô hình. Đổi sang Ollama: thêm `AI_CHAT_PROVIDER=openai`, `AI_BASE_URL=http://localhost:11434`, khoá giả `ollama`. ⚠️ Đường `openai` **không** giữ được `thought_signature` nên **không dùng cho Gemini 3** (xem #80). `GEMINI_API_KEY` và `AI_CHAT_MODEL` giữ nguyên ý nghĩa (đã thử mô hình trong báo lỗi: `gemini-3.5-flash-lite`, và mặc định `gemini-2.5-flash`).

### Lớp chính

- `config/`: `AiProperties`, `AiEnabledCondition`, `AiEnvironmentPostProcessor` (đăng ký ở `META-INF/spring.factories`), `AiConfig` (bean `Client` Google GenAI, `TourVectorStore`, `ChatMemory`, hai `ChatClient`: `tourAssistant` `@Primary` có tool và `tourAssistantRagOnly` không tool dùng cho dự phòng), `TrackingProperties`.
- `ai/`: `GenAiClientFactory` (dựng `Client` SDK Gemini: khoá, base-url, timeout, thử lại 429/503), `TourVectorService` (nạp/lập lại/upsert/search/similar), `TourDocumentFactory`, `ThrottledEmbeddingModel`, `SemanticSearchService`, `PriceHintParser`, `TourTools` (`searchTours`, `getDepartures`, `addToCart`), `AiChatService` (4xx ở vòng tool → thử lại **một lần không tool**, xem #80), `TourIndexInitializer` (lúc `ApplicationReadyEvent`), `TourIndexListener` + `TourChangedEvent` (đồng bộ sau commit khi admin sửa tour).
- `service/`: `TrackingService`, `BehaviorStatsService`, `RecommendationService`.
- `controller/`: `api/TrackingApiController` (`POST /api/tracking`), `api/ChatApiController` (`POST /api/chat`), `api/TourApiController#semantic` (`GET /api/tours/semantic`), `admin/AdminAnalyticsController` (`/admin/analytics`), `admin/AdminAiController` (`/admin/ai`, `POST /admin/ai/reindex` → redirect + flash).
- `entity/`: `UserEvent`, `EventType` + `EventTypeConverter`; `repository/UserEventRepository` (JPQL thuần, không native).
- Giao diện: `static/js/tracker.js`, `static/js/chat.js`, `fragments/chat-widget.html` (nằm trong fragment `scripts` nên mọi trang công khai đều có), khối "Có thể bạn thích" (`index.html`), "Tour tương tự" (`tour/detail.html`), công tắc "Tìm bằng AI" (`tour/list.html`), mục 15 của `app.css`.
- CSDL: `02_schema.sql` (+`user_events`, 17 bảng), `99_drop_all.sql`, **`05_user_events.sql`** (idempotent, cho CSDL đã có sẵn). `user_events` **không có khoá ngoại** (ghi nhanh, không cản xoá tour/tài khoản; cũng tránh lỗ hổng gotcha #42) nên xoá tour để lại dòng mồ côi vô hại — mọi truy vấn thống kê đều JOIN `tours`.

### Quy tắc phía server của tracking

`userId` lấy từ `SecurityContext`, `sessionId` từ `HttpSession` và **băm SHA-256** trước khi lưu (không lưu mã phiên đang sống); loại sự kiện chặt (enum), thiếu trường → 400; bot (User-Agent) → 202 nhưng không ghi; 60 sự kiện/phút/phiên (429); `TIME_ON_PAGE` < 0,5 s bị bỏ, > 30 phút bị cắt; từ khoá cắt 200 ký tự; `page_url` chỉ giữ đường dẫn; tour không tồn tại → bỏ; lỗi ghi CSDL → nuốt + log (tracking không được làm hỏng nghiệp vụ); `@Scheduled` 03:15 xoá sự kiện quá `retention-days` (180). `tracker.js` tôn trọng `Do Not Track`.

### Cách AI "tắt" hoạt động

`AiEnvironmentPostProcessor` đặt `spring.ai.model.chat/embedding=none` khi AI tắt → autoconfig OpenAI **và Google GenAI** của Spring AI không chạy (đã có kiểm thử: không khoá thì không có bean `Client`/`GoogleGenAiChatModel`); `AiConfig` (`@Conditional(AiEnabledCondition)`) không tạo `TourVectorStore`/`ChatClient`. Mọi service dùng `ObjectProvider` nên trả "không khả dụng" thay vì lỗi: tìm ngữ nghĩa → lùi về LIKE (bỏ cụm "dưới 5 triệu" khỏi câu và đổi thành bộ lọc giá), chatbot → 503 + câu thân thiện, widget hiện "AI chưa được cấu hình", "Tour tương tự"/"Có thể bạn thích" lùi về cùng danh mục / phổ biến.

### Đã kiểm bằng gì (đọc kỹ — đây là chỗ dễ nói quá)

- `./mvnw -q -DskipTests compile` sạch; `./mvnw test` = **86 kiểm thử xanh, 0 lỗi** (không cần SQL Server): parser giá, rate limiter, `TrackingService`, validate `TrackingRequest`, thống kê, gợi ý, `TourVectorService` với mô hình nhúng giả (lưu/nạp tệp, upsert, lọc giá, tour tương tự), `ThrottledEmbeddingModel`, `AiChatService` với `ChatModel` giả (kể cả dự phòng không tool, khôi phục bộ nhớ, log WARN), `TourTools`, `AiProperties`, `AiWiringTest` (dựng thật cấu hình Spring AI từ `application.properties`: chat = Google GenAI, nhúng = OpenAI-compat, chuyển `AI_CHAT_PROVIDER=openai`) và `GeminiToolLoopTest` (mô hình chat thật của Spring AI chạy với máy chủ giả ép luật chữ ký: đường OpenAI-compat bị 400 đúng như lỗi thật, đường SDK gốc qua; thử lại 429).
- `AiEnabledSmokeTest` / `AiDisabledSmokeTest`: **chạy cả ứng dụng thật** (Thymeleaf, Security + CSRF, `DataSeeder` 20 tour, JPA validate/create) trên **H2 (chế độ SQL Server)** với **máy chủ giả Gemini** (`FakeGeminiServer`: `POST /v1beta/models/{model}:generateContent` cho chat, `/v1beta/openai/embeddings` cho nhúng; chat **ép luật `thought_signature`** như Gemini 3): lập chỉ mục 20 tour, tìm ngữ nghĩa + lọc giá, tour tương tự, tracking đủ 5 loại + ca sai, vòng tool-calling thật (`searchTours` rồi `addToCart` vào giỏ, mỗi vòng đúng một chữ ký phát ra và được trả lại, 0 lần bị từ chối), dự phòng không tool khi nhà cung cấp luôn 400 ở yêu cầu có tool, các lỗi không dự phòng (401/403/500/400-hai-lần) → 503, thống kê admin, reindex PRG, phân quyền, và toàn bộ nhánh AI tắt/khoá rỗng/nhà cung cấp lỗi 500.
- **CHƯA kiểm**: SQL Server thật (H2 không bắt được lỗi riêng của SQL Server, xem #58 — nên chạy `05_user_events.sql` và mở `/admin/analytics` trên máy có SQL Server); **Gemini thật** (không gọi được từ môi trường làm việc; **chưa xác minh** rằng chữ ký thật sự được Gemini 3 trả về khi không bật `include-thoughts`, rằng lược đồ tool của Spring AI được Gemini nhận, và tài liệu ai.google.dev vì bị chặn mạng; đường dẫn, tên tham số, độ giống cosine thực tế của `gemini-embedding-001`, ngưỡng 0.5 là ước đoán cần chỉnh); giao diện bằng mắt/trình duyệt (không có trình duyệt trong môi trường này — widget chat, bố cục trang thống kê chưa được nhìn thấy).

### Hạn chế đã biết

Bộ nhớ hội thoại giữ trong RAM theo phiên (không giới hạn số phiên); mã phiên đổi khi đăng nhập nên phễu của khách ẩn danh bị tách làm hai; không có banner xin đồng ý theo dõi (chỉ tôn trọng DNT); `SimpleVectorStore` nạp cả tệp vào RAM, đủ cho vài trăm tour; tìm LIKE dự phòng vẫn khớp nguyên cụm; thứ tự sắp xếp của người dùng bị bỏ qua ở chế độ ngữ nghĩa (xếp theo độ giống); gói miễn phí của Gemini có thể dùng prompt để cải thiện sản phẩm Google — không đưa dữ liệu khách thật vào lúc demo.

### Gotcha mới

71. ⚠️ **Autoconfig OpenAI của Spring AI ném `OpenAI API key must be set` lúc khởi động** nếu khoá rỗng — và `${GEMINI_API_KEY:mặc-định}` **không cứu được** khi biến môi trường tồn tại nhưng để trống (placeholder chỉ dùng mặc định khi biến vắng mặt). Dòng "biến môi trường rỗng" trong cấu hình chạy IntelliJ là kịch bản thật. Cách xử lý: `AiEnvironmentPostProcessor` tắt `spring.ai.model.chat/embedding` khi AI tắt. Có kiểm thử (`AiWiringTest`).
72. ⚠️ **`SimpleVectorStore.add` gọi API nhúng TỪNG tài liệu một** (`embed(Document)`), không gộp lô — 20 tour = 20 request liên tiếp, dễ chạm 429 ở gói miễn phí. `ThrottledEmbeddingModel` giãn cách tối thiểu 700 ms; `spring.ai.retry.on-http-codes=429` (mặc định Spring AI **không** thử lại 4xx). Và không bao giờ nhúng lại lúc khởi động: tệp `data/vector-store.json` + `.meta.json` (khoá = nhà cung cấp|mô hình|số chiều; đổi một trong ba thì lập lại).
73. ⚠️ **Toán tử ba ngôi `cond ? Long.longValue() : Long` tự mở hộp (unbox) cả nhánh sau** → `NullPointerException` khi nhánh sau là `null`. Vấp thật ở `SemanticSearchService` (mọi tìm ngữ nghĩa không có giá tối thiểu đều lùi về từ khoá một cách im lặng); chỉ lộ ra nhờ kiểm thử. Dùng `Long.valueOf(...)` cho nhánh đầu.
74. ⚠️ **Lập chỉ mục khi khởi động phải chờ `DataSeeder`**: `ApplicationRunner @Order(100)` chạy trước `CommandLineRunner` không `@Order` nên lập chỉ mục **0 tour** rồi lưu tệp rỗng — lần khởi động sau nạp tệp rỗng và không bao giờ lập lại. Giờ dùng `ApplicationReadyEvent`, và tệp nạp lên mà rỗng thì lập lại.
75. **`@DynamicPropertySource` đến muộn hơn `EnvironmentPostProcessor`** (và khoá ngữ cảnh của Spring Test **không** gồm system property): kiểm thử cần biến sớm thì đặt bằng `System.setProperty` trong khối `static`, và đổi `@SpringBootTest(properties=...)` để hai lớp kiểm thử không dùng chung một ngữ cảnh đã cache.
76. **JSON có enum lạ (`"type":"HACK"`) ném `HttpMessageNotReadableException`** — `ApiExceptionHandler` cũ chỉ có handler `Exception` chung nên trả **500**. Đã thêm handler → 400.
77. **`app.js` khoá nút submit của MỌI form** sau khi gửi (trừ `[data-ajax-cart]`); form chat gửi bằng AJAX nên phải có `data-no-lock`, nếu không nút Gửi bị vô hiệu vĩnh viễn sau lần gửi đầu.
78. `SimpleVectorStoreContent` là lớp package-private nhưng trường `store` của `SimpleVectorStore` là `protected`: lớp con vẫn gọi được `store.keySet()/clear()` (không nêu tên kiểu), nhờ đó có `ids()`/`clearAll()` mà không phải sao chép thư viện.
79. ⚠️ **Cấu hình là `application.properties` (và `src/test/resources/application-h2.properties` cho kiểm thử) theo yêu cầu môn học — đừng quay lại YAML.** Đã chuyển nguyên trạng từ `application.yml`/`application-h2.yml` (93 + 9 khoá, so bằng `YamlPropertySourceLoader` và `PropertiesPropertySourceLoader`: 0 khác biệt, không có tài liệu `---` nào). **Bảng mã:** Spring Boot 3.5.16 đọc `.properties` bằng `OriginTrackedPropertiesLoader` theo **ISO-8859-1** (xem `javap -c` lớp `CharacterReader`: `StandardCharsets.ISO_8859_1`; thử thật: `a=Việt Nam` ra `Viá»t Nam`, còn `b=Vi\u1EC7t Nam` ra đúng) — khác YAML (luôn UTF-8) và khác `messages*.properties` (đã khai `spring.messages.encoding=UTF-8`). Hệ quả: **comment** tiếng Việt trong file thì vô hại, nhưng **giá trị** có dấu phải viết `\uXXXX` (hiện mọi giá trị đều ASCII). Khác biệt cú pháp cần nhớ: `#`/`!` chỉ là comment ở đầu dòng nên mật khẩu `Tour@2026#IUH` nguyên vẹn; `key=` (rỗng) cho chuỗi rỗng giống `key:` trong YAML; mọi giá trị là chuỗi (`true`, `5s`, `30m` do Spring Boot tự ép kiểu khi bind); không có `---`, hồ sơ riêng thì tách tệp `application-<profile>.properties`. Bẫy build: Maven **không xoá** `target/classes/application.yml` cũ sau khi đổi tên — nó vẫn bị nạp chung; xoá tay hoặc `./mvnw clean`.
80. ⚠️ **Gemini 3 bắt buộc gửi lại `thought_signature` của function call trong cùng lượt; lớp OpenAI của Spring AI 1.1.8 làm rơi nó → `HTTP 400 Function call is missing a thought_signature ... position N`.** Gặp thật khi chạy trên Windows với khoá miễn phí và `AI_CHAT_MODEL=gemini-3.5-flash-lite`: chatbot hỏng ngay khi mô hình gọi tool (`searchTours`). Cơ chế: endpoint tương thích OpenAI trả chữ ký ở `tool_calls[].extra_content.google.thought_signature`; Spring AI chuyển phản hồi vào POJO không có trường đó rồi dựng lại tin nhắn `assistant` cho vòng tool nên yêu cầu kế tiếp thiếu chữ ký. Tái hiện bằng `GeminiToolLoopTest` (máy chủ giả ép luật). **Cách sửa:** chat đi qua `spring-ai-starter-model-google-genai` (SDK `google-genai` 1.37.0, khoá API thường); bằng `javap` trên `GoogleGenAiChatModel` thấy nó lưu `Part.thoughtSignature()` vào metadata `thoughtSignatures` của `AssistantMessage` và gắn lại vào `functionCall` part khi dựng yêu cầu tiếp theo; tài liệu Spring AI `google-genai-chat.adoc` (tag v1.1.8) mô tả cùng hành vi. Nhúng vector giữ nguyên starter OpenAI (cùng mô hình/số chiều/`AI_BASE_URL` ⇒ khoá chỉ mục không đổi, **không lập lại chỉ mục**). **Đừng bật `spring.ai.google.genai.chat.options.include-thoughts`**: tài liệu Spring AI ghi "bắt buộc cho Gemini 3 Pro", nhưng bản 1.1.8 không lọc `Part.thought` nên mỗi phần tóm tắt suy nghĩ thành một `Generation` riêng và có thể lọt vào câu trả lời (đọc bytecode, chưa chạy với Gemini thật; nếu sau này thấy Gemini 3 không trả chữ ký khi tắt cờ này thì phải tự lọc phần `thought`). Không dùng mẹo chữ ký giả `skip_thought_signature_validator` (chỉ biết qua bài viết cộng đồng, không xác minh được trong tài liệu chính thức vì ai.google.dev bị chặn). **Dự phòng** (cho bất kỳ lỗi 4xx nào tương tự về sau): `AiChatService` thử lại đúng một lần bằng `tourAssistantRagOnly` (RAG, không tool, nhắc khách dùng trang `/tours`), log WARN, trả lại bộ nhớ hội thoại về trạng thái trước khi thử (advisor bộ nhớ đã ghi tin nhắn của khách), **trừ 401/403/429** (bỏ tool không cứu được) và 5xx.
81. **SDK Gemini gốc không thừa hưởng cấu hình của Spring AI:** (a) `RetryTemplate`/`spring.ai.retry.*` chỉ thử lại `TransientAiException`, còn lỗi 429 của SDK là `ApiException` nên **không** được thử lại → `GenAiClientFactory` đặt `HttpRetryOptions` (3 lần, giãn cách luỹ thừa, mã 429/503) và timeout; (b) autoconfig không có thuộc tính base-url/timeout nên `AiConfig` tự khai bean `Client` (autoconfig lùi lại); (c) có hai `ChatClient` (`tourAssistant` `@Primary` + `tourAssistantRagOnly` có `@Qualifier`) nên chỗ nào chỉ cần "ChatClient" mặc định vẫn lấy được bản có tool; (d) thông báo lỗi OpenAI-compat dạng `400 - [...]` (hoặc `HTTP 400 - [...]` khi có `on-http-codes`), còn SDK ném `com.google.genai.errors.ApiException` mang `code()` — `AiChatService.clientErrorStatus` đọc cả hai; (e) lần đầu build sau khi pull phải **có mạng** (thêm ~25 jar: `google-genai`, `okhttp`, `google-auth-library`, `protobuf-java`, `guava`...), chạy `./mvnw clean ...` không `-o`; (f) nhiệt độ 0.3 là của bản cũ — Google khuyến nghị giữ mặc định cho Gemini 3 (chưa xác minh được tài liệu), nếu thấy trả lời lặp/kém thì thử bỏ `spring.ai.google.genai.chat.options.temperature`.

---

## Cấu trúc thư mục

```
project_www/
├── PLAN.md                 ← file này
├── CLAUDE.md               ← quy ước ngắn cho agent
├── .claude/skills/         ← taste-skill (12 skill) + impeccable
├── pom.xml, mvnw, .mvn/
├── database/               00_enable_sqlserver_tcp.ps1 · 01_create_database.sql ·
│                           02_schema.sql (artifact được chấm) · 03_seed_data.sql ·
│                           04_sample_queries.sql · 99_drop_all.sql
├── docs/                   uml/ (TourBooking.vpp + 44 PNG + tools/) · screenshots/ (10 ảnh báo cáo) ·
│                           2 file tài liệu môn học
├── uploads/tours/          runtime, .gitignore
└── src/main/
    ├── java/vn/edu/iuh/fit/tourbooking/
    │   ├── config/         SecurityConfig · WebMvcConfig · I18nConfig · ValidationConfig ·
    │   │                   AppProperties · DataSeeder
    │   ├── entity/         16 entity + Auditable(@MappedSuperclass) + 8 enum
    │   ├── entity/converter/  8 converter (BẮT BUỘC, xem #6)
    │   ├── repository/     16 repository + spec/TourSpecifications
    │   ├── service/        Cart · Tour · Category · Booking · User · Departure · Statistics ·
    │   │                   FileStorage · Promotion · Email(+impl)
    │   ├── session/        Cart · CartItem      ← POJO, KHÔNG phải entity
    │   ├── dto/            form/ · view/ · ApiError
    │   ├── mapper/         ViewUrls · TourMapper · CategoryMapper · DepartureMapper · CartMapper
    │   ├── controller/     web/ · admin/ · api/ · LanguageController
    │   ├── security/       CustomUserDetails(Service) · LoginSuccessHandler · ApiErrorWriter ·
    │   │                   ApiAuthenticationEntryPoint · ApiAccessDeniedHandler
    │   ├── exception/      GlobalExceptionHandler (web) · ApiExceptionHandler (api) ·
    │   │                   BusinessRuleException · ResourceNotFoundException
    │   ├── validation/     @UniqueEmail · @PasswordsMatch · @ValidPaymentMethod
    │   └── util/           SlugUtil · CodeGenerator · MessageHelper · SafeRedirect
    └── resources/
        ├── application.properties
        ├── messages{,_vi,_en}.properties   ← 712 khoá, khớp nhau
        ├── static/  css/app.css · js/{app.js,admin.js} · images/
        └── templates/  fragments/ · tour/ · cart/ · auth/ · checkout/ · account/ ·
                        admin/{tour,departure,category,user,booking}/ · email/ · error/
```

---

## Việc còn lại

1. **`docs/screenshots/`** nay có **10 ảnh** cho báo cáo, tất cả 1440px và dữ liệu thật:
   `screenshot-trang-chu`, `-danh-sach-tour`, `-chi-tiet-tour`, `-thanh-toan`, `-admin-dashboard`,
   `-admin-tour`, `-admin-review` (lọc "Chờ duyệt", 1 dòng thật), `-contact`, `-admin-contact` (lọc
   "Mới", 3 dòng thật), và `-409` (phản hồi 409 thật của `DELETE /api/admin/promotions/1`, mã
   `HELLO2026` đã có lượt dùng - bằng chứng "chặn xoá kiểm trong chương trình chứ không phải trong
   CSDL" mà mục này từng ghi là còn thiếu, nay đã có ở Phase 7.6).
2. **Kiểm thử tự động**: `pom.xml` **chưa có** `spring-boot-starter-test`, phải thêm (lần chạy đầu
   cần mạng). Dự kiến: `@DataJpaTest` cho các truy vấn đếm chặn xoá · `@WebMvcTest + @WithMockUser`
   chứng minh phân quyền · `@SpringBootTest + MockMvc` cho luồng đặt tour · `CartServiceTest` thuần
   JUnit cho quy tắc "0 khách thì xoá dòng".
3. **README.md** hướng dẫn dựng lại từ số không.
4. File `Phieu DK...docx` đã nằm trong `docs/` ✅.
5. 🔶 **Tám trong chín bảng mở rộng có tầng giao diện VÀ có nơi ghi vào thật** (không chỉ
   `countBy...` cho quy tắc chặn xoá như trước Phase 7.6) - **`booking_passengers` là ngoại lệ, xem
   cảnh báo riêng ngay dưới bảng này:**
   - `promotions`/`coupon_usages` → `/admin/promotions` (Phase 7.4, CRUD + xem lượt dùng + chặn xoá).
   - `reviews` → đánh giá tour ở trang chi tiết tour (UC018, Phase 7.4) **và kiểm duyệt** ở
     `/admin/reviews` (UC020, Phase 7.5).
   - ✅ **`booking_passengers` nay ĐÃ có nơi ghi vào thật** (Phase 12, mục 12.7 - SPEC_CHUNG.md đã
     chốt đây là bản sửa **có chủ đích** của luồng UC007, không phải vá lỗ hổng ngoài kế hoạch như
     cảnh báo trước đây từng ghi). `/checkout` dựng đúng N+M ô hành khách theo từng dòng giỏ hàng
     (`CheckoutController.buildPassengerGroups`), `BookingService.placeOrder` ghi hành khách **trong
     cùng giao dịch** với đơn (`BookingService.attachPassengers`/`toPassenger`), và màn quản trị sửa
     số khách (`BookingService.updateDetailQuantity`) giữ đúng bất biến số hành khách theo loại luôn
     khớp `num_adults`/`num_children` - xem chi tiết ở Phase 12 bên dưới. Vế **hiển thị** (Phase 7.4)
     vẫn giữ nguyên, không đổi.
   - `contact_messages` → `/contact` (UC021) + `/admin/contacts` (UC022, Phase 7.6).
   - `payments` → sinh dòng `PENDING` lúc checkout, quản trị viên đánh dấu `PAID` (Phase 7.6).
   - `booking_status_history` → ghi mỗi lần đổi trạng thái đơn kể cả dòng đầu lúc đặt, hiện dòng
     thời gian ở cả hai phía (Phase 7.6).
   - `password_reset_tokens` là bảng **duy nhất** còn "chết" theo đúng chủ đích (xem quyết định trong
     `docs/report/SPEC_CHUNG.md` mục 9: "tạm để lại, lần này vá ba bảng đầu") - không có luồng "quên
     mật khẩu" nào phát hành vé cho bảng này. **Không liên quan tới việc `EmailService` có gửi SMTP
     thật hay không** (Phase 9 đã thêm `SmtpEmailService` + thư chào mừng đăng ký, chế độ mặc định vẫn
     là console). Bảng này bị xoá theo người dùng khi xoá tài khoản (`UserService.delete`), không chặn
     xoá.
6. ✅ Quy tắc "không xoá quản trị viên cuối cùng" **vẫn chưa kích hoạt bằng thao tác thật** với đúng
   ngữ nghĩa gốc (hạ admin gốc xuống CUSTOMER rồi thử xoá) - nhưng Phase 7.6 đã tạo sẵn
   `admin2@tourbooking.vn` (mật khẩu `123456`) cho việc này, và đã dùng tài khoản đó để chứng minh một
   quy tắc chặn xoá KHÁC (`error.user.delete.hasBookingHistory`) bằng dữ liệu không phải seed. Muốn
   chứng minh nốt quy tắc "admin cuối" thì: đăng nhập `admin2`, đổi vai trò `admin@tourbooking.vn`
   thành CUSTOMER qua `/admin/users`, rồi thử xoá `admin2` (lúc đó không đơn/đánh giá/lịch sử nào
   chặn, quy tắc "admin cuối" sẽ là quy tắc duy nhất còn chặn - nhưng vì `admin2` **đã có** lịch sử
   đổi trạng thái đơn từ Phase 7.6 nên thực ra quy tắc `hasBookingHistory` sẽ chặn trước; cần tạo
   admin thứ ba "sạch" nếu muốn cô lập đúng một quy tắc "admin cuối").
7. ✅ ~~Đánh giá tour duyệt ngay~~ — đã sửa ở Phase 7.5: `approved = false` khi gửi, chờ duyệt ở
   `/admin/reviews` đúng thiết kế gốc của entity.
8. Biểu đồ doanh thu trong `screenshot-admin-dashboard.png` trống (chỉ ảnh hưởng ảnh chụp tĩnh dùng
   cho báo cáo, không phải lỗi ứng dụng - xem chi tiết ở Phase 7.4).

### Lệnh đối chiếu ba file câu chữ *(chạy trong `src/main/resources`)*

```bash
export PYTHONIOENCODING=utf-8 && python - <<'PY'
import pathlib
def keys(p):
    out = []
    for line in pathlib.Path(p).read_text(encoding='utf-8').splitlines():
        s = line.strip()
        if s and not s.startswith('#') and '=' in s:
            out.append(s.split('=', 1)[0].strip())
    return out
vi, en, v2 = keys('messages.properties'), keys('messages_en.properties'), keys('messages_vi.properties')
print('VI:', len(vi), '| EN:', len(en), '| vi_file:', len(v2))
print('Trung khoa:', sorted({k for k in vi if vi.count(k) > 1}) or 'khong')
print('Lech khoa :', sorted(set(vi) ^ set(en)) or 'khong')
PY
```

---

## Phase 11 — Nghiệp vụ đợt B (SPEC_CHUNG.md mục 12.1-12.6, 12.9)

Subagent NGHIỆP VỤ — ĐỢT B. Làm 12.1 (máy trạng thái), 12.2 (hoàn tiền), 12.3 (UC023 khách tự huỷ),
12.4 (tự huỷ quá hạn), 12.5 (hạn chót đặt), 12.6 (khoá sửa đợt có khách), 12.9 (thống kê). Không làm
12.7 (nhập hành khách) và 12.8 (trang chi tiết riêng) — để đợt sau như phân công.

**File đã sửa (26 file + 1 file mới):**
- `application.yml` (nay là `application.properties`) — thêm khối `app.booking.*` (cutoff-days=2, pending-expiry-hours=24,
  self-cancel-min-days=3, full-refund-min-days=7, partial-refund-percent=50).
- `config/AppProperties.java` — thêm record lồng `Booking`.
- `TourBookingApplication.java` — thêm `@EnableScheduling`.
- `entity/BookingStatus.java` — thêm `canTransitionTo(target)` (chỉ kiểm cấu trúc, bốn chuyển hợp lệ,
  COMPLETED/CANCELLED là cuối).
- `entity/TourDeparture.java` — `isBookable()` đổi chữ ký thành `isBookable(int cutoffDays)`.
- `entity/Promotion.java` — thêm `decreaseUsedCount()`.
- `repository/TourDepartureRepository.java` — `findBookable`/`findNextBookable` nhận mốc ngày đã cộng
  cutoff thay vì `today` suông.
- `repository/BookingRepository.java` — thêm `findDetailById`, `findExpiredPendingIds`; đổi
  `sumTotalAmountExcludingStatus`/`revenueByMonth` thành lọc theo tập trạng thái được tính (IN)
  thay vì loại trừ một trạng thái (mục 12.9).
- `repository/BookingDetailRepository.java` — `findTopSellingTours` lọc theo trạng thái đơn.
- `mapper/DepartureMapper.java` — nhận `AppProperties` để tính `bookable` đúng cutoff.
- `service/BookingService.java` — viết lại `updateStatus` (gỡ nhánh khôi phục, thêm kiểm
  `canTransitionTo` + điều kiện phụ COMPLETED), thêm `validNextStatuses`, `evaluateSelfCancel`,
  `cancelBySelf` (UC023), `expirePendingBooking` (mục 12.4), `releaseHold` dùng chung cho mọi nhánh
  huỷ (trả chỗ + trả lượt mã).
- `service/BookingExpiryScheduler.java` (mới) — `@Scheduled` mỗi 15 phút, mỗi đơn một giao dịch
  riêng (cố ý không `@Transactional` ở phương thức lập lịch).
- `service/PaymentService.java` — thêm `paidTotal`, `refundIfPaid` (mục 12.2, chỉ THÊM dòng
  `REFUNDED`); xoá `restorePendingForBooking` (chết theo nhánh khôi phục đã gỡ).
- `service/PromotionService.java` — thêm `releaseUsage(booking)` (khoá bi quan + `refresh()` giống
  `recordUsage`, xem gotcha #60).
- `service/CartService.java`, `service/TourService.java` — áp `cutoffDays` từ `AppProperties` vào mọi
  đường: thêm giỏ, đợt gần nhất tự chọn, danh sách đợt còn đặt được.
- `service/StatisticsService.java` — dùng tập trạng thái `REVENUE_STATUSES = {CONFIRMED, COMPLETED}`.
- `service/DepartureService.java` — mục 12.6: khoá đổi ngày khởi hành khi còn đơn chưa huỷ (quy tắc
  "không hạ số chỗ dưới số đã bán" vốn đã có sẵn từ trước, không phải viết mới).
- `controller/admin/AdminBookingController.java` — đẩy `validNextStatuses` thay vì toàn bộ enum.
- `controller/web/AccountController.java` — thêm `POST /account/bookings/{code}/cancel` (UC023) +
  đẩy `selfCancelPolicy` vào model GET.
- `templates/admin/booking/detail.html` — ô chọn trạng thái chỉ liệt kê `validNextStatuses`; gỡ đoạn
  chú thích "khôi phục".
- `templates/account/booking-detail.html` — thêm khối UC023 (số tiền hoàn xem trước + hộp xác nhận),
  và vá một lỗi có sẵn từ trước: trang này thiếu hẳn `fragments/components :: alerts` nên
  flash `successMessage`/`errorMessage` (kể cả của UC018 gửi đánh giá) chưa từng hiện ra — vá luôn vì
  luồng huỷ đơn mới thêm phụ thuộc đúng cơ chế đó.
- `static/js/app.js` — mọi `<form data-confirm="...">` hiện hộp `confirm()` trước khi gửi (lớp tăng
  cường, tắt JS vẫn gửi thẳng).
- `messages.properties` / `messages_vi.properties` / `messages_en.properties` — 712 → 728 khoá,
  vẫn khớp cả ba (lệnh đối chiếu cuối file cho ra 0 lệch). Xoá 1 khoá chết
  (`error.booking.restoreNoSeats`), thêm 17 khoá mới.
- `database/03_seed_data.sql` — sửa bẫy dữ liệu mẫu mục 12.4 (xem bên dưới).

### Bẫy dữ liệu mẫu — đã sửa và verify bằng số thật

Hai đơn `PENDING` mẫu (id 2, id 4) trước đó đặt cách đây 5 ngày và 2 ngày — quá xa ngưỡng mặc
định `pending-expiry-hours=24`. Đổi cột mốc thời gian của khối VALUES trong bước 6 từ số ngày
(`DATEADD(DAY, ...)`) sang số giờ (`DATEADD(HOUR, ...)`), giữ nguyên ý nghĩa cho ba đơn còn lại
(nhân `* 24`), riêng hai đơn PENDING đổi thành 5 giờ và 3 giờ trước — còn nguyên trong hạn.

### Dựng lại CSDL từ đầu — theo yêu cầu của user, đã làm và verify số thật

`99_drop_all.sql` → `02_schema.sql` → `03_seed_data.sql` (đều `-f 65001`, đọc lỗi bằng
`grep -iE "msg|error"`, không dòng nào khớp). `99_drop_all.sql` không đụng `sysdiagrams` +
7 procedure `sp_*diagram*` + `fn_diagramobjects` do SSMS sinh — xoá riêng bằng `DROP TABLE/PROCEDURE/
FUNCTION IF EXISTS` sau khi seed xong. Phép kiểm cuối `02_schema.sql` (số thật, sau khi xoá cả SSMS):

| Chỉ tiêu | Số đo |
|---|---|
| Số bảng (`sys.tables`) | 16 |
| CHECK constraint | 0 |
| Function | 0 |
| Stored procedure | 0 |
| Trigger | 0 |

Mất `Diagram_0` cùng dữ liệu test của các lượt trước (`admin2@tourbooking.vn`, các đơn
`TB2026092…` cũ) — user đã đồng ý đánh đổi này.

### Nghiệm thu — số thật, đo trực tiếp trên CSDL vừa dựng lại

1. `clean compile` BUILD SUCCESS · app khởi động sạch với `ddl-auto=validate` (không đổi cột nào,
   chỉ đổi phương thức Java — đúng thiết kế "không cần đổi schema").
2. Máy trạng thái (`curl` thật qua `/admin/bookings/{code}/status`, đọc lại DB sau mỗi lần):
   - `COMPLETED → CANCELLED` (đơn `TB20260814LM3D8R`) → chặn, thông báo
     `Không thể chuyển đơn từ trạng thái "Hoàn thành" sang "Đã huỷ".`, trạng thái DB không đổi.
   - `CANCELLED → CONFIRMED` (đơn `TB20260903ZC5H7J`) → chặn tương tự, DB không đổi.
   - `PENDING → COMPLETED` (đơn `TB20260923QT7B9X`) → chặn, DB không đổi.
   - `CONFIRMED → COMPLETED` khi chưa tới ngày khởi hành (đơn `TB20260923QT7B9X` sau khi
     `PENDING→CONFIRMED` hợp lệ) → chặn với thông báo khác hẳn:
     `Chỉ chuyển sang "Hoàn thành" được khi đã tới hoặc qua ngày khởi hành.`
   - `CONFIRMED → COMPLETED` khi đã tới ngày nhưng chưa có khoản PAID (lùi ngày khởi hành đợt đó
     về hôm qua bằng SQL, đơn chỉ có payment `PENDING`) → chặn với thông báo thứ ba:
     `Chỉ chuyển sang "Hoàn thành" được khi đơn đã có khoản thanh toán đã thu tiền.`
   - Đánh dấu payment đó `PAID` qua đúng nút "Đã thu tiền" rồi thử lại `CONFIRMED → COMPLETED` →
     thành công, đơn sang `COMPLETED`.
   - Màn quản trị: đơn `PENDING` chỉ hiện hai lựa chọn `CONFIRMED`/`CANCELLED` trong ô chọn (không có
     `COMPLETED`); đơn ở trạng thái cuối thì ô chọn biến mất, hiện câu "Đơn đã ở trạng thái cuối,
     không thể đổi trạng thái nữa."
3. Hoàn tiền (12.2): admin huỷ đơn `TB20260911HK4M2P` (đang `CONFIRMED`, đã `PAID` 50% =
   8.086.500 đ) → dòng `PAID` giữ nguyên, dòng `PENDING` còn lại chuyển `FAILED`, dòng `REFUNDED`
   mới đúng 8.086.500 đ (100%) với ghi chú "Quản trị viên huỷ đơn - hoàn 100% khoản đã thu.". Bằng
   chứng: truy vấn `payments WHERE booking_id=1` trả về đúng 3 dòng PAID/FAILED/REFUNDED. Đơn này còn
   đang dùng mã `HELLO2026` — huỷ xong `coupon_usages` mất dòng, `promotions.used_count` giảm 1→0
   (hành vi mở rộng hợp lý ngoài yêu cầu tối thiểu của 12.2, áp dụng thống nhất cho mọi nhánh huỷ).
4. UC023 (12.3) — dựng 3 đơn thật cho `an.nguyen@gmail.com` ở ba đợt khởi hành +8/+4/+1 ngày, đều
   đã `PAID` 8.000.000 đ:
   - Đợt +8 ngày (`TBSELFCANCEL7D`) → hộp xem trước hiện đúng "hoàn 100%" / "8.000.000 ₫", tự huỷ
     thành công, `REFUNDED` = 8.000.000, chỗ trả lại đủ.
   - Đợt +4 ngày (`TBSELFCANCEL4D`, có gắn mã `VIP20`) → hộp xem trước hiện "hoàn 50%" /
     "4.000.000 ₫", tự huỷ thành công, `REFUNDED` = 4.000.000 đúng 50%; `coupon_usages` mất dòng,
     `VIP20.used_count` giảm 1→0.
   - Đợt +1 ngày (`TBSELFCANCEL1D`) → không có nút huỷ, chỉ hiện câu hướng dẫn liên hệ tổng đài;
     gửi thẳng `POST /cancel` (bỏ qua giao diện) vẫn bị chặn ở tầng service với thông báo
     `Đơn đã cách ngày khởi hành dưới 3 ngày...`, trạng thái DB không đổi.
   - `binh.tran@gmail.com` mở `/account/bookings/TBSELFCANCEL1D` (đơn của `an.nguyen`) → 403; gửi
     `POST /cancel` cho đơn đó → cũng 403.
   - Phát hiện kèm vá: `account/booking-detail.html` thiếu `fragments/components :: alerts` từ trước
     (không phải lỗi mới) — flash message (kể cả lỗi UC023 lẫn UC018 cũ) chưa từng hiện ra; đã thêm.
5. Tự huỷ quá hạn (12.4) — dựng đơn `TBEXPIRYTEST` cho `cuong.le@gmail.com`, `booking_date` lùi
   30 giờ (quá ngưỡng 24 giờ), chỉ có payment `PENDING` (chưa `PAID`). Khởi động lại app (bộ hẹn giờ
   có `initialDelay=1 phút`) → log thật:
   `Bộ hẹn giờ mục 12.4: 1 đơn CHỜ quá 24 giờ, bắt đầu tự huỷ` /
   `Đơn TBEXPIRYTEST tự huỷ do quá hạn thanh toán (quá 24 giờ)`. Kết quả DB: `CANCELLED`, payment
   `PENDING→FAILED`, chỗ trả lại đủ (28→30), `booking_status_history.changed_by_id = NULL`, lý do
   "Quá hạn thanh toán". Hai đơn PENDING mẫu (5 giờ, 3 giờ trước) không bị đụng tới.
6. Hạn chót đặt (12.5) — chèn tạm hai đợt khởi hành +1 ngày và +3 ngày cho tour 1: đợt +1 ngày
   không xuất hiện trong `GET /api/tours/1/departures` lẫn ô chọn ngày ở `/tours/1`; `POST
   /api/cart/items` với đợt đó → 409 `error.cart.notBookable`. Đợt +3 ngày xuất hiện đủ và thêm
   giỏ thành công (200). 60 đợt khởi hành mẫu gốc (+14/+30/+50 ngày) đều còn đặt được sau khi áp luật.
7. Khoá sửa đợt (12.6) — đợt id 4 (tour Nha Trang, đã có 2 khách của đơn `TB20260923QT7B9X`):
   đổi ngày khởi hành → chặn, thông báo `Không đổi được ngày khởi hành: đợt này đã có 2 khách đặt
   (đơn chưa huỷ)...`; hạ tổng chỗ xuống 1 (< 2 đã bán) → chặn `Không hạ được tổng số chỗ xuống dưới
   2...`; sửa giá cùng ngày cũ → thành công (302, giá trong DB đổi đúng).
8. Thống kê (12.9) — `GET /api/admin/stats/overview` sau khi có đủ 4 trạng thái
   (`PENDING`=19.136.000, `CONFIRMED`=8.000.000, `COMPLETED`=26.446.000, `CANCELLED`=56.323.000) trả
   `revenue = 34.446.000` = đúng tổng `CONFIRMED + COMPLETED`, không cộng `PENDING`/`CANCELLED`.
   `GET /api/admin/stats/top-tours` khớp truy vấn SQL lọc cùng điều kiện trạng thái.
9. Câu chữ: ba file `messages*.properties` đối chiếu 0 khoá lệch, 728 khoá mỗi file.
   `grep -c "</html>"` = 1 trên mọi trang đã sửa (`admin/booking/detail`, `account/booking-detail`,
   `admin/departure/list`, `admin/bookings`, `admin` dashboard, `admin/tours`) — không trang nào cụt.
   Không có dòng `ERROR`/`Exception` nào trong log ứng dụng suốt phiên kiểm thử (trừ một dòng vô hại
   `Exception loading sessions from persistent storage` của Tomcat lúc devtools restart — không liên
   quan tới mã đã sửa).

### Chưa làm / chưa kiểm được trong đợt này
- 12.7 (nhập hành khách khi thanh toán) và 12.8 (trang chi tiết riêng cho admin) — **đã làm ở
  Phase 12** (xem mục riêng bên dưới), gạch ở đây theo đúng ghi chép gốc của Phase 11.
- Chưa test đồng thời hai request tự huỷ quá hạn chạy song song thật (chỉ verify logic + đọc mã: mỗi
  đơn một giao dịch riêng, bắt lỗi từng đơn - xem Javadoc `BookingExpiryScheduler`).
- Dữ liệu test còn để lại trong CSDL (không xoá, dùng làm bằng chứng): đơn `TB20260911HK4M2P`
  (CANCELLED, đã hoàn tiền), `TB20260923QT7B9X`/`TB20260814LM3D8R` (COMPLETED), `TBSELFCANCEL7D`/
  `TBSELFCANCEL4D` (CANCELLED, đã tự huỷ), `TBSELFCANCEL1D` (CONFIRMED, còn nguyên vì bị chặn huỷ),
  `TBEXPIRYTEST` (CANCELLED do hệ thống), ba đợt khởi hành phụ id 63/64/65 (tour Côn Đảo) dùng cho
  test UC023, đợt id 4 (tour Nha Trang) đã bị lùi ngày khởi hành về quá khứ để phục vụ test mục 6.

### Gotcha mới

63. `@Scheduled` không có thuộc tính `initialDelayUnit` — chỉ có `timeUnit` dùng chung cho cả
    `fixedDelay`/`fixedRate`/`initialDelay`. Viết `initialDelayUnit = TimeUnit.MINUTES` là lỗi biên
    dịch `cannot find symbol`, dễ đoán nhầm vì `@Scheduled` có nhiều biến thể thuộc tính trông giống
    nhau.
64. Bộ hẹn giờ `@Scheduled` gọi một phương thức `@Transactional` của bean khác thì KHÔNG được bọc
    thêm `@Transactional` ở chính phương thức lập lịch - nếu bọc, mọi lời gọi bên trong vòng lặp sẽ
    gia nhập lại đúng MỘT giao dịch bao ngoài (`REQUIRED` mặc định), phá vỡ yêu cầu "mỗi đơn một giao
    dịch riêng để một đơn lỗi không kéo cả mẻ". Không có giao dịch bao ngoài thì mỗi lời gọi sang bean
    khác tự mở giao dịch mới của riêng nó - đây là hành vi ĐÚNG cần giữ, nên phương thức lập lịch phải
    cố ý không có `@Transactional`.
65. Một trang có `errorMessage`/`successMessage` flash attribute nhưng thiếu hẳn
    `th:replace="~{fragments/components :: alerts}"` thì không lỗi gì cả lúc biên dịch hay chạy -
    POST vẫn set flash, redirect vẫn xảy ra, trang đích vẫn trả 200 - chỉ là thông báo không bao giờ
    hiện lên. Vấp ở `account/booking-detail.html`: thiếu từ trước (ảnh hưởng cả lỗi UC018 gửi đánh giá
    cũ), lộ ra khi thêm luồng UC023 mới cũng đi qua đúng cơ chế đó. Cách rà toàn site: mọi khuôn mẫu
    có controller set `errorMessage`/`successMessage` phải có dòng `th:replace` đó ở đâu đó.
66. `sqlcmd` (thực thi Windows) không đọc được đường dẫn kiểu Git Bash `/tmp/...` khi chạy `-i` -
    báo `Access is denied` dù cú pháp không sai. Phải ghi file SQL tạm vào một thư mục nằm trong cây
    Windows thật (ví dụ trong chính thư mục dự án), không dùng `/tmp` của Git Bash.
67. ⚠️ **`th:field` KHÔNG dùng SpEL để tính chỉ số trong ngoặc vuông.** Viết
    `th:field="*{passengerGroups[itemStat.index].adults[pStat.index].fullName}"` biên dịch trang
    được nhưng vỡ ngay khi render: `SpringInputGeneralFieldTagProcessor` đưa chuỗi
    `"passengerGroups[itemStat.index]"` thẳng cho `BeanWrapperImpl` phân tích như một CHUỖI KÝ TỰ,
    không phải biểu thức - `BeanWrapperImpl` cố ép `"itemStat.index"` thành số nguyên và ném
    `NumberFormatException`. Khác hẳn `th:each="p, pStat : *{...}"` (một biểu thức SpEL thật, chỉ số
    biến thoải mái) - hai cơ chế trông giống nhau nhưng xử lý chuỗi hoàn toàn khác nhau. Cách đúng:
    dùng cú pháp TIỀN XỬ LÝ `__${itemStat.index}__` để Thymeleaf thay số thật vào chuỗi TRƯỚC khi
    `th:field`/`th:errors` nhìn thấy nó, ví dụ
    `th:field="*{passengerGroups[__${itemStat.index}__].adults[__${pStat.index}__].fullName}"`.
    Đây chính là ca dùng "chính đáng" của `__..__` mà gotcha #55 cảnh báo đừng nhầm với BEM.
    Lỗi thật gặp: `org.thymeleaf.exceptions.TemplateInputException` bọc
    `InvalidPropertyException: Invalid index in property path`, chỉ hiện khi có dữ liệu form (GET
    `/checkout` với giỏ hàng có ít nhất 1 dòng) - test không có giỏ hàng sẽ không bao giờ chạy qua
    nhánh này và tưởng nhầm là đã xong. `grep -c "</html>"` là cách phát hiện nhanh (trang cụt ở
    đúng vị trí thẻ `th:field` đầu tiên bị lỗi).
68. Tệp tạm dựng bằng `paste -sd'&' file > out` (hay bất kỳ lệnh nào kết output bằng dòng mới) vẫn để
    lại `\n` cuối file - `curl --data-binary @out` gửi luôn ký tự đó, ký tự cuối cùng dính vào GIÁ
    TRỊ của tham số cuối (ở đây là `_csrf`) nên khớp CSRF luôn sai, nhận 403 dù thân yêu cầu "nhìn"
    đúng hệt bằng mắt. Mặt thứ ba của gotcha #62 (echo/heredoc/paste đều mắc): sửa bằng
    `printf '%s' "$(cat file)" > file2` (loại bỏ mọi dòng mới cuối) rồi mới `--data-binary @file2`.

---

## Phase 12 — Nghiệp vụ đợt C (SPEC_CHUNG.md mục 12.7, 12.8)

Subagent NGHIỆP VỤ — ĐỢT C. Làm 12.7 (nhập danh sách hành khách + giữ bất biến khi admin sửa số
khách) và 12.8 (ba trang chi tiết CHỈ XEM cho admin: tour, danh mục, tài khoản). Không đụng
12.1-12.6, 12.9 (Phase 11) hay `docs/report/` (đã chốt).

### 12.7 — Nhập danh sách hành khách

**File mới:**
- `dto/form/PassengerForm.java` — họ tên bắt buộc, còn lại (giới tính, ngày sinh, CCCD/hộ chiếu,
  điện thoại, phòng đơn, ghi chú) tuỳ chọn. `@PastOrPresent` chặn ngày sinh tương lai ở tầng DTO.
- `dto/form/PassengerGroupForm.java` — hành khách của MỘT dòng giỏ hàng, tách hẳn hai danh sách
  `adults`/`children` thay vì một danh sách kèm cờ loại: người dùng không có ô nào để tự chọn loại,
  vị trí trong danh sách nào quyết định loại đó.
- `.tmp_ref/fix_stale_passengers_2026-09-23.sql` (chỉ để trên máy, cố ý KHÔNG đưa vào `database/`: script vá riêng ID các đơn test, chạy trên CSDL mới sẽ vô nghĩa và làm người chấm rối) — script vá 4 dòng dữ liệu thử nghiệm lệch bất
  biến từ Phase 11 (xem mục "Bốn dòng lệch cũ" bên dưới). Không phải seed, chạy một lần bằng tay.

**File sửa:**
- `dto/form/CheckoutForm.java` — thêm `List<PassengerGroupForm> passengerGroups`.
- `controller/web/CheckoutController.java` —
  - GET `/checkout`: `buildPassengerGroups(cart, ...)` dựng đúng N+M ô theo từng dòng giỏ hàng
    (thứ tự khớp `cart.getItems()`, ổn định vì `Cart` dùng `LinkedHashMap`), điền sẵn hành khách
    đầu tiên bằng tên + số điện thoại người đặt.
  - POST `/checkout`: `validatePassengerStructure(form, cart, binding)` đối chiếu số dòng, đúng
    `departureId` theo thứ tự, và đúng số ô mỗi loại với giỏ hàng HIỆN TẠI trong session - lệch thì
    `binding.reject(...)` (lỗi toàn cục, KHÔNG throw/redirect) nên biểu mẫu hiện lại với dữ liệu đã
    nhập nguyên vẹn, giống hệt cách các lỗi `@Valid` khác đã hoạt động từ trước.
- `service/BookingService.java` —
  - Thêm `BookingPassengerRepository` vào constructor.
  - `placeOrder`: mỗi `BookingDetail` mới tạo được gắn hành khách ngay bằng `attachPassengers`
    (tìm đúng nhóm theo `departureId`, kiểm LẠI số lượng khớp `item.numAdults`/`numChildren` -
    không tin tầng controller đã chặn hết, đề phòng giỏ hàng đổi giữa hai lần kiểm) rồi
    `toPassenger` tạo từng `BookingPassenger` (kiểm lại họ tên không rỗng + ngày sinh không ở
    tương lai lần thứ hai, độc lập với `@Valid`). Toàn bộ vẫn nằm trong đúng MỘT giao dịch với đơn.
  - `updateDetailQuantity`: đổi chữ ký, nhận thêm `List<String> newAdultNames, newChildNames` và
    trả về `PassengerNameGap` thay vì `Booking` - xem thiết kế bất biến bên dưới.
- `controller/admin/AdminBookingController.java` — `updateDetail` nhận thêm hai tham số danh sách
  tên, đẩy `pendingDetailId`/`pendingAdults`/`pendingChildren`/`pendingAdultsNeeded`/
  `pendingChildrenNeeded` qua flash khi còn thiếu tên.
- `templates/checkout/checkout.html` — khối "Danh sách hành khách" (mới): lặp `cart.items`, với mỗi
  dòng lặp tiếp `*{passengerGroups[__${itemStat.index}__].adults}` /`.children` để dựng đúng số ô.
  **Gotcha #67**: chỉ số động trong `th:field` PHẢI qua cú pháp tiền xử lý `__${...}__`, viết thẳng
  `[itemStat.index]` biên dịch được nhưng vỡ lúc render.
- `templates/admin/booking/detail.html` — form sửa số khách giữ nguyên, thêm khối "chờ xác nhận"
  hiện đúng số ô nhập tên còn thiếu khi `pendingDetailId == d.id`, POST lại CHÍNH địa chỉ cũ.
- `messages*.properties` — 728 → 752 khoá (24 khoá mới, khớp cả ba file).

**Thiết kế giữ bất biến ở màn quản trị (đề bài bắt buộc còn chức năng sửa số khách):**

Chọn cách đơn giản nhất còn đúng, KHÔNG cần checkbox chọn người bị bỏ như gợi ý trong đề bài gốc:

1. Đếm hành khách HIỆN CÓ trực tiếp từ CSDL (`countByDetailIdAndPassengerType`), không dựa vào
   `detail.numAdults` cũ — nhờ vậy phương thức còn **tự chữa** được dòng đã lệch bất biến từ trước.
2. **Tăng** số khách một loại ⇒ chênh lệch dương ⇒ nếu số tên gửi kèm (`newAdultNames`/
   `newChildNames`, đã lọc rỗng) chưa đủ, **không áp dụng gì cả** (kể cả số chỗ/thành tiền) — trả
   `PassengerNameGap` báo còn thiếu bao nhiêu, controller hiện lại đúng bấy nhiêu ô nhập tên ngay
   dưới dòng đang sửa (cùng một địa chỉ POST, không cần route mới).
3. **Giảm** số khách một loại ⇒ chênh lệch âm ⇒ tự động bỏ những hành khách có `id` LỚN NHẤT của
   đúng loại đó (thêm vào gần đây nhất) — không cần giao diện chọn người, vẫn đúng bất biến.
4. Luật cũ "đơn đã có khoản PAID thì chặn sửa số khách" (Phase 11) **giữ nguyên**, kiểm trước khi
   tính toán bất biến.

**Bốn dòng lệch cũ (đo được lúc bắt đầu đợt này):** đơn `TBSELFCANCEL7D`/`TBSELFCANCEL4D`/
`TBSELFCANCEL1D`/`TBEXPIRYTEST` (dữ liệu thử nghiệm Phase 11, tạo trước khi có mục 12.7) mỗi dòng
`num_adults=2, num_children=0`, 0 hành khách. **Không sửa được qua chính giao diện mới** vì ba đơn
đang `CANCELLED` (trạng thái CUỐI) và một đơn `CONFIRMED` nhưng đã có khoản `PAID` — cả hai đều là
luật chặn sửa số khách có chủ đích từ Phase 11, không nên nới lỏng chỉ để vá dữ liệu mẫu. Xử lý bằng
`.tmp_ref/fix_stale_passengers_2026-09-23.sql` (chỉ `INSERT`, có ghi chú rõ trong file, đã chạy và
verify — xem mục nghiệm thu bên dưới).

### 12.8 — Trang chi tiết riêng cho quản trị viên

**File mới:**
- `templates/admin/tour/detail.html` — thông tin, ảnh, lịch trình, các đợt khởi hành (đã bán/còn
  trống), số đơn (đếm `DISTINCT booking.id`, không phải số dòng chi tiết), số đánh giá, điểm trung
  bình.
- `templates/admin/category/detail.html` — thông tin danh mục + toàn bộ tour thuộc nó (kể cả tour
  đã ngừng bán, khác trang công khai).
- `templates/admin/user/detail.html` — thông tin tài khoản (không có ô nhập nào, không đụng
  `user.password`) + lịch sử đơn đã đặt (mã, ngày, tổng, trạng thái, link sang chi tiết đơn), có
  phân trang.

**File sửa:**
- `service/TourService.java` — thêm `record TourAdminDetail` + `adminDetail(id)` (tái dùng
  `getForEdit` đã có JOIN FETCH category+images, cộng thêm đợt khởi hành, số đơn, số đánh giá,
  điểm trung bình).
- `service/CategoryService.java` — thêm `record CategoryAdminDetail` + `adminDetail(id)`.
- `repository/TourRepository.java` — thêm `findByCategoryIdOrderByNameAsc` (khác
  `findByCategoryIdAndActiveTrue`: admin phải thấy cả tour ngừng bán).
- `repository/BookingDetailRepository.java` — thêm `countDistinctBookingsByTourId` (đếm ĐƠN, không
  phải dòng chi tiết — một đơn có hai dòng cùng tour sẽ bị đếm trùng nếu dùng `countByDeparture_Tour_Id`).
- `controller/admin/AdminTourController.java`/`AdminCategoryController.java` — thêm
  `GET /admin/{tours|categories}/{id}/view`.
- `controller/admin/AdminUserController.java` — thêm `GET /admin/users/{id}/view` (tiêm thêm
  `BookingService` để lấy lịch sử đơn qua đúng `findByUser` đã dùng ở `AccountController`).
- `templates/admin/{tour,category,user}/list.html` — mỗi dòng thêm nút mắt "Xem" trỏ sang trang chi
  tiết mới, đứng trước nút "Sửa".

**Kiểm không lộ mật khẩu:** trang `admin/user/detail.html` đọc thẳng entity `User` (không qua
`AdminUserForm`) nhưng KHÔNG có `th:field` nào, chỉ `th:text` các trường vô hại; ghi chú nội bộ
dùng `<!--/* ... */-->` (chú thích mức bộ phân tích của Thymeleaf) đúng quy ước ở `admin/user/list.html`
để bản thân ghi chú không vô tình chứa chuỗi `$2a$` làm phép kiểm tra báo nhầm.

### Nghiệm thu — số thật, thao tác thật

1. `./mvnw -o clean compile` sạch; app khởi động với `validate` (log `Started TourBookingApplication`,
   không có exception nào ở `DataSeeder`/Hibernate).
2. **Đặt tour thật qua trình duyệt** (Claude Browser, không phải `curl`): đăng nhập
   `an.nguyen@gmail.com`, thêm tour Côn Đảo 2 người lớn + 1 trẻ em vào giỏ, mở `/checkout`, gõ tên
   hai hành khách còn lại bằng JavaScript giả lập gõ phím rồi bấm nút "Xác nhận đặt tour" thật ⇒ đơn
   `TB20260923RDM7TY` tạo thành công, `booking_passengers` có đúng 3 dòng (2 ADULT + 1 CHILD) khớp
   `num_adults=2, num_children=1`. Ảnh chụp màn hình 375px và 1440px không tràn ngang
   (`document.documentElement.scrollWidth <= window.innerWidth` cả hai cỡ), console không có lỗi.
   Đường dẫn trước đó cũng thử bằng `curl` thật (đơn `TB20260923YVMZZF`) cho cùng kết quả 3 dòng
   đúng loại - hai cách đều đi qua đúng một luồng server, xác nhận không phải trùng hợp.
3. **Request tự chế thiếu tên / sai số lượng** (curl, không qua trình duyệt): gửi thiếu hẳn
   `adults[1].fullName` ⇒ `binding.reject` bắt được, trang trả **200** (không redirect) với thông
   báo "Danh sách hành khách không khớp với giỏ hàng của bạn. Vui lòng thử lại.", không có đơn nào
   được tạo (đếm `bookings` trước/sau không đổi). Gửi tên rỗng cho một hành khách (đúng số lượng)
   ⇒ lỗi `@NotBlank` hiện đúng dưới ô đó (`is-invalid` + thông báo), **các ô khác giữ nguyên dữ liệu
   đã gõ** (đã xem trực tiếp trong HTML trả về: số điện thoại, tên hành khách thứ hai, tên trẻ em
   đều còn nguyên).
4. **Admin tăng và giảm số khách** một đơn PENDING chưa thu tiền (đơn `TB20260923YVMZZF`, dòng
   chi tiết id 11): tăng 2→3 người lớn không kèm tên ⇒ CSDL không đổi
   (`num_adults` vẫn 2, khớp 2 hành khách), trang hiện đúng 1 ô nhập tên; gửi lại kèm tên "Le Van
   Cuong" ⇒ `num_adults=3`, 3 dòng `booking_passengers` loại ADULT khớp. Giảm 3→1 người lớn ⇒
   `num_adults=1`, còn đúng 1 hành khách ADULT (giữ người có id nhỏ nhất - "Nguyễn Văn An", bỏ hai
   người thêm sau).
5. **Truy vấn bất biến = 0** trên toàn bộ CSDL (đo bằng script SPEC_CHUNG.md mục 12.7) - trước khi
   vá còn 4 dòng lệch (dữ liệu thử nghiệm Phase 11), sau khi chạy
   `.tmp_ref/fix_stale_passengers_2026-09-23.sql` còn **0 dòng**, giữ nguyên 0 sau mọi thao tác ở
   bước 2-4 phía trên.
6. **Ba trang chi tiết admin**: `/admin/tours/3/view`, `/admin/categories/1/view`,
   `/admin/users/2/view` đều `grep -c "</html>"` = 1 (không LazyInitializationException nào cắt
   trang giữa chừng dù `open-in-view` tắt), trang tài khoản `grep -c '\$2a\$'` = **0**, có bảng lịch
   sử đơn với link sang `/admin/bookings/{code}` hoạt động thật.
7. **Messages đối chiếu 0 lệch**: `VI: 752 | EN: 752 | vi_file: 752`, không khoá trùng, không khoá
   lệch giữa ba file (lệnh ở cuối PLAN.md).
8. **Console/tràn ngang**: đã kiểm bằng Claude Browser ở bước 2 (375px và 1440px), không lỗi
   console (`read_console_messages(onlyErrors=true)` rỗng), tiếng Anh (`?lang=en`) cũng hiện đúng
   câu chữ mới ("Passenger list" v.v., không sót khoá nào rơi lại tiếng Việt).

### Dữ liệu test để lại
- Đơn `TB20260923YVMZZF` (curl, PENDING, 3 hành khách, đã qua vòng tăng/giảm số khách ở admin) và
  `TB20260923RDM7TY` (trình duyệt thật, PENDING, 3 hành khách) - cả hai đều đơn thật trên tour Côn
  Đảo đợt id 64.
- Bốn đơn thử nghiệm cũ (`TBSELFCANCEL7D/4D/1D`, `TBEXPIRYTEST`) đã được vá thêm 2 hành khách
  ADULT mỗi dòng (tên đặt rõ "Hành khách bổ sung 1/2 (dữ liệu thử nghiệm Phase 11)" + ghi chú lý do
  trong cột `note`) — không xoá, giữ làm bằng chứng đã xử lý.

### Chưa làm / chưa kiểm được trong đợt này
- Chưa viết `@SpringBootTest`/`@WebMvcTest` tự động cho luồng này (dự án vẫn chưa thêm
  `spring-boot-starter-test`, xem mục "Việc còn lại" #2 - không đổi trong đợt này).
- Chưa thử race thật hai request admin cùng sửa số khách một dòng chi tiết song song (chỉ đọc mã:
  không có khoá bi quan riêng cho bước đếm hành khách/áp dụng, khác hẳn `findByIdForUpdate` dùng khi
  đặt tour - rủi ro lý thuyết nếu hai quản trị viên bấm gần như đồng thời, chấp nhận được vì đây là
  thao tác quản trị hiếm khi có hai người cùng sửa một đơn).
- Ba trang chi tiết 12.8 chưa có ảnh chụp màn hình riêng trong `docs/screenshots/` (không được đụng
  `docs/report/`, và việc thêm ảnh không nằm trong yêu cầu nghiệm thu của đợt này).

---

## Phase 13 — Sửa 7 lỗi do agent rà lần ba (2026-09-23/24)

Subagent SỬA LỖI. Một lỗi NGHIÊM TRỌNG (khoá bi quan đơn hàng), hai lỗi VỪA (checkout đứt gánh
khi giỏ đổi giữa chừng, `DataSeeder` trái nghiệp vụ), bốn lỗi NHẸ (500 lộ CSDL, seed SQL lệch
12.1/12.2, nhãn doanh thu, giảm số khách không nêu tên).

### 1. NGHIÊM TRỌNG — không khoá dòng `bookings` (đã sửa + verify bằng đua thật)

**File sửa:** `repository/BookingRepository.java` (thêm `findByCodeForUpdate`/`findByIdForUpdate`
kiểu `@Lock(PESSIMISTIC_WRITE)`, truy vấn TỐI GIẢN không JOIN FETCH) · `service/BookingService.java`
(thêm `EntityManager`, helper `lockBookingByCode`/`lockBookingById` gọi **đầu tiên** trong
`updateStatus`, `cancelBySelf`, `expirePendingBooking`, `updateDetailQuantity`, luôn
`entityManager.refresh()` ngay sau khi khoá - đúng gotcha #60) · `service/PaymentService.java`
(`markPaid` khoá **đơn cha** bằng `findByCodeForUpdate(bookingCode)` + `refresh()` TRƯỚC KHI nạp
`Payment`, chặn thêm khi `booking.getStatus() == CANCELLED`). Không thêm `@Version` mới, không đổi
schema. Thứ tự khoá: đơn luôn khoá trước, đợt khởi hành (đã có `@Version` từ Phase 1) không cần
khoá bi quan thêm vì không có method nào khoá cả hai cùng lúc.

**Verify bằng đua thật** (kỹ thuật: một phiên `sqlcmd` giữ `UPDLOCK, ROWLOCK` trên đúng dòng
`bookings` 10 giây để nới khe đua, bắn hai `curl` song song bằng `&`/`wait`, đọc CSDL sau khi
`sqlcmd` COMMIT), tạo 4 đơn thật qua checkout thật (`TB20260923EH9VH9`, `TB202609232R9SBJ`,
`TB20260923S85XQ8`, `TB20260923QCK86L`):

- **Kịch bản A, khách thắng trước** (`TB20260923EH9VH9`): tự huỷ commit trước → đơn CANCELLED,
  khoản thu 22 PENDING→FAILED đúng; admin `mark-paid` chạy sau, khoá xong đọc lại thấy CANCELLED,
  bị chặn **BusinessRuleException** `error.payment.bookingCancelled` ("Đơn ... đã bị huỷ, không thể
  đánh dấu đã thu tiền..."), khoản thu **không** bị ghi đè thành PAID.
- **Kịch bản A, admin thắng trước** (`TB202609232R9SBJ`): `mark-paid` commit trước → khoản 24 PAID
  (txn_ref `RACE-TEST-C`); tự huỷ chạy sau, khoá xong đọc lại thấy PAID, **giữ nguyên** dòng PAID và
  **thêm** đúng một dòng REFUNDED 6.589.000đ (100%, đợt còn ~50 ngày) - không mất khoản đã thu, có
  hoàn tiền đúng luật 12.2.
- **Kịch bản B, admin thắng trước** (`TB20260923S85XQ8`): xác nhận PENDING→CONFIRMED commit trước,
  tự huỷ chạy sau thấy CONFIRMED (hợp lệ để huỷ theo 12.3) → CANCELLED, đợt 2 về đúng 25/25 chỗ,
  lịch sử ghi đủ ba dòng PENDING→CONFIRMED (admin)→CANCELLED (khách) - đúng, không phải lỗi.
- **Kịch bản B, khách thắng trước** (`TB20260923QCK86L`, tái hiện đúng lỗi gốc): tự huỷ commit
  trước → CANCELLED, đợt 5 về đúng 25/25; admin xác nhận chạy sau, khoá xong đọc lại thấy CANCELLED
  (trạng thái CUỐI), bị chặn **BusinessRuleException** `error.booking.invalidTransition` ("Không thể
  chuyển đơn từ trạng thái "Đã huỷ" sang "Đã xác nhận"."), lịch sử **chỉ** có PENDING→CANCELLED,
  không có dòng CONFIRMED giả - đúng là lỗi đã tái hiện ở Kịch bản B gốc (đợt 22 hiện 30/30 trong
  khi đơn giữ 1 khách, khoản thu FAILED) nay không còn xảy ra nữa.

Cả bốn lượt đua đều cho kết quả nhất quán: một bên thắng áp dụng đúng, bên thua bị chặn có thông
báo tiếng Việt tử tế, không mất khoản thu, không lệch số chỗ.

### 2. VỪA — checkout đứt gánh khi giỏ đổi giữa chừng (đã sửa + verify bằng thao tác thật)

**File sửa:** `controller/web/CheckoutController.java` - `validatePassengerStructure` tách ra
`structureMatches` (chỉ so khớp, không sửa gì); lệch cấu trúc thì `rebuildPassengerGroups` dựng lại
đúng theo giỏ hàng HIỆN TẠI (giữ tên đã nhập nếu `departureId` còn khớp, chỉ prefill hành khách đầu
tiên nếu ô đó đang trống), gắn `error.checkout.cartChangedRebuilt` (thông báo "giỏ hàng vừa thay
đổi..."). Thêm `@InitBinder("checkoutForm")` hạ `setAutoGrowCollectionLimit` xuống 64 (Nhẹ - 4).

**Verify thật** (tài khoản `binh.tran@gmail.com`, qua `curl` thao tác thật): mở `/checkout` với 1
tour trong giỏ (nạp form CŨ, 1 `passengerGroup`) → "mở tab khác" thêm tour thứ hai vào giỏ (giỏ
session giờ có 2 dòng) → gửi lại form CŨ (vẫn chỉ 1 `passengerGroup`) ⇒ trang trả **200**
(`grep -c "</html>" = 1`), thông báo đúng câu mới, và **dòng tour thứ hai đã có đủ ô nhập hành
khách với `departureId` đúng** (trước đây rỗng) - tên đã gõ ở dòng đầu **vẫn còn nguyên**. Gửi lại
lần hai kèm đủ thông tin ⇒ đặt thành công thật, đơn `TB20260923QF3KK2`.

### 3. VỪA — `DataSeeder` trái nghiệp vụ (đã sửa + verify trên database tạm)

**File sửa:** `config/DataSeeder.java` - `createBooking` đổi chữ ký nhận `LocalDateTime bookingDate`
thay vì `int daysAgo`; hai đơn PENDING nay đặt 5 giờ và 3 giờ trước (trong hạn 24 giờ); thêm
`attachPassengers` gắn đủ hành khách khớp bất biến cho mọi dòng; thêm `seedPayment`/`seedHistory`
sinh `payments`/`booking_status_history` hợp lý cho cả 5 đơn; thêm `rewindOneDepartureToPast` lùi
**đúng một** đợt (không phải cả 60) về quá khứ cho đơn COMPLETED, và đơn đó nay có đủ 2 khoản PAID
100%.

**Verify trên database tạm** (`TourBookingDB_tmp`, dựng bằng login Windows Auth có quyền
`sysadmin` - xem gotcha #69 - vì login `tourapp` của ứng dụng không có quyền `CREATE DATABASE`):
chạy `02_schema.sql` (16 bảng, 0 CHECK/Function/Procedure/Trigger), khởi động app trỏ
`--spring.datasource.url=...databaseName=TourBookingDB_tmp...`, `DataSeeder` chạy sạch không
exception. Đo ngay sau khi seed: 2 đơn PENDING lúc 5h/3h trước · đơn COMPLETED trỏ đợt id 16
ngày **2026-09-03** (đã qua) · 8 dòng `payments` cho 5 đơn · bất biến hành khách **= 0**. Đợi hơn
**75 giây** (qua mốc `initialDelay = 1 phút` của `BookingExpiryScheduler`) rồi đọc lại: **hai đơn
PENDING vẫn còn nguyên** (không bị huỷ oan). Xong thì tắt app, `DROP DATABASE TourBookingDB_tmp`.

### 4. NHẸ — 500 lộ CSDL (đã sửa + verify bằng thao tác thật)

**File sửa:** `CheckoutController` (InitBinder ở mục 2) · `service/BookingService.java` -
`cleanNames` kiểm thêm độ dài ≤ 100 ký tự (khớp `booking_passengers.full_name NVARCHAR(100)`), ném
`BusinessRuleException("error.checkout.passengerNameTooLong")` thay vì để lỗi cắt chuỗi SQL lộ ra.

**Verify thật**: admin gửi `newAdultNames` dài 150 ký tự cho đơn `TB20260923QF3KK2` ⇒ **302** (không
500), flash hiện đúng "Họ tên hành khách không được quá 100 ký tự.", `num_adults` trong CSDL **không
đổi** (vẫn 1, không ghi dở dang).

### 5. NHẸ — `03_seed_data.sql` lệch 12.1/12.2 (đã sửa + verify trên database tạm)

**File sửa:** `database/03_seed_data.sql` - thêm bước 5b lùi riêng đợt `id = 16` (Sa Pa, đợt của đơn
COMPLETED) về `-20` ngày bằng `UPDATE` sau khi insert; thêm dòng `payments PENDING` cho đơn 4 (trước
đây không có dòng nào); thêm dòng `payments PAID` cho đơn 5 TRƯỚC dòng REFUNDED đã có sẵn (huỷ đơn
đã thu tiền mới có hoàn, không phải REFUNDED mồ côi).

**Verify trên database tạm**: chạy bản sao có `USE TourBookingDB_tmp;` (script gốc hard-code
`USE TourBookingDB;` - xem gotcha #70) qua `sqlcmd -f 65001`, 0 dòng lỗi. Đọc lại: đơn 3 COMPLETED
trỏ đợt `departure_date = 2026-09-03` (PAST_OK) · đủ 8 dòng `payments` cho 5 đơn (đơn 4 có PENDING,
đơn 5 có cả PAID lẫn REFUNDED) · bất biến hành khách **= 0** · số chỗ mọi đợt khớp. **Không đụng**
CSDL chính (`TourBookingDB` vẫn 16 bookings/4 users như trước, đã kiểm lại sau khi phát hiện
`USE TourBookingDB;` hard-code khiến lần chạy thử đầu tiên gõ nhầm `-d TourBookingDB_tmp` lại chạm
đúng CSDL chính - may là toàn bộ câu lệnh đó chỉ báo lỗi "đã tồn tại" (Msg 2714) chứ không xoá/ghi
đè gì).

### 6. NHẸ — nhãn doanh thu lệch 12.9 (đã sửa + verify)

**File sửa:** `messages.properties`/`messages_vi.properties`/`messages_en.properties` -
`admin.dashboard.revenue` đổi từ "Doanh thu (không tính đơn huỷ)" thành "Doanh thu (chỉ tính đơn đã
xác nhận và hoàn tất)" (bản EN: "Revenue (confirmed and completed orders only)"). Verify: mở
`/admin` thật, nhãn mới hiện đúng trên dashboard.

### 7. NHẸ — giảm số khách không nêu tên ai bị bỏ (đã sửa + verify bằng thao tác thật)

**File sửa:** `service/BookingService.java` - `PassengerNameGap` thêm trường `droppedNames`;
`syncPassengers` trả về danh sách tên vừa bị bỏ (id lớn nhất trước) · `AdminBookingController` nối
thêm câu `admin.booking.passengers.dropped` vào flash thành công khi có tên bị bỏ.

**Verify thật**: tăng dòng chi tiết id 23 (đơn `TB20260923QF3KK2`) từ 1→2 người lớn kèm tên "Phạm
Văn Test", rồi giảm lại 2→1 ⇒ flash: *"Đã cập nhật số khách. Thành tiền và số chỗ trống đã được
tính lại. Đã bỏ (những) hành khách: Phạm Văn Test."* - đúng tên người vừa bị bỏ.

### Dọn dữ liệu test của agent review

- `TB202609236RQEL2` (kịch bản B cũ, CONFIRMED nhưng đợt 22 đã 30/30, khoản thu FAILED): đưa về
  nhất quán bằng chính API đã sửa - gọi `POST /admin/bookings/TB202609236RQEL2/status` với
  `status=CANCELLED` (đợt 22 đã sẵn 30/30 nên `releaseSeats` chỉ `Math.min` giữ nguyên, không âm
  chỗ). Kết quả: CANCELLED, đợt 22 = 30/30, khoản thu vẫn FAILED - nhất quán.
- `TB20260923Q9M8GS` (kịch bản A cũ, mất khoản đã thu): **để nguyên, ghi rõ chưa sửa được** - hiện
  đang CANCELLED + khoản thu FAILED, một tổ hợp *hợp lệ* xét thuần theo bất biến (không kích hoạt
  cảnh báo nào), nhưng lịch sử thật của nó (từng PAID rồi bị ghi đè) không phục hồi lại được vì
  `txn_ref`/`paid_at` gốc đã mất - không có cơ sở để bịa số liệu mới.
- `U4XU6P`, `HPGGSR`, `RUD3VT`: không tìm thấy trong CSDL hiện tại (không còn tồn tại hoặc đã bị dọn
  từ trước) - không cần xử lý thêm.
- Dữ liệu test MỚI để lại làm bằng chứng (không xoá): `TB20260923EH9VH9`, `TB202609232R9SBJ`,
  `TB20260923S85XQ8`, `TB20260923QCK86L` (bốn đơn dùng cho bốn lượt đua khoá bi quan, mục 1) và
  `TB20260923QF3KK2` (đơn dùng cho verify mục 2/4/7).

### Nghiệm thu cuối - số thật

`./mvnw -o clean compile` **BUILD SUCCESS** · messages `VI: 756 | EN: 756 | vi_file: 756`, 0 trùng,
0 lệch · bất biến hành khách toàn CSDL **= 0** · số chỗ mọi đợt khởi hành khớp
`total_seats - Σ khách các đơn chưa huỷ` (0 đợt lệch) · `sys.check_constraints` = 0, Function/SP/
Trigger = 0 · cổng 8081 đã đóng sau khi kiểm xong.

### Chưa làm / chưa kiểm được trong đợt này

- `TB20260923Q9M8GS` không phục hồi được lịch sử PAID gốc (xem mục "Dọn dữ liệu test" ở trên) - để
  nguyên, không bịa số liệu.
- Chưa test hai admin cùng sửa số khách một dòng chi tiết song song (không thuộc 7 lỗi được giao,
  và mục "Chưa làm" của Phase 12 đã ghi nhận đây là rủi ro lý thuyết chấp nhận được).
- `docs/report/` không đụng tới (đúng phạm vi được giao).

### Gotcha mới

69. ⚠️ **Login của ứng dụng (`tourapp`) không có quyền `CREATE DATABASE`** - đúng thiết kế bảo mật
    (chỉ `db_owner` trên đúng một CSDL), nhưng lại chặn luôn cách dựng "database tạm" mà nhiều gotcha
    trước (`#65` cũ, hướng dẫn nghiệm thu Phase 11/12) gợi ý. Trên máy dev cục bộ, login **Windows
    Authentication của chính người dùng** (`sqlcmd -E`) thường có sẵn `sysadmin` (kiểm bằng
    `SELECT IS_SRVROLEMEMBER('sysadmin')`) - dùng login đó để `CREATE DATABASE ... COLLATE
    Vietnamese_CI_AI`, rồi `CREATE USER tourapp FOR LOGIN tourapp; ALTER ROLE db_owner ADD MEMBER
    tourapp;` để ứng dụng (vẫn nối bằng `tourapp` trong `application.properties`) đọc/ghi được CSDL tạm đó
    qua `--spring.datasource.url=...databaseName=<tên_tạm>...`. Đây KHÔNG phải bỏ qua "thao tác cần
    quyền admin" của `CLAUDE.md` (vốn nói về bật TCP/restart service) - tạo một CSDL tạm trên chính
    máy dev, tự dọn ngay sau khi xong, là thao tác an toàn và trong tầm với qua chính hệ thống
    đăng nhập Windows sẵn có, không phải thứ "user phải tự chạy".
70. ⚠️ **File `.sql` seed hard-code `USE TourBookingDB;` ở đầu file "thắng" mọi cờ `-d` trên dòng
    lệnh `sqlcmd`.** Chạy `sqlcmd -d TourBookingDB_tmp -i 02_schema.sql` tưởng đâu trỏ vào CSDL tạm,
    nhưng câu `USE TourBookingDB;` bên trong file đổi ngữ cảnh về CSDL CHÍNH ngay dòng đầu tiên -
    toàn bộ phần còn lại chạy nhầm chỗ. May mắn lần vấp thật (Phase 13) vô hại vì mọi `CREATE TABLE`
    đều thất bại êm với `Msg 2714` (đối tượng đã tồn tại) chứ không xoá/ghi đè gì - nhưng đây là may
    mắn, không phải điều đảm bảo. **Cách đúng để kiểm script trên CSDL tạm**: `sed
    's/USE TourBookingDB;/USE TourBookingDB_tmp;/' file.sql > ban_sao.sql` rồi chạy bản sao đó,
    tuyệt đối đừng tin cờ `-d`/`-S` khi file có sẵn `USE`. Kèm gotcha #66 cũ: bản sao phải nằm trong
    cây thư mục Windows thật (ví dụ `.tmp_ref/`), không phải `/tmp` của Git Bash.

## Phụ lục: ánh xạ mã use case sang báo cáo Word (sắp xếp lại theo biểu đồ tổng quát)

Báo cáo Word đã đánh số lại 21 use case theo thứ tự trên biểu đồ use case tổng quát. Các mã UC trong tệp này, trong SPEC_CHUNG.md và trong chú thích mã nguồn là mã nội bộ cũ, **không đổi**; dùng bảng sau để đối chiếu:

| Mã nội bộ (PLAN.md, SPEC_CHUNG.md, chú thích trong mã) | Mã trong báo cáo Word mới | Tên |
|---|---|---|
| UC002 | UC001 | Tìm kiếm và lọc tour |
| UC021 | UC002 | Gửi liên hệ |
| UC005 | UC003 | Đăng ký tài khoản |
| UC004 | UC004 | Quản lý giỏ hàng |
| UC006 | UC005 | Đăng nhập (Đăng xuất là «extend») |
| UC007 | UC006 | Đặt tour (trước đây “Đặt tour trực tuyến”) |
| UC017 | UC007 | Áp dụng mã giảm giá |
| UC023 | UC008 | Huỷ đơn đặt tour |
| UC008 | UC009 | Xem lịch sử đơn đặt |
| UC018 | UC010 | Đánh giá tour đã đi |
| UC009 | UC011 | Cập nhật hồ sơ cá nhân |
| UC010 | UC012 | Đổi mật khẩu |
| UC011, UC024, UC025, UC026 | UC013 | Quản lý danh mục tour (Thêm, Sửa, Xoá) |
| UC012 | UC014 | Quản lý tour và ảnh |
| UC013 | UC015 | Quản lý lịch khởi hành (đợt khởi hành) |
| UC014 | UC016 | Quản lý đơn đặt tour |
| UC015 | UC017 | Quản lý tài khoản |
| UC019 | UC018 | Quản lý mã khuyến mãi |
| UC016 | UC019 | Xem thống kê doanh thu |
| UC020 | UC020 | Kiểm duyệt đánh giá |
| UC022 | UC021 | Duyệt liên hệ (menu giao diện vẫn là “Quản lý liên hệ”) |
| UC001, UC003 | (bỏ) | Xem trang chủ và giới thiệu; Xem chi tiết tour: không còn là use case trong báo cáo, trang vẫn tồn tại |

