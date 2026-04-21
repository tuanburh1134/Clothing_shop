(function () {
    const statusButtons = Array.from(document.querySelectorAll('[data-order-tab]'));
    const panels = Array.from(document.querySelectorAll('[data-order-panel]'));

    if (statusButtons.length > 0 && panels.length > 0) {
        const showPanel = function (panelKey) {
            statusButtons.forEach(function (button) {
                const active = button.getAttribute('data-order-tab') === panelKey;
                button.classList.toggle('is-active', active);
            });

            panels.forEach(function (panel) {
                const active = panel.getAttribute('data-order-panel') === panelKey;
                panel.hidden = !active;
            });
        };

        statusButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                showPanel(button.getAttribute('data-order-tab'));
            });
        });

        const activeButton = statusButtons.find(function (button) {
            return button.classList.contains('is-active');
        });
        showPanel(activeButton ? activeButton.getAttribute('data-order-tab') : 'pending');
    }

    document.querySelectorAll('.js-open-cancel-modal').forEach(function (button) {
        button.addEventListener('click', function () {
            const card = button.closest('.order-card');
            if (!card) {
                return;
            }

            const modal = card.querySelector('.js-cancel-modal');
            if (!modal) {
                return;
            }

            modal.hidden = false;
        });
    });

    document.querySelectorAll('.js-close-cancel-modal').forEach(function (button) {
        button.addEventListener('click', function () {
            const modal = button.closest('.js-cancel-modal');
            if (!modal) {
                return;
            }

            modal.hidden = true;
        });
    });

    document.querySelectorAll('.confirm-received-form').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            const ok = window.confirm('Bạn xác nhận đã nhận được hàng cho đơn này?');
            if (!ok) {
                event.preventDefault();
            }
        });
    });

    const skipExpandSelector = 'button, a, input, textarea, select, label, form';

    document.querySelectorAll('.js-expandable-order').forEach(function (card) {
        card.addEventListener('click', function (event) {
            const target = event.target;
            if (target instanceof Element && target.closest(skipExpandSelector)) {
                return;
            }

            const beforeTop = card.getBoundingClientRect().top;
            const alreadyExpanded = card.classList.contains('is-expanded');
            document.querySelectorAll('.js-expandable-order.is-expanded').forEach(function (expandedCard) {
                expandedCard.classList.remove('is-expanded');
            });

            if (!alreadyExpanded) {
                card.classList.add('is-expanded');
            }

            const afterTop = card.getBoundingClientRect().top;
            window.scrollBy(0, afterTop - beforeTop);
        });
    });

    document.querySelectorAll('.js-notification-card').forEach(function (card) {
        const href = card.getAttribute('data-href');
        if (!href) {
            return;
        }

        card.addEventListener('click', function () {
            window.location.href = href;
        });

        card.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                window.location.href = href;
            }
        });
    });

    document.querySelectorAll('.review-form').forEach(function (form) {
        const input = form.querySelector('.js-rating-input');
        const starsWrap = form.querySelector('.js-rating-stars');
        const stars = Array.from(form.querySelectorAll('.rating-star'));

        if (!input || !starsWrap || stars.length === 0) {
            return;
        }

        const renderStars = function (rating) {
            stars.forEach(function (star) {
                const value = Number(star.getAttribute('data-rating-value'));
                star.classList.toggle('is-active', value <= rating);
            });
        };

        stars.forEach(function (star) {
            star.addEventListener('click', function () {
                const value = Number(star.getAttribute('data-rating-value'));
                if (!Number.isFinite(value) || value < 1 || value > 5) {
                    return;
                }

                input.value = String(value);
                renderStars(value);
            });

            star.addEventListener('mouseenter', function () {
                const value = Number(star.getAttribute('data-rating-value'));
                renderStars(value);
            });
        });

        starsWrap.addEventListener('mouseleave', function () {
            const current = Number(input.value || '5');
            renderStars(Number.isFinite(current) && current >= 1 ? current : 5);
        });

        const initial = Number(input.value || '5');
        renderStars(Number.isFinite(initial) && initial >= 1 ? initial : 5);
    });
})();
