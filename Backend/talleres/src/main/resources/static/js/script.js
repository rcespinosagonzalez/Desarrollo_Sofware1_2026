// Login contra la tabla usuario en MySQL (POST /api/auth/login).
// admin -> Mis talleres; cualquier otro rol del menú -> Buscar talleres.
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
                    window.location.href = data.rol === 'admin' ? '/mi-taller' : '/buscar-talleres';
                } else {
                    mostrarError(data.mensaje);
                }
            })
            .catch(function () {
                mostrarError('No se pudo conectar con el servidor. ¿Está arrancado Spring Boot?');
            });
    });
})();
