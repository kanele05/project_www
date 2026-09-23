-- Tạo database TourBookingDB (collation Vietnamese_CI_AI) và tài khoản đăng nhập tourapp.
USE master;
GO

IF DB_ID(N'TourBookingDB') IS NULL
BEGIN
    CREATE DATABASE TourBookingDB COLLATE Vietnamese_CI_AI;
    PRINT N'[OK] Da tao database TourBookingDB';
END
ELSE
    PRINT N'[SKIP] Database TourBookingDB da ton tai';
GO

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

ALTER ROLE db_owner ADD MEMBER tourapp;
GO

SELECT  DB_NAME()                                   AS [Database],
        DATABASEPROPERTYEX(DB_NAME(), 'Collation')  AS [Collation],
        USER_NAME()                                 AS [CurrentUser];
GO
