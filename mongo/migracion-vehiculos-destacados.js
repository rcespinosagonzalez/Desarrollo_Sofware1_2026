// Migracion de datos demo (base ya cargada): agrega vehiculos a los clientes que no tienen y deja
// algunos talleres con plan "destacado". No borra ni cambia nada mas y se puede correr varias veces:
// solo toca clientes sin vehiculos y solo marca talleres si ninguno esta destacado todavia.
//
// Uso con Docker (PowerShell, desde la carpeta del proyecto):
//   Get-Content mongo\migracion-vehiculos-destacados.js | docker compose exec -T mongo sh -c 'mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin "$MONGO_INITDB_DATABASE" || mongo --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin "$MONGO_INITDB_DATABASE"'
// (mongosh en MongoDB 5+; el shell "mongo" en 4.4)

(function () {
    'use strict';

    var DIA_MS = 24 * 3600 * 1000;

    function crearAzar() {
        return {
            n: Math.random,
            entre: function (min, max) { return min + Math.floor(Math.random() * (max - min + 1)); },
            de: function (lista) { return lista[Math.floor(Math.random() * lista.length)]; },
            peso: function (lista) {
                var total = 0, i;
                for (i = 0; i < lista.length; i++) total += lista[i].peso;
                var x = Math.random() * total;
                for (i = 0; i < lista.length; i++) {
                    x -= lista[i].peso;
                    if (x < 0) return lista[i];
                }
                return lista[lista.length - 1];
            }
        };
    }

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


    var azar = crearAzar();
    var nuevoId = function () { return new ObjectId(); };

    // ---- Vehiculos para los clientes que todavia no tienen
    var clientes = db.usuarios.find({ rol: 'user', $or: [{ vehiculos: { $exists: false } }, { vehiculos: { $size: 0 } }] },
        { _id: 1 }).toArray();
    clientes.forEach(function (c) {
        db.usuarios.updateOne({ _id: c._id }, { $set: { vehiculos: vehiculosAleatorios(azar, nuevoId) } });
    });
    print('Clientes con vehiculos nuevos: ' + clientes.length);

    // ---- Talleres destacados (solo si todavia no hay ninguno)
    if (db.talleres.countDocuments({ destacadoHasta: { $exists: true } }) === 0) {
        var ids = db.talleres.find({}, { _id: 1 }).toArray();
        var vigentes = 0, vencidos = 0, ahora = new Date();
        ids.forEach(function (t) {
            var x = azar.n();
            if (x < 0.08) {
                db.talleres.updateOne({ _id: t._id }, { $set: { destacadoHasta: new Date(ahora.getTime() + azar.entre(10, 60) * DIA_MS) } });
                vigentes++;
            } else if (x < 0.11) {
                db.talleres.updateOne({ _id: t._id }, { $set: { destacadoHasta: new Date(ahora.getTime() - azar.entre(5, 40) * DIA_MS) } });
                vencidos++;
            }
        });
        print('Talleres destacados: ' + vigentes + ' vigentes y ' + vencidos + ' vencidos');
    } else {
        print('Ya hay talleres destacados: no se cambio ninguno');
    }
})();
