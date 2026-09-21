/* ===========================================================================
   03_seed_data.sql - Dữ liệu mẫu cho TourBookingDB

   Chạy sau 02_schema.sql. Script có thể chạy lại nhiều lần: phần đầu xoá sạch
   dữ liệu cũ và đặt lại bộ đếm IDENTITY.

   ---------------------------------------------------------------------------
   HAI ĐIỂM CẦN LƯU Ý
   ---------------------------------------------------------------------------
   1. Mọi chuỗi tiếng Việt đều có tiền tố N'...'. Thiếu chữ N, SQL Server sẽ
      hiểu literal là VARCHAR theo code page hiện hành và mọi dấu tiếng Việt
      biến thành dấu ? trước cả khi kịp lưu vào cột NVARCHAR.

   2. Ngày khởi hành và ngày đặt tính TƯƠNG ĐỐI so với GETDATE() chứ không ghi
      cứng. Nếu ghi cứng, chỉ vài tháng sau là toàn bộ đợt khởi hành nằm ở quá
      khứ và website không còn tour nào đặt được - hỏng buổi trình bày.

   Mật khẩu của cả bốn tài khoản mẫu đều là 123456 (đã băm bằng BCrypt).

   Cách chạy:
     sqlcmd -S 127.0.0.1,1433 -U tourapp -P 'Tour@2026#IUH' \
            -d TourBookingDB -C -i database/03_seed_data.sql
   =========================================================================== */

USE TourBookingDB;
GO

/* --- Xoá dữ liệu cũ, theo thứ tự ngược chiều khoá ngoại --- */
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
/* promotions phải xoá SAU bookings: từ khi đơn hàng có cột promotion_id thì
   bookings là bảng con của promotions, xoá ngược lại là lỗi khoá ngoại 547. */
DELETE FROM promotions;
DELETE FROM tour_departures;
DELETE FROM tour_images;
DELETE FROM tours;
DELETE FROM tour_categories;
DELETE FROM users;
GO

