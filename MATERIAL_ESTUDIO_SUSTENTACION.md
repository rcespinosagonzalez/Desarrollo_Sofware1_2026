# Material de estudio — Asistencia en carretera

Documento para **sustentar el proyecto** (4° semestre): rutas, vistas, MongoDB, inyección de dependencias, seguridad básica y preguntas frecuentes del profesor.

**Contenido ampliado (estudiar antes de la sustentación):**
- Secciones **1–15:** rutas, vistas, flujos, preguntas base
- Sección **16:** stack tecnológico (Maven, Spring, MongoDB, Leaflet, etc.)
- Sección **17:** base de datos MongoDB (colecciones, documentos embebidos, índices, mapeo a Java)
- Sección **18:** **60 preguntas** con temas A–H
- Sección **19:** glosario

---

## 1. Resumen del proyecto

| Aspecto | Detalle |
|--------|---------|
| **Nombre** | Asistencia en carretera |
| **Objetivo** | Buscar talleres en mapa, pedir citas y dejar valoraciones |
| **Stack** | Spring Boot 4 + Thymeleaf + MongoDB + JavaScript (Leaflet + OpenStreetMap) |
| **BD** | MongoDB, base `asistencia_en_carretera` (datos demo: `mongo/seed.js`) |
| **Puerto** | `http://localhost:8080` |

### Roles de usuario (campo `rol` de la colección `usuarios`)

| Rol en BD | Rol en el token (front) | Pantalla principal |
|-----------|----------------------|-------------------|
| `admin` | `admin` | `/mi-taller` — administración |
| `user` | `user` | `/buscar-talleres` — cliente |
| `taller` | `taller` | `/panel-taller` — dueño del taller |

Contraseñas: se guardan con **BCrypt** (los usuarios demo ya vienen con hash; la clave de todos es `demo12345`).

---

## 2. Arquitectura en capas (cómo explicarlo)

```
Navegador (HTML + CSS + JS)
        │
        ▼
Controllers  ──►  reciben HTTP, devuelven vista o JSON
        │
        ▼
TallerService (interfaz)  ◄── inyección de dependencias
        │
        ▼
TallerServiceMongoImpl  ──►  MongoTemplate  ──►  MongoDB
```

**Frase para sustentación:**  
*"Separamos la interfaz web (controladores) de la lógica de negocio (servicio) y de los datos (MongoDB). Spring conecta las piezas sin que yo haga `new TallerServiceMongoImpl()` a mano en cada clase."*

---

## 3. Inyección de dependencias (DI)

### ¿Qué es?

Spring **crea y entrega** los objetos que una clase necesita por el **constructor**, en lugar de instanciarlos dentro de la clase.

### Ejemplos en el proyecto

**1) `VistaController` recibe el servicio:**

```java
@Controller
public class VistaController {
    private final TallerService tallerService;

    public VistaController(TallerService tallerService) {
        this.tallerService = tallerService;
    }
}
```

**2) `TallerServiceMongoImpl` recibe `MongoTemplate`:**

```java
@Service
public class TallerServiceMongoImpl implements TallerService {
    private final MongoTemplate mongo;
    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder passwordEncoder;
    // Spring entrega las tres piezas por el constructor
}
```

**3) `AuthController` recibe el repositorio de usuarios, el codificador BCrypt y el servicio de tokens:**

```java
public AuthController(UsuarioRepositorio usuarios, PasswordEncoder passwordEncoder,
                      JwtService jwtService, LoginAttemptService intentos) { ... }
```

### Preguntas típicas y respuestas

| Pregunta | Respuesta corta |
|----------|-----------------|
| ¿Por qué una interfaz `TallerService`? | Para desacoplar el controlador de la implementación; hoy es MongoDB (antes fue MySQL con JDBC: el cambio no tocó los controladores). |
| ¿Quién implementa `TallerService`? | `TallerServiceMongoImpl`, marcada con `@Service`. |
| ¿Quién crea `MongoTemplate`? | Spring Boot al detectar `spring-boot-starter-data-mongodb` y la propiedad `spring.mongodb.uri` de `application.properties`. |
| ¿Qué es `@Controller` vs `@RestController`? | `@Controller` devuelve nombres de plantillas HTML; `@RestController` devuelve JSON directamente. |
| ¿Qué es `@Component`? | Marca una clase para que Spring la registre (ej. un filtro o una configuración). |

---

## 4. MongoDB y `MongoTemplate`

### ¿Qué es MongoDB?

Base de datos **documental**: en vez de tablas y filas guarda **documentos** (parecidos a JSON) dentro de **colecciones**. Un documento puede llevar otros documentos y listas dentro (datos **embebidos**), así que lo que se lee junto se guarda junto. En Spring usamos **`MongoTemplate`** (Spring Data MongoDB) para consultar y modificar.

