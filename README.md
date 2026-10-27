# buy-01

An e-commerce marketplace built with **Spring Boot microservices** and an **Angular** frontend, fronted by a **Caddy** reverse proxy with HTTPS.
Users register as **clients** (browse products) or **sellers** (manage their own products and images).

## Architecture

```txt
                 Angular SPA (HTTPS via Caddy)
                              |
                              | https://yourdomain.com/api/
                              v
                       +-------------+
                       |    Caddy    | (HTTPS termination, /api/ prefix routing)
                       +-------------+
                              |
                              v
                       +--------------+        +-------------------+
                       | api-gateway  |------->| discovery-service |  (Eureka)
                       +--------------+        +-------------------+
                        |      |      |
               +--------+      |      +---------+
               v               v                v
        user-service   product-service   media-service ---> media-storage (S3-compatible)
               |               |                |
            user-db       product-mongo     media-mongo
```

| Service             | Role                                                           | Data                                                       |
| ------------------- | -------------------------------------------------------------- | ---------------------------------------------------------- |
| `caddy`             | HTTPS termination, static asset serving, and reverse proxy     | none                                                       |
| `api-gateway`       | Single entry point: routing, CORS, JWT validation              | none                                                       |
| `discovery-service` | Eureka service registry                                        | none                                                       |
| `user-service`      | Registration, login (JWT), profiles, roles `CLIENT` / `SELLER` | MongoDB (`user-db`)                                        |
| `product-service`   | Product catalog (CRUD, ownership)                              | MongoDB (`product-mongo`)                                  |
| `media-service`     | Image upload and download (2 MB limit, content checks)         | MongoDB (`media-mongo`) + object storage (`media-storage`) |

**Tech stack:** Java 21, Spring Boot 4, Spring Security, Spring Cloud (Gateway, Eureka), MongoDB 7,
S3-compatible object storage (`pgsty/minio`), Docker Compose, Angular, Caddy (HTTPS).

## Security model

- **Caddy** terminates TLS/HTTPS for the domain and securely proxies paths starting with `/api/` to the backend gateway.
- The **gateway** validates the JWT. The downstream services do not check it again.
- The gateway forwards the identity in two headers, `X-User-Id` and `X-User-Role`
  (names are configurable with `AUTH_USER_ID_HEADER` / `AUTH_USER_ROLE_HEADER`).
  It must remove any such header sent by the client before setting its own.
- Product and Media trust those headers, so **they are never published to the outside**.
- Writes need the `SELLER` role. Only the owner of a product or image can modify or delete it
  (reported as `404` for anyone else).
- Passwords are hashed with BCrypt in the User Service. <!-- TODO: confirm with the user-service owner -->

## API documentation

The OpenAPI documentation of each service is published here:

| Service         | Documentation                                                                                           |
| --------------- | ------------------------------------------------------------------------------------------------------- |
| User service    | <https://registry.scalar.com/@default-team-wyzcr/apis/openapi-definition@v0#description/introduction>   |
| Product service | <https://registry.scalar.com/@default-team-wyzcr/apis/openapi-definition-2@v1#description/introduction> |
| Media service   | <https://registry.scalar.com/@default-team-wyzcr/apis/openapi-definition-3@v0#description/introduction> |

Each service also serves its own spec at `/v3/api-docs` (and Swagger UI at `/swagger-ui.html`)
when its dev port is open (see *Development ports*). Re-publish the registry copy whenever an endpoint changes.

### Endpoints at a glance

All external API calls go through Caddy via HTTPS at `https://yourdomain.com/api/` (which strips or forwards the prefix to the gateway at `http://api-gateway:8080`).

| Service | Method and path                                              | Access                                    |
| ------- | ------------------------------------------------------------ | ----------------------------------------- |
| User    | `POST /api/auth/register`, `POST /api/auth/login`            | public                                    |
| User    | `GET /api/profile/me`, `PUT /api/profile/me`                 | authenticated <!-- TODO: confirm path --> |
| Product | `GET /api/products`, `GET /api/products/{id}`                | public                                    |
| Product | `GET /api/products/me`                                       | SELLER                                    |
| Product | `POST /api/products`, `PUT /api/products/{id}`, `DELETE ...` | SELLER (owner)                            |
| Media   | `GET /api/media/images/{id}`                                 | public                                    |
| Media   | `POST /api/media/images`                                     | SELLER                                    |
| Media   | `DELETE /api/media/images/{id}`                              | SELLER (owner)                            |

