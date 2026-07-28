# Spring Boot Payment Microservices System

A production-style, multi-service payment processing system built with **Java 21** and **Spring Boot 3**, designed to simulate a real-world e-commerce payment flow with third‑party provider integration.

The project demonstrates **microservices architecture**, **secure communication**, **event-driven messaging**, and **containerized infrastructure**, making it suitable for backend engineering portfolios and system design discussions.

---

## High-Level Architecture

The system is composed of **four independent microservices**, each owning a single responsibility and communicating via REST and messaging.

```
Client
  |
  v
Payment Validation Service (8081)
  |
  v
Payment Processing Service (8082)
  |
  v
Trustly Provider Adapter (8083)
  |
  v
Mock Trustly API (8084)
```

---

## Services Overview

### 1. Payment Validation Service (Port 8081)

**Public entry point** for all payment requests.

Responsibilities:

* Client authentication using **HMAC-SHA256**
* Validation of request payload and business rules
* Fetching dynamic rules from **Redis**
* Orchestrating the payment flow

---

### 2. Payment Processing Service (Port 8082)

**Core transactional service** responsible for payment lifecycle management.

Responsibilities:

* Persisting payment state in **MySQL**
* Internal service-to-service security using **RSA digital signatures**
* Publishing final payment status events to **ActiveMQ**

---

### 3. Trustly Provider Service (Port 8083)

**Adapter service** responsible for communication with the external payment provider.

Responsibilities:

* Transforming internal requests to Trustly-compatible payloads
* Handling provider responses
* Storing raw request/response payloads in **MongoDB** for auditing

---

### 4. Mock Trustly Service (Port 8084)

**Simulator** for the external Trustly API.

Responsibilities:

* Emulating third-party payment responses
* Enabling end-to-end testing without real provider dependency

---

## Key Technologies

### Backend

* Java 21
* Spring Boot 3
* Spring Web, Spring Data JPA

### Security

* **HMAC-SHA256** for client authentication
* **RSA signatures** for internal service communication

### Data Stores

* **MySQL** – transactional payment data
* **MongoDB** – audit logs and raw provider payloads
* **Redis** – dynamic business rule caching

### Messaging

* **ActiveMQ** – asynchronous payment status notifications

### Infrastructure

* **Docker & Docker Compose** – containerized services

---

## Getting Started

### Prerequisites

* Java 21+
* Docker Desktop
* IntelliJ IDEA / STS
* Postman

---

### Step 1: Start Infrastructure Services

All dependent services are managed via Docker Compose.

From the project root:

```bash
docker-compose up -d
```

This starts:

* MySQL
* MongoDB
* Redis
* ActiveMQ

---

### Step 2: Run Microservices

Start each Spring Boot service individually from your IDE or via terminal:

```bash
mvn spring-boot:run
```

Ports used:

| Service            | Port |
| ------------------ | ---- |
| Payment Validation | 8081 |
| Payment Processing | 8082 |
| Trustly Provider   | 8083 |
| Mock Trustly       | 8084 |

---

### Step 3: Test the Payment Flow

1. Send a payment request to **Payment Validation Service**
2. Request is authenticated and validated
3. Payment is processed and persisted
4. Provider interaction is simulated
5. Final status is published asynchronously

---

## Project Highlights

* Clean separation of concerns across services
* Secure external and internal communication
* Event-driven architecture using messaging
* Fully containerized infrastructure
* Real-world payment workflow simulation

---

## Future Improvements

* API documentation with **Swagger / OpenAPI**
* Centralized configuration using Spring Cloud Config
* Distributed tracing (Zipkin / OpenTelemetry)
* Circuit breaker and retries (Resilience4j)
* Kubernetes deployment

---

## Author

Developed as a backend-focused microservices project using Spring Boot.

---

## License

This project is for educational and demonstration purposes.
