// Pide confirmacion antes de enviar los formularios marcados con data-confirmar (cancelar cita).
// Va en un archivo aparte porque la CSP no permite onclick ni scripts dentro del HTML.
document.addEventListener('submit', function (evento) {
    var mensaje = evento.target.getAttribute('data-confirmar');
    if (mensaje && !window.confirm(mensaje)) {
        evento.preventDefault();
    }
});
