(function () {
    const form = document.getElementById('inventoryForm');
    const container = document.getElementById('variantContainer');
    const addVariantBtn = document.getElementById('addVariantBtn');
    const variantDataField = document.getElementById('variantDataField');
    const totalQuantityField = document.getElementById('totalQuantityField');
    const totalQuantityPreview = document.getElementById('totalQuantityPreview');
    const priceDisplay = document.getElementById('priceDisplay');
    const priceValue = document.getElementById('priceValue');

    if (!form || !container || !addVariantBtn || !variantDataField || !totalQuantityField) {
        return;
    }

    function formatPrice(value) {
        const digits = (value || '').toString().replace(/\D/g, '');
        if (!digits) {
            return '';
        }
        return Number(digits).toLocaleString('vi-VN');
    }

    function createVariantBlock(data, expanded) {
        const isExpanded = expanded === true;
        const wrapper = document.createElement('div');
        wrapper.className = 'variant-editor';
        wrapper.innerHTML = `
            <div class="variant-editor-head">
                <input type="text" class="variant-color" placeholder="Nhập màu sắc" value="${data.color || ''}">
                <button type="button" class="toggle-sizes">v</button>
                <button type="button" class="remove-variant">×</button>
            </div>
            <div class="size-grid ${isExpanded ? '' : 'collapsed'}">
                <label>S <input type="number" class="size-input" data-size="S" min="0" value="${data.s || 0}"></label>
                <label>M <input type="number" class="size-input" data-size="M" min="0" value="${data.m || 0}"></label>
                <label>L <input type="number" class="size-input" data-size="L" min="0" value="${data.l || 0}"></label>
                <label>XL <input type="number" class="size-input" data-size="XL" min="0" value="${data.xl || 0}"></label>
                <label>XXL <input type="number" class="size-input" data-size="XXL" min="0" value="${data.xxl || 0}"></label>
            </div>
        `;

        wrapper.querySelector('.toggle-sizes').addEventListener('click', function () {
            const sizeGrid = wrapper.querySelector('.size-grid');
            sizeGrid.classList.toggle('collapsed');
        });

        wrapper.querySelector('.remove-variant').addEventListener('click', function () {
            wrapper.remove();
            syncVariantData();
        });

        wrapper.querySelectorAll('input').forEach(function (input) {
            input.addEventListener('input', syncVariantData);
        });

        container.appendChild(wrapper);
    }

    function syncVariantData() {
        let total = 0;
        const parts = [];

        container.querySelectorAll('.variant-editor').forEach(function (block) {
            const color = block.querySelector('.variant-color').value.trim();
            if (!color) {
                return;
            }

            const sizeValues = [];
            block.querySelectorAll('.size-input').forEach(function (input) {
                const value = parseInt(input.value || '0', 10) || 0;
                total += value;
                sizeValues.push(`${input.dataset.size}=${value}`);
            });

            parts.push(`${color}::${sizeValues.join(',')}`);
        });

        variantDataField.value = parts.join(';;');
        totalQuantityField.value = total;
        totalQuantityPreview.textContent = total;
    }

    addVariantBtn.addEventListener('click', function () {
        createVariantBlock({}, false);
    });

    if (priceDisplay && priceValue) {
        priceDisplay.value = formatPrice(priceValue.value);
        priceDisplay.addEventListener('input', function () {
            const digits = this.value.replace(/\D/g, '');
            this.value = formatPrice(digits);
            priceValue.value = digits ? digits : '';
        });
    }

    const rawVariants = form.dataset.variants || '';
    if (rawVariants.trim()) {
        rawVariants.split(';;').forEach(function (entry) {
            const pieces = entry.split('::');
            const color = pieces[0] || '';
            const sizes = { color: color, s: 0, m: 0, l: 0, xl: 0, xxl: 0 };

            if (pieces[1]) {
                pieces[1].split(',').forEach(function (sizeEntry) {
                    const pair = sizeEntry.split('=');
                    const key = (pair[0] || '').toLowerCase();
                    const value = parseInt(pair[1] || '0', 10) || 0;
                    if (key === 's') sizes.s = value;
                    if (key === 'm') sizes.m = value;
                    if (key === 'l') sizes.l = value;
                    if (key === 'xl') sizes.xl = value;
                    if (key === 'xxl') sizes.xxl = value;
                });
            }

            const hasStock = sizes.s > 0 || sizes.m > 0 || sizes.l > 0 || sizes.xl > 0 || sizes.xxl > 0;
            createVariantBlock(sizes, hasStock);
        });
    }

    if (container.children.length === 0) {
        createVariantBlock({}, false);
    }

    form.addEventListener('submit', function () {
        syncVariantData();
        if (priceDisplay && priceValue) {
            priceValue.value = priceDisplay.value.replace(/\D/g, '');
        }
    });

    syncVariantData();
})();
