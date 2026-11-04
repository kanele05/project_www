-- Dữ liệu mẫu cho TourBookingDB (MariaDB) - xoá sạch rồi chèn lại, ngày tháng tính tương đối theo NOW().
USE TourBookingDB;

DELETE FROM booking_passengers;
DELETE FROM coupon_usages;
DELETE FROM payments;
DELETE FROM booking_status_history;
DELETE FROM reviews;
DELETE FROM contact_messages;
DELETE FROM password_reset_tokens;
DELETE FROM tour_itineraries;
DELETE FROM booking_details;
DELETE FROM bookings;

DELETE FROM promotions;
DELETE FROM tour_departures;
DELETE FROM tour_images;
DELETE FROM tours;
DELETE FROM tour_categories;
DELETE FROM users;

ALTER TABLE booking_passengers AUTO_INCREMENT = 1;
ALTER TABLE coupon_usages AUTO_INCREMENT = 1;
ALTER TABLE promotions AUTO_INCREMENT = 1;
ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE booking_status_history AUTO_INCREMENT = 1;
ALTER TABLE reviews AUTO_INCREMENT = 1;
ALTER TABLE contact_messages AUTO_INCREMENT = 1;
ALTER TABLE password_reset_tokens AUTO_INCREMENT = 1;
ALTER TABLE tour_itineraries AUTO_INCREMENT = 1;
ALTER TABLE booking_details AUTO_INCREMENT = 1;
ALTER TABLE bookings AUTO_INCREMENT = 1;
ALTER TABLE tour_departures AUTO_INCREMENT = 1;
ALTER TABLE tour_images AUTO_INCREMENT = 1;
ALTER TABLE tours AUTO_INCREMENT = 1;
ALTER TABLE tour_categories AUTO_INCREMENT = 1;

ALTER TABLE users AUTO_INCREMENT = 1;

INSERT INTO users (id, full_name, email, password, phone, address, role, enabled, created_at, updated_at)
VALUES
 (1, 'Quản trị hệ thống', 'admin@tourbooking.vn',
     '$2a$10$XuHU4Pp6gFEL9A4GqPa2MO2pZwy9lxeFZYmmocoamnVUIayjOf0cG',
     '0909000001', '12 Nguyễn Văn Bảo, Gò Vấp, TP. Hồ Chí Minh', 'ADMIN',    1, NOW(6), NOW(6)),
 (2, 'Nguyễn Văn An',     'an.nguyen@gmail.com',
     '$2a$10$XuHU4Pp6gFEL9A4GqPa2MO2pZwy9lxeFZYmmocoamnVUIayjOf0cG',
     '0912345678', '25 Lê Lợi, Quận 1, TP. Hồ Chí Minh',         'CUSTOMER', 1, NOW(6), NOW(6)),
 (3, 'Trần Thị Bình',     'binh.tran@gmail.com',
     '$2a$10$XuHU4Pp6gFEL9A4GqPa2MO2pZwy9lxeFZYmmocoamnVUIayjOf0cG',
     '0987654321', '48 Trần Hưng Đạo, Hải Châu, Đà Nẵng',        'CUSTOMER', 1, NOW(6), NOW(6)),
 (4, 'Lê Quốc Cường',     'cuong.le@gmail.com',
     '$2a$10$XuHU4Pp6gFEL9A4GqPa2MO2pZwy9lxeFZYmmocoamnVUIayjOf0cG',
     '0938111222', '77 Kim Mã, Ba Đình, Hà Nội',                 'CUSTOMER', 1, NOW(6), NOW(6));

INSERT INTO tour_categories (id, name, slug, description, image_url, active, created_at, updated_at)
VALUES
 (1, 'Du lịch biển đảo',           'du-lich-bien-dao',
     'Nghỉ dưỡng ven biển, lặn ngắm san hô và khám phá các hòn đảo.',
     '/images/categories/du-lich-bien-dao.svg',      1, NOW(6), NOW(6)),
 (2, 'Du lịch núi rừng',           'du-lich-nui-rung',
     'Chinh phục đỉnh cao, săn mây và trải nghiệm khí hậu vùng cao.',
     '/images/categories/du-lich-nui-rung.svg',      1, NOW(6), NOW(6)),
 (3, 'Du lịch văn hoá - tâm linh', 'du-lich-van-hoa-tam-linh',
     'Hành hương, thăm di tích lịch sử và các công trình kiến trúc cổ.',
     '/images/categories/du-lich-van-hoa-tam-linh.svg', 1, NOW(6), NOW(6)),
 (4, 'Du lịch nước ngoài',         'du-lich-nuoc-ngoai',
     'Các hành trình khám phá châu Á và châu Âu.',
     '/images/categories/du-lich-nuoc-ngoai.svg',    1, NOW(6), NOW(6)),
 (5, 'Du lịch sinh thái',          'du-lich-sinh-thai',
     'Miệt vườn, rừng ngập mặn và các khu bảo tồn thiên nhiên.',
     '/images/categories/du-lich-sinh-thai.svg',     1, NOW(6), NOW(6)),
 (6, 'Du lịch nghỉ dưỡng cao cấp', 'du-lich-nghi-duong-cao-cap',
     'Resort 4-5 sao, suối khoáng nóng và dịch vụ chăm sóc sức khoẻ.',
     '/images/categories/du-lich-nghi-duong-cao-cap.svg', 1, NOW(6), NOW(6));

INSERT INTO tours (id, code, name, slug, short_description, departure_location, destination,
                   duration_days, duration_nights, base_price, transportation,
                   featured, active, view_count, search_text, category_id, created_at, updated_at)
