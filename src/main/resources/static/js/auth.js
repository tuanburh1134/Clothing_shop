(function () {
    const loginTab = document.getElementById('loginTab');
    const registerTab = document.getElementById('registerTab');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    if (!loginTab || !registerTab || !loginForm || !registerForm) {
        return;
    }

    function activate(tabName) {
        const isLogin = tabName === 'login';

        loginTab.classList.toggle('active', isLogin);
        registerTab.classList.toggle('active', !isLogin);
        loginForm.classList.toggle('active', isLogin);
        registerForm.classList.toggle('active', !isLogin);
    }

    loginTab.addEventListener('click', function () {
        activate('login');
    });

    registerTab.addEventListener('click', function () {
        activate('register');
    });

    const activeFromServer = window.AUTH_ACTIVE_TAB;
    activate(activeFromServer === 'register' ? 'register' : 'login');
})();
