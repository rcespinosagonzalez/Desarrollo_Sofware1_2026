// Boton Nominatim: lleno lat/lng antes de enviar el formulario al servidor

var btnCoordenadas = document.getElementById('btn-buscar-coordenadas');
var textoCoordenadas = document.getElementById('texto-buscar-coordenadas');
var campoDireccion = document.getElementById('dir-taller');
var campoCiudad = document.getElementById('ciudad-taller');
var campoLat = document.getElementById('lat-taller');
var campoLng = document.getElementById('lng-taller');

if (btnCoordenadas && campoDireccion && campoCiudad && campoLat && campoLng) {
    // Click: consulta Nominatim y escribe lat/lng en los inputs
    btnCoordenadas.onclick = function () {
        var direccion = campoDireccion.value.trim();
        var ciudad = campoCiudad.value.trim();
        if (!direccion && !ciudad) {
            if (textoCoordenadas) {
                textoCoordenadas.textContent = 'Escribe direccion o ciudad.';
            }
            return;
        }
        var consulta = direccion;
        if (ciudad) {
            consulta = consulta ? consulta + ', ' + ciudad : ciudad;
        }
        consulta = consulta + ', Colombia';

        if (textoCoordenadas) {
            textoCoordenadas.textContent = 'Buscando...';
        }

        var url = 'https://nominatim.openstreetmap.org/search?q='
            + encodeURIComponent(consulta)
            + '&format=json&limit=1';

        fetch(url)
            .then(function (respuesta) {
                return respuesta.json();
            })
            .then(function (lista) {
                if (!lista || !lista.length) {
                    if (textoCoordenadas) {
                        textoCoordenadas.textContent = 'No se encontro la direccion. Escribe latitud y longitud a mano.';
                    }
                    return;
                }
                campoLat.value = lista[0].lat;
                campoLng.value = lista[0].lon;
                if (textoCoordenadas) {
                    textoCoordenadas.textContent = 'Coordenadas listas para guardar.';
                }
            })
            .catch(function () {
                if (textoCoordenadas) {
                    textoCoordenadas.textContent = 'No se pudo consultar el mapa. Escribe latitud y longitud a mano.';
                }
            });
    };
}
