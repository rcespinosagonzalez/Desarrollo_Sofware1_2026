// Sidebar: lee el rol guardado en sessionStorage (lo pone script.js al hacer login).
// admin = dueño de talleres; user = cliente que busca y pide citas.
(function () {
    var role = sessionStorage.getItem('role');
    var body = document.body;
    var vista = body.getAttribute('data-vista');
    var rolRequerido = body.getAttribute('data-role');

    if (!role) {
        window.location.href = '/';
        return;
    }
    if (rolRequerido && role !== rolRequerido) {
        window.location.href = role === 'admin' ? '/mi-taller' : '/buscar-talleres';
        return;
    }

    // Menú ADMIN: gestionar talleres propios, citas y opiniones
    var menuAdmin = [
        { url: '/mi-taller', texto: 'Mis talleres', vista: 'mi-taller', icono: 'shop' },
        { url: '/registrar-taller', texto: 'Nuevo taller', vista: 'registrar-taller', icono: 'tool' },
        { url: '/citas-pendientes', texto: 'Citas pendientes', vista: 'citas-pendientes', icono: 'calendar' },
        { url: '/valoraciones', texto: 'Valoraciones', vista: 'valoraciones', icono: 'star' }
    ];
    // Menú USER: buscar, ver un taller (sin id = muestra el primero en el servidor) y citas
    var menuUser = [
        { url: '/buscar-talleres', texto: 'Buscar talleres', vista: 'buscar-talleres', icono: 'search' },
        { url: '/detalle-taller', texto: 'Detalle del taller', vista: 'detalle-taller', icono: 'pin' },
        { url: '/solicitar-cita', texto: 'Solicitar cita', vista: 'solicitar-cita', icono: 'edit' },
        { url: '/historial-busquedas', texto: 'Historial de búsquedas', vista: 'historial-busquedas', icono: 'clock' },
        { url: '/calificar-taller', texto: 'Calificar taller', vista: 'calificar-taller', icono: 'thumb' }
    ];

    var iconosSvg = {
        tool: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14.7 6.3a4.5 4.5 0 0 1 5.9 5.9l-9 9a2.2 2.2 0 1 1-3.1-3.1l9-9A4.5 4.5 0 0 1 14.7 6.3z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        calendar: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3.5" y="5" width="17" height="15.5" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M8 3.8v3.4M16 3.8v3.4M3.5 9.2h17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
        star: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3.8 2.6 5.3 5.8.8-4.2 4.1 1 5.8L12 17l-5.2 2.8 1-5.8-4.2-4.1 5.8-.8L12 3.8z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
        shop: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 9.5h16l-1.1 10H5.1L4 9.5zM6 9.5V6.8A2.8 2.8 0 0 1 8.8 4h6.4A2.8 2.8 0 0 1 18 6.8v2.7" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        search: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="m16 16 4 4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
        pin: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 21s6-6 6-10a6 6 0 1 0-12 0c0 4 6 10 6 10z" fill="none" stroke="currentColor" stroke-width="1.8"/><circle cx="12" cy="11" r="2.2" fill="none" stroke="currentColor" stroke-width="1.8"/></svg>',
        edit: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 20 4.1-.8L19 8.3a1.9 1.9 0 0 0 0-2.6l-.7-.7a1.9 1.9 0 0 0-2.6 0L4.8 15.9 4 20z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        clock: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M12 7.6v4.8l3.2 1.8" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        thumb: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9.6 10.2 11 5.6a2.3 2.3 0 0 1 4.4.6v3.3h3.1a2 2 0 0 1 2 2.4l-1.1 5.5a2.4 2.4 0 0 1-2.3 1.9H9.6v-9.1zM4.2 10.2h3.1v9.1H4.2z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
        logout: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 4.5h4.2A1.8 1.8 0 0 1 20 6.3v11.4a1.8 1.8 0 0 1-1.8 1.8H14M10.8 8.5 6.5 12l4.3 3.5M6.8 12H20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>'
    };

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
            var icono = document.createElement('span');
            icono.className = 'item-icono';
            icono.innerHTML = iconosSvg[item.icono] || iconosSvg.search;
            var texto = document.createElement('span');
            texto.className = 'item-texto';
            texto.textContent = item.texto;
            a.appendChild(icono);
            a.appendChild(texto);
            if (vista === item.vista) a.className = 'activo';
            li.appendChild(a);
            ul.appendChild(li);
        });
        nav.appendChild(ul);
        sidebarEl.appendChild(nav);
        var cerrar = document.createElement('div');
        cerrar.className = 'cerrar-sesion';
        var a = document.createElement('a');
        a.href = '/';
        var iconoCerrar = document.createElement('span');
        iconoCerrar.className = 'item-icono';
        iconoCerrar.innerHTML = iconosSvg.logout;
        var textoCerrar = document.createElement('span');
        textoCerrar.className = 'item-texto';
        textoCerrar.textContent = 'Cerrar sesión';
        a.appendChild(iconoCerrar);
        a.appendChild(textoCerrar);
        cerrar.appendChild(a);
        sidebarEl.appendChild(cerrar);
    }

    var tituloRol = document.getElementById('titulo-rol');
    if (tituloRol) tituloRol.textContent = role === 'admin' ? 'Menú Administrador' : 'Menú Usuario';
})();
