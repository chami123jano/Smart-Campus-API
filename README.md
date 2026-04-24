# Smart Campus Sensor & Room Management API
**Module:** 5COSC022W Client-Server Architectures  
**Student:** C. J. Ambawatta  
**IIT ID:** 20240170
**UOW ID:** w2153375

## Overview
This is a high-performance RESTful web service built with JAX-RS (Jakarta RESTful Web Services) to manage a university's Smart Campus infrastructure. The API handles the management of Rooms and diverse Sensors deployed across the campus, providing a robust interface for facilities managers and automated building systems.

## Project Setup & Launch Instructions

### Prerequisites
* Java 11 or higher
* Maven

### How to Build and Run
1. Open a terminal in the project root directory (where the `pom.xml` is located).
2. Clean and build the project using Maven:
   ```bash
   mvn clean install
   ```
3. Run the application using the `exec:java` Maven plugin or by running the `Main` class:
   ```bash
   mvn exec:java -Dexec.mainClass="com.smartcampus.Main"
   ```
4. The API will start on: `http://localhost:8080/api/v1/`

---

## Conceptual Report

### Part 1: Service Architecture & Setup

**Question 1.1: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.**

**Answer:**
By default, JAX-RS resource classes are **request-scoped**, meaning the JAX-RS runtime instantiates a brand new instance of the resource class for every single incoming HTTP request. Once the response is sent back to the client, that instance is destroyed and garbage-collected. 

Because of this architectural design, we cannot safely store state data (such as lists or maps of Rooms and Sensors) as regular instance variables inside our Resource classes. If we did, the data would reset to empty on every request, causing severe data loss. To prevent this, the in-memory data structures must be declared as `static` (or managed as an injected singleton). 
Furthermore, since a web server handles multiple HTTP requests simultaneously across different threads, these shared static data structures must be thread-safe. This is why we must use synchronized collections like `ConcurrentHashMap` rather than a standard `HashMap`. `ConcurrentHashMap` handles concurrent read and write operations without explicitly locking the entire structure, preventing race conditions and potential `ConcurrentModificationException` crashes under heavy load.

**Question 1.2: Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?**

**Answer:**
Providing Hypermedia (HATEOAS - Hypermedia as the Engine of Application State) is the highest level (Level 3) of the Richardson Maturity Model for REST APIs. It ensures the API is "self-discoverable." Instead of merely returning flat data, the server provides actionable links (hypermedia) within the JSON response to indicate what actions or linked resources are currently available based on the state of the entity.

This approach significantly benefits client developers because it heavily **decouples** the client application from the server's routing rules. Instead of hardcoding all the API endpoint URLs based on static, potentially outdated documentation, the client code can dynamically read the `_links` object in the response to find the correct URL for its next action. If the backend team needs to change the URL structure (e.g., from `/api/v1/rooms` to `/api/v1/spaces`), the client won't break as long as it continues to follow the link mapped to the relational key (like `"rooms"`). It creates a highly resilient, flexible, and evolvable system.

---

### Part 2 & 3: Room Management & Basic Sensor Ops

**Question 2.1: Returning IDs vs full objects for lists. When designing `GET /rooms` or `GET /sensors`, what are the tradeoffs between returning a list of simple IDs versus full JSON objects?**

**Answer:**
Returning full objects requires more bandwidth and processing overhead (serialization/deserialization) but allows the client to retrieve everything in a single HTTP request natively reducing the required overall number of network operations. Conversely, returning only IDs keeps the initial API payload small and fast, saving server resources and bandwidth, but heavily penalizes clients who suddenly must iterate and submit hundreds of subsequent `GET /{id}` requests to fetch the actual details (also known as the N+1 problem). Usually, a balanced approach is best: returning a summary object containing basic identifiers alongside HATEOAS references.

**Question 2.2: Idempotency of your `DELETE` operation. Explain what it means for your `DELETE /rooms/{roomId}` endpoint to be idempotent.**

**Answer:**
Idempotency dictates that an HTTP method can be called once or multiple times consecutively with the exact same resulting state on the server. If a client successfully deletes `Room-A`, the resource is gone. If the client repeats the exact same `DELETE` request for `Room-A` due to a network glitch or a retry mechanism, the underlying server state remains identically unchanged (the room is still not there). Standard behavior usually returns a `404 Not Found` or `204 No Content` for the subsequent requests interchangeably, preserving the safety property of the method.

**Question 2.3: Consequences of mismatched MediaTypes. What happens if a client submits an XML payload to an endpoint explicitly annotated with `@Consumes(MediaType.APPLICATION_JSON)`?**

**Answer:**
The JAX-RS runtime will fundamentally intercept the incoming request during the content negotiation phase before it even reaches the internal Java method. Because the `Content-Type` header (e.g., `application/xml`) explicitly conflicts with the `@Consumes` definition, the web framework inherently rejects it and generates an HTTP **`415 Unsupported Media Type`** response gracefully.

