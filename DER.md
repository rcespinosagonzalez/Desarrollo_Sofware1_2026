## Diagrama Entidad–Relación (DER)

### Alcance real del repositorio (lo que existe hoy)

En el código actual **no existe capa de persistencia**:

- No hay entidades JPA (`@Entity` / `@Table`) ni repositorios Spring Data reales.
- No hay migraciones (`Flyway`/`Liquibase`) ni `schema.sql`.
- El backend devuelve una lista “mock” de `Taller`.
- El frontend modela funcionalmente: **login por rol (admin/user)**, **taller**, **búsqueda por ubicación** (Nominatim/Overpass, o talleres estáticos), **citas**, **valoraciones**, **historial de búsquedas**.

Este DER es el **modelo de datos propuesto** para soportar todas esas vistas y escalar a producción.

### DER (Mermaid)

```mermaid
erDiagram
  USUARIO {
    uuid id PK
    varchar email UK
    varchar password_hash
    varchar nombre
    varchar rol  "admin|user"
    boolean activo
    timestamptz created_at
    timestamptz updated_at
  }

  TALLER {
    uuid id PK
    uuid propietario_usuario_id FK
    varchar nombre
    varchar telefono
    text descripcion
    varchar estado "borrador|publicado|suspendido"
    timestamptz created_at
    timestamptz updated_at
  }

  UBICACION_TALLER {
    uuid id PK
    uuid taller_id FK
    varchar direccion_texto
    varchar ciudad
    varchar departamento
    varchar pais
    decimal lat
    decimal lng
    timestamptz geocodificado_en
  }

  MECANICO {
    uuid id PK
    uuid taller_id FK
    varchar nombres
    varchar apellidos
    varchar telefono
    boolean activo
    timestamptz created_at
  }

  SERVICIO {
    uuid id PK
    varchar nombre UK
    text descripcion
  }

  TALLER_SERVICIO {
    uuid taller_id FK
    uuid servicio_id FK
    numeric precio_desde
    int duracion_min_estimada
    boolean activo
    timestamptz updated_at
  }

  CITA {
    uuid id PK
    uuid usuario_id FK
    uuid taller_id FK
    uuid servicio_id FK
    timestamptz fecha_hora
    varchar estado "solicitada|confirmada|cancelada|finalizada|no_asistio"
    text notas_usuario
    text notas_taller
    timestamptz created_at
    timestamptz updated_at
  }

  VALORACION {
    uuid id PK
    uuid usuario_id FK
    uuid taller_id FK
    uuid cita_id FK "nullable (si califica sin cita)"
    smallint estrellas "1..5"
    text comentario
    timestamptz created_at
  }

  HISTORIAL_BUSQUEDA {
    uuid id PK
    uuid usuario_id FK "nullable (si anónimo)"
    varchar termino
    decimal origen_lat
    decimal origen_lng
    int radio_m
    int resultados
    timestamptz created_at
  }

  RESULTADO_BUSQUEDA {
    uuid id PK
    uuid historial_busqueda_id FK
    varchar fuente "overpass|manual"
    varchar fuente_id "osm element id o interno"
    varchar nombre
    varchar direccion
    varchar tipo
    decimal lat
    decimal lng
    numeric distancia_km
  }

  %% Relaciones principales
  USUARIO ||--o{ CITA : "solicita"
  USUARIO ||--o{ VALORACION : "califica"
  USUARIO ||--o{ HISTORIAL_BUSQUEDA : "realiza"

  USUARIO ||--o{ TALLER : "posee (admin)"

  TALLER ||--|| UBICACION_TALLER : "tiene"
  TALLER ||--o{ MECANICO : "emplea"
  TALLER ||--o{ CITA : "recibe"
  TALLER ||--o{ VALORACION : "recibe"

  SERVICIO ||--o{ CITA : "motiva"
  TALLER ||--o{ TALLER_SERVICIO : "ofrece"
  SERVICIO ||--o{ TALLER_SERVICIO : "se_ofrece_en"

  CITA ||--o| VALORACION : "genera (opcional)"

  HISTORIAL_BUSQUEDA ||--o{ RESULTADO_BUSQUEDA : "incluye"
```

### Notas de escalabilidad (resumen)

- **IDs**: `uuid` para evitar colisiones y facilitar particionado/replicación.
- **Geoespacial**: en producción, ideal `PostgreSQL + PostGIS` (index GIST/BRIN) para búsquedas cercanas.
- **Separar “catálogo” vs “externo”**: `RESULTADO_BUSQUEDA` permite guardar resultados de Overpass sin convertirlos en “talleres propios”.
- **Normalización**: `MECANICO` y `SERVICIO` evitan meter listas en un string (como el campo `Mecanicos` actual).

