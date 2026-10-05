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
SELECT id, email, display_name, role, created_at, updated_at
FROM user_accounts
ORDER BY id;
```

## Backend

Set the OpenAI API key in the environment, then run the local profile:

```powershell
cd C:\Workspace\napo
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:OPENAI_API_KEY="sk-your-valid-key"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Backend URL:

```text
http://localhost:8080
```

Run tests:

```powershell
$env:OPENAI_API_KEY="test-key"
.\mvnw.cmd test
```

The unit tests do not require PostgreSQL to be running.

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

Use Basic Auth for protected endpoints:

```text
username: test@napo.local
password: password123
```

Notification history:

```http
GET /api/notification-attempts
GET /api/notification-attempts/reminders/{reminderId}
```
