# Smart Campus — Sensor & Room Management API

A JAX-RS RESTful API built with Jersey 2.x and an embedded Grizzly HTTP server.
No database, no Spring Boot — pure JAX-RS as required.

---

## Project Structure

```
smart-campus/
|-- pom.xml
|-- src/main/java/com/smartcampus/
    |── Main.java                            -starts the embedded server
    |-- application/
    |   |-- SmartCampusApplication.java      - @ApplicationPath("/api/v1")
    |   |-- DataStore.java                   - singleton in-memory ConcurrentHashMap store
    |-- model/
    |   |-- Room.java
    |   |-- Sensor.java
    |   |-- SensorReading.java
    |-- resource/
    |   |-- DiscoveryResource.java           - GET /api/v1
    |   |-- RoomResource.java                - /api/v1/rooms
    |   |-- SensorResource.java              - /api/v1/sensors
    |   |-- SensorReadingResource.java       - sub-resource: /api/v1/sensors/{id}/readings
    |-- exception/
    |   |-- RoomNotEmptyException.java + Mapper        - HTTP 409
    |   |-- LinkedResourceNotFoundException.java + Mapper - HTTP 422
    |   |-- SensorUnavailableException.java + Mapper   - HTTP 403
    |   |-- GlobalExceptionMapper.java                 - HTTP 500
    |-- filter/
        |-- LoggingFilter.java               - logs every request and response
```

---

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Build
```bash
cd smart-campus
mvn clean package
```
This produces `target/smart-campus-api.jar` — a self-contained fat JAR.

### Run
```bash
java -jar target/smart-campus-api.jar
```

The server starts on **http://localhost:8080**. Press ENTER to stop it.

---

## Sample curl Commands

### 1. Discovery endpoint
```bash
curl http://localhost:8080/api/v1
```

### 2. Get all rooms
```bash
curl http://localhost:8080/api/v1/rooms
```

### 3. Create a new room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-01","name":"Main Hall","capacity":200}'
```

### 4. Filter sensors by type
```bash
curl "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 5. Post a reading to an ACTIVE sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 23.4}'
```

### 6. Attempt to delete a room with sensors (→ 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 7. Attempt reading on MAINTENANCE sensor (→ 403 Forbidden)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 5.0}'
```

