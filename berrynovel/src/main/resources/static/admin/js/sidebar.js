function setActiveMenu() {
    // Simple test first
    alert("setActiveMenu called");

    const currentPath = window.location.pathname;
    console.log("Current path:", currentPath);

    // Check if sidebar exists
    const sidebar = document.getElementById("sidebar");
    console.log("Sidebar element:", sidebar);

    if (!sidebar) {
        console.error("Sidebar not found!");
        return;
    }

    const menuLinks = document.querySelectorAll(".menu li a");
    console.log("Menu links found:", menuLinks.length);

    if (menuLinks.length === 0) {
        console.error("No menu links found!");
        return;
    }

    menuLinks.forEach((link, index) => {
        const href = link.getAttribute("href");
        console.log(`Link ${index}:`, href, "- Current path:", currentPath);

        // Remove active class first
        link.parentElement.classList.remove("active");

        if (href && href !== "") {
            // Simple exact match first
            if (currentPath === href) {
                console.log("EXACT MATCH:", href);
                link.parentElement.classList.add("active");
            } else if (currentPath.includes(href.replace(/\/$/, ""))) {
                console.log("CONTAINS MATCH:", href);
                link.parentElement.classList.add("active");
            }
        } else {
            console.log(`Link ${index} has no href or empty href`);
        }
    });
}

// Call setActiveMenu immediately
console.log("sidebar.js loaded");
setActiveMenu();

// Also call it on page show in case of browser back/forward
window.addEventListener("pageshow", function () {
    console.log("Page show event");
    setActiveMenu();
});