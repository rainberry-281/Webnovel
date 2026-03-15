// Kết cấu của tóm tắt nội dung
// document.addEventListener("DOMContentLoaded", function() {
//     const intro = document.getElementById("introText");
//     let content = intro.innerText;
//     content = content.replace(/\. /g, ".<br>");
//     intro.innerHTML = content;
// });

// Nút xem thêm
const btn = document.getElementById("toggleBtn");
const text = document.getElementById("introText");

if (btn && text) {
    btn.addEventListener("click", function () {
        text.classList.toggle("expanded");

        if (text.classList.contains("expanded")) {
            btn.innerText = "Thu gọn";
        } else {
            btn.innerText = "Xem thêm";
        }
    });
}


// Comment
const submitCommentButton = document.getElementById("submitComment");

if (submitCommentButton) {
    submitCommentButton.addEventListener("click", function () {

        const usernameInput = document.getElementById("username");
        const commentInput = document.getElementById("commentInput");
        const commentList = document.getElementById("commentList");

        if (!usernameInput || !commentInput || !commentList) {
            return;
        }

        const name = usernameInput.value.trim();
        const text = commentInput.value.trim();

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

        commentList.insertAdjacentHTML("afterbegin", commentHTML);

        usernameInput.value = "";
        commentInput.value = "";
    });
}