// Mapa del cliente: cargo talleres de /api/talleres, filtro por texto y guardo historial
(function () {
    var inputBusqueda = document.getElementById('entrada-busqueda-lugar');
    var botonBuscar = document.getElementById('boton-buscar-lugar');
    var botonVerMapa = document.getElementById('boton-ver-mapa-completo');
    var mapaContenedor = document.getElementById('mapa-talleres');
    var listaTalleres = document.getElementById('lista-resultados-talleres');
    var estadoBusqueda = document.getElementById('texto-estado-busqueda');
    if (!inputBusqueda || !botonBuscar || !botonVerMapa || !mapaContenedor || !listaTalleres || !estadoBusqueda || typeof L === 'undefined') return;

    var talleres = [];
    var marcadorOrigen = null;
    var catalogoCompleto = [];
    var busquedaEnCurso = false;
    var ultimoHistorialTermino = '';
    var ultimoHistorialMs = 0;
    // Leaflet: centro Cartagena por defecto
    var mapa = L.map('mapa-talleres', { zoomControl: true }).setView([10.40, -75.52], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(mapa);

    // Texto bajo el mapa (cargando, N talleres, etc.)
    function setEstado(texto) {
        estadoBusqueda.textContent = texto || '';
    }

    // Quita marcadores y tarjetas antes de pintar de nuevo
    function limpiarResultados() {
        talleres.forEach(function (t) { t.marker.remove(); });
        talleres = [];
        listaTalleres.innerHTML = '';
    }

    // Evito XSS al meter nombres en innerHTML
    function escapeHtml(texto) {
        return (texto || '').replace(/[&<>"']/g, function (c) {
            var m = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
            return m[c];
        });
    }

    // Zoom para que entren todos los marcadores visibles
    function ajustarMapaAVista() {
        if (!talleres.length) return;
        var bounds = L.latLngBounds(talleres.map(function (t) { return [t.lat, t.lng]; }));
        if (marcadorOrigen) bounds.extend(marcadorOrigen.getLatLng());
        mapa.fitBounds(bounds.pad(0.15));
    }

    // Normalizo el JSON del backend al formato que usa el mapa
    function itemDesdeBd(t) {
        return {
            id: t.id,
            nombre: t.nombre || 'Taller',
            direccion: t.direccion || 'Sin dirección',
            ciudad: t.ciudad || '',
            telefono: t.telefono || '',
            horario: t.horario || '',
            servicios: t.servicios || '',
            estrellas: t.calificacionPromedio || 0,
            numValoraciones: t.numValoraciones || 0,
            lat: t.lat,
            lng: t.lng,
            tipoLegible: 'Taller'
        };
    }

    // HTML de estrellas en la tarjeta del listado
    function insigniaValoracion(item) {
        if (item.numValoraciones > 0) {
            return '<span class="insignia-valoracion" title="' + item.numValoraciones + ' valoraciones">' +
                Math.round(item.estrellas * 10) / 10 + '<small>/5</small></span>';
        }
        return '<span class="insignia-valoracion insignia-sin-datos">—</span>';
    }

    // Tarjeta en la lista lateral; click centra el mapa si hay marker
    function crearTarjeta(item, marker) {
        var tarjeta = document.createElement('article');
        tarjeta.className = 'tarjeta tarjeta-resultado-taller';
        var meta = [];
        if (item.telefono) meta.push('<span class="meta-taller">' + escapeHtml(item.telefono) + '</span>');
        if (item.horario) meta.push('<span class="meta-taller">' + escapeHtml(item.horario) + '</span>');
        var botonMapa = marker
            ? '<button type="button" class="boton-centrar-mapa boton-accion-tarjeta">Mapa</button>'
            : '';
        tarjeta.innerHTML =
            '<div class="tarjeta-taller-cabecera">' +
            '<h3 class="titulo-resultado-taller">' + escapeHtml(item.nombre) + '</h3>' +
            insigniaValoracion(item) +
            '</div>' +
            '<p class="texto-direccion-resultado">' + escapeHtml(item.direccion) +
            (item.ciudad ? ' · ' + escapeHtml(item.ciudad) : '') + '</p>' +
            (item.servicios ? '<p class="linea-servicios-taller">' + escapeHtml(item.servicios) + '</p>' : '') +
            (meta.length ? '<div class="fila-meta-taller">' + meta.join('') + '</div>' : '') +
            '<div class="fila-botones-resultado">' +
            botonMapa +
            '<a class="boton-accion-tarjeta boton-ficha" href="/detalle-taller?id=' + item.id + '">Ficha</a>' +
            '<a class="boton-accion-tarjeta boton-cita-tarjeta" href="/buscar-talleres?tallerCita=' + item.id + '&ver=solicitar">Cita</a>' +
            '</div>';

        if (marker) {
            var btnMapa = tarjeta.querySelector('.boton-centrar-mapa');
            btnMapa.addEventListener('click', function (ev) {
                ev.preventDefault();
                ev.stopPropagation();
                mapa.setView([item.lat, item.lng], 16, { animate: true });
                marker.openPopup();
            });
            tarjeta.addEventListener('click', function (event) {
                if (event.target && event.target.closest('a, button')) return;
                mapa.setView([item.lat, item.lng], 16, { animate: true });
                marker.openPopup();
            });
        }
        listaTalleres.appendChild(tarjeta);
    }

    // Contenido del popup del marcador Leaflet
    function popupHtml(item) {
        var estrellasTxt = item.numValoraciones > 0
            ? '★ ' + Math.round(item.estrellas) + '/5'
            : 'Sin valoraciones';
        return '<strong>' + escapeHtml(item.nombre) + '</strong><br>' +
            escapeHtml(item.direccion) + '<br>' +
            '<small>' + escapeHtml(item.horario) + '</small><br>' +
            '<small>' + escapeHtml(item.servicios) + '</small><br>' +
            '<small>' + estrellasTxt + '</small><br>' +
            '<a href="/detalle-taller?id=' + item.id + '">Ficha</a>';
    }

    // Marcadores solo si el taller tiene lat y lng
    function pintarEnMapa(lista) {
        limpiarResultados();
        lista.forEach(function (raw) {
            var item = raw.id != null ? raw : itemDesdeBd(raw);
            var marker = null;
            if (item.lat != null && item.lng != null) {
                marker = L.marker([item.lat, item.lng]).addTo(mapa);
                marker.bindPopup(popupHtml(item));
                talleres.push({ lat: item.lat, lng: item.lng, marker: marker });
            }
            crearTarjeta(item, marker);
        });
        if (talleres.length) {
            ajustarMapaAVista();
        }
    }

    // Relleno el bloque historial en buscar-talleres
    function pintarListaHistorial(items) {
        var contenedor = document.getElementById('historial-busquedas-contenedor');
        var vacio = document.getElementById('historial-vacio');
        if (!contenedor) return;
        contenedor.innerHTML = '';
        if (!items || !items.length) {
            if (vacio) vacio.style.display = 'block';
            return;
        }
        if (vacio) vacio.style.display = 'none';
        items.forEach(function (b) {
            var tarjeta = document.createElement('div');
            tarjeta.className = 'tarjeta tarjeta-historial-busqueda';
            tarjeta.innerHTML =
                '<p class="texto-busqueda-historial">' + escapeHtml(b.texto || b.termino || '') + '</p>' +
                '<p class="fecha-busqueda-historial texto-secundario">' + escapeHtml(b.fechaHora || '') + '</p>' +
                '<p><a href="#seccion-mapa" class="boton-secundario">Mapa</a></p>';
            contenedor.appendChild(tarjeta);
        });
    }

    // GET /api/historial-busqueda y pinto tarjetas
    function refrescarHistorialEnPantalla() {
        return fetch('/api/historial-busqueda', { credentials: 'same-origin' })
            .then(function (r) {
                if (!r.ok) return [];
                return r.json();
            })
            .then(function (items) {
                pintarListaHistorial(items);
            })
            .catch(function () {});
    }

    // Busqueda local en catalogoCompleto (sin llamar al servidor)
    function filtrarPorTexto(termino) {
        var t = (termino || '').toLowerCase().trim();
        if (!t) return catalogoCompleto.slice();
        return catalogoCompleto.filter(function (item) {
            return (item.nombre && item.nombre.toLowerCase().indexOf(t) >= 0) ||
                (item.ciudad && item.ciudad.toLowerCase().indexOf(t) >= 0) ||
                (item.direccion && item.direccion.toLowerCase().indexOf(t) >= 0) ||
                (item.servicios && item.servicios.toLowerCase().indexOf(t) >= 0);
        });
    }

    // POST a /api/historial-busqueda (no repito el mismo termino en 5 segundos)
    function guardarHistorial(termino, lat, lng, cantidad) {
        var ahora = Date.now();
        if (termino === ultimoHistorialTermino && ahora - ultimoHistorialMs < 5000) {
            return refrescarHistorialEnPantalla();
        }
        ultimoHistorialTermino = termino;
        ultimoHistorialMs = ahora;
        return fetch('/api/historial-busqueda', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                termino: termino,
                origenLat: lat,
                origenLng: lng,
                radioM: 0,
                resultados: cantidad
            })
        }).then(function (r) {
            if (r.ok) {
                return refrescarHistorialEnPantalla();
            }
        }).catch(function () {});
    }

    // Si no hay talleres en BD, busco el lugar en Nominatim para centrar el mapa
    function geocodificarLugar(termino) {
        var url = 'https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=co&q=' + encodeURIComponent(termino);
        return fetch(url).then(function (r) { return r.json(); });
    }

    // Leaflet recalcula tamano si el contenedor cambio
    function invalidarTamanoMapa() {
        mapa.invalidateSize();
    }

    if (typeof ResizeObserver !== 'undefined' && mapaContenedor.parentElement) {
        new ResizeObserver(invalidarTamanoMapa).observe(mapaContenedor.parentElement);
    }
    window.addEventListener('resize', invalidarTamanoMapa);

    // Circulo azul donde el usuario busco (ej. Cartagena)
    function colocarMarcadorOrigen(lat, lng, etiqueta) {
        if (marcadorOrigen) marcadorOrigen.remove();
        marcadorOrigen = L.circleMarker([lat, lng], {
            radius: 7,
            color: '#0ea5e9',
            weight: 2,
            fillColor: '#38bdf8',
            fillOpacity: 0.85
        }).addTo(mapa).bindPopup(etiqueta || '');
    }

    // Pinta talleres y ajusta zoom segun cuantos hay
    function centrarSegunResultados(lista, termino) {
        pintarEnMapa(lista);
        if (!lista.length) {
            return;
        }
        if (lista.length === 1 && lista[0].lat != null && lista[0].lng != null) {
            mapa.setView([lista[0].lat, lista[0].lng], 15, { animate: true });
            if (talleres.length && talleres[0].marker) {
                talleres[0].marker.openPopup();
            }
            return;
        }
        ajustarMapaAVista();
    }

    // Filtra catalogoCompleto; si vacio geocodifica el termino igual
    function ejecutarBusqueda() {
        if (busquedaEnCurso) return;
        var termino = inputBusqueda.value.trim() || 'Cartagena';
        busquedaEnCurso = true;
        botonBuscar.disabled = true;
        setEstado('Buscando…');

        var lista = filtrarPorTexto(termino);

        // Cierra busqueda: marcador origen, mapa, historial en BD
        function terminar(lat, lng, mensaje) {
            if (lat != null && lng != null) {
                colocarMarcadorOrigen(lat, lng, termino);
            }
            centrarSegunResultados(lista, termino);
            setEstado(mensaje);
            return guardarHistorial(termino, lat, lng, lista.length);
        }

        // Habilito de nuevo el boton buscar
        function finalizarBusqueda() {
            busquedaEnCurso = false;
            botonBuscar.disabled = false;
            requestAnimationFrame(function () {
                invalidarTamanoMapa();
                setTimeout(invalidarTamanoMapa, 120);
            });
        }

        if (lista.length > 0) {
            var ref = lista[0];
            var latRef = ref.lat != null ? ref.lat : 10.40;
            var lngRef = ref.lng != null ? ref.lng : -75.52;
            var msg = lista.length === 1
                ? '1 taller: ' + ref.nombre
                : lista.length + ' talleres · ' + termino;
            terminar(latRef, lngRef, msg).finally(finalizarBusqueda);
            return;
        }

        geocodificarLugar(termino)
            .then(function (geo) {
                var lat = 10.40;
                var lng = -75.52;
                if (geo && geo.length) {
                    lat = parseFloat(geo[0].lat);
                    lng = parseFloat(geo[0].lon);
                }
                mapa.setView([lat, lng], 12, { animate: true });
                return terminar(lat, lng, '0 talleres en BD para «' + termino + '»');
            })
            .catch(function () {
                return terminar(10.40, -75.52, '0 talleres en BD para «' + termino + '»');
            })
            .finally(finalizarBusqueda);
    }

    // Al cargar la pagina traigo todos los talleres de MySQL
    function cargarCatalogo() {
        setEstado('Cargando talleres…');
        return fetch('/api/talleres', { credentials: 'same-origin', cache: 'no-store' })
            .then(function (r) {
                if (!r.ok) throw new Error('api');
                return r.json();
            })
            .then(function (lista) {
                catalogoCompleto = lista.map(itemDesdeBd);
                if (!catalogoCompleto.length) {
                    setEstado('Sin talleres en MySQL (ejecutar sql/02_asistencia_en_carretera.sql)');
                    return;
                }
                inputBusqueda.value = 'Cartagena';
                pintarEnMapa(catalogoCompleto);
                setEstado(catalogoCompleto.length + ' talleres · MySQL');
                ajustarMapaAVista();
            })
            .catch(function () {
                setEstado('No se pudo cargar la lista de talleres. ¿Está corriendo el servidor?');
            });
    }

    botonVerMapa.addEventListener('click', function () {
        ajustarMapaAVista();
        invalidarTamanoMapa();
    });
    botonBuscar.addEventListener('click', ejecutarBusqueda);
    inputBusqueda.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            ejecutarBusqueda();
        }
    });

    setTimeout(function () {
        invalidarTamanoMapa();
        cargarCatalogo().then(function () {
            if (document.getElementById('historial-busquedas-contenedor')) {
                refrescarHistorialEnPantalla();
            }
        });
    }, 200);
})();
