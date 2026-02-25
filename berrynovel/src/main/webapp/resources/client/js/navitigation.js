let lastScroll = 0;

window.addEventListener("scroll", function () {

    const navbar = document.getElementById("mainNavbar");
    if (!navbar) return; // nếu chưa load thì bỏ qua

    let currentScroll = window.pageYOffset || document.documentElement.scrollTop;

    if (currentScroll > lastScroll && currentScroll > 100) {
        navbar.classList.add("navbar-hide");
    } else {
        navbar.classList.remove("navbar-hide");
    }

    lastScroll = currentScroll <= 0 ? 0 : currentScroll;
});