# Hợp đồng chung cho các subagent — báo cáo & UML TourBooking

> File này là **nguồn chân lý duy nhất** về danh sách tác nhân / use case / mã số.
> Ba subagent (docs, UML, UI) đều đọc file này. **Không subagent nào được tự đổi mã UC.**
> Muốn thêm/bớt use case thì phải báo agent điều phối, không tự sửa.

## 0. Bối cảnh

Đồ án **Lập trình WWW (Java)** — IUH. Website giới thiệu & đăng ký tour du lịch trực tuyến.
Spring Boot 3.5.16 + Thymeleaf SSR + REST cho AJAX + SQL Server. Chi tiết: [PLAN.md](../../PLAN.md).

**File mẫu để bắt chước định dạng:** `C:\Users\Khangg\Downloads\ptud\Nhom12_ApplicationDevelopment_Full.docx`
(báo cáo môn Phát triển ứng dụng — đề tài quản lý hiệu thuốc). Bản trích text sạch đã có sẵn ở
`.tmp_ref/template_clean.md`, ảnh mẫu ở `.tmp_ref/template_images/`.
⚠️ **Chỉ mượn CẤU TRÚC và ĐỊNH DẠNG, nội dung phải là TourBooking** — không được để sót chữ
"thuốc", "hiệu thuốc", "hóa đơn", "nhà cung cấp", "lô thuốc" trong sản phẩm cuối.

## 1. Tác nhân (actor)

| Mã | Tên | Mô tả |
|---|---|---|
| AC01 | Khách vãng lai | Người dùng chưa đăng nhập. Xem tour, tìm kiếm, dùng giỏ hàng (giỏ nằm trong `HttpSession`). |
| AC02 | Khách hàng | Tài khoản vai trò `CUSTOMER`. **Kế thừa toàn bộ** chức năng của Khách vãng lai. |
| AC03 | Quản trị viên | Tài khoản vai trò `ADMIN`. Toàn quyền khu `/admin/**`. |

## 2. Danh sách use case — CHỐT, không được đổi mã

### Nhóm khách vãng lai (AC01)

| Mã | Tên use case | Đường dẫn / ghi chú kỹ thuật |
|---|---|---|
| UC001 | Xem trang chủ và giới thiệu | `GET /`, `GET /about` |
| UC002 | Tìm kiếm và lọc tour | `GET /tours` — tìm không dấu trên cột `tours.search_text` |
| UC003 | Xem chi tiết tour | `GET /tours/{id}` — tăng `view_count` |
| UC004 | Quản lý giỏ hàng | `POST /cart/add,update,remove,clear` — giỏ trong Session, **không có bảng CSDL** |
| UC005 | Đăng ký tài khoản | `GET\|POST /register` — mật khẩu băm BCrypt, `@UniqueEmail` |
| UC006 | Đăng nhập / Đăng xuất | Spring Security form login; giỏ hàng sống sót qua đăng nhập (`changeSessionId`) |
| UC021 | Gửi liên hệ | `GET\|POST /contact` — lưu `contact_messages` với `status = NEW`; `tour_id` tuỳ chọn. Chi tiết ở **mục 9** |

### Nhóm khách hàng đã đăng nhập (AC02)

| Mã | Tên use case | Đường dẫn / ghi chú kỹ thuật |
|---|---|---|
| UC007 | Đặt tour trực tuyến | `GET\|POST /checkout` — `BookingService.placeOrder` trong **một giao dịch**: validate, trừ `available_seats`, sinh mã `TB…`, ghi `coupon_usages` |
| UC008 | Xem lịch sử đơn đặt | `GET /account/bookings`, `GET /account/bookings/{code}` — có kiểm tra quyền sở hữu (khác chủ → 403) |
| UC009 | Cập nhật hồ sơ cá nhân | `GET\|POST /account/profile` |
| UC010 | Đổi mật khẩu | `GET\|POST /account/password` |
| UC017 | Áp dụng mã giảm giá | `GET /api/promotions/check` — 4 nhánh sai trả **409**; trần giảm giá; 1 mã / 1 tài khoản |
| UC018 | Đánh giá tour đã đi | ✅ đã làm. Chỉ được đánh giá tour đã đặt & đơn đã hoàn tất. **Lưu `approved = false` — phải qua kiểm duyệt (UC020) mới hiện công khai**, KHÔNG duyệt ngay |
| UC023 | Huỷ đơn đặt tour | Khách tự huỷ đơn của mình theo chính sách thời gian; hoàn tiền theo tỉ lệ. Chi tiết ở **mục 12.3** |

### Nhóm quản trị viên (AC03)

