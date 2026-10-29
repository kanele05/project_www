(function ($) {
    'use strict';

    if (navigator.doNotTrack === '1' || window.doNotTrack === '1') {
        return;
    }

    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    if (!csrfHeader || !csrfToken || !window.fetch) {
        return;
    }

    const $page = $('[data-track-view]').first();
    const pageTourId = $page.length ? Number($page.data('track-view')) : null;

    function track(type, extra) {
        const headers = {'Content-Type': 'application/json'};
        headers[csrfHeader] = csrfToken;
        const body = Object.assign({type: type, pageUrl: location.pathname}, extra || {});
        try {
            fetch('/api/tracking', {
                method: 'POST',
                keepalive: true,
                credentials: 'same-origin',
                headers: headers,
                body: JSON.stringify(body)
            }).catch(function () { /* tracking hỏng không được làm phiền người dùng */ });
        } catch (e) { /* bỏ qua */ }
    }

    if (pageTourId) {
        track('VIEW', {tourId: pageTourId});
    }

    $(document).on('click', '[data-track-tour] a[href]', function () {
        const tourId = Number($(this).closest('[data-track-tour]').data('track-tour'));
        if (tourId) {
            track('CLICK', {tourId: tourId});
        }
    });

    $('form').has('input[name="q"]').on('submit', function () {
        const keyword = ($(this).find('input[name="q"]').val() || '').trim();
        if (keyword.length >= 2) {
            track('SEARCH', {keyword: keyword});
        }
    });

    if (pageTourId) {
        let shownAt = document.hidden ? null : Date.now();
        let visibleMs = 0;

        const flush = function () {
            if (shownAt !== null) {
                visibleMs += Date.now() - shownAt;
                shownAt = null;
            }
            if (visibleMs >= 1000) {
                track('TIME_ON_PAGE', {tourId: pageTourId, durationMs: Math.round(visibleMs)});
            }
            visibleMs = 0;
        };

        document.addEventListener('visibilitychange', function () {
            if (document.hidden) {
                flush();
            } else {
                shownAt = Date.now();
            }
        });
        window.addEventListener('pagehide', flush);
    }
})(jQuery);
