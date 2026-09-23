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

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ quản lý đợt khởi hành - luôn kiểm đợt có đúng thuộc tourId truyền vào không.
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

    // Tạo mới hoặc cập nhật một đợt: chặn ngày trùng, chặn đổi ngày/giảm chỗ khi đã có khách, ngày mới phải ở tương lai.
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

            requireFutureDate(form.getDepartureDate());
            departure = new TourDeparture(tour, form.getDepartureDate(), form.getReturnDate(),
                    form.getTotalSeats(), form.getPriceAdult(), form.getPriceChild());
        } else {
            departure = getOfTour(tourId, form.getId());
            int soldSeats = departure.getBookedSeats();

            if (!form.getDepartureDate().equals(departure.getDepartureDate())) {

                if (soldSeats > 0) {
                    throw new BusinessRuleException("error.departure.dateLockedHasBookings", soldSeats);
                }

                requireFutureDate(form.getDepartureDate());
            }

            if (form.getTotalSeats() < soldSeats) {

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

    // Xoá một đợt khởi hành; chặn nếu đã có lượt đặt.
    @Transactional
    public void delete(Long tourId, Long departureId) {
        long inBooking = bookingDetailRepository.countByDepartureId(departureId);
        if (inBooking > 0) {
            throw new BusinessRuleException("error.departure.delete.inBooking", inBooking);
        }
        departureRepository.delete(getOfTour(tourId, departureId));
    }

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

    // Chặn hai đợt của cùng một tour trùng ngày khởi hành.
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