| Mã | Tên use case | Đường dẫn / ghi chú kỹ thuật |
|---|---|---|
| UC011 | Quản lý danh mục tour | `/admin/categories` — **chặn xoá khi còn tour** (`error.category.delete.hasTours`) |
| UC012 | Quản lý tour và ảnh | `/admin/tours` — upload ảnh; chặn xoá khi đã có lượt đặt / đánh giá / liên hệ |
| UC013 | Quản lý đợt khởi hành | `/admin/tours/{tourId}/departures` — chặn xoá khi đã có lượt đặt; `@Version` chống bán quá chỗ |
| UC014 | Quản lý đơn đặt tour | `/admin/bookings` — đổi trạng thái, sửa số khách, huỷ đơn thì trả chỗ. **+ ghi `booking_status_history` và đánh dấu đã thu tiền `payments`** (mục 9) |
| UC015 | Quản lý tài khoản | `/admin/users` — không tự xoá mình, không xoá admin cuối, chặn khi còn đơn/đánh giá/… |
| UC016 | Xem thống kê doanh thu | `/admin` + `GET /api/admin/stats/revenue` — biểu đồ SVG vẽ bằng AJAX |
| UC019 | Quản lý mã khuyến mãi | ✅ đã làm. CRUD `promotions`, xem `coupon_usages`, chặn xoá khi đã có lượt dùng |
| UC020 | Kiểm duyệt đánh giá | ✅ đã làm. `/admin/reviews` — Duyệt · Bỏ duyệt · Trả lời · Xoá. Chi tiết ở **mục 8** |
| UC022 | Quản lý liên hệ | `/admin/contacts` — lọc theo `ContactStatus`, đổi trạng thái, ghi `reply_note`. Chi tiết ở **mục 9** |
| UC024 | Thêm danh mục tour | Thao tác con của UC011 («extend»). Đại diện mẫu CRUD cho các chức năng quản lý khác. Chi tiết **mục 13** |
| UC025 | Sửa danh mục tour | Thao tác con của UC011 («extend»). Chi tiết **mục 13** |
| UC026 | Xoá danh mục tour | Thao tác con của UC011 («extend»); chặn xoá khi còn tour, gợi ý ngừng hoạt động. Chi tiết **mục 13** |

**Tổng: 26 use case** (UC001–UC026). ⚠️ **Con số này phải khớp ở CẢ BA nơi**: bảng danh sách use
case trong báo cáo, số bảng đặc tả ở mục 2.7, và số hình e-líp trong biểu đồ
`01. Biểu đồ Use Case tổng quát`. Đã từng lệch một lần (báo cáo ghi 20 còn biểu đồ vẽ 19).

Quan hệ trên biểu đồ use case:
- `Khách hàng` ──▷ `Khách vãng lai` (Generalization)
- `UC007 Đặt tour trực tuyến` ⟶ «include» ⟶ `UC006 Đăng nhập`
- `UC007 Đặt tour trực tuyến` ⟶ «extend» ⟶ `UC017 Áp dụng mã giảm giá`
- `UC018 Đánh giá tour` ⟶ «include» ⟶ `UC006 Đăng nhập`

## 3. Quy ước đặt tên biểu đồ (dùng chung cho `.vpp`, `png/` và báo cáo)

```
01. Biểu đồ Use Case tổng quát
02. Biểu đồ lớp - Mô hình miền (lõi)
02b. Biểu đồ lớp - Chín bảng bổ sung
03. Biểu đồ ERD - TourBookingDB
AC-UC001. Biểu đồ hoạt động - Xem trang chủ và giới thiệu
SQ-UC001. Biểu đồ tuần tự - Xem trang chủ và giới thiệu
AC-UC002. …  (mỗi UC một cặp, đúng 19 cặp)
```

Tên file PNG xuất ra **trùng tên biểu đồ** + `.png`, nằm ở `docs/uml/png/`.

## 4. Phong cách biểu đồ — bám đúng file mẫu

**Biểu đồ hoạt động (activity):** hai làn bơi dọc (`swimlane`/partition) — làn trái là tác nhân
(`Khách hàng` / `Quản trị viên` / `Khách vãng lai`), làn phải là `Hệ thống`. Có nút khởi đầu (chấm
đen đặc), các hành động bo góc, hình thoi rẽ nhánh có nhãn điều kiện, nút kết thúc (chấm đen viền).
Xem `.tmp_ref/template_images/image4.png`.

**Biểu đồ tuần tự (sequence):** kiểu **boundary–control–entity**, các lifeline theo thứ tự:
`Actor` → `Giao diện` (boundary) → `ctrl_<tên>` (control) → `<Entity>` (entity, có thể nhiều).
Thông điệp **đánh số phân cấp** `1`, `1.1`, `1.2`, `1.2.1`, `1.2.2`, `2`, `3`. Thông điệp trả về vẽ
nét đứt. Nhánh rẽ bọc trong khung `alt` có `[điều kiện]`. Xem `.tmp_ref/template_images/image5.png`.

⚠️ **MỤC NÀY ĐÃ BỊ THAY THẾ BỞI MỤC 11.** Chỉ dẫn cũ "lifeline control đặt theo service thật trong
mã nguồn (`ctrl_BookingService`…)" **KHÔNG còn hiệu lực** — user yêu cầu dùng tên mức thiết kế.
Đọc **mục 11** để biết quy ước đang áp dụng.

## 5. Ràng buộc kỹ thuật bắt buộc (trích CLAUDE.md + PLAN.md)

