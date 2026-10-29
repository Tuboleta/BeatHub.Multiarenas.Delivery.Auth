# Microservicio de Autenticación y Autorización (Auth Service) - BeatHub Multiarenas

Microservicio del ecosistema **BeatHub Multiarenas** desarrollado en **Spring Boot 4.1.1 (Java 25 LTS)** encargado de la autenticación, emisión de tokens JWT, registro de usuarios y personas, roles, asignación multi-arena y scopes.

---

## 🏛️ Estructura del Proyecto

- **Schema de Base de Datos**: `auth`
- **Trazabilidad Homogénea**: `creacion_usuario` (Long), `creacion_fecha`, `actualizacion_usuario` (Long), `actualizacion_fecha`, `estado_id` (Integer), `arena_id`.
- **Flyway**: `src/main/resources/db/migration/` (incluye `V8__add_barra_role_and_shop_support.sql` con rol `ROLE_BARRA` y columnas `shop_id`, `shop_nombre` en `arena_usuario`).
- **Roles del Sistema**:
  - `ROLE_SUPER_ADMIN` (ID 1)
  - `ROLE_ADMIN_ARENA` (ID 2)
  - `ROLE_RUNNER` (ID 3)
  - `ROLE_CLIENTE` (ID 4)
  - `ROLE_BACKOFFICE` (ID 5)
  - `ROLE_BARRA` (ID 6): Personal de despacho en barras/mostradores físicos Take-away.

---

## 🌐 Endpoints Principales

| Recurso | Método | URL Downstream (8080) | URL Gateway (8000) | Roles Permitidos | Descripción |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Swagger UI** | `GET` | `http://localhost:8080/swagger-ui.html` | `http://localhost:8000/api/v1/auth/swagger-ui.html` | Público | Documentación interactiva OpenAPI |
| **Status** | `GET` | `http://localhost:8080/status` | `http://localhost:8000/api/v1/auth/status` | Público | Verificación de salud |
| **Login BackOffice** | `POST` | `http://localhost:8080/backoffice/login` | `http://localhost:8000/api/v1/auth/backoffice/login` | Público | Autenticación con usuario y contraseña |
| **Login SSO Azure** | `POST` | `http://localhost:8080/sso/login` | `http://localhost:8000/api/v1/auth/sso/login` | Público | Autenticación con token Azure AD / TuBoleta |
| **Validar Token** | `POST` | `http://localhost:8080/validate` | `http://localhost:8000/api/v1/auth/validate` | Público | Introspección y validación de claims JWT |
| **Perfil Actual** | `GET` | `http://localhost:8080/me` | `http://localhost:8000/api/v1/auth/me` | Autenticado | Obtener datos del usuario autenticado (incluye `shopId`, `shopNombre`) |
| **Listar Usuarios** | `GET` | `http://localhost:8080/usuarios` | `http://localhost:8000/api/v1/auth/usuarios` | `SUPER_ADMIN`, `ADMIN_ARENA`, `BACKOFFICE` | Consulta paginada con filtros (`arenaId`, `shopId`, `rol`, `search`, `estadoId`) |
| **Consultar Usuario ID** | `GET` | `http://localhost:8080/usuarios/{id}` | `http://localhost:8000/api/v1/auth/usuarios/{id}` | Admin o Mismo Usuario | Consulta por ID |
| **Consultar por Username**| `GET` | `http://localhost:8080/usuarios/username/{username}` | `http://localhost:8000/api/v1/auth/usuarios/username/{username}` | Admin o Mismo Usuario | Consulta por nombre de usuario |
| **Asignar Rol / Arena** | `POST` | `http://localhost:8080/usuarios/asignar-arena` | `http://localhost:8000/api/v1/auth/usuarios/asignar-arena` | `SUPER_ADMIN`, `ADMIN_ARENA`, `BACKOFFICE` | Dar de alta Runner, Admin de Arena o Barra |
| **Asignar/Desvincular Shop** | `PUT` | `http://localhost:8080/usuarios/{id}/arenas/{arenaId}/shop` | `http://localhost:8000/api/v1/auth/usuarios/{id}/arenas/{arenaId}/shop` | `SUPER_ADMIN`, `ADMIN_ARENA`, `BACKOFFICE` | Asignar barra (`shopId`), rotar o desvincular (`shopId: null`) |

---

## 🚀 Despliegue con Docker

```bash
docker compose up --build -d
```
