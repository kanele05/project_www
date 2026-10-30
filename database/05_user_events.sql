-- Thêm bảng user_events (ghi hành vi người dùng) vào CSDL ĐÃ CÓ SẴN. Chạy lại nhiều lần vẫn an toàn.
-- Bảng không có khoá ngoại: ghi sự kiện phải nhanh và không được cản việc xoá tour/tài khoản.
-- Dựng CSDL từ đầu thì không cần file này, vì 02_schema.sql đã có bảng user_events.
USE TourBookingDB;
GO

IF OBJECT_ID(N'dbo.user_events', N'U') IS NULL
BEGIN
    CREATE TABLE user_events (
        id           BIGINT IDENTITY(1,1) NOT NULL,
        event_type   NVARCHAR(20)   NOT NULL,
        session_id   NVARCHAR(64)   NOT NULL,
        user_id      BIGINT             NULL,
        tour_id      BIGINT             NULL,
        departure_id BIGINT             NULL,
        keyword      NVARCHAR(200)      NULL,
        duration_ms  INT                NULL,
        page_url     NVARCHAR(255)      NULL,
        occurred_at  DATETIME2(6)   NOT NULL,
        CONSTRAINT pk_user_events PRIMARY KEY (id)
    );
    CREATE INDEX idx_user_events_tour    ON user_events (tour_id, event_type);
    CREATE INDEX idx_user_events_time    ON user_events (occurred_at);
    CREATE INDEX idx_user_events_session ON user_events (session_id);
    CREATE INDEX idx_user_events_user    ON user_events (user_id);
    PRINT N'Đã tạo bảng user_events.';
END
ELSE
    PRINT N'Bảng user_events đã tồn tại, bỏ qua.';
GO

SELECT COUNT(*) AS so_dong_user_events FROM user_events;
SELECT COUNT(*) AS so_check_constraint FROM sys.check_constraints;
GO
