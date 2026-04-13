# catalog-service

Microservicio de catálogo musical de **RUBY MUSIC**. Gestiona géneros/estaciones, artistas, álbumes y canciones. Los contadores de reproducciones, likes y seguidores se actualizan de forma **asíncrona vía Kafka**, no inline con las peticiones.

---

## Responsabilidad

- CRUD completo de géneros, artistas, álbumes y canciones
- Búsqueda full-text de canciones (título + nombre de artista) y artistas
- Listas paginadas: nuevos lanzamientos, top álbumes por streams, top artistas
- Consume eventos Kafka para actualizar contadores cacheados (`play_count`, `likes_count`, `followers_count`, `monthly_listeners`)

---

## Stack

| Componente | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 3.2.5 |
| Spring Cloud | 2023.0.1 |
| Spring Data JPA | — |
| Spring Kafka | — |
| MapStruct | 1.5.5 |
| Lombok | — |
| SpringDoc OpenAPI | 2.5.0 |
| OpenAPI Generator (Maven plugin) | 7.4.0 |

---

## Puerto

| Servicio | Puerto |
|---|---|
| catalog-service | **8082** |
| Acceso vía gateway | `http://localhost:8080/api/v1/catalog` |

---

## Base de datos

| Parámetro | Valor |
|---|---|
| Engine | PostgreSQL |
| Database | `catalog_db` |
| Host | `localhost:5432` |
| DDL | `update` (Hibernate auto-schema) |

### Entidades

| Tabla | Descripción |
|---|---|
| `genres` | Géneros/estaciones con gradientes de color |
| `artists` | Artistas con contadores cacheados |
| `albums` | Álbumes con fecha de lanzamiento y streams |
| `songs` | Canciones con audio/cover URL, lyrics y contadores |
| `song_genres` | Tabla intermedia N:M canción ↔ género |

---

## Endpoints

Las interfaces de controller se generan desde `src/main/resources/openapi.yml` vía el plugin Maven `openapi-generator-maven-plugin`.

### Géneros / Estaciones

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/genres` | Listar todos los géneros |
| `POST` | `/genres` | Crear género (admin) |
| `GET` | `/genres/{id}` | Obtener género por ID |
| `PUT` | `/genres/{id}` | Actualizar género (admin) |
| `DELETE` | `/genres/{id}` | Eliminar género (admin) |
| `GET` | `/genres/{id}/songs` | Canciones de un género (paginado) |

### Artistas

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/artists` | Listar artistas (paginado) |
| `POST` | `/artists` | Crear artista (admin) |
| `GET` | `/artists/top` | Top artistas (`is_top = true`, orden por `monthly_listeners`) |
| `GET` | `/artists/search` | Buscar artistas por nombre (paginado) |
| `GET` | `/artists/{id}` | Obtener artista por ID |
| `PUT` | `/artists/{id}` | Actualizar artista (admin) |
| `DELETE` | `/artists/{id}` | Eliminar artista (admin) |
| `GET` | `/artists/{id}/albums` | Álbumes de un artista (paginado) |
| `GET` | `/artists/{id}/songs` | Top canciones de un artista (paginado) |

### Álbumes

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/albums` | Listar álbumes (paginado) |
| `POST` | `/albums` | Crear álbum (admin) |
| `GET` | `/albums/new-releases` | Nuevos lanzamientos (`release_date DESC`, paginado) |
| `GET` | `/albums/top` | Top álbumes (`total_streams DESC`, paginado) |
| `GET` | `/albums/{id}` | Obtener álbum por ID |
| `PUT` | `/albums/{id}` | Actualizar álbum (admin) |
| `DELETE` | `/albums/{id}` | Eliminar álbum (admin) |
| `GET` | `/albums/{id}/songs` | Canciones de un álbum (paginado) |

### Canciones

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/songs/search` | Búsqueda full-text por título o artista (paginado) |
| `GET` | `/songs/{id}` | Obtener canción por ID |
| `POST` | `/songs` | Crear canción (admin) |
| `PUT` | `/songs/{id}` | Actualizar canción (admin) |
| `DELETE` | `/songs/{id}` | Eliminar canción (admin) |

---

## Kafka — Consumidor

El `CatalogEventConsumer` escucha eventos de otros servicios y actualiza los contadores cacheados con queries `@Modifying` directas (sin cargar la entidad completa).

| Topic | Emisor | Acción en catalog-service |
|---|---|---|
| `song.played` | interaction-service | `song.play_count + 1` |
| `song.liked` | interaction-service | `song.likes_count + 1` |
| `song.unliked` | interaction-service | `song.likes_count - 1` (mínimo 0) |
| `artist.followed` | social-service | `artist.followers_count + 1` |
| `artist.unfollowed` | social-service | `artist.followers_count - 1` (mínimo 0) |