### 8. Create sensor with non-existent roomId (→ 422 Unprocessable Entity)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"NEW-001","type":"Temperature","status":"ACTIVE","currentValue":0,"roomId":"FAKE-999"}'
```

---

## Report: Answers to Coursework Questions

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request scope). This means no state is shared between requests through instance variables on the resource class itself.

This has a direct impact on data management: if each resource held its own `HashMap`, every request would start with an empty map and data would be lost immediately. To solve this, the project uses a **Singleton `DataStore` class** — a single shared instance retrieved via `DataStore.getInstance()`. All resource instances access the same underlying data. Because multiple requests can arrive concurrently, the store uses `ConcurrentHashMap` instead of a plain `HashMap` to prevent race conditions and data corruption without needing manual `synchronized` blocks on every operation.

---

### Part 1.2 — HATEOAS and Hypermedia

HATEOAS (Hypermedia as the Engine of Application State) means embedding navigational links directly inside API responses, so clients can discover available actions without relying solely on external documentation.

For example, the discovery endpoint at `GET /api/v1` returns links such as `"rooms": "/api/v1/rooms"` and `"sensors": "/api/v1/sensors"` inside the response body. This benefits client developers because they do not need to hardcode URLs or memorise the API structure — the server tells them where to go next. It also means the API can evolve (e.g. change a path) without breaking well-written clients that follow links rather than hardcoding them. It is considered a hallmark of advanced REST design because it makes the API truly self-describing and discoverable at runtime.

---

### Part 2.1 — Returning IDs vs Full Objects in Lists

Returning only IDs in a list response is bandwidth-efficient, but it forces the client to make N additional requests to fetch details for each item — a classic "N+1 problem". For a campus with thousands of rooms this would be extremely inefficient.

Returning full room objects in a single response costs more bandwidth per call, but gives the client everything it needs in one round trip. For this use case (facilities managers viewing room dashboards), returning full objects is the better trade-off because the client almost always needs the full details anyway, and network latency from N+1 requests far outweighs the larger initial payload.

---

### Part 2.2 — Idempotency of DELETE

The DELETE operation **is idempotent in terms of server state** — after the first successful deletion, the room no longer exists, and any further DELETE calls on the same room ID leave the server state unchanged (it is still gone).

However, the HTTP response code does differ: the first call returns `200 OK`, and subsequent calls return `404 Not Found` because the resource no longer exists to delete. This is widely accepted REST practice. The key point is that idempotency refers to the **effect on server state**, not the response code — and the state (room is absent) is identical after the first and all subsequent calls.

---

### Part 3.1 — @Consumes and Media Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the POST endpoint only accepts requests with a `Content-Type: application/json` header. If a client sends data as `text/plain` or `application/xml`, JAX-RS will automatically reject the request **before the method body is even executed** and return an HTTP `415 Unsupported Media Type` response. The developer does not need to write any manual content-type checking — the framework handles it declaratively. This keeps resource methods clean and focused purely on business logic.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

Using `@QueryParam` for filtering (e.g. `GET /api/v1/sensors?type=CO2`) is superior to embedding the filter in the path (e.g. `/api/v1/sensors/type/CO2`) for several reasons:

- **Semantic correctness**: Path segments identify a specific resource. Query parameters express optional constraints on a collection. Filtering is a constraint, not a resource identifier.
- **Optionality**: With `@QueryParam`, the parameter is naturally optional — omitting it returns all sensors. A path-based approach requires a separate route for the unfiltered case.
- **Composability**: Multiple filters can be combined naturally (`?type=CO2&status=ACTIVE`), whereas path-based filtering becomes deeply nested and ugly.
- **Caching and REST conventions**: Filtering a collection with query parameters is a universally understood REST convention that tools like Postman, Swagger, and API gateways handle natively.

---

### Part 4.1 — Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates responsibility for a nested path to a dedicated class. In this project, `SensorResource` contains a locator method annotated with `@Path("/{sensorId}/readings")` that returns a `SensorReadingResource` instance — it carries no `@GET` or `@POST` itself. JAX-RS then routes the actual HTTP method to the appropriate method on the returned object.

The architectural benefit is **separation of concerns**. Without this pattern, all endpoints for rooms, sensors, and readings would need to live in one enormous class, making it hard to read, test, and maintain. By splitting into dedicated classes, each class has a single responsibility, is independently testable, and the codebase scales cleanly as more nested resources are added.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Reference

When a client POSTs a new sensor referencing a `roomId` that does not exist, the request URI (`/api/v1/sensors`) is perfectly valid — the 404 would be misleading because the sensors endpoint *was* found. The problem is not a missing endpoint but a **semantic error inside a valid payload**: the JSON body references a resource that does not exist.

HTTP 422 Unprocessable Entity is more accurate because it signals that the server understood the request format and parsed the JSON successfully, but the **content is logically invalid** — the referenced room does not exist. A 404 would suggest the URL itself was wrong, which is not the case. 422 gives clients much clearer information about exactly what went wrong and where to look to fix it.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces in API responses is a significant security risk for several reasons:

- **Technology fingerprinting**: An attacker can identify the exact framework, library versions, and Java version in use, allowing them to look up known CVEs for those specific versions.
- **Internal path disclosure**: Stack traces reveal the full package structure and class names of the application, giving attackers a map of the codebase.
- **Logic disclosure**: The trace shows the exact sequence of method calls that led to the error, revealing business logic and internal architecture that should remain private.
- **Targeted exploitation**: With class names, method names, and line numbers, an attacker can craft inputs specifically designed to trigger and exploit vulnerable code paths.

The `GlobalExceptionMapper` in this project prevents all of this by logging the full trace server-side (visible only to authorised developers) while returning only a generic `500 Internal Server Error` message to the client.

---

### Part 5.5 — Filters vs Manual Logging

Using a JAX-RS filter for cross-cutting concerns like logging is far superior to inserting `Logger.info()` calls into every resource method because:

- **DRY principle**: One filter class covers every endpoint automatically. Adding a new resource method requires zero additional logging code.
- **Consistency**: Manual logging in individual methods is easy to forget, leading to gaps in observability. A filter is applied universally with no exceptions.
- **Separation of concerns**: Resource methods focus purely on business logic. Infrastructure concerns like logging, authentication, and CORS belong in filters.
- **Maintainability**: If the log format needs to change, it changes in one place rather than across dozens of methods.