VALUES
 (1, 'PQ-3N2D', 'Phú Quốc - Đảo Ngọc 3N2Đ', 'phu-quoc-dao-ngoc-3n2d',
     'Câu cá, lặn ngắm san hô ở Hòn Thơm và ngắm hoàng hôn tại Sunset Sanato.',
     'TP. Hồ Chí Minh', 'Phú Quốc', 3, 2, 5990000, 'Máy bay', 1, 1, 0,
     'phu quoc - dao ngoc 3n2d phu quoc tp. ho chi minh pq-3n2d', 1, NOW(6), NOW(6)),

 (2, 'NT-4N3D', 'Nha Trang - Vịnh Ngọc 4N3Đ', 'nha-trang-vinh-ngoc-4n3d',
     'Tắm bùn khoáng, tham quan VinWonders và bốn đảo nổi tiếng của vịnh Nha Trang.',
     'TP. Hồ Chí Minh', 'Nha Trang', 4, 3, 4590000, 'Máy bay', 1, 1, 0,
     'nha trang - vinh ngoc 4n3d nha trang tp. ho chi minh nt-4n3d', 1, NOW(6), NOW(6)),

 (3, 'CD-3N2D', 'Côn Đảo - Tâm Linh & Biển Xanh 3N2Đ', 'con-dao-tam-linh-bien-xanh-3n2d',
     'Viếng nghĩa trang Hàng Dương, thăm nhà tù Côn Đảo và tắm biển Đầm Trầu.',
     'TP. Hồ Chí Minh', 'Côn Đảo', 3, 2, 6490000, 'Máy bay', 0, 1, 0,
     'con dao - tam linh & bien xanh 3n2d con dao tp. ho chi minh cd-3n2d', 1, NOW(6), NOW(6)),

 (4, 'HL-3N2D', 'Hạ Long - Kỳ Quan Vịnh Biển 3N2Đ', 'ha-long-ky-quan-vinh-bien-3n2d',
     'Du thuyền ngủ đêm trên vịnh, chèo kayak và khám phá hang Sửng Sốt.',
     'Hà Nội', 'Hạ Long', 3, 2, 3890000, 'Xe du lịch', 1, 1, 0,
     'ha long - ky quan vinh bien 3n2d ha long ha noi hl-3n2d', 1, NOW(6), NOW(6)),

 (5, 'QN-4N3D', 'Quy Nhơn - Phú Yên 4N3Đ', 'quy-nhon-phu-yen-4n3d',
     'Kỳ Co - Eo Gió, Gành Đá Đĩa và ghềnh Đá Dĩa Phú Yên.',
     'TP. Hồ Chí Minh', 'Quy Nhơn', 4, 3, 5190000, 'Máy bay', 0, 1, 0,
     'quy nhon - phu yen 4n3d quy nhon tp. ho chi minh qn-4n3d', 1, NOW(6), NOW(6)),

 (6, 'SP-3N2D', 'Sa Pa - Săn Mây Fansipan 3N2Đ', 'sa-pa-san-may-fansipan-3n2d',
     'Cáp treo lên đỉnh Fansipan, bản Cát Cát và chợ phiên vùng cao.',
     'Hà Nội', 'Sa Pa', 3, 2, 3290000, 'Xe giường nằm', 1, 1, 0,
     'sa pa - san may fansipan 3n2d sa pa ha noi sp-3n2d', 2, NOW(6), NOW(6)),

 (7, 'HG-4N3D', 'Hà Giang - Cao Nguyên Đá 4N3Đ', 'ha-giang-cao-nguyen-da-4n3d',
     'Đèo Mã Pí Lèng, sông Nho Quế và cột cờ Lũng Cú.',
     'Hà Nội', 'Hà Giang', 4, 3, 4190000, 'Xe du lịch', 0, 1, 0,
     'ha giang - cao nguyen da 4n3d ha giang ha noi hg-4n3d', 2, NOW(6), NOW(6)),

 (8, 'DL-3N2D', 'Đà Lạt - Thành Phố Ngàn Hoa 3N2Đ', 'da-lat-thanh-pho-ngan-hoa-3n2d',
     'Đồi chè Cầu Đất, thác Datanla và chợ đêm Đà Lạt.',
     'TP. Hồ Chí Minh', 'Đà Lạt', 3, 2, 2890000, 'Xe du lịch', 1, 1, 0,
     'da lat - thanh pho ngan hoa 3n2d da lat tp. ho chi minh dl-3n2d', 2, NOW(6), NOW(6)),

 (9, 'MC-3N2D', 'Mộc Châu - Mùa Hoa 3N2Đ', 'moc-chau-mua-hoa-3n2d',
     'Đồi chè trái tim, thác Dải Yếm và rừng thông bản Áng.',
     'Hà Nội', 'Mộc Châu', 3, 2, 2690000, 'Xe du lịch', 0, 1, 0,
     'moc chau - mua hoa 3n2d moc chau ha noi mc-3n2d', 2, NOW(6), NOW(6)),

 (10, 'HUE-4N3D', 'Huế - Đà Nẵng - Hội An 4N3Đ', 'hue-da-nang-hoi-an-4n3d',
     'Đại Nội, chùa Thiên Mụ, Bà Nà Hills và phố cổ Hội An về đêm.',
     'TP. Hồ Chí Minh', 'Huế', 4, 3, 5490000, 'Máy bay', 1, 1, 0,
     'hue - da nang - hoi an 4n3d hue tp. ho chi minh hue-4n3d', 3, NOW(6), NOW(6)),

 (11, 'NB-2N1D', 'Ninh Bình - Tràng An - Bái Đính 2N1Đ', 'ninh-binh-trang-an-bai-dinh-2n1d',
     'Du thuyền Tràng An, chùa Bái Đính và hang Múa.',
     'Hà Nội', 'Ninh Bình', 2, 1, 1890000, 'Xe du lịch', 0, 1, 0,
     'ninh binh - trang an - bai dinh 2n1d ninh binh ha noi nb-2n1d', 3, NOW(6), NOW(6)),

 (12, 'TN-2N1D', 'Tây Ninh - Núi Bà Đen 2N1Đ', 'tay-ninh-nui-ba-den-2n1d',
     'Cáp treo núi Bà Đen, toà thánh Cao Đài và hồ Dầu Tiếng.',
     'TP. Hồ Chí Minh', 'Tây Ninh', 2, 1, 1290000, 'Xe du lịch', 0, 1, 0,
     'tay ninh - nui ba den 2n1d tay ninh tp. ho chi minh tn-2n1d', 3, NOW(6), NOW(6)),

 (13, 'YT-2N1D', 'Quảng Ninh - Yên Tử Hành Hương 2N1Đ', 'quang-ninh-yen-tu-hanh-huong-2n1d',
     'Chùa Đồng Yên Tử, thiền viện Trúc Lâm và khu di tích nhà Trần.',
     'Hà Nội', 'Quảng Ninh', 2, 1, 1690000, 'Xe du lịch', 0, 1, 0,
     'quang ninh - yen tu hanh huong 2n1d quang ninh ha noi yt-2n1d', 3, NOW(6), NOW(6)),

 (14, 'TL-5N4D', 'Thái Lan - Bangkok - Pattaya 5N4Đ', 'thai-lan-bangkok-pattaya-5n4d',
     'Chùa Phật Vàng, chợ nổi Bốn Miền và show Alcazar.',
     'TP. Hồ Chí Minh', 'Thái Lan', 5, 4, 9990000, 'Máy bay', 1, 1, 0,
     'thai lan - bangkok - pattaya 5n4d thai lan tp. ho chi minh tl-5n4d', 4, NOW(6), NOW(6)),

 (15, 'SG-4N3D', 'Singapore - Malaysia 4N3Đ', 'singapore-malaysia-4n3d',
     'Gardens by the Bay, đảo Sentosa và cao nguyên Genting.',
     'TP. Hồ Chí Minh', 'Singapore', 4, 3, 12900000, 'Máy bay', 0, 1, 0,
     'singapore - malaysia 4n3d singapore tp. ho chi minh sg-4n3d', 4, NOW(6), NOW(6)),

 (16, 'HQ-5N4D', 'Hàn Quốc - Seoul - Nami 5N4Đ', 'han-quoc-seoul-nami-5n4d',
     'Đảo Nami, cung Gyeongbok và tháp Namsan.',
     'Hà Nội', 'Hàn Quốc', 5, 4, 16900000, 'Máy bay', 1, 1, 0,
     'han quoc - seoul - nami 5n4d han quoc ha noi hq-5n4d', 4, NOW(6), NOW(6)),

 (17, 'NB-6N5D', 'Nhật Bản - Cung Đường Vàng 6N5Đ', 'nhat-ban-cung-duong-vang-6n5d',
     'Tokyo - Phú Sĩ - Kyoto - Osaka mùa hoa anh đào.',
     'TP. Hồ Chí Minh', 'Nhật Bản', 6, 5, 28900000, 'Máy bay', 0, 1, 0,
     'nhat ban - cung duong vang 6n5d nhat ban tp. ho chi minh nb-6n5d', 4, NOW(6), NOW(6)),

 (18, 'MT-3N2D', 'Miền Tây - Cần Thơ - Cà Mau 3N2Đ', 'mien-tay-can-tho-ca-mau-3n2d',
     'Chợ nổi Cái Răng, rừng tràm Trà Sư và mũi Cà Mau.',
     'TP. Hồ Chí Minh', 'Cần Thơ', 3, 2, 2390000, 'Xe du lịch', 0, 1, 0,
     'mien tay - can tho - ca mau 3n2d can tho tp. ho chi minh mt-3n2d', 5, NOW(6), NOW(6)),

 (19, 'CT-2N1D', 'Cát Tiên - Vườn Quốc Gia 2N1Đ', 'cat-tien-vuon-quoc-gia-2n1d',
     'Đi bộ xuyên rừng, ngắm thú đêm và thăm đảo Tiên.',
     'TP. Hồ Chí Minh', 'Đồng Nai', 2, 1, 1590000, 'Xe du lịch', 0, 1, 0,
     'cat tien - vuon quoc gia 2n1d dong nai tp. ho chi minh ct-2n1d', 5, NOW(6), NOW(6)),

 (20, 'MN-3N2D', 'Mũi Né - Resort Nghỉ Dưỡng 3N2Đ', 'mui-ne-resort-nghi-duong-3n2d',
     'Resort 4 sao sát biển, đồi cát bay và làng chài Mũi Né.',
     'TP. Hồ Chí Minh', 'Phan Thiết', 3, 2, 4290000, 'Xe du lịch', 1, 1, 0,
     'mui ne - resort nghi duong 3n2d phan thiet tp. ho chi minh mn-3n2d', 6, NOW(6), NOW(6));

