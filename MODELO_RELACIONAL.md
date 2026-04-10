## Modelo relacional (propuesto) — listo para escalar

### Observación importante (para “no dejar nada por fuera”)

El repositorio **aún no define** un esquema físico de BD. Este documento traduce a modelo relacional todo lo que las vistas del frontend y el backend “mock” requieren: **usuarios/roles**, **taller**, **ubicación (lat/lng)**, **citas**, **valoraciones**, **historial de búsquedas** y **resultados de búsqueda** (manual/Overpass).

El modelo está pensado para **PostgreSQL** (recomendado para escalabilidad y geodatos), pero es portable.

---

## 1) Tablas y claves (PK/FK)

### `usuario`

- **PK**: `id (uuid)`
- **UK**: `email`
- **Campos**:
  - `id uuid not null`
  - `email varchar(320) not null`
  - `password_hash varchar(255) not null`
  - `nombre varchar(120) not null`
  - `rol varchar(20) not null`  *(admin|user)*
  - `activo boolean not null default true`
  - `created_at timestamptz not null default now()`
  - `updated_at timestamptz not null default now()`

### `taller`

- **PK**: `id (uuid)`
- **FK**: `propietario_usuario_id -> usuario(id)` *(admin dueño del taller)*
- **Campos**:
  - `id uuid not null`
  - `propietario_usuario_id uuid not null`
  - `nombre varchar(160) not null`
  - `telefono varchar(40) null` *(no int: soporta +57, espacios, etc.)*
  - `descripcion text null`
  - `estado varchar(20) not null default 'publicado'`
  - `created_at timestamptz not null default now()`
  - `updated_at timestamptz not null default now()`

### `ubicacion_taller`

- **PK**: `id (uuid)`
- **FK**: `taller_id -> taller(id)` *(1:1 con taller, o 1:N si quieres múltiples sedes)*
- **Campos**:
  - `id uuid not null`
  - `taller_id uuid not null`
  - `direccion_texto varchar(300) not null`
  - `ciudad varchar(120) null`
  - `departamento varchar(120) null`
  - `pais varchar(80) null default 'CO'`
  - `lat decimal(9,6) null`
  - `lng decimal(9,6) null`
  - `geocodificado_en timestamptz null`

### `mecanico`

- **PK**: `id (uuid)`
- **FK**: `taller_id -> taller(id)`
- **Campos**:
  - `id uuid not null`
  - `taller_id uuid not null`
  - `nombres varchar(120) not null`
  - `apellidos varchar(120) null`
  - `telefono varchar(40) null`
  - `activo boolean not null default true`
  - `created_at timestamptz not null default now()`

### `servicio`

- **PK**: `id (uuid)`
- **UK**: `nombre`
- **Campos**:
  - `id uuid not null`
  - `nombre varchar(140) not null`
  - `descripcion text null`

### `taller_servicio` (N:M)

- **PK compuesta**: `(taller_id, servicio_id)`
- **FK**:
  - `taller_id -> taller(id)`
  - `servicio_id -> servicio(id)`
- **Campos**:
  - `taller_id uuid not null`
  - `servicio_id uuid not null`
  - `precio_desde numeric(12,2) null`
  - `duracion_min_estimada int null`
  - `activo boolean not null default true`
  - `updated_at timestamptz not null default now()`

### `cita`

- **PK**: `id (uuid)`
- **FK**:
  - `usuario_id -> usuario(id)`
  - `taller_id -> taller(id)`
  - `servicio_id -> servicio(id)` *(nullable si la cita es “general”)*
- **Campos**:
  - `id uuid not null`
  - `usuario_id uuid not null`
  - `taller_id uuid not null`
  - `servicio_id uuid null`
  - `fecha_hora timestamptz not null`
  - `estado varchar(20) not null default 'solicitada'`
  - `notas_usuario text null`
  - `notas_taller text null`
  - `created_at timestamptz not null default now()`
  - `updated_at timestamptz not null default now()`

### `valoracion`

