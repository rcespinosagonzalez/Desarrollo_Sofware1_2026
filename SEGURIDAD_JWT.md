# Seguridad: Spring Security + JWT

## Como funciona

1. El usuario envia correo y clave a `POST /api/auth/login`.
2. `AuthController` busca el usuario en MySQL y compara la clave con **BCrypt** (en la BD ya no hay texto plano).
3. Si es correcta, `JwtService` crea un **token JWT** firmado (HS256) con id, nombre y rol, y lo entrega en una
   **cookie HttpOnly** (`TALLERES_JWT`). Tambien viene en el JSON (`token`) para probar con Postman o curl.
4. En cada peticion, `JwtAuthenticationFilter` lee el token (cookie o cabecera `Authorization: Bearer ...`),
   verifica firma y vencimiento, y deja al usuario autenticado con su rol.
5. `SecurityConfig` decide quien entra a cada ruta segun el rol. Si no hay login redirige a `/` (API: 401);
   si el rol no corresponde lo manda a su panel (API: 403).
6. `GET /logout` borra la cookie.

No hay sesion en el servidor (STATELESS): todo lo que el servidor necesita saber viene en el token.

## Archivos

| Archivo | Para que sirve |
|---|---|
| `security/SecurityConfig.java` | Reglas de acceso por ruta y rol, BCrypt, modo sin sesion |
| `security/JwtService.java` | Crear/validar el token y armar la cookie |
| `security/JwtAuthenticationFilter.java` | Lee el token en cada peticion |
| `security/SesionActual.java` | Atajo para obtener id, rol y nombre del usuario logueado |
| `security/UsuarioAutenticado.java` | Datos que viajan en el token |
| `security/PasswordMigrationRunner.java` | Convierte a BCrypt claves viejas en texto plano al arrancar |
| `controllers/AuthController.java` | Login con BCrypt y emision del JWT |

## Rutas y roles

| Ruta | Quien entra |
|---|---|
| `/`, `/css/**`, `/js/**`, `/api/auth/login`, `/logout` | Todos |
| `/mi-taller`, `/valoraciones`, `/citas-pendientes` | admin |
| `/panel-taller` | taller |
| `/registrar-taller` | admin y taller |
| `/solicitar-cita`, `/calificar-taller`, `/historial-busquedas`, `/api/historial-busqueda` | user (cliente) |
| Lo demas (mapa, detalle, `/api/talleres`) | Cualquier usuario con login |

## Configuracion

Variables de entorno (en `.env` con Docker): `JWT_SECRET` (32 o mas caracteres), `JWT_EXPIRATION_MINUTES` (60 por defecto),
`JWT_COOKIE_SECURE` (poner `true` cuando haya HTTPS). Genera una clave con `openssl rand -base64 48`.

## Probar con curl

```bash
# Login: guarda la cookie
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -d "email=admin@demo.local" -d "password=demo"

# Ruta protegida con la cookie (200)
curl -b cookies.txt http://localhost:8080/api/talleres

# Sin token (401)
curl -i http://localhost:8080/api/talleres

# Con el token en cabecera (copia el valor de "token" del login)
curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/talleres

# Rol incorrecto: un cliente pidiendo una ruta de admin va a su panel (302)
```

## Limitaciones conocidas

- El logout borra la cookie, pero el token sigue siendo valido hasta que venza (es la naturaleza de un JWT sin lista negra).
- CSRF esta desactivado porque la cookie es `SameSite=Lax` (el navegador no la envia en POST de otros sitios).
- No hay bloqueo por intentos fallidos de login.
