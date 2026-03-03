// fetch("../layout/sidebar.html")
//     .then(res => res.text())
//     .then(data => {

//         document.getElementById("sidebar").innerHTML = data;

//         setActiveMenu();

//     });

// function setActiveMenu() {

//     const path = window.location.pathname;

//     let page = "";

//     if (path.includes("dashboard")) page = "dashboard";
//     if (path.includes("user")) page = "user";
//     if (path.includes("novel")) page = "novel";
//     if (path.includes("genre")) page = "genre";

//     const menuItems = document.querySelectorAll(".menu li");

//     menuItems.forEach(li => {

//         if (li.dataset.page === page) {
//             li.classList.add("active");
//         }

//     });

// }

fetch("../layout/sidebar.html")
    .then(res => res.text())
    .then(data => {

        document.getElementById("sidebar").innerHTML = data;

        setActiveMenu();

    });


function setActiveMenu() {

    const currentPath = window.location.pathname;

    const menuLinks = document.querySelectorAll(".menu li a");

    menuLinks.forEach(link => {

        const href = link.getAttribute("href");

        if (href && currentPath.includes(href.split("/").pop())) {

            link.parentElement.classList.add("active");

        }

    });

}