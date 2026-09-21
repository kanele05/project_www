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
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;
import vn.edu.iuh.fit.tourbooking.entity.Role;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourCategoryRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourDepartureRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;
import vn.edu.iuh.fit.tourbooking.util.CodeGenerator;
import vn.edu.iuh.fit.tourbooking.util.SlugUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đổ dữ liệu mẫu cho môi trường phát triển.
 *
 * <p>Chỉ chạy với profile {@code dev} và chỉ khi bảng còn rỗng, nên khởi động
 * lại ứng dụng nhiều lần cũng không nhân bản dữ liệu.</p>
 *
 * <p><b>Vì sao seed bằng Java thay vì file SQL:</b> mật khẩu phải là chuỗi băm
 * BCrypt, mà BCrypt có muối ngẫu nhiên nên không thể viết sẵn trong file SQL
 * (viết cứng một chuỗi băm thì mọi tài khoản dùng chung muối, và không ai đọc
 * file SQL mà biết mật khẩu gốc là gì). File {@code database/03_seed_data.sql}
 * được sinh ra <i>từ</i> dữ liệu này để nộp kèm báo cáo.</p>
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    /** Mật khẩu dùng chung cho mọi tài khoản mẫu - chỉ để tiện thử nghiệm. */
    private static final String DEMO_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final TourCategoryRepository categoryRepository;
    private final TourRepository tourRepository;
    private final TourDepartureRepository departureRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

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

    // =====================================================================
    // Người dùng
    // =====================================================================

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

    // =====================================================================
    // Danh mục
    // =====================================================================

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
        // .svg chứ không .jpg: sáu file minh hoạ danh mục là hình vẽ vector nằm
        // trong static/images/categories. Trước đây trỏ tới .jpg không tồn tại nên
        // lưới danh mục ở trang chủ toàn khung ảnh vỡ.
        c.setImageUrl("/images/categories/" + c.getSlug() + ".svg");
        return c;
    }

    // =====================================================================
    // Tour
    // =====================================================================

    /**
     * Mô tả gọn một tour mẫu.
     *
     * @param categoryIndex vị trí danh mục trong danh sách trả về từ {@link #seedCategories()}
     * @param price         giá tham khảo, đơn vị đồng
     */
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

    /**
     * Ảnh đại diện thật cho từng tour, theo đúng thứ tự khai báo ở
     * {@link #TOUR_SEEDS} (PQ, NT, CD, HL, QN, SP, HG, DL, MC, HUE, NB, TN, YT,
     * TL, SG, HQ, NB nhật, MT, CT, MN - khớp id 1-20 trong
     * {@code database/03_seed_data.sql}). Đường dẫn bắt đầu bằng "/": đây là
     * ảnh seed TĨNH trong {@code static/images/tours/}, không phải ảnh quản trị
     * viên tải lên qua {@code /admin/tours} (đường dẫn tương đối, nằm trong
     * {@code uploads/tours/} lúc chạy) - các khuôn mẫu hiển thị (xem
     * {@code fragments/components.html}) phân biệt hai trường hợp bằng dấu "/"
     * ở đầu chuỗi, cùng quy ước với {@code TourCategory.imageUrl}. Nguồn ảnh và
     * giấy phép của từng file: {@code static/images/tours/NGUON_ANH.md}.
     */
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

    // =====================================================================
    // Đợt khởi hành - mỗi tour ba đợt, cách nhau khoảng nửa tháng
    // =====================================================================

    private List<TourDeparture> seedDepartures(List<Tour> tours) {
        List<TourDeparture> all = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int[] offsets = {14, 30, 50};
        int[] seats = {30, 25, 20};

        for (Tour tour : tours) {
            for (int i = 0; i < offsets.length; i++) {
                LocalDate go = today.plusDays(offsets[i]);
                // Tour n ngày thì ngày về là ngày đi cộng (n - 1).
                LocalDate back = go.plusDays(tour.getDurationDays() - 1L);

                // Đợt sau nhích giá 5% mỗi đợt cho giống mùa cao điểm.
                BigDecimal adult = tour.getBasePrice()
                        .multiply(BigDecimal.valueOf(100 + 5L * i))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                // Trẻ em tính 70% giá người lớn.
                BigDecimal child = adult.multiply(BigDecimal.valueOf(0.7))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                all.add(new TourDeparture(tour, go, back, seats[i], adult, child));
            }
        }
        return departureRepository.saveAll(all);
    }

    // =====================================================================
    // Đơn đặt tour mẫu
    // =====================================================================

    private void seedBookings(List<User> users, List<TourDeparture> departures) {
        User an = users.get(1);
        User binh = users.get(2);
        User cuong = users.get(3);

        // Chỉ số 0, 3, 6... là đợt khởi hành đầu tiên của tour thứ 1, 2, 3...
        createBooking(an, BookingStatus.CONFIRMED, 12,
                detail(departures.get(0), 2, 1));
        createBooking(binh, BookingStatus.PENDING, 5,
                detail(departures.get(3), 2, 0));
        createBooking(cuong, BookingStatus.COMPLETED, 40,
                detail(departures.get(15), 4, 2));
        createBooking(an, BookingStatus.PENDING, 2,
                detail(departures.get(24), 2, 2),
                detail(departures.get(39), 1, 0));
        createBooking(binh, BookingStatus.CANCELLED, 20,
                detail(departures.get(9), 3, 0));
    }

    private BookingDetail detail(TourDeparture departure, int adults, int children) {
        return new BookingDetail(departure, adults, children);
    }

    /**
     * Tạo một đơn mẫu và trừ chỗ tương ứng - trừ đơn đã huỷ, vì huỷ đơn thì chỗ
     * phải được trả lại. Giữ đúng bất biến này ngay từ dữ liệu mẫu để các con số
     * trên màn hình quản trị luôn cộng khớp.
     */
    private void createBooking(User user, BookingStatus status, int daysAgo, BookingDetail... details) {
        Booking booking = new Booking();
        booking.setCode(CodeGenerator.uniqueBookingCode(bookingRepository::existsByCode));
        booking.setUser(user);
        booking.setBookingDate(LocalDateTime.now().minusDays(daysAgo));
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
        bookingRepository.save(booking);
    }
}
