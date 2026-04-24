(function () {
    var form = document.getElementById('formularioInicioSesion');
    if (!form) return;

    // Botón mostrar/ocultar contraseña
    var btnAlternar = document.getElementById('alternarContrasena');
    var inputContrasena = document.getElementById('contrasena');
    if (btnAlternar && inputContrasena) {
        btnAlternar.addEventListener('click', function () {
            var tipo = inputContrasena.getAttribute('type') === 'password' ? 'text' : 'password';
            inputContrasena.setAttribute('type', tipo);
            btnAlternar.classList.toggle('visible');
        });
    }

    // Validación visual de campos
    var inputCorreo = document.getElementById('correo');
    var errorCorreo = document.getElementById('errorCorreo');
    var errorContrasena = document.getElementById('errorContrasena');

    if (inputCorreo && errorCorreo) {
        inputCorreo.addEventListener('blur', function () {
            if (!inputCorreo.value.trim()) {
                errorCorreo.textContent = 'El correo es obligatorio.';
            } else if (!/\S+@\S+\.\S+/.test(inputCorreo.value)) {
                errorCorreo.textContent = 'Ingresa un correo válido.';
            } else {
                errorCorreo.textContent = '';
            }
        });
    }

    if (inputContrasena && errorContrasena) {
        inputContrasena.addEventListener('blur', function () {
            if (!inputContrasena.value) {
                errorContrasena.textContent = 'La contraseña es obligatoria.';
            } else {
                errorContrasena.textContent = '';
            }
        });
    }

    // Login contra el backend
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        var correo = (inputCorreo || {}).value || '';
        var contrasena = (inputContrasena || {}).value || '';
        var errorEl = document.getElementById('errorLogin');
        var boton = form.querySelector('.boton-inicio-sesion');

        // Validación antes de enviar
        var valido = true;
        if (!correo.trim()) {
            if (errorCorreo) errorCorreo.textContent = 'El correo es obligatorio.';
            valido = false;
        }
        if (!contrasena) {
            if (errorContrasena) errorContrasena.textContent = 'La contraseña es obligatoria.';
            valido = false;
        }
        if (!valido) return;

        // Mostrar cargador
        if (boton) boton.classList.add('cargando');

        // Enviar al backend
        fetch('/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'email=' + encodeURIComponent(correo) + '&password=' + encodeURIComponent(contrasena),
            redirect: 'follow'
        })
            .then(function (response) {
                if (boton) boton.classList.remove('cargando');
                if (response.redirected) {
                    // Guardar rol en sessionStorage según la URL de redirección
                    var role = response.url.includes('mi-taller') ? 'admin' : 'user';
                    try {
                        sessionStorage.setItem('role', role);
                    } catch (err) {}
                    window.location.href = response.url;
                } else {
                    if (errorEl) errorEl.textContent = 'Correo o contraseña incorrectos. Intenta de nuevo.';
                }
            })
            .catch(function () {
                if (boton) boton.classList.remove('cargando');
                if (errorEl) errorEl.textContent = 'Error de conexión. Intenta de nuevo.';
            });
    });
})();