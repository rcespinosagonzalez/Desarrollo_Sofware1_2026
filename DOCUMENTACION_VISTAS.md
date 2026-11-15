# Vistas del proyecto — Asistencia en carretera

Guía sencilla para estudiar el flujo (4° semestre).

## Cómo arrancar

1. MongoDB con Docker: copia `.env.example` a `.env`, completa los valores y ejecuta `docker compose up -d mongo`. La primera vez crea el usuario de la app y carga los datos demo (`mongo/seed.js`: 1.000 talleres). Ver README.
2. Spring Boot en `Backend/talleres` (puerto 8080) con `MONGODB_URI` y `JWT_SECRET` definidas (ver README).
3. Login: `cliente1@demo.local`, `admin@demo.local` o `taller1@demo.local`, todos con clave `demo12345`.

## Registro

`GET /registro` (público): un cliente nuevo crea su cuenta y entra directo al mapa.

## Mapa

- Los marcadores salen de **MongoDB** (`GET /api/talleres`), no de Overpass.
- Cada taller tiene su punto (`ubicacion.punto`, lat/lng) dentro del documento del taller.
- La búsqueda filtra por nombre, ciudad o dirección y centra el mapa con Nominatim (solo ubicación).

## Cliente — una sola página

`GET /buscar-talleres`

- Mapa + lista lateral
- Historial (solo al pulsar Buscar)
- Mis citas, pedir cita y valorar
- **Pedir cita con agenda:** al elegir taller y fecha, la hora es una lista con los turnos libres (1 hora cada uno) dentro del horario de atención; el sistema asigna un mecánico libre y no deja dos citas en la misma hora si ya no queda ningún mecánico

## Admin — panel único

`GET /mi-taller` (sin menú lateral)

- Talleres, citas y valoraciones en una sola vista responsive
- `GET /registrar-taller` para alta/edición

## Taller (dueño)

`GET /panel-taller` — mecánicos (agregar / dar de baja), citas con su mecánico asignado y valoraciones del taller en sesión (`taller1@demo.local` … `taller1000@demo.local`, clave `demo12345`).

## Agenda de citas (reglas)

- **Horario:** el taller define días de atención y hora de apertura/cierre en "Datos de mi taller" (arreglo `horarios` del taller; el texto "Lun - Sab 8:00 a 18:00" se genera solo).
- **Turnos:** cada cita dura 1 hora y empieza en la hora de apertura o una hora entera después (la última, una hora antes del cierre).
- **Disponibilidad:** un turno admite tantas citas como mecánicos activos tenga el taller. Cada cita queda asignada a un mecánico libre; un mismo mecánico nunca tiene dos citas a la misma hora.
- **Concurrencia:** no hay bloqueos: el índice único parcial `uk_citas_mecanico_turno` (mecánico + hora, solo citas no canceladas) impide que dos clientes tomen al mismo mecánico a la misma hora; a quien llega segundo se le asigna otro mecánico libre (`AgendaServiceMongoImpl`) o se le avisa que la hora ya no está disponible.
- **Cancelar:** una cita `cancelada` libera su turno.
- **API:** `GET /api/talleres/{id}/disponibilidad?fecha=AAAA-MM-DD` devuelve las horas libres de ese día.
