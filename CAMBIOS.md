# Registro de cambios (se aplicará en un solo commit)

Pendiente de la evaluación inicial; cada cambio se agrega aquí al hacerse.

## 1. Registro de clientes (arregla el botón «Registrarme»)
- `templates/index.html`: el enlace apuntaba a `/registro.html` (no existía) → ahora `/registro`.
- Nuevo `templates/registro.html` y `static/js/registro.js` (reutiliza `styles.css` del login).
- `AuthController`: nuevo `POST /api/auth/registro` (rol siempre `user`, BCrypt, validación de nombre/correo/clave ≥ 8, correo duplicado, inicia sesión al terminar).
- `VistaController`: nuevo `GET /registro`.
- `SecurityConfig`: `/registro` y `/api/auth/registro` son públicos.

## 2. Seguridad inmediata
- `TallerServiceJdbcImpl.guardar`: la clave del dueño de un taller nuevo se guardaba en texto plano; ahora BCrypt (y mínimo 8 caracteres). Constructor recibe `PasswordEncoder`.
- `application.properties` y `docker-compose.yml`: `DB_PASSWORD` y `JWT_SECRET` ya no tienen valor por defecto; sin ellas la app no arranca.
- `sql/01_crear_usuario_mysql.sql`: clave `taller123` reemplazada por `CAMBIA_ESTA_CLAVE`.
- `AuthController`: login y registro responden 400/401/409 en vez de 200 con `ok:false`; `script.js` y `registro.js` leen el mensaje en esos casos.
- `README.md`: documenta las variables de entorno obligatorias.

## 3. Limpieza de código muerto
- Eliminados: `Backend/back.js` (vacío, sin referencias), `PasswordMigrationRunner` (el SQL ya trae BCrypt; las claves nuevas se hashean al crearlas), `static/css/img/logo.jpg` y `templates/css/mapa.png` (1,5 MB, sin uso).
- `TallerServiceJdbcImpl`: quitados los parches para BD sin columna `origen` (`BadSqlGrammarException`, doble INSERT, lectura con try/catch). El `catch (DataAccessException)` del INSERT escondía cualquier error de BD.
- `SEGURIDAD_JWT.md`: quitada la fila del runner.
- `styles.css` (login/registro) y `estilos.css` (resto de la app) NO se fusionan: comparten una sola clase y no están duplicados.

## 4. Documentación alineada con el código (parcial)
- `DISPOSITIVA_PROYECTO.md`, `DOCUMENTACION_VISTAS.md`, `MATERIAL_ESTUDIO_SUSTENTACION.md`: ya no dicen que no hay Spring Security/Docker ni que la sesión es `HttpSession`/interceptor; describen JWT + BCrypt, el registro, `DB_PASSWORD`/`JWT_SECRET` y quitan `taller123`.
- `DER.md` y `MODELO_RELACIONAL.md`: aviso al inicio de que son una propuesta histórica (PostgreSQL/UUID), no el esquema real. Los PDF `DER.pdf` y `MODELO_RELACIONAL_COMPLETO.pdf` no se tocaron: siguen mostrando esa propuesta.

## 5. Seguridad adicional
- Nuevo `security/LoginAttemptService`: tras 5 logins fallidos (IP + correo) el login responde 429 con `Retry-After` durante 15 min; un login correcto lo reinicia. Probada la lógica por separado.
- `AuthController.login`: usa el limitador.
- `SecurityConfig`: cabeceras `X-Frame-Options: DENY`, `X-Content-Type-Options`, `Referrer-Policy` y `Content-Security-Policy` (scripts solo propios + unpkg, sin `unsafe-inline`).
- El script inline de `buscar-talleres.html` pasó a `static/js/buscar-talleres-ver.js` para poder aplicar esa CSP.
- `SEGURIDAD_JWT.md` actualizado.

