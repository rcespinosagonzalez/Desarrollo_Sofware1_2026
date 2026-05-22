# Vistas del proyecto — Asistencia en carretera

Guía sencilla para estudiar el flujo (4° semestre).

## Cómo arrancar

1. MySQL (como root): `sql/01_crear_usuario_mysql.sql` y luego **`sql/02_asistencia_en_carretera.sql`** (borra y recrea la BD `asistencia_en_carretera` con esquema + 10 talleres + valoraciones).
2. Spring Boot en `Backend/talleres` (puerto 8080).
3. Login: `cliente@demo.local` / `demo` o `admin@demo.local` / `demo`.

## Mapa

- Los marcadores salen de **MySQL** (`GET /api/talleres`), no de Overpass.
- Cada taller tiene `lat` y `lng` en `ubicacion_taller`.
- La búsqueda filtra por nombre, ciudad o dirección y centra el mapa con Nominatim (solo ubicación).

## Cliente — una sola página

`GET /buscar-talleres`

- Mapa + lista lateral
- Historial (solo al pulsar Buscar)
- Mis citas, pedir cita y valorar

## Admin — panel único

`GET /mi-taller` (sin menú lateral)

- Talleres, citas y valoraciones en una sola vista responsive
- `GET /registrar-taller` para alta/edición

## Taller (dueño)

`GET /panel-taller` — citas del taller en sesión (`taller1@demo.local` … `taller10@demo.local`, clave `demo`).