- `ddl-auto: validate` → sửa entity **phải** sửa `database/02_schema.sql` rồi chạy lại schema.
- Enum mới **phải** có `AttributeConverter` riêng, **cấm** `@Enumerated`, **cấm** `columnDefinition`
  kiểu chuỗi trên entity (gotcha #6, #14).
- Câu chữ mới vào **cả ba** file `messages.properties`, `messages_vi.properties`,
  `messages_en.properties` (hiện 540 khoá, phải khớp nhau — lệnh đối chiếu ở cuối PLAN.md).
- Sửa `src/main/resources/` xong **phải** `./mvnw -o process-resources`.
- **Đừng tin mã 200** — lỗi Thymeleaf giữa chừng vẫn trả 200 với HTML cụt. Kiểm `grep -c "</html>"`.
- Quy tắc nghiệp vụ ở **tầng service bằng Java**; cấm CHECK/Function/Stored Procedure/Trigger.
- Mọi POST kết thúc bằng `redirect:` + flash attribute (PRG).
- Tiếng Việt truyền qua **file**, không qua tham số dòng lệnh (gotcha #17, #40).
- Animation phải nằm sau lớp `.anim-ready` + có hẹn giờ bảo hiểm (gotcha #29, #33).

## 6. Môi trường

```bash
cd "C:/Users/Khangg/Downloads/www/project_www"
export JAVA_HOME="/c/Users/Khangg/.jdks/corretto-23.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
export SQLCMD="/c/Program Files/Microsoft SQL Server/Client SDK/ODBC/180/Tools/Binn/SQLCMD.EXE"
./mvnw -o spring-boot:run        # http://localhost:8081
```

Tài khoản mẫu, mật khẩu đều `123456`: `admin@tourbooking.vn` (ADMIN) · `an.nguyen@gmail.com` ·
`binh.tran@gmail.com` · `cuong.le@gmail.com`.

## 7. Trang bìa báo cáo — **để trống có chủ đích**

User sẽ tự điền. Subagent docs ghi placeholder rõ ràng dạng `«Nhóm: …»`, `«Lớp học phần: …»`,
`«GVHD: …»`, và bảng thành viên có đủ cột `STT | HỌ VÀ TÊN | MSSV | CHỮ KÝ` nhưng các ô để trống.
**Không tự bịa tên thành viên, mã số sinh viên hay tên giảng viên.**

---

## 8. UC020 — Kiểm duyệt đánh giá (bổ sung, user yêu cầu)

> Chốt bởi agent điều phối. **Cả agent UI lẫn agent docs đều bám đúng mục này**, không tự thiết kế khác.
> Lý do chốt trước: lần trước hai agent tự đoán thiết kế UC018/UC019 rồi phải viết lại toàn bộ đặc tả.

**Mã: UC020 · Tác nhân: AC03 Quản trị viên · Đường dẫn: `/admin/reviews`**

### Vì sao không cần đổi schema
Entity `Review` **đã thiết kế sẵn cho kiểm duyệt ngay từ đầu** — đọc Javadoc của nó:
*"`approved` mặc định là `false`: đánh giá phải qua kiểm duyệt mới hiện ra trang công khai"*.
Bảng `reviews` cũng đã có sẵn `approved BIT NOT NULL`, `admin_reply NVARCHAR(1000)`, `replied_at`.

⇒ **KHÔNG thêm cột, KHÔNG thêm enum, KHÔNG cần `AttributeConverter` mới.** Tránh được trọn vẹn
gotcha #6 và #14. `database/02_schema.sql` giữ nguyên, `ddl-auto: validate` không bị ảnh hưởng.

### Thay đổi hành vi
| Chỗ | Trước | Sau |
|---|---|---|
| Khách gửi đánh giá | `approved = true` (hiện ngay) | **`approved = false`** — chờ duyệt |
| Trang chi tiết tour | hiện mọi đánh giá | **chỉ hiện `approved = true`** |
| Điểm trung bình | tính trên mọi đánh giá | **chỉ tính trên đánh giá đã duyệt** |
| Trang cá nhân của khách | — | khách thấy đánh giá của chính mình kèm nhãn *"Chờ duyệt"* |

### Màn `/admin/reviews` phải có
- Danh sách phân trang, **lọc theo trạng thái**: Chờ duyệt · Đã duyệt · Tất cả. Mặc định mở ở *Chờ duyệt*.
- Mỗi dòng: tour, người viết, số sao, trích nội dung, ngày gửi, nhãn **"Đã xác thực"** khi `booking != null`.
- Thao tác: **Duyệt** (`approved = true`) · **Bỏ duyệt** (`approved = false`, gỡ khỏi trang công khai)
  · **Trả lời** (ghi `admin_reply` + `replied_at`, dùng sẵn phương thức `Review.reply(String)`)
  · **Xoá**.
- Huy hiệu đếm số đánh giá đang chờ duyệt trên menu quản trị.
- Mọi POST kết thúc bằng `redirect:` + flash attribute (PRG).

### Ràng buộc
- Quy tắc nghiệp vụ ở **tầng service bằng Java**, không CHECK/Trigger.
- Câu chữ mới vào **cả ba** file messages.
- Trang chi tiết tour đang dùng `LEFT JOIN FETCH` để nạp đánh giá (`open-in-view: false`) —
  thêm điều kiện lọc `approved` thì **phải sửa trong chính truy vấn đó**, đừng lọc ở template.

---

## 9. UC021–UC022 + hai bổ sung — "bốn bảng chết" (user yêu cầu)

> Chốt bởi agent điều phối **trước khi** giao, vì đã hai lần agent tự đoán thiết kế rồi phải làm lại.
> Cả ba agent bám đúng mục này.

### Vấn đề đang vá
Bốn bảng mở rộng chỉ tồn tại để **chặn xoá**, không chỗ nào trong ứng dụng ghi vào:
`contact_messages` · `payments` · `booking_status_history` · `password_reset_tokens`.
Hệ quả: báo cáo có bảng đặc tả cho `Payment`, `ContactMessage`… nhưng không có chức năng nào để chỉ;
quy tắc `error.tour.delete.hasContactMessages` chỉ kích hoạt được trên dữ liệu seed.

Lần này vá **ba bảng đầu**. `password_reset_tokens` tạm để lại (EmailService chỉ ghi console).

### KHÔNG đổi schema
Cả ba bảng **đã có đủ cột** trong `database/02_schema.sql`, và ba enum `ContactStatus`,
`PaymentMethod`, `PaymentStatus` **đã có `AttributeConverter` riêng**. ⇒ không thêm cột, không thêm
enum, không thêm converter. `ddl-auto: validate` không bị đụng. (Giống hệt UC020 — xem mục 8.)

### UC021 — Gửi liên hệ · AC01 Khách vãng lai · `/contact`
- `GET /contact` form, `POST /contact` gửi → PRG + flash.
- Lưu `status = NEW`. `tour_id` **tuỳ chọn**: mở từ nút "Liên hệ về tour này" ở trang chi tiết tour
  thì điền sẵn, vào thẳng `/contact` thì để trống.
- Validate ở DTO (`ContactForm`): họ tên, email đúng định dạng, tiêu đề, nội dung bắt buộc.

### UC022 — Quản lý liên hệ · AC03 Quản trị viên · `/admin/contacts`
- Danh sách phân trang, **lọc theo `ContactStatus`**: NEW · IN_PROGRESS · RESOLVED · SPAM. Mặc định NEW.
- Xem chi tiết; đổi trạng thái; ghi `reply_note`, đặt `handled_by_id` = admin đang đăng nhập và
  `handled_at` khi chuyển sang RESOLVED/SPAM.
- Huy hiệu đếm số liên hệ `NEW` trên menu quản trị (làm giống `pendingReviewCount` đã có).

### Bổ sung A — `BookingStatusHistory` ghi vào UC014, **không phải use case mới**
- Ghi **một dòng cho mỗi lần đổi trạng thái đơn**, kể cả dòng đầu lúc đặt tour
  (`from_status = NULL`, `to_status = PENDING`) — cột `from_status` vốn đã chú thích "null ở dòng đầu tiên".
- `changed_by_id` = người thao tác (null nếu hệ thống), `reason` lấy từ ô ghi chú của admin.
- Hiện **dòng thời gian** ở cả `/account/bookings/{code}` và `/admin/bookings/{code}`.

### Bổ sung B — `Payment` ghi vào UC007, **không phải use case mới**
- Lúc checkout sinh **một dòng `payments`**: `amount` = tổng sau giảm giá, `method` theo lựa chọn,
  `status = PENDING`, `paid_at = NULL`.
- Admin đánh dấu đã thu tiền → `status = PAID`, đặt `paid_at` và `txn_ref`.
- ⚠️ **`txn_ref` là UNIQUE trên cột cho phép NULL** → SQL Server chỉ chấp nhận **đúng một** dòng NULL.
  Gotcha #41 đã ghi: chỉ mục có lọc đã thử và **bỏ** (đòi `QUOTED_IDENTIFIER ON`, làm gãy script seed
  với `Msg 1934`). ⇒ **kiểm trùng `txn_ref` bằng Java ở tầng service**, và **đừng** đặt ràng buộc
  UNIQUE lên cột này.
- Hiện thông tin thanh toán ở trang chi tiết đơn (cả hai phía).

### Hệ quả phải kiểm (gotcha #42)
Ba bảng này nay **có dữ liệu thật**, nên các quy tắc chặn xoá vốn đang "chết" sẽ **sống dậy**:
xoá một admin từng đổi trạng thái đơn sẽ bị chặn vì còn dòng `booking_status_history.changed_by_id`.
Đó là hành vi **đúng như thiết kế**, nhưng phải thử tận tay và xác nhận thông báo ra tiếng Việt tử tế
kèm gợi ý thay thế, chứ không phải lỗi khoá ngoại trần trụi.

### Đánh số
**Tổng thành 22 use case** (UC001–UC022). Nhắc lại cảnh báo ở mục 2: con số này phải khớp ở **cả ba
nơi** — bảng danh sách use case, số bảng đặc tả 2.7, và số hình e-líp trên
`01. Biểu đồ Use Case tổng quát`.

---

## 10. VĂN PHONG BÁO CÁO — quy tắc bao trùm, đè lên mọi chỉ dẫn trước đó

> User yêu cầu. **Áp dụng cho toàn bộ `BAO_CAO.md`.** Mọi chỉ dẫn cũ bảo đưa route / tên phương
> thức / số gotcha vào báo cáo đều **HẾT HIỆU LỰC**.

### Nguyên tắc gốc
**Viết như thể tài liệu này được lập TRƯỚC khi viết một dòng mã nào.** Đây là tài liệu phân tích và
thiết kế, người đọc là giảng viên và khách hàng, không phải lập trình viên. Văn phong tự nhiên,
nghiệp vụ, giống hệt file mẫu `Nhom12_ApplicationDevelopment_Full.docx`
(bản trích ở `.tmp_ref/template_clean.md`).

### CẤM tuyệt đối trong báo cáo
| Loại | Ví dụ phải bỏ |
|---|---|
| Đường dẫn HTTP | `GET /tours/{id}`, `POST /account/reviews`, `/admin/contacts` |
| Mã trạng thái HTTP | "trả về 409", "404", "302" |
| Tên lớp / phương thức | `BookingService.placeOrder`, `ReviewService.submit()`, `AdminContactController` |
| Tên cột / bảng CSDL trong câu văn | `approved = false`, `txn_ref`, `booking_status_history` |
| Tên công nghệ, thư viện, chú giải | Spring, Thymeleaf, JPA, Hibernate, `@Version`, `LEFT JOIN FETCH`, `AttributeConverter` |
| Số gotcha, tên file mã nguồn | "gotcha #58", `02_schema.sql`, `messages.properties` |
| Thuật ngữ lập trình | "giao dịch roll back", "LazyInitializationException", "session", "endpoint" |

### Cách diễn đạt thay thế
- ~~"POST /account/reviews lưu `approved = false`"~~ → **"Hệ thống ghi nhận đánh giá ở trạng thái *chờ duyệt*, chưa hiển thị công khai."**
- ~~"`DELETE /api/admin/tours/3` trả 409"~~ → **"Hệ thống từ chối xoá và thông báo: «Không xoá được: còn 1 liên hệ đang hỏi về tour này»."**
- ~~"kiểm trùng `txn_ref` bằng Java"~~ → **"Hệ thống kiểm tra mã giao dịch đã tồn tại hay chưa trước khi ghi nhận."**
- ~~"giỏ hàng nằm trong HttpSession"~~ → **"Giỏ hàng được giữ tạm trong phiên làm việc của người dùng."**

### Vẫn ĐƯỢC giữ (vì chính file mẫu có)
- **Bảng đặc tả thực thể ở mục 3.2**: tên thuộc tính (`maKH`, `tenKH`), kiểu dữ liệu mức thiết kế
  (`String`, `int`, `boolean`, `LocalDate`), ràng buộc diễn đạt bằng lời ("Không rỗng",
  "10 số, bắt đầu bằng 0"), và phương thức mức thiết kế (`congDiem(int diem)`). File mẫu làm đúng
  như vậy — đây là **ký pháp thiết kế**, không phải mã nguồn.
- **Sơ đồ ERD và biểu đồ lớp** hiển thị tên bảng/cột — đó là bản chất của loại sơ đồ đó.
- Tên màn hình, tên chức năng, tên trạng thái nghiệp vụ (*Chờ duyệt*, *Đã huỷ*, *Hoàn tất*).

### Luồng sự kiện trong bảng đặc tả use case
Viết bằng hành động người dùng thấy được, đúng giọng file mẫu:
> *"1. Chọn chức năng «Đánh giá tour» ở trang chi tiết tour"* / *"2. Hiển thị biểu mẫu nhập số sao và nhận xét"*

**Không** viết: *"1. Gửi POST /account/reviews"* / *"2. ReviewService.submit() lưu bản ghi"*.

### Chương 4 — kiểm thử
Giữ nguyên tính trung thực (Pass / Chưa kiểm và lý do), nhưng diễn đạt bằng **thao tác người dùng**
và **câu thông báo thật hiện trên màn hình**, không phải lời gọi kỹ thuật.
**Bỏ hẳn** mục bàn về lỗi kỹ thuật nội bộ (thứ tự sắp xếp trùng, nạp quan hệ chậm…) — người đọc báo
cáo không cần biết, và nó phá vỡ giả định "tài liệu viết trước khi code".

### Phép tự kiểm bắt buộc
Sau khi sửa, `grep` trên `BAO_CAO.md` và trên text trích ngược từ `.docx`, **cả hai phải ra 0**:
```
GET /|POST /|PUT /|DELETE /|@|\.java|Spring|Thymeleaf|Hibernate|JPA|gotcha|409|404|LEFT JOIN
```
(trừ trong bảng đặc tả thực thể mục 3.2 và chú thích sơ đồ ERD).

---

## 11. PHONG CÁCH BIỂU ĐỒ HOẠT ĐỘNG & TUẦN TỰ — thay thế hoàn toàn mục 4

> User yêu cầu, kèm file mẫu mới. **Đè lên mục 4.** Chỉ dẫn cũ "lifeline đặt theo service thật
> trong mã nguồn" HẾT HIỆU LỰC.

**File mẫu:** `C:\Users\Khangg\Downloads\www\sy_project\Use case_1.docx` (đồ án web bán nhạc cụ).
Đã trích sẵn: 8 ảnh ở `.tmp_ref/uc1/`. Thứ tự: **ảnh lẻ = hoạt động, ảnh chẵn = tuần tự**.
- `image1/2` → UC Đăng ký tài khoản · `image3/4` → UC Đăng nhập
- `image5/6` → UC Quản lý giỏ hàng · `image7/8` → UC Đặt hàng và thanh toán (phức tạp nhất, xem kỹ)

### Nguyên tắc gốc
Cả hai loại biểu đồ **không được nhắc tới mã nguồn**. Cùng tinh thần với mục 10 (văn phong báo cáo):
giả định tài liệu được lập **trước khi** viết dòng mã nào.

### Biểu đồ hoạt động — THUẦN NGÔN NGỮ TỰ NHIÊN
- Hai làn dọc, tiêu đề đúng chữ **`Actor`** và **`System`**.
- Hộp hành động bo góc, nội dung là **câu tiếng Việt tự nhiên**:
  *"Chọn đăng ký tài khoản"* · *"Nhập thông tin cần thiết"* · *"Kiểm tra thông tin"* ·
  *"Hiển thị thông báo thành công"* · *"Gửi email xác nhận"*.
- **CẤM**: tên lớp, tên phương thức, đường dẫn, tên cột/bảng, mã trạng thái HTTP, tên công nghệ.
- Hình thoi rẽ nhánh có nhãn **`Có` / `Không`**; cạnh có nhãn điều kiện bằng lời
  (*"Dữ liệu hợp lệ"*, *"Chọn đăng ký lại"*, *"Chọn kết thúc"*).
- Nút khởi đầu chấm đen đặc, nút kết thúc chấm đen có vành.

### Biểu đồ tuần tự — tên MỨC THIẾT KẾ, không phải tên lớp Java
Thứ tự lifeline: **`Actor`** (hình người) → **`GiaoDien<Xxx>`** (boundary) →
**`Ctrl_<Xxx>`** (control) → **`<ThựcThể>`** (entity, có thể nhiều).

Ví dụ trong file mẫu: `Actor` → `GiaoDienDK` → `Ctrl_DangKy` → `TaiKhoan`.
Với TourBooking thì đặt kiểu `GiaoDienDatTour` → `Ctrl_DatTour` → `DonDatTour`, `DotKhoiHanh`…
**Không dùng** `ctrl_BookingService`, `:ReviewRepository`, `AdminContactController`.

**Quy tắc then chốt user nhấn mạnh — thông điệp Actor → Giao diện là HÀNH ĐỘNG CON NGƯỜI:**

| Chặng | Kiểu thông điệp | Ví dụ |
|---|---|---|
| `Actor` → `GiaoDien…` | **ngôn ngữ tự nhiên**, không dấu ngoặc, không tham số | `1: Nhập thông tin đăng ký` · `3: Nhấn "Đặt hàng"` |
| `GiaoDien…` → chính nó | lời gọi | `1.1: kiemTraThongTin(us,ps)` |
| `GiaoDien…` → `Ctrl_…` | lời gọi | `1.2: kiemTraTonTai(us,ps)` |
| `Ctrl_…` → thực thể | lời gọi | `1.2.1: getUser()` · `1.2.6: Add(NewObj_TK)` |
| trả về | nét đứt, tên dữ liệu | `1.2.2: userList` |
| thông báo ra giao diện | `msg("…")` | `1.2.5: msg("Đăng ký thành công")` |

- Đánh số **phân cấp**: `1`, `1.1`, `1.2`, `1.2.1`, `1.2.2`, `1.2.3`…
- Khung tổ hợp **`alt`** có `[điều kiện]` (lồng nhau được), **`loop`** khi lặp.
- Tạo đối tượng mới: mũi tên **nét đứt** `create()` trỏ tới đối tượng `NewObj_<Xxx>`.
- Hủy lifeline: dấu **X** ở cuối.

### Giữ nguyên
Tên 44 file biểu đồ (`AC-UCxxx. …`, `SQ-UCxxx. …`) **không đổi** — báo cáo đang trỏ vào đúng các
tên đó; chỉ nội dung bên trong được vẽ lại. Bốn biểu đồ còn lại (use case, 2 lớp, ERD) và
`so-do-phan-cap` / `screen-flow` **không thuộc phạm vi mục này**.

---

## 12. VÒNG ĐỜI ĐƠN HÀNG & NGHIỆP VỤ THỰC TẾ — chốt ngày 23/09 (user chọn làm cả 8 việc)

> Chốt bởi agent điều phối TRƯỚC khi giao. Các agent bám đúng mục này. Mọi con số là **mặc định**,
> đặt trong `application.properties` nhóm `app.booking.*` để đổi được, không viết cứng trong mã.

### 12.1 Máy trạng thái đơn hàng
```
CHỜ XÁC NHẬN (PENDING)  ──► ĐÃ XÁC NHẬN (CONFIRMED) ──► HOÀN TẤT (COMPLETED)
        │                          │
        └──────────► ĐÃ HUỶ (CANCELLED) ◄──┘
```
- Chỉ bốn chuyển trên là hợp lệ. **HOÀN TẤT và ĐÃ HUỶ là trạng thái cuối** — không chuyển tiếp.
  ⇒ Bỏ hẳn "khôi phục đơn đã huỷ". (Bản sửa lỗi 1.4 — tạo lại khoản thu khi khôi phục — trở thành
  thừa; gỡ nhánh khôi phục cho gọn.)
- **Được sang HOÀN TẤT** chỉ khi: ngày khởi hành ≤ hôm nay **và** đơn đã có khoản thu ĐÃ THANH TOÁN.
- Kiểm ở **tầng service** (mở rộng `BookingStatus` bằng hàm `canTransitionTo`). Giao diện admin chỉ
  liệt kê các trạng thái đích hợp lệ của đơn đang xem.
- Mọi lần chuyển vẫn ghi `booking_status_history`.

### 12.2 Hoàn tiền — không đổi schema
Bảng `payments` cho phép nhiều dòng mỗi đơn và đã có trạng thái `REFUNDED`.
- Huỷ một đơn **đã có khoản ĐÃ THANH TOÁN** ⇒ **giữ nguyên** dòng ĐÃ THANH TOÁN (lịch sử tiền vào), và
  **thêm một dòng mới** `status = REFUNDED`, `amount` = số tiền hoàn (số dương), `note` ghi lý do + tỉ lệ.
- Số tiền hoàn: **quản trị viên huỷ → hoàn 100%** (lỗi phía công ty). **Khách tự huỷ → theo chính sách 12.3.**
- Huỷ đơn **chưa** thu tiền ⇒ khoản CHỜ → THẤT BẠI như hiện nay, không có dòng hoàn.

### 12.3 UC023 — Khách tự huỷ đơn (use case MỚI, tác nhân AC02 Khách hàng)
Ở trang chi tiết đơn của khách. Chính sách mặc định (theo ngày còn lại tới khởi hành):
| Còn lại | Được tự huỷ? | Hoàn tiền (nếu đã thanh toán) |
|---|---|---|
| ≥ 7 ngày | Có | 100% |
| 3 – 6 ngày | Có | 50% |
| < 3 ngày | **Không** — hiện hướng dẫn liên hệ công ty | — |
- Chỉ huỷ được đơn CHỜ XÁC NHẬN hoặc ĐÃ XÁC NHẬN, và phải là đơn của chính mình.
- Trả chỗ, trả lại lượt dùng mã giảm giá (xoá dòng `coupon_usages`, giảm `used_count`) — để mã giới hạn
  1 lần/người không bị mất oan.
- Có hộp xác nhận hiện rõ **số tiền sẽ được hoàn** trước khi huỷ.
- Ghi lịch sử trạng thái với lý do "Khách tự huỷ".
- Tham số: `app.booking.self-cancel-min-days=3`, `app.booking.full-refund-min-days=7`,
  `app.booking.partial-refund-percent=50`.

### 12.4 Tự huỷ đơn chưa thanh toán quá hạn (hệ thống, thuộc luồng UC007/UC014)
- Đơn CHỜ XÁC NHẬN, chưa có khoản ĐÃ THANH TOÁN, đặt quá **24 giờ** ⇒ hệ thống tự huỷ: trả chỗ, trả lượt
  dùng mã, khoản CHỜ → THẤT BẠI, lịch sử ghi người đổi = hệ thống (`changed_by_id = NULL`), lý do "Quá hạn
  thanh toán".
- Chạy bằng `@Scheduled` định kỳ (ví dụ 15 phút). Tham số `app.booking.pending-expiry-hours=24`.
- ⚠️ **BẪY DỮ LIỆU MẪU**: nếu `03_seed_data.sql` có đơn CHỜ với ngày đặt cố định trong quá khứ, thì ngay
  lần khởi động đầu trên máy người chấm, bộ hẹn giờ sẽ huỷ sạch chúng. Phải sửa seed cho đơn CHỜ có ngày
  đặt **tương đối theo thời điểm chạy script** (ví dụ vài giờ trước `SYSDATETIME()`), và kiểm lại.

### 12.5 Hạn chót đặt tour
Đợt khởi hành chỉ **đặt được** khi ngày khởi hành ≥ hôm nay + **2 ngày**. Áp ở mọi đường: danh sách,
chi tiết, thêm giỏ, thanh toán, REST. Tham số `app.booking.cutoff-days=2`.
⚠️ Cùng bẫy: kiểm dữ liệu mẫu còn đợt nào đặt được sau khi áp luật.

### 12.6 Khoá sửa đợt khởi hành đã có khách
Đợt đã có người đặt (còn đơn chưa huỷ) ⇒ **không đổi được ngày khởi hành**, và **không giảm tổng số chỗ
xuống dưới số chỗ đã bán**. Báo lỗi tiếng Việt, gợi ý tạo đợt mới.

### 12.7 Nhập danh sách hành khách (sửa UC007)
- Ở bước thanh toán, mỗi dòng giỏ có N người lớn + M trẻ em ⇒ nhập đủ N+M hành khách (tối thiểu họ tên;
  các cột khác theo bảng `booking_passengers`). Loại hành khách tự suy ra, người dùng không chọn.
- **Bất biến bắt buộc** (đã có từ Phase 7): số hành khách theo từng loại **luôn khớp** `num_adults` /
  `num_children` của dòng đơn — sau **mọi** thao tác, kể cả khi quản trị viên sửa số khách (yêu cầu
  của đề bài, không được chặn). Agent tự thiết kế cách giữ bất biến này ở màn quản trị với giao diện
  tối thiểu, và phải chứng minh bằng truy vấn đếm lệch = 0.

### 12.8 Trang chi tiết riêng cho quản trị viên [Đề bài]
Phiếu ĐK đòi "xem chi tiết từng tour/loại" và "xem chi tiết từng tài khoản". Thêm trang **chỉ xem**
riêng (tách khỏi form sửa) cho tour, danh mục, tài khoản; chi tiết tài khoản hiện **lịch sử đơn đã đặt**,
**không bao giờ** hiện mật khẩu (kể cả bản băm). Có nút chuyển sang form sửa.

### 12.9 Số liệu thống kê
"Doanh thu" và "Tour bán chạy" **chỉ tính đơn ĐÃ XÁC NHẬN + HOÀN TẤT** (loại CHỜ và ĐÃ HUỶ).

### 12.10 Đánh số use case
Thêm **UC023 — Huỷ đơn đặt tour** (AC02). **Tổng: 23 use case.** Nhắc lại mục 2: con số phải khớp ở
cả ba nơi — bảng danh sách, số bảng đặc tả 2.7, số hình e-líp trên biểu đồ use case tổng quát.
Luồng **sửa đổi**: UC007 (nhập hành khách, hạn chót, đơn quá hạn), UC014 (máy trạng thái, hoàn tiền,
tự huỷ quá hạn), UC013 (khoá sửa đợt), UC016 (cách tính số liệu), UC011/UC012/UC015 (trang chi tiết).

---

## 13. UC024–UC026 — Thêm / Sửa / Xoá danh mục tour (user yêu cầu, 24/09)

- Ba use case con của **UC011 Quản lý danh mục tour**, nối bằng **«extend»** (UC024/025/026 ⟶ «extend» ⟶ UC011).
  Tác nhân AC03 Quản trị viên. Cách tách này giống file mẫu Nhóm 12 (Thêm / Sửa / Xoá thuốc là ba use case riêng).
- Chúng **đại diện cho mẫu CRUD** dùng chung ở các chức năng quản lý khác (tour, đợt khởi hành, tài khoản, mã
  khuyến mãi…); báo cáo ghi rõ điều này thay vì đặc tả CRUD lặp lại cho từng chức năng.
- Luồng và câu thông báo phải **khớp hành vi thật** của màn quản lý danh mục trong ứng dụng.
- **Tổng: 26 use case.** Không vẽ UML — user tự vẽ; báo cáo chỉ chừa chỗ chèn hình.

---

## 14. XOÁ 5 USE CASE CRUD & ĐÁNH SỐ LẠI (user yêu cầu, 24/09) — ĐÈ lên mục 2 và 13

User xoá khỏi báo cáo: **UC011 Quản lý danh mục tour · UC012 Quản lý tour và ảnh · UC013 Quản lý đợt khởi hành
· UC015 Quản lý tài khoản · UC019 Quản lý mã khuyến mãi** — vì đều là CRUD đơn giản, đã có **Thêm/Sửa/Xoá danh
mục tour** làm đại diện. Các chức năng này **vẫn tồn tại trong ứng dụng**, chỉ không đặc tả riêng.

**Tổng: 21 use case.** Đánh số lại liền mạch, **giữ nguyên thứ tự tương đối**:

| Mã cũ | Mã MỚI | Tên |
|---|---|---|
| UC001–UC010 | UC001–UC010 | (giữ nguyên) |
| UC014 | **UC011** | Quản lý đơn đặt tour |
| UC016 | **UC012** | Xem thống kê doanh thu |
| UC017 | **UC013** | Áp dụng mã giảm giá |
| UC018 | **UC014** | Đánh giá tour đã đi |
| UC020 | **UC015** | Kiểm duyệt đánh giá |
| UC021 | **UC016** | Gửi liên hệ |
| UC022 | **UC017** | Quản lý liên hệ |
| UC023 | **UC018** | Huỷ đơn đặt tour |
| UC024 | **UC019** | Thêm danh mục tour |
| UC025 | **UC020** | Sửa danh mục tour |
| UC026 | **UC021** | Xoá danh mục tour |

- UC019–UC021 (Thêm/Sửa/Xoá danh mục) **không còn «extend» UC011** (UC011 cũ đã xoá) — là use case độc lập của Quản trị viên.
- Câu *"mẫu Thêm/Sửa/Xoá áp dụng tương tự cho tour, đợt khởi hành, tài khoản, mã khuyến mãi…"* phải **được giữ lại** trong báo cáo (dời vào đặc tả UC019 hoặc ghi chú dưới bảng danh sách use case).
- Ca kiểm thử của các chức năng bị bỏ đặc tả **vẫn giữ** (chức năng vẫn chạy thật); chỉ bỏ mã UC cũ, ghi tên chức năng.
- File PNG trong `docs/uml/png/` vẫn mang **mã cũ** — dùng bảng trên để tra.

---

## 15. ĐỔI TÊN "ĐỢT KHỞI HÀNH" → "LỊCH KHỞI HÀNH" (user chốt 30/09)

Giảng viên không chấp nhận tên "Quản lý đợt khởi hành" (nội dung thì đã đồng ý). User chọn **"Lịch khởi hành"**.
- Chức năng: **"Quản lý lịch khởi hành"**. Thực thể trong câu văn: *lịch khởi hành* (một tour có nhiều lịch khởi hành).
- Phạm vi user giao: **báo cáo** (agent docs). Website (29 dòng chữ trong messages) **chưa** giao — hỏi user nếu muốn đổi đồng bộ.
- **Không thay máy móc**: viết lại từng câu cho xuôi; chữ "đợt" đứng một mình ("đợt này", "mỗi đợt", "các đợt") phải xét ngữ cảnh.
- **Tránh nhầm với "lịch trình"** — báo cáo đã dùng "lịch trình" cho nội dung từng ngày của tour.
- Tên trong mã nguồn (`TourDeparture`, bảng `tour_departures`) **không đổi**.

---

## 15. SẮP XẾP LẠI USE CASE THEO BIỂU ĐỒ TỔNG QUÁT (báo cáo Word) — ĐÈ lên mục 14

Báo cáo Word đánh số lại 21 use case theo thứ tự biểu đồ tổng quát (mã nội bộ trong tệp này không đổi). Bảng ánh xạ:

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
