-- Cấu trúc 16 bảng của TourBookingDB. Không CHECK/Function/SP/Trigger - mọi ràng buộc nghiệp vụ nằm ở tầng Java.
USE TourBookingDB;
GO

CREATE TABLE users (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    full_name   NVARCHAR(100)  NOT NULL,
    email       NVARCHAR(150)  NOT NULL,
    password    NVARCHAR(100)  NOT NULL,
    phone       NVARCHAR(20)       NULL,
    address     NVARCHAR(255)      NULL,
    role        NVARCHAR(20)   NOT NULL,
    enabled     BIT            NOT NULL,
    created_at  DATETIME2(6)       NULL,
    updated_at  DATETIME2(6)       NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE INDEX idx_users_role ON users (role);
GO

CREATE TABLE tour_categories (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(100)  NOT NULL,
    slug        NVARCHAR(120)  NOT NULL,
    description NVARCHAR(500)      NULL,
    image_url   NVARCHAR(255)      NULL,
    active      BIT            NOT NULL,
    created_at  DATETIME2(6)       NULL,
    updated_at  DATETIME2(6)       NULL,
    CONSTRAINT pk_tour_categories PRIMARY KEY (id),
    CONSTRAINT uk_tour_categories_name UNIQUE (name),
    CONSTRAINT uk_tour_categories_slug UNIQUE (slug)
);
GO

CREATE TABLE tours (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    code               NVARCHAR(30)   NOT NULL,
    name               NVARCHAR(200)  NOT NULL,
    slug               NVARCHAR(250)  NOT NULL,
    short_description  NVARCHAR(500)      NULL,
    description        NVARCHAR(MAX)      NULL,
    itinerary          NVARCHAR(MAX)      NULL,
    departure_location NVARCHAR(100)  NOT NULL,
    destination        NVARCHAR(100)  NOT NULL,
    duration_days      INT            NOT NULL,
    duration_nights    INT            NOT NULL,
    base_price         NUMERIC(15,2)  NOT NULL,
    thumbnail          NVARCHAR(255)      NULL,
    transportation     NVARCHAR(100)      NULL,
    featured           BIT            NOT NULL,
    active             BIT            NOT NULL,
    view_count         BIGINT         NOT NULL,

    search_text        NVARCHAR(500)      NULL,
    category_id        BIGINT         NOT NULL,
    created_at         DATETIME2(6)       NULL,
    updated_at         DATETIME2(6)       NULL,
    CONSTRAINT pk_tours PRIMARY KEY (id),
    CONSTRAINT uk_tours_code UNIQUE (code),
    CONSTRAINT uk_tours_slug UNIQUE (slug),
    CONSTRAINT fk_tours_category FOREIGN KEY (category_id)
        REFERENCES tour_categories (id)
);
CREATE INDEX idx_tours_category    ON tours (category_id);
CREATE INDEX idx_tours_featured    ON tours (featured);
CREATE INDEX idx_tours_destination ON tours (destination);
GO

CREATE TABLE tour_images (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    tour_id    BIGINT        NOT NULL,
    image_path NVARCHAR(255) NOT NULL,
    caption    NVARCHAR(255)     NULL,
    sort_order INT           NOT NULL,
    CONSTRAINT pk_tour_images PRIMARY KEY (id),
    CONSTRAINT fk_tour_images_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
);
CREATE INDEX idx_tour_images_tour ON tour_images (tour_id);
GO

CREATE TABLE tour_departures (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    tour_id         BIGINT        NOT NULL,
    departure_date  DATE          NOT NULL,
    return_date     DATE          NOT NULL,
    total_seats     INT           NOT NULL,
    available_seats INT           NOT NULL,
    price_adult     NUMERIC(15,2) NOT NULL,
    price_child     NUMERIC(15,2) NOT NULL,
    active          BIT           NOT NULL,

    version         BIGINT        NOT NULL,
    created_at      DATETIME2(6)      NULL,
    updated_at      DATETIME2(6)      NULL,
    CONSTRAINT pk_tour_departures PRIMARY KEY (id),

    CONSTRAINT uk_tour_departures_tour_date UNIQUE (tour_id, departure_date),
    CONSTRAINT fk_tour_departures_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
);
CREATE INDEX idx_tour_departures_date ON tour_departures (departure_date);
GO

CREATE TABLE bookings (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    code             NVARCHAR(20)   NOT NULL,
    user_id          BIGINT         NOT NULL,
    booking_date     DATETIME2(6)   NOT NULL,
    customer_name    NVARCHAR(100)  NOT NULL,
    customer_email   NVARCHAR(150)  NOT NULL,
    customer_phone   NVARCHAR(20)   NOT NULL,
    customer_address NVARCHAR(255)      NULL,

    total_amount     NUMERIC(15,2)  NOT NULL,

    promotion_id     BIGINT             NULL,
    discount_amount  NUMERIC(15,2)  NOT NULL,
    status           NVARCHAR(20)   NOT NULL,
    note             NVARCHAR(500)      NULL,
    payment_method   NVARCHAR(30)       NULL,
    created_at       DATETIME2(6)       NULL,
    updated_at       DATETIME2(6)       NULL,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT uk_bookings_code UNIQUE (code),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id)
        REFERENCES users (id)
);
CREATE INDEX idx_bookings_user   ON bookings (user_id);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_bookings_date   ON bookings (booking_date);
GO

