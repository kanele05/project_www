/* =============================================================================
   01_create_database.sql
   Đề tài : Website giới thiệu tour du lịch và đăng ký tour trực tuyến
   Môn    : Lập trình WWW (Java) - Khoa CNTT, ĐH Công nghiệp TP.HCM
   Mục đích: Tạo database + tài khoản đăng nhập riêng cho ứng dụng.

   Cách chạy (từ Git Bash, dùng Windows Authentication):
     SQLCMD="/c/Program Files/Microsoft SQL Server/Client SDK/ODBC/180/Tools/Binn/SQLCMD.EXE"
     "$SQLCMD" -S localhost -E -C -i database/01_create_database.sql

   Ghi chú về collation Vietnamese_CI_AI:
     CI = Case Insensitive   (không phân biệt hoa/thường)
     AI = Accent Insensitive (không phân biệt dấu)
     => Tìm kiếm "da nang" vẫn khớp "Đà Nẵng". Đây là lựa chọn có chủ đích
        để chức năng tìm kiếm thân thiện với người dùng Việt Nam.
   ============================================================================= */

USE master;
GO

/* ---------- 1. Database ---------------------------------------------------- */
IF DB_ID(N'TourBookingDB') IS NULL
BEGIN
    CREATE DATABASE TourBookingDB COLLATE Vietnamese_CI_AI;
    PRINT N'[OK] Da tao database TourBookingDB';
END
ELSE
    PRINT N'[SKIP] Database TourBookingDB da ton tai';
GO

/* ---------- 2. Login cấp server -------------------------------------------- */
/* Không dùng tài khoản 'sa' cho ứng dụng: nguyên tắc least-privilege.
   Cũng không dùng integratedSecurity=true vì driver JDBC sẽ cần file
   mssql-jdbc_auth-*.dll nằm trên java.library.path -> phiền khi mang đi máy khác. */
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'tourapp')
BEGIN
    CREATE LOGIN tourapp
        WITH PASSWORD      = 'Tour@2026#IUH',
             CHECK_POLICY  = ON,
             DEFAULT_DATABASE = TourBookingDB;
    PRINT N'[OK] Da tao login tourapp';
END
ELSE
    PRINT N'[SKIP] Login tourapp da ton tai';
GO

/* ---------- 3. User trong database + phân quyền ---------------------------- */
USE TourBookingDB;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'tourapp')
BEGIN
    CREATE USER tourapp FOR LOGIN tourapp;
    PRINT N'[OK] Da tao user tourapp trong TourBookingDB';
END
ELSE
    PRINT N'[SKIP] User tourapp da ton tai';
GO

/* db_owner cần trong lúc phát triển vì Hibernate ddl-auto=update phải
   CREATE/ALTER TABLE. Khi nộp bài (ddl-auto=validate) có thể hạ xuống:
     ALTER ROLE db_datareader ADD MEMBER tourapp;
     ALTER ROLE db_datawriter ADD MEMBER tourapp;                              */
ALTER ROLE db_owner ADD MEMBER tourapp;
GO

/* ---------- 4. Kiểm tra ---------------------------------------------------- */
SELECT  DB_NAME()                                   AS [Database],
        DATABASEPROPERTYEX(DB_NAME(), 'Collation')  AS [Collation],
        USER_NAME()                                 AS [CurrentUser];
GO
