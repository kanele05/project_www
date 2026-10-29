(function ($) {
    'use strict';

    const $root = $('#aiChat');
    if ($root.length === 0) {
        return;
    }

    const $panel = $('#aiChatPanel');
    const $toggle = $('#aiChatToggle');
    const $log = $('#aiChatLog');
    const $form = $('#aiChatForm');
    const $input = $('#aiChatInput');
    const enabled = String($root.data('enabled')) === 'true';

    function setOpen(open) {
        $panel.toggleClass('d-none', !open);
        $toggle.attr('aria-expanded', open ? 'true' : 'false');
        if (open && enabled) {
            $input.trigger('focus');
        }
    }

    $toggle.on('click', function () {
        setOpen($panel.hasClass('d-none'));
    });
    $('#aiChatClose').on('click', function () {
        setOpen(false);
        $toggle.trigger('focus');
    });
    $(document).on('keydown', function (e) {
        if (e.key === 'Escape' && !$panel.hasClass('d-none')) {
            setOpen(false);
        }
    });

    if (!enabled) {
        return;
    }

    function escapeHtml(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // Chỉ cho phép liên kết nội bộ /tours/... hoặc /cart, in đậm **...**, xuống dòng.
    function renderBot(text) {
        let html = escapeHtml(text);
        html = html.replace(/\[([^\]]+)\]\((\/(?:tours|cart)[^)\s]*)\)/g, '<a href="$2">$1</a>');
        html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        return html.replace(/\n/g, '<br>');
    }

    function addMessage(kind, text) {
        const $msg = $('<div class="ai-chat__msg"></div>').addClass('ai-chat__msg--' + kind);
        if (kind === 'bot') {
            $msg.html(renderBot(text));
        } else {
            $msg.text(text);
        }
        $log.append($msg);
        $log.scrollTop($log[0].scrollHeight);
        return $msg;
    }

    function send(text) {
        addMessage('user', text);
        const $pending = addMessage('bot', $root.data('msg-thinking')).addClass('ai-chat__msg--pending');
        $input.prop('disabled', true);

        $.ajax({
            url: '/api/chat',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({message: text})
        }).done(function (data) {
            $pending.removeClass('ai-chat__msg--pending').html(renderBot(data.reply));
        }).fail(function (jqXHR) {
            const message = jqXHR.responseJSON && jqXHR.responseJSON.message
                ? jqXHR.responseJSON.message : $root.data('msg-error');
            $pending.removeClass('ai-chat__msg--pending').addClass('ai-chat__msg--error').text(message);
        }).always(function () {
            $input.prop('disabled', false).trigger('focus');
            $log.scrollTop($log[0].scrollHeight);
        });
    }

    $form.on('submit', function (e) {
        e.preventDefault();
        const text = ($input.val() || '').trim();
        if (!text) {
            return;
        }
        $input.val('');
        send(text);
    });

    $('.ai-chat__chip').on('click', function () {
        setOpen(true);
        send($(this).text().trim());
    });
})(jQuery);
