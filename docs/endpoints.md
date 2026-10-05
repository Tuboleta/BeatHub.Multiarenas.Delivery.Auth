# 🔐 Documentación de Endpoints - BeatHub Delivery Auth

Microservicio responsable de la autenticación centralizada, emisión y validación de tokens JWT, integración SSO con TuBoleta (ADS), gestión de usuarios, personas y asignación de roles multi-arena.

---

## 📌 Información General

| Parámetro | Detalle |
| :--- | :--- |
| **Nombre Microservicio** | `beathub-delivery-auth` |
| **Puerto Local Directo** | `8080` |
| **Context Path Directo** | `/api/v1/auth` |
| **Base URL Directa** | `http://localhost:8080/api/v1/auth` |
| **Base URL Gateway** | `http://localhost:8000/api/v1/auth` |
| **Documentación Swagger**| `http://localhost:8080/api/v1/auth/swagger-ui.html` |
| **OpenAPI JSON Spec** | `http://localhost:8080/api/v1/auth/v3/api-docs` |
| **Formato de Peticiones**| `application/json; charset=UTF-8` |
| **Formato de Respuestas**| `application/json; charset=UTF-8` |

### Headers Globales Recomendados
```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer <TOKEN_JWT>  # En endpoints protegidos
X-Correlation-Id: <UUID>           # Opcional, para trazabilidad
```

### Estructura Estándar de Respuesta (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Mensaje descriptivo",
  "data": { ... },
  "timestamp": "2026-10-05T10:00:00"
}
```

### Estructura Estándar de Error (`ErrorResponse`)
```json
{
  "timestamp": "2026-10-05T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Detalle del error de validación o negocio",
  "path": "/api/v1/auth/login"
}
```

---

## 📋 Historial de Cambios (Changelog de Endpoints)

| Fecha | Versión | Endpoint / Recurso | Tipo de Cambio | Descripción |
| :--- | :---: | :--- | :---: | :--- |
| 2026-10-05 | 1.0.0 | Todos | Creación | Documentación técnica inicial completa de todos los endpoints de autenticación y usuarios. |

---

## 🧭 Índice Rápido de Endpoints

| Método | Endpoint Gateway | Requiere Auth | Roles Permitidos | Resumen |
| :---: | :--- | :---: | :--- | :--- |
| `POST` | `/api/v1/auth/login` | ❌ No | Público | Inicio de sesión tradicional (email o username + password). |
| `POST` | `/api/v1/auth/registro` | ❌ No | Público | Registro de nuevo cliente con datos de persona y usuario. |
| `POST` | `/api/v1/auth/sso/login` | ❌ No | Público | Inicio de sesión o alta automática vía SSO TuBoleta (ADS). |
| `POST` | `/api/v1/auth/validate` | ❌ No | Interno / Gateway | Introspección y validación de token JWT. |
| `GET` | `/api/v1/auth/me` | ✅ Sí | Autenticado | Obtener información y perfil del usuario en sesión. |
| `POST` | `/api/v1/auth/refresh` | ✅ Sí | Autenticado | Renovar token JWT o cambiar contexto de arena activa. |
| `POST` | `/api/v1/auth/usuarios/asignar-arena` | ✅ Sí | `SUPER_ADMIN`, `ADMIN_ARENA` | Asignar rol de Runner o Administrador de Arena a un usuario. |
| `GET` | `/api/v1/auth/usuarios/{id}` | ✅ Sí | `SUPER_ADMIN`, `ADMIN_ARENA` o Propio | Obtener usuario por ID numérico. |
| `GET` | `/api/v1/auth/usuarios/username/{username}` | ✅ Sí | `SUPER_ADMIN`, `ADMIN_ARENA` o Propio | Obtener usuario por username. |
| `GET` | `/api/v1/auth/status` | ❌ No | Público | Healthcheck básico del microservicio. |

---

## 🔍 Detalle Técnico de Endpoints

### 1. Iniciar Sesión Tradicional (`/login`)
Permite a usuarios registrados autenticarse usando su nombre de usuario o correo electrónico y contraseña.

- **Método:** `POST`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/login`
- **URL Directa:** `http://localhost:8080/api/v1/auth/login`
- **Autenticación:** No requerida.
- **Headers:** `Content-Type: application/json`

#### Request Body
| Campo | Tipo | Obligatorio | Descripción | Ejemplo |
| :--- | :---: | :---: | :--- | :--- |
| `login` | `string` | Sí | Nombre de usuario o correo electrónico registrado | `"cliente@beathub.com"` |
| `password` | `string` | Sí | Contraseña en texto plano | `"Password123*"` |

```json
{
  "login": "cliente@beathub.com",
  "password": "Password123*"
}
```

