package vn.edu.iuh.fit.tourbooking.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.iuh.fit.tourbooking.ai.TourVectorStore;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;
import vn.edu.iuh.fit.tourbooking.entity.EventType;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.entity.UserEvent;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;
import vn.edu.iuh.fit.tourbooking.service.BehaviorStatsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import vn.edu.iuh.fit.tourbooking.repository.UserEventRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cả ứng dụng thật trên H2 với khoá API để TRỐNG (biến môi trường có mặt nhưng rỗng): app phải chạy đủ chức năng, AI lùi về tìm từ khoá.
@SpringBootTest(properties = {"GEMINI_API_KEY=", "AI_ENABLED="})
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "h2"})
class AiDisabledSmokeTest {

    private static final String BROWSER = "Mozilla/5.0 (Windows NT 10.0) Chrome/126.0";

    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @Autowired UserEventRepository events;
    @Autowired UserRepository users;

    @Autowired BehaviorStatsService statsService;
    @Autowired BookingRepository bookings;

    private void event(EventType type, String session, Long tourId, String keyword, Integer durationMs) {
        UserEvent e = new UserEvent();
        e.setType(type);
        e.setSessionId(session);
        e.setTourId(tourId);
        e.setKeyword(keyword);
        e.setDurationMs(durationMs);
        e.setOccurredAt(LocalDateTime.now().minusMinutes(5));
        events.save(e);
    }

