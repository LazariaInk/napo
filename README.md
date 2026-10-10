# NAPO

NAPO is a Spring Boot reminder assistant MVP. It supports authenticated users, reminder CRUD, AI-assisted reminder creation, server-side scheduling, mock notification attempts, and simple recurrence.

## Requirements

- Java 21
- Maven wrapper included in the repository
- Docker Desktop, for local PostgreSQL
- Node.js and npm, only for the temporary React test UI

## Local Database

Start PostgreSQL:

```powershell
docker compose up -d
```

The database is exposed on:

```text
localhost:55432
database: napo
username: postgres
password: postgres
```

Connect with psql:

```powershell
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -h 127.0.0.1 -p 55432 -U postgres -d napo
```

List users:

```sql
SELECT id, email, display_name, role, auth_provider, email_verified, created_at, updated_at
FROM user_accounts
ORDER BY id;
```

## Backend

Set the OpenAI API key in the environment, then run the local profile:

```powershell
cd C:\Workspace\napo
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:OPENAI_API_KEY="your-openai-api-key"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Backend URL:

```text
http://localhost:8080
```

Mobile Google login configuration:

```powershell
$env:NAPO_JWT_SECRET="replace-with-a-long-random-secret-at-least-32-chars"
$env:NAPO_GOOGLE_ALLOWED_CLIENT_IDS="android-client-id.apps.googleusercontent.com,ios-client-id.apps.googleusercontent.com"
```

The mobile app signs in with Google natively, receives a Google `idToken`, then calls:

```http
POST /api/auth/google
```

```json
{
  "idToken": "google-id-token-from-react-native"
}
```

The backend verifies the Google token and returns a NAPO `accessToken`. Mobile requests should use:

```text
Authorization: Bearer <accessToken>
```

Run tests:

```powershell
$env:OPENAI_API_KEY="test-key"
.\mvnw.cmd test
```

The unit tests do not require PostgreSQL to be running.

Run the Docker integration test:

```powershell
docker compose up -d
$env:OPENAI_API_KEY="test-key"
$env:NAPO_RUN_INTEGRATION_TESTS="true"
.\mvnw.cmd -Dtest=NapoDockerIntegrationTest test
```

The integration test uses the real local PostgreSQL container on `localhost:55432`.
It creates a temporary user with an email like `it-xxxxxxxx@napo.local`, creates one
reminder through the REST API, manually processes due reminders, then verifies the
stored notification attempt.

## Temporary React Test UI

The React frontend is only a local testing tool. The production client is expected to be React Native later.

```powershell
cd C:\Workspace\napo\frontend
npm install
npm run dev
```

Frontend URL:

```text
http://127.0.0.1:5173
```

The Vite dev server proxies `/api` to `http://localhost:8080`.

## Useful API Flow

Register:

```http
POST /api/auth/register
```

```json
{
  "email": "test@napo.local",
  "password": "password123",
  "displayName": "Test User"
}
```

Use the returned JWT for protected endpoints:

```text
Authorization: Bearer <accessToken>
```

Basic Auth still works for local Postman testing with email/password users: username `test@napo.local`, password `password123`.

Notification history:

```http
GET /api/notification-attempts
GET /api/notification-attempts/reminders/{reminderId}
```
