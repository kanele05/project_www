/* ===========================================================================
   02_schema.sql - Cấu trúc bảng của TourBookingDB

   Đề tài : Website giới thiệu tour du lịch và đăng ký tour trực tuyến
   Môn    : Lập trình WWW (Java) - Khoa CNTT, ĐH Công nghiệp TP. Hồ Chí Minh

   ---------------------------------------------------------------------------
   GHI CHÚ VỀ YÊU CẦU CỦA ĐỀ BÀI
   ---------------------------------------------------------------------------
   Đề bài không cho phép dùng Function, Stored Procedure, Trigger và ràng buộc
   CHECK trong hệ quản trị CSDL. Toàn bộ quy tắc nghiệp vụ vì vậy nằm ở tầng
   Model của ứng dụng (Bean Validation + lớp service), cụ thể:

     - Giá trị hợp lệ của cột "role" và "status" do các lớp RoleConverter /
       BookingStatusConverter kiểm tra khi đọc - ghi, thay cho ràng buộc CHECK.
       (Hibernate sẽ tự sinh CHECK nếu ánh xạ hai cột đó bằng @Enumerated, nên
        dự án dùng AttributeConverter. Hai cột vẫn là NVARCHAR như mọi cột chữ
        khác: cấu hình use_nationalized_character_data khiến Hibernate đọc cột
        chuỗi bằng getNString(), mà trình điều khiển SQL Server không đọc được
        cột VARCHAR theo cách đó.)
     - Quy tắc "không xoá dữ liệu đang được tham chiếu" được kiểm tra bằng Java
       (các truy vấn countBy... trong tầng repository) rồi mới gọi lệnh xoá.
       Khoá ngoại bên dưới là lớp bảo vệ cuối cùng, không phải nơi báo lỗi cho
       người dùng.

   File này khớp 1-1 với các entity JPA: ở giai đoạn nộp bài, ứng dụng chạy với
   ddl-auto = validate, nghĩa là Hibernate sẽ từ chối khởi động nếu schema thật
   lệch so với entity.

   Cách chạy:
     sqlcmd -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' \
            -d TourBookingDB -C -i database/02_schema.sql
   =========================================================================== */

USE TourBookingDB;
GO

/* ---------------------------------------------------------------------------
   1. users - tài khoản khách hàng và quản trị viên
   Tên bảng là "users" vì USER là từ khoá dành riêng của SQL Server.
   --------------------------------------------------------------------------- */
