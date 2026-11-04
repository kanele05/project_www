-- Cấu trúc 17 bảng của TourBookingDB (MariaDB). Không CHECK/Function/SP/Trigger - mọi ràng buộc nghiệp vụ nằm ở tầng Java.
USE TourBookingDB;

CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT NOT NULL,
    full_name   VARCHAR(100)   NOT NULL,
    email       VARCHAR(150)   NOT NULL,
    password    VARCHAR(100)   NOT NULL,
    phone       VARCHAR(20)        NULL,
    address     VARCHAR(255)       NULL,
    role        VARCHAR(20)    NOT NULL,
    enabled     BIT(1)            NOT NULL,
    created_at  DATETIME(6)        NULL,
    updated_at  DATETIME(6)        NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;
CREATE INDEX idx_users_role ON users (role);

CREATE TABLE tour_categories (
    id          BIGINT AUTO_INCREMENT NOT NULL,
    name        VARCHAR(100)   NOT NULL,
    slug        VARCHAR(120)   NOT NULL,
    description VARCHAR(500)       NULL,
    image_url   VARCHAR(255)       NULL,
    active      BIT(1)            NOT NULL,
    created_at  DATETIME(6)        NULL,
    updated_at  DATETIME(6)        NULL,
    CONSTRAINT pk_tour_categories PRIMARY KEY (id),
    CONSTRAINT uk_tour_categories_name UNIQUE (name),
    CONSTRAINT uk_tour_categories_slug UNIQUE (slug)
) ENGINE=InnoDB;

CREATE TABLE tours (
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    code               VARCHAR(30)    NOT NULL,
    name               VARCHAR(200)   NOT NULL,
    slug               VARCHAR(250)   NOT NULL,
    short_description  VARCHAR(500)       NULL,
    description        LONGTEXT          NULL,
    itinerary          LONGTEXT          NULL,
    departure_location VARCHAR(100)   NOT NULL,
    destination        VARCHAR(100)   NOT NULL,
    duration_days      INT            NOT NULL,
    duration_nights    INT            NOT NULL,
    base_price         DECIMAL(15,2)  NOT NULL,
    thumbnail          VARCHAR(255)       NULL,
    transportation     VARCHAR(100)       NULL,
    featured           BIT(1)            NOT NULL,
    active             BIT(1)            NOT NULL,
    view_count         BIGINT         NOT NULL,

    search_text        VARCHAR(500)       NULL,
    category_id        BIGINT         NOT NULL,
    created_at         DATETIME(6)        NULL,
    updated_at         DATETIME(6)        NULL,
    CONSTRAINT pk_tours PRIMARY KEY (id),
    CONSTRAINT uk_tours_code UNIQUE (code),
    CONSTRAINT uk_tours_slug UNIQUE (slug),
    CONSTRAINT fk_tours_category FOREIGN KEY (category_id)
        REFERENCES tour_categories (id)
) ENGINE=InnoDB;
CREATE INDEX idx_tours_category    ON tours (category_id);
CREATE INDEX idx_tours_featured    ON tours (featured);
CREATE INDEX idx_tours_destination ON tours (destination);

CREATE TABLE tour_images (
    id         BIGINT AUTO_INCREMENT NOT NULL,
    tour_id    BIGINT        NOT NULL,
    image_path VARCHAR(255)  NOT NULL,
    caption    VARCHAR(255)      NULL,
    sort_order INT           NOT NULL,
    CONSTRAINT pk_tour_images PRIMARY KEY (id),
    CONSTRAINT fk_tour_images_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
) ENGINE=InnoDB;
CREATE INDEX idx_tour_images_tour ON tour_images (tour_id);

CREATE TABLE tour_departures (
    id              BIGINT AUTO_INCREMENT NOT NULL,
    tour_id         BIGINT        NOT NULL,
    departure_date  DATE          NOT NULL,
    return_date     DATE          NOT NULL,
    total_seats     INT           NOT NULL,
    available_seats INT           NOT NULL,
    price_adult     DECIMAL(15,2) NOT NULL,
    price_child     DECIMAL(15,2) NOT NULL,
    active          BIT(1)           NOT NULL,

    version         BIGINT        NOT NULL,
    created_at      DATETIME(6)       NULL,
    updated_at      DATETIME(6)       NULL,
    CONSTRAINT pk_tour_departures PRIMARY KEY (id),

    CONSTRAINT uk_tour_departures_tour_date UNIQUE (tour_id, departure_date),
    CONSTRAINT fk_tour_departures_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
) ENGINE=InnoDB;
CREATE INDEX idx_tour_departures_date ON tour_departures (departure_date);

