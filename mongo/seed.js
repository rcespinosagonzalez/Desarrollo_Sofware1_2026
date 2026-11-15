// Datos de demostracion para MongoDB: ~1.000 talleres coherentes con sus duenos, mecanicos, citas,
// valoraciones y busquedas. Es un generador con semilla fija: mismas reglas, mismos datos.
//
// Compatible con mongosh y con el shell antiguo de MongoDB 4.4 (JavaScript ES5).
// Uso (base vacia):      mongosh "<MONGODB_URI>" mongo/seed.js
// Docker Compose:        lo ejecuta solo la primera vez que arranca el contenedor de MongoDB
// Borrar y regenerar:    SEED_FORCE=1 mongosh "<MONGODB_URI>" mongo/seed.js   (BORRA los datos actuales)
//
// Todos los usuarios demo tienen la clave  demo12345  (guardada con BCrypt).
//   admin@demo.local · cliente1@demo.local ... cliente300@demo.local · taller1@demo.local ... taller1000@demo.local
//
// Las pruebas de coherencia estan en mongo/seed.test.js (node mongo/seed.test.js).

(function () {
    'use strict';

    var HASH_DEMO = '$2a$10$Q0M1oAmF4t/C/bBzFhxsc.ZHkQK.By/uJnr5SoUDq8fRuyUqSz5/O';
    // Colombia no tiene horario de verano: la hora local es siempre UTC-5
    var DESFASE_HORAS = 5;
    var DIA_MS = 24 * 3600 * 1000;

    // Ciudades: centro, peso (cuantos talleres tiene) y radio de dispersion en grados (~0.01 = 1,1 km)
    var CIUDADES = [
        { n: 'Cartagena', d: 'Bolivar', lat: 10.3910, lng: -75.4794, peso: 14, r: 0.05 },
        { n: 'Turbaco', d: 'Bolivar', lat: 10.3333, lng: -75.4167, peso: 3, r: 0.02 },
        { n: 'Barranquilla', d: 'Atlantico', lat: 10.9685, lng: -74.7813, peso: 14, r: 0.06 },
        { n: 'Santa Marta', d: 'Magdalena', lat: 11.2408, lng: -74.1990, peso: 6, r: 0.04 },
        { n: 'Monteria', d: 'Cordoba', lat: 8.7500, lng: -75.8814, peso: 5, r: 0.04 },
        { n: 'Sincelejo', d: 'Sucre', lat: 9.3047, lng: -75.3978, peso: 3, r: 0.03 },
        { n: 'Bogota', d: 'Cundinamarca', lat: 4.7110, lng: -74.0721, peso: 22, r: 0.09 },
        { n: 'Medellin', d: 'Antioquia', lat: 6.2442, lng: -75.5812, peso: 14, r: 0.06 },
        { n: 'Cali', d: 'Valle del Cauca', lat: 3.4516, lng: -76.5320, peso: 10, r: 0.06 },
        { n: 'Bucaramanga', d: 'Santander', lat: 7.1193, lng: -73.1227, peso: 6, r: 0.04 },
        { n: 'Cucuta', d: 'Norte de Santander', lat: 7.8939, lng: -72.5078, peso: 4, r: 0.04 }
    ];

    var TIPOS = ['Taller', 'Mecanica', 'Electroautos', 'Frenos y Suspension', 'Motos', 'Llantas y Alineacion',
        'Diesel', 'Autoservicio', 'Lubricentro', 'Multiservicios'];
    var NOMBRES_TALLER = ['El Rapido', 'La Candelaria', 'San Francisco', 'Bocagrande', 'Getsemani', 'El Bosque',
        'Bazurto', 'Manga', 'Centenario', 'Del Caribe', 'Los Andes', 'La Paz', 'El Progreso', 'Santa Ana',
        'Las Palmas', 'El Triunfo', 'La Esperanza', 'Alta Gama', 'Express', 'Don Carlos', 'Los Hermanos',
        'El Maestro', 'Nueva Era', 'Mi Carro', 'Turbo Center', 'Pit Stop', 'Norte', 'Sur', 'Central'];
    var CALLES = ['Calle', 'Carrera', 'Avenida', 'Transversal', 'Diagonal'];
    var SERVICIOS = ['Mecanica general', 'Aceite', 'Frenos', 'Diagnostico', 'Suspension', 'Electricidad',
        'Alineacion y balanceo', 'Llantas', 'Aire acondicionado', 'Escaner', 'Grua', 'Latoneria y pintura',
        'Cambio de bateria', 'Embrague', 'Sincronizacion'];
    var NOMBRES = ['Jorge', 'Luis', 'Carlos', 'Andres', 'Juan', 'Miguel', 'Pedro', 'Jose', 'Diego', 'Camilo',
        'Ricardo', 'Fernando', 'Hector', 'Oscar', 'Mario', 'Sergio', 'Daniel', 'Alberto', 'Cesar', 'Javier'];
    var APELLIDOS = ['Ramirez', 'Gomez', 'Perez', 'Rodriguez', 'Martinez', 'Lopez', 'Garcia', 'Hernandez',
        'Torres', 'Diaz', 'Vargas', 'Castro', 'Ortiz', 'Rojas', 'Mendoza', 'Herrera', 'Suarez', 'Cardenas'];
    var NOMBRES_CLIENTE = ['Maria', 'Laura', 'Camila', 'Valentina', 'Sofia', 'Daniela', 'Paula', 'Andrea',
        'Natalia', 'Carolina', 'Santiago', 'Mateo', 'Sebastian', 'Nicolas', 'Samuel', 'David', 'Felipe', 'Tomas'];

    // Patrones de horario: dias (1 = lunes ... 7 = domingo), apertura y cierre
    var HORARIOS = [
        { peso: 40, dias: [1, 2, 3, 4, 5, 6], ap: '08:00', ci: '18:00' },
        { peso: 20, dias: [1, 2, 3, 4, 5], ap: '07:00', ci: '17:00' },
        { peso: 12, dias: [1, 2, 3, 4, 5, 6, 7], ap: '07:00', ci: '19:00' },
        { peso: 10, dias: [1, 2, 3, 4, 5, 6], ap: '07:30', ci: '17:30' },
        { peso: 10, dias: [1, 2, 3, 4, 5], ap: '08:00', ci: '16:00' },
        { peso: 8, dias: [1, 2, 3, 4, 5, 6], ap: '09:00', ci: '20:00' }
    ];

    var MOTIVOS = ['Cambio de aceite y filtros', 'Revision de frenos', 'El carro no enciende', 'Ruido en la suspension',
        'Revision antes de viaje', 'Luz del motor encendida', 'Cambio de llantas', 'Alineacion y balanceo',
        'Recalentamiento del motor', 'Falla en el aire acondicionado', 'Cambio de bateria', 'Vibracion al frenar'];
    var COMENTARIOS = {
        5: ['Excelente atencion y rapidos.', 'Muy profesionales, resolvieron el problema el mismo dia.',
            'Buen precio y trabajo garantizado.', 'Totalmente recomendado.'],
        4: ['Buen servicio, algo de espera.', 'Trabajo bien hecho, volveria.', 'Atencion amable y precio justo.'],
        3: ['Servicio aceptable, demoraron mas de lo dicho.', 'Cumplieron, aunque sin mucha explicacion.'],
        2: ['Tardaron demasiado y el precio subio.', 'El problema volvio a la semana.'],
        1: ['No resolvieron la falla.', 'Mala atencion.']
    };
    var TERMINOS = ['taller mecanico', 'frenos', 'cambio de aceite', 'grua', 'llantas', 'electricidad automotriz',
        'taller cerca', 'alineacion', 'bateria', 'diagnostico'];

    // Vehiculos de demostracion: marca, modelos y tipo (carro, camioneta o moto)
    var VEHICULOS = [
        { m: 'Chevrolet', t: 'carro', mod: ['Spark', 'Sail', 'Onix', 'Joy'] },
        { m: 'Chevrolet', t: 'camioneta', mod: ['Tracker', 'Captiva', 'D-Max'] },
        { m: 'Renault', t: 'carro', mod: ['Logan', 'Sandero', 'Kwid', 'Stepway'] },
        { m: 'Renault', t: 'camioneta', mod: ['Duster', 'Koleos'] },
        { m: 'Mazda', t: 'carro', mod: ['2', '3', 'CX-30'] },
        { m: 'Mazda', t: 'camioneta', mod: ['CX-5', 'BT-50'] },
        { m: 'Kia', t: 'carro', mod: ['Picanto', 'Rio', 'Cerato'] },
        { m: 'Kia', t: 'camioneta', mod: ['Sportage', 'Seltos'] },
        { m: 'Toyota', t: 'carro', mod: ['Yaris', 'Corolla'] },
        { m: 'Toyota', t: 'camioneta', mod: ['Hilux', 'Prado', 'Fortuner'] },
        { m: 'Nissan', t: 'carro', mod: ['March', 'Versa', 'Sentra'] },
        { m: 'Hyundai', t: 'carro', mod: ['i10', 'Accent', 'Elantra'] },
        { m: 'Yamaha', t: 'moto', mod: ['FZ 150', 'MT-03', 'XTZ 125'] },
        { m: 'Honda', t: 'moto', mod: ['CB 190R', 'XR 150L', 'Wave 110'] },
        { m: 'Suzuki', t: 'moto', mod: ['Gixxer 150', 'DR 150', 'AX4'] },
        { m: 'Bajaj', t: 'moto', mod: ['Pulsar NS 200', 'Boxer CT 100'] }
    ];
    var COLORES_VEHICULO = ['Blanco', 'Negro', 'Gris', 'Rojo', 'Azul', 'Plata', 'Verde', 'Vinotinto'];
    var LETRAS_PLACA = 'ABCDEFGHJKLMNPRSTUVWXYZ';

    // De 1 a 3 vehiculos por cliente (55 % uno, 35 % dos, 10 % tres), sin placas repetidas dentro del mismo cliente
    function vehiculosAleatorios(azar, nuevoId) {
        var cuantos = azar.peso([{ peso: 55, v: 1 }, { peso: 35, v: 2 }, { peso: 10, v: 3 }]).v;
        var lista = [], placas = {};
        while (lista.length < cuantos) {
            var base = azar.de(VEHICULOS);
            var placa = '';
            for (var i = 0; i < 3; i++) placa += LETRAS_PLACA.charAt(azar.entre(0, LETRAS_PLACA.length - 1));
            if (base.t === 'moto') {
                placa += String(azar.entre(10, 99)) + LETRAS_PLACA.charAt(azar.entre(0, LETRAS_PLACA.length - 1));
            } else {
                placa += String(azar.entre(100, 999));
            }
            if (placas[placa]) continue;
            placas[placa] = true;
            lista.push({ _id: nuevoId(), placa: placa, tipo: base.t, marca: base.m, modelo: azar.de(base.mod),
                anio: azar.entre(2008, 2025), color: azar.de(COLORES_VEHICULO) });
        }
        return lista;
    }

    // Generador pseudoaleatorio con semilla (mulberry32): mismos numeros en cada ejecucion
    function crearAzar(semilla) {
        var a = semilla >>> 0;
        function siguiente() {
            a = (a + 0x6D2B79F5) >>> 0;
            var t = a;
            t = Math.imul(t ^ (t >>> 15), t | 1);
            t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
        }
        return {
            n: siguiente,
            entre: function (min, max) { return min + Math.floor(siguiente() * (max - min + 1)); },
            de: function (lista) { return lista[Math.floor(siguiente() * lista.length)]; },
            peso: function (lista) {
                var total = 0, i;
                for (i = 0; i < lista.length; i++) total += lista[i].peso;
                var x = siguiente() * total;
                for (i = 0; i < lista.length; i++) {
                    x -= lista[i].peso;
                    if (x < 0) return lista[i];
                }
                return lista[lista.length - 1];
            },
            mezclar: function (lista) {
                var c = lista.slice();
                for (var i = c.length - 1; i > 0; i--) {
                    var j = Math.floor(siguiente() * (i + 1));
                    var t = c[i]; c[i] = c[j]; c[j] = t;
                }
                return c;
            }
        };
    }

    function minutos(hhmm) { return parseInt(hhmm.slice(0, 2), 10) * 60 + parseInt(hhmm.slice(3), 10); }
    function hhmm(min) {
        var h = Math.floor(min / 60), m = min % 60;
        return (h < 10 ? '0' : '') + h + ':' + (m < 10 ? '0' : '') + m;
    }
    function redondear(x, dec) { var f = Math.pow(10, dec); return Math.round(x * f) / f; }

    // Inicios de turno de un dia: apertura, +1 h, ... mientras el turno de 1 hora termine antes o justo al cierre
    // (la misma regla que util/HorarioAtencion.turnos en Java)
    function turnos(ap, ci) {
        var lista = [];
        for (var t = minutos(ap); t + 60 <= minutos(ci); t += 60) lista.push(hhmm(t));
        return lista;
    }

    // Fecha local (aaaa, mes 0-11, dia) + "HH:mm" -> instante UTC
    function aUtc(anio, mes, dia, hora) {
        var m = minutos(hora);
        return new Date(Date.UTC(anio, mes, dia, Math.floor(m / 60) + DESFASE_HORAS, m % 60));
    }

    // Dia de la semana local (1 = lunes ... 7 = domingo) de un instante UTC
    function diaSemanaLocal(fechaUtc) {
        var local = new Date(fechaUtc.getTime() - DESFASE_HORAS * 3600 * 1000);
        var d = local.getUTCDay();
        return d === 0 ? 7 : d;
    }

    function generar(opc) {
        var azar = crearAzar(opc.semilla || 20261111);
        var ahora = opc.ahora || new Date();
        var nuevoId = opc.nuevoId;
        var nTalleres = opc.nTalleres || 1000;
        var nClientes = opc.nClientes || 300;

        var usuarios = [], talleres = [], citas = [], valoraciones = [], busquedas = [];

        function usuario(email, nombre, rol) {
            var u = { _id: nuevoId(), email: email, passwordHash: HASH_DEMO, nombre: nombre, rol: rol, activo: true,
                creadoEn: new Date(ahora.getTime() - azar.entre(100, 400) * DIA_MS) };
            usuarios.push(u);
            return u;
        }

        usuario('admin@demo.local', 'Administrador Demo', 'admin');
        var clientes = [];
        for (var c = 1; c <= nClientes; c++) {
            clientes.push(usuario('cliente' + c + '@demo.local',
                azar.de(NOMBRES_CLIENTE) + ' ' + azar.de(APELLIDOS) + ' ' + azar.de(APELLIDOS), 'user'));
        }

        // ---- Talleres con su dueno, ubicacion, horario y mecanicos
        var usados = {};
        for (var i = 1; i <= nTalleres; i++) {
            var ciudad = azar.peso(CIUDADES);
            var nombre, intentos = 0;
            do {
                nombre = azar.de(TIPOS) + ' ' + azar.de(NOMBRES_TALLER);
                intentos++;
                // Mismo nombre solo se repite si es en otra ciudad
            } while (usados[nombre + '|' + ciudad.n] && intentos < 20);
            if (usados[nombre + '|' + ciudad.n]) nombre = nombre + ' ' + i;
            usados[nombre + '|' + ciudad.n] = true;

            var dueno = usuario('taller' + i + '@demo.local', 'Dueno ' + nombre, 'taller');
            var plan = azar.peso(HORARIOS);
            var nMec = azar.peso([{ peso: 15, v: 1 }, { peso: 35, v: 2 }, { peso: 30, v: 3 }, { peso: 15, v: 4 }, { peso: 5, v: 5 }]).v;
            var mecanicos = [];
            for (var m = 0; m < nMec; m++) {
                mecanicos.push({
                    _id: nuevoId(),
                    nombres: azar.de(NOMBRES),
                    apellidos: azar.de(APELLIDOS),
                    telefono: '3' + azar.entre(0, 2) + azar.entre(0, 9) + String(azar.entre(1000000, 9999999)),
                    // Un mecanico de cada diez esta dado de baja, pero nunca el unico del taller
                    activo: !(nMec > 1 && m === nMec - 1 && azar.n() < 0.1)
                });
            }
            // Radio con raiz cuadrada: reparte los puntos parejo dentro del circulo
            var ang = azar.n() * 2 * Math.PI, rad = Math.sqrt(azar.n()) * ciudad.r;
            var lat = redondear(ciudad.lat + Math.sin(ang) * rad, 6);
            var lng = redondear(ciudad.lng + Math.cos(ang) * rad, 6);
            var servicios = azar.mezclar(SERVICIOS).slice(0, azar.entre(3, 6)).join(' - ');
            var creado = new Date(ahora.getTime() - azar.entre(30, 365) * DIA_MS);

            talleres.push({
                _id: nuevoId(),
                propietarioId: dueno._id,
                nombre: nombre,
                telefono: '60' + azar.entre(1, 8) + String(azar.entre(1000000, 9999999)),
                descripcion: 'Taller en ' + ciudad.n + ' con servicio de ' + servicios.toLowerCase().split(' - ').slice(0, 3).join(', ') + '.',
                estado: 'publicado',
                servicios: servicios,
                ubicacion: {
                    direccion: azar.de(CALLES) + ' ' + azar.entre(1, 99) + ' # ' + azar.entre(1, 60) + '-' + azar.entre(1, 99),
                    ciudad: ciudad.n,
                    departamento: ciudad.d,
                    pais: 'CO',
                    punto: { type: 'Point', coordinates: [lng, lat] }
                },
                horarios: plan.dias.map(function (d) { return { dia: d, apertura: plan.ap, cierre: plan.ci }; }),
                mecanicos: mecanicos,
                resumen: { suma: 0, total: 0 },
                creadoEn: creado,
                actualizadoEn: creado,
                _plan: plan
            });
        }

        // ---- Citas: del pasado (hasta 90 dias) y del futuro (hasta 21 dias), sin romper la regla de agenda
        var porTaller = {};
        var ocupado = {};
        talleres.forEach(function (t) {
            var activos = t.mecanicos.filter(function (x) { return x.activo; });
            var n = azar.entre(2, 10);
            var hoyLocal = new Date(ahora.getTime() - DESFASE_HORAS * 3600 * 1000);
            for (var k = 0; k < n; k++) {
                var desplazamiento = azar.entre(-90, 21);
                var dl = new Date(Date.UTC(hoyLocal.getUTCFullYear(), hoyLocal.getUTCMonth(), hoyLocal.getUTCDate() + desplazamiento));
                var dia = dl.getUTCDay() === 0 ? 7 : dl.getUTCDay();
                var h = t.horarios.filter(function (x) { return x.dia === dia; })[0];
                if (!h) continue;
                var lista = turnos(h.apertura, h.cierre);
                if (!lista.length) continue;
                var hora = azar.de(lista);
                var fechaHora = aUtc(dl.getUTCFullYear(), dl.getUTCMonth(), dl.getUTCDate(), hora);
                var pasada = fechaHora.getTime() <= ahora.getTime();
                var estado;
                var r = azar.n();
                if (pasada) estado = r < 0.72 ? 'completada' : 'cancelada';
                else estado = r < 0.12 ? 'cancelada' : (r < 0.55 ? 'confirmada' : 'solicitada');
                var ocupa = estado !== 'cancelada';

                var mec = azar.de(activos);
                if (ocupa) {
                    // Busco un mecanico libre a esa hora (misma regla que la agenda de la app)
                    mec = null;
                    var barajados = azar.mezclar(activos);
                    for (var q = 0; q < barajados.length; q++) {
                        if (!ocupado[barajados[q]._id + '|' + fechaHora.getTime()]) { mec = barajados[q]; break; }
                    }
                    if (!mec) continue;
                    ocupado[mec._id + '|' + fechaHora.getTime()] = true;
                }
                var cli = azar.de(clientes);
                var cita = {
                    _id: nuevoId(),
                    clienteId: cli._id,
                    tallerId: t._id,
                    mecanicoId: mec._id,
                    tallerNombre: t.nombre,
                    mecanicoNombre: (mec.nombres + ' ' + mec.apellidos).trim(),
                    fechaHora: fechaHora,
                    estado: estado,
                    ocupaTurno: ocupa,
                    cliente: cli.nombre,
                    motivo: azar.de(MOTIVOS),
                    creadoEn: new Date(Math.min(fechaHora.getTime(), ahora.getTime()) - azar.entre(1, 10) * DIA_MS)
                };
                citas.push(cita);
                (porTaller[t._id] = porTaller[t._id] || []).push(cita);
            }
        });

        // ---- Valoraciones: la mayoria viene de una cita completada (una por cita), algunas son sueltas
        citas.forEach(function (ci) {
            if (ci.estado !== 'completada' || azar.n() > 0.55) return;
            var e = azar.peso([{ peso: 45, v: 5 }, { peso: 30, v: 4 }, { peso: 14, v: 3 }, { peso: 7, v: 2 }, { peso: 4, v: 1 }]).v;
            valoraciones.push({
                _id: nuevoId(), clienteId: ci.clienteId, clienteNombre: ci.cliente,
                tallerId: ci.tallerId, tallerNombre: ci.tallerNombre, citaId: ci._id,
                estrellas: e, servicio: ci.motivo, comentario: azar.de(COMENTARIOS[e]),
                creadoEn: new Date(Math.min(ahora.getTime(), ci.fechaHora.getTime() + azar.entre(1, 3) * DIA_MS))
            });
        });
        var porId = {};
        talleres.forEach(function (t) { porId[t._id] = t; });
        for (var s = 0; s < nTalleres * 0.6; s++) {
            var tt = azar.de(talleres), cl = azar.de(clientes);
            var es = azar.peso([{ peso: 45, v: 5 }, { peso: 30, v: 4 }, { peso: 14, v: 3 }, { peso: 7, v: 2 }, { peso: 4, v: 1 }]).v;
            valoraciones.push({
                _id: nuevoId(), clienteId: cl._id, clienteNombre: cl.nombre,
                tallerId: tt._id, tallerNombre: tt.nombre, citaId: null,
                estrellas: es, servicio: azar.de(MOTIVOS), comentario: azar.de(COMENTARIOS[es]),
                creadoEn: new Date(ahora.getTime() - azar.entre(1, 150) * DIA_MS)
            });
        }
        // El resumen del taller (suma y total) sale de sus valoraciones, igual que lo mantiene la app con $inc
        valoraciones.forEach(function (v) {
            var t = porId[v.tallerId];
            t.resumen.suma += v.estrellas;
            t.resumen.total += 1;
        });

        // ---- Busquedas del mapa: recientes (el indice TTL borra las de mas de 180 dias)
        for (var b = 0; b < nClientes * 4; b++) {
            var cb = azar.de(clientes), ciu = azar.peso(CIUDADES);
            busquedas.push({
                _id: nuevoId(), usuarioId: cb._id,
                termino: azar.de(TERMINOS) + ' ' + ciu.n,
                origenLat: redondear(ciu.lat + (azar.n() - 0.5) * ciu.r, 6),
                origenLng: redondear(ciu.lng + (azar.n() - 0.5) * ciu.r, 6),
                radioM: azar.de([0, 1000, 3000, 5000, 10000]),
                resultados: azar.entre(0, 25),
                origen: 'buscador',
                creadoEn: new Date(ahora.getTime() - azar.entre(0, 150) * DIA_MS - azar.entre(0, 86399) * 1000)
            });
        }

        // ---- Vehiculos de los clientes y talleres con plan destacado. Usan su propio generador para no cambiar
        // los demas datos que ya salian con la semilla.
        var azarExtra = crearAzar((opc.semilla || 20261111) + 7);
        clientes.forEach(function (c) { c.vehiculos = vehiculosAleatorios(azarExtra, nuevoId); });
        talleres.forEach(function (t) {
            var x = azarExtra.n();
            if (x < 0.08) {
                // ~8 % con plan vigente: aparecen como patrocinados bajo el mapa
                t.destacadoHasta = new Date(ahora.getTime() + azarExtra.entre(10, 60) * DIA_MS);
            } else if (x < 0.11) {
                // ~3 % con plan ya vencido: no se muestran
                t.destacadoHasta = new Date(ahora.getTime() - azarExtra.entre(5, 40) * DIA_MS);
            }
        });

        talleres.forEach(function (t) { delete t._plan; });
        return { usuarios: usuarios, talleres: talleres, citas: citas, valoraciones: valoraciones, busquedas: busquedas };
    }

    // Indices: mismos nombres y definiciones que config/MongoIndicesConfig.java, asi la app los reconoce
    function crearIndices(base) {
        base.usuarios.createIndex({ email: 1 }, { name: 'uk_usuarios_email', unique: true });
        base.talleres.createIndex({ 'ubicacion.punto': '2dsphere' }, { name: 'idx_talleres_ubicacion' });
        base.talleres.createIndex({ propietarioId: 1 }, { name: 'idx_talleres_propietario' });
        base.talleres.createIndex({ 'mecanicos._id': 1 }, { name: 'idx_talleres_mecanico' });
        base.citas.createIndex({ mecanicoId: 1, fechaHora: 1 },
            { name: 'uk_citas_mecanico_turno', unique: true, partialFilterExpression: { ocupaTurno: true } });
        base.citas.createIndex({ tallerId: 1, fechaHora: 1 }, { name: 'idx_citas_taller_fecha' });
        base.citas.createIndex({ clienteId: 1, fechaHora: -1 }, { name: 'idx_citas_cliente' });
        base.valoraciones.createIndex({ tallerId: 1, creadoEn: -1 }, { name: 'idx_valoraciones_taller' });
        base.valoraciones.createIndex({ clienteId: 1, creadoEn: -1 }, { name: 'idx_valoraciones_cliente' });
        base.busquedas.createIndex({ usuarioId: 1, creadoEn: -1 }, { name: 'idx_busquedas_usuario' });
        base.busquedas.createIndex({ creadoEn: 1 }, { name: 'ttl_busquedas', expireAfterSeconds: 180 * 24 * 3600 });
    }

    function ejecutar() {
        // mongosh usa process.env; el shell antiguo (MongoDB 4.4) usa _getEnv
        var forzar = (typeof process !== 'undefined' && process.env)
            ? process.env.SEED_FORCE === '1'
            : (typeof _getEnv === 'function' && _getEnv('SEED_FORCE') === '1');
        var yaHay = db.usuarios.estimatedDocumentCount() > 0;
        if (yaHay && !forzar) {
            print('La base "' + db.getName() + '" ya tiene datos: no hago nada. ' +
                'Para borrar y regenerar usa SEED_FORCE=1 (borra los datos actuales).');
            return;
        }
        ['usuarios', 'talleres', 'citas', 'valoraciones', 'busquedas'].forEach(function (n) { db.getCollection(n).drop(); });
        // Los indices van primero: asi el unico de agenda ya vigila los inserts
        crearIndices({ usuarios: db.usuarios, talleres: db.talleres, citas: db.citas,
            valoraciones: db.valoraciones, busquedas: db.busquedas });
        var d = generar({ nuevoId: function () { return new ObjectId(); }, ahora: new Date() });
        function insertar(nombre, docs) {
            for (var i = 0; i < docs.length; i += 1000) db.getCollection(nombre).insertMany(docs.slice(i, i + 1000));
        }
        insertar('usuarios', d.usuarios);
        insertar('talleres', d.talleres);
        insertar('citas', d.citas);
        insertar('valoraciones', d.valoraciones);
        insertar('busquedas', d.busquedas);
        print('Datos demo listos en "' + db.getName() + '": ' + d.usuarios.length + ' usuarios, ' + d.talleres.length +
            ' talleres, ' + d.citas.length + ' citas, ' + d.valoraciones.length + ' valoraciones, ' +
            d.busquedas.length + ' busquedas. Clave de todos los usuarios demo: demo12345');
    }

    var api = { generar: generar, turnos: turnos, diaSemanaLocal: diaSemanaLocal, aUtc: aUtc, HASH_DEMO: HASH_DEMO };
    if (typeof SEED_SOLO_GENERAR !== 'undefined' && typeof module !== 'undefined') {
        // Modo prueba (Node): no toca ninguna base, solo entrega las funciones
        module.exports = api;
    } else {
        ejecutar();
    }
})();
