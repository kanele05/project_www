/* ===========================================================================
   04_sample_queries.sql - Các truy vấn minh hoạ, dùng cho báo cáo

   File này KHÔNG thay đổi dữ liệu. Mục đích là chứng minh cấu trúc CSDL đáp ứng
   được các yêu cầu của đề bài, và cung cấp sẵn câu lệnh để chụp màn hình đưa
   vào báo cáo.

   Chạy sau 02_schema.sql và 03_seed_data.sql:
     sqlcmd -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' \
            -d TourBookingDB -C -i database/04_sample_queries.sql
   =========================================================================== */

USE TourBookingDB;
GO

/* ---------------------------------------------------------------------------
   A. CHỨNG MINH TUÂN THỦ RÀNG BUỘC CỦA ĐỀ BÀI
   Đề bài cấm Function, Stored Procedure, Trigger và CHECK constraint.
   Cả bốn câu dưới đây phải trả về 0.
   --------------------------------------------------------------------------- */
SELECT N'CHECK constraint'  AS doi_tuong, COUNT(*) AS so_luong FROM sys.check_constraints
UNION ALL
SELECT N'Function',         COUNT(*) FROM sys.objects WHERE type IN ('FN', 'IF', 'TF')
UNION ALL
SELECT N'Stored Procedure', COUNT(*) FROM sys.procedures
UNION ALL
SELECT N'Trigger',          COUNT(*) FROM sys.triggers;
GO

/* Giỏ hàng được lưu trong Session của ứng dụng, không đụng tới CSDL.
   Câu này phải trả về 0 dòng. */
SELECT name AS bang_lien_quan_gio_hang
FROM sys.tables
WHERE name LIKE '%cart%' OR name LIKE '%gio_hang%';
GO

/* ---------------------------------------------------------------------------
   B. BỐN TRUY VẤN CHẶN XOÁ
   Ứng dụng gọi đúng các phép đếm này (bằng Spring Data: countByCategoryId,
   countByDeparture_Tour_Id, countByDepartureId, countByUserId) TRƯỚC khi xoá.
   Còn > 0 nghĩa là không được xoá, và người dùng nhận thông báo tiếng Việt kèm
   gợi ý "vô hiệu hoá" thay vì xoá.
   --------------------------------------------------------------------------- */

/* B1. Danh mục nào đang còn tour -> không xoá được */
SELECT c.id, c.name AS danh_muc, COUNT(t.id) AS so_tour
FROM tour_categories c
LEFT JOIN tours t ON t.category_id = c.id
GROUP BY c.id, c.name
ORDER BY so_tour DESC;
GO

/* B2. Tour nào đã có khách đặt -> không xoá được */
SELECT t.id, t.code, t.name AS tour, COUNT(bd.id) AS so_dong_dat
FROM tours t
LEFT JOIN tour_departures d  ON d.tour_id = t.id
LEFT JOIN booking_details bd ON bd.departure_id = d.id
GROUP BY t.id, t.code, t.name
HAVING COUNT(bd.id) > 0
ORDER BY so_dong_dat DESC;
GO

/* B3. Đợt khởi hành nào đã có khách đặt -> không xoá được */
SELECT d.id AS dot_khoi_hanh, t.name AS tour, d.departure_date AS ngay_di,
       COUNT(bd.id) AS so_dong_dat
FROM tour_departures d
JOIN tours t             ON t.id = d.tour_id
JOIN booking_details bd  ON bd.departure_id = d.id
GROUP BY d.id, t.name, d.departure_date
ORDER BY d.id;
GO

/* B4. Tài khoản nào đã từng đặt tour -> không xoá được.
   Ngoài ra ứng dụng còn chặn tự xoá chính mình và xoá quản trị viên cuối cùng. */
SELECT u.id, u.full_name AS ho_ten, u.email, u.role AS vai_tro,
       COUNT(b.id) AS so_don
FROM users u
LEFT JOIN bookings b ON b.user_id = u.id
GROUP BY u.id, u.full_name, u.email, u.role
ORDER BY so_don DESC;
GO

/* Số quản trị viên đang hoạt động - phải >= 1 mới cho xoá bớt */
SELECT COUNT(*) AS so_quan_tri_vien_dang_hoat_dong
FROM users
WHERE role = 'ADMIN' AND enabled = 1;
GO

/* ---------------------------------------------------------------------------
   C. TÌM KIẾM KHÔNG DẤU
   Cột search_text do ứng dụng dựng bằng Java. Cần nó vì collation
   Vietnamese_CI_AI bỏ được dấu thanh (N'à' = N'a' đúng) nhưng KHÔNG coi Đ là D
   (N'Đ' = N'D' sai) - hai câu đầu dưới đây chứng minh điều đó.
   --------------------------------------------------------------------------- */
SELECT CASE WHEN N'à' = N'a' THEN N'BẰNG NHAU' ELSE N'KHÁC NHAU' END AS [a_huyen_so_voi_a],
       CASE WHEN N'Đ' = N'D' THEN N'BẰNG NHAU' ELSE N'KHÁC NHAU' END AS [D_gach_ngang_so_voi_D];
GO

/* Tìm thẳng trên cột name: gõ không dấu KHÔNG ra kết quả */
SELECT COUNT(*) AS tim_tren_cot_name FROM tours WHERE name LIKE N'%da lat%';

/* Tìm trên search_text: ra đúng tour Đà Lạt */
SELECT id, code, name FROM tours WHERE search_text LIKE '%da lat%';
GO

/* ---------------------------------------------------------------------------
   D. TRANG DANH SÁCH TOUR (trang công khai)
   --------------------------------------------------------------------------- */

