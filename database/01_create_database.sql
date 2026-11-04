-- Tạo database TourBookingDB (utf8mb4, không phân biệt dấu và hoa thường) và tài khoản tourapp. Chạy bằng root.
CREATE DATABASE IF NOT EXISTS TourBookingDB
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_uca1400_ai_ci;

CREATE USER IF NOT EXISTS 'tourapp'@'localhost' IDENTIFIED BY 'Tour@2026#IUH';
GRANT ALL PRIVILEGES ON TourBookingDB.* TO 'tourapp'@'localhost';
FLUSH PRIVILEGES;

USE TourBookingDB;

SELECT DATABASE()                AS `Database`,
       @@collation_database      AS `Collation`,
       CURRENT_USER()            AS `CurrentUser`;
