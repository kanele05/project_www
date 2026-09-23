package vn.edu.iuh.fit.tourbooking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingDetail;
import vn.edu.iuh.fit.tourbooking.entity.BookingPassenger;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatusHistory;
import vn.edu.iuh.fit.tourbooking.entity.PassengerType;
import vn.edu.iuh.fit.tourbooking.entity.Payment;
import vn.edu.iuh.fit.tourbooking.entity.PaymentMethod;
import vn.edu.iuh.fit.tourbooking.entity.PaymentStatus;
import vn.edu.iuh.fit.tourbooking.entity.Role;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;
import vn.edu.iuh.fit.tourbooking.repository.BookingStatusHistoryRepository;
import vn.edu.iuh.fit.tourbooking.repository.PaymentRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourCategoryRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourDepartureRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;
import vn.edu.iuh.fit.tourbooking.util.CodeGenerator;
import vn.edu.iuh.fit.tourbooking.util.SlugUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
// Đổ dữ liệu mẫu khi CSDL còn rỗng: 4 người dùng, 6 danh mục, 20 tour, đợt khởi hành, và vài đơn hàng tình huống thật.
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final TourCategoryRepository categoryRepository;
    private final TourRepository tourRepository;
    private final TourDepartureRepository departureRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingStatusHistoryRepository bookingStatusHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    // Điểm vào: bỏ qua nếu đã có dữ liệu, ngược lại đổ tuần tự người dùng -> danh mục -> tour -> đợt khởi hành -> đơn hàng.
    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Đã có dữ liệu, bỏ qua bước đổ dữ liệu mẫu.");
            return;
        }

        log.info("CSDL còn rỗng - bắt đầu đổ dữ liệu mẫu...");
        List<User> users = seedUsers();
        List<TourCategory> categories = seedCategories();
        List<Tour> tours = seedTours(categories);
        List<TourDeparture> departures = seedDepartures(tours);
        seedBookings(users, departures);

        log.info("Đổ dữ liệu mẫu xong: {} người dùng, {} danh mục, {} tour, {} đợt khởi hành, {} đơn.",
                userRepository.count(), categoryRepository.count(), tourRepository.count(),
                departureRepository.count(), bookingRepository.count());
        log.info("Tài khoản thử nghiệm - quản trị: admin@tourbooking.vn / {} ; khách: an.nguyen@gmail.com / {}",
                DEMO_PASSWORD, DEMO_PASSWORD);
    }

    private List<User> seedUsers() {
        String hashed = passwordEncoder.encode(DEMO_PASSWORD);

        User admin = new User("Quản trị hệ thống", "admin@tourbooking.vn", hashed, Role.ADMIN);
        admin.setPhone("0909000001");
        admin.setAddress("12 Nguyễn Văn Bảo, Gò Vấp, TP. Hồ Chí Minh");

        User an = new User("Nguyễn Văn An", "an.nguyen@gmail.com", hashed, Role.CUSTOMER);
        an.setPhone("0912345678");
        an.setAddress("25 Lê Lợi, Quận 1, TP. Hồ Chí Minh");

        User binh = new User("Trần Thị Bình", "binh.tran@gmail.com", hashed, Role.CUSTOMER);
        binh.setPhone("0987654321");
        binh.setAddress("48 Trần Hưng Đạo, Hải Châu, Đà Nẵng");

        User cuong = new User("Lê Quốc Cường", "cuong.le@gmail.com", hashed, Role.CUSTOMER);
        cuong.setPhone("0938111222");
        cuong.setAddress("77 Kim Mã, Ba Đình, Hà Nội");

        return userRepository.saveAll(List.of(admin, an, binh, cuong));
    }

    private List<TourCategory> seedCategories() {
        List<TourCategory> list = List.of(
                category("Du lịch biển đảo",
                        "Nghỉ dưỡng ven biển, lặn ngắm san hô và khám phá các hòn đảo."),
                category("Du lịch núi rừng",
                        "Chinh phục đỉnh cao, săn mây và trải nghiệm khí hậu vùng cao."),
                category("Du lịch văn hoá - tâm linh",
                        "Hành hương, thăm di tích lịch sử và các công trình kiến trúc cổ."),
                category("Du lịch nước ngoài",
                        "Các hành trình khám phá châu Á và châu Âu."),
                category("Du lịch sinh thái",
                        "Miệt vườn, rừng ngập mặn và các khu bảo tồn thiên nhiên."),
                category("Du lịch nghỉ dưỡng cao cấp",
                        "Resort 4-5 sao, suối khoáng nóng và dịch vụ chăm sóc sức khoẻ.")
        );
        return categoryRepository.saveAll(list);
    }

    private TourCategory category(String name, String description) {
        TourCategory c = new TourCategory(name, SlugUtil.toSlug(name), description);

        c.setImageUrl("/images/categories/" + c.getSlug() + ".svg");
        return c;
    }

    private record TourSeed(String code, String name, int categoryIndex, String from,
                            String destination, int days, int nights, long price,
                            String transportation, boolean featured, String summary) {
    }

    private static final List<TourSeed> TOUR_SEEDS = List.of(
            new TourSeed("PQ-3N2D", "Phú Quốc - Đảo Ngọc 3N2Đ", 0, "TP. Hồ Chí Minh", "Phú Quốc",
                    3, 2, 5_990_000, "Máy bay", true,
                    "Câu cá, lặn ngắm san hô ở Hòn Thơm và ngắm hoàng hôn tại Sunset Sanato."),
            new TourSeed("NT-4N3D", "Nha Trang - Vịnh Ngọc 4N3Đ", 0, "TP. Hồ Chí Minh", "Nha Trang",
                    4, 3, 4_590_000, "Máy bay", true,
                    "Tắm bùn khoáng, tham quan VinWonders và bốn đảo nổi tiếng của vịnh Nha Trang."),
            new TourSeed("CD-3N2D", "Côn Đảo - Tâm Linh & Biển Xanh 3N2Đ", 0, "TP. Hồ Chí Minh", "Côn Đảo",
                    3, 2, 6_490_000, "Máy bay", false,
                    "Viếng nghĩa trang Hàng Dương, thăm nhà tù Côn Đảo và tắm biển Đầm Trầu."),
            new TourSeed("HL-3N2D", "Hạ Long - Kỳ Quan Vịnh Biển 3N2Đ", 0, "Hà Nội", "Hạ Long",
                    3, 2, 3_890_000, "Xe du lịch", true,
                    "Du thuyền ngủ đêm trên vịnh, chèo kayak và khám phá hang Sửng Sốt."),
            new TourSeed("QN-4N3D", "Quy Nhơn - Phú Yên 4N3Đ", 0, "TP. Hồ Chí Minh", "Quy Nhơn",
                    4, 3, 5_190_000, "Máy bay", false,
                    "Kỳ Co - Eo Gió, Gành Đá Đĩa và ghềnh Đá Dĩa Phú Yên."),

            new TourSeed("SP-3N2D", "Sa Pa - Săn Mây Fansipan 3N2Đ", 1, "Hà Nội", "Sa Pa",
                    3, 2, 3_290_000, "Xe giường nằm", true,
                    "Cáp treo lên đỉnh Fansipan, bản Cát Cát và chợ phiên vùng cao."),
            new TourSeed("HG-4N3D", "Hà Giang - Cao Nguyên Đá 4N3Đ", 1, "Hà Nội", "Hà Giang",
                    4, 3, 4_190_000, "Xe du lịch", false,
                    "Đèo Mã Pí Lèng, sông Nho Quế và cột cờ Lũng Cú."),
            new TourSeed("DL-3N2D", "Đà Lạt - Thành Phố Ngàn Hoa 3N2Đ", 1, "TP. Hồ Chí Minh", "Đà Lạt",
                    3, 2, 2_890_000, "Xe du lịch", true,
                    "Đồi chè Cầu Đất, thác Datanla và chợ đêm Đà Lạt."),
            new TourSeed("MC-3N2D", "Mộc Châu - Mùa Hoa 3N2Đ", 1, "Hà Nội", "Mộc Châu",
                    3, 2, 2_690_000, "Xe du lịch", false,
                    "Đồi chè trái tim, thác Dải Yếm và rừng thông bản Áng."),

            new TourSeed("HUE-4N3D", "Huế - Đà Nẵng - Hội An 4N3Đ", 2, "TP. Hồ Chí Minh", "Huế",
                    4, 3, 5_490_000, "Máy bay", true,
                    "Đại Nội, chùa Thiên Mụ, Bà Nà Hills và phố cổ Hội An về đêm."),
            new TourSeed("NB-2N1D", "Ninh Bình - Tràng An - Bái Đính 2N1Đ", 2, "Hà Nội", "Ninh Bình",
                    2, 1, 1_890_000, "Xe du lịch", false,
                    "Du thuyền Tràng An, chùa Bái Đính và hang Múa."),
            new TourSeed("TN-2N1D", "Tây Ninh - Núi Bà Đen 2N1Đ", 2, "TP. Hồ Chí Minh", "Tây Ninh",
                    2, 1, 1_290_000, "Xe du lịch", false,
                    "Cáp treo núi Bà Đen, toà thánh Cao Đài và hồ Dầu Tiếng."),
            new TourSeed("YT-2N1D", "Quảng Ninh - Yên Tử Hành Hương 2N1Đ", 2, "Hà Nội", "Quảng Ninh",
                    2, 1, 1_690_000, "Xe du lịch", false,
                    "Chùa Đồng Yên Tử, thiền viện Trúc Lâm và khu di tích nhà Trần."),

            new TourSeed("TL-5N4D", "Thái Lan - Bangkok - Pattaya 5N4Đ", 3, "TP. Hồ Chí Minh", "Thái Lan",
                    5, 4, 9_990_000, "Máy bay", true,
                    "Chùa Phật Vàng, chợ nổi Bốn Miền và show Alcazar."),
            new TourSeed("SG-4N3D", "Singapore - Malaysia 4N3Đ", 3, "TP. Hồ Chí Minh", "Singapore",
                    4, 3, 12_900_000, "Máy bay", false,
                    "Gardens by the Bay, đảo Sentosa và cao nguyên Genting."),
            new TourSeed("HQ-5N4D", "Hàn Quốc - Seoul - Nami 5N4Đ", 3, "Hà Nội", "Hàn Quốc",
                    5, 4, 16_900_000, "Máy bay", true,
                    "Đảo Nami, cung Gyeongbok và tháp Namsan."),
            new TourSeed("NB-6N5D", "Nhật Bản - Cung Đường Vàng 6N5Đ", 3, "TP. Hồ Chí Minh", "Nhật Bản",
                    6, 5, 28_900_000, "Máy bay", false,
                    "Tokyo - Phú Sĩ - Kyoto - Osaka mùa hoa anh đào."),

            new TourSeed("MT-3N2D", "Miền Tây - Cần Thơ - Cà Mau 3N2Đ", 4, "TP. Hồ Chí Minh", "Cần Thơ",
                    3, 2, 2_390_000, "Xe du lịch", false,
                    "Chợ nổi Cái Răng, rừng tràm Trà Sư và mũi Cà Mau."),
            new TourSeed("CT-2N1D", "Cát Tiên - Vườn Quốc Gia 2N1Đ", 4, "TP. Hồ Chí Minh", "Đồng Nai",
                    2, 1, 1_590_000, "Xe du lịch", false,
                    "Đi bộ xuyên rừng, ngắm thú đêm và thăm đảo Tiên."),

            new TourSeed("MN-3N2D", "Mũi Né - Resort Nghỉ Dưỡng 3N2Đ", 5, "TP. Hồ Chí Minh", "Phan Thiết",
                    3, 2, 4_290_000, "Xe du lịch", true,
                    "Resort 4 sao sát biển, đồi cát bay và làng chài Mũi Né.")
    );

    private static final List<String> TOUR_THUMBNAILS = List.of(
            "/images/tours/tour01_phu-quoc.jpg",
            "/images/tours/tour02_nha-trang.jpg",
            "/images/tours/tour03_con-dao.jpg",
            "/images/tours/tour04_ha-long.jpg",
            "/images/tours/tour05_quy-nhon.jpg",
            "/images/tours/tour06_sa-pa.jpg",
            "/images/tours/tour07_ha-giang.jpg",
            "/images/tours/tour08_da-lat.jpg",
            "/images/tours/tour09_moc-chau.jpg",
            "/images/tours/tour10_hoi-an.jpg",
            "/images/tours/tour11_ninh-binh.jpg",
            "/images/tours/tour12_tay-ninh.jpg",
            "/images/tours/tour13_quang-ninh.jpg",
            "/images/tours/tour14_thai-lan.jpg",
            "/images/tours/tour15_singapore.jpg",
            "/images/tours/tour16_han-quoc.jpg",
            "/images/tours/tour17_nhat-ban.jpg",
            "/images/tours/tour18_mien-tay.jpg",
            "/images/tours/tour19_cat-tien.jpg",
            "/images/tours/tour20_mui-ne.jpg"
    );

    // Dựng 20 tour mẫu từ TOUR_SEEDS, gắn ảnh thật theo đúng thứ tự TOUR_THUMBNAILS.
    private List<Tour> seedTours(List<TourCategory> categories) {
        List<Tour> tours = new ArrayList<>();
        int index = 0;
        for (TourSeed seed : TOUR_SEEDS) {
            Tour t = new Tour();
            t.setCode(seed.code());
            t.setName(seed.name());
            t.setSlug(SlugUtil.toSlug(seed.name()));
            t.setShortDescription(seed.summary());
            t.setDescription(buildDescription(seed));
            t.setItinerary(buildItinerary(seed));
            t.setDepartureLocation(seed.from());
            t.setDestination(seed.destination());
            t.setDurationDays(seed.days());
            t.setDurationNights(seed.nights());
            t.setBasePrice(BigDecimal.valueOf(seed.price()));
            t.setTransportation(seed.transportation());
            t.setFeatured(seed.featured());
            t.setCategory(categories.get(seed.categoryIndex()));
            t.setThumbnail(TOUR_THUMBNAILS.get(index));
            tours.add(t);
            index++;
        }
        return tourRepository.saveAll(tours);
    }

    // Dựng đoạn mô tả tour mẫu từ dữ liệu seed.
    private String buildDescription(TourSeed seed) {
        return """
                %s

                Hành trình %d ngày %d đêm khởi hành từ %s, di chuyển bằng %s.
                Giá tour đã bao gồm vé tham quan, khách sạn tiêu chuẩn, các bữa ăn
                theo chương trình, hướng dẫn viên suốt tuyến và bảo hiểm du lịch.
                Giá chưa bao gồm chi phí cá nhân, đồ uống và tiền tip cho hướng dẫn viên.
                """.formatted(seed.summary(), seed.days(), seed.nights(),
                seed.from(), seed.transportation().toLowerCase());
    }

    // Dựng lịch trình từng ngày mẫu (ngày đầu/cuối khác ngày giữa) từ dữ liệu seed.
    private String buildItinerary(TourSeed seed) {
        StringBuilder sb = new StringBuilder();
        for (int day = 1; day <= seed.days(); day++) {
            sb.append("Ngày ").append(day).append(": ");
            if (day == 1) {
                sb.append(seed.from()).append(" - ").append(seed.destination())
                        .append(". Khởi hành, nhận phòng và tham quan các điểm gần trung tâm.");
            } else if (day == seed.days()) {
                sb.append(seed.destination()).append(" - ").append(seed.from())
                        .append(". Mua sắm đặc sản, trả phòng và về điểm xuất phát.");
            } else {
                sb.append("Tham quan các điểm nổi bật của ").append(seed.destination())
                        .append(", tự do trải nghiệm ẩm thực địa phương buổi tối.");
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    // Mỗi tour có 3 đợt khởi hành mẫu (14/30/50 ngày tới), giá tăng dần theo đợt.
    private List<TourDeparture> seedDepartures(List<Tour> tours) {
        List<TourDeparture> all = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int[] offsets = {14, 30, 50};
        int[] seats = {30, 25, 20};

        for (Tour tour : tours) {
            for (int i = 0; i < offsets.length; i++) {
                LocalDate go = today.plusDays(offsets[i]);

                LocalDate back = go.plusDays(tour.getDurationDays() - 1L);

                BigDecimal adult = tour.getBasePrice()
                        .multiply(BigDecimal.valueOf(100 + 5L * i))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

                BigDecimal child = adult.multiply(BigDecimal.valueOf(0.7))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                all.add(new TourDeparture(tour, go, back, seats[i], adult, child));
            }
        }
        return departureRepository.saveAll(all);
    }

    // Năm đơn mẫu đủ tình huống: đã xác nhận đặt cọc, chờ thanh toán, đã hoàn thành, hai dòng tour, đã huỷ + hoàn tiền.
    private void seedBookings(List<User> users, List<TourDeparture> departures) {
        User admin = users.get(0);
        User an = users.get(1);
        User binh = users.get(2);
        User cuong = users.get(3);

        TourDeparture completedDeparture = rewindOneDepartureToPast(departures.get(15));

        Booking b1 = createBooking(an, BookingStatus.CONFIRMED, LocalDateTime.now().minusDays(12),
                detail(departures.get(0), 2, 1));
        attachPassengers(b1.getDetails().get(0),
                List.of("Nguyễn Văn An", "Trần Thị Bích Ngọc"), List.of("Nguyễn Bảo Anh"));
        bookingRepository.save(b1);
        seedHistory(b1, null, BookingStatus.PENDING, an, "Khách đặt tour trên website");
        seedHistory(b1, BookingStatus.PENDING, BookingStatus.CONFIRMED, admin, "Đã nhận tiền cọc, xác nhận chỗ");
        seedPayment(b1, new BigDecimal("0.5"), PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID,
                "SEED-VCB-0001", b1.getBookingDate().plusDays(1), "Đặt cọc 50% khi giữ chỗ");
        seedPayment(b1, new BigDecimal("0.5"), PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING,
                null, null, "Còn lại, thu trước ngày khởi hành");

        Booking b2 = createBooking(binh, BookingStatus.PENDING, LocalDateTime.now().minusHours(5),
                detail(departures.get(3), 2, 0));
        attachPassengers(b2.getDetails().get(0), List.of("Trần Văn Bình", "Lê Thị Hồng"), List.of());
        bookingRepository.save(b2);
        seedHistory(b2, null, BookingStatus.PENDING, binh, "Khách đặt tour trên website");
        seedPayment(b2, BigDecimal.ONE, PaymentMethod.MOMO, PaymentStatus.PENDING,
                null, null, "Khách chọn thanh toán qua ví MoMo");

        Booking b3 = createBooking(cuong, BookingStatus.COMPLETED, LocalDateTime.now().minusDays(40),
                detail(completedDeparture, 4, 2));
        attachPassengers(b3.getDetails().get(0),
                List.of("Lê Minh Cường", "Phạm Thị Mai", "Lê Minh Khang", "Đặng Thị Kiều Ửng"),
                List.of("Lê Bảo Ngọc", "Lê Gia Huy"));
        bookingRepository.save(b3);
        seedHistory(b3, null, BookingStatus.PENDING, cuong, "Khách đặt tour trên website");
        seedHistory(b3, BookingStatus.PENDING, BookingStatus.CONFIRMED, admin, "Đã nhận đủ tiền, xác nhận chỗ");
        seedHistory(b3, BookingStatus.CONFIRMED, BookingStatus.COMPLETED, admin, "Chuyến đi đã kết thúc");
        seedPayment(b3, new BigDecimal("0.5"), PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID,
                "SEED-VCB-0002", b3.getBookingDate().plusDays(2), "Đặt cọc 50%");
        seedPayment(b3, new BigDecimal("0.5"), PaymentMethod.CASH, PaymentStatus.PAID,
                "SEED-PT-0001", b3.getBookingDate().plusDays(3), "Trả nốt tại văn phòng");

        Booking b4 = createBooking(an, BookingStatus.PENDING, LocalDateTime.now().minusHours(3),
                detail(departures.get(24), 2, 2),
                detail(departures.get(39), 1, 0));
        attachPassengers(b4.getDetails().get(0),
                List.of("Nguyễn Văn An", "Trần Thị Bích Ngọc"), List.of("Nguyễn Bảo Anh", "Nguyễn Minh Quân"));
        attachPassengers(b4.getDetails().get(1), List.of("Nguyễn Văn An"), List.of());
        bookingRepository.save(b4);
        seedHistory(b4, null, BookingStatus.PENDING, an, "Khách đặt tour trên website");
        seedPayment(b4, BigDecimal.ONE, PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING,
                null, null, "Chờ chuyển khoản trước ngày khởi hành");

        Booking b5 = createBooking(binh, BookingStatus.CANCELLED, LocalDateTime.now().minusDays(20),
                detail(departures.get(9), 3, 0));
        attachPassengers(b5.getDetails().get(0),
                List.of("Trần Văn Bình", "Lê Thị Hồng", "Trần Quốc Toản"), List.of());
        bookingRepository.save(b5);
        seedHistory(b5, null, BookingStatus.PENDING, binh, "Khách đặt tour trên website");
        seedHistory(b5, BookingStatus.PENDING, BookingStatus.CANCELLED, binh, "Khách báo bận, xin huỷ đơn");
        seedPayment(b5, BigDecimal.ONE, PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID,
                "SEED-VCB-0003", b5.getBookingDate().plusDays(1), "Đã thu đủ trước khi khách xin huỷ");
        seedPayment(b5, BigDecimal.ONE, PaymentMethod.BANK_TRANSFER, PaymentStatus.REFUNDED,
                "SEED-REFUND-0001", LocalDateTime.now().minusDays(19), "Đã hoàn tiền 100% sau khi khách xin huỷ");
    }

    // Đẩy lùi ngày một đợt khởi hành về quá khứ, phục vụ đơn mẫu ở trạng thái COMPLETED.
    private TourDeparture rewindOneDepartureToPast(TourDeparture departure) {
        LocalDate pastDeparture = LocalDate.now().minusDays(20);
        departure.setDepartureDate(pastDeparture);
        departure.setReturnDate(pastDeparture.plusDays(departure.getTour().getDurationDays() - 1L));
        return departureRepository.save(departure);
    }

    private BookingDetail detail(TourDeparture departure, int adults, int children) {
        return new BookingDetail(departure, adults, children);
    }

    private void attachPassengers(BookingDetail detail, List<String> adultNames, List<String> childNames) {
        for (String name : adultNames) {
            BookingPassenger p = new BookingPassenger(name, PassengerType.ADULT);
            p.setDetail(detail);
            detail.getPassengers().add(p);
        }
        for (String name : childNames) {
            BookingPassenger p = new BookingPassenger(name, PassengerType.CHILD);
            p.setDetail(detail);
            detail.getPassengers().add(p);
        }
    }

    // Sinh một khoản thanh toán mẫu bằng tỉ lệ của tổng đơn (ví dụ 0.5 = đặt cọc 50%).
    private void seedPayment(Booking booking, BigDecimal ratio, PaymentMethod method, PaymentStatus status,
                             String txnRef, LocalDateTime paidAt, String note) {
        BigDecimal amount = booking.getTotalAmount().multiply(ratio).setScale(0, RoundingMode.HALF_UP);
        Payment payment = new Payment(booking, amount, method);
        payment.setStatus(status);
        payment.setTxnRef(txnRef);
        payment.setPaidAt(paidAt);
        payment.setNote(note);
        paymentRepository.save(payment);
    }

    private void seedHistory(Booking booking, BookingStatus from, BookingStatus to, User changedBy, String reason) {
        bookingStatusHistoryRepository.save(new BookingStatusHistory(booking, from, to, changedBy, reason));
    }

    // Dựng một đơn mẫu từ thông tin người dùng và các dòng chi tiết truyền vào.
    private Booking createBooking(User user, BookingStatus status, LocalDateTime bookingDate, BookingDetail... details) {
        Booking booking = new Booking();
        booking.setCode(CodeGenerator.uniqueBookingCode(bookingRepository::existsByCode));
        booking.setUser(user);
        booking.setBookingDate(bookingDate);
        booking.setCustomerName(user.getFullName());
        booking.setCustomerEmail(user.getEmail());
        booking.setCustomerPhone(user.getPhone());
        booking.setCustomerAddress(user.getAddress());
        booking.setStatus(status);
        booking.setPaymentMethod("Chuyển khoản ngân hàng");
        booking.setNote(status == BookingStatus.CANCELLED ? "Khách báo bận, xin huỷ đơn." : null);

        for (BookingDetail d : details) {
            booking.addDetail(d);
            if (status != BookingStatus.CANCELLED) {
                d.getDeparture().holdSeats(d.getTotalGuests());
            }
        }
        booking.recalculateTotal();
        return booking;
    }
}