### Configuración (`application.properties`)

```properties
spring.mongodb.uri=${MONGODB_URI}
```

`MONGODB_URI` trae usuario, clave y base: `mongodb://taller_app:clave@localhost:27017/asistencia_en_carretera?authSource=asistencia_en_carretera`. No tiene valor por defecto: sin ella la app no arranca.

### Operaciones que hace el proyecto

| Método `MongoTemplate` | Uso en el proyecto |
|---------------------|-------------------|
| `find(query, Clase)` | Listar talleres, citas, valoraciones |
| `findOne` / `findById` | Un usuario por correo, un taller por id |
| `insert(doc)` | Crear usuario, cita, valoración |
| `updateFirst(query, update)` | Cambiar un taller, sumar estrellas con `$inc` |
| `indexOps(...)` | Crear los índices al arrancar (`MongoIndicesConfig`) |

### Ejemplo real — login (`UsuarioRepositorio`)

```java
mongo.findOne(Query.query(Criteria.where("email").is(email).and("activo").is(true)), UsuarioDoc.class);
// la clave NO se consulta: se compara el hash con passwordEncoder.matches(...)
```

### Ejemplo real — listar talleres

En `TallerServiceMongoImpl.listarTodos()` se lee **un solo documento por taller**: ya trae la ubicación (con el punto GeoJSON), el horario, los mecánicos y el resumen de valoraciones (`suma` y `total`; el promedio es `suma / total`). Antes en MySQL hacía falta un JOIN con tres tablas y una subconsulta `AVG`.

### Guardar taller

1. Un solo documento en `talleres`: nombre, teléfono, `ubicacion` (dirección, ciudad, **punto GeoJSON `[lng, lat]`**), `horarios` y `mecanicos`
2. Si es nuevo dueño: se inserta en `usuarios` con rol `taller` (el correo es único por índice)
3. Si faltan coordenadas: `GeocodificacionOpenStreetMap` (HTTP a Nominatim) (si no la encuentra, rechaza el registro con un aviso)

### Ids

Cada documento tiene un `_id` de tipo `ObjectId` (24 caracteres hexadecimales). En la web viajan como texto (`util/Ids`); si alguien inventa un id inválido simplemente "no existe".

---

## 5. Inyección en MongoDB (NoSQL injection) — qué decir en la sustentación

### ¿Qué es?

Igual que la inyección SQL, pero con operadores de MongoDB: si se arma el filtro a partir de JSON que manda el usuario, alguien puede mandar `{"$ne": null}` en vez de un texto y saltarse la condición.

### ¿Cómo lo evitamos?

**Los filtros se arman con `Criteria` y valores ya tipados** (texto, `ObjectId`, número); nunca se pasa JSON del usuario como filtro:

```java
// BIEN — el valor es siempre un dato, nunca un operador
Criteria.where("email").is(email)

// MAL — nunca hacer esto (el usuario controla el filtro completo)
BasicQuery(jsonQueMandoElUsuario)
```

Además los parámetros llegan como `String` (`@RequestParam String`), así que `{"$ne": null}` solo sería un texto raro. Los ids se validan con `ObjectId.isValid`.

### Limitación honesta del proyecto

- Las claves van con BCrypt, pero el logout no invalida el JWT hasta que vence.
- No hay HTTPS por defecto (la cookie segura se activa con `JWT_COOKIE_SECURE=true`).
- CSRF está desactivado porque la cookie es `SameSite=Lax`.

---

## 6. Seguridad: Spring Security + JWT

### Flujo de login

1. Usuario envía formulario en `/` → `script.js` hace `POST /api/auth/login` (o `POST /api/auth/registro` si es cliente nuevo)
2. `AuthController` busca el usuario en MongoDB, compara la clave con BCrypt y crea un **JWT** (HS256) con `id`, `nombre` y `rol`
3. El token viaja en la cookie HttpOnly `TALLERES_JWT`; el servidor no guarda sesión (STATELESS)
4. Redirección según rol: admin → `/mi-taller`, taller → `/panel-taller`, user → `/buscar-talleres`

### Filtro JWT y reglas por ruta

`JwtAuthenticationFilter` lee el token en cada petición y `SecurityConfig` decide el acceso:

- **Público:** `/`, `/registro`, `/css/*`, `/js/*`, `/api/auth/login`, `/api/auth/registro`, `/logout`
- **Resto:** exige token válido; sin él, las páginas redirigen a `/` y la API responde 401

### Control por rol en controladores

`SecurityConfig` ya decide el rol por ruta; además `VistaController` redirige al panel correcto con métodos privados:

- `redirectSiNoEsAdmin` → solo admin en `/mi-taller`, `/registrar-taller`
- `redirectSiNoEsCliente` → solo `user` en citas/valoraciones del cliente
- `redirectSiNoEsTaller` → solo `taller` en `/panel-taller`

