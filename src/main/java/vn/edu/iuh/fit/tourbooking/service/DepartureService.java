package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.dto.form.DepartureForm;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.BookingDetailRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourDepartureRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Nghiệp vụ quản lý đợt khởi hành.
 *
 * <p>Đợt khởi hành là tài nguyên con của tour, không tồn tại độc lập - vì vậy
 * mọi phương thức ở đây đều nhận {@code tourId} và kiểm tra đợt có đúng thuộc
 * tour đó không. Nếu chỉ tra theo {@code departureId}, ai đó sửa số trên thanh
 * địa chỉ là chỉnh được đợt khởi hành của tour khác.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartureService {

    private final TourDepartureRepository departureRepository;
    private final TourRepository tourRepository;
    private final BookingDetailRepository bookingDetailRepository;

    @Transactional(readOnly = true)
    public List<TourDeparture> findByTour(Long tourId) {
        return departureRepository.findByTourIdOrderByDepartureDateAsc(tourId);
    }

    @Transactional(readOnly = true)
    public TourDeparture getOfTour(Long tourId, Long departureId) {
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> ResourceNotFoundException.of("đợt khởi hành", departureId));

        if (!departure.getTour().getId().equals(tourId)) {
            throw ResourceNotFoundException.of("đợt khởi hành", departureId);
        }
        return departure;
    }

    /**
     * Thêm mới hoặc cập nhật một đợt khởi hành.
     *
     * <p>Khi sửa số chỗ, {@code availableSeats} được điều chỉnh theo <b>mức
     * chênh lệch</b> chứ không gán bằng {@code totalSeats}: đợt này có thể đã có
     * khách đặt, gán thẳng sẽ xoá sạch dấu vết số chỗ đã bán và làm bảng điều
     * khiển hiện số liệu sai.</p>
     */
    @Transactional
    public TourDeparture save(Long tourId, DepartureForm form) {
        if (form.getReturnDate().isBefore(form.getDepartureDate())) {
            throw new BusinessRuleException("error.departure.returnBeforeDeparture");
        }

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", tourId));

        boolean creating = form.getId() == null;
        requireUniqueDate(tourId, form);

        TourDeparture departure;
        if (creating) {
            // "Phải ở tương lai" chỉ bắt buộc lúc TẠO MỚI, hoặc lúc SỬA MÀ ĐỔI
            // NGÀY (nhánh else bên dưới) - xem Javadoc DepartureForm.departureDate.
            // Tạo mới thì chắc chắn chưa có khách nào, không có lý do gì cho ngày
            // khởi hành nằm ở quá khứ.
            requireFutureDate(form.getDepartureDate());
            departure = new TourDeparture(tour, form.getDepartureDate(), form.getReturnDate(),
                    form.getTotalSeats(), form.getPriceAdult(), form.getPriceChild());
        } else {
            departure = getOfTour(tourId, form.getId());

            if (!form.getDepartureDate().equals(departure.getDepartureDate())) {
                // Đổi sang một ngày khác thì ngày mới đó vẫn phải ở tương lai. Ngược
                // lại - giữ nguyên ngày cũ, chỉ sửa giá/số chỗ/trạng thái của một đợt
                // đã qua ngày (vẫn cần sửa được vì đã có khách đặt) - thì bỏ qua.
                requireFutureDate(form.getDepartureDate());
            }

            int soldSeats = departure.getBookedSeats();
            if (form.getTotalSeats() < soldSeats) {
                // Không cho hạ tổng số chỗ xuống dưới số khách đã đặt: làm vậy thì
                // availableSeats sẽ âm và có khách đã trả tiền nhưng không còn chỗ.
                throw new BusinessRuleException("error.departure.seatsBelowSold", soldSeats);
            }

            departure.setDepartureDate(form.getDepartureDate());
            departure.setReturnDate(form.getReturnDate());
            departure.setTotalSeats(form.getTotalSeats());
            departure.setAvailableSeats(form.getTotalSeats() - soldSeats);
            departure.setPriceAdult(form.getPriceAdult());
            departure.setPriceChild(form.getPriceChild());
        }
        departure.setActive(form.isActive());

        TourDeparture saved = departureRepository.save(departure);
        log.info("{} đợt khởi hành {} của tour {}",
                creating ? "Đã thêm" : "Đã cập nhật", saved.getDepartureDate(), tour.getCode());
        return saved;
    }

    /**
     * Xoá một đợt khởi hành.
     *
     * <p>Quy tắc chặn xoá thứ ba của đề bài: đợt đã có khách đặt thì không xoá
     * được. Kiểm tra bằng Java ở đây chứ không dựa vào khoá ngoại.</p>
     */
    @Transactional
    public void delete(Long tourId, Long departureId) {
        long inBooking = bookingDetailRepository.countByDepartureId(departureId);
        if (inBooking > 0) {
            throw new BusinessRuleException("error.departure.delete.inBooking", inBooking);
        }
        departureRepository.delete(getOfTour(tourId, departureId));
    }

    /** Đóng / mở bán một đợt - dùng khi không xoá được vì đã có khách đặt. */
    @Transactional
    public boolean toggleActive(Long tourId, Long departureId) {
        TourDeparture departure = getOfTour(tourId, departureId);
        departure.setActive(!departure.isActive());
        return departure.isActive();
    }

    private void requireFutureDate(LocalDate departureDate) {
        if (!departureDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("error.departure.departureDate.future");
        }
    }

    private void requireUniqueDate(Long tourId, DepartureForm form) {
        boolean duplicated = form.getId() == null
                ? departureRepository.existsByTourIdAndDepartureDate(tourId, form.getDepartureDate())
                : departureRepository.existsByTourIdAndDepartureDateAndIdNot(
                        tourId, form.getDepartureDate(), form.getId());
        if (duplicated) {
            throw new BusinessRuleException("error.departure.duplicateDate", form.getDepartureDate());
        }
    }
}
