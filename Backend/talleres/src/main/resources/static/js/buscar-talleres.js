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
    var listaBase = [];   // talleres de la ultima busqueda (o todos), antes de aplicar los filtros del panel
    var filtros = null;   // panel de filtros (filtros-talleres.js)
    var partida = null;   // punto de partida del usuario: { lat, lng, tipo: 'gps' | 'mapa', precision }
    var marcadorPartida = null;
    var circuloPrecision = null;
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
            horarios: t.horarios || [],
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
        if (typeof item.distanciaKm === 'number') {
            meta.push('<span class="meta-taller meta-distancia">a ' +
                (item.distanciaKm < 10 ? item.distanciaKm.toFixed(1) : Math.round(item.distanciaKm)) + ' km</span>');
        }
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
            '<a class="boton-accion-tarjeta boton-cita-tarjeta" href="/solicitar-cita?tallerId=' + encodeURIComponent(item.id) + '">Cita</a>' +
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
        if (!lista.length) {
            mostrarVacio();
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
            listaBase = lista;
            if (filtros) filtros.actualizarConteos(listaBase);
            var vis = visibles(lista);
            centrarSegunResultados(vis, termino);
            // Con filtros activos el contador dice "5 de 12 talleres"
            var conFiltros = filtros && filtros.hayFiltros() && lista.length > 0;
            setEstado(conFiltros ? textoConteo(vis.length, lista.length) + ' · ' + termino : mensaje);
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

    // Lista lateral sin resultados: distingue "los filtros son muy estrictos" de "no hay talleres para esa busqueda"
    function mostrarVacio() {
        var conFiltros = filtros && filtros.hayFiltros() && listaBase.length > 0;
        var caja = document.createElement('div');
        caja.className = 'resultados-vacio';
        caja.innerHTML =
            '<p class="resultados-vacio-titulo">' + (conFiltros ? 'Ningún taller cumple estos filtros' : 'No encontramos talleres') + '</p>' +
            '<p class="resultados-vacio-texto">' + (conFiltros
                ? 'Quita alguno para ampliar los resultados.'
                : 'Prueba con otra ciudad o con el nombre de un taller.') + '</p>' +
            (conFiltros ? '<button type="button" class="boton-accion-tarjeta boton-limpiar-vacio">Quitar filtros</button>' : '');
        var boton = caja.querySelector('.boton-limpiar-vacio');
        if (boton) boton.addEventListener('click', function () { filtros.limpiar(); });
        listaTalleres.appendChild(caja);
    }

    // Talleres de la lista que pasan los filtros del panel, ya ordenados
    function visibles(lista) {
        return filtros ? filtros.aplicar(lista) : lista;
    }

    function textoConteo(n, total) {
        if (!total) return '0 talleres';
        var palabra = total === 1 ? ' taller' : ' talleres';
        return filtros && filtros.hayFiltros() ? n + ' de ' + total + palabra : total + palabra;
    }

    // Repinta mapa y lista con la lista base y los filtros actuales (se llama al cambiar un filtro)
    function actualizarVista() {
        var vis = visibles(listaBase);
        pintarEnMapa(vis);
        setEstado(textoConteo(vis.length, listaBase.length));
    }

    // Al cargar la pagina traigo todos los talleres de MongoDB
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
                    setEstado('Sin talleres en la base (ejecutar mongo/seed.js, ver README)');
                    return;
                }
                inputBusqueda.value = 'Cartagena';
                listaBase = catalogoCompleto;
                if (filtros) filtros.actualizarConteos(listaBase);
                actualizarVista();
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

    // ---- Punto de partida: ubicacion estimada (GPS del navegador) o un punto marcado en el mapa
    var CLAVE_PARTIDA = 'partidaTalleres';
    var botonUbicacion = document.getElementById('boton-mi-ubicacion');
    var ayudaPartida = document.getElementById('partida-ayuda');
    var chipPartida = document.getElementById('partida-chip');
    var textoPartida = document.getElementById('partida-texto');
    var botonQuitarPartida = document.getElementById('boton-quitar-partida');
    var TEXTO_AYUDA = 'o haz clic en el mapa para marcar tu punto de partida.';

    function textoDePartida(p) {
        if (p.tipo === 'gps') {
            return 'Ubicación estimada' + (p.precision ? ' (± ' + Math.round(p.precision) + ' m)' : '');
        }
        return 'Punto marcado en el mapa';
    }

    function guardarPartida() {
        try {
            if (partida) sessionStorage.setItem(CLAVE_PARTIDA, JSON.stringify(partida));
            else sessionStorage.removeItem(CLAVE_PARTIDA);
        } catch (e) { /* sessionStorage no disponible: la partida solo dura en esta pantalla */ }
    }

    // opc.sinCentrar: no mover el mapa (al arrastrar el marcador o al restaurar); opc.silencioso: no repintar
    function fijarPartida(lat, lng, tipo, precision, opc) {
        opc = opc || {};
        partida = { lat: lat, lng: lng, tipo: tipo, precision: precision || null };
        if (!marcadorPartida) {
            marcadorPartida = L.marker([lat, lng], {
                draggable: true,
                title: 'Tu punto de partida (puedes arrastrarlo)',
                zIndexOffset: 1000,
                icon: L.divIcon({ className: 'punto-partida', iconSize: [22, 22], iconAnchor: [11, 11] })
            }).addTo(mapa);
            marcadorPartida.on('dragend', function () {
                var p = marcadorPartida.getLatLng();
                fijarPartida(p.lat, p.lng, 'mapa', null, { sinCentrar: true });
            });
        } else {
            marcadorPartida.setLatLng([lat, lng]);
        }
        if (circuloPrecision) {
            circuloPrecision.remove();
            circuloPrecision = null;
        }
        if (precision) {
            circuloPrecision = L.circle([lat, lng], { radius: precision, className: 'circulo-precision', interactive: false }).addTo(mapa);
        }
        textoPartida.textContent = textoDePartida(partida);
        chipPartida.hidden = false;
        ayudaPartida.hidden = true;
        guardarPartida();
        if (!opc.sinCentrar) {
            mapa.setView([lat, lng], Math.max(mapa.getZoom(), 13));
        }
        if (filtros) filtros.alFijarPartida();
        if (!opc.silencioso) actualizarVista();
    }

    function quitarPartida() {
        partida = null;
        if (marcadorPartida) {
            marcadorPartida.remove();
            marcadorPartida = null;
        }
        if (circuloPrecision) {
            circuloPrecision.remove();
            circuloPrecision = null;
        }
        chipPartida.hidden = true;
        ayudaPartida.hidden = false;
        ayudaPartida.textContent = TEXTO_AYUDA;
        guardarPartida();
        if (filtros) filtros.refrescar();
        actualizarVista();
    }

    // Pide la ubicacion al navegador. Si no se puede, el usuario puede marcar el punto en el mapa
    function pedirUbicacion() {
        if (!navigator.geolocation) {
            ayudaPartida.hidden = false;
            ayudaPartida.textContent = 'Tu navegador no da la ubicación. Haz clic en el mapa para marcar tu punto de partida.';
            return;
        }
        botonUbicacion.disabled = true;
        ayudaPartida.hidden = false;
        ayudaPartida.textContent = 'Buscando tu ubicación…';
        navigator.geolocation.getCurrentPosition(function (pos) {
            botonUbicacion.disabled = false;
            fijarPartida(pos.coords.latitude, pos.coords.longitude, 'gps', pos.coords.accuracy);
        }, function (error) {
            botonUbicacion.disabled = false;
            ayudaPartida.hidden = false;
            ayudaPartida.textContent = (error && error.code === 1
                ? 'No diste permiso para usar tu ubicación.'
                : 'No pudimos obtener tu ubicación.') + ' Haz clic en el mapa para marcar tu punto de partida.';
        }, { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 });
    }

    if (botonUbicacion && ayudaPartida && chipPartida && textoPartida && botonQuitarPartida) {
        botonUbicacion.addEventListener('click', pedirUbicacion);
        botonQuitarPartida.addEventListener('click', quitarPartida);
        // Un clic en el mapa (no en un marcador) marca el punto de partida
        mapa.on('click', function (ev) {
            fijarPartida(ev.latlng.lat, ev.latlng.lng, 'mapa', null);
        });
        // Si la persona ya habia fijado su punto en esta sesion, lo recupero sin mover el mapa
        try {
            var guardada = JSON.parse(sessionStorage.getItem(CLAVE_PARTIDA) || 'null');
            if (guardada && typeof guardada.lat === 'number' && typeof guardada.lng === 'number') {
                fijarPartida(guardada.lat, guardada.lng, guardada.tipo === 'gps' ? 'gps' : 'mapa',
                    guardada.precision, { sinCentrar: true, silencioso: true });
            }
        } catch (e) { /* dato guardado danado: lo ignoro */ }
    }

    // Panel de filtros: al cambiar algo repinta; las distancias salen del punto de partida
    if (window.FiltrosTalleres) {
        filtros = window.FiltrosTalleres.iniciar({
            alCambiar: actualizarVista,
            getOrigenPartida: function () {
                return partida ? { lat: partida.lat, lng: partida.lng } : null;
            },
            pedirUbicacion: pedirUbicacion,
            getOrigenBusqueda: function () {
                if (!marcadorOrigen) return null;
                var p = marcadorOrigen.getLatLng();
                return { lat: p.lat, lng: p.lng };
            }
        });
        // Si recupere un punto de partida de la sesion, el panel pasa a ordenar por cercania
        if (filtros && partida) filtros.alFijarPartida();
    }

    setTimeout(function () {
        invalidarTamanoMapa();
        cargarCatalogo().then(function () {
            if (document.getElementById('historial-busquedas-contenedor')) {
                refrescarHistorialEnPantalla();
            }
        });
    }, 200);
})();