CREATE TABLE booking_details (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    booking_id         BIGINT        NOT NULL,
    departure_id       BIGINT        NOT NULL,
    tour_name_snapshot NVARCHAR(200) NOT NULL,
    num_adults         INT           NOT NULL,
    num_children       INT           NOT NULL,
    unit_price_adult   NUMERIC(15,2) NOT NULL,
    unit_price_child   NUMERIC(15,2) NOT NULL,
    subtotal           NUMERIC(15,2) NOT NULL,
    CONSTRAINT pk_booking_details PRIMARY KEY (id),
    CONSTRAINT fk_booking_details_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id),
    CONSTRAINT fk_booking_details_departure FOREIGN KEY (departure_id)
        REFERENCES tour_departures (id)
);
CREATE INDEX idx_booking_details_booking   ON booking_details (booking_id);
CREATE INDEX idx_booking_details_departure ON booking_details (departure_id);
GO

CREATE TABLE tour_itineraries (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    tour_id       BIGINT         NOT NULL,
    day_no        INT            NOT NULL,
    title         NVARCHAR(200)  NOT NULL,
    description   NVARCHAR(2000)     NULL,
    meals         NVARCHAR(100)      NULL,
    accommodation NVARCHAR(150)      NULL,
    CONSTRAINT pk_tour_itineraries PRIMARY KEY (id),

    CONSTRAINT uk_tour_itineraries_tour_day UNIQUE (tour_id, day_no),
    CONSTRAINT fk_tour_itineraries_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
);
CREATE INDEX idx_tour_itineraries_tour ON tour_itineraries (tour_id);
GO

CREATE TABLE booking_passengers (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    detail_id      BIGINT         NOT NULL,
    full_name      NVARCHAR(100)  NOT NULL,
    passenger_type NVARCHAR(10)   NOT NULL,
    gender         NVARCHAR(10)       NULL,
    birth_date     DATE               NULL,
    id_number      NVARCHAR(30)       NULL,
    phone          NVARCHAR(20)       NULL,
    single_room    BIT            NOT NULL,
    note           NVARCHAR(255)      NULL,
    CONSTRAINT pk_booking_passengers PRIMARY KEY (id),
    CONSTRAINT fk_booking_passengers_detail FOREIGN KEY (detail_id)
        REFERENCES booking_details (id)
);
CREATE INDEX idx_booking_passengers_detail ON booking_passengers (detail_id);
GO

CREATE TABLE payments (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT         NOT NULL,
    amount     NUMERIC(15,2)  NOT NULL,
    method     NVARCHAR(30)   NOT NULL,
    status     NVARCHAR(20)   NOT NULL,
    txn_ref    NVARCHAR(50)       NULL,
    paid_at    DATETIME2(6)       NULL,
    note       NVARCHAR(255)      NULL,
    created_at DATETIME2(6)       NULL,
    updated_at DATETIME2(6)       NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id)
);
CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_status  ON payments (status);

CREATE INDEX idx_payments_txn_ref ON payments (txn_ref);
GO

CREATE TABLE booking_status_history (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    booking_id    BIGINT        NOT NULL,
    from_status   NVARCHAR(20)      NULL,
    to_status     NVARCHAR(20)  NOT NULL,
    changed_by_id BIGINT            NULL,
    reason        NVARCHAR(255)     NULL,
    changed_at    DATETIME2(6)  NOT NULL,
    CONSTRAINT pk_booking_status_history PRIMARY KEY (id),
    CONSTRAINT fk_booking_status_history_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id),
    CONSTRAINT fk_booking_status_history_user FOREIGN KEY (changed_by_id)
        REFERENCES users (id)
);
CREATE INDEX idx_booking_status_history_booking ON booking_status_history (booking_id);
CREATE INDEX idx_booking_status_history_time    ON booking_status_history (changed_at);
GO

