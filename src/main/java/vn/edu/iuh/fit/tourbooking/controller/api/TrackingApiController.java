package vn.edu.iuh.fit.tourbooking.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.dto.form.TrackingRequest;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.TrackingService;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
// Nhận sự kiện hành vi từ tracker.js; trả 202 ngay, userId/sessionId do server tự lấy.
public class TrackingApiController {

    private final TrackingService trackingService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void track(@Valid @RequestBody TrackingRequest request,
                      HttpSession session,
                      HttpServletRequest http,
                      @AuthenticationPrincipal CustomUserDetails principal) {
        trackingService.record(request, session.getId(),
                principal == null ? null : principal.getId(),
                http.getHeader(HttpHeaders.USER_AGENT));
    }
}
