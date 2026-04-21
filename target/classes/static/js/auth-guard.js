(function () {
    var isAuthenticated = Boolean(window.__SHOP_AUTHENTICATED__);
    if (isAuthenticated) {
        return;
    }

    var modal = document.getElementById('loginRequiredModal');
    var cancelBtn = document.getElementById('loginRequiredCancelBtn');
    var confirmBtn = document.getElementById('loginRequiredConfirmBtn');
    if (!modal || !cancelBtn || !confirmBtn) {
        return;
    }

    var pendingUrl = '/auth';
    var protectedPathPatterns = [
        /^\/account(?:\/|$)/,
        /^\/cart(?:\/|$)/,
        /^\/checkout(?:\/|$)/,
        /^\/chat(?:\/|$)/,
        /^\/products\/\d+\/(buy-now|cart)(?:\/|$)/
    ];

    function isProtectedPath(pathname) {
        return protectedPathPatterns.some(function (pattern) {
            return pattern.test(pathname || '');
        });
    }

    function toUrl(target) {
        try {
            return new URL(target, window.location.origin);
        } catch (e) {
            return null;
        }
    }

    function openModal(nextUrl) {
        pendingUrl = '/auth';
        if (nextUrl) {
            pendingUrl = '/auth?redirect=' + encodeURIComponent(nextUrl);
        }

        confirmBtn.setAttribute('href', pendingUrl);
        modal.classList.add('is-open');
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
    }

    function closeModal() {
        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
    }

    cancelBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', function (event) {
        if (event.target === modal) {
            closeModal();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && modal.classList.contains('is-open')) {
            closeModal();
        }
    });

    document.addEventListener('click', function (event) {
        var trigger = event.target.closest('[data-requires-auth], a[href]');
        if (!trigger) {
            return;
        }

        if (trigger.hasAttribute('data-requires-auth')) {
            event.preventDefault();
            event.stopImmediatePropagation();
            openModal(window.location.pathname + window.location.search);
            return;
        }

        if (trigger.tagName !== 'A') {
            return;
        }

        var href = trigger.getAttribute('href');
        if (!href || href.startsWith('#') || href.startsWith('javascript:')) {
            return;
        }

        var url = toUrl(href);
        if (!url || url.origin !== window.location.origin) {
            return;
        }

        if (isProtectedPath(url.pathname)) {
            event.preventDefault();
            event.stopImmediatePropagation();
            openModal(url.pathname + url.search);
        }
    }, true);

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }

        if (!form.action) {
            return;
        }

        var actionUrl = toUrl(form.action);
        if (!actionUrl || actionUrl.origin !== window.location.origin) {
            return;
        }

        if (form.hasAttribute('data-requires-auth') || isProtectedPath(actionUrl.pathname)) {
            event.preventDefault();
            event.stopImmediatePropagation();
            openModal(actionUrl.pathname + actionUrl.search);
        }
    }, true);
})();