Product lists are paged (`page`, `size`, `sort`; `size` is capped at 100).

### How products and images work together

1. **Create the product:** `POST /api/products` returns its `id`.
2. **Upload images:** `POST /api/media/images` as `multipart/form-data` with up to **5** `files` (2 MB each)
   and the product's `productId`. Only JPEG, PNG, GIF and WebP are accepted, and the content is checked,
   not just the filename or declared type.
3. **Read the product:** `GET /api/products/{id}` includes `imageUrls`, filled by the Media Service.
   - `[]` means the product has no images.
   - `null` means the images could not be loaded right now (Media unavailable). The product itself is still returned.

Deleting a product also deletes its images. A file upload without a `productId` is used for avatars.

### Errors

Errors share one JSON shape:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "Product not found: abc", "fieldErrors": null }
```

| Status | Meaning                                              |
| ------ | ---------------------------------------------------- |
| 400    | Invalid input, bad or oversized file, malformed JSON |
| 401    | Not authenticated                                    |
| 403    | Authenticated, but the role is not allowed           |
| 404    | Not found, or not owned by you                       |
| 503    | A required service is unavailable                    |

## Run it

Requirements: Docker with Compose, and a configured Caddyfile pointing to your domain.

```bash
cp .env.example .env        # then set real passwords, secrets, and domain info
docker compose up -d --build
```

### Development ports

`compose.dev.yml` opens extra ports so you can reach each service directly:

```bash
docker compose -f compose.yml -f compose.dev.yml up -d --build
```

In this mode you can test the services without a JWT by sending the identity headers yourself (directly to the backend services if ports are exposed, or via Caddy path routing).

Never use direct debugging ports in a shared or production environment.

## Configuration

Copy `.env.example` to `.env`. It is not committed. Each service receives only the variables it needs.

| Variable                                       | Used by                       | Description                                |
| ---------------------------------------------- | ----------------------------- | ------------------------------------------ |
| `EUREKA_URL`                                   | all services                  | Discovery server URL                       |
| `AUTH_USER_ID_HEADER`, `AUTH_USER_ROLE_HEADER` | gateway, user, product, media | Identity header names                      |
| `secret_key`                                   | gateway, user                 | JWT signing secret                         |
| `API_GATEWAY_PORT`                             | gateway                       | Internal port                              |
| `DOMAIN_NAME`                                  | caddy                         | Domain name for automatic TLS/HTTPS        |
| `CORS_ALLOWED_ORIGINS`                         | gateway                       | Allowed frontend origin(s)                 |
| `USERDB_*`                                     | user                          | User database host, name, credentials      |
| `PRODUCT_MONGO_*`, `PRODUCT_MONGO_URI`         | product                       | Product database                           |
| `MEDIA_MONGO_*`, `MEDIA_MONGO_URI`             | media                         | Media database                             |
| `MEDIA_STORAGE_*`                              | media, media-storage          | Storage endpoint, credentials, bucket name |

## Project structure

```txt
buy-01/
  api-gateway/         Spring Cloud Gateway
  discovery-service/   Eureka server
  user-service/        users, auth, profiles
  product-service/     product catalog
  media-service/       image upload and storage
  caddy/               Caddyfile & reverse proxy configuration
  compose.yml          full stack with Caddy HTTPS proxy
  compose.dev.yml      extra dev ports
  .env.example         configuration template
```

## Known limits

- Images stay in storage if a product is deleted while the Media Service is down.
- A product can have any number of images in total (the limit is 5 per upload request).
- Rate limiting and Kafka events are not implemented (both optional in the subject).
- HTTPS is fully configured and handled via Caddy (automatic Let's Encrypt certificates).