CREATE TABLE promotions (
    id                   BIGINT IDENTITY(1,1) NOT NULL,
    code                 NVARCHAR(30)   NOT NULL,
    name                 NVARCHAR(150)  NOT NULL,
    description          NVARCHAR(500)      NULL,
    discount_type        NVARCHAR(20)   NOT NULL,
    discount_value       NUMERIC(15,2)  NOT NULL,
    max_discount         NUMERIC(15,2)      NULL,
    min_order_amount     NUMERIC(15,2)  NOT NULL,
    usage_limit          INT                NULL,
    usage_limit_per_user INT                NULL,
    used_count           INT            NOT NULL,
    start_at             DATETIME2(6)   NOT NULL,
    end_at               DATETIME2(6)   NOT NULL,
    active               BIT            NOT NULL,
    created_at           DATETIME2(6)       NULL,
    updated_at           DATETIME2(6)       NULL,
    CONSTRAINT pk_promotions PRIMARY KEY (id),
    CONSTRAINT uk_promotions_code UNIQUE (code)
);
CREATE INDEX idx_promotions_active ON promotions (active);
CREATE INDEX idx_promotions_period ON promotions (start_at, end_at);
GO

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_promotion FOREIGN KEY (promotion_id)
        REFERENCES promotions (id);
CREATE INDEX idx_bookings_promotion ON bookings (promotion_id);
GO

CREATE TABLE coupon_usages (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    promotion_id    BIGINT        NOT NULL,
    user_id         BIGINT        NOT NULL,
    booking_id      BIGINT        NOT NULL,
    discount_amount NUMERIC(15,2) NOT NULL,
    used_at         DATETIME2(6)  NOT NULL,
    CONSTRAINT pk_coupon_usages PRIMARY KEY (id),

    CONSTRAINT uk_coupon_usages_booking UNIQUE (booking_id),
    CONSTRAINT fk_coupon_usages_promotion FOREIGN KEY (promotion_id)
        REFERENCES promotions (id),
    CONSTRAINT fk_coupon_usages_user FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_coupon_usages_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id)
);
CREATE INDEX idx_coupon_usages_promotion ON coupon_usages (promotion_id);
CREATE INDEX idx_coupon_usages_user      ON coupon_usages (user_id);
GO

CREATE TABLE reviews (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    tour_id     BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    booking_id  BIGINT              NULL,
    rating      INT             NOT NULL,
    title       NVARCHAR(200)       NULL,
    content     NVARCHAR(1000)  NOT NULL,
    approved    BIT             NOT NULL,
    admin_reply NVARCHAR(1000)      NULL,
    replied_at  DATETIME2(6)        NULL,
    created_at  DATETIME2(6)        NULL,
    updated_at  DATETIME2(6)        NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uk_reviews_user_tour UNIQUE (user_id, tour_id),
    CONSTRAINT fk_reviews_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id)
);
CREATE INDEX idx_reviews_tour     ON reviews (tour_id);
CREATE INDEX idx_reviews_approved ON reviews (approved);
GO

CREATE TABLE contact_messages (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    full_name     NVARCHAR(100)  NOT NULL,
    email         NVARCHAR(150)  NOT NULL,
    phone         NVARCHAR(20)       NULL,
    subject       NVARCHAR(200)  NOT NULL,
    content       NVARCHAR(2000) NOT NULL,
    status        NVARCHAR(20)   NOT NULL,
    tour_id       BIGINT             NULL,
    handled_by_id BIGINT             NULL,
    reply_note    NVARCHAR(1000)     NULL,
    handled_at    DATETIME2(6)       NULL,
    created_at    DATETIME2(6)       NULL,
    updated_at    DATETIME2(6)       NULL,
    CONSTRAINT pk_contact_messages PRIMARY KEY (id),
    CONSTRAINT fk_contact_messages_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id),
    CONSTRAINT fk_contact_messages_user FOREIGN KEY (handled_by_id)
        REFERENCES users (id)
);
CREATE INDEX idx_contact_messages_status  ON contact_messages (status);
CREATE INDEX idx_contact_messages_created ON contact_messages (created_at);
GO

CREATE TABLE password_reset_tokens (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    user_id    BIGINT        NOT NULL,
    token      NVARCHAR(100) NOT NULL,
    expires_at DATETIME2(6)  NOT NULL,
    used_at    DATETIME2(6)      NULL,
    created_at DATETIME2(6)      NULL,
    updated_at DATETIME2(6)      NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id)
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
GO

SELECT COUNT(*) AS so_check_constraint FROM sys.check_constraints;
SELECT COUNT(*) AS so_function         FROM sys.objects WHERE type IN ('FN','IF','TF');
SELECT COUNT(*) AS so_stored_procedure FROM sys.procedures;
SELECT COUNT(*) AS so_trigger          FROM sys.triggers;
SELECT COUNT(*) AS so_bang             FROM sys.tables;
GO
