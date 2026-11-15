// Pruebas de coherencia del generador de datos demo. Ejecutar: node mongo/seed.test.js
// No necesita MongoDB: solo comprueba que los datos generados cumplen las reglas de la aplicacion.
'use strict';
const assert = require('assert');
const path = require('path');

globalThis.SEED_SOLO_GENERAR = true;
const seed = require(path.join(__dirname, 'seed.js'));

let contador = 0;
function nuevoId() {
    const hex = (++contador).toString(16).padStart(24, '0');
    return { toString: () => hex };
}

const ahora = new Date('2026-11-13T19:00:00Z');
const d = seed.generar({ nuevoId, ahora, semilla: 7 });
const d2 = (() => { contador = 0; return seed.generar({ nuevoId, ahora, semilla: 7 }); })();
let ok = 0;
function prueba(nombre, fn) { fn(); ok++; console.log('  ok  ' + nombre); }
const clave = x => String(x);

prueba('cantidades: 1000 talleres, 1 admin, 300 clientes, 1000 duenos', () => {
    assert.strictEqual(d.talleres.length, 1000);
    assert.strictEqual(d.usuarios.length, 1 + 300 + 1000);
    assert.ok(d.citas.length > 3000, 'citas: ' + d.citas.length);
    assert.ok(d.valoraciones.length > 500);
});

prueba('es determinista: misma semilla, mismos datos', () => {
    const quitar = x => JSON.stringify(x, (k, v) => v);
    assert.strictEqual(quitar(d.talleres.map(t => t.nombre)), quitar(d2.talleres.map(t => t.nombre)));
    assert.strictEqual(d.citas.length, d2.citas.length);
});

prueba('correos unicos y en minusculas; clave BCrypt; roles validos', () => {
    const vistos = new Set();
    for (const u of d.usuarios) {
        assert.strictEqual(u.email, u.email.toLowerCase());
        assert.ok(!vistos.has(u.email), 'repetido ' + u.email);
        vistos.add(u.email);
        assert.ok(/^\$2a\$10\$.{53}$/.test(u.passwordHash));
        assert.ok(['admin', 'taller', 'user'].includes(u.rol));
        assert.strictEqual(u.activo, true);
    }
    assert.strictEqual(d.usuarios.filter(u => u.rol === 'admin').length, 1);
});

prueba('cada taller tiene un dueno distinto con rol taller', () => {
    const duenos = new Map(d.usuarios.map(u => [clave(u._id), u]));
    const usados = new Set();
    for (const t of d.talleres) {
        const u = duenos.get(clave(t.propietarioId));
        assert.ok(u && u.rol === 'taller');
        assert.ok(!usados.has(clave(u._id)));
        usados.add(clave(u._id));
    }
});

prueba('ubicacion GeoJSON [lng, lat] dentro de Colombia', () => {
    for (const t of d.talleres) {
        const p = t.ubicacion.punto;
        assert.strictEqual(p.type, 'Point');
        const [lng, lat] = p.coordinates;
        assert.ok(lng > -80 && lng < -66, 'lng ' + lng);
        assert.ok(lat > -5 && lat < 13, 'lat ' + lat);
        assert.ok(t.ubicacion.direccion && t.ubicacion.ciudad && t.ubicacion.pais === 'CO');
    }
});

prueba('horarios: dias 1-7 sin repetir, apertura < cierre y al menos un turno', () => {
    for (const t of d.talleres) {
        assert.ok(t.horarios.length > 0);
        const dias = new Set();
        for (const h of t.horarios) {
            assert.ok(h.dia >= 1 && h.dia <= 7 && !dias.has(h.dia));
            dias.add(h.dia);
            assert.ok(/^\d\d:\d\d$/.test(h.apertura) && /^\d\d:\d\d$/.test(h.cierre));
            assert.ok(h.apertura < h.cierre);
            assert.ok(seed.turnos(h.apertura, h.cierre).length >= 1);
        }
    }
});

prueba('mecanicos: entre 1 y 5, al menos uno activo, ids unicos', () => {
    const ids = new Set();
    for (const t of d.talleres) {
        assert.ok(t.mecanicos.length >= 1 && t.mecanicos.length <= 5);
        assert.ok(t.mecanicos.some(m => m.activo));
        for (const m of t.mecanicos) {
            assert.ok(!ids.has(clave(m._id)));
            ids.add(clave(m._id));
        }
    }
});

prueba('citas: cada una cae en un turno valido del horario de su taller (hora local)', () => {
    const porId = new Map(d.talleres.map(t => [clave(t._id), t]));
    for (const c of d.citas) {
        const t = porId.get(clave(c.tallerId));
        assert.ok(t, 'taller inexistente');
        const local = new Date(c.fechaHora.getTime() - 5 * 3600 * 1000);
        const dia = seed.diaSemanaLocal(c.fechaHora);
        const h = t.horarios.find(x => x.dia === dia);
        assert.ok(h, 'cita en un dia que el taller no atiende');
        const hora = local.toISOString().slice(11, 16);
        assert.ok(seed.turnos(h.apertura, h.cierre).includes(hora), 'hora fuera de turno ' + hora);
        assert.strictEqual(local.getUTCSeconds(), 0);
    }
});

