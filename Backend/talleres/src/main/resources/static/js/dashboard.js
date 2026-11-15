// Dashboards de admin y de dueño de taller. Piden el JSON a la API (data-url) y dibujan tarjetas, graficos y listas
// con graficos.js. Va en un archivo aparte porque la CSP no permite scripts dentro del HTML.
(function () {
    'use strict';

    var seccion = document.getElementById('seccion-dashboard');
    if (!seccion || !window.Graficos) return;
    var G = window.Graficos;
    var tipo = seccion.getAttribute('data-dashboard');
    var url = seccion.getAttribute('data-url');
    var contenedorKpis = document.getElementById('dash-kpis');
    var botonActualizar = document.getElementById('dash-actualizar');

    function $(id) { return document.getElementById(id); }

    function tarjeta(etiqueta, valor, nota, tono) {
        var t = G.html('div', 'dash-kpi' + (tono ? ' dash-kpi-' + tono : ''));
        t.appendChild(G.html('span', 'dash-kpi-etiqueta', etiqueta));
        t.appendChild(G.html('strong', 'dash-kpi-valor', valor));
        if (nota) t.appendChild(G.html('span', 'dash-kpi-nota', nota));
        return t;
    }

    function pintarKpis(tarjetas) {
        G.vaciar(contenedorKpis);
        tarjetas.forEach(function (t) { contenedorKpis.appendChild(tarjeta(t[0], t[1], t[2], t[3])); });
    }

    function estrellas(n) {
        return '★'.repeat(n) + '☆'.repeat(Math.max(0, 5 - n));
    }

    function listaValoraciones(contenedor, lista, conTaller) {
        G.vaciar(contenedor);
        if (!lista || !lista.length) {
            contenedor.appendChild(G.html('li', 'graf-vacio', 'Todavía no hay valoraciones'));
            return;
        }
        lista.forEach(function (v) {
            var li = G.html('li', 'dash-item');
            var cab = G.html('div', 'dash-item-cab');
            cab.appendChild(G.html('span', 'dash-estrellas', estrellas(v.estrellas)));
            cab.appendChild(G.html('strong', null, conTaller ? v.taller : v.cliente));
            li.appendChild(cab);
            li.appendChild(G.html('p', 'dash-item-texto', (conTaller ? v.cliente + ': ' : '') + (v.comentario || 'Sin comentario')));
            contenedor.appendChild(li);
        });
    }

    function pintarAdmin(d) {
        var k = d.kpis;
        pintarKpis([
            ['Talleres', G.numero(k.talleres), '+' + k.talleresNuevos30d + ' en 30 días'],
            ['Clientes', G.numero(k.clientes), '+' + k.clientesNuevos30d + ' en 30 días'],
            ['Dueños de taller', G.numero(k.duenos), null],
            ['Citas en total', G.numero(k.citasTotal), k.citasHoy + ' hoy'],
            ['Por confirmar', G.numero(k.citasPendientes), 'citas solicitadas', k.citasPendientes > 0 ? 'alerta' : null],
            ['Valoración media', k.valoraciones ? k.promedioGeneral.toFixed(1) + ' / 5' : '—', G.numero(k.valoraciones) + ' valoraciones'],
            ['Talleres destacados', G.numero(k.destacadosActivos), 'con plan vigente'],
            ['Cancelación', k.tasaCancelacion.toFixed(1) + ' %', 'del total de citas']
        ]);
        G.barras($('graf-citas-mes'), d.citasPorMes.etiquetas, d.citasPorMes.valores, 'Citas por mes');
        G.donut($('graf-estados'), d.estados.etiquetas, d.estados.valores, ['#fbbf24', '#2563eb', '#16a34a', '#ef4444']);
        G.barrasH($('graf-ciudades'), d.ciudades.etiquetas, d.ciudades.valores);
        G.barrasH($('graf-servicios'), d.servicios.etiquetas, d.servicios.valores);

        var mejores = $('lista-mejores');
        G.vaciar(mejores);
        if (!d.mejoresTalleres.length) mejores.appendChild(G.html('li', 'graf-vacio', 'Aún no hay talleres con 5 o más valoraciones'));
        d.mejoresTalleres.forEach(function (t) {
            var li = G.html('li', 'dash-item');
            var enlace = G.html('a', null, t.nombre);
            enlace.href = '/detalle-taller?id=' + encodeURIComponent(t.id);
            var cab = G.html('div', 'dash-item-cab');
            cab.appendChild(enlace);
            cab.appendChild(G.html('span', 'dash-estrellas', '★ ' + t.promedio.toFixed(1)));
            li.appendChild(cab);
            li.appendChild(G.html('p', 'dash-item-texto', (t.ciudad || '—') + ' · ' + t.total + ' valoraciones'));
            mejores.appendChild(li);
        });

        var citas = $('lista-citas-recientes');
        G.vaciar(citas);
        d.ultimasCitas.forEach(function (c) {
            var li = G.html('li', 'dash-item');
            var cab = G.html('div', 'dash-item-cab');
            cab.appendChild(G.html('strong', null, c.cliente));
            cab.appendChild(G.html('span', 'dash-estado estado-' + c.estado, c.estado));
            li.appendChild(cab);
            li.appendChild(G.html('p', 'dash-item-texto', c.taller + ' · ' + c.cuando));
            citas.appendChild(li);
        });
        listaValoraciones($('lista-valoraciones-recientes'), d.ultimasValoraciones, true);
    }

    function pintarTaller(d) {
        if (d.sinTaller) {
            pintarKpis([]);
            return;
        }
        var k = d.kpis;
        pintarKpis([
            ['Citas hoy', G.numero(k.citasHoy), k.capacidadHoy ? 'Ocupación ' + k.ocupacionHoy + ' % de ' + k.capacidadHoy + ' turnos' : 'Hoy no atiendes'],
            ['Próximos 7 días', G.numero(k.proximas7d), 'citas agendadas'],
            ['Por confirmar', G.numero(k.pendientes), 'solicitudes nuevas', k.pendientes > 0 ? 'alerta' : null],
            ['Completadas este mes', G.numero(k.completadasMes), null],
            ['Valoración media', k.valoraciones ? k.promedio.toFixed(1) + ' / 5' : '—', G.numero(k.valoraciones) + ' valoraciones'],
            ['Cancelación', k.tasaCancelacion.toFixed(1) + ' %', 'de ' + G.numero(k.citasTotal) + ' citas'],
            ['Mecánicos activos', G.numero(k.mecanicosActivos), null],
            ['Plan', d.plan.destacado ? 'Destacado' : 'Estándar', d.plan.destacado ? 'hasta ' + d.plan.hasta : 'sin patrocinio', d.plan.destacado ? 'ok' : null]
        ]);
        G.barras($('graf-ultimos14'), d.ultimos14.etiquetas, d.ultimos14.valores, 'Citas de los últimos 14 días');
        G.barras($('graf-proximos7'), d.proximos7.etiquetas, d.proximos7.valores, 'Citas de los próximos 7 días');
        G.donut($('graf-estados'), d.estados.etiquetas, d.estados.valores, ['#fbbf24', '#2563eb', '#16a34a', '#ef4444']);
        G.barrasH($('graf-estrellas'), d.estrellas.etiquetas, d.estrellas.valores);
        G.barras($('graf-citas-mes'), d.citasPorMes.etiquetas, d.citasPorMes.valores, 'Citas por mes');
        listaValoraciones($('lista-valoraciones-recientes'), d.ultimasValoraciones, false);
    }

    function cargar() {
        if (botonActualizar) botonActualizar.disabled = true;
        return fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (datos) {
                if (tipo === 'admin') pintarAdmin(datos); else pintarTaller(datos);
            })
            .catch(function () {
                G.vaciar(contenedorKpis);
                contenedorKpis.appendChild(G.html('p', 'aviso-error', 'No se pudo cargar el resumen. Intenta de nuevo con «Actualizar».'));
            })
            .then(function () {
                if (botonActualizar) botonActualizar.disabled = false;
            });
    }

    if (botonActualizar) botonActualizar.addEventListener('click', cargar);
    cargar();
})();