CREATE TABLE bookings (
    id               BIGINT AUTO_INCREMENT NOT NULL,
    code             VARCHAR(20)    NOT NULL,
    user_id          BIGINT         NOT NULL,
    booking_date     DATETIME(6)    NOT NULL,
    customer_name    VARCHAR(100)   NOT NULL,
    customer_email   VARCHAR(150)   NOT NULL,
    customer_phone   VARCHAR(20)    NOT NULL,
    customer_address VARCHAR(255)       NULL,

    total_amount     DECIMAL(15,2)  NOT NULL,

    promotion_id     BIGINT             NULL,
    discount_amount  DECIMAL(15,2)  NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    note             VARCHAR(500)       NULL,
    payment_method   VARCHAR(30)        NULL,
    created_at       DATETIME(6)        NULL,
    updated_at       DATETIME(6)        NULL,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT uk_bookings_code UNIQUE (code),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id)
        REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_bookings_user   ON bookings (user_id);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_bookings_date   ON bookings (booking_date);

CREATE TABLE booking_details (
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    booking_id         BIGINT        NOT NULL,
    departure_id       BIGINT        NOT NULL,
    tour_name_snapshot VARCHAR(200)  NOT NULL,
    num_adults         INT           NOT NULL,
    num_children       INT           NOT NULL,
    unit_price_adult   DECIMAL(15,2) NOT NULL,
    unit_price_child   DECIMAL(15,2) NOT NULL,
    subtotal           DECIMAL(15,2) NOT NULL,
    CONSTRAINT pk_booking_details PRIMARY KEY (id),
    CONSTRAINT fk_booking_details_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id),
    CONSTRAINT fk_booking_details_departure FOREIGN KEY (departure_id)
        REFERENCES tour_departures (id)
) ENGINE=InnoDB;
CREATE INDEX idx_booking_details_booking   ON booking_details (booking_id);
CREATE INDEX idx_booking_details_departure ON booking_details (departure_id);

CREATE TABLE tour_itineraries (
    id            BIGINT AUTO_INCREMENT NOT NULL,
    tour_id       BIGINT         NOT NULL,
    day_no        INT            NOT NULL,
    title         VARCHAR(200)   NOT NULL,
    description   VARCHAR(2000)      NULL,
    meals         VARCHAR(100)       NULL,
    accommodation VARCHAR(150)       NULL,
    CONSTRAINT pk_tour_itineraries PRIMARY KEY (id),

    CONSTRAINT uk_tour_itineraries_tour_day UNIQUE (tour_id, day_no),
    CONSTRAINT fk_tour_itineraries_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
) ENGINE=InnoDB;
CREATE INDEX idx_tour_itineraries_tour ON tour_itineraries (tour_id);

CREATE TABLE booking_passengers (
    id             BIGINT AUTO_INCREMENT NOT NULL,
    detail_id      BIGINT         NOT NULL,
    full_name      VARCHAR(100)   NOT NULL,
    passenger_type VARCHAR(10)    NOT NULL,
    gender         VARCHAR(10)        NULL,
    birth_date     DATE               NULL,
    id_number      VARCHAR(30)        NULL,
    phone          VARCHAR(20)        NULL,
    single_room    BIT(1)            NOT NULL,
    note           VARCHAR(255)       NULL,
    CONSTRAINT pk_booking_passengers PRIMARY KEY (id),
    CONSTRAINT fk_booking_passengers_detail FOREIGN KEY (detail_id)
        REFERENCES booking_details (id)
) ENGINE=InnoDB;
CREATE INDEX idx_booking_passengers_detail ON booking_passengers (detail_id);

CREATE TABLE payments (
    id         BIGINT AUTO_INCREMENT NOT NULL,
    booking_id BIGINT         NOT NULL,
    amount     DECIMAL(15,2)  NOT NULL,
    method     VARCHAR(30)    NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    txn_ref    VARCHAR(50)        NULL,
    paid_at    DATETIME(6)        NULL,
    note       VARCHAR(255)       NULL,
    created_at DATETIME(6)        NULL,
    updated_at DATETIME(6)        NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id)
) ENGINE=InnoDB;
CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_status  ON payments (status);

CREATE INDEX idx_payments_txn_ref ON payments (txn_ref);

CREATE TABLE booking_status_history (
    id            BIGINT AUTO_INCREMENT NOT NULL,
    booking_id    BIGINT        NOT NULL,
    from_status   VARCHAR(20)       NULL,
    to_status     VARCHAR(20)   NOT NULL,
    changed_by_id BIGINT            NULL,
    reason        VARCHAR(255)      NULL,
    changed_at    DATETIME(6)   NOT NULL,
    CONSTRAINT pk_booking_status_history PRIMARY KEY (id),
    CONSTRAINT fk_booking_status_history_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id),
    CONSTRAINT fk_booking_status_history_user FOREIGN KEY (changed_by_id)
        REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_booking_status_history_booking ON booking_status_history (booking_id);
CREATE INDEX idx_booking_status_history_time    ON booking_status_history (changed_at);

