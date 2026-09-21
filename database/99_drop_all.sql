/* ===========================================================================
   99_drop_all.sql - Xoá sạch toàn bộ bảng của TourBookingDB

   Dùng khi cần dựng lại CSDL từ con số không (ví dụ sau khi đổi cách ánh xạ
   entity). Chỉ chạy trên máy phát triển - script này XOÁ HẾT DỮ LIỆU.

   Thứ tự xoá đi ngược chiều khoá ngoại: bảng con trước, bảng cha sau.

   Cách chạy:
     sqlcmd -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' \
            -d TourBookingDB -C -i database/99_drop_all.sql
   =========================================================================== */

USE TourBookingDB;
GO

/* Nhóm bảng bổ sung - xoá trước vì đều trỏ ngược về bảy bảng gốc */
DROP TABLE IF EXISTS booking_passengers;
DROP TABLE IF EXISTS coupon_usages;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS booking_status_history;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS contact_messages;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS tour_itineraries;

/* Bảy bảng gốc. bookings trỏ sang promotions (cột promotion_id) nên phải bỏ
   bookings trước, rồi mới tới promotions. */
DROP TABLE IF EXISTS booking_details;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS promotions;
DROP TABLE IF EXISTS tour_departures;
DROP TABLE IF EXISTS tour_images;
DROP TABLE IF EXISTS tours;
DROP TABLE IF EXISTS tour_categories;
DROP TABLE IF EXISTS users;
GO

/* Kiểm tra lại: cả ba câu đều phải trả về 0 */
SELECT COUNT(*) AS remaining_tables      FROM sys.tables;
SELECT COUNT(*) AS remaining_checks      FROM sys.check_constraints;
SELECT COUNT(*) AS remaining_procedures  FROM sys.procedures;
GO
