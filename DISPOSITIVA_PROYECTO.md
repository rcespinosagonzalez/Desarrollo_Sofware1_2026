# Guía para diapositiva — Asistencia en carretera

Usa este documento como **guion de presentación** (PowerPoint, Canva o Google Slides).  
Sugerencia: **12–15 diapositivas**, 8–10 minutos.

---

## Diapositiva 1 — Portada

**Título:** Asistencia en carretera  
**Subtítulo:** Plataforma web para buscar talleres, pedir citas y valorar servicios  
**Datos:** Nombre del estudiante · Curso · 4° semestre · 2026  

**Nota al presentar:** Una frase: *"Conectamos conductores con talleres mecánicos usando mapa y base de datos."*

---

## Diapositiva 2 — Problema y solución

**Problema**
- En carretera es difícil encontrar un taller confiable y cercano
- No hay un solo lugar para ver ubicación, horario y opiniones

**Solución**
- Aplicación web con mapa interactivo
- Citas en línea y valoraciones de clientes
- Panel para administrador y para dueños de taller

---

## Diapositiva 3 — Objetivos del proyecto

- Buscar talleres por zona o texto en un **mapa**
- **Registrar** talleres con ubicación real (latitud/longitud)
- **Solicitar citas** como cliente
- **Valorar** talleres después del servicio
- **Administrar** talleres, citas y valoraciones (rol admin)

---

## Diapositiva 4 — Tecnologías (stack completo)

| Capa | Tecnología | Detalle |
|------|------------|---------|
| Lenguaje | Java 17 | Backend |
| Framework | Spring Boot 4.0.3 | Web + JDBC + Thymeleaf |
| Build | Maven | `pom.xml`, `mvn spring-boot:run` |
| Vistas | Thymeleaf | `templates/*.html` |
| BD | MySQL 8 | `asistencia_en_carretera` |
| Acceso datos | JDBC `JdbcTemplate` | Sin JPA/Hibernate |
| Cliente web | HTML, CSS, JavaScript | Sin React |
| Mapa | Leaflet + tiles OSM | Marcadores con lat/lng |
| APIs externas | Nominatim | Dirección → coordenadas |
| Servidor | Tomcat embebido | Puerto 8080 |

**No usamos:** Spring Security, Redis, Angular, Docker (obligatorio).

---

## Diapositiva 5 — Arquitectura (diagrama)

Incluir este esquema (puedes copiarlo a la diapositiva):

```
┌─────────────┐
│  Navegador  │  HTML + JS + Leaflet
└──────┬──────┘
       │ HTTP
┌──────▼──────┐
│ Controllers │  VistaController, AuthController, APIs
└──────┬──────┘
       │
┌──────▼──────┐
│   Service   │  TallerService (interfaz)
└──────┬──────┘
       │
┌──────▼──────┐
│ JDBC + MySQL│  TallerServiceJdbcImpl
└─────────────┘
```

**Frase clave:** *"Capas separadas: web, negocio y datos."*

---

## Diapositiva 6 — Base de datos (nombre y motor)

**Nombre:** `asistencia_en_carretera`  
**Motor:** MySQL 8 · InnoDB · `utf8mb4`  
**Scripts:** `sql/01_crear_usuario_mysql.sql` + `sql/02_asistencia_en_carretera.sql`  
**Conexión app:** `taller_app` → `application.properties`

---

## Diapositiva 6b — Relaciones (diagrama verbal)

Dibuja o muestra este esquema:

```
USUARIO (admin / user / taller)
   │
   ├──1:N──► TALLER ◄──1:1── UBICACION_TALLER (lat, lng)
   │            │
   │            ├──1:N──► CITA ◄── usuario cliente
   │            └──1:N──► VALORACION
   │
   └──1:N──► HISTORIAL_BUSQUEDA
```

**Frases clave:**
- Un **dueño** (`rol taller`) es propietario de un **taller**
- La **ubicación** es 1 a 1: sin lat/lng no hay pin en el mapa
- El **cliente** pide **citas** y escribe **valoraciones**

---

## Diapositiva 6c — Tablas que sí usa la app

| Tabla | Uso real |
|-------|----------|
| usuario | Login y dueños |
| taller + ubicacion_taller | Mapa, admin, registro |
| cita | Cliente, admin, panel dueño |
| valoracion | Cliente y listados |
| historial_busqueda | Al pulsar Buscar |

*En SQL existen también `mecanico`, `servicio`, `taller_servicio` — preparadas para escalar.*

---

## Diapositiva 7 — Roles de usuario

| Rol | Usuario demo | Pantalla |
|-----|--------------|----------|
| Administrador | admin@demo.local | Gestión de talleres, citas y valoraciones |
| Cliente | cliente@demo.local | Mapa, citas, valoraciones |
| Dueño de taller | taller1@demo.local | Citas de su taller |

**Demostración en vivo:** mostrar los 3 logins y pantallas distintas

---

## Diapositiva 8 — Pantalla cliente (mapa)