#### Respuestas
- **`200 OK`**:
```json
{
  "success": true,
  "message": "Inicio de sesión exitoso",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  },
  "timestamp": "2026-10-05T10:00:00"
}
```
- **`401 Unauthorized`**: Credenciales inválidas o usuario inactivo.

#### cURL de Ejemplo
```bash
curl -X POST "http://localhost:8000/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "login": "cliente@beathub.com",
    "password": "Password123*"
  }'
```

---

### 2. Registro de Nuevo Usuario Tradicional (`/registro`)
Permite crear la ficha de persona y cuenta de usuario con rol de `CLIENTE`.

- **Método:** `POST`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/registro`
- **URL Directa:** `http://localhost:8080/api/v1/auth/registro`
- **Autenticación:** No requerida.
- **Headers:** `Content-Type: application/json`

#### Request Body
| Campo | Tipo | Obligatorio | Descripción | Ejemplo |
| :--- | :---: | :---: | :--- | :--- |
| `nombres` | `string` | Sí | Nombres de la persona | `"Carlos"` |
| `apellidos` | `string` | Sí | Apellidos de la persona | `"Gómez"` |
| `tipoDocumentoId` | `integer` | Sí | ID del tipo de documento (1: CC, 2: CE, 3: Pasaporte) | `1` |
| `numeroDocumento` | `string` | Sí | Número de identificación | `"1012345678"` |
| `email` | `string` | Sí | Correo electrónico único | `"carlos.gomez@correo.com"` |
| `telefono` | `string` | No | Número telefónico o móvil | `"+573001234567"` |
| `paisId` | `integer` | No | ID de país | `1` |
| `departamentoId` | `integer` | No | ID de departamento o estado | `11` |
| `ciudadId` | `integer` | No | ID de municipio o ciudad | `11001` |
| `direccion` | `string` | No | Dirección física de residencia | `"Calle 100 # 15-20"` |
| `username` | `string` | Sí | Nombre de usuario único (3-50 caracteres) | `"carlosgomez"` |
| `password` | `string` | Sí | Contraseña (mínimo 6 caracteres) | `"MiClaveSegura2026*"` |
| `arenaId` | `integer` | No | Arena de origen si aplica | `1` |

```json
{
  "nombres": "Carlos",
  "apellidos": "Gómez",
  "tipoDocumentoId": 1,
  "numeroDocumento": "1012345678",
  "email": "carlos.gomez@correo.com",
  "telefono": "+573001234567",
  "paisId": 1,
  "departamentoId": 11,
  "ciudadId": 11001,
  "direccion": "Calle 100 # 15-20",
  "username": "carlosgomez",
  "password": "MiClaveSegura2026*",
  "arenaId": 1
}
```

#### Respuestas
- **`201 Created`**:
```json
{
  "success": true,
  "message": "Usuario registrado exitosamente",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  },
  "timestamp": "2026-10-05T10:00:00"
}
```
- **`400 Bad Request`**: Datos incompletos, email ya registrado o username en uso.

#### cURL de Ejemplo
```bash
curl -X POST "http://localhost:8000/api/v1/auth/registro" \
  -H "Content-Type: application/json" \
  -d '{
    "nombres": "Carlos",
    "apellidos": "Gómez",
    "tipoDocumentoId": 1,
    "numeroDocumento": "1012345678",
    "email": "carlos.gomez@correo.com",
    "telefono": "+573001234567",
    "paisId": 1,
    "departamentoId": 11,
    "ciudadId": 11001,
    "direccion": "Calle 100 # 15-20",
    "username": "carlosgomez",
    "password": "MiClaveSegura2026*",
    "arenaId": 1
  }'
```

---

### 3. Login / Registro SSO TuBoleta (`/sso/login`)
Punto de entrada para autenticación delegada desde las aplicaciones de TuBoleta (PWA Cliente, App Runner, Admin Web). Si el usuario no existe en BeatHub, se aprovisiona automáticamente con los datos entregados por TuBoleta.

- **Método:** `POST`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/sso/login`
- **URL Directa:** `http://localhost:8080/api/v1/auth/sso/login`
- **Autenticación:** No requerida.
- **Headers:** `Content-Type: application/json`

