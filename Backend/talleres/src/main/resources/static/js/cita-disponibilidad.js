// Formulario "Pedir cita": al elegir taller y fecha pide a la API las horas con mecanico libre
// y las muestra en la lista de horas (solo se puede agendar dentro del horario del taller).
(function () {
    var selTaller = document.getElementById('select-taller-cita');
    var inpFecha = document.getElementById('fecha-cita');
    var selHora = document.getElementById('hora-cita');
    if (!selTaller || !inpFecha || !selHora) return;

    var pedido = 0; // para ignorar respuestas viejas si el usuario cambia rapido de opcion

    // No dejo elegir fechas pasadas (el servidor lo vuelve a validar)
    var hoy = new Date();
    var mm = String(hoy.getMonth() + 1).padStart(2, '0');
    var dd = String(hoy.getDate()).padStart(2, '0');
    inpFecha.min = hoy.getFullYear() + '-' + mm + '-' + dd;

    function mensaje(texto) {
        selHora.innerHTML = '';
        var op = document.createElement('option');
        op.value = '';
        op.disabled = true;
        op.selected = true;
        op.textContent = texto;
        selHora.appendChild(op);
    }

    function cargar() {
        var tallerId = selTaller.value;
        var fecha = inpFecha.value;
        if (!tallerId || !fecha) {
            mensaje('Elige taller y fecha primero');
            return;
        }
        var mio = ++pedido;
        mensaje('Buscando horas libres...');
        // En "Editar cita" la propia cita no debe contar como hora ocupada
        var excluir = selTaller.getAttribute('data-excluir-cita');
        var url = '/api/talleres/' + encodeURIComponent(tallerId) + '/disponibilidad?fecha=' + encodeURIComponent(fecha)
            + (excluir ? '&excluirCita=' + encodeURIComponent(excluir) : '');
        fetch(url)
            .then(function (r) {
                if (!r.ok) throw new Error('http ' + r.status);
                return r.json();
            })
            .then(function (data) {
                if (mio !== pedido) return;
                if (!data.horas || !data.horas.length) {
                    mensaje('Sin horas libres ese día');
                    return;
                }
                selHora.innerHTML = '';
                var primera = document.createElement('option');
                primera.value = '';
                primera.disabled = true;
                primera.selected = true;
                primera.textContent = 'Elige una hora';
                selHora.appendChild(primera);
                // En "Editar cita" dejo marcada la hora actual si sigue en el mismo dia
                var horaActual = selHora.getAttribute('data-hora-actual');
                var mismoDia = selHora.getAttribute('data-fecha-actual') === fecha;
                data.horas.forEach(function (h) {
                    var op = document.createElement('option');
                    op.value = h;
                    op.textContent = h;
                    if (horaActual && mismoDia && h === horaActual) {
                        op.selected = true;
                        primera.selected = false;
                    }
                    selHora.appendChild(op);
                });
            })
            .catch(function () {
                if (mio === pedido) mensaje('No se pudo consultar las horas');
            });
    }

    selTaller.addEventListener('change', cargar);
    inpFecha.addEventListener('change', cargar);
    cargar();
})();
