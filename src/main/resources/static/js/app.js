/* ===========================================================================
   Kịch bản phía trình duyệt.

   Nguyên tắc xuyên suốt: MỌI tính năng ở đây chỉ là lớp tăng cường. Tắt
   JavaScript đi thì website vẫn dùng được trọn vẹn - biểu mẫu vẫn gửi theo cách
   thường, trang vẫn nạp lại, giỏ hàng vẫn thêm được. Vì vậy không chỗ nào gọi
   preventDefault() rồi mới đi tìm dữ liệu; luôn kiểm tra đủ điều kiện trước.

   Toàn bộ web service nằm dưới /api/** và dùng chính phiên đăng nhập để xác
   thực, nên yêu cầu ghi nào cũng cần token CSRF - xem phần đầu tiên.
   =========================================================================== */

/* ---------------------------------------------------------------------------
   Tiện ích dùng chung
   --------------------------------------------------------------------------- */

/** Đọc một câu chữ do máy chủ nhúng vào thẻ meta (xem fragments/layout.html). */
function appMessage(key) {
    return $('meta[name="msg.' + key + '"]').attr('content') || '';
}

/** Ngôn ngữ đang dùng, lấy từ thuộc tính lang của thẻ html do Thymeleaf đặt. */
function appLocale() {
    return document.documentElement.lang || 'vi';
}

/** Định dạng tiền theo ngôn ngữ hiện tại, ví dụ "5.990.000 ₫". */
function formatMoney(amount) {
    const number = Number(amount || 0);
    return number.toLocaleString(appLocale(), {maximumFractionDigits: 0})
        + ' ' + appMessage('currency');
}

/**
 * Hiện một thông báo nổi.
 *
 * Dựng thùng chứa ngay lúc cần thay vì bắt mọi khuôn mẫu phải khai báo sẵn một
 * thẻ div rỗng - bớt được một thứ dễ quên khi thêm trang mới.
 */
function showToast(message, variant) {
    if (!message) {
        return;
    }
    let $container = $('#toastContainer');
    if ($container.length === 0) {
        $container = $('<div id="toastContainer" class="toast-container position-fixed top-0 end-0 p-3"></div>')
            .appendTo('body');
    }

    const $toast = $(
        '<div class="toast align-items-center text-bg-' + (variant || 'success') + ' border-0" role="alert">' +
        '  <div class="d-flex">' +
        '    <div class="toast-body"></div>' +
        '    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>' +
        '  </div>' +
        '</div>');

    $toast.find('.toast-body').text(message);
    $container.append($toast);

    const toast = new bootstrap.Toast($toast[0], {delay: 4000});
    $toast.on('hidden.bs.toast', function () {
        $toast.remove();
    });
    toast.show();
}

/**
 * Rút câu thông báo ra khỏi một phản hồi lỗi.
 *
 * Máy chủ luôn trả về JSON đúng khuôn ApiError, nhưng vẫn phải phòng trường hợp
 * lỗi xảy ra trước khi tới được controller (mất kết nối, máy chủ tắt) - lúc đó
 * responseJSON là undefined và đọc thẳng .message sẽ hiện ra chữ "undefined"
 * thay vì một câu tử tế.
 */
function apiErrorMessage(jqXHR) {
    if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
        return jqXHR.responseJSON.message;
    }
    return appMessage('error.generic');
}

/** Cập nhật huy hiệu số dòng trên thanh điều hướng từ dữ liệu giỏ hàng vừa nhận. */
function updateCartBadge(cart) {
    const $badge = $('#cartBadge');
    if ($badge.length === 0) {
        return;
    }
    $badge.text(cart.itemCount);
    $badge.toggleClass('d-none', cart.itemCount === 0);
}

