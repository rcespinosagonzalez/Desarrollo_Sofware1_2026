(function () {
    var inputBusqueda = document.getElementById('input-busqueda-lugar');
    var botonBuscar = document.getElementById('btn-buscar-lugar');
    var botonVerMapa = document.getElementById('btn-ver-en-mapa');
    var mapaContenedor = document.getElementById('mapa-talleres');
    var listaTalleres = document.getElementById('lista-talleres');
    var estadoBusqueda = document.getElementById('estado-busqueda');
    if (!inputBusqueda || !botonBuscar || !botonVerMapa || !mapaContenedor || !listaTalleres || !estadoBusqueda || typeof L === 'undefined') return;

    var talleres = [];
    var marcadorOrigen = null;
    var mapa = L.map('mapa-talleres', { zoomControl: true }).setView([9.8, -74.9], 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(mapa);

    function setEstado(texto) {
        estadoBusqueda.textContent = texto || '';
    }

    function limpiarResultados() {
        talleres.forEach(function (t) { t.marker.remove(); });
        talleres = [];
        listaTalleres.innerHTML = '';
    }

    function ajustarMapaAVista() {
        if (!talleres.length) return;
        var bounds = L.latLngBounds(talleres.map(function (t) { return [t.lat, t.lng]; }));
        if (marcadorOrigen) bounds.extend(marcadorOrigen.getLatLng());
        mapa.fitBounds(bounds.pad(0.3));
    }

    function escapeHtml(texto) {
        return (texto || '').replace(/[&<>"']/g, function (c) {
            var m = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
            return m[c];
        });
    }

    function etiquetaTipoLegible(tags, tipoCrudo) {
        var shop = (tags.shop || '').toLowerCase();
        var amenity = (tags.amenity || '').toLowerCase();
        var raw = (tipoCrudo || '').toLowerCase();
        if (amenity === 'car_repair' || shop === 'car_repair' || raw === 'car_repair') {
            return 'Taller mecánico · autos';
        }
        if (shop === 'motorcycle' || raw === 'motorcycle') {
            return 'Motos y repuestos';
        }
        if (amenity || shop) {
            return 'Servicio al vehículo';
        }
        return 'Taller o zona de servicio';
    }

    function crearTarjeta(item, marker) {
        var tarjeta = document.createElement('article');
        tarjeta.className = 'tarjeta tarjeta-taller';
        tarjeta.innerHTML =
            '<h3 class="nombre-taller">' + escapeHtml(item.nombre) + '</h3>' +
            '<p class="direccion-taller">' + escapeHtml(item.direccion) + '</p>' +
            '<span class="chip-tipo-taller">' + escapeHtml(item.tipoLegible) + '</span>' +
            '<div class="acciones-tarjeta-taller">' +
            '<button type="button" class="btn-centrar-mapa">Centrar en el mapa</button></div>';

        var btn = tarjeta.querySelector('.btn-centrar-mapa');
        btn.addEventListener('click', function (ev) {
            ev.preventDefault();
            ev.stopPropagation();
            mapa.setView([item.lat, item.lng], 15, { animate: true });
            marker.openPopup();
        });

        tarjeta.addEventListener('click', function (event) {
            if (event.target && event.target.closest('button')) return;
            mapa.setView([item.lat, item.lng], 15, { animate: true });
            marker.openPopup();
        });
        listaTalleres.appendChild(tarjeta);
    }

    function normalizarResultado(el) {
        var lat = typeof el.lat === 'number' ? el.lat : el.center && el.center.lat;
        var lng = typeof el.lon === 'number' ? el.lon : el.center && el.center.lon;
        if (typeof lat !== 'number' || typeof lng !== 'number') return null;

        var tags = el.tags || {};
        var nombre = tags.name || 'Taller automotriz';
        var direccion = tags['addr:full'] ||
            [tags['addr:street'], tags['addr:housenumber']].filter(Boolean).join(' ') ||
            tags['addr:city'] ||
            'Dirección no disponible';
        var tipoCrudo = tags['service:vehicle:type'] || tags.shop || tags.amenity || '';
        var tipoLegible = etiquetaTipoLegible(tags, tipoCrudo);
        return { nombre: nombre, direccion: direccion, tipoLegible: tipoLegible, lat: lat, lng: lng };
    }

    function geocodificarLugar(termino) {
        var url = 'https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=co&q=' + encodeURIComponent(termino);
        return fetch(url).then(function (r) { return r.json(); });
    }

    function buscarTalleres(lat, lng) {
        var query = '[out:json][timeout:25];(' +
            'node["amenity"="car_repair"](around:12000,' + lat + ',' + lng + ');' +
            'way["amenity"="car_repair"](around:12000,' + lat + ',' + lng + ');' +
            'relation["amenity"="car_repair"](around:12000,' + lat + ',' + lng + ');' +
            'node["shop"="car_repair"](around:12000,' + lat + ',' + lng + ');' +
            'way["shop"="car_repair"](around:12000,' + lat + ',' + lng + ');' +
            'node["shop"="motorcycle"](around:12000,' + lat + ',' + lng + ');' +
            'way["shop"="motorcycle"](around:12000,' + lat + ',' + lng + ');' +
            ');out center 120;';
        var url = 'https://overpass-api.de/api/interpreter?data=' + encodeURIComponent(query);
        return fetch(url).then(function (r) { return r.json(); });
    }

    function ejecutarBusqueda() {
        var termino = inputBusqueda.value.trim() || 'Cartagena';
        botonBuscar.disabled = true;
        setEstado('Buscando ubicación en ' + termino + '…');
        limpiarResultados();

        geocodificarLugar(termino)
            .then(function (geo) {
                if (!geo || !geo.length) throw new Error('sin-ubicacion');
                var lat = parseFloat(geo[0].lat);
                var lng = parseFloat(geo[0].lon);
                if (marcadorOrigen) marcadorOrigen.remove();
                marcadorOrigen = L.circleMarker([lat, lng], {
                    radius: 8,
                    color: '#0ea5e9',
                    weight: 2,
                    fillColor: '#38bdf8',
                    fillOpacity: 0.9
                }).addTo(mapa).bindPopup('Ubicación buscada: ' + termino);
                mapa.setView([lat, lng], 12, { animate: true });
                setEstado('Buscando talleres cercanos…');
                return buscarTalleres(lat, lng).then(function (data) {
                    var items = (data.elements || [])
                        .map(normalizarResultado)
                        .filter(Boolean);
                    if (!items.length) {
                        setEstado('No se encontraron talleres cercanos en esta zona.');
                        return;
                    }

                    items.forEach(function (item) {
                        var marker = L.marker([item.lat, item.lng]).addTo(mapa);
                        marker.bindPopup(
                            '<strong>' + escapeHtml(item.nombre) + '</strong><br>' +
                            escapeHtml(item.direccion) + '<br><small>' + escapeHtml(item.tipoLegible) + '</small>'
                        );
                        talleres.push({ lat: item.lat, lng: item.lng, marker: marker });
                        crearTarjeta(item, marker);
                    });
                    setEstado('Se encontraron ' + talleres.length + ' talleres cercanos en ' + termino + '.');
                    ajustarMapaAVista();
                    setTimeout(function () { mapa.invalidateSize(); }, 50);
                });
            })
            .catch(function () {
                setEstado('No fue posible consultar talleres en este momento.');
            })
            .finally(function () {
                botonBuscar.disabled = false;
            });
    }

    botonVerMapa.addEventListener('click', function () {
        ajustarMapaAVista();
    });

    botonBuscar.addEventListener('click', function () {
        ejecutarBusqueda();
    });

    inputBusqueda.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            ejecutarBusqueda();
        }
    });

    setTimeout(function () {
        mapa.invalidateSize();
        inputBusqueda.value = 'Cartagena';
        ejecutarBusqueda();
    }, 100);
})();
