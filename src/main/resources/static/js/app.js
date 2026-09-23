
function appMessage(key) {
    return $('meta[name="msg.' + key + '"]').attr('content') || '';
}

function appLocale() {
    return document.documentElement.lang || 'vi';
}

function formatMoney(amount) {
    const number = Number(amount || 0);
    return number.toLocaleString(appLocale(), {maximumFractionDigits: 0})
        + ' ' + appMessage('currency');
}

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

function apiErrorMessage(jqXHR) {
    if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
        return jqXHR.responseJSON.message;
    }
    return appMessage('error.generic');
}

function updateCartBadge(cart) {
    const $badge = $('#cartBadge');
    if ($badge.length === 0) {
        return;
    }
    $badge.text(cart.itemCount);
    $badge.toggleClass('d-none', cart.itemCount === 0);
}

$(function () {

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

    window.setTimeout(function () {
        $('.alert-dismissible').fadeOut(400);
    }, 5000);

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

const REVEAL_FAILSAFE_MS = 2500;

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
        return;
    }

    document.documentElement.classList.add('reveal-ready');

    const failsafe = window.setTimeout(revealAll, REVEAL_FAILSAFE_MS);

    const observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');

                observer.unobserve(entry.target);
            }
        });
    }, {rootMargin: '0px 0px -8% 0px', threshold: .12});

    items.forEach(function (item) {

        if (item.getBoundingClientRect().top < window.innerHeight) {
            item.classList.add('is-visible');
        }
        observer.observe(item);
    });

    window.addEventListener('pagehide', function () {
        window.clearTimeout(failsafe);
    }, {once: true});
}

function prefersReducedMotion() {
    return window.matchMedia
        && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

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

function initCartForms() {
    $('form[data-ajax-cart]').on('submit', function (e) {
        e.preventDefault();

        const $form = $(this);
        const $button = $form.find('button[type="submit"]');

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

            if (pending) {
                pending.abort();
            }
            pending = $.getJSON('/api/tours/suggest', {q: keyword, limit: 6})
                .done(function (tours) {
                    renderSuggestions($panel, tours);
                })
                .fail(function (jqXHR) {

                    if (jqXHR.statusText !== 'abort') {
                        $panel.addClass('d-none').empty();
                    }
                });
        }, 250);
    });

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

        const $item = $('<a class="list-group-item list-group-item-action py-2"></a>')
            .attr('href', tour.detailUrl);

        $item.append($('<div class="small fw-semibold"></div>').text(tour.name));
        $item.append($('<div class="small text-muted"></div>')
            .text(tour.destination + ' - ' + formatMoney(tour.basePrice)));

        $panel.append($item);
    });
    $panel.removeClass('d-none');
}

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

    $total.data('base', ($total.text() || '').replace(/[^\d]/g, ''));
}