$(function () {

    /* -----------------------------------------------------------------------
       CSRF cho AJAX

       Gắn token vào header đúng MỘT lần thay vì phải nhớ ở từng lời gọi - quên
       một chỗ là nhận 403 trông y hệt lỗi phân quyền và rất mất công lần.

       Chỉ gắn cho yêu cầu ghi: GET/HEAD/OPTIONS/TRACE là các phương thức an
       toàn, Spring Security không kiểm tra token nên gửi kèm cũng vô ích.
       ----------------------------------------------------------------------- */
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

    if (csrfToken && csrfHeader) {
        $.ajaxSetup({
            beforeSend: function (xhr, settings) {
                if (!/^(GET|HEAD|OPTIONS|TRACE)$/i.test(settings.type)) {
                    xhr.setRequestHeader(csrfHeader, csrfToken);
                }
            }
        });
    }

    // Tự ẩn thông báo sau vài giây cho đỡ vướng mắt. Người dùng vẫn bấm nút x
    // để đóng sớm được.
    window.setTimeout(function () {
        $('.alert-dismissible').fadeOut(400);
    }, 5000);

    // Chặn gửi biểu mẫu hai lần khi người dùng bấm nhanh tay. Không có đoạn này
    // thì bấm đúp nút "Đặt tour" sẽ thêm hai lần vào giỏ.
    // Bỏ qua các biểu mẫu do AJAX xử lý: chúng không nạp lại trang nên nút bị
    // khoá sẽ nằm im mãi mãi.
    //
    // Biểu mẫu có [data-confirm] (ví dụ UC023 - tự huỷ đơn, mục 12.3) phải hiện
    // hộp xác nhận TRƯỚC: câu hỏi đã được máy chủ dựng sẵn kèm số tiền sẽ hoàn,
    // huỷ hộp thoại thì dừng hẳn - không khoá nút, không gửi biểu mẫu. Đây vẫn
    // chỉ là lớp tăng cường: tắt JavaScript thì biểu mẫu gửi thẳng không hỏi lại.
    $('form').not('[data-ajax-cart]').on('submit', function (e) {
        const confirmMessage = $(this).data('confirm');
        if (confirmMessage && !window.confirm(confirmMessage)) {
            e.preventDefault();
            return;
        }
        const $button = $(this).find('button[type="submit"]');
        window.setTimeout(function () {
            $button.prop('disabled', true);
        }, 0);
    });

    // Bật các hiệu ứng chuyển động thuần CSS (hero hiện dần, khối tô nền quét
    // ngang). CSS chỉ khai báo chúng bên trong .anim-ready, nên máy không chạy
    // được JavaScript sẽ thấy hero hiện đầy đủ ngay chứ không mất nội dung nào -
    // xem chú thích dài ở mục "HIỆU ỨNG VÀO CỦA HERO" trong app.css.
    document.documentElement.classList.add('anim-ready');

    initStickyNav();
    initReveal();
    initReel();
    initMarquee();
    initCountUp();
    initCartForms();
    initSearchSuggest();
    initDeparturePicker();
    initEmailCheck();
    initCouponCheck();
});

/* ---------------------------------------------------------------------------
   Giao diện: thanh điều hướng và hiệu ứng hiện dần
   --------------------------------------------------------------------------- */

/**
 * Thanh điều hướng của trang chủ trong suốt khi ở đỉnh trang rồi đặc lại khi cuộn.
 *
 * Không có đoạn này thì chữ trắng của thanh điều hướng sẽ nằm đè lên nền sáng của
 * khối ngay dưới hero và không đọc được nữa.
 *
 * Dùng requestAnimationFrame để một lần cuộn chỉ đọc vị trí đúng một lần: gắn
 * thẳng vào sự kiện scroll thì trình duyệt gọi hàm hàng trăm lần mỗi giây và
 * việc đọc scrollY liên tục làm trang giật.
 */
function initStickyNav() {
    const nav = document.querySelector('.site-nav--overlay');
    if (!nav) {
        return;
    }

    let ticking = false;
    const apply = function () {
        nav.classList.toggle('is-scrolled', window.scrollY > 40);
        ticking = false;
    };

    window.addEventListener('scroll', function () {
        if (!ticking) {
            window.requestAnimationFrame(apply);
            ticking = true;
        }
    }, {passive: true});

    apply();
}

/** Sau ngần này mili giây mà một phần tử vẫn chưa hiện thì cho hiện luôn. */
const REVEAL_FAILSAFE_MS = 2500;

