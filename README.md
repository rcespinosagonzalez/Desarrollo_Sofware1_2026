# Asistencia en carretera

Proyecto de aula (4° semestre): buscar talleres mecánicos en mapa, pedir citas y valoraciones.
Spring Boot 4 + Thymeleaf + **MongoDB**.

## Arranque rápido con Docker

```bash
cp .env.example .env        # completa las claves (y JWT_SECRET: openssl rand -base64 48)
docker compose up -d --build
```

http://localhost:8080 — la primera vez, MongoDB crea el usuario de la app y carga los datos demo (1.000 talleres).

## MongoDB

- Base: `asistencia_en_carretera`; colecciones `usuarios`, `talleres`, `citas`, `valoraciones`, `busquedas`.
- Índices: los crea la app al arrancar (`config/MongoIndicesConfig`) y también `mongo/seed.js`, con los mismos nombres. El importante es `uk_citas_mecanico_turno` (único parcial): impide dos citas del mismo mecánico a la misma hora.
- Fechas en UTC; la zona `America/Bogota` (`APP_TZ`) se usa para el horario de atención.
- Los scripts de `mongo/init` y `mongo/seed.js` solo corren **la primera vez** (volumen vacío). Para volver a empezar: `docker compose down -v`.
- Regenerar los datos demo en una base existente (**borra los datos actuales**):
  `SEED_FORCE=1 mongosh "<MONGODB_URI>" mongo/seed.js`
- Pruebas de coherencia del generador (sin MongoDB): `node mongo/seed.test.js`
- Esquema anterior en MySQL: carpeta `sql/` (solo historia).

### Si MongoDB no arranca ("Illegal instruction")

MongoDB 5 en adelante necesita un procesador con AVX. Si el contenedor se cae con ese mensaje, pon `MONGO_IMAGE=mongo:4.4` en `.env`, borra el volumen (`docker compose down -v`) y vuelve a levantar. El proyecto funciona igual con 4.4.

## Variables de entorno obligatorias

La app **no arranca** sin ellas (no hay valores por defecto):

| Variable | Descripción |
|---|---|
| `MONGODB_URI` | Cadena de conexión con usuario, clave y base, por ejemplo `mongodb://taller_app:clave@localhost:27017/asistencia_en_carretera?authSource=asistencia_en_carretera` (con Docker la arma `docker-compose.yml`) |
| `JWT_SECRET` | Clave para firmar los JWT, 32+ caracteres (`openssl rand -base64 48`) |

Opcionales: `APP_TZ`, `JWT_EXPIRATION_MINUTES`, `JWT_COOKIE_SECURE`.

## Backend sin Docker para la app (solo MongoDB en Docker)

```bash
docker compose up -d mongo
cd Backend/talleres
export MONGODB_URI="mongodb://taller_app:TU_CLAVE@localhost:27017/asistencia_en_carretera?authSource=asistencia_en_carretera"
export JWT_SECRET=$(openssl rand -base64 48)
mvn spring-boot:run
```

http://localhost:8080 — login en `/`

## Credenciales demo

Todos con clave `demo12345`:

- Cliente: `cliente1@demo.local` … `cliente300@demo.local`
- Admin: `admin@demo.local`
- Talleres: `taller1@demo.local` … `taller1000@demo.local`