UPDATE tours SET thumbnail = CASE id
    WHEN 1  THEN '/images/tours/tour01_phu-quoc.jpg'
    WHEN 2  THEN '/images/tours/tour02_nha-trang.jpg'
    WHEN 3  THEN '/images/tours/tour03_con-dao.jpg'
    WHEN 4  THEN '/images/tours/tour04_ha-long.jpg'
    WHEN 5  THEN '/images/tours/tour05_quy-nhon.jpg'
    WHEN 6  THEN '/images/tours/tour06_sa-pa.jpg'
    WHEN 7  THEN '/images/tours/tour07_ha-giang.jpg'
    WHEN 8  THEN '/images/tours/tour08_da-lat.jpg'
    WHEN 9  THEN '/images/tours/tour09_moc-chau.jpg'
    WHEN 10 THEN '/images/tours/tour10_hoi-an.jpg'
    WHEN 11 THEN '/images/tours/tour11_ninh-binh.jpg'
    WHEN 12 THEN '/images/tours/tour12_tay-ninh.jpg'
    WHEN 13 THEN '/images/tours/tour13_quang-ninh.jpg'
    WHEN 14 THEN '/images/tours/tour14_thai-lan.jpg'
    WHEN 15 THEN '/images/tours/tour15_singapore.jpg'
    WHEN 16 THEN '/images/tours/tour16_han-quoc.jpg'
    WHEN 17 THEN '/images/tours/tour17_nhat-ban.jpg'
    WHEN 18 THEN '/images/tours/tour18_mien-tay.jpg'
    WHEN 19 THEN '/images/tours/tour19_cat-tien.jpg'
    WHEN 20 THEN '/images/tours/tour20_mui-ne.jpg'