#### Request Body
| Campo | Tipo | Obligatorio | Descripción | Ejemplo |
| :--- | :---: | :---: | :--- | :--- |
| `ssoId` | `string` | Sí | Identificador único de usuario en TuBoleta ADS | `"TBL-USER-987654"` |
| `ssoProvider` | `string` | No | Nombre del proveedor SSO (por defecto `"TUBOLETA"`) | `"TUBOLETA"` |
| `email` | `string` | Sí | Correo electrónico verificado | `"fan@tuboleta.com"` |
| `clientType` | `string` | No | Canal de cliente: `USER_APP`, `RUNNER_APP`, `ADMIN_WEB` | `"USER_APP"` |
| `arenaId` | `integer` | No | ID de la arena sede activa en la app | `1` |
| `nombres` | `string` | No | Nombres recuperados del SSO | `"Andrea"` |
| `apellidos` | `string` | No | Apellidos recuperados del SSO | `"Martínez"` |
| `tipoDocumentoId` | `integer`| No | Tipo de documento (ej. 1) | `1` |
| `numeroDocumento` | `string` | No | Cédula o identificación | `"52987654"` |
| `telefono` | `string` | No | Celular del usuario | `"+573105559988"` |
| `ssoToken` | `string` | No | Token de acceso provisto por el proveedor SSO | `"ext-token-abc"` |

```json
{
  "ssoId": "TBL-USER-987654",
  "ssoProvider": "TUBOLETA",
  "email": "fan@tuboleta.com",
  "clientType": "USER_APP",
  "arenaId": 1,
  "nombres": "Andrea",
  "apellidos": "Martínez",
  "numeroDocumento": "52987654",
  "telefono": "+573105559988"
}
```

#### Respuestas
- **`200 OK`**:
```json
{
  "success": true,
  "message": "Autenticación SSO exitosa",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  },
  "timestamp": "2026-10-05T10:00:00"
}
```

#### cURL de Ejemplo
```bash
curl -X POST "http://localhost:8000/api/v1/auth/sso/login" \
  -H "Content-Type: application/json" \
  -d '{
    "ssoId": "TBL-USER-987654",
    "ssoProvider": "TUBOLETA",
    "email": "fan@tuboleta.com",
    "clientType": "USER_APP",
    "arenaId": 1,
    "nombres": "Andrea",
    "apellidos": "Martínez"
  }'
```

---

### 4. Validar Token JWT (`/validate`)
Permite al Gateway o a otros microservicios validar la firma, vigencia y extraer los claims de un token JWT.

- **Método:** `POST`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/validate`
- **URL Directa:** `http://localhost:8080/api/v1/auth/validate`
- **Autenticación:** No requerida (o token en body).

#### Request Body
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### Respuestas
- **`200 OK`**:
```json
{
  "success": true,
  "message": "Validación completada",
  "data": {
    "valid": true,
    "usuarioId": 15,
    "username": "carlosgomez",
    "roles": ["CLIENTE"],
    "scopes": ["READ", "WRITE"],
    "arenaId": 1
  },
  "timestamp": "2026-10-05T10:00:00"
}
```

#### cURL de Ejemplo
```bash
curl -X POST "http://localhost:8000/api/v1/auth/validate" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }'
```

---

### 5. Obtener Perfil del Usuario en Sesión (`/me`)
Retorna los datos completos de la persona y usuario autenticado mediante el token JWT.

- **Método:** `GET`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/me`
- **URL Directa:** `http://localhost:8080/api/v1/auth/me`
- **Autenticación:** Requerida (`Bearer <token>`).
- **Headers:** `Authorization: Bearer <token>`

#### Respuestas
- **`200 OK`**:
```json
{
  "success": true,
  "message": "Perfil de usuario recuperado",
  "data": {
    "usuarioId": 15,
    "username": "carlosgomez",
    "ssoId": null,
    "ssoProvider": null,
    "tcposClientId": null,
    "estadoId": 1,
    "personaId": 12,
    "nombres": "Carlos",
    "apellidos": "Gómez",
    "tipoDocumentoId": 1,
    "numeroDocumento": "1012345678",
    "email": "carlos.gomez@correo.com",
    "telefono": "+573001234567",
    "paisId": 1,
    "departamentoId": 11,
    "ciudadId": 11001,
    "direccion": "Calle 100 # 15-20",
    "roles": ["CLIENTE"],
    "scopes": ["READ", "WRITE"],
    "arenaIds": [1],
    "creacionFecha": "2026-10-05T09:30:00"
  },
  "timestamp": "2026-10-05T10:00:00"
}
```
- **`401 Unauthorized`**: Token expirado, ausente o manipulado.

#### cURL de Ejemplo
```bash
curl -X GET "http://localhost:8000/api/v1/auth/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Accept: application/json"
```

---

### 6. Renovar Token JWT o Cambiar Contexto de Arena (`/refresh`)
Genera un nuevo token JWT válido. Permite además conmutar el `arenaId` activo en el token cuando el usuario cambia de sede.

- **Método:** `POST`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/refresh?arenaId={arenaId}`
- **URL Directa:** `http://localhost:8080/api/v1/auth/refresh?arenaId={arenaId}`
- **Autenticación:** Requerida (`Bearer <token>`).
- **Query Params:**
  - `arenaId` (opcional): ID numérico de la nueva arena a fijar en los claims.

