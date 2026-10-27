# Product Service

Spring Boot microservice that manages products (CRUD). Data is stored in MongoDB. Images are uploaded through the Media Service, and only their URLs are stored here.

## Endpoints

| Method | Path               | Access         | Description                                   |
| ------ | ------------------ | -------------- | --------------------------------------------- |
| GET    | `/products`        | Public         | List products (paged: `page`, `size`, `sort`) |
| GET    | `/products/{id}`   | Public         | Get one product                               |
| GET    | `/products/me`     | SELLER         | List my products                              |
| POST   | `/products`        | SELLER         | Create a product                              |
| PUT    | `/products/{id}`   | SELLER (owner) | Update a product                              |
| DELETE | `/products/{id}`   | SELLER (owner) | Delete a product                              |
| GET    | `/actuator/health` | Public         | Health check                                  |

### Product body (POST / PUT)

```json
{
  "name": "Shoes",
  "description": "Running shoes",
  "price": 49.9,
  "quantity": 3,
  "imageUrls": ["/media/images/abc"]
}
```

Rules: `name` required (max 100), `price` > 0, `quantity` >= 0, up to 10 `imageUrls`.

### Status codes

`200` OK, `201` created, `204` deleted, `400` invalid input, `401` not authenticated, `403` wrong role, `404` not found or not owned.

## Security

- The **gateway** validates the JWT. This service does not check it again.
- The gateway forwards the identity in two headers: `X-User-Id` and `X-User-Role`.
- The owner (`userId`) of a product is always taken from `X-User-Id`, never from the request body.
- Only the owner can update or delete a product.
- The service must **not be reachable directly**. In Docker Compose, only the gateway is exposed to the outside.

## Configuration

| Variable                | Description               | Default                         |
| ----------------------- | ------------------------- | ------------------------------- |
| `PRODUCT_MONGO_URI`     | MongoDB connection string | required                        |
| `PRODUCT_SERVICE_PORT`  | Server port               | `8082`                          |
| `PRODUCT_SERVICE_NAME`  | Eureka service name       | `product-service`               |
| `EUREKA_URL`            | Discovery server URL      | `http://localhost:8761/eureka/` |
| `AUTH_USER_ID_HEADER`   | Header with the user id   | `X-User-Id`                     |
| `AUTH_USER_ROLE_HEADER` | Header with the user role | `X-User-Role`                   |

Copy `.env.example` to `.env` and set the values before running.

## Run

```bash
docker compose up -d --build product-service
```

Check it: `curl localhost:8080/actuator/health`

Local development (MongoDB and Eureka in Docker, service from the terminal):

```bash
docker compose up -d product-mongo discovery-service
set -a; source .env; set +a
./gradlew bootRun
```
