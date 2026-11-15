// Formulario de registro: POST /api/auth/registro y, si sale bien, entra directo al mapa
(function () {
    var form = document.getElementById('formularioRegistro');
    if (!form) return;

    var errorEl = document.getElementById('errorRegistro');
    var boton = form.querySelector('button[type="submit"]');

    function mostrarError(texto) {
        errorEl.textContent = texto || 'No se pudo completar el registro.';
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        errorEl.textContent = '';

        var nombre = document.getElementById('nombre').value.trim();
        var correo = document.getElementById('correo').value.trim();
        var clave = document.getElementById('contrasena').value;
        var confirmar = document.getElementById('confirmar').value;

        if (!nombre || !correo || !clave) {
            mostrarError('Completa todos los campos.');
            return;
        }
        if (clave !== confirmar) {
            mostrarError('Las contraseñas no coinciden.');
            return;
        }

        var body = new URLSearchParams();
        body.set('nombre', nombre);
        body.set('email', correo);
        body.set('password', clave);

        boton.disabled = true;
        fetch('/api/auth/registro', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body.toString()
        })
            .then(function (r) {
                // 400/401/409 traen {ok:false, mensaje}; otro error (500, red) cae en el catch
                if (!r.ok && r.status >= 500) throw new Error('Error del servidor');
                return r.json();
            })
            .then(function (data) {
                if (!data.ok) {
                    mostrarError(data.mensaje);
                    boton.disabled = false;
                    return;
                }
                // Misma sesion de navegador que deja script.js tras el login
                try {
                    sessionStorage.setItem('role', data.rol);
                    sessionStorage.setItem('nombre', data.nombre || correo);
                    if (data.usuarioId != null) {
                        sessionStorage.setItem('usuarioId', String(data.usuarioId));
                    }
                } catch (err) {}
                window.location.href = '/buscar-talleres';
            })
            .catch(function () {
                mostrarError('No se pudo conectar con el servidor.');
                boton.disabled = false;
            });
    });
})();
