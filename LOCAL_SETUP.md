# Local Development Setup

This guide explains how to run the Payment Labeling Engine locally using Docker Compose.

## Prerequisites

- Docker and Docker Compose installed
- Java 21 (for local development without Docker)
- Maven 3.9+ (for local development without Docker)

## Quick Start with Docker Compose

### Option 1: Run Full Stack (PostgreSQL + Application)

```bash
docker-compose up -d
```

This will:
- Start PostgreSQL database on `localhost:5432`
- Build and start the Spring Boot application on `localhost:8080`
- Run database migrations automatically via Flyway

### Option 2: Run Only PostgreSQL (Local Development)

```bash
docker-compose up -d postgres
```

Then run the Spring Boot application locally:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

## Configuration Files

### `docker-compose.yml`
- Defines PostgreSQL service and optional application service
- PostgreSQL runs on port 5432
- Application runs on port 8080
- Uses named volume `postgres_data` for persistent storage

### `application-local.yml`
- Configuration for running the app locally on your machine
- Connects to PostgreSQL on `localhost:5432`
- Used when running `mvn spring-boot:run` with `--spring.profiles.active=local`

### `application-docker.yml`
- Configuration for running the app inside Docker container
- Connects to PostgreSQL using Docker service name `postgres`
- Used automatically when running via Docker Compose

### `Dockerfile`
- Multi-stage build for efficient image creation
- Build stage: Compiles the Maven project
- Runtime stage: Runs the application on Java 21 Alpine image

## Database Details

- **Host**: `postgres` (in Docker) or `localhost` (local)
- **Port**: `5432`
- **Database**: `payment_labeling_db`
- **Username**: `postgres`
- **Password**: `postgres`

## Common Commands

### Start all services
```bash
docker-compose up -d
```

### Stop all services
```bash
docker-compose down
```

### View logs
```bash
docker-compose logs -f app
docker-compose logs -f postgres
```

### Rebuild images
```bash
docker-compose up -d --build
```

### Remove volumes (⚠️ deletes database)
```bash
docker-compose down -v
```

### Access PostgreSQL CLI
```bash
docker exec -it payment-labeling-postgres psql -U postgres -d payment_labeling_db
```

## Troubleshooting

### Application can't connect to database
- Ensure PostgreSQL container is healthy: `docker-compose ps`
- Check logs: `docker-compose logs postgres`
- Verify port 5432 is available

### Port already in use
Modify ports in `docker-compose.yml`:
```yaml
postgres:
  ports:
    - "5433:5432"  # Use 5433 on host instead
```

### Database migrations failed
Check Flyway logs:
```bash
docker-compose logs app | grep -i flyway
```

## Spring Profiles

- **`local`**: Running locally with local PostgreSQL or Docker-compose database
- **`docker`**: Running inside Docker container

Set profile when running locally:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```
