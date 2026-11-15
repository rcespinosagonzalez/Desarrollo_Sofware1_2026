// Alta o edicion de taller en pasos: arma el texto de servicios con casillas, ubica el taller en un mapa,
// busca coordenadas en Nominatim y muestra una vista previa. El formulario envia los mismos campos de siempre.
(function () {
    'use strict';

    var form = document.getElementById('form-taller');
    if (!form) return;
    document.body.classList.add('con-js');

    function $(id) { return document.getElementById(id); }
    function norm(t) { return (t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, ''); }

    // ------------------------------------------------------------ servicios (mismas categorias que los filtros del mapa)
    var GRUPOS = [
        { nombre: 'Mantenimiento', items: [
            { t: 'Mecánica general', k: ['mecanica'] }, { t: 'Aceite y lubricación', k: ['aceite', 'lubric'] },
            { t: 'Sincronización', k: ['sincroniz'] }, { t: 'Embrague', k: ['embrague'] }] },
        { nombre: 'Frenos y suspensión', items: [
            { t: 'Frenos', k: ['freno'] }, { t: 'Suspensión', k: ['suspension'] },
            { t: 'Alineación y balanceo', k: ['alineacion', 'balanceo'] }] },
        { nombre: 'Eléctrico y diagnóstico', items: [
            { t: 'Electricidad', k: ['electric'] }, { t: 'Escáner', k: ['escaner'] },
            { t: 'Diagnóstico', k: ['diagnostic'] }, { t: 'Cambio de batería', k: ['bateria'] }] },
        { nombre: 'Otros servicios', items: [
            { t: 'Llantas', k: ['llanta', 'neumatic'] }, { t: 'Latonería y pintura', k: ['latoneria', 'pintura'] },
            { t: 'Aire acondicionado', k: ['aire acondicionado'] }, { t: 'Grúa y remolque', k: ['grua', 'remolque'] }] }
    ];

    var campoServicios = $('serv-taller');
    var campoOtros = $('otros-servicios');
    var contenedorServicios = $('rt-servicios');

    function construirServicios() {
        GRUPOS.forEach(function (g) {
            var bloque = document.createElement('fieldset');
            bloque.className = 'rt-grupo-servicios';
            var leyenda = document.createElement('legend');
            leyenda.textContent = g.nombre;
            bloque.appendChild(leyenda);
            g.items.forEach(function (s) {
                var etiqueta = document.createElement('label');
                etiqueta.className = 'rt-servicio';
                var caja = document.createElement('input');
                caja.type = 'checkbox';
                caja.setAttribute('data-servicio', s.t);
                var texto = document.createElement('span');
                texto.textContent = s.t;
                etiqueta.appendChild(caja);
                etiqueta.appendChild(texto);
                bloque.appendChild(etiqueta);
            });
            contenedorServicios.appendChild(bloque);
        });
    }

    function partirServicios(texto) {
        return (texto || '').split(/\s+-\s+|\s*[,;|]\s*/).map(function (s) { return s.trim(); }).filter(Boolean);
    }

    // Marca las casillas que coinciden con el texto guardado; lo que no coincide pasa a "otros"
    function cargarServiciosGuardados() {
        var otros = [];
        partirServicios(campoServicios.value).forEach(function (token) {
            var n = norm(token);
            var encontrado = null;
            GRUPOS.forEach(function (g) {
                g.items.forEach(function (s) {
                    if (!encontrado && (norm(s.t) === n || s.k.some(function (k) { return n.indexOf(k) >= 0; }))) encontrado = s;
                });
            });
            var caja = encontrado && contenedorServicios.querySelector('input[data-servicio="' + encontrado.t + '"]');
            if (caja) caja.checked = true; else otros.push(token);
        });
        campoOtros.value = otros.join(', ');
    }

    // Texto final que se envia: casillas marcadas + otros, separados por " - "
    function armarServicios() {
        var elegidos = Array.prototype.map.call(
            contenedorServicios.querySelectorAll('input[data-servicio]:checked'),
            function (c) { return c.getAttribute('data-servicio'); });
        var otros = partirServicios(campoOtros.value);
        campoServicios.value = elegidos.concat(otros).join(' - ');
        return elegidos.concat(otros);
    }

    // ------------------------------------------------------------ dias y horario
    var cajasDias = form.querySelectorAll('input[name="diasAtencion"]');
    var NOMBRES_DIAS = { 1: 'Lun', 2: 'Mar', 3: 'Mié', 4: 'Jue', 5: 'Vie', 6: 'Sáb', 7: 'Dom' };

    function diasMarcados() {
        return Array.prototype.filter.call(cajasDias, function (c) { return c.checked; })
            .map(function (c) { return parseInt(c.value, 10); }).sort();
    }

    // [1,2,3,4,5] -> "Lun–Vie"; [1,3] -> "Lun, Mié"
    function textoDias(dias) {
        if (!dias.length) return '';
        var consecutivos = dias.every(function (d, i) { return i === 0 || d === dias[i - 1] + 1; });
        if (dias.length >= 3 && consecutivos) return NOMBRES_DIAS[dias[0]] + '–' + NOMBRES_DIAS[dias[dias.length - 1]];
        return dias.map(function (d) { return NOMBRES_DIAS[d]; }).join(', ');
    }

    Array.prototype.forEach.call(form.querySelectorAll('.rt-atajo[data-dias]'), function (b) {
        b.addEventListener('click', function () {
            var dias = b.getAttribute('data-dias').split(',').map(Number);
            Array.prototype.forEach.call(cajasDias, function (c) { c.checked = dias.indexOf(parseInt(c.value, 10)) >= 0; });
            actualizarVistaPrevia();
        });
    });

    // ------------------------------------------------------------ vista previa
    function actualizarVistaPrevia() {
        $('vp-nombre').textContent = $('nom-taller').value.trim() || 'Nombre del taller';
        $('vp-ciudad').textContent = [$('dir-taller').value.trim(), $('ciudad-taller').value.trim()].filter(Boolean).join(' · ') || 'Dirección y ciudad';
        var dias = textoDias(diasMarcados());
        var ap = $('hor-apertura').value, ci = $('hor-cierre').value;
        $('vp-horario').textContent = dias && ap && ci ? dias + ' · ' + ap + ' – ' + ci : 'Horario de atención';
        $('vp-telefono').textContent = $('tel-taller').value.trim() || 'Teléfono';
        var lista = $('vp-servicios');
        while (lista.firstChild) lista.removeChild(lista.firstChild);
        armarServicios().slice(0, 8).forEach(function (s) {
            var li = document.createElement('li');
            li.textContent = s;
            lista.appendChild(li);
        });
        $('vp-descripcion').textContent = $('desc-taller').value.trim();
        $('desc-contador').textContent = $('desc-taller').value.length;
    }

    // ------------------------------------------------------------ mapa para ubicar el taller
    var campoLat = $('lat-taller');
    var campoLng = $('lng-taller');
    var mapa = null;
    var marcador = null;

    function fijarPunto(lat, lng, centrar) {
        campoLat.value = Number(lat).toFixed(6);
        campoLng.value = Number(lng).toFixed(6);
        if (!mapa) return;
        if (!marcador) {
            marcador = L.marker([lat, lng], { draggable: true, title: 'Arrastra para afinar la ubicación' }).addTo(mapa);
            marcador.on('dragend', function () {
                var p = marcador.getLatLng();
                fijarPunto(p.lat, p.lng, false);
            });
        } else {
            marcador.setLatLng([lat, lng]);
        }
        if (centrar) mapa.setView([lat, lng], Math.max(mapa.getZoom(), 16));
    }

    function iniciarMapa() {
        if (mapa || !window.L) return;
        var lat = parseFloat(campoLat.value), lng = parseFloat(campoLng.value);
        var hayPunto = !isNaN(lat) && !isNaN(lng);
        mapa = L.map('rt-mapa').setView(hayPunto ? [lat, lng] : [10.40, -75.52], hayPunto ? 16 : 12);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19, attribution: '© OpenStreetMap'
        }).addTo(mapa);
        mapa.on('click', function (ev) { fijarPunto(ev.latlng.lat, ev.latlng.lng, false); });
        if (hayPunto) fijarPunto(lat, lng, false);
    }

    function moverMarcadorDesdeCampos() {
        var lat = parseFloat(campoLat.value), lng = parseFloat(campoLng.value);
        if (!isNaN(lat) && !isNaN(lng) && mapa) fijarPunto(lat, lng, true);
    }
    campoLat.addEventListener('change', moverMarcadorDesdeCampos);
    campoLng.addEventListener('change', moverMarcadorDesdeCampos);

    var btnCoordenadas = $('btn-buscar-coordenadas');
    var textoCoordenadas = $('texto-buscar-coordenadas');
    btnCoordenadas.addEventListener('click', function () {
        var direccion = $('dir-taller').value.trim();
        var ciudad = $('ciudad-taller').value.trim();
        if (!direccion && !ciudad) {
            textoCoordenadas.textContent = 'Escribe la dirección o la ciudad.';
            return;
        }
        textoCoordenadas.textContent = 'Buscando…';
        var consulta = (direccion ? direccion + ', ' : '') + (ciudad ? ciudad + ', ' : '') + 'Colombia';
        fetch('https://nominatim.openstreetmap.org/search?q=' + encodeURIComponent(consulta) + '&format=json&limit=1')
            .then(function (r) { return r.json(); })
            .then(function (lista) {
                if (!lista || !lista.length) {
                    textoCoordenadas.textContent = 'No se encontró la dirección. Haz clic en el mapa para ubicarla.';
                    return;
                }
                fijarPunto(parseFloat(lista[0].lat), parseFloat(lista[0].lon), true);
                textoCoordenadas.textContent = 'Ubicación encontrada. Puedes arrastrar el marcador para afinarla.';
            })
            .catch(function () {
                textoCoordenadas.textContent = 'No se pudo consultar el mapa. Haz clic en el mapa para ubicarla.';
            });
    });

    // ------------------------------------------------------------ pasos
    var pasos = Array.prototype.slice.call(form.querySelectorAll('.rt-paso'));
    var indicador = $('rt-pasos');
    var btnAtras = $('rt-atras');
    var btnSiguiente = $('rt-siguiente');
    var btnGuardar = $('rt-guardar');
    var cajaError = $('rt-error');
    var actual = 0;

    pasos.forEach(function (p, i) {
        var li = document.createElement('li');
        li.className = 'rt-paso-item';
        var numero = document.createElement('span');
        numero.className = 'rt-paso-numero';
        numero.textContent = String(i + 1);
        var titulo = document.createElement('span');
        titulo.className = 'rt-paso-titulo';
        titulo.textContent = p.getAttribute('data-titulo');
        li.appendChild(numero);
        li.appendChild(titulo);
        indicador.appendChild(li);
    });

    function mostrarError(mensaje) {
        cajaError.textContent = mensaje || '';
        cajaError.hidden = !mensaje;
    }

    // Revisa un paso: campos nativos (required, minlength...) y reglas propias. Devuelve true si esta bien.
    function validarPaso(i) {
        mostrarError('');
        var paso = pasos[i];
        var campos = paso.querySelectorAll('input, select, textarea');
        for (var k = 0; k < campos.length; k++) {
            if (!campos[k].checkValidity()) {
                irAPaso(i);
                campos[k].reportValidity();
                return false;
            }
        }
        var n = parseInt(paso.getAttribute('data-paso'), 10);
        if (n === 2) {
            if (!armarServicios().length) {
                irAPaso(i);
                mostrarError('Elige al menos un servicio.');
                return false;
            }
            if (!diasMarcados().length) {
                irAPaso(i);
                mostrarError('Elige al menos un día de atención.');
                return false;
            }
            if ($('hor-apertura').value >= $('hor-cierre').value) {
                irAPaso(i);
                mostrarError('La hora de apertura debe ser anterior a la de cierre.');
                return false;
            }
        }
        return true;
    }

    function irAPaso(i) {
        actual = i;
        pasos.forEach(function (p, k) { p.hidden = k !== i; });
        Array.prototype.forEach.call(indicador.children, function (li, k) {
            li.classList.toggle('rt-paso-activo', k === i);
            li.classList.toggle('rt-paso-hecho', k < i);
            if (k === i) li.setAttribute('aria-current', 'step'); else li.removeAttribute('aria-current');
        });
        var ultimo = i === pasos.length - 1;
        btnAtras.hidden = i === 0;
        btnSiguiente.hidden = ultimo;
        btnGuardar.hidden = !ultimo;
        if (pasos[i].querySelector('#rt-mapa')) {
            iniciarMapa();
            if (mapa) setTimeout(function () { mapa.invalidateSize(); }, 50);
        }
    }

    btnSiguiente.addEventListener('click', function () {
        if (validarPaso(actual)) {
            irAPaso(actual + 1);
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    });
    btnAtras.addEventListener('click', function () {
        mostrarError('');
        irAPaso(actual - 1);
    });

    // Enter en un campo de un paso intermedio avanza en vez de enviar; al guardar se revisan todos los pasos
    form.addEventListener('submit', function (ev) {
        if (actual < pasos.length - 1) {
            ev.preventDefault();
            btnSiguiente.click();
            return;
        }
        for (var i = 0; i < pasos.length; i++) {
            if (!validarPaso(i)) {
                ev.preventDefault();
                return;
            }
        }
        armarServicios();
    });

    // ------------------------------------------------------------ generador de contrasena (alta de dueño)
    var btnClave = $('btn-generar-clave');
    if (btnClave) {
        btnClave.addEventListener('click', function () {
            var minusculas = 'abcdefghijkmnopqrstuvwxyz', mayusculas = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
            var numeros = '23456789', simbolos = '!@#$%&*?';
            var todo = minusculas + mayusculas + numeros + simbolos;
            function azar(n) {
                var a = new Uint32Array(1);
                var limite = Math.floor(4294967296 / n) * n;
                do { window.crypto.getRandomValues(a); } while (a[0] >= limite);
                return a[0] % n;
            }
            var chars = [minusculas, mayusculas, numeros, simbolos].map(function (s) { return s.charAt(azar(s.length)); });
            while (chars.length < 14) chars.push(todo.charAt(azar(todo.length)));
            for (var i = chars.length - 1; i > 0; i--) {
                var j = azar(i + 1), t = chars[i];
                chars[i] = chars[j];
                chars[j] = t;
            }
            var campo = $('clave-dueno');
            campo.value = chars.join('');
            campo.type = 'text';
            var ojo = form.querySelector('.perfil-ojo[data-ver="clave-dueno"]');
            if (ojo) ojo.textContent = 'Ocultar';
        });
    }

    // ------------------------------------------------------------ arranque
    construirServicios();
    cargarServiciosGuardados();
    contenedorServicios.addEventListener('change', actualizarVistaPrevia);
    form.addEventListener('input', actualizarVistaPrevia);
    form.addEventListener('change', actualizarVistaPrevia);
    actualizarVistaPrevia();
    irAPaso(0);
})();