## 6. Errores explícitos en vez de valores por defecto 
- `agregarCita`: antes usaba fecha `1970-01-01`, hora `00:00` o "el último taller" si faltaba algo, y volvía sin avisar. Ahora exige taller existente, motivo y fecha/hora válidas y futuras; si no, muestra un mensaje.
- `registrarCalificacion`: antes, si el nombre del taller no existía, la valoración se guardaba en **el último taller creado**; ahora avisa "No encontramos un taller con ese nombre". Las estrellas fuera de 1–5 ya no se recortan, se rechazan.
- `detalle-taller` sin id válido ya no muestra "el último taller": vuelve al mapa con un aviso. Eliminado `obtenerPrincipal()`.
- Nuevo `ManejadorErrores` (`@RestControllerAdvice`) para `/api`: 400 con el mensaje o 500 genérico (el detalle va al log). Nueva `templates/error.html` para las páginas.
- `VistaController`: `solicitar-cita` y `calificar-taller` muestran los mensajes de error.
- `completarCoordenadas`: si Nominatim no encuentra la dirección ya no se guarda el taller en Cartagena; se rechaza con un aviso (decisión tuya). También valida el rango de lat/lng.

## 7. Agenda de citas: horario de atención, turnos y mecánico asignado
Decisiones tuyas: horario estructurado, turnos fijos de 1 hora, mecánico asignado automáticamente.
- **Horario de atención:** nueva tabla `horario_taller` (taller, día 1–7, apertura, cierre). Se quita `taller.horario` (texto libre). El formulario del taller ahora pide días (casillas) y apertura/cierre; el texto "Lun - Sab 8:00 a 18:00" se genera (`util/HorarioAtencion`, probado por separado).
- **Citas:** solo en un turno de 1 h dentro del horario; fecha/hora pasadas se rechazan usando la zona `app.zona-horaria` (America/Bogota).
- **Disponibilidad:** `cita.mecanico_id`; un turno admite tantas citas como mecánicos activos. Nuevo `AgendaService` asigna un mecánico libre y rechaza si no queda ninguno. La concurrencia se resuelve con una transacción que bloquea (`FOR UPDATE`) las filas de los mecánicos del taller. Nueva `config/TransaccionesConfig` para que `@Transactional` funcione siempre.
- **API/pantalla:** `GET /api/talleres/{id}/disponibilidad?fecha=` y `cita-disponibilidad.js`: la hora del formulario es una lista de horas libres.
- **Mecánicos:** `MecanicoService`; el dueño los agrega y da de baja en su panel (no se puede dar de baja al último ni a uno con citas pendientes). Cada taller nuevo nace con un mecánico "Principal".
- **Taller:** `guardar` ahora es transaccional (taller + ubicación + horario + mecánico se guardan juntos o ninguno) y el horario es obligatorio.
- **Lista de citas** (cliente, dueño, admin): muestran el estado y el mecánico asignado; el panel del dueño tiene columna "Mecánico".
- **SQL:** `02` actualizado (esquema y datos demo: horarios, 11 mecánicos). Nuevo `03_agenda_horarios_y_mecanicos.sql` para bases ya instaladas.
- **Docker:** el contenedor de la app recibe `TZ` y `APP_TZ`.
- Se eliminan `agregarCita`, `leerFechaHora` y `siguienteIdEn("cita")`: la cita usa AUTO_INCREMENT.

### Para la migración a MongoDB (anotado)
- `horario_taller` → arreglo embebido `horarios: [{dia, apertura, cierre}]` dentro de `talleres`; `mecanicos` embebidos o colección propia con `tallerId`.
- `citas.mecanicoId` + índice único parcial `(mecanicoId, fechaHora)` solo para citas no canceladas (`partialFilterExpression` con `estado` en las activas), o transacción equivalente al bloqueo actual.
- `fechaHora` como `Date` en UTC y la zona de Bogotá aplicada al validar.

