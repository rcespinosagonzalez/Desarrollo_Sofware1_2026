// Layout por rol: sidebar y protección de vistas
(function () {
    var role = sessionStorage.getItem('role');
    var body = document.body;
    var vista = body.getAttribute('data-vista');
    var rolRequerido = body.getAttribute('data-role');

    if (!role) {
        window.location.href = 'index.html';
        return;
    }
    if (rolRequerido && role !== rolRequerido) {
        window.location.href = role === 'admin' ? 'mi-taller.html' : 'buscar-talleres.html';
        return;
    }

    var menuAdmin = [
        { url: 'registrar-taller.html', texto: 'Registrar taller', vista: 'registrar-taller' },
        { url: 'citas-pendientes.html', texto: 'Citas pendientes', vista: 'citas-pendientes' },
        { url: 'valoraciones.html', texto: 'Valoraciones', vista: 'valoraciones' },
        { url: 'mi-taller.html', texto: 'Mi taller', vista: 'mi-taller' }
    ];
    var menuUser = [
        { url: 'buscar-talleres.html', texto: 'Buscar talleres', vista: 'buscar-talleres' },
        { url: 'detalle-taller.html', texto: 'Detalle del taller', vista: 'detalle-taller' },
        { url: 'solicitar-cita.html', texto: 'Solicitar cita', vista: 'solicitar-cita' },
        { url: 'historial-busquedas.html', texto: 'Historial de búsquedas', vista: 'historial-busquedas' },
        { url: 'calificar-taller.html', texto: 'Calificar taller', vista: 'calificar-taller' }
    ];

    var items = role === 'admin' ? menuAdmin : menuUser;
    var sidebarEl = document.getElementById('sidebar-app');
    if (sidebarEl) {
var nav = document.createElement('nav');
    nav.style.flex = '1';
    var ul = document.createElement('ul');
    items.forEach(function (item) {
            var li = document.createElement('li');
            var a = document.createElement('a');
            a.href = item.url;
            a.textContent = item.texto;
            if (vista === item.vista) a.className = 'activo';
            li.appendChild(a);
            ul.appendChild(li);
        });
        nav.appendChild(ul);
        sidebarEl.appendChild(nav);
        var cerrar = document.createElement('div');
        cerrar.className = 'cerrar-sesion';
        var a = document.createElement('a');
        a.href = 'index.html';
        a.textContent = 'Cerrar sesión';
        cerrar.appendChild(a);
        sidebarEl.appendChild(cerrar);
    }

    var tituloRol = document.getElementById('titulo-rol');
    if (tituloRol) tituloRol.textContent = role === 'admin' ? 'Menú Administrador' : 'Menú Usuario';
})();
