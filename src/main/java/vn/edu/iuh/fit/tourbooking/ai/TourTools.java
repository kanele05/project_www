package vn.edu.iuh.fit.tourbooking.ai;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.service.CartService;
import vn.edu.iuh.fit.tourbooking.service.TourService;
import vn.edu.iuh.fit.tourbooking.service.TrackingService;
import vn.edu.iuh.fit.tourbooking.session.CartItem;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
// Công cụ cho chatbot gọi (Tool Calling): tìm tour, xem lịch khởi hành còn chỗ, thêm vào giỏ. Không có công cụ thanh toán/đặt tour.
public class TourTools {

    public static final String CTX_SESSION = "session";
    public static final String CTX_USER_ID = "userId";

    private static final int MAX_RESULTS = 5;

    public record TourBrief(Long id, String name, String destination, String duration,
                            long priceFromVnd, String summary, String url) {
    }

    public record DepartureBrief(Long departureId, String date, int seatsLeft,
                                 long priceAdultVnd, long priceChildVnd) {
    }

    private final SemanticSearchService searchService;
    private final TourService tourService;
    private final CartService cartService;
    private final TrackingService trackingService;
    private final MessageHelper messages;

    @Tool(description = "Tìm tour phù hợp nhu cầu của khách theo ý nghĩa (biển, núi, gia đình, nghỉ dưỡng, ngân sách...). "
            + "Trả về tối đa 5 tour kèm id, giá từ và đường dẫn.")
    public List<TourBrief> searchTours(
            @ToolParam(description = "Mô tả nhu cầu bằng tiếng Việt, ví dụ: biển yên tĩnh cho gia đình") String query,
            @ToolParam(required = false, description = "Giá tối đa theo đồng (VND), bỏ trống nếu khách không nói giá") Long maxPriceVnd) {
        return searchService.searchList(query, maxPriceVnd, MAX_RESULTS).stream()
                .map(TourTools::toBrief)
                .toList();
    }

    @Tool(description = "Lấy các lịch khởi hành còn chỗ của một tour: id lịch, ngày đi, số chỗ còn, giá người lớn và trẻ em. "
            + "Luôn gọi công cụ này trước khi nói về ngày đi hoặc số chỗ.")
    public List<DepartureBrief> getDepartures(@ToolParam(description = "Id của tour (lấy từ searchTours)") Long tourId) {
        return tourService.findBookableDepartures(tourId).stream()
                .limit(6)
                .map(d -> new DepartureBrief(d.getId(), d.getDepartureDate().toString(),
                        d.getAvailableSeats(), d.getPriceAdult().longValue(), d.getPriceChild().longValue()))
                .toList();
    }

    @Tool(description = "Thêm một lịch khởi hành vào giỏ hàng của khách. CHỈ gọi sau khi khách đã nói rõ đồng ý và đã chọn "
            + "lịch khởi hành cùng số người lớn, trẻ em. Công cụ này không thanh toán; khách tự vào Giỏ hàng để đặt tour.")
    public String addToCart(
            @ToolParam(description = "Id lịch khởi hành lấy từ getDepartures") Long departureId,
            @ToolParam(description = "Số người lớn, tối thiểu 1") int adults,
            @ToolParam(description = "Số trẻ em, có thể là 0") int children,
            ToolContext toolContext) {
        Object session = toolContext.getContext().get(CTX_SESSION);
        if (!(session instanceof HttpSession httpSession)) {
            return "Không thêm được vì không xác định được giỏ hàng của khách.";
        }
        try {
            CartItem item = cartService.addByDeparture(httpSession, departureId, adults, children);
            Object userId = toolContext.getContext().get(CTX_USER_ID);
            trackingService.recordAddToCart(httpSession.getId(),
                    userId instanceof Long id ? id : null, item.getTourId(), item.getDepartureId());
            return "Đã thêm vào giỏ: " + item.getTourName() + " (" + item.getNumAdults() + " người lớn, "
                    + item.getNumChildren() + " trẻ em). Khách vào trang Giỏ hàng để kiểm tra và thanh toán.";
        } catch (BusinessRuleException e) {
            return "Không thêm được: " + messages.of(e);
        }
    }

    static TourBrief toBrief(Tour tour) {
        return new TourBrief(tour.getId(), tour.getName(), tour.getDestination(), tour.getDurationText(),
                tour.getBasePrice().longValue(), tour.getShortDescription(),
                "/tours/" + tour.getId() + "/" + tour.getSlug());
    }
}