/**
 * Nội dung hiện dần khi cuộn tới.
 *
 * Lớp .reveal-ready được thêm vào thẻ html Ở ĐÂY chứ không viết sẵn trong khuôn
 * mẫu. Lý do: CSS chỉ ẩn phần tử khi có lớp đó, nên máy không chạy được
 * JavaScript sẽ không bao giờ rơi vào cảnh cả trang tàng hình vĩnh viễn - một
 * lỗi kinh điển của các trang dùng hiệu ứng cuộn.
 *
 * NHƯNG "có JavaScript" chưa đủ. Khi kiểm thử, gặp đúng một môi trường trình
 * duyệt mà IntersectionObserver tồn tại nhưng KHÔNG BAO GIỜ bắn sự kiện (khung
 * xem không dựng hình). Lúc ấy .reveal-ready đã được thêm vào, phần tử đã bị ẩn,
 * và không có gì hiện chúng lên nữa - cả trang trắng từ dưới hero trở xuống.
 * Vì vậy có thêm HAI lớp bảo hiểm:
 *   1. hẹn giờ: quá REVEAL_FAILSAFE_MS thì hiện hết những gì còn sót;
 *   2. hiện ngay những phần tử đã nằm trong khung nhìn lúc tải trang, không đợi
 *      quan sát viên báo - nội dung đầu trang không có lý do gì phải chờ.
 * Một hiệu ứng trang trí không bao giờ được phép làm mất nội dung.
 */
function initReveal() {
    const items = document.querySelectorAll('.reveal');
    if (items.length === 0) {
        return;
    }

    const revealAll = function () {
        items.forEach(function (item) {
            item.classList.add('is-visible');
        });
    };

    if (!('IntersectionObserver' in window)) {
        return;   // không ẩn gì cả, trang hiện bình thường
    }

    document.documentElement.classList.add('reveal-ready');

    // Lớp bảo hiểm 1: dù chuyện gì xảy ra, nội dung cũng phải hiện.
    const failsafe = window.setTimeout(revealAll, REVEAL_FAILSAFE_MS);

    const observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
                // Hiện xong thì thôi theo dõi - hiệu ứng chỉ chạy một lần, cuộn
                // lên cuộn xuống mà nội dung cứ nhấp nháy thì rất khó chịu.
                observer.unobserve(entry.target);
            }
        });
    }, {rootMargin: '0px 0px -8% 0px', threshold: .12});

    items.forEach(function (item) {
        // Lớp bảo hiểm 2: phần tử đã nằm trong khung nhìn thì hiện ngay.
        if (item.getBoundingClientRect().top < window.innerHeight) {
            item.classList.add('is-visible');
        }
        observer.observe(item);
    });

    // Rời trang thì dọn hẹn giờ cho sạch.
    window.addEventListener('pagehide', function () {
        window.clearTimeout(failsafe);
    }, {once: true});
}

/* ---------------------------------------------------------------------------
   Trang chủ: ba hiệu ứng, ba lớp bảo hiểm

   Nguyên tắc chung của cả ba: TRẠNG THÁI KHÔNG CÓ JAVASCRIPT PHẢI LÀ TRẠNG THÁI
   ĐÚNG. Chồng thẻ đã nghiêng sẵn bằng CSS, hai dải địa danh đã có đủ tên, bốn con
   số đã là số thật do máy chủ dựng. JavaScript ở đây chỉ thêm chuyển động lên
   trên một trang vốn đã hoàn chỉnh - không có dòng nào trong này là điều kiện để
   nhìn thấy nội dung.
   --------------------------------------------------------------------------- */

