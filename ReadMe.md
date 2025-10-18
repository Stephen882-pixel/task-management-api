# Task Management API with Google Calendar Sync

A Spring Boot REST API for managing tasks with automatic Google Calendar synchronization.

## Features

- Complete CRUD operations for tasks
- Two-way sync with Google Calendar
- Automatic conflict detection and resolution
- Docker support with PostgreSQL

---

## Prerequisites

- Docker & Docker Compose
- Java 21 & Maven 3.9+ (for initial OAuth setup)
- Google Account with Calendar access

---

## Quick Start

### 1. Clone and Configure

```bash
git clone https://github.com/Stephen882-pixel/task-management-api
cd taskManagement

# Create environment file
cp .env.example .env
```

Edit `.env`:
```env
POSTGRES_DB=task_management
POSTGRES_USER=postgres
POSTGRES_PASSWORD=YourPassword123
GOOGLE_CALENDAR_ENABLED=true
```

### 2. Google Calendar Setup

#### Create Service Account (Recommended)

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project: `Task Management API`
3. Enable **Google Calendar API**
4. Go to **IAM & Admin** → **Service Accounts** → **Create Service Account**
5. Download JSON key and save as `credentials.json` in project root
6. Share your Google Calendar with the service account email (found in JSON)
    - Permission: "Make changes to events"

#### Alternative: OAuth 2.0 (Desktop App)

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. **APIs & Services** → **OAuth consent screen** → Configure
3. **Credentials** → **Create OAuth client ID** → Desktop app
4. Download JSON and save as `credentials.json`

### 3. Run Application

** Important for Docker:** OAuth requires browser access. You must authenticate locally first.

```bash
# Step 1: Authenticate locally (opens browser)
./mvnw spring-boot:run
# Grant permissions in browser, wait for "tokens" folder to be created

# Step 2: Run with Docker
docker compose up --build
```

**API Available at:** `http://localhost:8080`

---

## Running Locally (Without Docker)

### Setup PostgreSQL

```bash
# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Create database
sudo -u postgres psql
CREATE DATABASE task_management;
CREATE USER taskuser WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE task_management TO taskuser;
\q
```

### Configure Application

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/task_management
spring.datasource.username=taskuser
spring.datasource.password=password
google.calendar.enabled=true
google.calendar.credentials.path=credentials.json
```

### Run

```bash
./mvnw spring-boot:run
```

---

## API Endpoints

### Tasks
- `GET /api/tasks` - Get all tasks
- `POST /api/tasks` - Create task
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task

### Calendar Sync
- `POST /api/calendar/sync/enable` - Enable sync
- `POST /api/calendar/sync/task-to-calendar/{taskId}` - Sync to calendar
- `GET /api/calendar/sync/status/{taskId}` - Get sync status

## Troubleshooting

### Docker OAuth Issue
**Problem:** Browser doesn't open in Docker container

**Solution:** Always authenticate locally first:
```bash
./mvnw spring-boot:run  # Authenticate here first
docker compose up --build  # Then run Docker
```

### Credentials Not Found
**Problem:** `Resource not found: credentials.json`

**Solution:**
- Ensure `credentials.json` is in project root
- Check file permissions: `chmod 644 credentials.json`

### Calendar Sync Failed
**Problem:** `Failed to sync with Google Calendar`

**Solution:**
- Verify Calendar API is enabled
- Check service account has calendar access
- Ensure calendar ID is correct (usually your email)

---

## Tech Stack

- Java 21
- Spring Boot 3.5
- PostgreSQL 15
- Google Calendar API
- Docker & Docker Compose

---