---

## 7. Catálogo de rutas

### Vistas HTML (Thymeleaf) — `VistaController`

| Método | Ruta | Plantilla | Quién | Qué hace |
|--------|------|-----------|-------|----------|
| GET | `/` | `index.html` | Público | Login |
| GET | `/logout` | — | Todos | Cierra sesión → `/` |
| GET | `/buscar-talleres` | `buscar-talleres.html` | Sesión (cliente completo si `user`) | Mapa, búsqueda, citas, valoraciones, historial |
| GET | `/mi-taller` | `mi-taller.html` | Admin | Tablas: talleres, citas, valoraciones |
| GET | `/registrar-taller` | `registrar-taller.html` | Admin | Formulario alta/edición |
| POST | `/registrar-taller` | redirect | Admin | Guarda en BD → `#seccion-talleres` |
| GET | `/panel-taller` | `panel-taller.html` | Dueño `taller` | Citas de sus talleres |
| GET | `/detalle-taller?id=` | `detalle-taller.html` | Sesión | Ficha de un taller |
| GET | `/valoraciones` | redirect | Admin | → `/mi-taller#seccion-valoraciones` |
| GET | `/citas-pendientes` | redirect | Admin | → `/mi-taller#seccion-citas` |
| GET | `/calificar-taller` | redirect | Cliente | → buscar-talleres `?ver=calificar` |
| POST | `/calificar-taller` | redirect | Cliente | Guarda valoración |
| GET | `/solicitar-cita` | redirect | Cliente | → buscar-talleres `?ver=solicitar` |
| POST | `/solicitar-cita` | redirect | Cliente | Inserta cita |
| GET | `/historial-busquedas` | redirect | Cliente | → `?ver=historial` |

### API REST (JSON)

| Método | Ruta | Controlador | Qué hace |
|--------|------|-------------|----------|
| POST | `/api/auth/login` | `AuthController` | Valida con BCrypt, emite JWT en cookie, JSON `{ok, rol, nombre...}` (401 si falla) |
| POST | `/api/auth/registro` | `AuthController` | Crea un cliente (rol `user`) e inicia sesión |
| GET | `/api/talleres` | `TalleresController` | Lista todos los talleres (para el mapa en JS) |
| GET | `/api/historial-busqueda` | `HistorialBusquedaApiController` | Historial del usuario logueado |
| POST | `/api/historial-busqueda` | `HistorialBusquedaApiController` | Guarda término de búsqueda al pulsar Buscar |

---

## 8. Qué hace cada vista (estudio por pantalla)

### `index.html` — Login

- **JS:** `static/js/script.js`
- **Backend:** `POST /api/auth/login`
- **No usa Thymeleaf dinámico** (página estática con fetch)

### `buscar-talleres.html` — Cliente (pantalla principal)

- **Backend GET:** carga `talleresBd`, citas del cliente, formulario de valoración, taller preseleccionado para cita (`tallerCita`).
- **JS:** `buscar-talleres.js`
  - Mapa Leaflet + tiles OpenStreetMap
  - `GET /api/talleres` → marcadores solo si hay `lat` y `lng`
  - Filtro local por texto (nombre, ciudad, dirección, servicios)
  - `POST /api/historial-busqueda` al buscar
  - Geocodificación cliente con Nominatim (referencia de búsqueda en mapa)
- **Formularios en la misma página:**
  - `POST /solicitar-cita` — pedir cita
  - `POST /calificar-taller` — valorar por nombre de taller

### `mi-taller.html` — Administrador

- Tablas con paginación y búsqueda en el navegador (`mi-taller.js`)
- Enlaces: registrar taller, editar (`/registrar-taller?id=`), ver ficha cliente
- Datos: `tallerService.listarTodos()`, `listarTodasCitas()`, `listarTodasValoraciones()`

### `registrar-taller.html` — Alta/edición taller

- **POST** con `@ModelAttribute Taller` — Spring llena el objeto desde el formulario
- Campos: datos del taller + ubicación (ciudad, lat, lng) + dueño (solo en alta)
- **JS:** `registrar-taller.js` — botón para consultar coordenadas en Nominatim
- **Servicio:** `guardar(taller, uid, esAdmin)` → colección `talleres` (con ubicación, horario y mecánicos embebidos) y, si es nuevo dueño, `usuarios`

### `panel-taller.html` — Dueño del taller

- Ve **solo citas** de talleres donde él es `propietario_usuario_id`
- `listarTalleresDelPropietario(uid)`, `listarCitasPorPropietario(uid)`

### `detalle-taller.html` — Ficha pública

- Muestra un taller por `id` (o el último si no hay id)
- Valoraciones de ese taller: `listarValoracionesPorTaller(id)`