**Question 2.4: `@QueryParam` vs Path variables for filtering. Why use `@QueryParam` (e.g., `/sensors?type=temperature`) instead of a Path parameter (e.g., `/sensors/temperature`) for optional filters?**

**Answer:**
Path parameters (`@PathParam`) structurally identify specific, unique hierarchal resources inside the API route tree (e.g., `/rooms/101`). Conversely, query parameters (`@QueryParam`) act as operational modifiers or optional filters to refine a broader collection natively (e.g., filtering all sensors to limit the output purely to 'temperature' types). If filtering uses path segments casually, the URL tree expands infinitely and implies subset nesting instead of optional narrowing. Query parameters logically assert optionality, sorting, and constraints dynamically.

---

### Part 4: Deep Nesting & Sub-Resources

**Question 4.1: Architectural benefits of the Sub-Resource Locator pattern. What exact architectural problems are alleviated by introducing it?**

**Answer:**
Routing deeply nested paths inherently bloats standard JAX-RS controller boundaries. The overriding benefit of injecting a dynamically instantiated "Sub-Resource Locator" (`@Path("/{sensorId}")` acting essentially as a gateway returning a `new SensorReadingResource(sensorId)` snippet) structurally solves the Single Responsibility Principle violation.
By leveraging this paradigm, we extract the entire logical domain mapping, CRUD execution, and validation algorithms associated specifically into its independent component (a detached subclass). This fundamentally prevents monolithic God-class creation within the parent API file. Consequently, it logically cascades `PathParam` context variables downwards safely (the sub-resource inherently grasps which `sensorId` it handles upon inception), preventing immense amounts of duplicated boilerplate signature rewriting, streamlining readability, extending encapsulation optimally, and maintaining long-term maintainability for intricate microservice architectures significantly better. 

---

### Part 5: Error Handling, Logging & Documentation

**Question 5.1: HTTP 422 vs 404 for payload dependencies. When registering a Sensor with a `roomId` that doesn't exist, why is HTTP `422 Unprocessable Entity` sometimes preferred over `404 Not Found`?**

**Answer:**
A `404 Not Found` usually applies to the main target URI. If a client sends a `POST` request to `/sensors`, the endpoint itself exists. The failure isn't that the URI is missing; rather, the data payload refers to a secondary relational entity (`roomId`) that doesn't exist. Thus, `422 Unprocessable Entity` is Semantically better—it means "The server understands your payload format, but cannot process the semantic meaning because the logical relational data (the room) is invalid."

**Question 5.2: Cybersecurity risks of exposing Java stack traces. Why is returning an unhandled Java stack trace to the end-user dangerous?**

**Answer:**
Stack traces are fundamentally internal debugging blueprints. If returned to an unauthenticated external client, they securely leak internal backend architecture insights: the specific frameworks used (e.g., Tomcat, Jersey, SQL mapping), library versions, component file paths, and exact lines of code where faults occurred. This constitutes an "Information Disclosure" vulnerability, granting malicious actors the exact topological blueprint needed to exploit library-specific vulnerabilities dynamically. Globally trapping exceptions with a `GenericExceptionMapper` mapping into a clean `500 Internal Server Error` JSON message obscures the system entirely from the client while safely logging the exact error into an internal log file for administrators.

**Question 5.3: Advantages of using JAX-RS Filters over manual logging statements.**

**Answer:**
JAX-RS `<ContainerRequestFilter>` acts generically at the very edge of the service boundary layer. If we inserted manual `System.out.println("Request arrived")` inside every controller method, our code would be heavily duplicated (violating DRY) and tightly coupled. The JAX-RS Filter intercepts *all* traffic generically before reaching our controllers and logs HTTP methods, status codes, and URI metadata globally natively! This inherently guarantees robust observability without polluting business core logic with logging boilerplates.

## Example Usage: Top 5 cURL Commands

**1. Root Discovery URL:**
```bash
curl -X GET "http://localhost:8080/api/v1/"
```

**2. Create a Room:**
```bash
curl -X POST "http://localhost:8080/api/v1/rooms" -H "Content-Type: application/json" -d '{"name": "Seminar Room", "capacity": 100}'
```

**3. Register a Sensor:**
```bash
curl -X POST "http://localhost:8080/api/v1/sensors" -H "Content-Type: application/json" -d '{"type": "temperature", "status": "ACTIVE", "roomId": "LIB-301"}'
```

**4. Post a Sensor Reading:**
```bash
curl -X POST "http://localhost:8080/api/v1/sensors/<SENSOR_ID_HERE>/readings" -H "Content-Type: application/json" -d '{"value": 24.5}'
```

**5. Test 409 Conflict (Try to delete a Room that has active Sensors):**
```bash
curl -X DELETE "http://localhost:8080/api/v1/rooms/LIB-301"
```

