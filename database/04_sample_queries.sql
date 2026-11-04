-- Các truy vấn minh hoạ cho báo cáo, không thay đổi dữ liệu.
USE TourBookingDB;

SELECT 'CHECK constraint'  AS doi_tuong, COUNT(*) AS so_luong FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE()
UNION ALL
SELECT 'Function',         COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_TYPE = 'FUNCTION'
UNION ALL
SELECT 'Stored Procedure', COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_TYPE = 'PROCEDURE'
UNION ALL
SELECT 'Trigger',          COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE();
SELECT TABLE_NAME AS bang_lien_quan_gio_hang
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME LIKE '%cart%' OR TABLE_NAME LIKE '%gio_hang%');
SELECT c.id, c.name AS danh_muc, COUNT(t.id) AS so_tour
FROM tour_categories c
LEFT JOIN tours t ON t.category_id = c.id
GROUP BY c.id, c.name
ORDER BY so_tour DESC;
SELECT t.id, t.code, t.name AS tour, COUNT(bd.id) AS so_dong_dat
FROM tours t
LEFT JOIN tour_departures d  ON d.tour_id = t.id
LEFT JOIN booking_details bd ON bd.departure_id = d.id
GROUP BY t.id, t.code, t.name
HAVING COUNT(bd.id) > 0
ORDER BY so_dong_dat DESC;
SELECT d.id AS dot_khoi_hanh, t.name AS tour, d.departure_date AS ngay_di,
       COUNT(bd.id) AS so_dong_dat
FROM tour_departures d
JOIN tours t             ON t.id = d.tour_id
JOIN booking_details bd  ON bd.departure_id = d.id
GROUP BY d.id, t.name, d.departure_date
ORDER BY d.id;
SELECT u.id, u.full_name AS ho_ten, u.email, u.role AS vai_tro,
       COUNT(b.id) AS so_don
FROM users u
LEFT JOIN bookings b ON b.user_id = u.id
GROUP BY u.id, u.full_name, u.email, u.role
ORDER BY so_don DESC;
SELECT COUNT(*) AS so_quan_tri_vien_dang_hoat_dong
FROM users
WHERE role = 'ADMIN' AND enabled = 1;
SELECT CASE WHEN 'à' = 'a' THEN 'BẰNG NHAU' ELSE 'KHÁC NHAU' END AS `a_huyen_so_voi_a`,
       CASE WHEN 'Đ' = 'D' THEN 'BẰNG NHAU' ELSE 'KHÁC NHAU' END AS `D_gach_ngang_so_voi_D`;
SELECT COUNT(*) AS tim_tren_cot_name FROM tours WHERE name LIKE '%da lat%';

SELECT id, code, name FROM tours WHERE search_text LIKE '%da lat%';
SELECT t.code, t.name AS tour, c.name AS danh_muc, t.base_price AS gia_tu
FROM tours t
JOIN tour_categories c ON c.id = t.category_id
WHERE t.featured = 1 AND t.active = 1
ORDER BY t.created_at DESC;
SELECT t.code, t.name AS tour, t.destination AS diem_den, t.base_price AS gia
FROM tours t
WHERE t.active = 1
  AND t.category_id = 1
  AND t.base_price BETWEEN 3000000 AND 7000000
ORDER BY t.base_price ASC;
SELECT d.id, d.departure_date AS ngay_di, d.return_date AS ngay_ve,
       d.price_adult AS gia_nguoi_lon, d.price_child AS gia_tre_em,
       d.available_seats AS con_trong
FROM tour_departures d
WHERE d.tour_id = 1
  AND d.active = 1
  AND d.departure_date > CURDATE()
  AND d.available_seats > 0
ORDER BY d.departure_date;
SELECT YEAR(b.booking_date) AS nam, MONTH(b.booking_date) AS thang,
       COUNT(*) AS so_don, SUM(b.total_amount) AS doanh_thu
FROM bookings b
WHERE b.status <> 'CANCELLED'
GROUP BY YEAR(b.booking_date), MONTH(b.booking_date)
ORDER BY nam, thang;
SELECT bd.tour_name_snapshot AS tour,
       SUM(bd.num_adults + bd.num_children) AS so_khach,
       SUM(bd.subtotal) AS doanh_thu
FROM booking_details bd
JOIN bookings b ON b.id = bd.booking_id
WHERE b.status <> 'CANCELLED'
GROUP BY bd.tour_name_snapshot
ORDER BY so_khach DESC;
SELECT status AS trang_thai, COUNT(*) AS so_don, SUM(total_amount) AS tong_tien
FROM bookings
GROUP BY status
ORDER BY so_don DESC;
SELECT t.name AS tour, d.departure_date AS ngay_di,
       d.total_seats AS tong_cho, d.available_seats AS con_trong,
       d.total_seats - d.available_seats AS da_ban,
       CAST(100.0 * (d.total_seats - d.available_seats) / d.total_seats AS DECIMAL(5,1)) AS ty_le_lap_day
FROM tour_departures d
JOIN tours t ON t.id = d.tour_id
WHERE d.available_seats < d.total_seats
ORDER BY ty_le_lap_day DESC;
SELECT id, subtotal, unit_price_adult * num_adults + unit_price_child * num_children AS dung_ra_phai_la
FROM booking_details
WHERE subtotal <> unit_price_adult * num_adults + unit_price_child * num_children;
SELECT b.id, b.code, b.total_amount, SUM(bd.subtotal) AS tong_cac_dong
FROM bookings b
JOIN booking_details bd ON bd.booking_id = b.id
GROUP BY b.id, b.code, b.total_amount
HAVING b.total_amount <> SUM(bd.subtotal);
SELECT id, total_seats, available_seats
FROM tour_departures
WHERE available_seats < 0 OR available_seats > total_seats;
SELECT d.id, d.total_seats - d.available_seats AS da_ban_theo_cot,
       IFNULL(x.so_khach, 0) AS da_ban_theo_don
FROM tour_departures d
LEFT JOIN (SELECT bd.departure_id, SUM(bd.num_adults + bd.num_children) AS so_khach
           FROM booking_details bd
           JOIN bookings b ON b.id = bd.booking_id
           WHERE b.status <> 'CANCELLED'
           GROUP BY bd.departure_id) AS x ON x.departure_id = d.id
WHERE d.total_seats - d.available_seats <> IFNULL(x.so_khach, 0);
SELECT email, CONCAT(LEFT(password, 7), '...') AS dau_chuoi_bam, CHAR_LENGTH(password) AS do_dai
FROM users;

SELECT email FROM users WHERE password NOT LIKE '$2a$%' OR CHAR_LENGTH(password) <> 60;
