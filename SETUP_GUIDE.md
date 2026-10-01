# Setup Guide for Different Environments

## Option 1: Docker Compose (Recommended - works everywhere)
```bash
docker-compose up
```
Uses: `.env` with `postgres:5432` (internal Docker network)

## Option 2: Local Development (macOS/Linux/Windows)

1. **Start only PostgreSQL in Docker:**
   ```bash
   docker-compose up postgres kafka
   ```

2. **Create `.env` for local development:**
   ```bash
   DB_URL=jdbc:postgresql://localhost:8088/ea-db
   ```

3. **Run Spring Boot locally:**
   ```bash
   cd api
   export $(cat .env | grep -v '^#' | xargs)
   mvn spring-boot:run
   ```

## Option 3: Different Machine on Network
Replace `localhost` with the actual hostname/IP:
```bash
DB_URL=jdbc:postgresql://your-hostname:8088/ea-db
```

## Key Points
- **Docker hostnames** (`postgres:5432`) only work inside containers
- **localhost:8088** only works from the machine running Docker
- **Remote machines** need the actual machine hostname/IP with exposed port (8088)
