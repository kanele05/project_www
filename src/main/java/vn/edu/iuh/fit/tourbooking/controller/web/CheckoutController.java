package vn.edu.iuh.fit.tourbooking.controller.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.dto.form.CheckoutForm;
import vn.edu.iuh.fit.tourbooking.dto.form.PassengerForm;
import vn.edu.iuh.fit.tourbooking.dto.form.PassengerGroupForm;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.BookingService;
import vn.edu.iuh.fit.tourbooking.service.CartService;
import vn.edu.iuh.fit.tourbooking.service.UserService;
import vn.edu.iuh.fit.tourbooking.session.Cart;
import vn.edu.iuh.fit.tourbooking.session.CartItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thanh toán - biến giỏ hàng trong session thành một đơn hàng trong CSDL.
 *
 * <p>Địa chỉ {@code /checkout} đã được {@code SecurityConfig} đặt ở mức
 * {@code authenticated()}: khách chưa đăng nhập sẽ bị đưa sang trang đăng nhập,
 * và sau khi đăng nhập xong quay lại đúng đây (nhờ {@code defaultSuccessUrl} đặt
 * tham số thứ hai là false). Giỏ hàng vẫn còn nguyên vì phiên chỉ bị đổi mã chứ
 * không bị tạo lại.</p>
 */
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final BookingService bookingService;
    private final UserService userService;
    private final MessageSource messageSource;

    /**
     * Nhẹ - 4 (đã tái hiện): giới hạn mặc định của Spring cho việc tự nới danh
     * sách khi bind form là 256 phần tử. Một request tự chế trỏ vào chỉ số xa
     * (ví dụ {@code passengerGroups[0].adults[300].fullName}) vượt ngưỡng đó ném
     * {@code InvalidPropertyException} thẳng ra trang 500. Hạ ngưỡng xuống một
     * số đủ dùng (giỏ hàng tối đa {@link CartService#MAX_GUESTS_PER_ITEM} khách
     * mỗi dòng) để lỗi bị chặn sớm và gọn hơn, không đợi tới tận controller.
     */
    @InitBinder("checkoutForm")
    public void initBinder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(64);
    }

    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails principal,
                               HttpSession session,
                               Model model) {
        Cart cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        // Điền sẵn thông tin từ hồ sơ cho đỡ phải gõ lại, nhưng vẫn cho sửa:
        // người đặt tour không nhất thiết là người đi.
        if (!model.containsAttribute("checkoutForm")) {
            CheckoutForm form = prefill(principal.getId());
            // Mục 12.7: mỗi dòng giỏ có N người lớn + M trẻ em thì dựng sẵn đúng
            // N+M ô hành khách - loại tự suy ra từ việc nằm trong danh sách adults
            // hay children, người dùng không có ô nào để chọn loại.
            form.setPassengerGroups(buildPassengerGroups(cart, form.getCustomerName(), form.getCustomerPhone()));
            model.addAttribute("checkoutForm", form);
        }
        return "checkout/checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("checkoutForm") CheckoutForm form,
                             BindingResult binding,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             HttpSession session,
                             RedirectAttributes ra) {
        Cart cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        // Đối chiếu CẤU TRÚC danh sách hành khách gửi lên với giỏ hàng hiện tại
        // (đang nằm trong session, phía máy chủ) - không tin số dòng/số ô mà
        // trình duyệt gửi lên khớp với những gì trang GET đã dựng. Lỗi cấu trúc
        // được gắn vào chính BindingResult (lỗi toàn cục) nên nhánh "còn lỗi"
        // bên dưới xử lý luôn, biểu mẫu vẫn hiện lại với dữ liệu người dùng đã gõ.
        // Chạy độc lập với @Valid ở trên: có lỗi @NotBlank tên khách KHÔNG che
        // mất lỗi lệch số lượng, cả hai cùng hiện một lượt.
        validatePassengerStructure(form, cart, binding);
        if (binding.hasErrors()) {
            // VỪA - 2 (đã tái hiện): KHÔNG dựng lại trang từ form CŨ khi cấu trúc
            // đã lệch - "cart" trong model (GlobalModelAdvice) luôn là giỏ hàng
            // HIỆN TẠI, nhưng checkoutForm.passengerGroups vẫn là dữ liệu của lần
            // GET trước; nếu giỏ hàng vừa đổi (mở tab khác thêm tour/đổi số khách)
            // thì để nguyên form cũ khiến Spring tự "nới" một PassengerGroupForm
            // rỗng cho dòng mới (departureId null, không ô hành khách nào) - gửi
            // lại bao nhiêu lần cũng lệch. Dựng lại đúng theo giỏ hàng hiện tại,
            // giữ nguyên tên đã nhập cho những đợt khởi hành còn trong giỏ.
            form.setPassengerGroups(rebuildPassengerGroups(form.getPassengerGroups(), cart,
                    form.getCustomerName(), form.getCustomerPhone()));
            return "checkout/checkout";
        }

        try {
            Booking booking = bookingService.placeOrder(principal.getId(), form, cart);

            // Xoá giỏ hàng CHỈ SAU KHI giao dịch đã commit xong. Đây chính là dòng
            // hiện thực yêu cầu "Session được xoá về null" của đề bài.
            cartService.clear(session);

            return "redirect:/checkout/success/" + booking.getCode();

        } catch (BusinessRuleException e) {
            // Ví dụ: trong lúc khách còn đang điền biểu mẫu thì người khác đã đặt
            // hết chỗ. Giỏ hàng giữ nguyên để khách sửa lại số lượng.
            ra.addFlashAttribute("errorMessage", messageSource.getMessage(
                    e.getMessageKey(), e.getArgs(), LocaleContextHolder.getLocale()));
            return "redirect:/cart";
        }
    }

    /**
     * Trang báo đặt tour thành công.
     *
     * <p>Vẫn đi qua {@code getOwnedByCode} để kiểm tra quyền sở hữu: đây là một
     * địa chỉ có mã đơn nằm ngay trên thanh địa chỉ, không thể vì nó tên là
     * "success" mà bỏ qua bước kiểm tra.</p>
     */
    @GetMapping("/checkout/success/{code}")
    public String success(@PathVariable String code,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        model.addAttribute("booking",
                bookingService.getOwnedByCode(code, principal.getId(), principal.isAdmin()));
        return "checkout/success";
    }

    // ---------------------------------------------------------------------

    private CheckoutForm prefill(Long userId) {
        User user = userService.getById(userId);
        CheckoutForm form = new CheckoutForm();
        form.setCustomerName(user.getFullName());
        form.setCustomerEmail(user.getEmail());
        form.setCustomerPhone(user.getPhone());
        form.setCustomerAddress(user.getAddress());
        form.setPaymentMethod("Chuyển khoản ngân hàng");
        return form;
    }

    /**
     * Dựng sẵn N+M ô hành khách cho từng dòng giỏ hàng (mục 12.7), theo đúng
     * thứ tự lặp của {@code cart.getItems()} - thứ tự này ổn định vì {@link Cart}
     * dùng {@code LinkedHashMap}, nên chỉ số trong danh sách khớp lại được giữa
     * lần dựng biểu mẫu (GET) và lần đối chiếu lúc gửi (POST).
     *
     * <p>Hành khách đầu tiên của tour đầu tiên được điền sẵn tên và số điện
     * thoại người đặt cho tiện - đúng yêu cầu "điền sẵn hành khách đầu tiên
     * bằng thông tin người đặt".</p>
     */
    private List<PassengerGroupForm> buildPassengerGroups(Cart cart, String customerName, String customerPhone) {
        List<PassengerGroupForm> groups = new ArrayList<>();
        boolean first = true;
        for (CartItem item : cart.getItems()) {
            PassengerGroupForm group = new PassengerGroupForm();
            group.setDepartureId(item.getDepartureId());
            for (int i = 0; i < item.getNumAdults(); i++) {
                PassengerForm p = new PassengerForm();
                if (first) {
                    p.setFullName(customerName);
                    p.setPhone(customerPhone);
                    first = false;
                }
                group.getAdults().add(p);
            }
            for (int i = 0; i < item.getNumChildren(); i++) {
                group.getChildren().add(new PassengerForm());
            }
            groups.add(group);
        }
        return groups;
    }

    /**
     * Đối chiếu {@code form.passengerGroups} với giỏ hàng hiện tại - đúng số
     * dòng, đúng {@code departureId} theo đúng thứ tự, và số ô hành khách mỗi
     * loại khớp {@code numAdults}/{@code numChildren} của dòng giỏ hàng đó.
     *
     * <p>Đây là lớp phòng thủ ở tầng controller (giữ được dữ liệu đã nhập khi
     * sai) - {@link vn.edu.iuh.fit.tourbooking.service.BookingService#placeOrder}
     * vẫn kiểm tra lại lần nữa trước khi ghi CSDL, không tin bước này đã chặn
     * hết mọi trường hợp (ví dụ giỏ hàng đổi ngay giữa lúc gửi biểu mẫu).</p>
     */
    private void validatePassengerStructure(CheckoutForm form, Cart cart, BindingResult binding) {
        List<CartItem> items = new ArrayList<>(cart.getItems());
        if (structureMatches(form.getPassengerGroups(), items)) {
            return;
        }
        // VỪA - 2: đây không còn là "dữ liệu tự chế sai" đơn thuần - rất có thể
        // giỏ hàng thật sự vừa đổi giữa lúc khách mở trang và lúc gửi biểu mẫu
        // (mở tab khác thêm tour, đổi số khách...). Khoá reject riêng để câu
        // thông báo nói đúng nguyên nhân, khác với trường hợp cấu trúc sai hẳn
        // ở BookingService.placeOrder (request tự chế gửi thẳng, không qua trang
        // này) vẫn dùng error.checkout.passengerMismatch.
        binding.reject("error.checkout.cartChangedRebuilt");
    }

    /** true nếu cấu trúc {@code groups} khớp hoàn toàn với giỏ hàng {@code items} hiện tại. */
    private boolean structureMatches(List<PassengerGroupForm> groups, List<CartItem> items) {
        if (groups == null || groups.size() != items.size()) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            PassengerGroupForm group = groups.get(i);
            if (group == null || group.getDepartureId() == null
                    || !group.getDepartureId().equals(item.getDepartureId())) {
                return false;
            }
            int adultCount = group.getAdults() == null ? 0 : group.getAdults().size();
            int childCount = group.getChildren() == null ? 0 : group.getChildren().size();
            if (adultCount != item.getNumAdults() || childCount != item.getNumChildren()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Dựng lại {@code passengerGroups} theo đúng giỏ hàng HIỆN TẠI (VỪA - 2),
     * nhưng vẫn giữ lại những tên đã nhập nếu {@code departureId} đó còn trong
     * giỏ - khách không phải gõ lại từ đầu chỉ vì đã lỡ mở thêm một dòng khác.
     * Cùng khuôn với {@link #buildPassengerGroups}: chỉ hành khách đầu tiên của
     * dòng đầu tiên được điền sẵn tên/điện thoại người đặt, và chỉ khi ô đó
     * đang thật sự trống (không ghi đè tên khách vừa gõ).
     */
    private List<PassengerGroupForm> rebuildPassengerGroups(List<PassengerGroupForm> oldGroups, Cart cart,
                                                              String customerName, String customerPhone) {
        Map<Long, PassengerGroupForm> byDeparture = new HashMap<>();
        if (oldGroups != null) {
            for (PassengerGroupForm g : oldGroups) {
                if (g != null && g.getDepartureId() != null) {
                    byDeparture.put(g.getDepartureId(), g);
                }
            }
        }

        List<PassengerGroupForm> result = new ArrayList<>();
        boolean first = true;
        for (CartItem item : cart.getItems()) {
            PassengerGroupForm old = byDeparture.get(item.getDepartureId());
            PassengerGroupForm group = new PassengerGroupForm();
            group.setDepartureId(item.getDepartureId());
            group.setAdults(resizeKeep(old == null ? null : old.getAdults(), item.getNumAdults()));
            group.setChildren(resizeKeep(old == null ? null : old.getChildren(), item.getNumChildren()));

            if (first && !group.getAdults().isEmpty()) {
                PassengerForm firstAdult = group.getAdults().get(0);
                if (firstAdult.getFullName() == null || firstAdult.getFullName().isBlank()) {
                    firstAdult.setFullName(customerName);
                    firstAdult.setPhone(customerPhone);
                }
            }
            first = false;
            result.add(group);
        }
        return result;
    }

    /** Giữ lại tối đa {@code size} phần tử đầu của {@code old} (nếu có), phần còn thiếu thì tạo ô trống mới. */
    private List<PassengerForm> resizeKeep(List<PassengerForm> old, int size) {
        List<PassengerForm> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (old != null && i < old.size() && old.get(i) != null) {
                result.add(old.get(i));
            } else {
                result.add(new PassengerForm());
            }
        }
        return result;
    }
}