/** Người dùng đã bật "giảm chuyển động" ở hệ điều hành chưa. */
function prefersReducedMotion() {
    return window.matchMedia
        && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * Nhân bản ảnh trong băng chạy dọc ở hero để vòng lặp khép kín.
 *
 * CSS cho băng trôi từ translateY(-50%) về 0 rồi lặp lại. Muốn không thấy mối
 * nối thì nửa dưới của băng phải giống hệt nửa trên, nên bước cuối cùng ở đây là
 * nhân đôi TOÀN BỘ nội dung.
 *
 * Trước đó còn một vòng nhân bản nữa: ít tour nổi bật quá thì cả băng ngắn hơn
 * khung nhìn và sẽ lộ ra một khoảng trống chạy qua. Sáu tour của dữ liệu mẫu thì
 * không gặp, nhưng CSDL của người khác thì chưa chắc.
 *
 * Không cần đánh dấu aria-hidden cho bản sao: cả khối băng đã mang aria-hidden
 * ngay trong khuôn mẫu vì nó thuần tuý là trang trí.
 */
function initReel() {
    const reel = document.querySelector('[data-reel]');
    if (!reel) {
        return;
    }

    const track = reel.querySelector('.home-reel__track');
    if (!track || track.children.length === 0) {
        return;
    }

    const original = Array.prototype.slice.call(track.children);

    const cloneInto = function (nodes) {
        nodes.forEach(function (node) {
            track.appendChild(node.cloneNode(true));
        });
    };

    let guard = 0;
    while (track.scrollHeight < reel.clientHeight * 1.25 && guard < 12) {
        cloneInto(original);
        guard += 1;
    }

    cloneInto(Array.prototype.slice.call(track.children));
}

/**
 * Nhân bản nội dung hai dải địa danh để vòng lặp khép kín.
 *
 * CSS dịch dải đi -50% rồi quay lại 0. Muốn không thấy mối nối thì nửa sau phải
 * giống hệt nửa đầu, nên đoạn này nhân đôi TOÀN BỘ nội dung. Trước đó còn một
 * vòng nhân bản nữa: ít điểm đến quá thì cả dải hẹp hơn màn hình và sẽ lộ ra một
 * khoảng trống chạy ngang - có 14 địa danh thì không gặp, nhưng CSDL của người
 * khác thì chưa chắc.
 *
 * Bản sao bị đánh dấu aria-hidden và tabindex -1: mắt cần chúng, còn trình đọc
 * màn hình và phím Tab thì không - đọc "Đà Lạt" bốn lần liên tiếp là một trải
 * nghiệm tệ hơn hẳn việc không có hiệu ứng nào.
 */
function initMarquee() {
    const boxes = document.querySelectorAll('[data-marquee]');

    boxes.forEach(function (box) {
        const track = box.querySelector('.marquee__track');
        if (!track || track.children.length === 0) {
            return;
        }

        const original = Array.prototype.slice.call(track.children);

        const cloneInto = function (nodes) {
            nodes.forEach(function (node) {
                const copy = node.cloneNode(true);
                copy.setAttribute('aria-hidden', 'true');
                copy.setAttribute('tabindex', '-1');
                track.appendChild(copy);
            });
        };

        let guard = 0;
        while (track.scrollWidth < window.innerWidth * 1.25 && guard < 12) {
            cloneInto(original);
            guard += 1;
        }

        cloneInto(Array.prototype.slice.call(track.children));
    });
}

/**
 * Bốn con số ở chân hero chạy từ 0 lên giá trị thật khi cuộn tới.
 *
 * Con số THẬT đã nằm sẵn trong HTML do máy chủ dựng; hàm này chỉ ghi đè trong
 * lúc chạy và luôn kết thúc bằng chính con số ấy. Trình duyệt không chạy được
 * JavaScript, hoặc requestAnimationFrame không bao giờ nổ, thì nội dung vẫn là
 * số đúng - không có nhánh nào để lại số 0 trên màn hình.
 */
function initCountUp() {
    const items = document.querySelectorAll('[data-countup]');
    if (items.length === 0 || prefersReducedMotion() || !('IntersectionObserver' in window)) {
        return;
    }

    const DURATION = 900;

    const run = function (el) {
        const target = parseInt(String(el.textContent || '').replace(/\D/g, ''), 10);
        if (!isFinite(target) || target <= 0) {
            return;
        }
        const startedAt = window.performance ? window.performance.now() : Date.now();

        const tick = function (now) {
            const progress = Math.min((now - startedAt) / DURATION, 1);
            // Cùng đường cong với --ease-out của app.css: bung nhanh rồi hãm dần.
            const eased = 1 - Math.pow(1 - progress, 3);
            if (progress < 1) {
                el.textContent = String(Math.round(target * eased));
                window.requestAnimationFrame(tick);
            } else {
                el.textContent = String(target);
            }
        };
        window.requestAnimationFrame(tick);
    };

    const observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                observer.unobserve(entry.target);
                run(entry.target);
            }
        });
    }, {threshold: .45});

    items.forEach(function (item) {
        observer.observe(item);
    });
}

