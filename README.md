# Backend Análisis - Spring Boot 3.4.3 (Java 21)

Proyecto base configurado con Maven y arquitectura limpia por capas (Layered Architecture), 100% optimizado para Java 21 LTS y listo para codificar.

---

## 🏛️ Arquitectura por Capas

El proyecto está organizado bajo el paquete base `com.analisis.backend`:

```text
src/main/java/com/analisis/backend/
├── config/                # Configuraciones globales (CORS, seguridad, beans generales)
│   └── CorsConfig.java
├── controller/            # Capa de presentación (REST Controllers, exposición de endpoints HTTP)
│   └── UserController.java
├── service/               # Capa de lógica de negocio (interfaces y contratos)
│   ├── UserService.java
│   └── impl/              # Implementación concreta de las reglas de negocio
│       └── UserServiceImpl.java
├── repository/            # Capa de acceso a datos (Spring Data JPA)
│   └── UserRepository.java
├── model/                 # Capa de dominio y persistencia
│   ├── entity/            # Entidades JPA mapeadas a la base de datos
│   │   ├── BaseEntity.java (auditoría: createdAt, updatedAt con @PrePersist/@PreUpdate)
│   │   └── User.java
│   └── enums/             # Enumeraciones del dominio
│       └── UserStatus.java
├── dto/                   # Data Transfer Objects (desacoplamiento de las entidades)
│   ├── request/           # DTOs de entrada con anotaciones de validación Jakarta
│   │   └── UserRequestDto.java
│   └── response/          # DTOs de salida y envoltura estándar de respuesta
│       ├── ApiResponse.java (estructura uniforme: success, message, data, timestamp)
│       └── UserResponseDto.java
├── mapper/                # Capa de transformación entre Entidades y DTOs
│   └── UserMapper.java
├── exception/             # Manejo global y centralizado de excepciones
│   ├── BadRequestException.java
│   ├── ResourceNotFoundException.java
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java (@RestControllerAdvice)
└── BackendAnalisisApplication.java # Clase principal de inicio de Spring Boot
```

---

## 🚀 Tecnologías y Dependencias

- **Java**: 21 (LTS)
- **Framework**: Spring Boot (Maven)
- **Persistencia**: Spring Data JPA + Hibernate
- **Base de Datos Principal**: PostgreSQL (driver en runtime)
- **Base de Datos de Pruebas**: H2 in-memory (para ejecutar tests sin requerir base de datos externa)
- **Validaciones**: Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, etc.)
- **Utilidades**: Project Lombok (getters, setters, builders, constructores)
- **Control de Acceso**: Configuración CORS preparada para frontend (`http://localhost:*`)

---

## ⚙️ Configuración de Base de Datos

El archivo de configuración principal se encuentra en [`src/main/resources/application.yml`](src/main/resources/application.yml).

Soporta variables de entorno del sistema con valores por defecto para desarrollo local:

| Variable de Entorno | Valor por Defecto | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/analisis_db` | URL JDBC de conexión |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuario de PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Contraseña de PostgreSQL |
| `PORT` | `8080` | Puerto del servidor web |

---

## 📡 Endpoints de Ejemplo Listos para Probar

El módulo `User` viene completamente implementado como referencia de la arquitectura:

| Método | Endpoint | Descripción | Body / Parámetros |
|---|---|---|---|
| `POST` | `/api/v1/users` | Registrar nuevo usuario | JSON: `firstName`, `lastName`, `email` |
| `GET` | `/api/v1/users` | Listar todos los usuarios | Ninguno |
| `GET` | `/api/v1/users/{id}` | Obtener usuario por ID | ID numérico |
| `GET` | `/api/v1/users/page` | Listado paginado | Query params: `?page=0&size=10&sort=id,asc` |
| `PUT` | `/api/v1/users/{id}` | Actualizar datos de usuario | ID numérico + JSON con nuevos datos |
| `DELETE` | `/api/v1/users/{id}` | Eliminar usuario | ID numérico |

### Formato de Respuesta Estándar (`ApiResponse<T>`):
```json
{
  "success": true,
  "message": "Usuario creado exitosamente",
  "data": {
    "id": 1,
    "firstName": "Juan",
    "lastName": "Pérez",
    "email": "juan.perez@example.com",
    "status": "ACTIVE",
    "createdAt": "2026-10-17T11:30:00",
    "updatedAt": "2026-10-17T11:30:00"
  },
  "timestamp": "2026-10-17T11:30:00"
}
```

---

## 🛠️ Comandos de Ejecución

El proyecto incluye el Maven Wrapper (`mvnw`), por lo que **no requieres tener Maven instalado globalmente**:

### Compilar el proyecto:
```bash
./mvnw.cmd compile
```

### Ejecutar pruebas automáticas:
```bash
./mvnw.cmd test
```

### Levantar la aplicación:
```bash
./mvnw.cmd spring-boot:run
```

### Empaquetar el JAR para despliegue:
```bash
./mvnw.cmd clean package -DskipTests
```