END;

UPDATE tours
SET description = CONCAT(
        short_description, '\n\n',
        'Hành trình ', duration_days, ' ngày ', duration_nights, ' đêm khởi hành từ ', departure_location,
        ', di chuyển bằng ', LOWER(transportation), '.', '\n',
        'Giá tour đã bao gồm vé tham quan, khách sạn tiêu chuẩn, các bữa ăn theo ',
        'chương trình, hướng dẫn viên suốt tuyến và bảo hiểm du lịch. Giá chưa bao ',
        'gồm chi phí cá nhân, đồ uống và tiền tip cho hướng dẫn viên.');

UPDATE tours t
SET itinerary = (
    SELECT GROUP_CONCAT(
               CONCAT('Ngày ', d.n, ': ',
                      CASE
                          WHEN d.n = 1 THEN
                              CONCAT(t.departure_location, ' - ', t.destination,
                                     '. Khởi hành, nhận phòng và tham quan các điểm gần trung tâm.')
                          WHEN d.n = t.duration_days THEN
                              CONCAT(t.destination, ' - ', t.departure_location,
                                     '. Mua sắm đặc sản, trả phòng và về điểm xuất phát.')
                          ELSE
                              CONCAT('Tham quan các điểm nổi bật của ', t.destination,
                                     ', tự do trải nghiệm ẩm thực địa phương buổi tối.')
                      END)
               ORDER BY d.n SEPARATOR '\n')
    FROM (
        SELECT 1 AS n
        UNION ALL SELECT 2
        UNION ALL SELECT 3
        UNION ALL SELECT 4
        UNION ALL SELECT 5
        UNION ALL SELECT 6
    ) AS d
    WHERE d.n <= t.duration_days
);

INSERT INTO tour_departures (id, tour_id, departure_date, return_date, total_seats,
                             available_seats, price_adult, price_child, active, version,
                             created_at, updated_at)
SELECT (t.id - 1) * 3 + d.thu_tu,
       t.id,
       (CURDATE() + INTERVAL (d.cach_ngay) DAY),
       (CURDATE() + INTERVAL (d.cach_ngay + t.duration_days - 1) DAY),
       d.so_cho,
       d.so_cho,
       ROUND(t.base_price * d.he_so_gia, 2),
       ROUND(t.base_price * d.he_so_gia * 0.7, 2),
       1,
       0,
       NOW(6), NOW(6)
FROM tours t
CROSS JOIN (
    SELECT 1 AS thu_tu, 14 AS cach_ngay, 30 AS so_cho, 1.00 AS he_so_gia
    UNION ALL SELECT 2, 30, 25, 1.05
    UNION ALL SELECT 3, 50, 20, 1.10
) AS d;

UPDATE tour_departures
SET departure_date = (CURDATE() + INTERVAL (-20) DAY),
    return_date = (CURDATE() + INTERVAL (-20 + (SELECT duration_days FROM tours WHERE id = tour_departures.tour_id) - 1) DAY)
WHERE id = 16;

INSERT INTO bookings (id, code, user_id, booking_date, customer_name, customer_email,
                      customer_phone, customer_address, total_amount, promotion_id,
                      discount_amount, status, note,
                      payment_method, created_at, updated_at)
SELECT b.id,
       CONCAT('TB', DATE_FORMAT(NOW(6) - INTERVAL b.cach_gio HOUR, '%Y%m%d'), b.hau_to),
       u.id,
       NOW(6) - INTERVAL b.cach_gio HOUR,
       u.full_name, u.email, u.phone, u.address,
       0,
       NULL, 0,
       b.trang_thai,
       b.ghi_chu,
       'Chuyển khoản ngân hàng',
       NOW(6), NOW(6)
FROM (
    SELECT 1 AS id, 2 AS user_id, 12 * 24 AS cach_gio, 'CONFIRMED' AS trang_thai, 'HK4M2P' AS hau_to, CAST(NULL AS CHAR(500)) AS ghi_chu
    UNION ALL SELECT 2, 3, 5, 'PENDING', 'QT7B9X', NULL
    UNION ALL SELECT 3, 4, 40 * 24, 'COMPLETED', 'LM3D8R', NULL
    UNION ALL SELECT 4, 2, 3, 'PENDING', 'VP6K2N', NULL
    UNION ALL SELECT 5, 3, 20 * 24, 'CANCELLED', 'ZC5H7J', 'Khách báo bận, xin huỷ đơn.'
) AS b
JOIN users u ON u.id = b.user_id;