CREATE TABLE users (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    full_name   NVARCHAR(100)  NOT NULL,
    email       NVARCHAR(150)  NOT NULL,   -- đồng thời là tên đăng nhập
    password    NVARCHAR(100)  NOT NULL,   -- chuỗi băm BCrypt, 60 ký tự
    phone       NVARCHAR(20)       NULL,
    address     NVARCHAR(255)      NULL,
    role        NVARCHAR(20)   NOT NULL,   -- CUSTOMER | ADMIN (kiểm tra ở tầng Model)
    enabled     BIT            NOT NULL,   -- 0 = tài khoản bị vô hiệu hoá
    created_at  DATETIME2(6)       NULL,
    updated_at  DATETIME2(6)       NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE INDEX idx_users_role ON users (role);
GO

/* ---------------------------------------------------------------------------
   2. tour_categories - danh mục tour
   --------------------------------------------------------------------------- */
CREATE TABLE tour_categories (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(100)  NOT NULL,
    slug        NVARCHAR(120)  NOT NULL,   -- dạng dùng trong URL: du-lich-bien-dao
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

/* ---------------------------------------------------------------------------
   3. tours - chương trình tour được rao bán
   Ngày đi, giá và số chỗ cụ thể nằm ở bảng tour_departures.
   --------------------------------------------------------------------------- */
CREATE TABLE tours (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    code               NVARCHAR(30)   NOT NULL,
    name               NVARCHAR(200)  NOT NULL,
    slug               NVARCHAR(250)  NOT NULL,
    short_description  NVARCHAR(500)      NULL,
    description        NVARCHAR(MAX)      NULL,
    itinerary          NVARCHAR(MAX)      NULL,   -- lịch trình từng ngày
    departure_location NVARCHAR(100)  NOT NULL,
    destination        NVARCHAR(100)  NOT NULL,
    duration_days      INT            NOT NULL,
    duration_nights    INT            NOT NULL,
    base_price         NUMERIC(15,2)  NOT NULL,   -- giá tham khảo "từ ..."
    thumbnail          NVARCHAR(255)      NULL,   -- đường dẫn TƯƠNG ĐỐI trong thư mục uploads
    transportation     NVARCHAR(100)      NULL,
    featured           BIT            NOT NULL,
    active             BIT            NOT NULL,
    view_count         BIGINT         NOT NULL,
    /* Bản sao đã bỏ dấu của tên + điểm đến + nơi khởi hành + mã tour, do ứng
       dụng dựng bằng Java (không dùng Function của CSDL). Cần cột này vì
       collation Vietnamese_CI_AI bỏ được dấu thanh nhưng KHÔNG coi Đ là D,
       nên gõ "da lat" sẽ không tìm ra "Đà Lạt" nếu tìm thẳng trên cột name. */
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

/* ---------------------------------------------------------------------------
   4. tour_images - thư viện ảnh của tour
   Ảnh thuộc sở hữu của tour: xoá tour thì ứng dụng xoá luôn ảnh (cascade ALL
   + orphanRemoval ở tầng JPA) và xoá file vật lý sau khi giao dịch commit.
   --------------------------------------------------------------------------- */
CREATE TABLE tour_images (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    tour_id    BIGINT        NOT NULL,
    image_path NVARCHAR(255) NOT NULL,   -- đường dẫn TƯƠNG ĐỐI, ví dụ tours/<uuid>.jpg
    caption    NVARCHAR(255)     NULL,
    sort_order INT           NOT NULL,
    CONSTRAINT pk_tour_images PRIMARY KEY (id),
    CONSTRAINT fk_tour_images_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
);
CREATE INDEX idx_tour_images_tour ON tour_images (tour_id);
GO

/* ---------------------------------------------------------------------------
   5. tour_departures - đợt khởi hành cụ thể (thứ khách hàng thực sự đặt)
   --------------------------------------------------------------------------- */
CREATE TABLE tour_departures (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    tour_id         BIGINT        NOT NULL,
    departure_date  DATE          NOT NULL,
    return_date     DATE          NOT NULL,
    total_seats     INT           NOT NULL,
    available_seats INT           NOT NULL,   -- giảm khi đặt, tăng lại khi huỷ
    price_adult     NUMERIC(15,2) NOT NULL,
    price_child     NUMERIC(15,2) NOT NULL,
    active          BIT           NOT NULL,
    /* Khoá lạc quan (@Version): hai khách cùng đặt chỗ cuối cùng thì giao dịch
       thứ hai bị từ chối thay vì cả hai cùng ghi đè available_seats. */
    version         BIGINT        NOT NULL,
    created_at      DATETIME2(6)      NULL,
    updated_at      DATETIME2(6)      NULL,
    CONSTRAINT pk_tour_departures PRIMARY KEY (id),
    /* Một tour không thể có hai đợt cùng ngày khởi hành. */
    CONSTRAINT uk_tour_departures_tour_date UNIQUE (tour_id, departure_date),
    CONSTRAINT fk_tour_departures_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
);
CREATE INDEX idx_tour_departures_date ON tour_departures (departure_date);
GO

/* ---------------------------------------------------------------------------
   6. bookings - đơn đặt tour
   Bốn cột customer_* là BẢN SAO thông tin liên hệ tại thời điểm đặt, không đọc
   lại từ bảng users: đơn hàng là chứng từ, khách đổi số điện thoại sau này thì
   đơn cũ vẫn phải giữ nguyên thông tin lúc đặt.
   --------------------------------------------------------------------------- */
CREATE TABLE bookings (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    code             NVARCHAR(20)   NOT NULL,  -- mã hiển thị cho khách: TB20260807ABC123
    user_id          BIGINT         NOT NULL,
    booking_date     DATETIME2(6)   NOT NULL,
    customer_name    NVARCHAR(100)  NOT NULL,
    customer_email   NVARCHAR(150)  NOT NULL,
    customer_phone   NVARCHAR(20)   NOT NULL,
    customer_address NVARCHAR(255)      NULL,
    /* Số tiền khách phải trả, ĐÃ trừ discount_amount. */
    total_amount     NUMERIC(15,2)  NOT NULL,
    /* Mã giảm giá đã áp và số tiền được giảm. Số tiền là BẢN SAO tại thời điểm
       đặt: quản trị viên sửa giá trị mã về sau thì hoá đơn cũ vẫn giữ số cũ. */
    promotion_id     BIGINT             NULL,
    discount_amount  NUMERIC(15,2)  NOT NULL,
    status           NVARCHAR(20)   NOT NULL,  -- PENDING | CONFIRMED | COMPLETED | CANCELLED
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

/* ---------------------------------------------------------------------------
   7. booking_details - từng dòng trong đơn
   tour_name_snapshot và hai cột unit_price_* cũng là bản sao tại thời điểm đặt,
   để quản trị viên chỉnh giá đợt sau không làm đổi số tiền của hoá đơn cũ.

   Đây cũng là bảng chặn xoá: còn dòng nào trỏ tới một đợt khởi hành thì không
   được xoá đợt đó, kéo theo không xoá được tour và danh mục ở trên.
   --------------------------------------------------------------------------- */
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

/* ---------------------------------------------------------------------------
   8. tour_itineraries - lịch trình từng ngày của tour

   Trước đây cả lịch trình nằm gọn trong cột văn bản tours.itinerary. Cột đó vẫn
   giữ cho dữ liệu cũ, còn bảng này là cách biểu diễn có cấu trúc: hiện được dạng
   gấp mở từng ngày, sửa một ngày không phải chép tay lại cả đoạn.
   --------------------------------------------------------------------------- */
CREATE TABLE tour_itineraries (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    tour_id       BIGINT         NOT NULL,
    day_no        INT            NOT NULL,   -- ngày thứ mấy, bắt đầu từ 1
    title         NVARCHAR(200)  NOT NULL,
    description   NVARCHAR(2000)     NULL,
    meals         NVARCHAR(100)      NULL,   -- ví dụ: Sáng, Trưa, Tối
    accommodation NVARCHAR(150)      NULL,   -- nơi nghỉ đêm
    CONSTRAINT pk_tour_itineraries PRIMARY KEY (id),
    /* Một tour không thể có hai "ngày 2". */
    CONSTRAINT uk_tour_itineraries_tour_day UNIQUE (tour_id, day_no),
    CONSTRAINT fk_tour_itineraries_tour FOREIGN KEY (tour_id)
        REFERENCES tours (id)
);
CREATE INDEX idx_tour_itineraries_tour ON tour_itineraries (tour_id);
GO

/* ---------------------------------------------------------------------------
   9. booking_passengers - từng hành khách trong một dòng của đơn

   Đơn hàng biết "2 người lớn, 1 trẻ em" nhưng không biết AI đi; bảng này trả lời
   câu đó. Gắn vào booking_details chứ không gắn thẳng vào bookings, vì một đơn có
   thể đặt hai tour và mỗi tour có danh sách khách riêng.

   Quy tắc "đếm hành khách phải khớp num_adults / num_children" kiểm ở tầng
   service - đề bài không cho dùng CHECK hay Trigger.
   --------------------------------------------------------------------------- */
CREATE TABLE booking_passengers (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    detail_id      BIGINT         NOT NULL,
    full_name      NVARCHAR(100)  NOT NULL,
    passenger_type NVARCHAR(10)   NOT NULL,   -- ADULT | CHILD (kiểm tra ở tầng Model)
    gender         NVARCHAR(10)       NULL,   -- MALE | FEMALE | OTHER
    birth_date     DATE               NULL,
    id_number      NVARCHAR(30)       NULL,   -- CCCD hoặc số hộ chiếu
    phone          NVARCHAR(20)       NULL,
    single_room    BIT            NOT NULL,   -- 1 = yêu cầu phòng đơn, có phụ thu
    note           NVARCHAR(255)      NULL,   -- ăn chay, dị ứng, cần xe lăn...
    CONSTRAINT pk_booking_passengers PRIMARY KEY (id),
    CONSTRAINT fk_booking_passengers_detail FOREIGN KEY (detail_id)
        REFERENCES booking_details (id)
);
CREATE INDEX idx_booking_passengers_detail ON booking_passengers (detail_id);
GO

/* ---------------------------------------------------------------------------
   10. payments - các lần thanh toán của đơn

   Quan hệ một - NHIỀU với bookings vì thực tế bán tour hay chia hai lần: đặt cọc
   lúc giữ chỗ, trả nốt trước ngày đi. Tổng các lần PAID mới là số tiền đã thu.
   --------------------------------------------------------------------------- */
CREATE TABLE payments (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT         NOT NULL,
    amount     NUMERIC(15,2)  NOT NULL,
    method     NVARCHAR(30)   NOT NULL,   -- BANK_TRANSFER | CASH | MOMO
    status     NVARCHAR(20)   NOT NULL,   -- PENDING | PAID | FAILED | REFUNDED
    txn_ref    NVARCHAR(50)       NULL,   -- mã giao dịch của ngân hàng / ví
    paid_at    DATETIME2(6)       NULL,   -- null khi chưa thực nhận tiền
    note       NVARCHAR(255)      NULL,
    created_at DATETIME2(6)       NULL,
    updated_at DATETIME2(6)       NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id)
);
CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_status  ON payments (status);
/* Chỉ mục thường, KHÔNG duy nhất - tính duy nhất của mã giao dịch do tầng service
   kiểm bằng PaymentRepository.findByTxnRef.

   Vì sao không đặt ràng buộc ở đây: SQL Server coi NULL là một giá trị nên UNIQUE
   thường chỉ cho phép ĐÚNG MỘT dòng NULL, mà lần thanh toán nào chưa hoàn tất
   cũng để trống cột này. Cách chuẩn của SQL Server là chỉ mục CÓ LỌC
   (WHERE txn_ref IS NOT NULL), nhưng bảng nào mang chỉ mục có lọc thì MỌI lệnh
   INSERT/DELETE trên bảng đó đều đòi QUOTED_IDENTIFIER ON - sqlcmd mặc định để
   OFF nên chính script seed sẽ gãy với lỗi Msg 1934. Đã thử và bỏ. */
CREATE INDEX idx_payments_txn_ref ON payments (txn_ref);
GO

/* ---------------------------------------------------------------------------
   11. booking_status_history - nhật ký đổi trạng thái đơn

   bookings.status chỉ giữ trạng thái HIỆN TẠI; bảng này giữ cả quá trình, để trả
   lời được câu "sao đơn tôi bị huỷ". Chỉ ghi thêm, không sửa, không xoá.
   changed_by_id để trống được: hệ thống tự đổi thì không có người đứng sau.
   --------------------------------------------------------------------------- */
CREATE TABLE booking_status_history (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    booking_id    BIGINT        NOT NULL,
    from_status   NVARCHAR(20)      NULL,   -- null ở dòng đầu tiên
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

/* ---------------------------------------------------------------------------
   12. promotions - chương trình khuyến mãi / mã giảm giá

   Mọi điều kiện áp mã (thời gian, số lượt, giá trị đơn tối thiểu, trần giảm giá)
   đều kiểm bằng Java trong Promotion.isRunning / calculateDiscount.
   --------------------------------------------------------------------------- */
CREATE TABLE promotions (
    id                   BIGINT IDENTITY(1,1) NOT NULL,
    code                 NVARCHAR(30)   NOT NULL,   -- khách gõ ở trang thanh toán
    name                 NVARCHAR(150)  NOT NULL,
    description          NVARCHAR(500)      NULL,
    discount_type        NVARCHAR(20)   NOT NULL,   -- PERCENT | AMOUNT
    discount_value       NUMERIC(15,2)  NOT NULL,
    max_discount         NUMERIC(15,2)      NULL,   -- trần giảm của loại PERCENT
    min_order_amount     NUMERIC(15,2)  NOT NULL,
    usage_limit          INT                NULL,   -- null = không giới hạn
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

/* Khoá ngoại bookings.promotion_id phải khai ở đây chứ không khai được ngay
   trong CREATE TABLE bookings: bảng bookings được tạo trước promotions. Giữ thứ
   tự này (bảng lõi trước, bảng bổ sung sau) rồi thêm một câu ALTER, dễ đọc hơn
   là đảo cả file để chiều được một khoá ngoại. */
ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_promotion FOREIGN KEY (promotion_id)
        REFERENCES promotions (id);
CREATE INDEX idx_bookings_promotion ON bookings (promotion_id);
GO

/* ---------------------------------------------------------------------------
   13. coupon_usages - mã giảm giá đã dùng thật

   Không có bảng này thì promotions.used_count chỉ là con số không đối chiếu được,
   và không cách nào chặn "mỗi người một lượt".

   discount_amount là BẢN SAO số tiền đã giảm lúc đặt - sửa giá trị mã về sau
   không được làm đổi hoá đơn cũ, cùng nguyên tắc với booking_details.
   --------------------------------------------------------------------------- */
CREATE TABLE coupon_usages (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    promotion_id    BIGINT        NOT NULL,
    user_id         BIGINT        NOT NULL,
    booking_id      BIGINT        NOT NULL,
    discount_amount NUMERIC(15,2) NOT NULL,
    used_at         DATETIME2(6)  NOT NULL,
    CONSTRAINT pk_coupon_usages PRIMARY KEY (id),
    /* Mỗi đơn chỉ áp một mã - cộng dồn nhiều mã lên một đơn là nguồn gốc của
       những hoá đơn không ai giải thích nổi. */
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

/* ---------------------------------------------------------------------------
   14. reviews - đánh giá tour

   Hai quy tắc làm nên giá trị của bảng, cả hai kiểm bằng Java:
     - chỉ khách đã có đơn COMPLETED chứa tour này mới được đánh giá (booking_id
       giữ lại đơn làm bằng chứng, nhờ đó hiện được nhãn "Đã xác thực");
     - mỗi tài khoản đánh giá một tour đúng một lần (ràng buộc duy nhất bên dưới).
   approved mặc định 0: phải qua kiểm duyệt mới hiện ra trang công khai.
   --------------------------------------------------------------------------- */
CREATE TABLE reviews (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    tour_id     BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    booking_id  BIGINT              NULL,
    rating      INT             NOT NULL,   -- 1..5, kiểm ở tầng Model
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

/* ---------------------------------------------------------------------------
   15. contact_messages - hộp thư liên hệ

   Người gửi KHÔNG bắt buộc có tài khoản nên bảng lưu thẳng họ tên / email / điện
   thoại thay vì trỏ sang users. tour_id để trống được: khách hỏi chung, hoặc bấm
   "Tư vấn tour này" từ trang chi tiết thì mới có tour cụ thể.
   --------------------------------------------------------------------------- */
CREATE TABLE contact_messages (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    full_name     NVARCHAR(100)  NOT NULL,
    email         NVARCHAR(150)  NOT NULL,
    phone         NVARCHAR(20)       NULL,
    subject       NVARCHAR(200)  NOT NULL,
    content       NVARCHAR(2000) NOT NULL,
    status        NVARCHAR(20)   NOT NULL,   -- NEW | IN_PROGRESS | RESOLVED | SPAM
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

/* ---------------------------------------------------------------------------
   16. password_reset_tokens - vé một lần để đặt lại mật khẩu

   Ba quy tắc an toàn (chưa dùng, chưa hết hạn, cấp vé mới thì huỷ vé cũ) đều
   kiểm bằng Java. Token là chuỗi ngẫu nhiên dài: đoán được token là đổi được mật
   khẩu, nên nó phải khó đoán và có hạn dùng.
   --------------------------------------------------------------------------- */
CREATE TABLE password_reset_tokens (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    user_id    BIGINT        NOT NULL,
    token      NVARCHAR(100) NOT NULL,
    expires_at DATETIME2(6)  NOT NULL,
    used_at    DATETIME2(6)      NULL,   -- null = vé chưa dùng
    created_at DATETIME2(6)      NULL,
    updated_at DATETIME2(6)      NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id)
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
GO

/* ---------------------------------------------------------------------------
   Kiểm chứng sau khi chạy: cả bốn con số dưới đây PHẢI bằng 0,
   chứng minh CSDL không dùng CHECK / Function / Stored Procedure / Trigger.
   Riêng số bảng phải bằng 16.
   --------------------------------------------------------------------------- */
SELECT COUNT(*) AS so_check_constraint FROM sys.check_constraints;
SELECT COUNT(*) AS so_function         FROM sys.objects WHERE type IN ('FN','IF','TF');
SELECT COUNT(*) AS so_stored_procedure FROM sys.procedures;
SELECT COUNT(*) AS so_trigger          FROM sys.triggers;
SELECT COUNT(*) AS so_bang             FROM sys.tables;
GO
