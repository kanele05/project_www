# BÀN GIAO PHIÊN LÀM VIỆC — đọc file này ĐẦU TIÊN sau khi compact

> Viết ngày 30/09/2026, ngay trước khi user compact phiên "Đọc plan.md".
> Nguyên văn toàn bộ phiên (hội thoại + transcript mọi subagent):
> `.tmp_ref/session-export-2026-09-30.zip` (bản gốc: `C:\Users\Khangg\Downloads\session-export-1790746980080.zip`).
> Các file nguồn chân lý khác: `PLAN.md` (tiến độ + gotcha tới #70), `docs/report/SPEC_CHUNG.md` (mục 0–15),
> `CLAUDE.md`, `docs/uml/README.md`.

---

## 1. ĐÃ XONG 30/09: đổi "đợt khởi hành" → "lịch khởi hành"
- Báo cáo: 0 chữ "đợt", 120 "lịch khởi hành"; cấu trúc/hình giữ nguyên. Sao lưu: `BaoCao_TourBooking.backup-2026-09-30-truoc-doi-lich-khoi-hanh.docx`.
- Website: đã đổi messages (vi + mặc định), template, chú thích Java; commit `afde63e` đã push. Bản tiếng Anh giữ "departure".
- User còn phải: Update Field mục lục; sửa chữ trong các hình UML tự vẽ.

## 1b. (LƯU TRỮ) Việc đổi tên như đã giao trước khi compact

**Đổi "đợt khởi hành" → "lịch khởi hành" trong BÁO CÁO**, giao cho agent docs. Chi tiết: `SPEC_CHUNG.md` **mục 15**.
- User chọn tên **"Lịch khởi hành"**, chức năng **"Quản lý lịch khởi hành"**. Lý do: giảng viên không chấp nhận tên cũ
  (nội dung thì đã đồng ý).
- Báo cáo có **115** chỗ "đợt khởi hành" + **3** chỗ "Quản lý đợt khởi hành" (đếm ngày 30/09).
- Phạm vi user nói: "nhờ agent docs kiểm tra và đổi tên" → **chỉ báo cáo**. Website có **29 dòng** trong
  `messages*.properties` vẫn ghi "Đợt khởi hành" — **chưa được giao**; nhắc user (lúc demo cô sẽ thấy tên cũ trên
  màn quản trị) và hỏi có muốn đổi đồng bộ không. Nếu đổi website thì phải commit + push.
- Cách làm: viết lại **từng câu cho xuôi**, không thay máy móc; "đợt" đứng một mình xét theo ngữ cảnh; tránh nhầm
  với "lịch trình" (đã dùng cho nội dung từng ngày của tour); chú thích hình/bảng có chữ đó cũng đổi.
- Quy trình docx như mọi lần (mục 3 dưới đây). Agent docs có thể nối lại được: id `a24bd1c51fe15e4ae`
  (đã nắm cấu trúc file; nếu không nối được thì tạo agent mới, model `sonnet`).

---

## 2. SỞ THÍCH / QUYẾT ĐỊNH CỦA USER (bắt buộc giữ)

- Trả lời bằng **tiếng Việt**. User là Lê Hoàng Khang, sinh viên IUH, đồ án nhóm môn Lập trình WWW (Java),
  GV ThS. Đặng Thị Thu Hà, đề tài 52 "Website giới thiệu tour du lịch và cho phép đăng ký tour trực tuyến".
- **Model subagent**: mặc định **`sonnet`** (tiết kiệm usage); **review** thì dùng **`opus`** (user muốn "model mới nhất").
- **Lỗi** tìm được khi review → **tự sửa hết rồi báo**. **Thiếu nghiệp vụ / tính năng mới** → **trình user chọn** trước.
- **Push thẳng lên `main`**, không tạo nhánh/PR. Remote: `https://github.com/kanele05/project_www.git` (repo **công khai**).
  Commit kết thúc bằng: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- `.gitignore` loại: `.claude/`, `.tmp_ref/`, `docs/`, `uploads/`, `CLAUDE.md`, `PLAN.md` (user chỉ định).
- **Mật khẩu CSDL `Tour@2026#IUH` để nguyên trong `application.yml`** — user đã quyết, đừng đổi.
- **Không README** — user đã từ chối hai lần.
- **Không comment dài trong mã**: đã xoá toàn bộ, chỉ còn **1 dòng tiếng Việt cho mỗi lớp/hàm**. Mã mới viết thêm
  cũng theo kiểu này.
- **UML: user TỰ vẽ và tự chụp** — agent **không** vẽ lại UML nữa, chỉ chừa chỗ chèn hình trong báo cáo.
- Diagram SSMS: đã xoá khi dựng lại CSDL (user cho phép).

---

## 3. BÁO CÁO — QUY TẮC SỐNG CÒN

- File gốc duy nhất: **`docs/report/BaoCao_TourBooking.docx`** — **bản user sửa tay trong Word** (~14 MB, user đã chèn
  thêm hình tự vẽ).
- **TUYỆT ĐỐI KHÔNG CHẠY `docs/report/build_docx.py`** — nó sinh lại `.docx` từ `BAO_CAO.md` (đã lỗi thời) và **ghi đè
  toàn bộ chỉnh sửa tay**. Luôn sửa thẳng `.docx` bằng `python-docx`.
- Trước mỗi lần sửa: **kiểm Word khoá** (`ls -a docs/report | grep '^~\$'`) và **tạo bản sao lưu**. Các bản đang có:
  `backup-2026-09-23` (bản user up đầu tiên) · `…24_035955` · `…24_174318` · `…24_182032-truoc-khi-xoa-UC` ·
  `…24_220208-truoc-va-3-ma` (mới nhất).
- **Word hay tách một chuỗi thành nhiều run** (ví dụ `UC0`+`23`) → thay ở mức đoạn; đổi hàng loạt thì **một lượt theo
  từ điển**, không đổi nối tiếp từng cặp (đã từng hỏng dây chuyền).
- **Văn phong (SPEC mục 10)**: viết như tài liệu lập **trước khi viết mã** — cấm đường dẫn HTTP, mã trạng thái, tên lớp,
  tên cột trong câu văn, số gotcha. **Ngoại lệ Chương 4** (Hiện thực ứng dụng Web): được nêu tên công nghệ
  (Spring Boot, JPA, Security, Thymeleaf, SQL Server, JavaMail) vì CLO5/CLO6 chấm phần này.
- **Mục lục là field Word** → luôn nhắc user tự bấm *Update Field → Update entire table*.
- Kiểm thử ở Chương 4 chỉ ghi **Pass khi có bằng chứng thật** trong `PLAN.md`; không có thì "Chưa kiểm".

### Trạng thái báo cáo hiện tại
- 5 chương theo Phiếu ĐK: **Giới thiệu đề tài · Phân tích yêu cầu · Phân tích thiết kế · Hiện thực ứng dụng Web
  (4.8 là Kiểm thử) · Kết luận**. Chương 1 có mục 1.5 lý do chọn đề tài.
- **21 use case** sau khi xoá UC011/012/013/015/019 cũ (CRUD, đã có Thêm/Sửa/Xoá danh mục làm đại diện).
  Bảng ánh xạ mã cũ → mới: `SPEC_CHUNG.md` **mục 14** (UC014→011, 016→012, 017→013, 018→014, 020→015, 021→016,
  022→017, 023→018 Huỷ đơn, 024→019 Thêm danh mục, 025→020 Sửa danh mục, 026→021 Xoá danh mục).
  ⚠️ File PNG trong `docs/uml/png/` vẫn mang **mã cũ**.
- Bảng câu hỏi 2.2: 22 câu (có câu hỏi nghiệp vụ thực tế). Kiểm thử: **33 Pass / 4 Chưa kiểm / 37**
  (TS36 Sửa danh mục chưa có bằng chứng — user có thể tự thao tác rồi ghi).
- Chú thích hình liền mạch: Chương 2 = 2.1–2.45, Chương 3 = 3.1–3.11. 0 mã UC ngoài UC001–UC021.
- Hình UML user cần tự làm/sửa: Use Case tổng quát (Hình 2.2 — bỏ 5 UC đã xoá, thêm UC018–UC021 mới), sơ đồ phân cấp
  (2.1), hình activity/sequence của các UC đổi luồng (Đặt tour, Quản lý đơn, Đăng ký…), các chỗ đánh dấu đỏ
  `[Chèn hình: …]` nếu còn.

---

## 4. MÃ NGUỒN & GITHUB

- `main` trên GitHub ở **`d69fa09`** (email thật, sau `afde63e` đổi tên lịch khởi hành, trước đó `cb536de`). Lịch sử: `2250677` (khởi tạo) → `f9e668a` (thư đăng ký + 16 lỗi) → `41903d9`
  (vòng đời đơn: máy trạng thái, hoàn tiền, khách tự huỷ, tự huỷ quá hạn, hạn chót, khoá sửa đợt, thống kê) →
  `3d6ed2b` (nhập hành khách + trang chi tiết admin) → `33efd1d` (khoá bi quan chống ghi đè đồng thời, sửa seed) →
  `cb536de` (bỏ comment, giữ 1 dòng mô tả).
- **THAY ĐỔI CỤC BỘ CHƯA COMMIT/PUSH** (4 file): email người gửi đổi sang `${MAIL_FROM:khanglehoang2602@gmail.com}`
  trong `application.yml`, và email liên hệ trên `about.html`, `contact.html`, `fragments/layout.html`.
  **ĐÃ PUSH 30/09 (`d69fa09`), user đồng ý.**
- Kiểm nhanh sau mọi thay đổi mã: `./mvnw -o clean compile`; đối chiếu 3 file messages (hiện **756 khoá**, lệnh ở cuối
  PLAN.md); `grep -c "</html>"`; bất biến hành khách = 0; số chỗ các đợt khớp.
- Môi trường: `JAVA_HOME=/c/Users/Khangg/.jdks/corretto-23.0.2`, app cổng **8081**, SQL Server local,
  `sqlcmd` luôn `-f 65001`. User chạy app trong IntelliJ (cấu hình `TourBookingApplication`, JDK 25; cấu hình
  `TourbookingApplication` có dấu X đỏ là hỏng).
- CSDL `TourBookingDB`: 16 bảng, 0 CHECK/function/procedure/trigger; chủ sở hữu `sa`; `@@SERVERNAME` còn tên máy cũ
  `LAPTOP-AH4SP2FB` (máy nay là `KHANG-LE`) — chưa sửa, cần restart service nếu sửa.

## 5. EMAIL SMTP — user đang làm dở
- App mặc định `app.mail.mode: console` (thư chỉ in ra log). Gửi thật: biến môi trường
  `APP_MAIL_MODE=smtp;MAIL_USERNAME=khanglehoang2602@gmail.com;MAIL_PASSWORD=<App Password 16 ký tự, bỏ khoảng trắng>`
  trong IntelliJ (*Modify options → Environment variables*).
- User **đã dán App Password vào chat** → đã khuyên thu hồi và tạo mã mới. **Không bao giờ chép lại mã đó.**
- Chế độ `smtp` **chưa từng được thử với tài khoản thật**. Nếu user gửi log lỗi: `535` = sai mật khẩu/chưa bật 2 bước;
  timeout = mạng chặn cổng 587.

## 6. VIỆC CÒN MỞ (không gấp)
- Chưa có kiểm thử tự động · chưa có luồng quên mật khẩu · chưa có giá em bé · mã giảm giá chưa gắn theo tour ·
  quy tắc "không xoá admin cuối" chưa cô lập được bằng thao tác thật · TS36 chưa kiểm.
- Tác vụ hẹn giờ `tourbooking-smtp-va-chuong-5` **đã tắt** (việc đã làm trong phiên).

## 7. BÀI HỌC CÁCH LÀM (đã chứng minh trong phiên)
- **Chốt thiết kế vào `SPEC_CHUNG.md` trước khi giao agent chạy song song** — hai lần không chốt là hai lần làm lại.
- Agent chạy **tuần tự** khi cùng sửa `BookingService` / ba file messages.
- **Luôn tự kiểm chứng độc lập** kết quả agent (đã bắt được: danh sách hình UML thiếu, 3 mã UC sai tên, chú thích lộ
  tên file…).
- Hết hạn mức giữa chừng: kiểm hiện trạng trên đĩa trước, rồi `SendMessage` nối lại agent với tóm tắt chỗ nó dừng.
- Commit mốc cục bộ trước mỗi đợt thay đổi lớn.
