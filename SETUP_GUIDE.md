# Setup Guide for Different Environments

## Option 1: Docker Compose (Recommended - works everywhere)
```bash
docker-compose up -d postgres api
```
Uses PostgreSQL in Docker. Kafka is started separately with the `local-kafka`
profile when local order processing is required.

### Local Kafka (default development setup)

Start the local Kafka broker and connect the API to it using the Docker service
name `kafka:9092`:

```bash
KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
docker-compose --profile local-kafka up -d kafka api
```

The local broker is exposed on port `9092` and advertises `kafka:9092` to
containers on the Compose network. The API is available at
`http://localhost:8099`.

Order processing uses these Kafka topics and consumer groups:

| Topic | Consumer group | Purpose |
|---|---|---|
| `order.incoming` | `eatrading-order-arrival` | Save the incoming order and obtain its market price |
| `order.unvalidated` | `eatrading-order-validation` | Validate cash, holdings, and market status |
| `order.unexecuted` | `eatrading-order-execution` | Execute accepted orders and update holdings |

Check the local broker and consumer groups:

```bash
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

for group in eatrading-order-arrival eatrading-order-validation eatrading-order-execution; do
   docker exec kafka kafka-consumer-groups.sh \
      --bootstrap-server localhost:9092 \
      --describe \
      --group "$group"
done
```

Stop the local Kafka broker when finished:

```bash
docker-compose --profile local-kafka stop kafka
```

### Remote Kafka instead of local Kafka

Set `KAFKA_BOOTSTRAP_SERVERS` to a broker address reachable from the API
container. Do not use `localhost` unless Kafka is running in the same container
as the API:

```bash
KAFKA_BOOTSTRAP_SERVERS=10.23.132.254:9092 \
docker-compose up -d --build api
```

The remote broker must advertise an address that the API container can resolve
and reach, and TCP port `9092` must be allowed by its firewall/security group.
Do not start the local Kafka profile when using the remote broker.

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
   docker-compose up -d postgres
   ```

   To use local Kafka as well, run the local Kafka command from Option 1.

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
KAFKA_BOOTSTRAP_SERVERS=your-kafka-hostname:9092
```


## Key Points
- **Docker hostnames** (`postgres:5432`) only work inside containers
- **localhost:8088** only works from the machine running Docker
- **Remote machines** need the actual machine hostname/IP with exposed ports
   (8088 for PostgreSQL and 9092 for Kafka)
- **Kafka local-first** uses `--profile local-kafka` and `kafka:9092` inside the
   Compose network
