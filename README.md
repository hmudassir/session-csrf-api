# Session and CSRF API

A small Spring Boot API that signs a person in with a **server-side session cookie** and requires a **CSRF token** on every state-changing request.

Passwords are stored with **Argon2id** (`Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`). The parameters are the Spring Security defaults: salt 16, hash 32, parallelism 1, 16 MiB of memory, 2 iterations. This sample does not weaken them.

The browser never receives the password hash. The session id lives in an `HttpOnly` cookie named `SESSION`. The CSRF token is a separate cookie, `XSRF-TOKEN`, which JavaScript can read and send back as the `X-XSRF-TOKEN` header.

The boundaries, the request path, and the failure responses are in [docs/architecture.md](docs/architecture.md).

## Run

Java 21.

```sh
./mvnw spring-boot:run
```

The sample uses an in-memory H2 database so it starts with no extra services. Flyway creates the `users` table. Spring Session creates its own tables.

## Try it

```sh
curl -sS -c /tmp/session.cookies -b /tmp/session.cookies http://127.0.0.1:8080/api/auth/csrf
token=$(awk '$6=="XSRF-TOKEN"{print $7}' /tmp/session.cookies)

curl -sS -c /tmp/session.cookies -b /tmp/session.cookies \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $token" \
  -d '{"name":"Ada","email":"ada@example.com","password":"correct-horse-1"}' \
  http://127.0.0.1:8080/api/auth/signup

curl -sS -b /tmp/session.cookies http://127.0.0.1:8080/api/auth/me
```

A signup without the CSRF header is `403`. `GET /api/auth/me` without a session is `401`.

## Tests

```sh
./mvnw test
```

## What this is

A portfolio proof of concept. It is not a production application and it does not connect to a real user database.