> **Formato del mensaje:** UUID en texto plano (ID del recurso afectado).
> Los contadores nunca se actualizan inline con la petición — son **eventualmente consistentes**.

---

## Contadores cacheados

| Campo | Entidad | Actualizado por |
|---|---|---|
| `play_count` | `Song` | `song.played` Kafka event |
| `likes_count` | `Song` | `song.liked` / `song.unliked` Kafka events |
| `followers_count` | `Artist` | `artist.followed` / `artist.unfollowed` Kafka events |
| `monthly_listeners` | `Artist` | `song.played` Kafka event (vía lógica en interaction-service) |
| `total_streams` | `Album` | Derivado de `song.played` |

---

## Reglas de negocio

- **Géneros únicos:** No se permiten nombres duplicados (case-insensitive)
- **Canción sin álbum:** `album_id` es nullable — soporta singles
- **Géneros en canción:** Al crear/actualizar una canción, todos los `genre_id` deben existir; se rechaza si alguno es inválido
- **Archivos binarios:** `audio_url`, `cover_url` y `photo_url` nunca se almacenan en DB — solo la URL a cloud storage
- **Decrementos seguros:** Los queries de decremento usan `GREATEST(counter - 1, 0)` para evitar negativos

---

## Estructura del proyecto

```
catalog-service/
├── src/
│   ├── main/
│   │   ├── java/com/rubymusic/catalog/
│   │   │   ├── CatalogServiceApplication.java
│   │   │   ├── config/                        ← (reservado, .gitkeep)
│   │   │   ├── controller/
│   │   │   │   ├── GenresController.java       ← implements GenresApi
│   │   │   │   ├── ArtistsController.java      ← implements ArtistsApi
│   │   │   │   ├── AlbumsController.java       ← implements AlbumsApi
│   │   │   │   └── SongsController.java        ← implements SongsApi
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── kafka/
│   │   │   │   └── CatalogEventConsumer.java   ← Kafka listener para contadores
│   │   │   ├── mapper/
│   │   │   │   ├── GenreMapper.java
│   │   │   │   ├── ArtistMapper.java
│   │   │   │   ├── AlbumMapper.java            ← uses ArtistMapper
│   │   │   │   └── SongMapper.java             ← uses ArtistMapper, AlbumMapper, GenreMapper
│   │   │   ├── model/
│   │   │   │   ├── Genre.java
│   │   │   │   ├── Artist.java
│   │   │   │   ├── Album.java
│   │   │   │   └── Song.java
│   │   │   ├── repository/
│   │   │   │   ├── GenreRepository.java
│   │   │   │   ├── ArtistRepository.java
│   │   │   │   ├── AlbumRepository.java
│   │   │   │   └── SongRepository.java
│   │   │   └── service/
│   │   │       ├── GenreService.java
│   │   │       ├── ArtistService.java
│   │   │       ├── AlbumService.java
│   │   │       ├── SongService.java
│   │   │       └── impl/
│   │   │           ├── GenreServiceImpl.java
│   │   │           ├── ArtistServiceImpl.java
│   │   │           ├── AlbumServiceImpl.java
│   │   │           └── SongServiceImpl.java
│   │   └── resources/
│   │       ├── application.yml                 ← nombre + import config-server
│   │       └── openapi.yml                     ← contrato OpenAPI 3.0.3 completo
│   └── test/
│       └── java/com/rubymusic/catalog/
│           └── CatalogServiceApplicationTests.java
└── pom.xml
```

### Clases generadas por OpenAPI Generator

El plugin Maven genera en `target/generated-sources/`:
- Interfaces: `GenresApi`, `ArtistsApi`, `AlbumsApi`, `SongsApi`
- DTOs de request/response: `GenreRequest`, `ArtistResponse`, `SongCreateRequest`, `AlbumPage`, etc.

---

## Manejo de errores

| Excepción | HTTP |
|---|---|
| `NoSuchElementException` | `404 Not Found` |
| `IllegalArgumentException` | `400 Bad Request` |
| `DataIntegrityViolationException` | `409 Conflict` |
| `MethodArgumentNotValidException` | `422 Unprocessable Entity` |
| `Exception` (genérico) | `500 Internal Server Error` |

---

## Variables de entorno

Inyectadas desde `config-server` (`config/catalog-service.yml`):

| Variable | Descripción | Default |
|---|---|---|
| `DB_USERNAME` | Usuario PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña PostgreSQL | `password` |

---

## Build & Run

```bash
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Test
mvn test -Dtest=CatalogServiceApplicationTests
```

> Requiere `discovery-service`, `config-server`, PostgreSQL en `localhost:5432` con `catalog_db` creada, y Kafka en `localhost:9092`.
