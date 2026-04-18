(function () {
    const mainImage = document.getElementById('mainGalleryImage');
    const thumbs = Array.from(document.querySelectorAll('.gallery-thumb'));
    const prevBtn = document.querySelector('.gallery-nav.prev');
    const nextBtn = document.querySelector('.gallery-nav.next');
    const buyNowModal = document.getElementById('buyNowModal');
    const openBuyNowBtn = document.getElementById('openBuyNowBtn');
    const cancelBuyNowBtn = document.getElementById('cancelBuyNowBtn');
    const addToCartForms = Array.from(document.querySelectorAll('.js-add-to-cart-form'));
    const cartButton = document.getElementById('headerCartBtn');

    if (!mainImage || thumbs.length === 0) {
        return;
    }

    let currentIndex = 0;

    function activate(index) {
        currentIndex = (index + thumbs.length) % thumbs.length;
        const currentThumb = thumbs[currentIndex];
        mainImage.src = currentThumb.dataset.image;
        thumbs.forEach((thumb, idx) => {
            thumb.classList.toggle('active', idx === currentIndex);
        });
    }

    thumbs.forEach((thumb, index) => {
        thumb.addEventListener('click', function () {
            activate(index);
        });
    });

    if (prevBtn) {
        prevBtn.addEventListener('click', function () {
            activate(currentIndex - 1);
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener('click', function () {
            activate(currentIndex + 1);
        });
    }

    activate(0);

    if (buyNowModal && openBuyNowBtn && cancelBuyNowBtn) {
        buyNowModal.hidden = true;

        openBuyNowBtn.addEventListener('click', function () {
            buyNowModal.hidden = false;
        });

        cancelBuyNowBtn.addEventListener('click', function () {
            buyNowModal.hidden = true;
        });

        buyNowModal.addEventListener('click', function (event) {
            if (event.target === buyNowModal) {
                buyNowModal.hidden = true;
            }
        });
    }

    addToCartForms.forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!mainImage || !cartButton) {
                return;
            }

            event.preventDefault();
            runFlyToCart(mainImage, cartButton, function () {
                form.submit();
            });
        });
    });

    function runFlyToCart(sourceImage, targetButton, done) {
        const sourceRect = sourceImage.getBoundingClientRect();
        const targetRect = targetButton.getBoundingClientRect();

        const ghost = sourceImage.cloneNode(true);
        ghost.classList.add('fly-cart-ghost');
        ghost.style.left = sourceRect.left + 'px';
        ghost.style.top = sourceRect.top + 'px';
        ghost.style.width = sourceRect.width + 'px';
        ghost.style.height = sourceRect.height + 'px';
        ghost.style.transition = 'transform 0.65s cubic-bezier(0.19, 1, 0.22, 1), opacity 0.65s ease';

        document.body.appendChild(ghost);

        const dx = targetRect.left + targetRect.width / 2 - (sourceRect.left + sourceRect.width / 2);
        const dy = targetRect.top + targetRect.height / 2 - (sourceRect.top + sourceRect.height / 2);

        requestAnimationFrame(function () {
            ghost.style.transform = 'translate(' + dx + 'px, ' + dy + 'px) scale(0.12)';
            ghost.style.opacity = '0.2';
        });

        window.setTimeout(function () {
            ghost.remove();
            done();
        }, 680);
    }
})();
