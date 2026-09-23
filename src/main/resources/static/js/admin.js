
$(function () {

    $('form[data-confirm]').not('[data-ajax-delete]').on('submit', function (e) {
        if (!window.confirm($(this).data('confirm'))) {
            e.preventDefault();

            $(this).find('button[type="submit"]').prop('disabled', false);
        }
    });

    $('#departureDate').on('change', function () {
        const $return = $('#returnDate');
        if (this.value && !$return.val()) {
            $return.val(this.value);
        }
        $return.attr('min', this.value);
    });

    initAjaxDelete();
    initRevenueChart();
});

function initAjaxDelete() {
    $('form[data-ajax-delete]').on('submit', function (e) {
        e.preventDefault();

        const $form = $(this);
        const message = $form.data('confirm');
        if (message && !window.confirm(message)) {
            return;
        }

        const $button = $form.find('button[type="submit"]');
        $button.prop('disabled', true);

        $.ajax({
            url: $form.data('ajax-delete'),
            type: 'DELETE'
        }).done(function () {

            showToast(appMessage('deleted'), 'success');
            $form.closest('tr').fadeOut(300, function () {
                $(this).remove();
            });
        }).fail(function (jqXHR) {
            showToast(apiErrorMessage(jqXHR), 'danger');
            $button.prop('disabled', false);
        });
    });
}

function initRevenueChart() {
    const $chart = $('#revenueChart');
    if ($chart.length === 0) {
        return;
    }

    loadRevenueChart($chart);
    $('#reloadRevenue').on('click', function () {
        loadRevenueChart($chart);
    });
}

function loadRevenueChart($chart) {
    $.getJSON('/api/admin/stats/revenue')
        .done(function (points) {
            drawRevenueChart($chart, points);
        })
        .fail(function (jqXHR) {
            showToast(apiErrorMessage(jqXHR), 'danger');
        });
}

function paletteColor(name, fallback) {
    const value = getComputedStyle(document.documentElement)
        .getPropertyValue(name).trim();
    return value || fallback;
}

function compactMoney(amount) {
    try {
        return new Intl.NumberFormat(appLocale(), {
            notation: 'compact',
            maximumFractionDigits: 1
        }).format(Number(amount || 0));
    } catch (e) {

        return String(Math.round(Number(amount || 0)));
    }
}

function drawRevenueChart($chart, points) {
    $chart.empty();

    if (!points || points.length === 0) {
        $chart.append($('<p class="text-muted small mb-0"></p>').text(appMessage('chart.empty')));
        return;
    }

    const width = 1000;
    const height = 300;
    const paddingBottom = 40;
    const paddingTop = 26;
    const plotHeight = height - paddingBottom;
    const plotSpan = plotHeight - paddingTop;

    const jade = paletteColor('--jade-600', '#1B7A6B');
    const jadeDeep = paletteColor('--jade-800', '#0E4F47');
    const hairline = paletteColor('--sand-300', '#DFD2BD');
    const inkSoft = paletteColor('--ink-500', '#55635E');

    const slot = Math.min(width / points.length, 110);
    const groupLeft = (width - slot * points.length) / 2;
    const barWidth = Math.min(slot * 0.52, 46);

    const svg = svgElement('svg');
    svg.setAttribute('viewBox', '0 0 ' + width + ' ' + height);
    svg.setAttribute('class', 'w-100');
    svg.setAttribute('role', 'img');

    [0, 1 / 3, 2 / 3].forEach(function (ratio) {
        const y = paddingTop + plotSpan * ratio;
        const grid = svgElement('line');
        grid.setAttribute('x1', 0);
        grid.setAttribute('y1', y);
        grid.setAttribute('x2', width);
        grid.setAttribute('y2', y);
        grid.setAttribute('stroke', hairline);
        grid.setAttribute('stroke-width', '1');
        grid.setAttribute('stroke-dasharray', '4 7');
        svg.appendChild(grid);
    });

    const axis = svgElement('line');
    axis.setAttribute('x1', 0);
    axis.setAttribute('y1', plotHeight);
    axis.setAttribute('x2', width);
    axis.setAttribute('y2', plotHeight);
    axis.setAttribute('stroke', hairline);
    axis.setAttribute('stroke-width', '2');
    svg.appendChild(axis);

    points.forEach(function (point, index) {
        const centerX = groupLeft + slot * index + slot / 2;

        const barHeight = Math.max(plotSpan * point.percentOfMax / 100, 3);
        const barTop = plotHeight - barHeight;

        const bar = svgElement('rect');
        bar.setAttribute('x', centerX - barWidth / 2);
        bar.setAttribute('y', barTop);
        bar.setAttribute('width', barWidth);
        bar.setAttribute('height', barHeight);
        bar.setAttribute('rx', 6);

        bar.setAttribute('fill', point.percentOfMax >= 99.5 ? jadeDeep : jade);

        const tooltip = svgElement('title');
        tooltip.textContent = point.label + ': ' + formatMoney(point.amount);
        bar.appendChild(tooltip);
        svg.appendChild(bar);

        if (points.length <= 6 && Number(point.amount) > 0) {
            const value = svgElement('text');
            value.setAttribute('x', centerX);
            value.setAttribute('y', barTop - 9);
            value.setAttribute('text-anchor', 'middle');
            value.setAttribute('font-size', '17');
            value.setAttribute('font-weight', '600');
            value.setAttribute('fill', jadeDeep);
            value.textContent = compactMoney(point.amount);
            svg.appendChild(value);
        }

        const label = svgElement('text');
        label.setAttribute('x', centerX);
        label.setAttribute('y', height - 14);
        label.setAttribute('text-anchor', 'middle');
        label.setAttribute('font-size', '17');
        label.setAttribute('fill', inkSoft);
        label.textContent = point.label;
        svg.appendChild(label);
    });

    $chart.append(svg);
}

function svgElement(name) {
    return document.createElementNS('http://www.w3.org/2000/svg', name);
}