/* ---------------------------------------------------------------------------
   Thêm vào giỏ không nạp lại trang
   --------------------------------------------------------------------------- */

/**
 * Bắt các biểu mẫu có đánh dấu data-ajax-cart.
 *
 * Biểu mẫu vẫn giữ nguyên action và method thật, nên khi không có JavaScript nó
 * gửi theo cách thường và máy chủ trả về trang giỏ hàng như trước. Đoạn dưới
 * chỉ chen ngang khi chắc chắn xử lý được.
 */
function initCartForms() {
    $('form[data-ajax-cart]').on('submit', function (e) {
        e.preventDefault();

        const $form = $(this);
        const $button = $form.find('button[type="submit"]');

        // Gom dữ liệu từ chính các ô của biểu mẫu, không viết cứng - nhờ vậy một
        // hàm dùng được cho cả nút ở trang danh sách (chỉ có tourId) lẫn biểu mẫu
        // ở trang chi tiết (có departureId chọn bằng nút tròn và số khách).
        const payload = {
            departureId: numberOrNull($form.find('[name="departureId"]:checked').val()
                || $form.find('input[type="hidden"][name="departureId"]').val()),
            tourId: numberOrNull($form.find('[name="tourId"]').val()),
            numAdults: Number($form.find('[name="numAdults"]').val() || 1),
            numChildren: Number($form.find('[name="numChildren"]').val() || 0)
        };

        $button.prop('disabled', true);

        $.ajax({
            url: '/api/cart/items',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload)
        }).done(function (cart) {
            updateCartBadge(cart);
            showToast(cart.message, 'success');
        }).fail(function (jqXHR) {
            // 409 = vi phạm quy tắc nghiệp vụ (hết chỗ, thiếu người lớn...).
            // Câu chữ đã do máy chủ tra sẵn từ messages.properties.
            showToast(apiErrorMessage(jqXHR), 'danger');
        }).always(function () {
            $button.prop('disabled', false);
        });
    });
}

function numberOrNull(value) {
    const number = Number(value);
    return value === undefined || value === '' || isNaN(number) ? null : number;
}

/* ---------------------------------------------------------------------------
   Gợi ý tìm kiếm
   --------------------------------------------------------------------------- */

/**
 * Gợi ý tour ngay khi người dùng đang gõ.
 *
 * Có hoãn 250 mili giây: gõ "da nang" là bảy lần nhấn phím, không hoãn thì
 * thành bảy lời gọi và bảy câu truy vấn cho một lần tìm. Mỗi lần gõ tiếp lại
 * huỷ hẹn cũ, nên máy chủ chỉ nhận đúng một yêu cầu sau khi người dùng ngừng tay.
 *
 * Máy chủ tìm trên cột tours.search_text đã bỏ dấu nên gõ "da lat" vẫn ra
 * "Đà Lạt" - việc chuẩn hoá từ khoá do máy chủ lo, ở đây gửi nguyên văn.
 */
function initSearchSuggest() {
    const $input = $('#navSearchInput');
    const $panel = $('#searchSuggest');
    if ($input.length === 0 || $panel.length === 0) {
        return;
    }

    let timer = null;
    let pending = null;

    $input.on('input', function () {
        const keyword = $input.val().trim();
        window.clearTimeout(timer);

        if (keyword.length < 2) {
            $panel.addClass('d-none').empty();
            return;
        }

        timer = window.setTimeout(function () {
            // Huỷ lời gọi trước nếu nó còn đang chạy: mạng chậm thì kết quả của
            // từ khoá cũ có thể về sau kết quả mới và đè lên, hiện ra danh sách
            // không khớp với chữ đang có trong ô.
            if (pending) {
                pending.abort();
            }
            pending = $.getJSON('/api/tours/suggest', {q: keyword, limit: 6})
                .done(function (tours) {
                    renderSuggestions($panel, tours);
                })
                .fail(function (jqXHR) {
                    // statusText 'abort' là do chính mình huỷ, không phải lỗi.
                    if (jqXHR.statusText !== 'abort') {
                        $panel.addClass('d-none').empty();
                    }
                });
        }, 250);
    });

    // Bấm ra ngoài thì đóng bảng gợi ý.
    $(document).on('click', function (e) {
        if (!$(e.target).closest('#navSearchForm').length) {
            $panel.addClass('d-none');
        }
    });

    $input.on('focus', function () {
        if ($panel.children().length > 0) {
            $panel.removeClass('d-none');
        }
    });
}

