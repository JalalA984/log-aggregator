# Log Aggregator Microservices System

Trying to learn Spring Boot microservices: a demo that ingests, indexes, and searches logs with a live dashboard.

- Java 21

- PostgreSQL 14+

- Node.js 

## Quick Start

1. Start PostgreSQL and create databases:
```sql
CREATE DATABASE log_aggregator;
CREATE DATABASE log_query_db;
```

2. Start services in order:

```bash
# Terminal 1: Service Registry
cd service-registry
./gradlew bootRun

# Terminal 2: Log Ingest Service  
cd log-ingest-service
./gradlew bootRun

# Terminal 3: Log Query Service
cd log-query-service
./gradlew bootRun

# Terminal 4: API Gateway
cd api-gateway
./gradlew bootRun

# Optional: Log Simulators
cd web-app-simulator
./gradlew bootRun

cd payment-simulator
./gradlew bootRun
```

3. Start dashboard

```bash
cd web-dashboard
node server.js
```

## Services
| Service            | Port | Description                                |
| ------------------ | ---- | ------------------------------------------ |
| service-registry   | 8761 | Eureka – service discovery                 |
| api-gateway        | 8080 | Routes all API traffic                     |
| log-ingest-service | 8090 | Receives logs → stores in `log_aggregator` |
| log-query-service  | 8091 | Indexes logs → stores in `log_query_db`    |
| web-app-simulator  | 9001 | Generates fake web app logs                |
| payment-simulator  | 9002 | Generates fake payment logs                |
| web-dashboard      | 3000 | HTML dashboard to view and search logs     |


## Key Endpoints

**Eureka Dashboard:** http://localhost:8761

**Ingest Log:** POST http://localhost:8080/ingest/log

**Search Logs**: POST http://localhost:8080/query/search

**Get Stats: **GET http://localhost:8080/query/stats?hours=24


## Send a test log
curl -X POST http://localhost:8080/ingest/log \
  -H "Content-Type: application/json" \
  -d '{"sourceApp":"test","level":"INFO","message":"Test log"}'

## Search logs
curl -X POST http://localhost:8080/query/search \
  -H "Content-Type: application/json" \
  -d '{"query":"test","page":0,"size":10}'

## TODOS
- Distributed Tracing with Zipkin
- Probably a config server
- RabbitMQ?