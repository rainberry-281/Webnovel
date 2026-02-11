fetch('../layout/navigation.html')
    .then(response => response.text())
    .then(data => {
        document.getElementById('navbar').innerHTML = data;
    })
    .catch(err => console.error('Lỗi load navbar:', err));