**Ruta:** `/buscar-talleres`

**Elementos a mostrar (captura de pantalla):**
- Mapa con marcadores de talleres
- Buscador por texto
- Lista de resultados al lado
- Formularios: pedir cita, valorar, historial

**Cómo funciona (3 viñetas):**
1. JavaScript carga talleres: `GET /api/talleres`
2. Solo pinta marcador si hay latitud y longitud en BD
3. Al buscar, guarda historial: `POST /api/historial-busqueda`

---

## Diapositiva 9 — Pantalla administrador

**Ruta:** `/mi-taller`

**Elementos:**
- Resumen: cantidad de talleres, citas, valoraciones
- Tabla de talleres (buscar, paginar, editar)
- Secciones de citas y valoraciones

**Ruta relacionada:** `/registrar-taller` — alta con ciudad y coordenadas para el mapa

---

## Diapositiva 10 — Registrar taller (flujo)

**Pasos numerados:**
1. Admin completa formulario (datos + ubicación + dueño)
2. Opcional: botón "Buscar coordenadas" (OpenStreetMap)
3. `POST /registrar-taller` → guarda en MySQL
4. Cliente recarga mapa → aparece el nuevo marcador

**Mensaje:** *"Sin coordenadas no hay marcador en el mapa."*

---

## Diapositiva 11 — Seguridad básica

- **Login** contra tabla `usuario`
- **Sesión HTTP** (`usuarioId`, `rol` en servidor)
- **Interceptor:** sin sesión no entra a rutas privadas
- **Control por rol** en controladores (admin / cliente / taller)
- **SQL seguro:** consultas con `?` (evita inyección SQL)

**Limitación honesta:** contraseñas demo en texto plano (solo para aula)

---

## Diapositiva 12 — Inyección de dependencias (1 diapositiva técnica)

**Idea simple:** Spring entrega las dependencias por constructor.

Ejemplo en pantalla:
```java
public VistaController(TallerService tallerService) {
    this.tallerService = tallerService;
}
```

**Beneficio:** código más limpio y fácil de probar/cambiar implementación.

---

## Diapositiva 13 — JDBC (1 diapositiva técnica)

- Spring Boot configura conexión en `application.properties`
- `JdbcTemplate` ejecuta SQL en `TallerServiceJdbcImpl`
- Consultas con **JOIN** para traer dirección y promedio de valoraciones
- **INSERT/UPDATE** al registrar taller, citas y valoraciones

---

## Diapositiva 14 — Resultados y pruebas

**Qué se logró:**
- CRUD de talleres funcional
- Mapa con talleres de MySQL
- Citas y valoraciones persistentes
- Tres perfiles de usuario operativos

**Prueba rápida en demo:**
1. Crear taller nuevo
2. Verlo en mapa como cliente
3. Pedir cita y verla en panel admin

---

## Diapositiva 15 — Conclusiones y trabajo futuro

**Conclusiones**
- Proyecto full stack académico con arquitectura en capas
- Integración mapa + BD resuelve el caso de "asistencia en carretera"

**Trabajo futuro**
- Contraseñas con hash (BCrypt)
- Registro público de clientes
- App móvil o notificaciones de citas
- Paginación en servidor (SQL `LIMIT`)

**Cierre:** *"¿Preguntas?"*

---

## Anexo — Ideas visuales por diapositiva

| # | Visual sugerido |
|---|-----------------|
| 1 | Logo + foto de carretera/taller |
| 5 | Diagrama de capas (bloques) |
| 6 | ER simplificado o tabla Excel |
| 8 | Captura mapa Leaflet |
| 9 | Captura panel admin |
| 10 | Flecha formulario → BD → mapa |

---

## Anexo — Demo en vivo (orden recomendado, ~3 min)

1. Login **admin** → mostrar `/mi-taller`
2. **Registrar taller** con Cartagena + buscar coordenadas → Guardar
3. Logout → Login **cliente** → `/buscar-talleres` → buscar nombre del taller → marcador
4. **Pedir cita** → volver admin → ver cita en tabla
5. (Opcional) Login **taller1@demo.local** → `/panel-taller`

---

## Anexo — Una frase por si preguntan...

| Tema | Frase |
|------|--------|
| Spring Boot | "Nos ahorra configurar el servidor y la conexión a MySQL." |
| Thymeleaf | "El servidor arma el HTML con datos de la base antes de enviarlo." |
| Leaflet | "Biblioteca JavaScript gratuita para mapas con OpenStreetMap." |
| JDBC | "Conectamos Java directamente con MySQL usando plantillas de Spring." |
| DI | "No instanciamos el servicio a mano; Spring lo inyecta en el controlador." |
| Inyección SQL | "Usamos parámetros ? en lugar de pegar texto del usuario en el SQL." |

---

*Material complementario detallado: `MATERIAL_ESTUDIO_SUSTENTACION.md` (secciones 16–19: tecnologías, BD, 60+ preguntas)*
