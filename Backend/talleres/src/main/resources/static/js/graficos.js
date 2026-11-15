// Graficos sencillos para los dashboards (SVG y CSS, sin librerias externas). Todo texto se pone con textContent.
(function () {
    'use strict';

    var NS = 'http://www.w3.org/2000/svg';
    var COLORES = ['#2563eb', '#fbbf24', '#16a34a', '#ef4444', '#8b5cf6', '#06b6d4'];

    function svg(tag, atributos) {
        var e = document.createElementNS(NS, tag);
        Object.keys(atributos || {}).forEach(function (k) { e.setAttribute(k, atributos[k]); });
        return e;
    }

    function html(tag, clase, texto) {
        var e = document.createElement(tag);
        if (clase) e.className = clase;
        if (texto !== undefined) e.textContent = texto;
        return e;
    }

    function vaciar(c) {
        while (c.firstChild) c.removeChild(c.firstChild);
    }

    function suma(valores) {
        return valores.reduce(function (a, b) { return a + b; }, 0);
    }

    function numero(n) {
        return Number(n).toLocaleString('es-CO');
    }

    function vacio(c, mensaje) {
        vaciar(c);
        c.appendChild(html('p', 'graf-vacio', mensaje || 'Sin datos todavía'));
    }

    // Barras verticales: una por etiqueta, con el valor encima
    function barras(c, etiquetas, valores, titulo) {
        vaciar(c);
        if (!valores || !valores.length || suma(valores) === 0) return vacio(c);
        var W = 520, H = 230, padB = 28, padT = 22;
        var maximo = Math.max.apply(null, valores);
        var ancho = W / valores.length;
        var bw = Math.min(44, ancho * 0.62);
        var raiz = svg('svg', { viewBox: '0 0 ' + W + ' ' + H, role: 'img', 'class': 'graf-svg',
            'aria-label': (titulo || 'Gráfico de barras') + ': ' + etiquetas.map(function (e, i) { return e + ' ' + valores[i]; }).join(', ') });
        for (var g = 0; g <= 3; g++) {
            var y = padT + (H - padT - padB) * g / 3;
            raiz.appendChild(svg('line', { x1: 0, x2: W, y1: y, y2: y, 'class': 'graf-guia' }));
        }
        valores.forEach(function (v, i) {
            var h = maximo === 0 ? 0 : (H - padT - padB) * v / maximo;
            var x = ancho * i + (ancho - bw) / 2;
            var yTop = H - padB - h;
            var barra = svg('rect', { x: x, y: yTop, width: bw, height: Math.max(h, v > 0 ? 2 : 0), rx: 6, 'class': 'graf-barra' });
            var tip = svg('title');
            tip.textContent = etiquetas[i] + ': ' + numero(v);
            barra.appendChild(tip);
            raiz.appendChild(barra);
            var valor = svg('text', { x: x + bw / 2, y: yTop - 6, 'text-anchor': 'middle', 'class': 'graf-valor' });
            valor.textContent = numero(v);
            raiz.appendChild(valor);
            var etiqueta = svg('text', { x: x + bw / 2, y: H - 8, 'text-anchor': 'middle', 'class': 'graf-etiqueta' });
            etiqueta.textContent = etiquetas[i];
            raiz.appendChild(etiqueta);
        });
        c.appendChild(raiz);
    }

    // Barras horizontales (rankings): etiqueta, barra proporcional y valor
    function barrasH(c, etiquetas, valores) {
        vaciar(c);
        if (!valores || !valores.length || suma(valores) === 0) return vacio(c);
        var maximo = Math.max.apply(null, valores);
        var lista = html('ul', 'graf-h');
        valores.forEach(function (v, i) {
            var fila = html('li');
            fila.appendChild(html('span', 'graf-h-etiqueta', etiquetas[i]));
            var pista = html('span', 'graf-h-pista');
            var relleno = html('span', 'graf-h-relleno');
            relleno.style.width = (maximo === 0 ? 0 : Math.round(100 * v / maximo)) + '%';
            pista.appendChild(relleno);
            fila.appendChild(pista);
            fila.appendChild(html('span', 'graf-h-valor', numero(v)));
            lista.appendChild(fila);
        });
        c.appendChild(lista);
    }

    // Dona con leyenda; en el centro va el total
    function donut(c, etiquetas, valores, colores) {
        vaciar(c);
        var total = suma(valores || []);
        if (!total) return vacio(c);
        colores = colores || COLORES;
        var R = 60, GROSOR = 26, LARGO = 2 * Math.PI * R;
        var contenedor = html('div', 'graf-donut');
        var raiz = svg('svg', { viewBox: '0 0 160 160', role: 'img', 'class': 'graf-donut-svg',
            'aria-label': etiquetas.map(function (e, i) { return e + ' ' + valores[i]; }).join(', ') });
        raiz.appendChild(svg('circle', { cx: 80, cy: 80, r: R, fill: 'none', stroke: '#e2e8f0', 'stroke-width': GROSOR }));
        var acumulado = 0;
        valores.forEach(function (v, i) {
            if (!v) return;
            var largo = LARGO * v / total;
            raiz.appendChild(svg('circle', {
                cx: 80, cy: 80, r: R, fill: 'none', stroke: colores[i % colores.length], 'stroke-width': GROSOR,
                'stroke-dasharray': largo + ' ' + (LARGO - largo), 'stroke-dashoffset': -acumulado,
                transform: 'rotate(-90 80 80)'
            }));
            acumulado += largo;
        });
        var centro = svg('text', { x: 80, y: 86, 'text-anchor': 'middle', 'class': 'graf-donut-total' });
        centro.textContent = numero(total);
        raiz.appendChild(centro);
        contenedor.appendChild(raiz);
        var leyenda = html('ul', 'graf-leyenda');
        valores.forEach(function (v, i) {
            var fila = html('li');
            var punto = html('i');
            punto.style.background = colores[i % colores.length];
            fila.appendChild(punto);
            fila.appendChild(html('span', 'graf-leyenda-nombre', etiquetas[i]));
            fila.appendChild(html('strong', null, numero(v) + ' · ' + Math.round(100 * v / total) + '%'));
            leyenda.appendChild(fila);
        });
        contenedor.appendChild(leyenda);
        c.appendChild(contenedor);
    }

    window.Graficos = { barras: barras, barrasH: barrasH, donut: donut, vacio: vacio, numero: numero, html: html, vaciar: vaciar };
})();