CREATE TABLE promotions (
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    code                 VARCHAR(30)    NOT NULL,
    name                 VARCHAR(150)   NOT NULL,
    description          VARCHAR(500)       NULL,
    discount_type        VARCHAR(20)    NOT NULL,
    discount_value       DECIMAL(15,2)  NOT NULL,
    max_discount         DECIMAL(15,2)      NULL,
    min_order_amount     DECIMAL(15,2)  NOT NULL,
    usage_limit          INT                NULL,
    usage_limit_per_user INT                NULL,
    used_count           INT            NOT NULL,
    start_at             DATETIME(6)    NOT NULL,
    end_at               DATETIME(6)    NOT NULL,
    active               BIT(1)            NOT NULL,
    created_at           DATETIME(6)        NULL,
    updated_at           DATETIME(6)        NULL,
    CONSTRAINT pk_promotions PRIMARY KEY (id),
    CONSTRAINT uk_promotions_code UNIQUE (code)
) ENGINE=InnoDB;
CREATE INDEX idx_promotions_active ON promotions (active);
CREATE INDEX idx_promotions_period ON promotions (start_at, end_at);

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_promotion FOREIGN KEY (promotion_id)
        REFERENCES promotions (id);
CREATE INDEX idx_bookings_promotion ON bookings (promotion_id);

CREATE TABLE coupon_usages (
    id              BIGINT AUTO_INCREMENT NOT NULL,
    promotion_id    BIGINT        NOT NULL,
    user_id         BIGINT        NOT NULL,
    booking_id      BIGINT        NOT NULL,
    discount_amount DECIMAL(15,2) NOT NULL,
    used_at         DATETIME(6)   NOT NULL,
    CONSTRAINT pk_coupon_usages PRIMARY KEY (id),

    CONSTRAINT uk_coupon_usages_booking UNIQUE (booking_id),
    CONSTRAINT fk_coupon_usages_promotion FOREIGN KEY (promotion_id)
        REFERENCES promotions (id),
    CONSTRAINT fk_coupon_usages_user FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_coupon_usages_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id)
) ENGINE=InnoDB;
CREATE INDEX idx_coupon_usages_promotion ON coupon_usages (promotion_id);
CREATE INDEX idx_coupon_usages_user      ON coupon_usages (user_id);

CREATE TABLE reviews (
    id          BIGINT AUTO_INCREMENT NOT NULL,
    tour_id     BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    booking_id  BIGINT              NULL,
    rating      INT             NOT NULL,
    title       VARCHAR(200)        NULL,
    content     VARCHAR(1000)   NOT NULL,
    approved    BIT(1)             NOT NULL,
    admin_reply VARCHAR(1000)       NULL,
    replied_at  DATETIME(6)         NULL,
    created_at  DATETIME(6)         NULL,
    updated_at  DATETIME(6)         NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uk_reviews_user_tour UNIQUE (user_id, tour_id),
    CONSTRAINT fk_reviews_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id)
) ENGINE=InnoDB;
CREATE INDEX idx_reviews_tour     ON reviews (tour_id);
CREATE INDEX idx_reviews_approved ON reviews (approved);

CREATE TABLE contact_messages (
    id            BIGINT AUTO_INCREMENT NOT NULL,
    full_name     VARCHAR(100)   NOT NULL,
    email         VARCHAR(150)   NOT NULL,
    phone         VARCHAR(20)        NULL,
    subject       VARCHAR(200)   NOT NULL,
    content       VARCHAR(2000)  NOT NULL,
    status        VARCHAR(20)    NOT NULL,
    tour_id       BIGINT             NULL,
    handled_by_id BIGINT             NULL,
    reply_note    VARCHAR(1000)      NULL,
    handled_at    DATETIME(6)        NULL,
    created_at    DATETIME(6)        NULL,
    updated_at    DATETIME(6)        NULL,
    CONSTRAINT pk_contact_messages PRIMARY KEY (id),
    CONSTRAINT fk_contact_messages_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id),
    CONSTRAINT fk_contact_messages_user FOREIGN KEY (handled_by_id)
        REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_contact_messages_status  ON contact_messages (status);
CREATE INDEX idx_contact_messages_created ON contact_messages (created_at);

CREATE TABLE password_reset_tokens (
    id         BIGINT AUTO_INCREMENT NOT NULL,
    user_id    BIGINT        NOT NULL,
    token      VARCHAR(100)  NOT NULL,
    expires_at DATETIME(6)   NOT NULL,
    used_at    DATETIME(6)       NULL,
    created_at DATETIME(6)       NULL,
    updated_at DATETIME(6)       NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id)
) ENGINE=InnoDB;
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);

CREATE TABLE user_events (
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
CREATE INDEX idx_user_events_tour    ON user_events (tour_id, event_type);
CREATE INDEX idx_user_events_time    ON user_events (occurred_at);
CREATE INDEX idx_user_events_session ON user_events (session_id);
CREATE INDEX idx_user_events_user    ON user_events (user_id);

SELECT COUNT(*) AS so_check_constraint FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE();
SELECT COUNT(*) AS so_function         FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_TYPE = 'FUNCTION';
SELECT COUNT(*) AS so_stored_procedure FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_TYPE = 'PROCEDURE';
SELECT COUNT(*) AS so_trigger          FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE();
SELECT COUNT(*) AS so_bang             FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE';
