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

@Controller
@RequiredArgsConstructor
// Trang thanh toán: hiện form đặt tour kèm ô nhập hành khách theo từng dòng giỏ, tạo đơn thật.
public class CheckoutController {

    private final CartService cartService;
    private final BookingService bookingService;
    private final UserService userService;
    private final MessageSource messageSource;

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

        if (!model.containsAttribute("checkoutForm")) {
            CheckoutForm form = prefill(principal.getId());

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

        validatePassengerStructure(form, cart, binding);
        if (binding.hasErrors()) {

            form.setPassengerGroups(rebuildPassengerGroups(form.getPassengerGroups(), cart,
                    form.getCustomerName(), form.getCustomerPhone()));
            return "checkout/checkout";
        }

        try {
            Booking booking = bookingService.placeOrder(principal.getId(), form, cart);

            cartService.clear(session);

            return "redirect:/checkout/success/" + booking.getCode();

        } catch (BusinessRuleException e) {

            ra.addFlashAttribute("errorMessage", messageSource.getMessage(
                    e.getMessageKey(), e.getArgs(), LocaleContextHolder.getLocale()));
            return "redirect:/cart";
        }
    }

    @GetMapping("/checkout/success/{code}")
    public String success(@PathVariable String code,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        model.addAttribute("booking",
                bookingService.getOwnedByCode(code, principal.getId(), principal.isAdmin()));
        return "checkout/success";
    }

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

    // Dựng sẵn N+M ô nhập hành khách theo đúng số người lớn/trẻ em của từng dòng giỏ hàng; điền sẵn tên/SĐT khách đặt vào người lớn đầu tiên.
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

    // Kiểm cấu trúc nhóm hành khách gửi lên còn khớp với giỏ hàng hiện tại không (giỏ có thể đã đổi giữa chừng).
    private void validatePassengerStructure(CheckoutForm form, Cart cart, BindingResult binding) {
        List<CartItem> items = new ArrayList<>(cart.getItems());
        if (structureMatches(form.getPassengerGroups(), items)) {
            return;
        }

        binding.reject("error.checkout.cartChangedRebuilt");
    }

    // So khớp từng nhóm hành khách với từng dòng giỏ: đúng đợt khởi hành, đúng số người lớn/trẻ em.
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

    // Dựng lại nhóm hành khách khi cấu trúc lệch giỏ, giữ lại dữ liệu khách đã gõ nếu còn khớp đợt khởi hành.
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

    // Co giãn danh sách hành khách về đúng "size" phần tử, giữ nguyên dữ liệu cũ ở các vị trí còn lại.
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
