(() => {
	const carousel = document.querySelector('.ad-carousel');
	if (!carousel) {
		return;
	}

	const slides = Array.from(carousel.querySelectorAll('.ad-slide'));
	const dots = Array.from(carousel.querySelectorAll('.ad-dot'));
	const prevBtn = carousel.querySelector('.ad-control.prev');
	const nextBtn = carousel.querySelector('.ad-control.next');

	if (slides.length <= 1) {
		return;
	}

	let activeIndex = Math.max(0, slides.findIndex(slide => slide.classList.contains('is-active')));
	if (activeIndex === -1) {
		activeIndex = 0;
	}

	let autoTimer;

	const render = () => {
		slides.forEach((slide, index) => {
			slide.classList.toggle('is-active', index === activeIndex);
		});

		dots.forEach((dot, index) => {
			dot.classList.toggle('is-active', index === activeIndex);
			dot.setAttribute('aria-current', index === activeIndex ? 'true' : 'false');
		});
	};

	const goTo = (index) => {
		activeIndex = (index + slides.length) % slides.length;
		render();
	};

	const next = () => goTo(activeIndex + 1);
	const prev = () => goTo(activeIndex - 1);

	const restartAutoPlay = () => {
		if (autoTimer) {
			window.clearInterval(autoTimer);
		}

		autoTimer = window.setInterval(next, 5000);
	};

	if (prevBtn) {
		prevBtn.addEventListener('click', () => {
			prev();
			restartAutoPlay();
		});
	}

	if (nextBtn) {
		nextBtn.addEventListener('click', () => {
			next();
			restartAutoPlay();
		});
	}

	dots.forEach((dot, index) => {
		dot.addEventListener('click', () => {
			goTo(index);
			restartAutoPlay();
		});
	});

	carousel.addEventListener('mouseenter', () => {
		if (autoTimer) {
			window.clearInterval(autoTimer);
		}
	});

	carousel.addEventListener('mouseleave', restartAutoPlay);

	render();
	restartAutoPlay();
})();
