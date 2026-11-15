// Filtros del listado de talleres (columna izquierda de buscar-talleres).
// Este archivo solo sabe filtrar, ordenar y dibujar el panel; buscar-talleres.js lo usa para pintar el mapa.
// Todo se filtra en el navegador con los datos que ya trae /api/talleres.
(function () {
    'use strict';

    // Categorias de servicio. "k" son palabras (minusculas y sin tildes) que se buscan en el texto de servicios del taller
    var GRUPOS = [
        { id: 'mantenimiento', nombre: 'Mantenimiento', items: [
            { id: 'mecanica', nombre: 'Mecánica general', k: ['mecanica'] },
            { id: 'aceite', nombre: 'Aceite y lubricación', k: ['aceite', 'lubric'] },
            { id: 'sincronizacion', nombre: 'Sincronización', k: ['sincroniz'] },
            { id: 'embrague', nombre: 'Embrague', k: ['embrague'] }
        ] },
        { id: 'frenos', nombre: 'Frenos y suspensión', items: [
            { id: 'frenos', nombre: 'Frenos', k: ['freno'] },
            { id: 'suspension', nombre: 'Suspensión', k: ['suspension'] },
            { id: 'alineacion', nombre: 'Alineación y balanceo', k: ['alineacion', 'balanceo'] }
        ] },
        { id: 'electrico', nombre: 'Eléctrico y diagnóstico', items: [
            { id: 'electricidad', nombre: 'Electricidad', k: ['electric'] },
            { id: 'escaner', nombre: 'Escáner', k: ['escaner'] },
            { id: 'diagnostico', nombre: 'Diagnóstico', k: ['diagnostic'] },
            { id: 'bateria', nombre: 'Cambio de batería', k: ['bateria'] }
        ] },
        { id: 'otros', nombre: 'Otros servicios', items: [
            { id: 'llantas', nombre: 'Llantas', k: ['llanta', 'neumatic'] },
            { id: 'latoneria', nombre: 'Latonería y pintura', k: ['latoneria', 'pintura'] },
            { id: 'aire', nombre: 'Aire acondicionado', k: ['aire acondicionado'] }
        ] }
    ];
    var GRUA = { id: 'grua', nombre: 'Con grúa', k: ['grua', 'remolque'] };
    var DEFECTO = { lat: 10.40, lng: -75.52 }; // Cartagena, igual que el centro por defecto del mapa

    var POR_ID = {};
    GRUPOS.forEach(function (g) {
        g.items.forEach(function (s) { POR_ID[s.id] = s; });
    });

    // Minusculas y sin tildes para comparar textos
    function norm(texto) {
        return (texto || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }

    function tiene(item, servicio) {
        if (item._servNorm === undefined) item._servNorm = norm(item.servicios);
        for (var i = 0; i < servicio.k.length; i++) {
            if (item._servNorm.indexOf(servicio.k[i]) >= 0) return true;
        }
        return false;
    }

    // "08:30" -> minutos desde medianoche; null si no se entiende
    function minutos(texto) {
        var m = /^(\d{1,2}):(\d{2})$/.exec(texto || '');
        return m ? Number(m[1]) * 60 + Number(m[2]) : null;
    }

    // Dia de la semana como en HorarioDia: 1 = lunes ... 7 = domingo
    function horarioDelDia(item, dia) {
        var hs = item.horarios || [];
        for (var i = 0; i < hs.length; i++) {
            if (Number(hs[i].dia) === dia) return hs[i];
        }
        return null;
    }

    function abiertoAhora(item) {
        var d = new Date();
        var h = horarioDelDia(item, d.getDay() === 0 ? 7 : d.getDay());
        if (!h) return false;
        var ap = minutos(h.apertura);
        var ci = minutos(h.cierre);
        if (ap === null || ci === null) return false;
        var ahora = d.getHours() * 60 + d.getMinutes();
        return ahora >= ap && ahora < ci;
    }

    // Distancia en km entre dos puntos (formula de haversine)
    function km(lat1, lng1, lat2, lng2) {
        var rad = Math.PI / 180;
        var dLat = (lat2 - lat1) * rad;
        var dLng = (lng2 - lng1) * rad;
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    function puntaje(item) {
        return item.numValoraciones > 0 ? item.estrellas : -1;
    }

    function iniciar(config) {
        var panel = document.getElementById('panel-filtros');
        if (!panel) return null;
        config = config || {};

        var contServicios = document.getElementById('filtro-servicios');
        var contChips = document.getElementById('filtros-activos');
        var botonLimpiar = document.getElementById('filtros-limpiar');
        var contador = document.getElementById('filtros-contador');
        var botonMovil = document.getElementById('boton-abrir-filtros');
        var contadorMovil = document.getElementById('filtros-contador-movil');
        var pistaOrden = document.getElementById('filtro-orden-pista');

        function $(id) { return document.getElementById(id); }

        // Casillas de servicio agrupadas por categoria (textos fijos de GRUPOS, no vienen del usuario)
        function construirServicios() {
            var html = '';
            GRUPOS.forEach(function (g) {
                html += '<details class="filtro-categoria" open>' +
                    '<summary><span class="categoria-nombre">' + g.nombre + '</span>' +
                    '<span class="categoria-contador" data-grupo="' + g.id + '" hidden>0</span></summary>' +
                    '<div class="categoria-cuerpo">';
                g.items.forEach(function (s) {
                    html += '<label class="filtro-casilla">' +
                        '<input type="checkbox" data-svc="' + s.id + '">' +
                        '<span class="casilla-marca" aria-hidden="true"></span>' +
                        '<span class="casilla-texto">' + s.nombre + '</span>' +
                        '<span class="filtro-conteo" data-conteo="svc:' + s.id + '"></span></label>';
                });
                html += '</div></details>';
            });
            contServicios.innerHTML = html;
        }

        // Estado actual leido de los controles del panel
        function leer() {
            var cal = panel.querySelector('input[name="filtro-calificacion"]:checked');
            var modo = panel.querySelector('input[name="filtro-modo"]:checked');
            return {
                grua: $('filtro-grua').checked,
                abierto: $('filtro-abierto').checked,
                sabado: $('filtro-sabado').checked,
                domingo: $('filtro-domingo').checked,
                minCal: cal ? parseInt(cal.value, 10) : 0,
                modo: modo ? modo.value : 'cualquiera',
                servicios: Array.prototype.map.call(
                    panel.querySelectorAll('input[data-svc]:checked'),
                    function (i) { return i.getAttribute('data-svc'); }),
                orden: $('filtro-orden').value
            };
        }

        function coincide(item, e) {
            if (e.grua && !tiene(item, GRUA)) return false;
            if (e.abierto && !abiertoAhora(item)) return false;
            if (e.sabado && !horarioDelDia(item, 6)) return false;
            if (e.domingo && !horarioDelDia(item, 7)) return false;
            if (e.minCal > 0 && !(item.numValoraciones > 0 && item.estrellas >= e.minCal)) return false;
            if (e.servicios.length) {
                var prueba = function (id) { return tiene(item, POR_ID[id]); };
                var ok = e.modo === 'todos' ? e.servicios.every(prueba) : e.servicios.some(prueba);
                if (!ok) return false;
            }
            return true;
        }

        // Origen de las distancias: el punto de partida del usuario; si no hay, el punto de busqueda o Cartagena
        function origen() {
            var partida = config.getOrigenPartida && config.getOrigenPartida();
            return partida || (config.getOrigenBusqueda && config.getOrigenBusqueda()) || DEFECTO;
        }

        function hayPartida() {
            return !!(config.getOrigenPartida && config.getOrigenPartida());
        }

        function textoOrigen(e) {
            if (hayPartida()) return 'Distancias desde tu punto de partida';
            if (e.orden !== 'cercanos') return '';
            return config.getOrigenBusqueda && config.getOrigenBusqueda()
                ? 'Distancias desde el punto de búsqueda'
                : 'Marca tu punto de partida en el mapa para medir distancias';
        }

        function ordenar(lista, orden) {
            var copia = lista.slice();
            if (orden === 'calificacion') {
                copia.sort(function (a, b) {
                    return (puntaje(b) - puntaje(a)) || (b.numValoraciones - a.numValoraciones);
                });
            } else if (orden === 'valoraciones') {
                copia.sort(function (a, b) {
                    return (b.numValoraciones - a.numValoraciones) || (puntaje(b) - puntaje(a));
                });
            } else if (orden === 'cercanos') {
                copia.sort(function (a, b) {
                    var da = typeof a._dist === 'number' ? a._dist : Infinity;
                    var db = typeof b._dist === 'number' ? b._dist : Infinity;
                    return da - db;
                });
            }
            return copia;
        }

        // Devuelve los talleres de "lista" que cumplen los filtros, ya ordenados
        function aplicar(lista) {
            var e = leer();
            lista.forEach(function (i) { delete i.distanciaKm; delete i._dist; });
            var res = lista.filter(function (i) { return coincide(i, e); });
            // Con punto de partida cada tarjeta muestra su distancia; sin el, solo al ordenar por cercania
            if (e.orden === 'cercanos' || hayPartida()) {
                var o = origen();
                var mostrar = hayPartida();
                res.forEach(function (i) {
                    if (i.lat == null || i.lng == null) return;
                    i._dist = km(o.lat, o.lng, i.lat, i.lng);
                    // La distancia solo se muestra si sale del punto de partida del usuario; si no, solo sirve para ordenar
                    if (mostrar) i.distanciaKm = i._dist;
                });
            }
            return ordenar(res, e.orden);
        }

        function chipsActivos(e) {
            var c = [];
            if (e.grua) c.push({ clave: 'grua', texto: 'Con grúa' });
            if (e.abierto) c.push({ clave: 'abierto', texto: 'Abierto ahora' });
            if (e.sabado) c.push({ clave: 'sabado', texto: 'Abre sábados' });
            if (e.domingo) c.push({ clave: 'domingo', texto: 'Abre domingos' });
            if (e.minCal) c.push({ clave: 'cal', texto: e.minCal + '★ o más' });
            e.servicios.forEach(function (id) {
                c.push({ clave: 'svc:' + id, texto: POR_ID[id].nombre });
            });
            return c;
        }

        function hayFiltros() {
            return chipsActivos(leer()).length > 0;
        }

        // Redibuja etiquetas activas, contadores y textos de ayuda
        function pintarPanel(e) {
            var chips = chipsActivos(e);
            contChips.innerHTML = chips.map(function (c) {
                return '<button type="button" class="filtro-chip" data-quitar="' + c.clave + '" ' +
                    'aria-label="Quitar filtro ' + c.texto + '">' + c.texto + '<span aria-hidden="true">×</span></button>';
            }).join('');
            contChips.hidden = chips.length === 0;
            botonLimpiar.hidden = chips.length === 0;
            contador.textContent = chips.length;
            contador.hidden = chips.length === 0;
            if (contadorMovil) {
                contadorMovil.textContent = chips.length;
                contadorMovil.hidden = chips.length === 0;
            }
            GRUPOS.forEach(function (g) {
                var n = g.items.filter(function (s) { return e.servicios.indexOf(s.id) >= 0; }).length;
                var el = panel.querySelector('[data-grupo="' + g.id + '"]');
                el.textContent = n;
                el.hidden = n === 0;
            });
            pistaOrden.textContent = textoOrigen(e);
            pistaOrden.hidden = !pistaOrden.textContent;
        }

        function cambio() {
            var e = leer();
            // "Más cercanos" sin punto de partida: pido la ubicacion (buscar-talleres.js la guarda como partida)
            if (e.orden === 'cercanos' && !hayPartida() && config.pedirUbicacion) config.pedirUbicacion();
            pintarPanel(e);
            if (config.alCambiar) config.alCambiar();
        }

        // Cuantos talleres de la lista base tienen cada opcion (los numeros chicos al lado de cada casilla)
        function actualizarConteos(base) {
            var c = { grua: 0, abierto: 0, sabado: 0, domingo: 0 };
            var porServicio = {};
            Object.keys(POR_ID).forEach(function (id) { porServicio[id] = 0; });
            base.forEach(function (i) {
                if (tiene(i, GRUA)) c.grua++;
                if (abiertoAhora(i)) c.abierto++;
                if (horarioDelDia(i, 6)) c.sabado++;
                if (horarioDelDia(i, 7)) c.domingo++;
                Object.keys(POR_ID).forEach(function (id) {
                    if (tiene(i, POR_ID[id])) porServicio[id]++;
                });
            });
            Array.prototype.forEach.call(panel.querySelectorAll('[data-conteo]'), function (el) {
                var clave = el.getAttribute('data-conteo');
                var n = clave.indexOf('svc:') === 0 ? porServicio[clave.slice(4)] : c[clave];
                el.textContent = n;
                el.parentNode.classList.toggle('sin-resultados', n === 0);
            });
            pintarPanel(leer());
        }

        function quitar(clave) {
            if (clave === 'grua') $('filtro-grua').checked = false;
            else if (clave === 'abierto') $('filtro-abierto').checked = false;
            else if (clave === 'sabado') $('filtro-sabado').checked = false;
            else if (clave === 'domingo') $('filtro-domingo').checked = false;
            else if (clave === 'cal') $('filtro-cal-0').checked = true;
            else if (clave.indexOf('svc:') === 0) {
                var caja = panel.querySelector('input[data-svc="' + clave.slice(4) + '"]');
                if (caja) caja.checked = false;
            }
            cambio();
        }

        // Quita todos los filtros; el orden elegido se conserva
        function limpiar() {
            ['filtro-grua', 'filtro-abierto', 'filtro-sabado', 'filtro-domingo'].forEach(function (id) {
                $(id).checked = false;
            });
            $('filtro-cal-0').checked = true;
            $('filtro-modo-cualquiera').checked = true;
            Array.prototype.forEach.call(panel.querySelectorAll('input[data-svc]'), function (i) { i.checked = false; });
            cambio();
        }

        construirServicios();
        panel.addEventListener('change', cambio);
        contChips.addEventListener('click', function (ev) {
            var b = ev.target.closest('[data-quitar]');
            if (b) quitar(b.getAttribute('data-quitar'));
        });
        botonLimpiar.addEventListener('click', limpiar);
        if (botonMovil) {
            botonMovil.addEventListener('click', function () {
                var abierto = panel.classList.toggle('panel-filtros--abierto');
                botonMovil.setAttribute('aria-expanded', abierto ? 'true' : 'false');
            });
        }
        pintarPanel(leer());

        // Al fijar el punto de partida: ordeno por cercania si el orden era el de por defecto, y repinto textos
        function alFijarPartida() {
            var orden = $('filtro-orden');
            if (orden.value === 'recomendados') orden.value = 'cercanos';
            pintarPanel(leer());
        }

        return {
            aplicar: aplicar,
            actualizarConteos: actualizarConteos,
            hayFiltros: hayFiltros,
            limpiar: limpiar,
            alFijarPartida: alFijarPartida,
            refrescar: function () { pintarPanel(leer()); }
        };
    }

    window.FiltrosTalleres = { iniciar: iniciar };
})();