---

## 9. JavaScript del frontend (por archivo)

| Archivo | Pantalla | Función principal |
|---------|----------|-------------------|
| `script.js` | Login | `fetch` login, redirección por rol |
| `app.js` | Varias | Utilidades menú / sesión en cliente |
| `buscar-talleres.js` | Mapa cliente | Leaflet, API talleres, historial, filtros |
| `mi-taller.js` | Admin | Paginación y filtro de tablas en HTML |
| `registrar-taller.js` | Registrar | Rellenar lat/lng con Nominatim |

---

## 10. Modelo de datos (colecciones usadas en código)

| Colección | Para qué |
|-------|----------|
| `usuarios` | Login, dueños, clientes, admin |
| `talleres` | Negocio mecánico, con ubicación, horario, mecánicos y resumen de valoraciones **embebidos** |
| `citas` | Solicitudes de servicio (turno de 1 h con un mecánico) |
| `valoraciones` | Estrellas y comentarios |
| `busquedas` | Términos buscados en el mapa (se borran solas a los 180 días) |

Detalle completo en la sección 17. Los esquemas viejos de MySQL (`sql/`) y los DER/modelo relacional quedan solo como historia.

---

## 11. Flujos importantes (para dibujar en el examen)

### A) Crear taller y verlo en el mapa

```
Admin → /registrar-taller (form)
     → POST /registrar-taller
     → TallerServiceMongoImpl.guardar()
     → un documento en talleres (con ubicacion.punto = [lng, lat])
Cliente → /buscar-talleres
     → buscar-talleres.js → GET /api/talleres
     → Leaflet.marker([lat, lng])
```

### B) Pedir cita

```
Cliente → formulario en buscar-talleres
       → POST /solicitar-cita
       → AgendaServiceMongoImpl.solicitarCita() → elige un mecánico libre → insert en citas
Admin   → /mi-taller#seccion-citas ve la cita
Dueño   → /panel-taller ve citas de su taller
```

### C) Valorar taller

```
Cliente → POST /calificar-taller
       → registrarCalificacion() → insert en valoraciones + $inc en talleres.resumen
       → Promedio = resumen.suma / resumen.total (sin recalcular)
```

---

## 12. Preguntas de sustentación (con respuesta modelo)

### Spring y arquitectura

1. **¿Por qué Spring Boot?**  
   Arranque rápido, servidor embebido, configuración por `application.properties`, integración con MongoDB y Thymeleaf.

2. **¿Qué es Thymeleaf?**  
   Motor de plantillas HTML: el servidor rellena datos (`th:text`, `th:each`) antes de enviar la página.

3. **¿Qué es un `@ModelAttribute`?**  
   Enlaza los campos del formulario HTML a un objeto Java (`Taller`) en un POST.

4. **¿Qué es `RedirectAttributes`?**  
   Mensajes flash (`msgOk`, `msgError`) que sobreviven una redirección.

### Base de datos

5. **¿Por qué la ubicación va dentro del documento del taller?**  
   Porque el mapa siempre necesita taller y coordenadas juntos: se lee un solo documento, sin unir nada.

6. **¿Cuándo usar referencia y no documento embebido?**  
   Cuando la lista crece sin límite (citas, valoraciones): van en su colección y apuntan al taller con `tallerId`.

7. **¿Índices?**  
   Sí, los crea `MongoIndicesConfig` al arrancar: correo único, 2dsphere para el mapa, único parcial de agenda y TTL de búsquedas.

### Seguridad

8. **¿Cómo autentican?**  
   Con un JWT en cookie HttpOnly, tras validar el correo y comparar la clave con BCrypt.

9. **¿Cómo autorizan por rol?**  
   `SecurityConfig` (reglas por ruta y rol según el JWT) + redirecciones en `VistaController`.

10. **¿Riesgo de inyección SQL?**  
    Ya no hay SQL. El equivalente en MongoDB (inyección de operadores) se evita armando los filtros con `Criteria` y valores tipados, sin usar JSON del usuario como filtro.

### Mapa

11. **¿De dónde salen las coordenadas?**  
    Script demo `mongo/seed.js`; al registrar, formulario + geocodificación OpenStreetMap + guardado en `ubicacion.punto` del taller.

12. **¿Por qué un taller no aparece en el mapa?**  
    Si el taller no tiene `ubicacion.punto` (lat/lng), `buscar-talleres.js` no crea marcador (sí puede listar la tarjeta).

---

## 13. Estructura de carpetas (memorizar)