- **PK**: `id (uuid)`
- **FK**:
  - `usuario_id -> usuario(id)`
  - `taller_id -> taller(id)`
  - `cita_id -> cita(id)` *(nullable)*
- **Campos**:
  - `id uuid not null`
  - `usuario_id uuid not null`
  - `taller_id uuid not null`
  - `cita_id uuid null`
  - `estrellas smallint not null` *(check 1..5)*
  - `comentario text null`
  - `created_at timestamptz not null default now()`

### `historial_busqueda`

- **PK**: `id (uuid)`
- **FK**: `usuario_id -> usuario(id)` *(nullable para permitir anónimo)*
- **Campos**:
  - `id uuid not null`
  - `usuario_id uuid null`
  - `termino varchar(200) not null`
  - `origen_lat decimal(9,6) null`
  - `origen_lng decimal(9,6) null`
  - `radio_m int not null default 12000`
  - `resultados int not null default 0`
  - `created_at timestamptz not null default now()`

### `resultado_busqueda`

- **PK**: `id (uuid)`
- **FK**: `historial_busqueda_id -> historial_busqueda(id)`
- **Campos**:
  - `id uuid not null`
  - `historial_busqueda_id uuid not null`
  - `fuente varchar(20) not null` *(overpass|manual)*
  - `fuente_id varchar(80) null` *(ej: OSM element id)*
  - `nombre varchar(200) not null`
  - `direccion varchar(300) null`
  - `tipo varchar(80) null`
  - `lat decimal(9,6) not null`
  - `lng decimal(9,6) not null`
  - `distancia_km numeric(10,3) null`

---

## 2) Cardinalidades (resumen)

- `usuario (admin)` 1 —— N `taller`
- `taller` 1 —— 1 `ubicacion_taller` *(o 1 —— N si hay múltiples sedes)*
- `taller` 1 —— N `mecanico`
- `taller` N —— M `servicio` (por `taller_servicio`)
- `usuario` 1 —— N `cita`
- `taller` 1 —— N `cita`
- `usuario` 1 —— N `valoracion`
- `taller` 1 —— N `valoracion`
- `historial_busqueda` 1 —— N `resultado_busqueda`

---

## 3) Índices recomendados (para rendimiento)

- **Únicos**
  - `usuario(email)`
  - `servicio(nombre)`
  - *(si 1:1)* `ubicacion_taller(taller_id)` unique

- **Búsqueda / listados**
  - `taller(propietario_usuario_id)`
  - `cita(taller_id, fecha_hora desc)`
  - `cita(usuario_id, fecha_hora desc)`
  - `valoracion(taller_id, created_at desc)`
  - `historial_busqueda(usuario_id, created_at desc)`
  - `resultado_busqueda(historial_busqueda_id)`

- **Geoespacial (si PostGIS)**
  - Reemplazar `lat/lng` por `geography(Point, 4326)` y crear `GIST` index:
    - `ubicacion_taller(geo)`
    - `resultado_busqueda(geo)`

---

## 4) Reglas / constraints recomendadas

- `valoracion.estrellas` con `CHECK (estrellas between 1 and 5)`.
- Evitar doble calificación:
  - `UNIQUE (usuario_id, taller_id, cita_id)` cuando `cita_id` no sea null; o
  - `UNIQUE (usuario_id, taller_id)` si solo se permite una valoración total por taller.
- Control de estados (`ENUM` o `CHECK`) para `taller.estado` y `cita.estado`.

---

## 5) Mapeo directo desde el código actual (para que se vea “completo”)

- El modelo Java `Taller` hoy contiene: `Nombre`, `Direccion`, `Telefono`, `Mecanicos` (string).
  - En BD: `taller.nombre`, `ubicacion_taller.direccion_texto`, `taller.telefono`, `mecanico` (normalizado).
- El buscador usa: `nombre`, `direccion`, `tipo`, `lat`, `lng`.
  - En BD: `resultado_busqueda` (cacheable) y/o `ubicacion_taller`.

