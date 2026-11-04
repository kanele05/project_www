-- Thêm bảng user_events (ghi hành vi người dùng) vào CSDL ĐÃ CÓ SẴN. Chạy lại nhiều lần vẫn an toàn.
-- Bảng không có khoá ngoại: ghi sự kiện phải nhanh và không được cản việc xoá tour/tài khoản.
-- Dựng CSDL từ đầu thì không cần file này, vì 02_schema.sql đã có bảng user_events.
USE TourBookingDB;

CREATE TABLE IF NOT EXISTS user_events (
    id           BIGINT AUTO_INCREMENT NOT NULL,
    event_type   VARCHAR(20)    NOT NULL,
    session_id   VARCHAR(64)    NOT NULL,
    user_id      BIGINT             NULL,
    tour_id      BIGINT             NULL,
    departure_id BIGINT             NULL,
    keyword      VARCHAR(200)       NULL,
    duration_ms  INT                NULL,
    page_url     VARCHAR(255)       NULL,
    occurred_at  DATETIME(6)    NOT NULL,
    CONSTRAINT pk_user_events PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_user_events_tour    ON user_events (tour_id, event_type);
CREATE INDEX IF NOT EXISTS idx_user_events_time    ON user_events (occurred_at);
CREATE INDEX IF NOT EXISTS idx_user_events_session ON user_events (session_id);
CREATE INDEX IF NOT EXISTS idx_user_events_user    ON user_events (user_id);

SELECT COUNT(*) AS so_dong_user_events FROM user_events;
SELECT COUNT(*) AS so_check_constraint FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE();