INSERT INTO booking_details (id, booking_id, departure_id, tour_name_snapshot,
                             num_adults, num_children, unit_price_adult, unit_price_child, subtotal)
SELECT x.id, x.booking_id, x.departure_id, t.name,
       x.nguoi_lon, x.tre_em,
       d.price_adult, d.price_child,
       d.price_adult * x.nguoi_lon + d.price_child * x.tre_em
FROM (
    SELECT 1 AS id, 1 AS booking_id, 1 AS departure_id, 2 AS nguoi_lon, 1 AS tre_em
    UNION ALL SELECT 2, 2, 4, 2, 0
    UNION ALL SELECT 3, 3, 16, 4, 2
    UNION ALL SELECT 4, 4, 25, 2, 2
    UNION ALL SELECT 5, 4, 40, 1, 0
    UNION ALL SELECT 6, 5, 10, 3, 0
) AS x
JOIN tour_departures d ON d.id = x.departure_id
JOIN tours t           ON t.id = d.tour_id;

UPDATE bookings b
JOIN (SELECT booking_id, SUM(subtotal) AS tong
      FROM booking_details
      GROUP BY booking_id) AS x ON x.booking_id = b.id
SET b.total_amount = x.tong;

UPDATE tour_departures d
JOIN (SELECT bd.departure_id, SUM(bd.num_adults + bd.num_children) AS so_khach
      FROM booking_details bd
      JOIN bookings b ON b.id = bd.booking_id
      WHERE b.status <> 'CANCELLED'
      GROUP BY bd.departure_id) AS x ON x.departure_id = d.id
SET d.available_seats = d.total_seats - x.so_khach;

INSERT INTO tour_itineraries (tour_id, day_no, title, description, meals, accommodation)
SELECT t.id,
       n.day_no,
       CASE
           WHEN n.day_no = 1 THEN CONCAT('Khởi hành: ', t.departure_location, ' - ', t.destination)
           WHEN n.day_no = t.duration_days THEN CONCAT('Mua sắm và tiễn khách về ', t.departure_location)
           ELSE CONCAT('Khám phá ', t.destination)
       END,
       CASE
           WHEN n.day_no = 1 THEN 'Xe/máy bay đón khách, làm thủ tục nhận phòng, ăn tối và tự do nghỉ ngơi.'
           WHEN n.day_no = t.duration_days THEN 'Trả phòng, mua đặc sản làm quà, xe đưa ra sân bay hoặc bến xe.'
           ELSE 'Tham quan các điểm nổi bật theo chương trình, buổi chiều tự do tắm biển hoặc nghỉ tại resort.'
       END,
       CASE
           WHEN n.day_no = 1 THEN 'Trưa, Tối'
           WHEN n.day_no = t.duration_days THEN 'Sáng'
           ELSE 'Sáng, Trưa, Tối'
       END,
       CASE
           WHEN n.day_no = t.duration_days THEN 'Kết thúc chương trình'
           ELSE CONCAT('Khách sạn 4 sao tại ', t.destination)
       END
FROM tours t
JOIN (
    SELECT 1 AS day_no
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
) AS n ON n.day_no <= t.duration_days
WHERE t.id <= 3;

INSERT INTO booking_passengers (detail_id, full_name, passenger_type, gender,
                                birth_date, id_number, phone, single_room, note)
SELECT p.detail_id, p.ho_ten, p.loai, p.gioi_tinh,
       CASE WHEN p.tuoi IS NULL THEN NULL
            ELSE (CURDATE() + INTERVAL (-p.tuoi) YEAR) END,
       p.so_giay_to, p.dien_thoai, p.phong_don, p.ghi_chu
