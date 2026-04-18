(function () {
    const mainImage = document.getElementById('mainGalleryImage');
    const thumbs = Array.from(document.querySelectorAll('.gallery-thumb'));
    const prevBtn = document.querySelector('.gallery-nav.prev');
    const nextBtn = document.querySelector('.gallery-nav.next');
    const variantRows = Array.from(document.querySelectorAll('.variant-stock-row'));
    const colorOptions = document.getElementById('colorOptions');
    const sizeOptions = document.getElementById('sizeOptions');
    const variantSelectionHint = document.getElementById('variantSelectionHint');
    const buyNowForm = document.querySelector('.buy-now-form');
    const buyNowSelectedColor = document.getElementById('buyNowSelectedColor');
    const buyNowSelectedSize = document.getElementById('buyNowSelectedSize');
    const buyNowButton = buyNowForm ? buyNowForm.querySelector('button[type="submit"]') : null;
    const addToCartForms = Array.from(document.querySelectorAll('.js-add-to-cart-form'));
    const cartSelectedColor = document.getElementById('cartSelectedColor');
    const cartSelectedSize = document.getElementById('cartSelectedSize');
    const addToCartButton = addToCartForms.length > 0 ? addToCartForms[0].querySelector('button[type="submit"]') : null;
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

    const variantMap = buildVariantMap(variantRows);
    let selectedColor = '';
    let selectedSize = '';

    if (variantRows.length > 0 && colorOptions && sizeOptions) {
        renderColorButtons();
        updateActionState();
    }

    if (buyNowForm) {
        buyNowForm.addEventListener('submit', function (event) {
            if (!isSelectionReady()) {
                event.preventDefault();
                showHint('Vui lòng chọn màu sắc và size trước khi mua.');
            }
        });
    }

    addToCartForms.forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!isSelectionReady()) {
                event.preventDefault();
                showHint('Vui lòng chọn màu sắc và size trước khi thêm vào giỏ.');
                return;
            }

            if (!mainImage || !cartButton) {
                return;
            }

            event.preventDefault();
            runFlyToCart(mainImage, cartButton, function () {
                form.submit();
            });
        });
    });

    function buildVariantMap(rows) {
        const map = {};
        rows.forEach(function (row) {
            const color = (row.dataset.color || '').trim();
            if (!color) {
                return;
            }

            map[color] = {
                S: parseInt(row.dataset.s || '0', 10) || 0,
                M: parseInt(row.dataset.m || '0', 10) || 0,
                L: parseInt(row.dataset.l || '0', 10) || 0,
                XL: parseInt(row.dataset.xl || '0', 10) || 0,
                XXL: parseInt(row.dataset.xxl || '0', 10) || 0
            };
        });

        return map;
    }

    function renderColorButtons() {
        colorOptions.innerHTML = '';
        Object.keys(variantMap).forEach(function (color) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'variant-chip color-chip';
            button.textContent = color;

            button.addEventListener('click', function () {
                selectedColor = color;
                selectedSize = '';
                renderColorButtons();
                renderSizeButtons();
                updateActionState();
                showHint('Đã chọn màu ' + color + '. Hãy chọn size.');
            });

            if (selectedColor === color) {
                button.classList.add('selected');
            }

            colorOptions.appendChild(button);
        });
    }

    function renderSizeButtons() {
        sizeOptions.innerHTML = '';
        if (!selectedColor || !variantMap[selectedColor]) {
            return;
        }

        ['S', 'M', 'L', 'XL', 'XXL'].forEach(function (size) {
            const stock = variantMap[selectedColor][size] || 0;
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'variant-chip size-chip';
            button.textContent = size + ' (' + stock + ')';

            if (stock <= 0) {
                button.disabled = true;
                button.classList.add('disabled');
            } else {
                button.addEventListener('click', function () {
                    selectedSize = size;
                    renderSizeButtons();
                    updateActionState();
                    showHint('Đã chọn: ' + selectedColor + ' - ' + size + '.');
                });
            }

            if (selectedSize === size) {
                button.classList.add('selected');
            }

            sizeOptions.appendChild(button);
        });
    }

    function isSelectionReady() {
        return Boolean(selectedColor && selectedSize);
    }

    function updateActionState() {
        if (buyNowSelectedColor) {
            buyNowSelectedColor.value = selectedColor;
        }
        if (buyNowSelectedSize) {
            buyNowSelectedSize.value = selectedSize;
        }
        if (cartSelectedColor) {
            cartSelectedColor.value = selectedColor;
        }
        if (cartSelectedSize) {
            cartSelectedSize.value = selectedSize;
        }

        const canSubmit = isSelectionReady() || variantRows.length === 0;
        if (buyNowButton) {
            buyNowButton.disabled = !canSubmit;
        }
        if (addToCartButton) {
            addToCartButton.disabled = !canSubmit;
        }
    }

    function showHint(text) {
        if (!variantSelectionHint) {
            return;
        }
        variantSelectionHint.textContent = text;
    }

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