```
Backend/talleres/src/main/java/com/tecnologico/talleres/
├── TalleresApplication.java      # main Spring Boot
├── controllers/                  # HTTP entrada
├── services/                     # interfaz negocio
│   └── impl/                     # *ServiceMongoImpl (taller, agenda, mecánicos)
├── documents/                    # TallerDoc, CitaDoc... (lo que se guarda en MongoDB)
├── repositories/                 # UsuarioRepositorio
├── config/MongoIndicesConfig.java # índices al arrancar
├── model/                        # POJOs (Taller, Cita, ...)
├── dto/                          # LoginResponse, HistorialBusquedaRequest
├── util/GeocodificacionOpenStreetMap.java
└── security/                     # SecurityConfig, JwtService, filtro JWT

src/main/resources/
├── application.properties
├── templates/                    # HTML Thymeleaf
└── static/css, static/js         # assets
```

---

## 14. Cómo ejecutar y demostrar (checklist)

1. MongoDB: `docker compose up -d mongo` (crea el usuario de la app y carga los datos demo; ver README)
2. `cd Backend/talleres` → definir `MONGODB_URI` y `JWT_SECRET` → `mvn spring-boot:run`
3. Login admin → registrar taller con ciudad y coordenadas → ver en `/mi-taller`
4. Login cliente → `/buscar-talleres` → ver marcador y pedir cita
5. Login `taller1@demo.local` (clave `demo12345`) → `/panel-taller` → ver citas

---

## 15. Limitaciones (decirlas con honestidad — suma puntos)

- Datos demo generados con un script (no son talleres reales)
- Los clientes se registran solos; los dueños de taller los crea el admin
- Geocodificación depende de internet (Nominatim)
- Paginación del admin es en JavaScript (no en la base)
- No hay pruebas unitarias automatizadas en el repo

---

## 16. Stack tecnológico — detalle completo

### 16.1 Lenguajes y entorno

| Tecnología | Versión / detalle | Para qué la usamos |
|------------|-------------------|-------------------|
| **Java** | 17 (`pom.xml`) | Lógica del servidor, Spring |
| **HTML5** | — | Estructura de páginas |
| **CSS3** | `static/css/estilos.css`, `styles.css` | Diseño login y app |
| **JavaScript** | Vanilla (sin React/Angular) | Login, mapa, paginación tablas |
| **JavaScript (mongosh)** | `mongo/seed.js` | Datos demo e índices |

### 16.2 Backend (Spring Boot)

| Componente | Dependencia Maven | Función en el proyecto |
|------------|-------------------|------------------------|
| **Spring Boot** | parent `4.0.3` | Arranque embebido (Tomcat), configuración automática |
| **spring-boot-starter-webmvc** | pom.xml | Controladores HTTP, REST, MVC |
| **spring-boot-starter-thymeleaf** | pom.xml | Plantillas HTML en servidor (`templates/`) |
| **spring-boot-starter-data-mongodb** | pom.xml | Driver de MongoDB y `MongoTemplate` |
| **Tomcat embebido** | incluido en web | Servidor en puerto 8080 |

**No usamos en este proyecto:** JPA/Hibernate, SQL, React, Angular, Redis. Sí usamos Spring Security (con JWT) y Docker Compose (opcional).

### 16.3 Base de datos y conexión

| Elemento | Valor |
|----------|--------|
| Motor | **MongoDB 8** (Docker Compose, imagen `mongo:8.0`) |
| Base de datos | `asistencia_en_carretera` |
| Usuario app | `taller_app` con permiso `readWrite` solo sobre esa base (`mongo/init/01-usuario-app.js`) |
| Conexión | Variable de entorno `MONGODB_URI` → `spring.mongodb.uri` en `application.properties` |
| Acceso | **Spring Data MongoDB** con `MongoTemplate` (sin repositorios mágicos ni ORM) |
| Fechas | `Instant` en UTC; la zona `America/Bogota` solo se usa para mostrar y validar el horario |

### 16.4 Frontend y mapas

| Tecnología | Dónde | Rol |
|------------|-------|-----|
| **Thymeleaf** | `templates/*.html` | El servidor inserta datos de BD en HTML (`th:each`, `th:text`) |
| **Leaflet** | CDN en `buscar-talleres.html` | Mapa interactivo (zoom, marcadores) |
| **OpenStreetMap** | Tiles URL en `buscar-talleres.js` | Imágenes del mapa (gratuito) |
| **Nominatim** | fetch desde JS y Java | Convertir dirección → lat/lng |
| **Fetch API** | `script.js`, `buscar-talleres.js` | Llamadas AJAX al backend |

### 16.5 Herramientas de desarrollo

| Herramienta | Uso |
|-------------|-----|
| **Maven** | Compilar, descargar dependencias (`mvn spring-boot:run`) |
| **IntelliJ / VS Code / Cursor** | Editar código |
| **MongoDB Compass / `mongosh`** | Ver y consultar los documentos |
| **Docker Compose** | Levantar MongoDB y la app |
| **Git** | Control de versiones del repositorio |