FROM (
    SELECT 1 AS detail_id, 'Nguyễn Văn An' AS ho_ten, 'ADULT' AS loai, 'MALE' AS gioi_tinh, 34 AS tuoi, '079201001234' AS so_giay_to, '0901234567' AS dien_thoai, 0 AS phong_don, CAST(NULL AS CHAR(255)) AS ghi_chu
    UNION ALL SELECT 1, 'Trần Thị Bích Ngọc', 'ADULT', 'FEMALE', 32, '079202001235', NULL, 0, NULL
    UNION ALL SELECT 1, 'Nguyễn Bảo Anh', 'CHILD', 'FEMALE', 8, NULL, NULL, 0, 'Bé 8 tuổi, cần ghế riêng trên xe'
    UNION ALL SELECT 2, 'Trần Văn Bình', 'ADULT', 'MALE', 41, '079203001236', '0912345678', 0, NULL
    UNION ALL SELECT 2, 'Lê Thị Hồng', 'ADULT', 'FEMALE', 39, '079204001237', NULL, 0, NULL
    UNION ALL SELECT 3, 'Lê Minh Cường', 'ADULT', 'MALE', 45, '079205001238', '0923456789', 0, NULL
    UNION ALL SELECT 3, 'Phạm Thị Mai', 'ADULT', 'FEMALE', 43, '079206001239', NULL, 0, NULL
    UNION ALL SELECT 3, 'Lê Minh Khang', 'ADULT', 'MALE', 28, '079207001240', NULL, 1, 'Yêu cầu phòng đơn'
    UNION ALL SELECT 3, 'Đặng Thị Kiều Ửng', 'ADULT', 'FEMALE', 26, '079208001241', NULL, 0, 'Ăn chay trường'
    UNION ALL SELECT 3, 'Lê Bảo Ngọc', 'CHILD', 'FEMALE', 10, NULL, NULL, 0, NULL
    UNION ALL SELECT 3, 'Lê Gia Huy', 'CHILD', 'MALE', 6, NULL, NULL, 0, 'Dị ứng hải sản'
    UNION ALL SELECT 4, 'Nguyễn Văn An', 'ADULT', 'MALE', 34, '079201001234', '0901234567', 0, NULL
    UNION ALL SELECT 4, 'Trần Thị Bích Ngọc', 'ADULT', 'FEMALE', 32, '079202001235', NULL, 0, NULL
    UNION ALL SELECT 4, 'Nguyễn Bảo Anh', 'CHILD', 'FEMALE', 8, NULL, NULL, 0, NULL
    UNION ALL SELECT 4, 'Nguyễn Minh Quân', 'CHILD', 'MALE', 5, NULL, NULL, 0, NULL
    UNION ALL SELECT 5, 'Nguyễn Văn An', 'ADULT', 'MALE', 34, '079201001234', '0901234567', 1, 'Đi công tác kết hợp, ở phòng đơn'
    UNION ALL SELECT 6, 'Trần Văn Bình', 'ADULT', 'MALE', 41, '079203001236', '0912345678', 0, NULL
    UNION ALL SELECT 6, 'Lê Thị Hồng', 'ADULT', 'FEMALE', 39, '079204001237', NULL, 0, NULL
    UNION ALL SELECT 6, 'Trần Quốc Toản', 'ADULT', 'MALE', 37, '079209001242', NULL, 0, NULL
) AS p;

INSERT INTO payments (booking_id, amount, method, status, txn_ref, paid_at, note,
                      created_at, updated_at)
SELECT b.id,
       CASE WHEN x.ty_le IS NULL THEN b.total_amount
            ELSE ROUND(b.total_amount * x.ty_le, 0) END,
       x.hinh_thuc, x.trang_thai, x.ma_gd,
       CASE WHEN x.trang_thai IN ('PAID', 'REFUNDED')
            THEN (b.booking_date + INTERVAL (x.sau_gio) HOUR) END,
       x.ghi_chu,
       NOW(6), NOW(6)
FROM (
    SELECT 1 AS booking_id, 0.5 AS ty_le, 'BANK_TRANSFER' AS hinh_thuc, 'PAID' AS trang_thai, 'VCB2026080100117' AS ma_gd, 2 AS sau_gio, CAST('Đặt cọc 50% khi giữ chỗ' AS CHAR(255)) AS ghi_chu
    UNION ALL SELECT 1, 0.5, 'BANK_TRANSFER', 'PENDING', NULL, 0, 'Còn lại, thu trước ngày khởi hành'
    UNION ALL SELECT 2, NULL, 'MOMO', 'PENDING', NULL, 0, 'Khách chọn thanh toán qua ví MoMo'
    UNION ALL SELECT 3, 0.5, 'BANK_TRANSFER', 'PAID', 'VCB2026070500342', 3, 'Đặt cọc 50%'
    UNION ALL SELECT 3, 0.5, 'CASH', 'PAID', 'PT-2026-0451', 72, 'Trả nốt tại văn phòng'
    UNION ALL SELECT 4, NULL, 'BANK_TRANSFER', 'PENDING', NULL, 0, 'Chờ chuyển khoản trước ngày khởi hành'
    UNION ALL SELECT 5, NULL, 'BANK_TRANSFER', 'PAID', 'VCB2026071200987', 1, 'Đã thu đủ trước khi khách xin huỷ'
    UNION ALL SELECT 5, NULL, 'BANK_TRANSFER', 'REFUNDED', 'VCB2026071200988', 5, 'Đã hoàn tiền sau khi khách xin huỷ'
) AS x
JOIN bookings b ON b.id = x.booking_id;

INSERT INTO booking_status_history (booking_id, from_status, to_status,
                                    changed_by_id, reason, changed_at)
SELECT b.id, h.tu, h.den, h.nguoi, h.ly_do, (b.booking_date + INTERVAL (h.sau_gio) HOUR)
FROM (
    SELECT 1 AS booking_id, CAST(NULL AS CHAR(20)) AS tu, 'PENDING' AS den, CAST(NULL AS SIGNED) AS nguoi, CAST('Khách đặt tour trên website' AS CHAR(255)) AS ly_do, 0 AS sau_gio
    UNION ALL SELECT 1, 'PENDING', 'CONFIRMED', 1, 'Đã nhận tiền cọc, xác nhận chỗ', 24
    UNION ALL SELECT 2, NULL, 'PENDING', NULL, 'Khách đặt tour trên website', 0
    UNION ALL SELECT 3, NULL, 'PENDING', NULL, 'Khách đặt tour trên website', 0
    UNION ALL SELECT 3, 'PENDING', 'CONFIRMED', 1, 'Đã nhận tiền cọc, xác nhận chỗ', 20
    UNION ALL SELECT 3, 'CONFIRMED', 'COMPLETED', 1, 'Đoàn đã kết thúc chương trình', 96
    UNION ALL SELECT 4, NULL, 'PENDING', NULL, 'Khách đặt tour trên website', 0
    UNION ALL SELECT 5, NULL, 'PENDING', NULL, 'Khách đặt tour trên website', 0
    UNION ALL SELECT 5, 'PENDING', 'CANCELLED', 1, 'Khách báo bận, xin huỷ đơn - đã hoàn tiền', 30
) AS h
JOIN bookings b ON b.id = h.booking_id;

