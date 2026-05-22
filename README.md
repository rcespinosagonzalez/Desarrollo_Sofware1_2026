# Asistencia en carretera

Proyecto de aula (4° semestre): buscar talleres mecánicos en mapa, pedir citas y valoraciones.

## Base de datos (MySQL)

1. `sql/01_crear_usuario_mysql.sql` — usuario `taller_app`
2. `sql/02_asistencia_en_carretera.sql` — base `asistencia_en_carretera` (esquema + datos demo)

## Backend

```bash
cd Backend/talleres
mvn spring-boot:run
```

http://localhost:8080 — login en `/`

## Credenciales demo

- Cliente: `cliente@demo.local` / `demo`
- Admin: `admin@demo.local` / `demo`
- Talleres: `taller1@demo.local` … `taller10@demo.local` / `demo`
