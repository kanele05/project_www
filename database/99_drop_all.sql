-- Xoá sạch toàn bộ bảng của TourBookingDB để dựng lại từ đầu (chỉ chạy trên máy phát triển).
USE TourBookingDB;
GO

DROP TABLE IF EXISTS booking_passengers;
DROP TABLE IF EXISTS coupon_usages;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS booking_status_history;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS contact_messages;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS tour_itineraries;

DROP TABLE IF EXISTS booking_details;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS promotions;
DROP TABLE IF EXISTS tour_departures;
DROP TABLE IF EXISTS tour_images;
DROP TABLE IF EXISTS tours;
DROP TABLE IF EXISTS tour_categories;
DROP TABLE IF EXISTS users;
GO

SELECT COUNT(*) AS remaining_tables      FROM sys.tables;
SELECT COUNT(*) AS remaining_checks      FROM sys.check_constraints;
SELECT COUNT(*) AS remaining_procedures  FROM sys.procedures;
GO
