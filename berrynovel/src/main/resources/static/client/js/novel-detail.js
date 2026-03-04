// Kết cấu của tóm tắt nội dung
document.addEventListener("DOMContentLoaded", function() {
    const intro = document.getElementById("introText");
    let content = intro.innerText;
    content = content.replace(/\. /g, ".<br>");
    intro.innerHTML = content;
});

// Bookmark
const icon = document.querySelector('.bookmark');

icon.addEventListener('click', function() {
    this.classList.toggle('active');

    if (this.classList.contains('active')) {
        this.classList.remove('fa-regular');
        this.classList.add('fa-solid');
    } else {
        this.classList.remove('fa-solid');
        this.classList.add('fa-regular');
        this.style.color = "";
    }
});


// Nút xem thêm
const btn = document.getElementById("toggleBtn");
const text = document.getElementById("introText");

btn.addEventListener("click", function() {
    text.classList.toggle("expanded");

    if (text.classList.contains("expanded")) {
        btn.innerText = "Thu gọn";
    } else {
        btn.innerText = "Xem thêm";
    }
});


// Comment
document.getElementById("submitComment").addEventListener("click", function() {

    const name = document.getElementById("username").value.trim();
    const text = document.getElementById("commentInput").value.trim();

    if (name === "" || text === "") {
        alert("Vui lòng nhập đầy đủ thông tin!");
        return;
    }

    const now = new Date();
    const timeString = now.toLocaleString("vi-VN");

    const commentHTML = `
        <div class="comment-item">
            <div class="d-flex justify-content-between">
                <div class="comment-name">${name}</div>
                <div class="comment-time">${timeString}</div>
            </div>
            <div class="comment-text">${text}</div>
        </div>
    `;

    document.getElementById("commentList").insertAdjacentHTML("afterbegin", commentHTML);

    document.getElementById("username").value = "";
    document.getElementById("commentInput").value = "";
});