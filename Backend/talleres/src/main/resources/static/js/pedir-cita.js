// Pedir cita: al tocar un servicio del taller se escribe en el motivo.
// Va en un archivo aparte porque la CSP no permite scripts dentro del HTML.
document.addEventListener('click', function (evento) {
    var boton = evento.target.closest('.chip-servicio');
    if (!boton) return;
    var motivo = document.getElementById('motivo-cita');
    if (!motivo) return;
    motivo.value = boton.getAttribute('data-servicio') || '';
    motivo.focus();
});
