(function () {
    const fillBtn = document.getElementById('fillCurrentAddressBtn');
    const addressField = document.getElementById('shippingAddressField');
    const phoneField = document.getElementById('phoneNumberField');

    if (!fillBtn || !addressField || !phoneField) {
        return;
    }

    fillBtn.addEventListener('click', function () {
        const address = (fillBtn.dataset.profileAddress || '').trim();
        const phone = (fillBtn.dataset.profilePhone || '').trim();

        if (address) {
            addressField.value = address;
        }

        if (phone) {
            phoneField.value = phone;
        }
    });
})();
