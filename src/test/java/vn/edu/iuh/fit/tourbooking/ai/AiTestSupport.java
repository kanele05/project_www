package vn.edu.iuh.fit.tourbooking.ai;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import vn.edu.iuh.fit.tourbooking.config.AiProperties;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

// Dữ liệu và tiện ích dùng chung cho các bài kiểm thử AI (không cần SQL Server, không cần mạng).
final class AiTestSupport {

    private AiTestSupport() {
    }

    static AiProperties props(Path dir, String embeddingModel) {
        return new AiProperties(true, "test-key", "http://localhost:0", "chat-test", embeddingModel,
                null, 0L, dir.resolve("vector-store.json").toString(), 0.1, 30, 4, 20, 100, 500);
    }

    static <T> ObjectProvider<T> provider(Class<T> type, T bean) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        if (bean != null) {
            factory.registerSingleton("bean", bean);
        }
        return factory.getBeanProvider(type);
    }

    static Tour tour(long id, String name, String destination, String category, long categoryId,
                     long price, String summary) {
        TourCategory c = new TourCategory();
        c.setId(categoryId);
        c.setName(category);
        Tour t = new Tour();
        t.setId(id);
        t.setCode("T" + id);
        t.setName(name);
        t.setSlug("tour-" + id);
        t.setDestination(destination);
        t.setDepartureLocation("TP. Hồ Chí Minh");
        t.setDurationDays(3);
        t.setDurationNights(2);
        t.setBasePrice(BigDecimal.valueOf(price));
        t.setShortDescription(summary);
        t.setTransportation("Máy bay");
        t.setCategory(c);
        t.setActive(true);
        t.setViewCount(id * 10);
        return t;
    }

    static List<Tour> sampleTours() {
        return List.of(
                tour(1, "Phú Quốc - Đảo Ngọc", "Phú Quốc", "Du lịch biển đảo", 1, 5_990_000,
                        "Câu cá, lặn ngắm san hô, biển xanh và hoàng hôn trên đảo."),
                tour(2, "Nha Trang - Vịnh Ngọc", "Nha Trang", "Du lịch biển đảo", 1, 4_590_000,
                        "Tắm biển, tắm bùn khoáng và bốn đảo nổi tiếng của vịnh."),
                tour(3, "Sa Pa - Săn Mây", "Sa Pa", "Du lịch núi rừng", 2, 3_290_000,
                        "Cáp treo Fansipan, săn mây trên núi và chợ phiên vùng cao."),
                tour(4, "Đà Lạt - Ngàn Hoa", "Đà Lạt", "Du lịch núi rừng", 2, 2_890_000,
                        "Đồi chè, thác nước, rừng thông và chợ đêm."),
                tour(5, "Hà Giang - Cao Nguyên Đá", "Hà Giang", "Du lịch núi rừng", 2, 4_190_000,
                        "Đèo Mã Pí Lèng, sông Nho Quế và núi rừng phía Bắc."));
    }
}