### 16.6 Patrón arquitectónico

**MVC adaptado:**

- **Model:** clases `model/` + colecciones MongoDB (`documents/`) + `TallerService`
- **View:** Thymeleaf + CSS + JS
- **Controller:** `VistaController`, `AuthController`, APIs REST

**Capa de servicio adicional** entre Controller y MongoDB (buena práctica aunque el curso solo pida MVC).

---

## 17. Base de datos — colecciones y diseño (MongoDB)

### 17.1 Idea central: qué va embebido y qué va aparte

Regla: **se embebe lo que se lee siempre junto y no crece sin límite; va aparte lo que crece o se consulta por su cuenta.**

```mermaid
erDiagram
    USUARIOS ||--o{ TALLERES : "propietarioId"
    USUARIOS ||--o{ CITAS : "clienteId"
    USUARIOS ||--o{ VALORACIONES : "clienteId"
    USUARIOS ||--o{ BUSQUEDAS : "usuarioId"
    TALLERES ||--o{ CITAS : "tallerId"
    TALLERES ||--o{ VALORACIONES : "tallerId"
    CITAS ||--o| VALORACIONES : "citaId (opcional)"
```

Dentro de `talleres` van embebidos: `ubicacion` (1:1), `horarios` (pocos, uno por día), `mecanicos` (pocos por taller) y `resumen` de valoraciones. Aparte: `citas`, `valoraciones` y `busquedas`, porque crecen sin límite.

### 17.2 Colección por colección

#### `usuarios`

| Campo | Notas |
|-------|-------|
| `_id` | `ObjectId` |
| `email` | Único (índice `uk_usuarios_email`), siempre en minúsculas |
| `passwordHash` | BCrypt |
| `nombre`, `rol` | `rol`: `admin`, `user` o `taller` |
| `activo` | Si es `false` no puede entrar |
| `creadoEn` | Fecha (UTC) |

#### `talleres`

| Campo | Notas |
|-------|-------|
| `propietarioId` | `ObjectId` del usuario dueño |
| `nombre`, `telefono`, `descripcion`, `servicios`, `estado` | Datos del formulario; `estado` = `publicado` |
| `ubicacion` | `{direccion, ciudad, departamento, pais, punto}`; `punto` es **GeoJSON** `{type:"Point", coordinates:[lng, lat]}` con índice `2dsphere` (listo para "talleres cerca de mí") |
| `horarios` | `[{dia (1=lun…7=dom), apertura "HH:mm", cierre "HH:mm"}]` |
| `mecanicos` | `[{_id, nombres, apellidos, telefono, activo}]`; su `_id` se guarda en las citas |
| `resumen` | `{suma, total}`: se actualiza con `$inc` al valorar; promedio = suma / total |

#### `citas`

| Campo | Notas |
|-------|-------|
| `clienteId`, `tallerId`, `mecanicoId` | Referencias |
| `tallerNombre`, `mecanicoNombre`, `cliente` | Copias para listar sin consultar otras colecciones |
| `fechaHora` | `Date` en UTC (turno de 1 hora) |
| `estado` | `solicitada`, `confirmada`, `cancelada`, `completada` |
| `ocupaTurno` | `true` mientras no esté cancelada |
| `motivo` | Texto del cliente |

**Regla de agenda sin bloqueos:** índice único parcial `uk_citas_mecanico_turno` sobre `(mecanicoId, fechaHora)` solo para `ocupaTurno: true`. Si dos personas piden el mismo mecánico a la misma hora, a la segunda el `insert` le falla con `DuplicateKeyException` y la app prueba con otro mecánico. Una cita cancelada pone `ocupaTurno` en `false` y libera el turno.

#### `valoraciones`

| Campo | Notas |
|-------|-------|
| `clienteId`, `clienteNombre`, `tallerId`, `tallerNombre` | Referencias y copias de nombres |
| `citaId` | Opcional |
| `estrellas` | 1–5 (se valida en el servicio) |
| `servicio`, `comentario` | Texto |

#### `busquedas`

| Campo | Notas |
|-------|-------|
| `usuarioId`, `termino`, `origenLat`, `origenLng`, `radioM`, `resultados`, `origen` | Búsqueda del mapa |
| `creadoEn` | Índice **TTL** `ttl_busquedas`: MongoDB borra solo las de más de 180 días |

### 17.3 Índices (`config/MongoIndicesConfig` los crea al arrancar)

