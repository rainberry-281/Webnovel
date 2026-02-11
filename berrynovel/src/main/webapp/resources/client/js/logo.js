fetch('../layout/logo.html')
    .then(response => response.text())
    .then(data => {
        document.getElementById('logo').innerHTML = data;
    })
    .catch(err => console.error('Lỗi load logo:', err));