## 8. Migración de MySQL a MongoDB
Decisiones tuyas: regenerar ~1.000 talleres coherentes y correr MongoDB con Docker Compose en tu máquina.
- **Persistencia:** `spring-boot-starter-data-mongodb` reemplaza JDBC y el driver de MySQL; la conexión es `MONGODB_URI` (`spring.mongodb.uri`). Se borran `TallerServiceJdbcImpl`, `AgendaServiceJdbcImpl`, `MecanicoServiceJdbcImpl`, `HorarioTallerDao`, `TransaccionesConfig` y los modelos sin uso `Usuario` y `UbicacionTaller`.
- **Modelo:** 5 colecciones (`usuarios`, `talleres`, `citas`, `valoraciones`, `busquedas`). Dentro de `talleres` van embebidos la ubicación (punto GeoJSON `[lng, lat]`), los horarios, los mecánicos y el resumen de valoraciones (`suma`, `total`; el promedio ya no se recalcula con AVG). Documentos en `documents/`, conversión a los modelos de vista en `services/impl/Mapeos`.
- **Ids:** pasan de `Long` a `ObjectId` en texto (24 hex) en servicios, controladores, JWT (`UsuarioAutenticado.id`, `LoginResponse.usuarioId`) y `SesionActual`. Un id con forma inválida solo da "no existe" (`util/Ids`). El front ya trataba los ids como texto, no cambió.
- **Índices** (`config/MongoIndicesConfig`, al arrancar): correo único; `2dsphere` en la ubicación (listo para "talleres cerca de mí", aún sin endpoint); único parcial `(mecanicoId, fechaHora)` con `ocupaTurno: true`; TTL de 180 días en `busquedas`; los de consulta por taller, cliente y dueño.
- **Agenda sin bloqueos:** ya no hay `FOR UPDATE` ni transacciones. Si dos clientes piden el mismo mecánico a la vez, al segundo el índice único le rechaza el `insert` y la app reintenta con otro mecánico libre. Una cita cancelada pone `ocupaTurno` en `false` y libera el turno. Dar de baja a un mecánico es una sola actualización atómica que exige que quede otro activo.
- **Valoraciones:** insertan la valoración y suman al resumen del taller con `$inc`.
- **Registro:** el correo repetido lo rechaza el índice único (409), también con dos registros simultáneos.
- **Fechas:** `Instant` en UTC; la zona `app.zona-horaria` (America/Bogota) solo se usa para el horario y para mostrar.
- **Datos demo:** `mongo/seed.js` genera 1 admin, 300 clientes, 1.000 talleres con su dueño, 1–5 mecánicos y horario (11 ciudades), ~5.000 citas sin choques ni fuera de horario, ~2.200 valoraciones con el resumen al día y ~1.200 búsquedas. Clave de todos: `demo12345` (BCrypt). Si la base ya tiene datos no hace nada, salvo `SEED_FORCE=1`. `mongo/seed.test.js` (15 pruebas, `node mongo/seed.test.js`) comprueba esas reglas sin necesitar MongoDB.
- **Docker:** `docker-compose.yml` ahora levanta `mongo:8.0` (usuario root + usuario de la app con `readWrite` solo en su base, puerto solo en 127.0.0.1) y la app con `MONGODB_URI`; `.env.example` actualizado. La carpeta `sql/` queda como historia (`sql/LEEME.md`).
- **Documentación:** README, DOCUMENTACION_VISTAS, SEGURIDAD_JWT, DISPOSITIVA_PROYECTO y MATERIAL_ESTUDIO_SUSTENTACION (secciones de base de datos y preguntas reescritas para MongoDB); DER y modelo relacional siguen marcados como históricos.
- **Tu exportación original (`datos_mongo/`)** no se usa ni va en el parche: tenía los ids numéricos del modelo MySQL y las claves en texto plano (`"demo"`), incompatible con este modelo y con el login con BCrypt.

### Pendientes conocidos
- No se pudo compilar ni ejecutar aquí (Maven Central bloqueado): hay que probarlo en tu máquina (ver lista abajo en la respuesta).
- `/api/talleres` expone el correo del dueño (`emailPropietario`); conviene quitarlo del JSON.
- No hay pantalla para confirmar o cancelar citas, y un mismo cliente puede reservar dos veces el mismo turno.
- La valoración aún no exige una cita completada.
- Falta el endpoint de talleres cercanos (el índice `2dsphere` ya está).