prueba('citas: el mecanico pertenece al taller y nadie tiene dos citas activas a la misma hora', () => {
    const porId = new Map(d.talleres.map(t => [clave(t._id), t]));
    const turnos = new Set();
    for (const c of d.citas) {
        const t = porId.get(clave(c.tallerId));
        const m = t.mecanicos.find(x => clave(x._id) === clave(c.mecanicoId));
        assert.ok(m, 'mecanico de otro taller');
        assert.strictEqual(c.mecanicoNombre, (m.nombres + ' ' + m.apellidos).trim());
        assert.strictEqual(c.tallerNombre, t.nombre);
        assert.strictEqual(c.ocupaTurno, c.estado !== 'cancelada');
        if (c.ocupaTurno) {
            assert.ok(m.activo, 'cita activa con mecanico dado de baja');
            const k = clave(c.mecanicoId) + '|' + c.fechaHora.getTime();
            assert.ok(!turnos.has(k), 'mecanico con dos citas a la misma hora');
            turnos.add(k);
        }
    }
});

prueba('citas: estados coherentes con el tiempo', () => {
    for (const c of d.citas) {
        const pasada = c.fechaHora <= ahora;
        if (pasada) assert.ok(['completada', 'cancelada'].includes(c.estado), c.estado);
        else assert.ok(['solicitada', 'confirmada', 'cancelada'].includes(c.estado), c.estado);
        assert.ok(c.creadoEn <= ahora);
    }
    assert.ok(d.citas.some(c => c.estado === 'solicitada' && c.fechaHora > ahora));
    assert.ok(d.citas.some(c => c.estado === 'cancelada'));
});

prueba('citas y valoraciones apuntan a clientes que existen', () => {
    const clientes = new Set(d.usuarios.filter(u => u.rol === 'user').map(u => clave(u._id)));
    for (const c of d.citas) assert.ok(clientes.has(clave(c.clienteId)));
    for (const v of d.valoraciones) assert.ok(clientes.has(clave(v.clienteId)));
    for (const b of d.busquedas) assert.ok(clientes.has(clave(b.usuarioId)));
});

prueba('valoraciones: 1-5 estrellas, las de cita son de una cita completada, una por cita', () => {
    const citas = new Map(d.citas.map(c => [clave(c._id), c]));
    const vistas = new Set();
    for (const v of d.valoraciones) {
        assert.ok(Number.isInteger(v.estrellas) && v.estrellas >= 1 && v.estrellas <= 5);
        assert.ok(v.comentario && v.servicio);
        assert.ok(v.creadoEn <= ahora);
        if (v.citaId) {
            const c = citas.get(clave(v.citaId));
            assert.ok(c && c.estado === 'completada');
            assert.strictEqual(clave(c.tallerId), clave(v.tallerId));
            assert.strictEqual(clave(c.clienteId), clave(v.clienteId));
            assert.ok(!vistas.has(clave(v.citaId)));
            vistas.add(clave(v.citaId));
        }
    }
});

prueba('resumen del taller = suma y total de sus valoraciones', () => {
    const suma = new Map(), total = new Map();
    for (const v of d.valoraciones) {
        const k = clave(v.tallerId);
        suma.set(k, (suma.get(k) || 0) + v.estrellas);
        total.set(k, (total.get(k) || 0) + 1);
    }
    for (const t of d.talleres) {
        assert.strictEqual(t.resumen.suma, suma.get(clave(t._id)) || 0);
        assert.strictEqual(t.resumen.total, total.get(clave(t._id)) || 0);
    }
});

prueba('busquedas: dentro de la ventana del TTL (180 dias) y con coordenadas', () => {
    for (const b of d.busquedas) {
        const dias = (ahora - b.creadoEn) / 86400000;
        assert.ok(dias >= 0 && dias < 175, 'dias ' + dias);
        assert.ok(b.termino && b.origen === 'buscador');
        assert.ok(typeof b.origenLat === 'number' && typeof b.origenLng === 'number');
    }
});

prueba('ids de documentos unicos en cada coleccion y sin campos internos', () => {
    for (const col of ['usuarios', 'talleres', 'citas', 'valoraciones', 'busquedas']) {
        const ids = new Set(d[col].map(x => clave(x._id)));
        assert.strictEqual(ids.size, d[col].length, col);
    }
    for (const t of d.talleres) assert.ok(!('_plan' in t));
});

prueba('clientes: de 1 a 3 vehiculos con placa, tipo, marca, modelo, anio y color', () => {
    const clientes = d.usuarios.filter(u => u.rol === 'user');
    for (const c of clientes) {
        assert.ok(Array.isArray(c.vehiculos) && c.vehiculos.length >= 1 && c.vehiculos.length <= 3);
        const placas = new Set();
        for (const v of c.vehiculos) {
            assert.ok(/^[A-Z]{3}(\d{3}|\d{2}[A-Z])$/.test(v.placa), 'placa ' + v.placa);
            assert.ok(!placas.has(v.placa), 'placa repetida ' + v.placa);
            placas.add(v.placa);
            assert.ok(['carro', 'camioneta', 'moto'].includes(v.tipo));
            assert.ok(v.marca && v.modelo && v.color && v.anio >= 2008 && v.anio <= 2025);
            assert.ok(v._id);
        }
    }
    // Solo los clientes llevan vehiculos
    for (const u of d.usuarios.filter(x => x.rol !== 'user')) assert.ok(!('vehiculos' in u));
});

prueba('talleres destacados: algunos con plan vigente y algunos vencidos', () => {
    const vigentes = d.talleres.filter(t => t.destacadoHasta && t.destacadoHasta > ahora);
    const vencidos = d.talleres.filter(t => t.destacadoHasta && t.destacadoHasta <= ahora);
    assert.ok(vigentes.length >= 40 && vigentes.length <= 140, 'vigentes ' + vigentes.length);
    assert.ok(vencidos.length >= 5, 'vencidos ' + vencidos.length);
});

console.log('\n' + ok + ' pruebas correctas. Citas: ' + d.citas.length + ', valoraciones: ' + d.valoraciones.length +
    ', busquedas: ' + d.busquedas.length);