function renderSuggestions($panel, tours) {
    $panel.empty();

    if (!tours || tours.length === 0) {
        $panel.append($('<div class="list-group-item small text-muted"></div>')
            .text(appMessage('suggest.empty')));
        $panel.removeClass('d-none');
        return;
    }

    tours.forEach(function (tour) {
        // Dựng bằng .text() chứ không nối chuỗi HTML: tên tour do quản trị viên
        // nhập, nối thẳng vào HTML là mở đường cho mã lạ chạy trong trang.
        const $item = $('<a class="list-group-item list-group-item-action py-2"></a>')
            .attr('href', tour.detailUrl);

        $item.append($('<div class="small fw-semibold"></div>').text(tour.name));
        $item.append($('<div class="small text-muted"></div>')
            .text(tour.destination + ' - ' + formatMoney(tour.basePrice)));

        $panel.append($item);
    });
    $panel.removeClass('d-none');
}

/* ---------------------------------------------------------------------------
   Ô chọn ngày khởi hành
   --------------------------------------------------------------------------- */

/**
 * Nạp lại danh sách đợt khởi hành của trang chi tiết.
 *
 * Danh sách đã được máy chủ dựng sẵn khi tải trang, nên đoạn này không phải để
 * trang chạy được - nó để SỐ CHỖ TRỐNG LUÔN ĐÚNG. Khách mở trang rồi đi pha cà
 * phê mười lăm phút, quay lại vẫn thấy "còn 3 chỗ" trong khi thực tế đã hết;
 * bấm nút làm mới là biết ngay, thay vì tới lúc bấm Đặt tour mới nhận thông báo.
 */
function initDeparturePicker() {
    const $list = $('#departureList');
    if ($list.length === 0) {
        return;
    }
    const tourId = $list.data('tour-id');

    $('#refreshDepartures').on('click', function () {
        const $button = $(this);
        $button.prop('disabled', true);

        $.getJSON('/api/tours/' + tourId + '/departures')
            .done(function (departures) {
                renderDepartures($list, departures);
                showToast($list.data('refreshed-message'), 'info');
            })
            .fail(function (jqXHR) {
                showToast(apiErrorMessage(jqXHR), 'danger');
            })
            .always(function () {
                $button.prop('disabled', false);
            });
    });
}

function renderDepartures($list, departures) {
    // Giữ lại lựa chọn hiện tại để không bắt khách chọn lại từ đầu.
    const selected = $list.find('input[name="departureId"]:checked').val();
    const seatsLabel = $list.data('seats-label') || '';
    $list.empty();

    if (!departures || departures.length === 0) {
        $list.append($('<p class="text-muted small mb-0"></p>')
            .text($list.data('empty-message') || ''));
        return;
    }

    departures.forEach(function (departure, index) {
        const id = 'dep-' + departure.id;

        const $radio = $('<input class="form-check-input" type="radio" name="departureId" required>')
            .attr('id', id)
            .attr('value', departure.id);
        // Chọn lại đúng đợt cũ; nếu đợt cũ không còn thì chọn đợt đầu tiên.
        if (String(departure.id) === String(selected) || (!selected && index === 0)) {
            $radio.prop('checked', true);
        }

        const $label = $('<label class="form-check-label w-100"></label>').attr('for', id);
        $label.append($('<span class="fw-semibold"></span>').text(departure.departureDateText));
        $label.append($('<span class="badge bg-success float-end"></span>')
            .text(seatsLabel.replace('{0}', departure.availableSeats)));
        $label.append('<br>');
        $label.append($('<small class="text-muted"></small>')
            .text(formatMoney(departure.priceAdult) + ' / ' + formatMoney(departure.priceChild)));

        $list.append($('<div class="form-check border rounded p-2 mb-2 departure-option"></div>')
            .append($radio).append($label));
    });
}

