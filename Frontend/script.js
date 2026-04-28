// Si abres el HTML sin Spring Boot, el fetch fallará. Con la app en http://localhost:8080/ valida contra MySQL.
(function () {
    var form = document.getElementById('formularioInicioSesion');
    if (!form) return;

    function mostrarError(texto) {
        var errorEl = document.getElementById('errorLogin');
        if (!errorEl) {
            errorEl = document.createElement('p');
            errorEl.id = 'errorLogin';
            errorEl.style.color = '#c00';
            errorEl.style.marginTop = '0.5rem';
            errorEl.style.fontSize = '0.9rem';
            form.appendChild(errorEl);
        }
        errorEl.textContent = texto || 'Correo o contraseña incorrectos. Intenta de nuevo.';
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        var correo = (document.getElementById('correo') || {}).value || '';
        var contrasena = (document.getElementById('contrasena') || {}).value || '';
        var errorEl = document.getElementById('errorLogin');
        if (errorEl) errorEl.textContent = '';

        var body = new URLSearchParams();
        body.set('email', correo.trim());
        body.set('password', contrasena);

        fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body.toString()
        })
            .then(function (r) {
                if (!r.ok) throw new Error('Error de red');
                return r.json();
            })
            .then(function (data) {
                if (data.ok) {
                    try {
                        sessionStorage.setItem('role', data.rol);
                        sessionStorage.setItem('nombre', data.nombre || correo.trim());
                    } catch (err) {}
                    window.location.href = data.rol === 'admin' ? 'mi-taller.html' : 'buscar-talleres.html';
                } else {
                    mostrarError(data.mensaje);
                }
            })
            .catch(function () {
                mostrarError('Abre el login desde Spring Boot (localhost:8080) para validar contra la base de datos.');
            });
    });
})();
