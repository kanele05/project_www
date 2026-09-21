/* ===========================================================================
   Kịch bản riêng của khu vực quản trị.

   Dùng lại các hàm tiện ích của app.js (showToast, apiErrorMessage, formatMoney,
   appMessage) - admin-layout.html nạp app.js trước file này.
   =========================================================================== */
$(function () {

    // Hỏi lại trước khi xoá. Đây chỉ là lớp nhắc nhở cho đỡ lỡ tay - việc chặn
    // thật sự nằm ở tầng service (bốn quy tắc chặn xoá), vì người dùng hoàn toàn
    // có thể tắt JavaScript.
    // Loại trừ biểu mẫu xoá bằng AJAX: nó tự hỏi lấy, để cả hai cùng hỏi thì
    // người dùng phải xác nhận hai lần.
    $('form[data-confirm]').not('[data-ajax-delete]').on('submit', function (e) {
        if (!window.confirm($(this).data('confirm'))) {
            e.preventDefault();
            // Nút gửi đã bị app.js vô hiệu hoá, phải bật lại nếu không người dùng
            // sẽ không bấm được lần nữa.
            $(this).find('button[type="submit"]').prop('disabled', false);
        }
    });

    // Ngày về mặc định nhảy theo ngày khởi hành cho đỡ phải chọn hai lần.
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

/* ---------------------------------------------------------------------------
   Xoá bằng AJAX
   --------------------------------------------------------------------------- */

/**
 * Xoá một bản ghi mà không nạp lại cả trang.
 *
 * Đây là chỗ dùng đến mã trạng thái 409 rõ nhất: bấm xoá một danh mục còn tour
 * thì máy chủ trả về 409 Conflict kèm câu giải thích, người dùng thấy ngay tại
 * chỗ chứ không phải chờ tải lại toàn bộ bảng để đọc một dòng thông báo. Nếu để
 * khoá ngoại của CSDL chặn thì phản hồi sẽ là 500 kèm một câu lỗi SQL, chẳng nói
 * được gì cho người dùng.
 *
 * Nút xoá vẫn nằm trong một biểu mẫu POST thật; tắt JavaScript thì nó gửi theo
 * cách thường và máy chủ chuyển hướng kèm thông báo như trước.
 */
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
            // 204 No Content: không có gì để đọc, chỉ việc gỡ dòng khỏi bảng.
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

/* ---------------------------------------------------------------------------
   Biểu đồ doanh thu
   --------------------------------------------------------------------------- */

/**
 * Vẽ biểu đồ doanh thu 12 tháng từ /api/admin/stats/revenue.
 *
 * Vẽ bằng SVG dựng tay chứ không dùng thư viện biểu đồ: dự án đang xây dựng
 * ngoại tuyến (mvn -o) nên thêm một phụ thuộc mới là hỏng bản dựng, mà một biểu
 * đồ cột thì cũng chỉ là mấy hình chữ nhật. Đổi lại, không có gì trong đây là
 * hộp đen khi cần giải thích lúc vấn đáp.
 *
 * Chiều cao mỗi cột đã được StatisticsService tính sẵn theo phần trăm so với
 * tháng cao nhất (percentOfMax): phép tính ấy thuộc về tầng nghiệp vụ, và
 * Thymeleaf cũng không có hàm lấy giá trị lớn nhất của một danh sách.
 */
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

/**
 * Đọc một token màu ra khỏi bảng màu trong app.css.
 *
 * Bản đầu viết cứng '#198754' (xanh lá mặc định của Bootstrap), '#dee2e6' và
 * '#6c757d' vào đây. Hậu quả: khối nổi bật nhất của bảng điều khiển là thứ DUY
 * NHẤT trên cả website không thuộc bảng màu "Ngọc bích & Đất nung" - nhìn vào là
 * thấy lạc ngay, mà sửa token ở app.css cũng không ăn thua vì màu nằm trong .js.
 *
 * SVG không nhận var(--x) ở thuộc tính fill/stroke một cách đáng tin trên mọi
 * trình duyệt, nên đọc giá trị thật ra rồi gán bằng số.
 */
function paletteColor(name, fallback) {
    const value = getComputedStyle(document.documentElement)
        .getPropertyValue(name).trim();
    return value || fallback;
}

/** Rút gọn số tiền cho nhãn trên đầu cột: 61255000 -> "61 Tr" / "61M". */
function compactMoney(amount) {
    try {
        return new Intl.NumberFormat(appLocale(), {
            notation: 'compact',
            maximumFractionDigits: 1
        }).format(Number(amount || 0));
    } catch (e) {
        // Intl thiếu notation compact ở vài trình duyệt cũ: thà hiện số đầy đủ
        // còn hơn để trống chỗ.
        return String(Math.round(Number(amount || 0)));
    }
}

function drawRevenueChart($chart, points) {
    $chart.empty();

    if (!points || points.length === 0) {
        $chart.append($('<p class="text-muted small mb-0"></p>').text(appMessage('chart.empty')));
        return;
    }

    // Toạ độ dùng hệ quy chiếu cố định 1000 x 300 rồi để viewBox co giãn theo bề
    // rộng thật của thẻ chứa - nhờ vậy biểu đồ tự vừa khung trên mọi cỡ màn hình
    // mà không phải đo đạc gì bằng JavaScript.
    const width = 1000;
    const height = 300;
    const paddingBottom = 40;
    const paddingTop = 26;              // chừa chỗ cho nhãn số tiền trên đầu cột
    const plotHeight = height - paddingBottom;
    const plotSpan = plotHeight - paddingTop;

    const jade = paletteColor('--jade-600', '#1B7A6B');
    const jadeDeep = paletteColor('--jade-800', '#0E4F47');
    const hairline = paletteColor('--sand-300', '#DFD2BD');
    const inkSoft = paletteColor('--ink-500', '#55635E');

    // ⚠️ Bề rộng một ô KHÔNG chia đều cả 1000 đơn vị nữa.
    // Dữ liệu thật của đồ án chỉ có hai tháng có doanh thu; chia đều thì hai cột
    // đứng cách nhau nửa màn hình, trông như biểu đồ hỏng. Chốt trần 110 đơn vị
    // cho một ô rồi căn giữa cả cụm: ít cột thì cụm nằm giữa, mười hai cột thì
    // vẫn trải kín như cũ.
    const slot = Math.min(width / points.length, 110);
    const groupLeft = (width - slot * points.length) / 2;
    const barWidth = Math.min(slot * 0.52, 46);

    const svg = svgElement('svg');
    svg.setAttribute('viewBox', '0 0 ' + width + ' ' + height);
    svg.setAttribute('class', 'w-100');
    svg.setAttribute('role', 'img');

    // Ba đường kẻ ngang mờ ở 1/3, 2/3 và đỉnh: mắt ước lượng được chiều cao
    // tương đối giữa các cột mà không cần trục số.
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

    // Đường kẻ chân biểu đồ - vẽ trước để các cột nằm đè lên.
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
        // Cột thấp nhất vẫn cao 3 đơn vị để tháng doanh thu 0 không biến mất hẳn.
        const barHeight = Math.max(plotSpan * point.percentOfMax / 100, 3);
        const barTop = plotHeight - barHeight;

        const bar = svgElement('rect');
        bar.setAttribute('x', centerX - barWidth / 2);
        bar.setAttribute('y', barTop);
        bar.setAttribute('width', barWidth);
        bar.setAttribute('height', barHeight);
        bar.setAttribute('rx', 6);
        // Tháng cao nhất đậm hơn hẳn: đọc ra "đỉnh" mà không cần chú giải.
        bar.setAttribute('fill', point.percentOfMax >= 99.5 ? jadeDeep : jade);

        // Số tiền hiện khi rê chuột - in hết vào biểu đồ thì chữ chồng lên nhau.
        const tooltip = svgElement('title');
        tooltip.textContent = point.label + ': ' + formatMoney(point.amount);
        bar.appendChild(tooltip);
        svg.appendChild(bar);

        // Nhãn số tiền chỉ in khi ít cột. Mười hai cột mà cột nào cũng có số thì
        // các nhãn dính vào nhau và không đọc được cái nào.
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

/**
 * Tạo một phần tử SVG.
 *
 * Bắt buộc dùng createElementNS với đúng không gian tên: tạo bằng
 * document.createElement('rect') thì trình duyệt coi đó là một thẻ HTML lạ, thêm
 * vào cây vẫn được nhưng không vẽ ra gì cả - lỗi im lặng khó đoán nhất khi làm
 * việc với SVG.
 */
function svgElement(name) {
    return document.createElementNS('http://www.w3.org/2000/svg', name);
}
