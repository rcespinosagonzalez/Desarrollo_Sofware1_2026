// Si la URL trae ?ver=seccion, baja a esa seccion y limpia el parametro (antes era un script inline)
(function () {
    var params = new URLSearchParams(window.location.search);
    var ver = params.get('ver');
    if (ver) {
        var el = document.getElementById('seccion-' + ver);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
        params.delete('ver');
        var qs = params.toString();
        var path = window.location.pathname + (qs ? '?' + qs : '');
        history.replaceState({}, '', path);
    }
})();
