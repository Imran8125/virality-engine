# Virality Engine

The Virality Engine is a stateless, high-performance Spring Boot microservice designed to handle intense concurrent environments while enforcing strict interaction limits and caching mechanisms utilizing Redis and PostgreSQL.

## Core Features
1. **Concurrency Protection**: The engine efficiently handles data persistence and guarantees constraints strictly without incurring the typical race condition behaviors or DB locking overheads.
2. **Notification Smart Batching**: Built-in scheduling and queueing to avoid notification storms from bots interacting with humans.
3. **Stateless Operations**: No data or interaction tracking state lives in Java's memory space, securing complete scalability. 

## Guaranteeing Thread Safety for Atomic Locks
To solve the **Race Conditions (The Spam Test)** specifically, we utilize Redis `INCR` atomic operations. This acts as a gateway lock before committing any potentially disruptive database action.
When 200 concurrent bot requests try to increment the `post:{id}:bot_count`:
- Instead of reading the value into Java memory `GET` and validating, which leads to Race Conditions, we perform a pure `INCR`.
- Redis handles the `INCR` operation atomically as a single threaded process natively. 
- Values returned from `1` to `100` are permitted to execute the DB insert operation. 
- Any thread receiving an `INCR` value `101` and above is immediately rejected with `429 Too Many Requests`.
This ensures that the Postgres database acts absolutely as the source of truth retaining exactly `100` comments, and no single race condition alters this limitation.

## Horizontal, Vertical, and Cooldown Caps
- **Vertical Caps (Depth)**: Simply checked by reading the parent comment.
- **Cooldown Cap**: Employs Redis `SETNX` (via `setIfAbsent`) with an explicit Time-To-Live (TTL) of 10 minutes. By atomically setting a key only if it does not exist, we cleanly assure that bots do not spam notifications onto the queue, blocking consecutive attempts. 

## Requirements
Java 21, Gradle, Docker & Docker-Compose.

## Getting Started
1. Run `docker-compose up -d` to get PostgreSQL and Redis started.
2. Execute `./gradlew bootRun` to run the active microservice locally.
3. Import `postman_collection.json` into Postman to play heavily with endpoints.