INSERT INTO promotions (id, code, name, description, discount_type, discount_value,
                        max_discount, min_order_amount, usage_limit, usage_limit_per_user,
                        used_count, start_at, end_at, active, created_at, updated_at)
VALUES
 (1, 'HELLO2026', 'Chào hè 2026',
     'Giảm 10% cho đơn từ 5 triệu, tối đa 500.000đ. Mỗi tài khoản dùng một lần.',
     'PERCENT', 10, 500000, 5000000, 100, 1, 1,
     (NOW(6) + INTERVAL (-30) DAY), (NOW(6) + INTERVAL (60) DAY), 1,
     NOW(6), NOW(6)),
 (2, 'SUMMER500', 'Giảm thẳng 500K',
     'Giảm 500.000đ cho đơn từ 10 triệu, không giới hạn số lượt.',
     'AMOUNT', 500000, NULL, 10000000, NULL, 1, 1,
     (NOW(6) + INTERVAL (-15) DAY), (NOW(6) + INTERVAL (45) DAY), 1,
     NOW(6), NOW(6)),
 (3, 'VIP20', 'Ưu đãi khách thân thiết',
     'Giảm 20% tối đa 2 triệu, chỉ phát cho 20 lượt đầu tiên.',
     'PERCENT', 20, 2000000, 15000000, 20, 1, 0,
     (NOW(6) + INTERVAL (-7) DAY), (NOW(6) + INTERVAL (23) DAY), 1,
     NOW(6), NOW(6)),
 (4, 'TET2026', 'Ưu đãi Tết 2026 (đã hết hạn)',
     'Chương trình đã kết thúc - giữ lại để đối chiếu số liệu.',
     'PERCENT', 15, 1000000, 3000000, 50, 1, 0,
     (NOW(6) + INTERVAL (-120) DAY), (NOW(6) + INTERVAL (-60) DAY), 1,
     NOW(6), NOW(6));

INSERT INTO coupon_usages (promotion_id, user_id, booking_id, discount_amount, used_at)
SELECT p.id, b.user_id, b.id,
       CASE WHEN p.discount_type = 'PERCENT'
            THEN CASE WHEN ROUND(b.total_amount * p.discount_value / 100, 0) > p.max_discount
                      THEN p.max_discount
                      ELSE ROUND(b.total_amount * p.discount_value / 100, 0) END
            ELSE p.discount_value END,
       b.booking_date
FROM (
    SELECT 1 AS booking_id, 'HELLO2026' AS ma
    UNION ALL SELECT 3, 'SUMMER500'
) AS x
JOIN bookings   b ON b.id   = x.booking_id
JOIN promotions p ON p.code = x.ma;

UPDATE bookings b
JOIN coupon_usages cu ON cu.booking_id = b.id
SET b.promotion_id    = cu.promotion_id,
    b.discount_amount = cu.discount_amount,
    b.total_amount    = b.total_amount - cu.discount_amount;

UPDATE promotions p
JOIN (SELECT promotion_id, COUNT(*) AS so_luot
      FROM coupon_usages GROUP BY promotion_id) AS x ON x.promotion_id = p.id
SET p.used_count = x.so_luot;

INSERT INTO reviews (tour_id, user_id, booking_id, rating, title, content, approved,
                     admin_reply, replied_at, created_at, updated_at)
SELECT COALESCE(r.tour_id_truc_tiep,
                (SELECT dep.tour_id
                 FROM booking_details bd
                 JOIN tour_departures dep ON dep.id = bd.departure_id
                 WHERE bd.booking_id = r.booking_id
                 LIMIT 1)),
       r.user_id, r.booking_id, r.diem, r.tieu_de, r.noi_dung, r.duyet,
       r.tra_loi,
       CASE WHEN r.tra_loi IS NULL THEN NULL ELSE NOW(6) END,
       NOW(6) - INTERVAL r.cach_ngay DAY, NOW(6)
FROM (
    SELECT CAST(NULL AS SIGNED) AS tour_id_truc_tiep, 3 AS booking_id, 4 AS user_id, 5 AS diem, CAST('Chuyến đi trọn vẹn' AS CHAR(200)) AS tieu_de, 'Hướng dẫn viên nhiệt tình, lịch trình hợp lý, khách sạn sạch sẽ. Gia đình tôi rất hài lòng.' AS noi_dung, 1 AS duyet, CAST('Cảm ơn anh Cường đã tin tưởng, hẹn gặp lại gia đình mình!' AS CHAR(1000)) AS tra_loi, 3 AS cach_ngay
    UNION ALL SELECT 1, NULL, 2, 5, 'Biển đẹp, đồ ăn ngon', 'Hòn Thơm nước trong vắt, buổi chiều ngắm hoàng hôn ở Sunset Sanato rất đáng tiền.', 1, NULL, 12
    UNION ALL SELECT 1, NULL, 3, 4, 'Đáng đi nhưng hơi gấp', 'Lịch trình ngày thứ hai khá dày, nên bớt một điểm để có thời gian nghỉ.', 1, NULL, 20
    UNION ALL SELECT 4, NULL, 3, 4, 'Nha Trang vui', 'Đảo đẹp, hải sản tươi. Xe đưa đón đúng giờ, tài xế thân thiện.', 1, NULL, 25
    UNION ALL SELECT 10, NULL, 2, 3, 'Ổn nhưng khách sạn cũ', 'Chương trình tham quan tốt, riêng phòng nghỉ hơi xuống cấp so với ảnh quảng cáo.', 0, NULL, 1
) AS r;

