// Pagina de perfil: ver/ocultar contraseñas, medidor de fortaleza y aviso si la confirmacion no coincide.
// Va en un archivo aparte porque la CSP no permite scripts dentro del HTML.
(function () {
    // Botones "Ver" / "Ocultar" junto a cada campo de contraseña
    document.addEventListener('click', function (evento) {
        var boton = evento.target.closest('.perfil-ojo');
        if (!boton) return;
        var campo = document.getElementById(boton.getAttribute('data-ver'));
        if (!campo) return;
        var mostrar = campo.type === 'password';
        campo.type = mostrar ? 'text' : 'password';
        boton.textContent = mostrar ? 'Ocultar' : 'Ver';
    });

    var nueva = document.getElementById('clave-nueva');
    var confirmacion = document.getElementById('clave-confirmacion');
    var fuerza = document.getElementById('clave-fuerza');
    var fuerzaTexto = document.getElementById('clave-fuerza-texto');
    var aviso = document.getElementById('clave-aviso');
    if (!nueva || !confirmacion || !fuerza) return;

    // 0 = vacia, 1 = floja ... 4 = muy buena (largo y variedad de caracteres)
    function nivel(clave) {
        if (!clave) return 0;
        var puntos = 0;
        if (clave.length >= 8) puntos++;
        if (clave.length >= 12) puntos++;
        if (/[a-z]/.test(clave) && /[A-Z]/.test(clave)) puntos++;
        if (/\d/.test(clave) && /[^A-Za-z0-9]/.test(clave)) puntos++;
        return Math.max(1, puntos);
    }

    var TEXTOS = ['Mínimo 8 caracteres', 'Débil', 'Aceptable', 'Buena', 'Muy buena'];

    function pintarFuerza() {
        var n = nivel(nueva.value);
        if (nueva.value && nueva.value.length < 8) n = 1;
        fuerza.setAttribute('data-nivel', String(n));
        fuerzaTexto.textContent = TEXTOS[n];
    }

    function revisarConfirmacion() {
        var difiere = confirmacion.value !== '' && confirmacion.value !== nueva.value;
        aviso.hidden = !difiere;
        confirmacion.setCustomValidity(difiere ? 'Las contraseñas no coinciden.' : '');
    }

    nueva.addEventListener('input', function () {
        pintarFuerza();
        revisarConfirmacion();
    });
    confirmacion.addEventListener('input', revisarConfirmacion);
})();
