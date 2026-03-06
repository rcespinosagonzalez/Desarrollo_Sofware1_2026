// Login: valida contra JSON y redirige según rol (admin / user)
(function () {
    var form = document.getElementById('formularioInicioSesion');
    if (!form) return;

    var usuarios = [
        { email: 'admin@gmail.com', password: '123456789', role: 'admin', nombre: 'Administrador' },
        { email: 'admi@gmail.com', password: '123456789', role: 'admin', nombre: 'Administrador' },
        { email: 'admi@gmail', password: '123456789', role: 'admin', nombre: 'Administrador' },
        { email: 'usuario@gmail.com', password: '123456789', role: 'user', nombre: 'Usuario' }
    ];

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        var correo = (document.getElementById('correo') || {}).value || '';
        var contrasena = (document.getElementById('contrasena') || {}).value || '';
        var errorEl = document.getElementById('errorLogin');

        var usuario = usuarios.find(function (u) {
            return u.email === correo.trim() && u.password === contrasena;
        });

        if (usuario) {
            try {
                sessionStorage.setItem('role', usuario.role);
                sessionStorage.setItem('nombre', usuario.nombre || usuario.email);
            } catch (err) {}
            window.location.href = usuario.role === 'admin' ? 'mi-taller.html' : 'buscar-talleres.html';
        } else {
            if (!errorEl) {
                errorEl = document.createElement('p');
                errorEl.id = 'errorLogin';
                errorEl.style.color = '#c00';
                errorEl.style.marginTop = '0.5rem';
                errorEl.style.fontSize = '0.9rem';
                form.appendChild(errorEl);
            }
            errorEl.textContent = 'Correo o contraseña incorrectos. Intenta de nuevo.';
        }
    });
})();
