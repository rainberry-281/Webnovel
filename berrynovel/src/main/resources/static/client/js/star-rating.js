(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const averageStars = document.getElementById("rating-stars-display");
        if (averageStars) {
            renderAverageStars(parseFloat(averageStars.dataset.avg || "0"));
        }

        const widget = document.querySelector(".star-widget");
        if (!widget) {
            return;
        }

        const stars = widget.querySelectorAll(".star");
        const novelId = widget.dataset.novelId;
        const csrf = widget.dataset.csrf;
        const csrfHeader = widget.dataset.csrfHeader || "X-CSRF-TOKEN";
        const feedback = document.getElementById("rating-feedback");

        highlightStars(parseInt(widget.dataset.userScore, 10) || 0);

        stars.forEach(function (star) {
            star.addEventListener("mouseenter", function () {
                highlightStars(parseInt(star.dataset.value, 10));
            });
            star.addEventListener("mouseleave", function () {
                highlightStars(parseInt(widget.dataset.userScore, 10) || 0);
            });
            star.addEventListener("click", function () {
                submitRating(parseInt(star.dataset.value, 10));
            });
        });

        function highlightStars(upTo) {
            stars.forEach(function (star) {
                const value = parseInt(star.dataset.value, 10);
                star.classList.toggle("active", value <= upTo);
            });
        }

        function submitRating(score) {
            const headers = {
                "Content-Type": "application/x-www-form-urlencoded"
            };
            if (csrf) {
                headers[csrfHeader] = csrf;
            }

            fetch("/novel/" + encodeURIComponent(novelId) + "/rate", {
                method: "POST",
                headers: headers,
                body: "score=" + encodeURIComponent(score)
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Rating request failed");
                    }
                    return response.json();
                })
                .then(function (data) {
                    if (!data.success) {
                        throw new Error(data.error || "Rating request failed");
                    }

                    widget.dataset.userScore = String(data.userScore || score);
                    highlightStars(parseInt(widget.dataset.userScore, 10) || 0);
                    updateAverage(data.ratingAvg, data.ratingCount);
                    showFeedback("Thanks for rating!", false);
                })
                .catch(function () {
                    showFeedback("Error. Please try again.", true);
                });
        }

        function updateAverage(ratingAvg, ratingCount) {
            const avg = Number(ratingAvg) || 0;
            const count = Number(ratingCount) || 0;
            const avgEl = document.getElementById("rating-avg");
            const countEl = document.getElementById("rating-count");

            if (avgEl) {
                avgEl.textContent = count > 0 ? avg.toFixed(1) : "-";
            }
            if (countEl) {
                countEl.textContent = "(" + count + " ratings)";
            }
            renderAverageStars(avg);
        }

        function showFeedback(message, isError) {
            if (!feedback) {
                return;
            }
            feedback.textContent = message;
            feedback.classList.toggle("is-error", isError);
            if (!isError) {
                window.setTimeout(function () {
                    feedback.textContent = "";
                }, 3000);
            }
        }

        function renderAverageStars(avg) {
            const display = document.getElementById("rating-stars-display");
            if (!display) {
                return;
            }

            let html = "";
            for (let i = 1; i <= 5; i++) {
                if (avg >= i) {
                    html += '<span class="star-filled">&#9733;</span>';
                } else if (avg >= i - 0.5) {
                    html += '<span class="star-half">&#9733;</span>';
                } else {
                    html += '<span class="star-empty">&#9734;</span>';
                }
            }
            display.innerHTML = html;
        }
    });
})();
