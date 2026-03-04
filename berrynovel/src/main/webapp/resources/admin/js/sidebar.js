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

        if (href && currentPath.includes(href.split("/")[1])) {
            link.parentElement.classList.add("active");
        }

    });

}

// fetch("../layout/sidebar.html")
//     .then(res => res.text())
//     .then(data => {

//         document.getElementById("sidebar").innerHTML = data;

//         setActiveMenu();

//     });


// function setActiveMenu() {

//     const currentPath = window.location.pathname;

//     const menuLinks = document.querySelectorAll(".menu li a");

//     menuLinks.forEach(link => {

//         const href = link.getAttribute("href");

//         if (href && currentPath.includes(href.split("/").pop())) {

//             link.parentElement.classList.add("active");

//         }

//     });

// }