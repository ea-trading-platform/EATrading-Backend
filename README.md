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

The application will start on `http://localhost:8081` by default. Connect to this service outside the linux ssh by using the public IP of the EC2 instance in place of localhost.