INSERT INTO contact_messages (full_name, email, phone, subject, content, status,
                              tour_id, handled_by_id, reply_note, handled_at,
                              created_at, updated_at)
SELECT c.ho_ten, c.email, c.dien_thoai, c.tieu_de, c.noi_dung, c.trang_thai,
       c.tour_id, c.nguoi_xu_ly, c.tra_loi,
       CASE WHEN c.nguoi_xu_ly IS NULL THEN NULL
            ELSE (NOW(6) + INTERVAL (-c.cach_ngay + 1) DAY) END,
       (NOW(6) + INTERVAL (-c.cach_ngay) DAY), NOW(6)
FROM (
    SELECT 'Phạm Thu Hà' AS ho_ten, 'thuha.pham@gmail.com' AS email, '0934567890' AS dien_thoai, 'Hỏi lịch khởi hành tháng sau' AS tieu_de, 'Cho mình hỏi tour Phú Quốc còn chỗ ngày 20 tháng sau không, đoàn mình 6 người lớn.' AS noi_dung, 'NEW' AS trang_thai, CAST(1 AS SIGNED) AS tour_id, CAST(NULL AS SIGNED) AS nguoi_xu_ly, CAST(NULL AS CHAR(1000)) AS tra_loi, 1 AS cach_ngay
    UNION ALL SELECT 'Võ Thanh Tùng', 'tung.vo@company.vn', '0945678901', 'Đặt tour cho công ty 30 người', 'Công ty mình muốn tổ chức team building 30 người trong 3 ngày, nhờ bên bạn báo giá giúp.', 'NEW', NULL, NULL, NULL, 2
    UNION ALL SELECT 'Ngô Thị Lan', 'lan.ngo@gmail.com', NULL, 'Đổi ngày khởi hành', 'Mình đã đặt tour nhưng có việc đột xuất, muốn dời sang tuần sau có được không?', 'IN_PROGRESS', NULL, 1, NULL, 3
    UNION ALL SELECT 'Đỗ Minh Khoa', 'khoa.do@gmail.com', '0956789012', 'Góp ý về website', 'Phần tìm kiếm gõ không dấu vẫn ra đúng kết quả, rất tiện. Cảm ơn nhóm phát triển.', 'RESOLVED', NULL, 1, 'Đã cảm ơn khách qua email.', 8
) AS c;

INSERT INTO password_reset_tokens (user_id, token, expires_at, used_at, created_at, updated_at)
VALUES
 (2, 'seed-token-da-su-dung-0000000000000001',
     ((NOW(6) + INTERVAL (-5) DAY) + INTERVAL (-30) MINUTE),
     ((NOW(6) + INTERVAL (-5) DAY) + INTERVAL (-50) MINUTE),
     (NOW(6) + INTERVAL (-5) DAY), NOW(6)),
 (3, 'seed-token-het-han-000000000000000002',
     (NOW(6) + INTERVAL (-2) DAY), NULL,
     (NOW(6) + INTERVAL (-2) DAY), NOW(6));

SELECT 'Người dùng'      AS bang, COUNT(*) AS so_dong FROM users
UNION ALL SELECT 'Danh mục',        COUNT(*) FROM tour_categories
UNION ALL SELECT 'Tour',            COUNT(*) FROM tours
UNION ALL SELECT 'Đợt khởi hành',   COUNT(*) FROM tour_departures
UNION ALL SELECT 'Đơn đặt tour',    COUNT(*) FROM bookings
UNION ALL SELECT 'Chi tiết đơn',    COUNT(*) FROM booking_details
UNION ALL SELECT 'Lịch trình ngày', COUNT(*) FROM tour_itineraries
UNION ALL SELECT 'Hành khách',      COUNT(*) FROM booking_passengers
UNION ALL SELECT 'Thanh toán',      COUNT(*) FROM payments
UNION ALL SELECT 'Lịch sử đơn',     COUNT(*) FROM booking_status_history
UNION ALL SELECT 'Khuyến mãi',      COUNT(*) FROM promotions
UNION ALL SELECT 'Lượt dùng mã',    COUNT(*) FROM coupon_usages
UNION ALL SELECT 'Đánh giá',        COUNT(*) FROM reviews
UNION ALL SELECT 'Liên hệ',         COUNT(*) FROM contact_messages
UNION ALL SELECT 'Vé đổi mật khẩu', COUNT(*) FROM password_reset_tokens;

SELECT bd.id AS dong_chi_tiet_lech
FROM booking_details bd
LEFT JOIN (SELECT detail_id,
                  SUM(CASE WHEN passenger_type = 'ADULT' THEN 1 ELSE 0 END) AS nguoi_lon,
                  SUM(CASE WHEN passenger_type = 'CHILD' THEN 1 ELSE 0 END) AS tre_em
           FROM booking_passengers GROUP BY detail_id) p ON p.detail_id = bd.id
WHERE IFNULL(p.nguoi_lon, 0) <> bd.num_adults
   OR IFNULL(p.tre_em, 0)    <> bd.num_children;

SELECT name, destination FROM tours ORDER BY id LIMIT 3;

SELECT COUNT(*) AS tim_da_lat  FROM tours WHERE search_text LIKE '%da lat%';

SELECT COUNT(*) AS tim_phu_quoc FROM tours WHERE search_text LIKE '%phu quoc%';

SELECT id, total_seats, available_seats
FROM tour_departures
WHERE available_seats < total_seats
ORDER BY id;