/* ---------------------------------------------------------------------------
   Kiểm tra email trùng ngay lúc gõ
   --------------------------------------------------------------------------- */

/**
 * Báo ngay khi email đã có người dùng, thay vì đợi gửi biểu mẫu mới biết.
 *
 * Chỉ hỏi máy chủ khi người dùng đã rời khỏi ô (sự kiện blur) chứ không hỏi theo
 * từng phím: gõ dở "an.ngu" thì câu trả lời nào cũng vô nghĩa.
 *
 * Đây là tiện ích, KHÔNG phải lớp kiểm tra. Việc chặn thật vẫn nằm ở @UniqueEmail
 * lúc gửi biểu mẫu và ở UserService.register - giữa lúc hỏi và lúc bấm gửi vẫn
 * có thể có người khác đăng ký mất email đó.
 */
function initEmailCheck() {
    const $input = $('input[data-check-email]');
    if ($input.length === 0) {
        return;
    }

    const $feedback = $('<div class="form-text"></div>').insertAfter($input);

    $input.on('blur', function () {
        const email = $input.val().trim();
        if (email.length === 0 || email.indexOf('@') < 0) {
            $feedback.empty().removeClass('text-danger text-success');
            return;
        }

        $feedback.removeClass('text-danger text-success').text(appMessage('email.checking'));

        $.getJSON('/api/users/email-available', {email: email})
            .done(function (result) {
                $feedback
                    .toggleClass('text-success', result.available)
                    .toggleClass('text-danger', !result.available)
                    .text(result.message);
            })
            .fail(function () {
                $feedback.empty().removeClass('text-danger text-success');
            });
    });
}

/* ---------------------------------------------------------------------------
   Trang thanh toán: xem trước mã giảm giá
   --------------------------------------------------------------------------- */

/**
 * Bấm "Áp dụng" thì hỏi máy chủ xem mã có dùng được không và giảm bao nhiêu.
 *
 * Đây CHỈ là xem trước. Số tiền thật do BookingService tính lại lúc ghi đơn, và
 * nó kiểm mã một lần nữa - giữa lúc xem trước và lúc bấm đặt, mã có thể đã hết
 * lượt. Vì vậy tắt JavaScript đi thì ô nhập mã vẫn hoạt động: mã được gửi kèm
 * biểu mẫu như một trường bình thường.
 *
 * Mã sai trả về 409 kèm câu tiếng Việt sẵn trong body, nên chỗ này không tự
 * dịch mã lỗi - cứ hiện thẳng message của máy chủ.
 */
function initCouponCheck() {
    const $box = $('[data-coupon-box]');
    if ($box.length === 0) {
        return;
    }

    const $input = $box.find('#couponCode');
    const $message = $box.find('[data-coupon-message]');
    const $row = $('[data-coupon-discount-row]');
    const $discount = $('[data-coupon-discount]');
    const $total = $('[data-coupon-total]');

    $box.find('[data-coupon-apply]').on('click', function () {
        const code = ($input.val() || '').trim();
        if (code.length === 0) {
            $message.removeClass('text-success text-danger').empty();
            $row.addClass('d-none');
            return;
        }

        const $button = $(this).prop('disabled', true);
        $message.removeClass('text-success text-danger').text('...');

        $.getJSON('/api/promotions/check', {code: code})
            .done(function (result) {
                $message.removeClass('text-danger').addClass('text-success').text(result.name);
                $discount.text('- ' + formatMoney(result.discount));
                $total.text(formatMoney(result.totalAfter));
                $row.removeClass('d-none');
            })
            .fail(function (xhr) {
                const body = xhr.responseJSON || {};
                $message.removeClass('text-success').addClass('text-danger')
                    .text(body.message || appMessage('error.generic'));
                $row.addClass('d-none');
                $total.text(formatMoney($total.data('base')));
            })
            .always(function () {
                $button.prop('disabled', false);
            });
    });

    // Nhớ tổng ban đầu để khi mã bị từ chối thì trả con số về đúng như cũ.
    $total.data('base', ($total.text() || '').replace(/[^\d]/g, ''));
}