#### Respuestas
- **`200 OK`**:
```json
{
  "success": true,
  "message": "Token renovado exitosamente",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  },
  "timestamp": "2026-10-05T10:00:00"
}
```

#### cURL de Ejemplo
```bash
curl -X POST "http://localhost:8000/api/v1/auth/refresh?arenaId=2" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json"
```

---

### 7. Asignar Rol y Arena a un Usuario (`/usuarios/asignar-arena`)
Permite a un administrador vincular un usuario a una arena específica con un rol operacional (`RUNNER`, `ADMIN_ARENA`). Si el usuario no existe previamente, se crea de forma desatendida.

- **Método:** `POST`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/usuarios/asignar-arena`
- **URL Directa:** `http://localhost:8080/api/v1/auth/usuarios/asignar-arena`
- **Autenticación:** Requerida (`SUPER_ADMIN` o `ADMIN_ARENA`).

#### Request Body
| Campo | Tipo | Obligatorio | Descripción | Ejemplo |
| :--- | :---: | :---: | :--- | :--- |
| `email` | `string` | Sí | Correo electrónico del usuario a asociar | `"runner01@beathub.com"` |
| `arenaId` | `integer` | Sí | ID de la arena a la que se vinculará | `1` |
| `rolId` | `integer` | Sí | `1`: SUPER_ADMIN, `2`: ADMIN_ARENA, `3`: RUNNER, `4`: CLIENTE | `3` |
| `nombres` | `string` | No | Nombres si el usuario se crea nuevo | `"Pedro"` |
| `apellidos` | `string` | No | Apellidos si el usuario se crea nuevo | `"Pérez"` |
| `tipoDocumentoId` | `integer` | No | ID del tipo de identificación | `1` |
| `numeroDocumento` | `string` | No | Número de cédula | `"80123456"` |
| `telefono` | `string` | No | Teléfono de contacto | `"+573150001122"` |

```json
{
  "email": "runner01@beathub.com",
  "arenaId": 1,
  "rolId": 3,
  "nombres": "Pedro",
  "apellidos": "Pérez",
  "tipoDocumentoId": 1,
  "numeroDocumento": "80123456",
  "telefono": "+573150001122"
}
```

#### Respuestas
- **`200 OK`**:
```json
{
  "success": true,
  "message": "Usuario asignado a la arena exitosamente",
  "data": {
    "usuarioId": 22,
    "username": "runner01",
    "email": "runner01@beathub.com",
    "roles": ["RUNNER"],
    "arenaIds": [1]
  },
  "timestamp": "2026-10-05T10:00:00"
}
```

#### cURL de Ejemplo
```bash
curl -X POST "http://localhost:8000/api/v1/auth/usuarios/asignar-arena" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "runner01@beathub.com",
    "arenaId": 1,
    "rolId": 3,
    "nombres": "Pedro",
    "apellidos": "Pérez",
    "numeroDocumento": "80123456"
  }'
```

---

### 8. Consultar Usuario por ID (`/usuarios/{id}`)
- **Método:** `GET`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/usuarios/{id}`
- **URL Directa:** `http://localhost:8080/api/v1/auth/usuarios/{id}`
- **Autenticación:** Requerida (`SUPER_ADMIN`, `ADMIN_ARENA` o ID coincidente con el usuario autenticado).

#### cURL de Ejemplo
```bash
curl -X GET "http://localhost:8000/api/v1/auth/usuarios/15" \
  -H "Authorization: Bearer <TOKEN>"
```

---

### 9. Consultar Usuario por Username (`/usuarios/username/{username}`)
- **Método:** `GET`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/usuarios/username/{username}`
- **URL Directa:** `http://localhost:8080/api/v1/auth/usuarios/username/{username}`
- **Autenticación:** Requerida (`SUPER_ADMIN`, `ADMIN_ARENA` o username coincidente).

#### cURL de Ejemplo
```bash
curl -X GET "http://localhost:8000/api/v1/auth/usuarios/username/carlosgomez" \
  -H "Authorization: Bearer <TOKEN>"
```

---

### 10. Health Check del Servicio (`/status`)
- **Método:** `GET`
- **URL Gateway:** `http://localhost:8000/api/v1/auth/status`
- **URL Directa:** `http://localhost:8080/api/v1/auth/status`
- **Autenticación:** No requerida.

#### Respuesta `200 OK`
```json
{
  "success": true,
  "message": "Microservicio Auth operativo",
  "data": {
    "service": "beathub-auth",
    "version": "1.0.0",
    "status": "UP",
    "timestamp": "2026-10-05T10:00:00"
  },
  "timestamp": "2026-10-05T10:00:00"
}
```
