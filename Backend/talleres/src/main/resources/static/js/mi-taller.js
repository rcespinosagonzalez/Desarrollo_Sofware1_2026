// Admin: filtro y paginacion en tablas de talleres, citas y valoraciones (solo en el navegador)

var paneles = document.querySelectorAll('.panel-paginable');

for (var i = 0; i < paneles.length; i++) {
    iniciarPanel(paneles[i]);
}

// Si la URL trae #seccion-talleres, baja hasta esa seccion
var hash = window.location.hash;
if (hash) {
    var seccion = document.querySelector(hash);
    if (seccion) {
        seccion.scrollIntoView();
    }
}

// Cada seccion con clase panel-paginable tiene su propia pagina y buscador
function iniciarPanel(panel) {
    var cuerpoTabla = panel.querySelector('.cuerpo-tabla');
    if (!cuerpoTabla) {
        return;
    }

    var filas = cuerpoTabla.querySelectorAll('.fila-tabla');
    var cajaBuscar = panel.querySelector('.input-buscar');
    var listaPorPagina = panel.querySelector('.select-por-pagina');
    var textoContador = panel.querySelector('.texto-contador');
    var zonaPaginacion = panel.querySelector('.paginacion-botones');
    var avisoSinResultados = panel.querySelector('.aviso-sin-resultados');
    var tabla = panel.querySelector('.tabla-listado');
    var contenedorTabla = panel.querySelector('.panel-admin-tabla-envoltorio');

    var paginaActual = 1;
    var porPagina = 8;
    if (listaPorPagina && listaPorPagina.value) {
        porPagina = parseInt(listaPorPagina.value, 10);
    }

    // Texto para filtrar: atributo data-texto-busqueda o contenido de la fila
    function textoFila(fila) {
        var datos = fila.getAttribute('data-texto-busqueda');
        if (datos) {
            return datos.toLowerCase();
        }
        return fila.textContent.toLowerCase();
    }

    // Filas que contienen lo escrito en el input de busqueda
    function filasQueCoinciden() {
        var busqueda = '';
        if (cajaBuscar && cajaBuscar.value) {
            busqueda = cajaBuscar.value.trim().toLowerCase();
        }
        if (busqueda === '') {
            return filas;
        }

        var resultado = [];
        for (var f = 0; f < filas.length; f++) {
            if (textoFila(filas[f]).indexOf(busqueda) !== -1) {
                resultado.push(filas[f]);
            }
        }
        return resultado;
    }

    // Oculto todas las filas y muestro solo la pagina actual
    function mostrarTabla() {
        var visibles = filasQueCoinciden();
        var total = visibles.length;
        var totalPaginas = Math.ceil(total / porPagina);
        if (totalPaginas < 1) {
            totalPaginas = 1;
        }
        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas;
        }

        for (var r = 0; r < filas.length; r++) {
            filas[r].style.display = 'none';
        }

        var desde = (paginaActual - 1) * porPagina;
        var hasta = desde + porPagina;
        for (var v = desde; v < hasta && v < total; v++) {
            visibles[v].style.display = '';
        }

        if (textoContador) {
            if (total === 0) {
                textoContador.textContent = '0 resultados';
            } else {
                var fin = hasta;
                if (fin > total) {
                    fin = total;
                }
                textoContador.textContent = 'Mostrando ' + (desde + 1) + '-' + fin + ' de ' + total;
            }
        }

        var noHayCoincidencias = total === 0 && filas.length > 0;
        if (avisoSinResultados) {
            avisoSinResultados.style.display = noHayCoincidencias ? 'block' : 'none';
        }
        if (tabla) {
            tabla.style.display = noHayCoincidencias ? 'none' : '';
        }
        if (contenedorTabla) {
            contenedorTabla.style.display = noHayCoincidencias ? 'none' : '';
        }

        pintarBotonesPagina(zonaPaginacion, paginaActual, totalPaginas, noHayCoincidencias);
    }

    // Botones Anterior, numeros y Siguiente
    function pintarBotonesPagina(zona, pagina, totalPaginas, ocultar) {
        if (!zona) {
            return;
        }
        zona.innerHTML = '';
        if (ocultar || totalPaginas <= 1) {
            zona.style.display = 'none';
            return;
        }
        zona.style.display = 'flex';

        crearBoton(zona, 'Anterior', pagina - 1, pagina <= 1, false);
        for (var p = 1; p <= totalPaginas; p++) {
            crearBoton(zona, String(p), p, false, p === pagina);
        }
        crearBoton(zona, 'Siguiente', pagina + 1, pagina >= totalPaginas, false);
    }

    // Un boton de pagina; activo = pagina actual
    function crearBoton(zona, texto, numeroPagina, desactivado, activo) {
        var boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'paginacion-admin-btn';
        if (activo) {
            boton.className += ' activo';
        }
        boton.textContent = texto;
        boton.disabled = desactivado;
        if (!desactivado) {
            boton.onclick = function () {
                paginaActual = numeroPagina;
                mostrarTabla();
            };
        }
        zona.appendChild(boton);
    }

    if (cajaBuscar) {
        cajaBuscar.oninput = function () {
            paginaActual = 1;
            mostrarTabla();
        };
    }
    if (listaPorPagina) {
        listaPorPagina.onchange = function () {
            porPagina = parseInt(listaPorPagina.value, 10);
            paginaActual = 1;
            mostrarTabla();
        };
    }

    mostrarTabla();
}
