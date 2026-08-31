# EATrading

## Overview
EATrading is a trading platform that allows users to trade various financial instruments. The backend is built using Spring Boot and connected to a PostgreSQL database hosted on Supabase. The frontend is built using Angular and communicates with the backend through RESTful APIs.

## Project Structure
This repository consists of the backend, organized into the standard Spring Boot structure:

```
backend/
├── config/          # Configuration classes (EnvConfig, etc.)
├── controller/      # REST Controllers (UserController, etc.)
├── entity/          # JPA Entity classes (User, etc.)
├── repository/      # JPA Repository interfaces (UserRepository, etc.)
├── service/         # Business logic layer (UserService, etc.)
├── resources/       # Application properties and configurations
└── java/            # Main application class
```

## Architecture
- **Controller**: Handles HTTP requests and responses
- **Service**: Contains business logic and orchestration
- **Repository**: Manages data access and database operations
- **Entity**: Represents database tables

## API Endpoints

### Health Check
- `GET /api/health` - Check if the server is online

### User Management
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get a user by ID
- `POST /api/users` - Create a new user
- `PUT /api/users/{id}` - Update a user
- `DELETE /api/users/{id}` - Delete a user

## Steps to Run the Project

### Option A — Local (Maven)

1. Create `.env` file in the backend folder and add the following environment variables:
```bash
SUPABASE_DB_URL=
SUPABASE_DB_USER=
SUPABASE_DB_PASSWORD=
```

2. Clean install of maven project
```bash
./mvnw clean install
```

3. Start the application:
```bash
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8081`. Connect from outside the EC2 instance using the public IP in place of `localhost`.

---

### Option B — Docker (Recommended)

#### Prerequisites
- Docker installed and running

#### 1. Build the Docker image
```bash
cd backend
docker build -t eatrading-backend:latest .
```

The multistage build will:
- **Stage 1 (builder):** Use `eclipse-temurin:17-jdk-alpine` to resolve dependencies and run `mvn clean install -DskipTests`
- **Stage 2 (runtime):** Copy only the JAR into a slim `eclipse-temurin:17-jre-alpine` image, running as a non-root user

#### 2. Run the container
```bash
docker run -d \
  --name eatrading-backend \
  -p 8081:8081 \
  -e SUPABASE_DB_URL=<your_db_url> \
  -e SUPABASE_DB_USER=<your_db_user> \
  -e SUPABASE_DB_PASSWORD=<your_db_password> \
  eatrading-backend:latest
```

#### 3. Check logs
```bash
docker logs -f eatrading-backend
```

#### 4. Stop the container
```bash
docker stop eatrading-backend && docker rm eatrading-backend
```

The application will be available at `http://localhost:8081`. Connect from outside the EC2 instance using the public IP in place of `localhost`.

#### Docker image details
| Property     | Value                          |
|--------------|--------------------------------|
| Base image   | eclipse-temurin:17-jre-alpine  |
| Exposed port | 8081                           |
| Run as user  | appuser (non-root)             |
| Image tag    | eatrading-backend:latest       |