/* D1. Tour nổi bật ở trang chủ */
SELECT t.code, t.name AS tour, c.name AS danh_muc, t.base_price AS gia_tu
FROM tours t
JOIN tour_categories c ON c.id = t.category_id
WHERE t.featured = 1 AND t.active = 1
ORDER BY t.created_at DESC;
GO

/* D2. Lọc theo danh mục + khoảng giá, sắp xếp theo giá tăng dần */
SELECT t.code, t.name AS tour, t.destination AS diem_den, t.base_price AS gia
FROM tours t
WHERE t.active = 1
  AND t.category_id = 1
  AND t.base_price BETWEEN 3000000 AND 7000000
ORDER BY t.base_price ASC;
GO

/* D3. Các đợt khởi hành khách còn đặt được của một tour
   (đúng dữ liệu mà web service /api/tours/{id}/departures trả về) */
SELECT d.id, d.departure_date AS ngay_di, d.return_date AS ngay_ve,
       d.price_adult AS gia_nguoi_lon, d.price_child AS gia_tre_em,
       d.available_seats AS con_trong
FROM tour_departures d
WHERE d.tour_id = 1
  AND d.active = 1
  AND d.departure_date > CAST(GETDATE() AS DATE)
  AND d.available_seats > 0
ORDER BY d.departure_date;
GO

/* ---------------------------------------------------------------------------
   E. BÁO CÁO CHO MÀN HÌNH QUẢN TRỊ
   --------------------------------------------------------------------------- */

/* E1. Doanh thu theo tháng - không tính đơn đã huỷ */
SELECT YEAR(b.booking_date) AS nam, MONTH(b.booking_date) AS thang,
       COUNT(*) AS so_don, SUM(b.total_amount) AS doanh_thu
FROM bookings b
WHERE b.status <> 'CANCELLED'
GROUP BY YEAR(b.booking_date), MONTH(b.booking_date)
ORDER BY nam, thang;
GO

/* E2. Tour bán chạy, tính theo số khách */
SELECT bd.tour_name_snapshot AS tour,
       SUM(bd.num_adults + bd.num_children) AS so_khach,
       SUM(bd.subtotal) AS doanh_thu
FROM booking_details bd
JOIN bookings b ON b.id = bd.booking_id
WHERE b.status <> 'CANCELLED'
GROUP BY bd.tour_name_snapshot
ORDER BY so_khach DESC;
GO

/* E3. Số đơn theo từng trạng thái */
SELECT status AS trang_thai, COUNT(*) AS so_don, SUM(total_amount) AS tong_tien
FROM bookings
GROUP BY status
ORDER BY so_don DESC;
GO

/* E4. Tình hình lấp đầy của các đợt đã bán được chỗ */
SELECT t.name AS tour, d.departure_date AS ngay_di,
       d.total_seats AS tong_cho, d.available_seats AS con_trong,
       d.total_seats - d.available_seats AS da_ban,
       CAST(100.0 * (d.total_seats - d.available_seats) / d.total_seats AS DECIMAL(5,1)) AS ty_le_lap_day
FROM tour_departures d
JOIN tours t ON t.id = d.tour_id
WHERE d.available_seats < d.total_seats
ORDER BY ty_le_lap_day DESC;
GO

/* ---------------------------------------------------------------------------
   F. KIỂM TRA TÍNH NHẤT QUÁN CỦA DỮ LIỆU
   Ba câu dưới đây đều PHẢI trả về 0 dòng. Đây chính là các bất biến mà tầng
   service của ứng dụng chịu trách nhiệm giữ gìn (thay cho CHECK constraint).
   --------------------------------------------------------------------------- */

/* F1. Thành tiền của mỗi dòng phải bằng số khách nhân đơn giá */
SELECT id, subtotal, unit_price_adult * num_adults + unit_price_child * num_children AS dung_ra_phai_la
FROM booking_details
WHERE subtotal <> unit_price_adult * num_adults + unit_price_child * num_children;
GO

/* F2. Tổng tiền của đơn phải bằng tổng các dòng chi tiết */
SELECT b.id, b.code, b.total_amount, SUM(bd.subtotal) AS tong_cac_dong
FROM bookings b
JOIN booking_details bd ON bd.booking_id = b.id
GROUP BY b.id, b.code, b.total_amount
HAVING b.total_amount <> SUM(bd.subtotal);
GO

/* F3. Số chỗ còn trống phải nằm trong khoảng [0, tổng số chỗ] */
SELECT id, total_seats, available_seats
FROM tour_departures
WHERE available_seats < 0 OR available_seats > total_seats;
GO

/* F4. Số chỗ đã bán phải khớp với tổng số khách của các đơn chưa huỷ */
SELECT d.id, d.total_seats - d.available_seats AS da_ban_theo_cot,
       ISNULL(x.so_khach, 0) AS da_ban_theo_don
FROM tour_departures d
LEFT JOIN (SELECT bd.departure_id, SUM(bd.num_adults + bd.num_children) AS so_khach
           FROM booking_details bd
           JOIN bookings b ON b.id = bd.booking_id
           WHERE b.status <> 'CANCELLED'
           GROUP BY bd.departure_id) AS x ON x.departure_id = d.id
WHERE d.total_seats - d.available_seats <> ISNULL(x.so_khach, 0);
GO

/* ---------------------------------------------------------------------------
   G. AN TOÀN MẬT KHẨU
   Mọi mật khẩu phải là chuỗi băm BCrypt (bắt đầu bằng $2a$ và dài 60 ký tự).
   Câu thứ hai phải trả về 0 dòng.
   --------------------------------------------------------------------------- */
SELECT email, LEFT(password, 7) + N'...' AS dau_chuoi_bam, LEN(password) AS do_dai
FROM users;

SELECT email FROM users WHERE password NOT LIKE '$2a$%' OR LEN(password) <> 60;
GO