| Índice | Para qué |
|--------|----------|
| `uk_usuarios_email` (único) | Un correo, una cuenta; frena registros simultáneos |
| `idx_talleres_ubicacion` (2dsphere) | Búsquedas por cercanía |
| `idx_talleres_propietario`, `idx_talleres_mecanico` | Talleres de un dueño; talleres de un mecánico |
| `uk_citas_mecanico_turno` (único parcial) | Un mecánico, una cita por hora |
| `idx_citas_taller_fecha`, `idx_citas_cliente` | Agenda del taller y citas del cliente |
| `idx_valoraciones_taller`, `idx_valoraciones_cliente` | Reseñas de un taller / de un cliente |
| `idx_busquedas_usuario`, `ttl_busquedas` | Historial del usuario; borrado automático |

### 17.4 Integridad sin claves foráneas

MongoDB no tiene FK ni `ON DELETE`: la integridad la cuida la aplicación. Por ejemplo, un mecánico no se puede dar de baja si es el último activo o tiene citas pendientes, y en la app no se borran talleres ni usuarios (se desactivan).

### 17.5 Mapeo BD → Java → pantalla

| Colección | Documento Java (`documents/`) | Modelo de vista (`model/`) | Pantalla / API |
|-----------|-------------------------------|---------------------------|----------------|
| usuarios | `UsuarioDoc` | (login directo) | `index`, `AuthController` |
| talleres | `TallerDoc` | `Taller`, `Mecanico`, `HorarioDia` | `mi-taller`, `/api/talleres`, mapa |
| citas | `CitaDoc` | `Cita` | `buscar-talleres`, `mi-taller`, `panel-taller` |
| valoraciones | `ValoracionDoc` | `Valoracion` | `detalle-taller`, `mi-taller`, formulario calificar |
| busquedas | `BusquedaDoc` | `BusquedaHistorial` | `buscar-talleres`, API historial |

`services/impl/Mapeos` convierte de documento a modelo de vista.

### 17.6 Datos demo (memorizar para demo oral)

`mongo/seed.js` genera con semilla fija: 1 admin, 300 clientes, **1.000 talleres** (cada uno con su dueño, 1–5 mecánicos y horario) en 11 ciudades de Colombia, ~5.000 citas coherentes con el horario y sin choques de mecánico, ~2.200 valoraciones con el resumen de cada taller al día y ~1.200 búsquedas. Clave de todos los usuarios demo: `demo12345`. Las pruebas de coherencia: `node mongo/seed.test.js`.

---

## 18. Banco ampliado de preguntas (sustentación)

### A. Proyecto y negocio

1. ¿Qué problema resuelve la aplicación?  
2. ¿Quiénes son los actores (admin, cliente, dueño)?  
3. ¿Por qué un mapa y no solo una lista?  
4. ¿Qué pasa si un taller no tiene coordenadas?  
5. ¿Cómo se calcula la calificación promedio de un taller?

### B. Tecnologías

6. ¿Por qué Spring Boot y no solo Servlets?  
7. ¿Qué es Maven y para qué sirve el `pom.xml`?  
8. ¿Qué es Thymeleaf y en qué se diferencia de HTML estático?  
9. ¿Qué es Leaflet y qué datos necesita?  
10. ¿Qué es OpenStreetMap y es de pago?  
11. ¿Qué es Nominatim?  
12. ¿Por qué Java 17?  
13. ¿Qué servidor HTTP usa la app? (Tomcat embebido)  
14. ¿Usan JPA o Hibernate? (No: Spring Data MongoDB con `MongoTemplate`)  
15. ¿Por qué no usaron un framework frontend pesado?

### C. Base de datos (MongoDB)

16. ¿Cuántas colecciones tiene el proyecto? (5: usuarios, talleres, citas, valoraciones, busquedas)  
17. ¿Qué es un documento y qué es una colección?  
18. ¿Qué va embebido en `talleres` y por qué? (ubicación, horarios, mecánicos, resumen)  
19. ¿Por qué las citas van en otra colección? (crecen sin límite)  
20. ¿Qué es un índice único (`uk_usuarios_email`)?  
21. ¿Qué es un índice único parcial y para qué lo usamos? (un mecánico, una cita por hora)  
22. ¿Qué es un índice TTL? (borra las búsquedas viejas solo)  
23. ¿Qué roles existen en `usuarios.rol`?  
24. ¿Qué guarda `citas`?  
25. ¿Puede una valoración existir sin cita? (Sí, `citaId` es opcional)  
26. ¿Dónde se valida que las estrellas sean de 1 a 5? (en el servicio, MongoDB no obliga)  
27. ¿Qué es GeoJSON y por qué el punto va como `[longitud, latitud]`?  
28. ¿Qué usuario usa la aplicación para conectarse a MongoDB? (`taller_app`, solo `readWrite` sobre su base)  
29. ¿Dónde está la cadena de conexión? (`MONGODB_URI` → `spring.mongodb.uri`)  
30. ¿Qué es un `ObjectId`?

### D. MongoDB desde Java