    @Test
    void behaviour_statistics_aggregate_events_and_real_bookings_for_the_date_range() {
        events.deleteAll();
        event(EventType.VIEW, "s1", 1L, null, null);
        event(EventType.VIEW, "s1", 1L, null, null);
        event(EventType.VIEW, "s2", 2L, null, null);
        event(EventType.CLICK, "s1", 2L, null, null);
        event(EventType.CLICK, "s2", 2L, null, null);
        event(EventType.ADD_TO_CART, "s1", 1L, null, null);
        event(EventType.TIME_ON_PAGE, "s1", 1L, null, 10_000);
        event(EventType.TIME_ON_PAGE, "s2", 1L, null, 20_000);
        event(EventType.SEARCH, "s1", null, "Đà Lạt", null);
        event(EventType.SEARCH, "s2", null, "đà lạt", null);
        event(EventType.SEARCH, "s2", null, "biển", null);
        UserEvent old = new UserEvent();
        old.setType(EventType.VIEW);
        old.setSessionId("old");
        old.setTourId(3L);
        old.setOccurredAt(LocalDateTime.now().minusDays(60));
        events.save(old);

        var report = statsService.report(LocalDate.now().minusDays(29), LocalDate.now());

        assertThat(report.totalEvents()).isEqualTo(11);
        assertThat(report.sessions()).isEqualTo(2);
        assertThat(report.topViewed()).extracting(BehaviorStatsService.TourCount::tourId, BehaviorStatsService.TourCount::count)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, 2L), org.assertj.core.groups.Tuple.tuple(2L, 1L));
        assertThat(report.topClicked()).singleElement().satisfies(t -> {
            assertThat(t.tourId()).isEqualTo(2L);
            assertThat(t.count()).isEqualTo(2L);
        });
        assertThat(report.avgSecondsOnPage()).isEqualTo(15.0);
        assertThat(report.keywords()).extracting(BehaviorStatsService.KeywordCount::keyword, BehaviorStatsService.KeywordCount::count)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("đà lạt", 2L), org.assertj.core.groups.Tuple.tuple("biển", 1L));
        assertThat(report.funnel()).extracting(BehaviorStatsService.FunnelStep::value).containsExactly(2L, 1L, report.funnel().get(2).value());
        var tour1 = report.tourFunnel().stream().filter(r -> r.tourId() == 1L).findFirst().orElseThrow();
        assertThat(tour1.views()).isEqualTo(2);
        assertThat(tour1.carts()).isEqualTo(1);
        assertThat(report.longestReads()).first().satisfies(t -> assertThat(t.avgSeconds()).isEqualTo(15.0));

        // Đơn đặt thật từ bảng bookings (dữ liệu mẫu): phễu lấy số đơn không huỷ trong khoảng ngày.
        var wide = statsService.report(LocalDate.now().minusDays(364), LocalDate.now());
        long placed = bookings.findAll().stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> !b.getBookingDate().isBefore(wide.range().start())).count();
        assertThat(wide.funnel().get(2).value()).isEqualTo(placed);
        assertThat(wide.mostBooked().stream().mapToLong(BehaviorStatsService.TourCount::count).sum()).isGreaterThanOrEqualTo(0);
        assertThat(report.range().from()).isEqualTo(LocalDate.now().minusDays(29));
    }

    @Test
    void site_works_fully_without_an_ai_key() throws Exception {
        assertThat(context.getBeansOfType(TourVectorStore.class)).isEmpty();
        assertThat(Files.exists(Path.of("data", "vector-store.json"))).isFalse();

        String home = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(home).contains("</html>").contains("AI chưa được cấu hình").doesNotContain("id=\"aiChatForm\"");

        // Bật "Tìm bằng AI" nhưng AI tắt: lùi về LIKE, vẫn thấy tour, có thông báo thân thiện.
        String list = mvc.perform(get("/tours").param("q", "đà lạt dưới 3 triệu").param("ai", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(list).contains("</html>").contains("Đà Lạt - Thành Phố Ngàn Hoa").contains("đang hiển thị kết quả tìm theo từ khoá");
        assertThat(list).doesNotContain("Phú Quốc - Đảo Ngọc");

        String json = mvc.perform(get("/api/tours/semantic").param("q", "đà lạt"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json).contains("\"mode\":\"FALLBACK\"").contains("\"aiAvailable\":false").contains("Đà Lạt");

        // Chatbot: 503 + câu thân thiện, không lộ chi tiết kỹ thuật.
        MvcResult chat = mvc.perform(post("/api/chat").session(new MockHttpSession()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"xin chào\"}"))
                .andExpect(status().isServiceUnavailable()).andReturn();
        assertThat(chat.getResponse().getContentAsString()).contains("chưa được cấu hình").doesNotContain("Exception");

        // Chi tiết tour: "Tour tương tự" lùi về cùng danh mục.
        String detail = mvc.perform(get("/tours/1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("</html>").contains("id=\"similar-tours\"").contains("cùng danh mục");

        // Theo dõi vẫn chạy; gợi ý cá nhân lùi về cùng danh mục.
        MockHttpSession session = new MockHttpSession();
        long before = events.count();
        mvc.perform(post("/api/tracking").session(session).with(csrf()).header("User-Agent", BROWSER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"VIEW\",\"tourId\":1}"))
                .andExpect(status().isAccepted());
        assertThat(events.count()).isEqualTo(before + 1);
        String homeAfter = mvc.perform(get("/").session(session)).andReturn().getResponse().getContentAsString();
        assertThat(homeAfter).contains("id=\"goi-y\"").contains("dựa trên những tour bạn vừa xem");

        // Quản trị: trang AI báo đang tắt; lập chỉ mục lại báo lỗi thân thiện qua flash (PRG).
        User admin = users.findByEmail("admin@tourbooking.vn").orElseThrow();
        String ai = mvc.perform(get("/admin/ai").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(ai).contains("</html>").contains("Đang tắt").contains("GEMINI_API_KEY");
        MvcResult reindex = mvc.perform(post("/admin/ai/reindex").with(csrf()).with(user(new CustomUserDetails(admin))))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(reindex.getFlashMap().get("errorMessage").toString()).contains("chưa được cấu hình");
        mvc.perform(get("/admin/analytics").with(user(new CustomUserDetails(admin)))).andExpect(status().isOk());
    }
}
