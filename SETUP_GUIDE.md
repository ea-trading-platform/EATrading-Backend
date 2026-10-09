# Setup Guide for Different Environments

## Option 1: Docker Compose (Recommended - works everywhere)
```bash
docker-compose up
```
Uses: `.env` with `postgres:5432` (internal Docker network)

## Option 2: Local Development with H2 (Quickest - No Database Setup)

Perfect for quick development without PostgreSQL:

```bash
cd api
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Or run the packaged JAR:
```bash
java -jar target/api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

**Features:**
- In-memory H2 database (auto-creates schema)
- H2 Console available at `http://localhost:8099/h2-console`
- Database resets on app restart
- Debug logging enabled
- No external dependencies needed
- Swagger UI at `http://localhost:8099/swagger-ui.html`

**H2 Console Credentials:**
- Connection URL: `jdbc:h2:mem:eatradingdb`
- Username: `sa`
- Password: (leave empty)

## Option 3: Local Development (macOS/Linux/Windows)

1. **Start only PostgreSQL in Docker:**
   ```bash
   docker-compose up postgres kafka
   ```

2. **Create `.env` for local development:**
   ```bash
   DB_URL=jdbc:postgresql://localhost:8088/ea-db
   ```

3. **Run Spring Boot locally with postgres profile:**
   ```bash
   cd api
   export $(cat .env | grep -v '^#' | xargs)
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=postgres"
   ```

## Option 4: Different Machine on Network
Replace `localhost` with the actual hostname/IP:
```bash
DB_URL=jdbc:postgresql://your-hostname:8088/ea-db
```


## Key Points
- **Docker hostnames** (`postgres:5432`) only work inside containers
- **localhost:8088** only works from the machine running Docker
- **Remote machines** need the actual machine hostname/IP with exposed port (8088)