31. ¿Qué es `MongoTemplate`?  
32. ¿Qué es `Criteria`/`Query`?  
33. ¿Cómo se evita la inyección de operadores NoSQL? (filtros con `Criteria` y valores tipados)  
34. ¿Qué es un documento embebido frente a una referencia? ¿Cuándo usar cada uno?  
35. ¿Cómo se evita que dos clientes tomen el mismo mecánico a la misma hora? (índice único parcial + reintento)  
36. ¿Cómo se mantiene el promedio de un taller? (`$inc` sobre `resumen.suma` y `resumen.total`)  
37. ¿Qué pasa si se inserta un correo repetido? (`DuplicateKeyException` → "Ese correo ya está registrado")  
38. ¿Por qué las fechas van en UTC? (así no dependen de la zona del servidor; se muestran en America/Bogota)  
39. ¿Hay transacciones? (No hacen falta: cada operación toca un solo documento o la resuelve un índice único)  
40. ¿Qué diferencia hay entre `find` y `findOne`?

### E. Spring e inyección de dependencias

41. ¿Qué es el contenedor IoC de Spring?  
42. ¿Qué significa `@Service`, `@Controller`, `@RestController`?  
43. ¿Qué es inyección por constructor y ventaja?  
44. ¿Por qué programar contra `TallerService` y no la implementación?  
45. ¿Qué es `@ModelAttribute`?  
46. ¿Qué es `RedirectAttributes` / flash attributes?  
47. ¿Qué hace `@GetMapping` vs `@PostMapping`?  
48. ¿Qué es un `@RestController`?  
49. ¿Qué es el `application.properties`?  
50. ¿Qué es un filtro en Spring Security? (`JwtAuthenticationFilter` revisa el token en cada petición)

### F. Seguridad y sesión

51. ¿Cómo funciona el login?  
52. ¿Dónde se guarda la sesión? (En ningún lado del servidor: va en el JWT, dentro de una cookie HttpOnly)  
53. ¿Qué datos lleva el token? (`id` como subject, `nombre`, `rol` y fecha de vencimiento)  
54. ¿Qué rutas son públicas?  
55. ¿Cómo impiden que un cliente entre a `/mi-taller`?  
56. ¿Cómo se guardan las contraseñas? (Con BCrypt; nunca en texto plano)  
57. ¿Qué es autenticación vs autorización?  
58. ¿Usan JWT? (Sí, firmado con HS256 y guardado en cookie HttpOnly)  
59. ¿Qué pasa si no hay token? (Las páginas redirigen a `/`; la API responde 401)  
60. ¿Qué es CSRF y lo tienen? (Está desactivado a propósito: la cookie es `SameSite=Lax`, así que el navegador no la envía en POST desde otros sitios)

### G. Frontend y flujos

61. ¿Cómo carga el mapa los talleres? (`GET /api/talleres`)  
62. ¿Qué hace el botón Buscar en el mapa?  
63. ¿Dónde se guarda el historial de búsqueda?  
64. ¿Cómo funciona la paginación en `mi-taller.js`?  
65. ¿Qué es `fetch` y `credentials: same-origin`?  
66. ¿Por qué admin y cliente usan pantallas distintas tras login?

### H. Respuestas rápidas “una línea”

| Pregunta corta | Respuesta |
|----------------|-----------|
| ¿ORM? | No, `MongoTemplate` |
| ¿BD? | MongoDB `asistencia_en_carretera` |
| ¿Mapa? | Leaflet + OSM |
| ¿Coordenadas? | `ubicacion.punto` (GeoJSON) en `talleres` |
| ¿DI? | Constructor + Spring |
| ¿Capas? | Controller → Service → MongoDB |

---

## 19. Glosario (términos del profesor)

| Término | Significado en tu proyecto |
|---------|---------------------------|
| **API REST** | URLs como `/api/talleres` que devuelven JSON |
| **DTO** | `LoginResponse`, `HistorialBusquedaRequest` — objetos para transferir datos |
| **POJO** | `Taller`, `Cita` — clases simples sin anotaciones de persistencia |
| **Documento / colección** | Registro tipo JSON / conjunto de registros (equivale a fila / tabla) |
| **ObjectId** | Identificador de 24 caracteres hexadecimales de cada documento |
| **Embebido** | Documento guardado dentro de otro (ej. `ubicacion` dentro de `talleres`) |
| **JWT** | Token firmado que lleva id, nombre y rol; viaja en una cookie HttpOnly |
| **BCrypt** | Algoritmo para guardar contraseñas como hash |
| **Redirect** | `return "redirect:/mi-taller"` — segunda petición GET |
| **Flash message** | `msgOk` / `msgError` una sola vez tras redirect |
| **Geocodificación** | Dirección → lat/lng |
| **Marker** | Pin del taller en Leaflet |

---

*Última revisión según código en `Backend/talleres` — proyecto Asistencia en carretera.*