DBCC CHECKIDENT ('booking_passengers',     RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('coupon_usages',          RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('promotions',             RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('payments',               RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('booking_status_history', RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('reviews',                RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('contact_messages',       RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('password_reset_tokens',  RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('tour_itineraries',       RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('booking_details',  RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('bookings',         RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('tour_departures',  RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('tour_images',      RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('tours',            RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('tour_categories',  RESEED, 0) WITH NO_INFOMSGS;
DBCC CHECKIDENT ('users',            RESEED, 0) WITH NO_INFOMSGS;
GO

/* ===========================================================================
   1. users - 1 quản trị viên + 3 khách hàng
   Cột password là chuỗi băm BCrypt của "123456". Không bao giờ lưu mật khẩu thô.
   =========================================================================== */
SET IDENTITY_INSERT users ON;

INSERT INTO users (id, full_name, email, password, phone, address, role, enabled, created_at, updated_at)
VALUES
 (1, N'Quản trị hệ thống', N'admin@tourbooking.vn',
     N'$2a$10$XuHU4Pp6gFEL9A4GqPa2MO2pZwy9lxeFZYmmocoamnVUIayjOf0cG',
     N'0909000001', N'12 Nguyễn Văn Bảo, Gò Vấp, TP. Hồ Chí Minh', 'ADMIN',    1, SYSDATETIME(), SYSDATETIME()),
 (2, N'Nguyễn Văn An',     N'an.nguyen@gmail.com',
     N'$2a$10$XuHU4Pp6gFEL9A4GqPa2MO2pZwy9lxeFZYmmocoamnVUIayjOf0cG',
     N'0912345678', N'25 Lê Lợi, Quận 1, TP. Hồ Chí Minh',         'CUSTOMER', 1, SYSDATETIME(), SYSDATETIME()),
 (3, N'Trần Thị Bình',     N'binh.tran@gmail.com',
     N'$2a$10$XuHU4Pp6gFEL9A4GqPa2MO2pZwy9lxeFZYmmocoamnVUIayjOf0cG',
     N'0987654321', N'48 Trần Hưng Đạo, Hải Châu, Đà Nẵng',        'CUSTOMER', 1, SYSDATETIME(), SYSDATETIME()),
 (4, N'Lê Quốc Cường',     N'cuong.le@gmail.com',
     N'$2a$10$XuHU4Pp6gFEL9A4GqPa2MO2pZwy9lxeFZYmmocoamnVUIayjOf0cG',
     N'0938111222', N'77 Kim Mã, Ba Đình, Hà Nội',                 'CUSTOMER', 1, SYSDATETIME(), SYSDATETIME());

SET IDENTITY_INSERT users OFF;
GO

/* ===========================================================================
   2. tour_categories - 6 danh mục
   =========================================================================== */
SET IDENTITY_INSERT tour_categories ON;

INSERT INTO tour_categories (id, name, slug, description, image_url, active, created_at, updated_at)
VALUES
 (1, N'Du lịch biển đảo',           N'du-lich-bien-dao',
     N'Nghỉ dưỡng ven biển, lặn ngắm san hô và khám phá các hòn đảo.',
     N'/images/categories/du-lich-bien-dao.svg',      1, SYSDATETIME(), SYSDATETIME()),
 (2, N'Du lịch núi rừng',           N'du-lich-nui-rung',
     N'Chinh phục đỉnh cao, săn mây và trải nghiệm khí hậu vùng cao.',
     N'/images/categories/du-lich-nui-rung.svg',      1, SYSDATETIME(), SYSDATETIME()),
 (3, N'Du lịch văn hoá - tâm linh', N'du-lich-van-hoa-tam-linh',
     N'Hành hương, thăm di tích lịch sử và các công trình kiến trúc cổ.',
     N'/images/categories/du-lich-van-hoa-tam-linh.svg', 1, SYSDATETIME(), SYSDATETIME()),
 (4, N'Du lịch nước ngoài',         N'du-lich-nuoc-ngoai',
     N'Các hành trình khám phá châu Á và châu Âu.',
     N'/images/categories/du-lich-nuoc-ngoai.svg',    1, SYSDATETIME(), SYSDATETIME()),
 (5, N'Du lịch sinh thái',          N'du-lich-sinh-thai',
     N'Miệt vườn, rừng ngập mặn và các khu bảo tồn thiên nhiên.',
     N'/images/categories/du-lich-sinh-thai.svg',     1, SYSDATETIME(), SYSDATETIME()),
 (6, N'Du lịch nghỉ dưỡng cao cấp', N'du-lich-nghi-duong-cao-cap',
     N'Resort 4-5 sao, suối khoáng nóng và dịch vụ chăm sóc sức khoẻ.',
     N'/images/categories/du-lich-nghi-duong-cao-cap.svg', 1, SYSDATETIME(), SYSDATETIME());

SET IDENTITY_INSERT tour_categories OFF;
GO

/* ===========================================================================
   3. tours - 20 chương trình tour

   Cột search_text là bản đã bỏ dấu của tên + điểm đến + nơi khởi hành + mã tour.
   Ứng dụng tự dựng lại cột này mỗi lần lưu tour (xem Tour.buildSearchText());
   ở đây ghi sẵn để website tìm kiếm được ngay sau khi nạp dữ liệu mẫu.

   Hai cột description và itinerary được điền ở bước 4 ngay bên dưới.
   =========================================================================== */
SET IDENTITY_INSERT tours ON;

INSERT INTO tours (id, code, name, slug, short_description, departure_location, destination,
                   duration_days, duration_nights, base_price, transportation,
                   featured, active, view_count, search_text, category_id, created_at, updated_at)
VALUES
 (1, N'PQ-3N2D', N'Phú Quốc - Đảo Ngọc 3N2Đ', N'phu-quoc-dao-ngoc-3n2d',
     N'Câu cá, lặn ngắm san hô ở Hòn Thơm và ngắm hoàng hôn tại Sunset Sanato.',
     N'TP. Hồ Chí Minh', N'Phú Quốc', 3, 2, 5990000, N'Máy bay', 1, 1, 0,
     N'phu quoc - dao ngoc 3n2d phu quoc tp. ho chi minh pq-3n2d', 1, SYSDATETIME(), SYSDATETIME()),

 (2, N'NT-4N3D', N'Nha Trang - Vịnh Ngọc 4N3Đ', N'nha-trang-vinh-ngoc-4n3d',
     N'Tắm bùn khoáng, tham quan VinWonders và bốn đảo nổi tiếng của vịnh Nha Trang.',
     N'TP. Hồ Chí Minh', N'Nha Trang', 4, 3, 4590000, N'Máy bay', 1, 1, 0,
     N'nha trang - vinh ngoc 4n3d nha trang tp. ho chi minh nt-4n3d', 1, SYSDATETIME(), SYSDATETIME()),

 (3, N'CD-3N2D', N'Côn Đảo - Tâm Linh & Biển Xanh 3N2Đ', N'con-dao-tam-linh-bien-xanh-3n2d',
     N'Viếng nghĩa trang Hàng Dương, thăm nhà tù Côn Đảo và tắm biển Đầm Trầu.',
     N'TP. Hồ Chí Minh', N'Côn Đảo', 3, 2, 6490000, N'Máy bay', 0, 1, 0,
     N'con dao - tam linh & bien xanh 3n2d con dao tp. ho chi minh cd-3n2d', 1, SYSDATETIME(), SYSDATETIME()),

 (4, N'HL-3N2D', N'Hạ Long - Kỳ Quan Vịnh Biển 3N2Đ', N'ha-long-ky-quan-vinh-bien-3n2d',
     N'Du thuyền ngủ đêm trên vịnh, chèo kayak và khám phá hang Sửng Sốt.',
     N'Hà Nội', N'Hạ Long', 3, 2, 3890000, N'Xe du lịch', 1, 1, 0,
     N'ha long - ky quan vinh bien 3n2d ha long ha noi hl-3n2d', 1, SYSDATETIME(), SYSDATETIME()),

 (5, N'QN-4N3D', N'Quy Nhơn - Phú Yên 4N3Đ', N'quy-nhon-phu-yen-4n3d',
     N'Kỳ Co - Eo Gió, Gành Đá Đĩa và ghềnh Đá Dĩa Phú Yên.',
     N'TP. Hồ Chí Minh', N'Quy Nhơn', 4, 3, 5190000, N'Máy bay', 0, 1, 0,
     N'quy nhon - phu yen 4n3d quy nhon tp. ho chi minh qn-4n3d', 1, SYSDATETIME(), SYSDATETIME()),

 (6, N'SP-3N2D', N'Sa Pa - Săn Mây Fansipan 3N2Đ', N'sa-pa-san-may-fansipan-3n2d',
     N'Cáp treo lên đỉnh Fansipan, bản Cát Cát và chợ phiên vùng cao.',
     N'Hà Nội', N'Sa Pa', 3, 2, 3290000, N'Xe giường nằm', 1, 1, 0,
     N'sa pa - san may fansipan 3n2d sa pa ha noi sp-3n2d', 2, SYSDATETIME(), SYSDATETIME()),

 (7, N'HG-4N3D', N'Hà Giang - Cao Nguyên Đá 4N3Đ', N'ha-giang-cao-nguyen-da-4n3d',
     N'Đèo Mã Pí Lèng, sông Nho Quế và cột cờ Lũng Cú.',
     N'Hà Nội', N'Hà Giang', 4, 3, 4190000, N'Xe du lịch', 0, 1, 0,
     N'ha giang - cao nguyen da 4n3d ha giang ha noi hg-4n3d', 2, SYSDATETIME(), SYSDATETIME()),

 (8, N'DL-3N2D', N'Đà Lạt - Thành Phố Ngàn Hoa 3N2Đ', N'da-lat-thanh-pho-ngan-hoa-3n2d',
     N'Đồi chè Cầu Đất, thác Datanla và chợ đêm Đà Lạt.',
     N'TP. Hồ Chí Minh', N'Đà Lạt', 3, 2, 2890000, N'Xe du lịch', 1, 1, 0,
     N'da lat - thanh pho ngan hoa 3n2d da lat tp. ho chi minh dl-3n2d', 2, SYSDATETIME(), SYSDATETIME()),

 (9, N'MC-3N2D', N'Mộc Châu - Mùa Hoa 3N2Đ', N'moc-chau-mua-hoa-3n2d',
     N'Đồi chè trái tim, thác Dải Yếm và rừng thông bản Áng.',
     N'Hà Nội', N'Mộc Châu', 3, 2, 2690000, N'Xe du lịch', 0, 1, 0,
     N'moc chau - mua hoa 3n2d moc chau ha noi mc-3n2d', 2, SYSDATETIME(), SYSDATETIME()),

 (10, N'HUE-4N3D', N'Huế - Đà Nẵng - Hội An 4N3Đ', N'hue-da-nang-hoi-an-4n3d',
     N'Đại Nội, chùa Thiên Mụ, Bà Nà Hills và phố cổ Hội An về đêm.',
     N'TP. Hồ Chí Minh', N'Huế', 4, 3, 5490000, N'Máy bay', 1, 1, 0,
     N'hue - da nang - hoi an 4n3d hue tp. ho chi minh hue-4n3d', 3, SYSDATETIME(), SYSDATETIME()),

 (11, N'NB-2N1D', N'Ninh Bình - Tràng An - Bái Đính 2N1Đ', N'ninh-binh-trang-an-bai-dinh-2n1d',
     N'Du thuyền Tràng An, chùa Bái Đính và hang Múa.',
     N'Hà Nội', N'Ninh Bình', 2, 1, 1890000, N'Xe du lịch', 0, 1, 0,
     N'ninh binh - trang an - bai dinh 2n1d ninh binh ha noi nb-2n1d', 3, SYSDATETIME(), SYSDATETIME()),

 (12, N'TN-2N1D', N'Tây Ninh - Núi Bà Đen 2N1Đ', N'tay-ninh-nui-ba-den-2n1d',
     N'Cáp treo núi Bà Đen, toà thánh Cao Đài và hồ Dầu Tiếng.',
     N'TP. Hồ Chí Minh', N'Tây Ninh', 2, 1, 1290000, N'Xe du lịch', 0, 1, 0,
     N'tay ninh - nui ba den 2n1d tay ninh tp. ho chi minh tn-2n1d', 3, SYSDATETIME(), SYSDATETIME()),

 (13, N'YT-2N1D', N'Quảng Ninh - Yên Tử Hành Hương 2N1Đ', N'quang-ninh-yen-tu-hanh-huong-2n1d',
     N'Chùa Đồng Yên Tử, thiền viện Trúc Lâm và khu di tích nhà Trần.',
     N'Hà Nội', N'Quảng Ninh', 2, 1, 1690000, N'Xe du lịch', 0, 1, 0,
     N'quang ninh - yen tu hanh huong 2n1d quang ninh ha noi yt-2n1d', 3, SYSDATETIME(), SYSDATETIME()),

 (14, N'TL-5N4D', N'Thái Lan - Bangkok - Pattaya 5N4Đ', N'thai-lan-bangkok-pattaya-5n4d',
     N'Chùa Phật Vàng, chợ nổi Bốn Miền và show Alcazar.',
     N'TP. Hồ Chí Minh', N'Thái Lan', 5, 4, 9990000, N'Máy bay', 1, 1, 0,
     N'thai lan - bangkok - pattaya 5n4d thai lan tp. ho chi minh tl-5n4d', 4, SYSDATETIME(), SYSDATETIME()),

 (15, N'SG-4N3D', N'Singapore - Malaysia 4N3Đ', N'singapore-malaysia-4n3d',
     N'Gardens by the Bay, đảo Sentosa và cao nguyên Genting.',
     N'TP. Hồ Chí Minh', N'Singapore', 4, 3, 12900000, N'Máy bay', 0, 1, 0,
     N'singapore - malaysia 4n3d singapore tp. ho chi minh sg-4n3d', 4, SYSDATETIME(), SYSDATETIME()),

 (16, N'HQ-5N4D', N'Hàn Quốc - Seoul - Nami 5N4Đ', N'han-quoc-seoul-nami-5n4d',
     N'Đảo Nami, cung Gyeongbok và tháp Namsan.',
     N'Hà Nội', N'Hàn Quốc', 5, 4, 16900000, N'Máy bay', 1, 1, 0,
     N'han quoc - seoul - nami 5n4d han quoc ha noi hq-5n4d', 4, SYSDATETIME(), SYSDATETIME()),

 (17, N'NB-6N5D', N'Nhật Bản - Cung Đường Vàng 6N5Đ', N'nhat-ban-cung-duong-vang-6n5d',
     N'Tokyo - Phú Sĩ - Kyoto - Osaka mùa hoa anh đào.',
     N'TP. Hồ Chí Minh', N'Nhật Bản', 6, 5, 28900000, N'Máy bay', 0, 1, 0,
     N'nhat ban - cung duong vang 6n5d nhat ban tp. ho chi minh nb-6n5d', 4, SYSDATETIME(), SYSDATETIME()),

 (18, N'MT-3N2D', N'Miền Tây - Cần Thơ - Cà Mau 3N2Đ', N'mien-tay-can-tho-ca-mau-3n2d',
     N'Chợ nổi Cái Răng, rừng tràm Trà Sư và mũi Cà Mau.',
     N'TP. Hồ Chí Minh', N'Cần Thơ', 3, 2, 2390000, N'Xe du lịch', 0, 1, 0,
     N'mien tay - can tho - ca mau 3n2d can tho tp. ho chi minh mt-3n2d', 5, SYSDATETIME(), SYSDATETIME()),

 (19, N'CT-2N1D', N'Cát Tiên - Vườn Quốc Gia 2N1Đ', N'cat-tien-vuon-quoc-gia-2n1d',
     N'Đi bộ xuyên rừng, ngắm thú đêm và thăm đảo Tiên.',
     N'TP. Hồ Chí Minh', N'Đồng Nai', 2, 1, 1590000, N'Xe du lịch', 0, 1, 0,
     N'cat tien - vuon quoc gia 2n1d dong nai tp. ho chi minh ct-2n1d', 5, SYSDATETIME(), SYSDATETIME()),

 (20, N'MN-3N2D', N'Mũi Né - Resort Nghỉ Dưỡng 3N2Đ', N'mui-ne-resort-nghi-duong-3n2d',
     N'Resort 4 sao sát biển, đồi cát bay và làng chài Mũi Né.',
     N'TP. Hồ Chí Minh', N'Phan Thiết', 3, 2, 4290000, N'Xe du lịch', 1, 1, 0,
     N'mui ne - resort nghi duong 3n2d phan thiet tp. ho chi minh mn-3n2d', 6, SYSDATETIME(), SYSDATETIME());

SET IDENTITY_INSERT tours OFF;
GO

/* ===========================================================================
   3b. Ảnh đại diện (thumbnail) - ảnh THẬT lấy từ Wikimedia Commons, đi kèm mã
   nguồn tại static/images/tours/ (không phải uploads/tours/, thư mục đó là
   runtime và bị .gitignore). Đường dẫn bắt đầu bằng "/" để phân biệt với ảnh
   quản trị viên tải lên qua /admin/tours (đường dẫn tương đối, ví dụ
   "tours/abc123.jpg") - cùng quy ước đã dùng cho TourCategory.imageUrl.
   Xem static/images/tours/NGUON_ANH.md để biết tác giả và giấy phép từng ảnh.
   =========================================================================== */
UPDATE tours SET thumbnail = CASE id
    WHEN 1  THEN N'/images/tours/tour01_phu-quoc.jpg'
    WHEN 2  THEN N'/images/tours/tour02_nha-trang.jpg'
    WHEN 3  THEN N'/images/tours/tour03_con-dao.jpg'
    WHEN 4  THEN N'/images/tours/tour04_ha-long.jpg'
    WHEN 5  THEN N'/images/tours/tour05_quy-nhon.jpg'
    WHEN 6  THEN N'/images/tours/tour06_sa-pa.jpg'
    WHEN 7  THEN N'/images/tours/tour07_ha-giang.jpg'
    WHEN 8  THEN N'/images/tours/tour08_da-lat.jpg'
    WHEN 9  THEN N'/images/tours/tour09_moc-chau.jpg'
    WHEN 10 THEN N'/images/tours/tour10_hoi-an.jpg'
    WHEN 11 THEN N'/images/tours/tour11_ninh-binh.jpg'
    WHEN 12 THEN N'/images/tours/tour12_tay-ninh.jpg'
    WHEN 13 THEN N'/images/tours/tour13_quang-ninh.jpg'
    WHEN 14 THEN N'/images/tours/tour14_thai-lan.jpg'
    WHEN 15 THEN N'/images/tours/tour15_singapore.jpg'
    WHEN 16 THEN N'/images/tours/tour16_han-quoc.jpg'
    WHEN 17 THEN N'/images/tours/tour17_nhat-ban.jpg'
    WHEN 18 THEN N'/images/tours/tour18_mien-tay.jpg'
    WHEN 19 THEN N'/images/tours/tour19_cat-tien.jpg'
    WHEN 20 THEN N'/images/tours/tour20_mui-ne.jpg'
END;
GO

/* ===========================================================================
   4. Mô tả chi tiết và lịch trình

   Sinh theo công thức thay vì gõ tay 20 đoạn văn: nội dung của chúng chỉ khác
   nhau ở tên điểm đến và số ngày. Dùng hàm dựng sẵn của SQL Server (STRING_AGG,
   CONCAT...) - đề bài chỉ cấm TẠO Function/Procedure/Trigger trong CSDL.
   =========================================================================== */
UPDATE tours
SET description =
        short_description + CHAR(13) + CHAR(10) + CHAR(13) + CHAR(10) +
        N'Hành trình ' + CAST(duration_days AS NVARCHAR(2)) + N' ngày ' +
        CAST(duration_nights AS NVARCHAR(2)) + N' đêm khởi hành từ ' + departure_location +
        N', di chuyển bằng ' + LOWER(transportation) + N'.' + CHAR(13) + CHAR(10) +
        N'Giá tour đã bao gồm vé tham quan, khách sạn tiêu chuẩn, các bữa ăn theo ' +
        N'chương trình, hướng dẫn viên suốt tuyến và bảo hiểm du lịch. Giá chưa bao ' +
        N'gồm chi phí cá nhân, đồ uống và tiền tip cho hướng dẫn viên.';
GO

/* Lưu ý cú pháp: câu lệnh ghép chuỗi phải nằm ở lớp truy vấn con "cac_dong",
   rồi STRING_AGG mới gộp trên đúng MỘT cột của lớp đó. Nếu viết CASE thẳng vào
   trong STRING_AGG, SQL Server báo lỗi 8124 vì biểu thức gộp khi đó vừa tham
   chiếu cột của bảng ngoài (t.destination) vừa tham chiếu cột của bảng trong (d.n). */
UPDATE t
SET itinerary = x.noi_dung
FROM tours t
CROSS APPLY (
    SELECT STRING_AGG(cac_dong.dong, CHAR(13) + CHAR(10))
               WITHIN GROUP (ORDER BY cac_dong.n) AS noi_dung
    FROM (
        SELECT d.n,
               CAST(N'Ngày ' + CAST(d.n AS NVARCHAR(2)) + N': ' +
                    CASE
                        WHEN d.n = 1 THEN
                            t.departure_location + N' - ' + t.destination +
                            N'. Khởi hành, nhận phòng và tham quan các điểm gần trung tâm.'
                        WHEN d.n = t.duration_days THEN
                            t.destination + N' - ' + t.departure_location +
                            N'. Mua sắm đặc sản, trả phòng và về điểm xuất phát.'
                        ELSE
                            N'Tham quan các điểm nổi bật của ' + t.destination +
                            N', tự do trải nghiệm ẩm thực địa phương buổi tối.'
                    END AS NVARCHAR(MAX)) AS dong
        FROM (VALUES (1), (2), (3), (4), (5), (6)) AS d(n)
        WHERE d.n <= t.duration_days
    ) AS cac_dong
) AS x;
GO

/* ===========================================================================
   5. tour_departures - mỗi tour 3 đợt (tổng 60 đợt)

   Ngày khởi hành cách hôm nay 14 / 30 / 50 ngày nên dữ liệu mẫu luôn còn tour
   để đặt, chạy script lúc nào cũng đúng. Đợt sau nhích giá 5% cho giống mùa cao
   điểm; giá trẻ em bằng 70% giá người lớn.

   Mã đợt được tính là (tour_id - 1) * 3 + thu_tu để các đơn hàng mẫu ở bước 6
   trỏ tới đúng đợt mà không cần truy vấn ngược.
   =========================================================================== */
SET IDENTITY_INSERT tour_departures ON;

INSERT INTO tour_departures (id, tour_id, departure_date, return_date, total_seats,
                             available_seats, price_adult, price_child, active, version,
                             created_at, updated_at)
SELECT (t.id - 1) * 3 + d.thu_tu,
       t.id,
       DATEADD(DAY, d.cach_ngay, CAST(GETDATE() AS DATE)),
       DATEADD(DAY, d.cach_ngay + t.duration_days - 1, CAST(GETDATE() AS DATE)),
       d.so_cho,
       d.so_cho,                                   -- ban đầu còn nguyên số chỗ
       ROUND(t.base_price * d.he_so_gia, 2),
       ROUND(t.base_price * d.he_so_gia * 0.7, 2),
       1,
       0,                                          -- version khởi tạo
       SYSDATETIME(), SYSDATETIME()
FROM tours t
CROSS JOIN (VALUES (1, 14, 30, 1.00),
                   (2, 30, 25, 1.05),
                   (3, 50, 20, 1.10)) AS d(thu_tu, cach_ngay, so_cho, he_so_gia);

SET IDENTITY_INSERT tour_departures OFF;
GO

/* ===========================================================================
   6. bookings - 5 đơn mẫu đủ bốn trạng thái

   Mã đơn được ghép từ ngày đặt để trông giống mã do ứng dụng sinh ra
   (CodeGenerator.bookingCode(): TB + yyyyMMdd + 6 ký tự ngẫu nhiên).
   Bốn cột customer_* chép từ bảng users - đúng như ứng dụng làm lúc thanh toán.
   =========================================================================== */
SET IDENTITY_INSERT bookings ON;

INSERT INTO bookings (id, code, user_id, booking_date, customer_name, customer_email,
                      customer_phone, customer_address, total_amount, promotion_id,
                      discount_amount, status, note,
                      payment_method, created_at, updated_at)
SELECT b.id,
       N'TB' + CONVERT(NVARCHAR(8), DATEADD(DAY, -b.cach_ngay, SYSDATETIME()), 112) + b.hau_to,
       u.id,
       DATEADD(DAY, -b.cach_ngay, SYSDATETIME()),
       u.full_name, u.email, u.phone, u.address,
       0,                                          -- tổng tiền tính lại ở bước 8
       NULL, 0,                                    -- mã giảm giá gán ở bước 15
       b.trang_thai,
       b.ghi_chu,
       N'Chuyển khoản ngân hàng',
       SYSDATETIME(), SYSDATETIME()
FROM (VALUES
        (1, 2, 12, 'CONFIRMED', N'HK4M2P', CAST(NULL AS NVARCHAR(500))),
        (2, 3,  5, 'PENDING',   N'QT7B9X', NULL),
        (3, 4, 40, 'COMPLETED', N'LM3D8R', NULL),
        (4, 2,  2, 'PENDING',   N'VP6K2N', NULL),
        (5, 3, 20, 'CANCELLED', N'ZC5H7J', N'Khách báo bận, xin huỷ đơn.')
     ) AS b(id, user_id, cach_ngay, trang_thai, hau_to, ghi_chu)
JOIN users u ON u.id = b.user_id;

SET IDENTITY_INSERT bookings OFF;
GO

/* ===========================================================================
   7. booking_details - 6 dòng chi tiết (đơn số 4 đặt cùng lúc 2 tour)

   Tên tour và đơn giá lấy từ đợt khởi hành ngay lúc này rồi CHÉP vào dòng chi
   tiết. Về sau quản trị viên có sửa giá đợt đó thì hoá đơn cũ vẫn giữ số cũ.
   =========================================================================== */
SET IDENTITY_INSERT booking_details ON;

INSERT INTO booking_details (id, booking_id, departure_id, tour_name_snapshot,
                             num_adults, num_children, unit_price_adult, unit_price_child, subtotal)
SELECT x.id, x.booking_id, x.departure_id, t.name,
       x.nguoi_lon, x.tre_em,
       d.price_adult, d.price_child,
       d.price_adult * x.nguoi_lon + d.price_child * x.tre_em
FROM (VALUES (1, 1,  1, 2, 1),    -- Phú Quốc, đợt 1
             (2, 2,  4, 2, 0),    -- Nha Trang, đợt 1
             (3, 3, 16, 4, 2),    -- Sa Pa, đợt 1
             (4, 4, 25, 2, 2),    -- Mộc Châu, đợt 1
             (5, 4, 40, 1, 0),    -- Thái Lan, đợt 1
             (6, 5, 10, 3, 0)     -- Hạ Long, đợt 1 (đơn này đã huỷ)
     ) AS x(id, booking_id, departure_id, nguoi_lon, tre_em)
JOIN tour_departures d ON d.id = x.departure_id
JOIN tours t           ON t.id = d.tour_id;

SET IDENTITY_INSERT booking_details OFF;
GO

/* ===========================================================================
   8. Tính lại tổng tiền của đơn
   =========================================================================== */
UPDATE b
SET b.total_amount = x.tong
FROM bookings b
JOIN (SELECT booking_id, SUM(subtotal) AS tong
      FROM booking_details
      GROUP BY booking_id) AS x ON x.booking_id = b.id;
GO

/* ===========================================================================
   9. Trừ số chỗ đã bán

   Đơn đã huỷ KHÔNG bị trừ chỗ - huỷ đơn thì chỗ phải được trả lại. Giữ đúng bất
   biến này ngay từ dữ liệu mẫu để các con số trên màn hình quản trị luôn khớp.
   =========================================================================== */
UPDATE d
SET d.available_seats = d.total_seats - x.so_khach
FROM tour_departures d
JOIN (SELECT bd.departure_id, SUM(bd.num_adults + bd.num_children) AS so_khach
      FROM booking_details bd
      JOIN bookings b ON b.id = bd.booking_id
      WHERE b.status <> 'CANCELLED'
      GROUP BY bd.departure_id) AS x ON x.departure_id = d.id;
GO

/* ===========================================================================
   10. tour_itineraries - lịch trình từng ngày cho 3 tour đầu

   Số ngày lấy thẳng từ tours.duration_days nên không bao giờ lệch: tour 3 ngày
   sinh đúng 3 dòng. Danh sách (1)..(5) chỉ là nguồn số ngày, điều kiện
   n.day_no <= t.duration_days cắt phần thừa.
   =========================================================================== */
INSERT INTO tour_itineraries (tour_id, day_no, title, description, meals, accommodation)
SELECT t.id,
       n.day_no,
       CASE
           WHEN n.day_no = 1 THEN N'Khởi hành: ' + t.departure_location + N' - ' + t.destination
           WHEN n.day_no = t.duration_days THEN N'Mua sắm và tiễn khách về ' + t.departure_location
           ELSE N'Khám phá ' + t.destination
       END,
       CASE
           WHEN n.day_no = 1 THEN N'Xe/máy bay đón khách, làm thủ tục nhận phòng, ăn tối và tự do nghỉ ngơi.'
           WHEN n.day_no = t.duration_days THEN N'Trả phòng, mua đặc sản làm quà, xe đưa ra sân bay hoặc bến xe.'
           ELSE N'Tham quan các điểm nổi bật theo chương trình, buổi chiều tự do tắm biển hoặc nghỉ tại resort.'
       END,
       CASE
           WHEN n.day_no = 1 THEN N'Trưa, Tối'
           WHEN n.day_no = t.duration_days THEN N'Sáng'
           ELSE N'Sáng, Trưa, Tối'
       END,
       CASE
           WHEN n.day_no = t.duration_days THEN N'Kết thúc chương trình'
           ELSE N'Khách sạn 4 sao tại ' + t.destination
       END
FROM tours t
JOIN (VALUES (1), (2), (3), (4), (5)) AS n(day_no) ON n.day_no <= t.duration_days
WHERE t.id <= 3;
GO

/* ===========================================================================
   11. booking_passengers - 19 hành khách của 6 dòng chi tiết

   Số người từng loại PHẢI khớp num_adults / num_children của dòng chi tiết -
   đây chính là bất biến mà tầng service kiểm khi lưu. Cột tuoi chỉ dùng để tính
   ngày sinh tương đối, không có trong bảng.
   =========================================================================== */
INSERT INTO booking_passengers (detail_id, full_name, passenger_type, gender,
                                birth_date, id_number, phone, single_room, note)
SELECT p.detail_id, p.ho_ten, p.loai, p.gioi_tinh,
       CASE WHEN p.tuoi IS NULL THEN NULL
            ELSE DATEADD(YEAR, -p.tuoi, CAST(SYSDATETIME() AS DATE)) END,
       p.so_giay_to, p.dien_thoai, p.phong_don, p.ghi_chu
FROM (VALUES
    -- Dòng 1: 2 người lớn + 1 trẻ em
    (1, N'Nguyễn Văn An',      'ADULT', 'MALE',   34, N'079201001234', N'0901234567', 0, CAST(NULL AS NVARCHAR(255))),
    (1, N'Trần Thị Bích Ngọc', 'ADULT', 'FEMALE', 32, N'079202001235', NULL,          0, NULL),
    (1, N'Nguyễn Bảo Anh',     'CHILD', 'FEMALE',  8, NULL,            NULL,          0, N'Bé 8 tuổi, cần ghế riêng trên xe'),
    -- Dòng 2: 2 người lớn
    (2, N'Trần Văn Bình',      'ADULT', 'MALE',   41, N'079203001236', N'0912345678', 0, NULL),
    (2, N'Lê Thị Hồng',        'ADULT', 'FEMALE', 39, N'079204001237', NULL,          0, NULL),
    -- Dòng 3: 4 người lớn + 2 trẻ em
    (3, N'Lê Minh Cường',      'ADULT', 'MALE',   45, N'079205001238', N'0923456789', 0, NULL),
    (3, N'Phạm Thị Mai',       'ADULT', 'FEMALE', 43, N'079206001239', NULL,          0, NULL),
    (3, N'Lê Minh Khang',      'ADULT', 'MALE',   28, N'079207001240', NULL,          1, N'Yêu cầu phòng đơn'),
    (3, N'Đặng Thị Kiều Ửng',  'ADULT', 'FEMALE', 26, N'079208001241', NULL,          0, N'Ăn chay trường'),
    (3, N'Lê Bảo Ngọc',        'CHILD', 'FEMALE', 10, NULL,            NULL,          0, NULL),
    (3, N'Lê Gia Huy',         'CHILD', 'MALE',    6, NULL,            NULL,          0, N'Dị ứng hải sản'),
    -- Dòng 4: 2 người lớn + 2 trẻ em
    (4, N'Nguyễn Văn An',      'ADULT', 'MALE',   34, N'079201001234', N'0901234567', 0, NULL),
    (4, N'Trần Thị Bích Ngọc', 'ADULT', 'FEMALE', 32, N'079202001235', NULL,          0, NULL),
    (4, N'Nguyễn Bảo Anh',     'CHILD', 'FEMALE',  8, NULL,            NULL,          0, NULL),
    (4, N'Nguyễn Minh Quân',   'CHILD', 'MALE',    5, NULL,            NULL,          0, NULL),
    -- Dòng 5: 1 người lớn
    (5, N'Nguyễn Văn An',      'ADULT', 'MALE',   34, N'079201001234', N'0901234567', 1, N'Đi công tác kết hợp, ở phòng đơn'),
    -- Dòng 6: 3 người lớn (đơn này đã huỷ)
    (6, N'Trần Văn Bình',      'ADULT', 'MALE',   41, N'079203001236', N'0912345678', 0, NULL),
    (6, N'Lê Thị Hồng',        'ADULT', 'FEMALE', 39, N'079204001237', NULL,          0, NULL),
    (6, N'Trần Quốc Toản',     'ADULT', 'MALE',   37, N'079209001242', NULL,          0, NULL)
) AS p(detail_id, ho_ten, loai, gioi_tinh, tuoi, so_giay_to, dien_thoai, phong_don, ghi_chu);
GO

/* ===========================================================================
   12. payments - các lần thanh toán

   Đơn 1 đặt cọc 50% rồi còn nợ; đơn 3 đã trả đủ làm hai lần; đơn 2 mới tạo yêu
   cầu thanh toán qua ví; đơn 4 chưa trả đồng nào; đơn 5 đã huỷ nên tiền được
   hoàn lại.

   Chú ý: hai dòng PENDING đều để trống txn_ref. Nếu ràng buộc duy nhất trên cột
   đó là UNIQUE thường thì dòng thứ hai đã bị từ chối (SQL Server chỉ cho một
   NULL) - đó là lý do schema dùng chỉ mục duy nhất CÓ LỌC.
   =========================================================================== */
INSERT INTO payments (booking_id, amount, method, status, txn_ref, paid_at, note,
                      created_at, updated_at)
SELECT b.id,
       CASE WHEN x.ty_le IS NULL THEN b.total_amount
            ELSE ROUND(b.total_amount * x.ty_le, 0) END,
       x.hinh_thuc, x.trang_thai, x.ma_gd,
       CASE WHEN x.trang_thai IN ('PAID', 'REFUNDED')
            THEN DATEADD(HOUR, x.sau_gio, b.booking_date) END,
       x.ghi_chu,
       SYSDATETIME(), SYSDATETIME()
FROM (VALUES
    (1, 0.5,  'BANK_TRANSFER', 'PAID',     N'VCB2026080100117', 2,  CAST(N'Đặt cọc 50% khi giữ chỗ' AS NVARCHAR(255))),
    (1, 0.5,  'BANK_TRANSFER', 'PENDING',  NULL,                0,  N'Còn lại, thu trước ngày khởi hành'),
    (2, NULL, 'MOMO',          'PENDING',  NULL,                0,  N'Khách chọn thanh toán qua ví MoMo'),
    (3, 0.5,  'BANK_TRANSFER', 'PAID',     N'VCB2026070500342', 3,  N'Đặt cọc 50%'),
    (3, 0.5,  'CASH',          'PAID',     N'PT-2026-0451',     72, N'Trả nốt tại văn phòng'),
    (5, NULL, 'BANK_TRANSFER', 'REFUNDED', N'VCB2026071200988', 5,  N'Đã hoàn tiền sau khi khách xin huỷ')
) AS x(booking_id, ty_le, hinh_thuc, trang_thai, ma_gd, sau_gio, ghi_chu)
JOIN bookings b ON b.id = x.booking_id;
GO

/* ===========================================================================
   13. booking_status_history - nhật ký đổi trạng thái

   Dòng đầu của mỗi đơn có from_status = NULL: lúc vừa tạo thì chưa có trạng thái
   cũ nào. Các dòng sau do quản trị viên (tài khoản id = 1) thao tác.
   =========================================================================== */
INSERT INTO booking_status_history (booking_id, from_status, to_status,
                                    changed_by_id, reason, changed_at)
SELECT b.id, h.tu, h.den, h.nguoi, h.ly_do, DATEADD(HOUR, h.sau_gio, b.booking_date)
FROM (VALUES
    (1, CAST(NULL AS NVARCHAR(20)), 'PENDING',   CAST(NULL AS BIGINT), CAST(N'Khách đặt tour trên website' AS NVARCHAR(255)), 0),
    (1, 'PENDING',   'CONFIRMED', 1,    N'Đã nhận tiền cọc, xác nhận chỗ', 24),
    (2, NULL,        'PENDING',   NULL, N'Khách đặt tour trên website', 0),
    (3, NULL,        'PENDING',   NULL, N'Khách đặt tour trên website', 0),
    (3, 'PENDING',   'CONFIRMED', 1,    N'Đã nhận tiền cọc, xác nhận chỗ', 20),
    (3, 'CONFIRMED', 'COMPLETED', 1,    N'Đoàn đã kết thúc chương trình', 96),
    (4, NULL,        'PENDING',   NULL, N'Khách đặt tour trên website', 0),
    (5, NULL,        'PENDING',   NULL, N'Khách đặt tour trên website', 0),
    (5, 'PENDING',   'CANCELLED', 1,    N'Khách báo bận, xin huỷ đơn - đã hoàn tiền', 30)
) AS h(booking_id, tu, den, nguoi, ly_do, sau_gio)
JOIN bookings b ON b.id = h.booking_id;
GO

/* ===========================================================================
   14. promotions - 4 mã giảm giá

   Mã cuối cùng đã hết hạn dù vẫn còn bật: giữ lại để thấy Promotion.isRunning()
   loại nó ra đúng như mong đợi, chứ không phải cứ active = 1 là dùng được.
   =========================================================================== */
SET IDENTITY_INSERT promotions ON;

INSERT INTO promotions (id, code, name, description, discount_type, discount_value,
                        max_discount, min_order_amount, usage_limit, usage_limit_per_user,
                        used_count, start_at, end_at, active, created_at, updated_at)
VALUES
 (1, N'HELLO2026', N'Chào hè 2026',
     N'Giảm 10% cho đơn từ 5 triệu, tối đa 500.000đ. Mỗi tài khoản dùng một lần.',
     N'PERCENT', 10, 500000, 5000000, 100, 1, 1,
     DATEADD(DAY, -30, SYSDATETIME()), DATEADD(DAY, 60, SYSDATETIME()), 1,
     SYSDATETIME(), SYSDATETIME()),
 (2, N'SUMMER500', N'Giảm thẳng 500K',
     N'Giảm 500.000đ cho đơn từ 10 triệu, không giới hạn số lượt.',
     N'AMOUNT', 500000, NULL, 10000000, NULL, 1, 1,
     DATEADD(DAY, -15, SYSDATETIME()), DATEADD(DAY, 45, SYSDATETIME()), 1,
     SYSDATETIME(), SYSDATETIME()),
 (3, N'VIP20', N'Ưu đãi khách thân thiết',
     N'Giảm 20% tối đa 2 triệu, chỉ phát cho 20 lượt đầu tiên.',
     N'PERCENT', 20, 2000000, 15000000, 20, 1, 0,
     DATEADD(DAY, -7, SYSDATETIME()), DATEADD(DAY, 23, SYSDATETIME()), 1,
     SYSDATETIME(), SYSDATETIME()),
 (4, N'TET2026', N'Ưu đãi Tết 2026 (đã hết hạn)',
     N'Chương trình đã kết thúc - giữ lại để đối chiếu số liệu.',
     N'PERCENT', 15, 1000000, 3000000, 50, 1, 0,
     DATEADD(DAY, -120, SYSDATETIME()), DATEADD(DAY, -60, SYSDATETIME()), 1,
     SYSDATETIME(), SYSDATETIME());

SET IDENTITY_INSERT promotions OFF;
GO

/* ===========================================================================
   15. coupon_usages - hai lượt dùng mã đã phát sinh

   discount_amount là bản sao số tiền đã giảm lúc đặt, tính theo đúng công thức
   của Promotion.calculateDiscount (phần trăm thì chặn trần max_discount).
   =========================================================================== */
INSERT INTO coupon_usages (promotion_id, user_id, booking_id, discount_amount, used_at)
SELECT p.id, b.user_id, b.id,
       CASE WHEN p.discount_type = N'PERCENT'
            THEN CASE WHEN ROUND(b.total_amount * p.discount_value / 100, 0) > p.max_discount
                      THEN p.max_discount
                      ELSE ROUND(b.total_amount * p.discount_value / 100, 0) END
            ELSE p.discount_value END,
       b.booking_date
FROM (VALUES (1, N'HELLO2026'), (3, N'SUMMER500')) AS x(booking_id, ma)
JOIN bookings   b ON b.id   = x.booking_id
JOIN promotions p ON p.code = x.ma;
GO

/* Ghi mã và số tiền giảm ngược lại vào đơn, rồi TRỪ vào tổng tiền.
   Thứ tự bắt buộc: bước 8 đã đặt total_amount = tiền hàng, bước 15 tính số tiền
   giảm dựa trên đúng con số đó; tới đây mới trừ. Đảo thứ tự là tính giảm giá
   trên một con số đã bị giảm rồi. */
UPDATE b
SET b.promotion_id    = cu.promotion_id,
    b.discount_amount = cu.discount_amount,
    b.total_amount    = b.total_amount - cu.discount_amount
FROM bookings b
JOIN coupon_usages cu ON cu.booking_id = b.id;
GO

/* Số lượt đã dùng của mỗi mã phải khớp số dòng trong coupon_usages - đây là con
   số mà PromotionService cộng dồn khi đặt tour thật. */
UPDATE p
SET p.used_count = x.so_luot
FROM promotions p
JOIN (SELECT promotion_id, COUNT(*) AS so_luot
      FROM coupon_usages GROUP BY promotion_id) AS x ON x.promotion_id = p.id;
GO

/* ===========================================================================
   16. reviews - 5 đánh giá

   Dòng đầu là đánh giá "đã xác thực": có booking_id trỏ tới đơn số 3 đang ở
   trạng thái COMPLETED, đúng quy tắc mà tầng service áp dụng cho đánh giá gửi từ
   website. Bốn dòng còn lại để booking_id NULL - đây là dữ liệu minh hoạ do quản
   trị viên nhập, cột này cho phép NULL chính vì trường hợp đó.
   =========================================================================== */
INSERT INTO reviews (tour_id, user_id, booking_id, rating, title, content, approved,
                     admin_reply, replied_at, created_at, updated_at)
SELECT d.tour_id, r.user_id, r.booking_id, r.diem, r.tieu_de, r.noi_dung, r.duyet,
       r.tra_loi,
       CASE WHEN r.tra_loi IS NULL THEN NULL ELSE SYSDATETIME() END,
       DATEADD(DAY, -r.cach_ngay, SYSDATETIME()), SYSDATETIME()
FROM (VALUES
    (CAST(NULL AS BIGINT), 3, 4, 5, CAST(N'Chuyến đi trọn vẹn' AS NVARCHAR(200)),
     N'Hướng dẫn viên nhiệt tình, lịch trình hợp lý, khách sạn sạch sẽ. Gia đình tôi rất hài lòng.',
     1, CAST(N'Cảm ơn anh Cường đã tin tưởng, hẹn gặp lại gia đình mình!' AS NVARCHAR(1000)), 3),
    (1,  NULL, 2, 5, N'Biển đẹp, đồ ăn ngon',
     N'Hòn Thơm nước trong vắt, buổi chiều ngắm hoàng hôn ở Sunset Sanato rất đáng tiền.',
     1, NULL, 12),
    (1,  NULL, 3, 4, N'Đáng đi nhưng hơi gấp',
     N'Lịch trình ngày thứ hai khá dày, nên bớt một điểm để có thời gian nghỉ.',
     1, NULL, 20),
    (4,  NULL, 3, 4, N'Nha Trang vui',
     N'Đảo đẹp, hải sản tươi. Xe đưa đón đúng giờ, tài xế thân thiện.',
     1, NULL, 25),
    (10, NULL, 2, 3, N'Ổn nhưng khách sạn cũ',
     N'Chương trình tham quan tốt, riêng phòng nghỉ hơi xuống cấp so với ảnh quảng cáo.',
     0, NULL, 1)
) AS r(tour_id_truc_tiep, booking_id, user_id, diem, tieu_de, noi_dung, duyet, tra_loi, cach_ngay)
CROSS APPLY (
    /* Đánh giá có đơn thì lấy đúng tour của đơn đó; không thì dùng tour chỉ định sẵn. */
    SELECT COALESCE(r.tour_id_truc_tiep,
                    (SELECT TOP 1 dep.tour_id
                     FROM booking_details bd
                     JOIN tour_departures dep ON dep.id = bd.departure_id
                     WHERE bd.booking_id = r.booking_id)) AS tour_id
) AS d;
GO

/* ===========================================================================
   17. contact_messages - 4 liên hệ gửi từ trang công khai

   Một thư gắn với tour cụ thể (khách bấm "Tư vấn tour này"), một thư đã xử lý
   xong bởi quản trị viên, hai thư còn mới để thấy huy hiệu đếm trên khu quản trị.
   =========================================================================== */
INSERT INTO contact_messages (full_name, email, phone, subject, content, status,
                              tour_id, handled_by_id, reply_note, handled_at,
                              created_at, updated_at)
SELECT c.ho_ten, c.email, c.dien_thoai, c.tieu_de, c.noi_dung, c.trang_thai,
       c.tour_id, c.nguoi_xu_ly, c.tra_loi,
       CASE WHEN c.nguoi_xu_ly IS NULL THEN NULL
            ELSE DATEADD(DAY, -c.cach_ngay + 1, SYSDATETIME()) END,
       DATEADD(DAY, -c.cach_ngay, SYSDATETIME()), SYSDATETIME()
FROM (VALUES
    (N'Phạm Thu Hà', N'thuha.pham@gmail.com', N'0934567890',
     N'Hỏi lịch khởi hành tháng sau',
     N'Cho mình hỏi tour Phú Quốc còn chỗ ngày 20 tháng sau không, đoàn mình 6 người lớn.',
     N'NEW', CAST(1 AS BIGINT), CAST(NULL AS BIGINT), CAST(NULL AS NVARCHAR(1000)), 1),
    (N'Võ Thanh Tùng', N'tung.vo@company.vn', N'0945678901',
     N'Đặt tour cho công ty 30 người',
     N'Công ty mình muốn tổ chức team building 30 người trong 3 ngày, nhờ bên bạn báo giá giúp.',
     N'NEW', NULL, NULL, NULL, 2),
    (N'Ngô Thị Lan', N'lan.ngo@gmail.com', NULL,
     N'Đổi ngày khởi hành',
     N'Mình đã đặt tour nhưng có việc đột xuất, muốn dời sang tuần sau có được không?',
     N'IN_PROGRESS', NULL, 1, NULL, 3),
    (N'Đỗ Minh Khoa', N'khoa.do@gmail.com', N'0956789012',
     N'Góp ý về website',
     N'Phần tìm kiếm gõ không dấu vẫn ra đúng kết quả, rất tiện. Cảm ơn nhóm phát triển.',
     N'RESOLVED', NULL, 1, N'Đã cảm ơn khách qua email.', 8)
) AS c(ho_ten, email, dien_thoai, tieu_de, noi_dung, trang_thai, tour_id,
       nguoi_xu_ly, tra_loi, cach_ngay);
GO

/* ===========================================================================
   18. password_reset_tokens - 2 vé đã hết giá trị

   CỐ Ý không seed vé nào còn dùng được: một vé đặt lại mật khẩu còn hiệu lực nằm
   trong file dữ liệu mẫu là một cái chìa khoá bỏ quên trước cửa. Hai dòng dưới
   đây chỉ để thấy hai nhánh "đã dùng" và "hết hạn".
   =========================================================================== */
INSERT INTO password_reset_tokens (user_id, token, expires_at, used_at, created_at, updated_at)
VALUES
 (2, N'seed-token-da-su-dung-0000000000000001',
     DATEADD(MINUTE, -30, DATEADD(DAY, -5, SYSDATETIME())),
     DATEADD(MINUTE, -50, DATEADD(DAY, -5, SYSDATETIME())),
     DATEADD(DAY, -5, SYSDATETIME()), SYSDATETIME()),
 (3, N'seed-token-het-han-000000000000000002',
     DATEADD(DAY, -2, SYSDATETIME()), NULL,
     DATEADD(DAY, -2, SYSDATETIME()), SYSDATETIME());
GO

/* ===========================================================================
   19. Kiểm chứng
   =========================================================================== */
SELECT N'Người dùng'      AS bang, COUNT(*) AS so_dong FROM users
UNION ALL SELECT N'Danh mục',        COUNT(*) FROM tour_categories
UNION ALL SELECT N'Tour',            COUNT(*) FROM tours
UNION ALL SELECT N'Đợt khởi hành',   COUNT(*) FROM tour_departures
UNION ALL SELECT N'Đơn đặt tour',    COUNT(*) FROM bookings
UNION ALL SELECT N'Chi tiết đơn',    COUNT(*) FROM booking_details
UNION ALL SELECT N'Lịch trình ngày', COUNT(*) FROM tour_itineraries
UNION ALL SELECT N'Hành khách',      COUNT(*) FROM booking_passengers
UNION ALL SELECT N'Thanh toán',      COUNT(*) FROM payments
UNION ALL SELECT N'Lịch sử đơn',     COUNT(*) FROM booking_status_history
UNION ALL SELECT N'Khuyến mãi',      COUNT(*) FROM promotions
UNION ALL SELECT N'Lượt dùng mã',    COUNT(*) FROM coupon_usages
UNION ALL SELECT N'Đánh giá',        COUNT(*) FROM reviews
UNION ALL SELECT N'Liên hệ',         COUNT(*) FROM contact_messages
UNION ALL SELECT N'Vé đổi mật khẩu', COUNT(*) FROM password_reset_tokens;

/* Bất biến quan trọng nhất của bảng hành khách: đếm theo loại phải khớp đúng
   num_adults / num_children của dòng chi tiết. Câu này PHẢI trả về 0 dòng. */
SELECT bd.id AS dong_chi_tiet_lech
FROM booking_details bd
LEFT JOIN (SELECT detail_id,
                  SUM(CASE WHEN passenger_type = 'ADULT' THEN 1 ELSE 0 END) AS nguoi_lon,
                  SUM(CASE WHEN passenger_type = 'CHILD' THEN 1 ELSE 0 END) AS tre_em
           FROM booking_passengers GROUP BY detail_id) p ON p.detail_id = bd.id
WHERE ISNULL(p.nguoi_lon, 0) <> bd.num_adults
   OR ISNULL(p.tre_em, 0)    <> bd.num_children;

/* Tiếng Việt phải hiện đúng dấu - nếu ra dấu ? là đã thiếu tiền tố N ở đâu đó */
SELECT TOP 3 name, destination FROM tours ORDER BY id;

/* Tìm không dấu: cả hai câu đều phải trả về đúng 1 tour */
SELECT COUNT(*) AS tim_da_lat  FROM tours WHERE search_text LIKE '%da lat%';
SELECT COUNT(*) AS tim_phu_quoc FROM tours WHERE search_text LIKE '%phu quoc%';

/* Số chỗ đã bị trừ đúng chưa */
SELECT id, total_seats, available_seats
FROM tour_departures
WHERE available_seats < total_seats
ORDER BY id;
GO
