# Architecture

This service keeps the credential on the server. The browser holds a session id it cannot read, and a separate CSRF token it can.

## Boundaries

```text
browser
  session cookie     HttpOnly, so script cannot read it
  CSRF cookie        readable, echoed as X-XSRF-TOKEN
        |
        v
SecurityFilterChain
  CORS allow-list, CSRF check, session lookup
        |
        v
AuthController
  signup, sign-in, logout, current user
        |
        +--> PasswordEncoder          Argon2id, Spring Security defaults
        +--> AuthenticationManager    email plus password
        +--> UserAccountRepository    email, name, password hash
        +--> SecurityContextRepository
                    |
                    v
              Spring Session JDBC     session row in the database
```

The controller does not invent a token for the client to store as a bearer credential. `SecurityContextRepository` writes the authenticated user into the server session. The response sets the `SESSION` cookie.

## Request path

1. `GET /api/auth/csrf` returns the token and sets `XSRF-TOKEN`.
2. `POST /api/auth/signup` or `POST /api/auth/signin` must send that value in `X-XSRF-TOKEN`.
3. The filter rejects a state-changing request with no matching token before the controller runs. That response is `403`.
4. Signup stores the Argon2id hash and then establishes a new session. Sign-in does the same after the password matches.
5. `GET /api/auth/me` reads the session. No session is `401`.

CORS allows only `http://127.0.0.1:8081` and `http://localhost:8081`, with credentials. A page on another origin cannot call this API with the cookie.

## Failure

| Condition | Result |
| --- | --- |
| State-changing request without the CSRF header | `403` before authentication |
| Wrong email or wrong password | `401` with the same message for both |
| `GET /api/auth/me` without a session | `401` |
| Signup for an email that already exists | `409` |

The password hash is the only stored form of the password. Logs and responses do not include it.

## What this sample leaves out

One process and an in-memory database. A deployed service would keep the same cookie, CSRF, and password rules and move the session tables to the shared database.
