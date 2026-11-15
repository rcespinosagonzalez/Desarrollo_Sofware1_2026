// Se ejecuta solo la primera vez que arranca el contenedor de MongoDB (volumen vacio).
// Crea el usuario de la aplicacion con permiso unicamente sobre su base (no es administrador).
// Para el equivalente a mano, en mongosh: ver README, seccion "MongoDB".
// mongosh usa process.env; el shell antiguo (MongoDB 4.4) usa _getEnv
function entorno(nombre) {
    return (typeof process !== 'undefined' && process.env) ? process.env[nombre] : _getEnv(nombre);
}
db.createUser({
    user: entorno('MONGO_APP_USER'),
    pwd: entorno('MONGO_APP_PASSWORD'),
    roles: [{ role: 'readWrite', db: db.getName() }]
});
print('Usuario de la aplicacion creado en "' + db.getName() + '"');
