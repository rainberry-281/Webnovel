document.addEventListener("DOMContentLoaded", function() {

    const sidebar = document.querySelector(".rd_sidebar");
    const toggleBtn = document.getElementById("rd-info-btn");
    const overlay = document.querySelector(".black-click");

    function openSidebar() {
        sidebar.classList.add("on");
        sidebar.setAttribute("aria-hidden", "false");
    }

    function closeSidebar() {
        sidebar.classList.remove("on");
        sidebar.setAttribute("aria-hidden", "true");
    }

    function toggleSidebar() {
        sidebar.classList.toggle("on");

        const isOpen = sidebar.classList.contains("on");
        sidebar.setAttribute("aria-hidden", !isOpen);
    }

    // Click icon
    toggleBtn.addEventListener("click", toggleSidebar);

    // Click nền đen
    overlay.addEventListener("click", closeSidebar);

    // Nhấn ESC để đóng
    document.addEventListener("keydown", function(e) {
        if (e.key === "Escape") {
            closeSidebar();
        }
    });

});