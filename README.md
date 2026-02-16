# E-Commerce Microservices Architecture
## Complete System Flow & Entity Mapping Documentation

**Date:** February 14, 2026  
**Architecture:** Microservices with Orchestration-based Saga Pattern  
**Technology Stack:** Spring Boot 3.x, Kafka, MySQL, JWT

---

## Table of Contents
1. [System Overview](#system-overview)
2. [Service Architecture](#service-architecture)
3. [Complete Order Flow](#complete-order-flow)
4. [Database Schema](#database-schema)
5. [Entity Relationships](#entity-relationships)
6. [Kafka Events](#kafka-events)
7. [Service Communication](#service-communication)
8. [State Transitions](#state-transitions)

---

## 1. System Overview

### Architecture Summary
- **Pattern:** Orchestration-based Saga Pattern
- **Total Services:** 5 Microservices
- **Communication:** REST (Sync) + Kafka (Async)
- **Database Strategy:** Database-per-Service
- **Authentication:** JWT-based with Spring Security

### Services & Ports

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| Auth Service | 8080 | auth_db | User authentication & JWT generation |
| Product Service | 8081 | product_db | Product catalog management |
| Cart Service | 8082 | cart_db | Shopping cart operations |
| Payment Service | 8083 | payment_db | Payment processing & gateway integration |
| Order Service | 8084 | order_db | Order management & Saga orchestration |
| Kafka | 9092 | - | Event streaming |
| Zookeeper | 2181 | - | Kafka coordination |

---

## 2. Service Architecture

### High-Level Architecture Flow

```
┌─────────────────────────────────────────────────────────┐
│                    USER/CLIENT                          │
│              (React/Angular/Postman)                    │
└─────────────┬───────────────────────────────────────────┘
              │
              ├──────→ Auth Service (8080) → auth_db
              │         - Register
              │         - Login (returns JWT)
              │
              ├──────→ Product Service (8081) → product_db
              │         - Browse products
              │         - View product details
              │
              ├──────→ Cart Service (8082) → cart_db
              │         - Add to cart
              │         - View cart
              │         - Update quantities
              │
              └──────→ Order Service (8084) → order_db
                        - Place order
                        │
                        ├─→ Calls Cart Service (REST)
                        │   Fetches cart items
                        │
                        ├─→ Saga Orchestrator
                        │   │
                        │   ├─→ Payment Service (REST)
                        │   │   Process payment → payment_db
                        │   │
                        │   └─→ Kafka (Async)
                        │       Publish events
                        │
                        └─→ Cart Service (via Kafka)
                            Clear cart on success
```

---

## 3. Complete Order Flow

### 3.1 Success Flow (Payment Succeeds - 80%)

```
STEP 1: User Places Order
├─→ POST /api/orders (with JWT token)
└─→ Order Controller receives request

STEP 2: Fetch Cart
├─→ OrderService calls CartService
├─→ GET http://localhost:8082/api/cart
└─→ Receives cart items & total amount

STEP 3: Saga Orchestrator Begins
├─→ OrderSagaOrchestrator.executeOrderSaga()
│
├─→ [Step 1] Create Order (PENDING)
│   ├─→ order.status = PENDING
│   ├─→ order.totalAmount = calculated
│   ├─→ order.items = cart items snapshot
│   └─→ Save to order_db → Order ID: 1
│
├─→ [Step 2] Process Payment
│   ├─→ Create PaymentRequest {orderId, userId, amount}
│   ├─→ Call Payment Service (REST)
│   │   POST http://localhost:8083/api/payments/process
│   │
│   └─→ Payment Service Flow:
│       ├─→ Save payment record (status=PENDING)
│       ├─→ Call Payment Gateway (Mock)
│       │   └─→ 80% success rate
│       ├─→ Gateway returns SUCCESS
│       ├─→ Update payment (status=SUCCESS, transactionId=TXN_ABC123)
│       └─→ Return PaymentResponse {status: SUCCESS, txnId: TXN_ABC123}
│
├─→ [Step 3] Update Order (PAID)
│   ├─→ order.status = PAID
│   ├─→ order.paymentId = TXN_ABC123
│   └─→ Save to order_db
│
├─→ [Step 4] Publish Event
│   ├─→ Create ORDER_COMPLETED event
│   │   {
│   │     "eventType": "ORDER_COMPLETED",
│   │     "username": "testuser",
│   │     "orderId": 1
│   │   }
│   └─→ Publish to Kafka topic: ORDER_EVENTS
│
└─→ [Step 5] Cart Service Consumes Event
    ├─→ Kafka → Cart Service Consumer
    ├─→ handleOrderCompleted()
    ├─→ cartService.clearCart("testuser")
    └─→ ✅ Cart cleared successfully

RESULT: Order status = PAID, Cart cleared
```

### 3.2 Failure Flow (Payment Fails - 20%)

```
STEP 1-2: Same as Success Flow
(User places order, cart fetched)

STEP 3: Saga Orchestrator Begins
├─→ [Step 1] Create Order (PENDING)
│   └─→ Save to order_db → Order ID: 2
│
├─→ [Step 2] Process Payment
│   ├─→ Call Payment Service
│   │
│   └─→ Payment Service Flow:
│       ├─→ Save payment record (status=PENDING)
│       ├─→ Call Payment Gateway
│       │   └─→ ❌ Payment FAILED
│       ├─→ Failure reasons (random):
│       │   - Insufficient funds
│       │   - Card declined
│       │   - Invalid card details
│       │   - Transaction limit exceeded
│       │   - Gateway timeout
│       ├─→ Update payment (status=FAILED, failureReason)
│       └─→ Return PaymentResponse {status: FAILED, reason: "Insufficient funds"}
│
├─→ [Step 3] ⚠️ COMPENSATION - Cancel Order
│   ├─→ order.status = CANCELLED
│   ├─→ order.failureReason = "Insufficient funds"
│   └─→ Save to order_db
│
├─→ [Step 4] Publish Cancellation Event
│   ├─→ Create ORDER_CANCELLED event
│   │   {
│   │     "eventType": "ORDER_CANCELLED",
│   │     "username": "testuser",
│   │     "orderId": 2,
│   │     "reason": "Insufficient funds"
│   │   }
│   └─→ Publish to Kafka topic: ORDER_EVENTS
│
└─→ [Step 5] Cart Service Handles Cancellation
    ├─→ Kafka → Cart Service Consumer
    ├─→ handleOrderCancelled()
    ├─→ Log: "Keeping cart intact - user can retry"
    └─→ ℹ️ Cart NOT cleared (preserved for retry)

RESULT: Order status = CANCELLED, Cart preserved
```

---

## 4. Database Schema

### 4.1 Auth Service Database (auth_db)

**Table: users**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,        -- BCrypt encrypted
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL,              -- USER, ADMIN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Sample Data:**
```
| id | username | password | email | role | created_at |
|----|----------|----------|-------|------|------------|
| 1  | john     | $2a$10.. | j@e.. | USER | 2026-02-14 |
```

---

### 4.2 Product Service Database (product_db)

**Table: products**
```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    description TEXT,
    stock INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Sample Data:**
```
| id | name   | price  | description        | stock |
|----|--------|--------|--------------------|-------|
| 1  | Laptop | 999.99 | High-performance.. | 50    |
| 2  | Mouse  | 29.99  | Wireless mouse     | 100   |
```

---

### 4.3 Cart Service Database (cart_db)

**Table: cart**
```sql
CREATE TABLE cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Table: cart_items**
```sql
CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE
);
```

**Sample Data:**
```
cart:
| id | username | created_at          |
|----|----------|---------------------|
| 1  | john     | 2026-02-14 10:00:00 |

cart_items:
| id | cart_id | product_id | quantity |
|----|---------|------------|----------|
| 1  | 1       | 1          | 2        |
| 2  | 1       | 2          | 3        |
```

---

### 4.4 Order Service Database (order_db)

**Table: orders**
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,            -- PENDING, PAID, CANCELLED, SHIPPED, DELIVERED
    payment_id VARCHAR(255),                 -- Transaction ID from Payment Service
    failure_reason VARCHAR(255),             -- Reason if cancelled
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Table: order_items**
```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,              -- Original product ID
    product_name VARCHAR(255) NOT NULL,      -- Snapshot
    price DECIMAL(10,2) NOT NULL,            -- Snapshot
    quantity INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

**Sample Data:**
```
orders:
| id | username | total_amount | status    | payment_id   | failure_reason      | created_at          |
|----|----------|--------------|-----------|--------------|---------------------|---------------------|
| 1  | john     | 2089.95      | PAID      | TXN_ABC123   | NULL                | 2026-02-14 10:30:00 |
| 2  | john     | 999.99       | CANCELLED | NULL         | Insufficient funds  | 2026-02-14 10:32:00 |
| 3  | john     | 999.99       | PAID      | TXN_XYZ789   | NULL                | 2026-02-14 10:35:00 |

order_items:
| id | order_id | product_id | product_name | price  | quantity |
|----|----------|------------|--------------|--------|----------|
| 1  | 1        | 1          | Laptop       | 999.99 | 2        |
| 2  | 1        | 2          | Mouse        | 29.99  | 3        |
| 3  | 3        | 1          | Laptop       | 999.99 | 1        |
```

---

### 4.5 Payment Service Database (payment_db)

**Table: payments**
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT UNIQUE NOT NULL,         -- One payment per order
    user_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,             -- SUCCESS, FAILED, PENDING
    transaction_id VARCHAR(255),              -- From payment gateway
    gateway_response TEXT,                    -- Raw response
    failure_reason VARCHAR(255),              -- Reason if failed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Sample Data:**
```
| id | order_id | user_id | amount  | status  | transaction_id | failure_reason      | created_at          |
|----|----------|---------|---------|---------|----------------|---------------------|---------------------|
| 1  | 1        | john    | 2089.95 | SUCCESS | TXN_ABC123     | NULL                | 2026-02-14 10:30:05 |
| 2  | 2        | john    | 999.99  | FAILED  | NULL           | Insufficient funds  | 2026-02-14 10:32:05 |
| 3  | 3        | john    | 999.99  | SUCCESS | TXN_XYZ789     | NULL                | 2026-02-14 10:35:05 |
```

---

## 5. Entity Relationships

### 5.1 Within-Service Relationships (Foreign Keys)

**Cart Service:**
```
cart (1) ─────── (N) cart_items
  └─ One cart has many cart items
```

**Order Service:**
```
orders (1) ─────── (N) order_items
  └─ One order has many order items
```

### 5.2 Cross-Service Logical Relationships (No FK)

```
User (auth_db)
  ├─→ username ─→ cart (cart_db)
  ├─→ username ─→ orders (order_db)
  └─→ user_id ──→ payments (payment_db)

Product (product_db)
  ├─→ product_id ─→ cart_items (cart_db)
  └─→ product_id ─→ order_items (order_db) [snapshot]

Order (order_db)
  ├─→ order_id ──→ payments (payment_db)
  └─→ payment_id ←─ transaction_id (payment_db)
```

### 5.3 Complete Entity Map

```
┌────────────────────────────────────────────────────────────┐
│                     AUTH SERVICE                           │
│  ┌──────────────────────────────────────────────────┐     │
│  │  USER                                            │     │
│  │  - id (PK)                                       │     │
│  │  - username (UK) ────────────────────┐          │     │
│  │  - password                           │          │     │
│  │  - email (UK)                         │          │     │
│  │  - role                               │          │     │
│  └──────────────────────────────────────┼──────────┘     │
└────────────────────────────────────────┼────────────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    │                    │                    │
          ┌─────────▼──────────┐  ┌──────▼────────┐  ┌──────▼────────┐
          │  CART SERVICE      │  │ ORDER SERVICE │  │PAYMENT SERVICE│
          │                    │  │               │  │               │
          │  CART              │  │  ORDER        │  │  PAYMENT      │
          │  - id (PK)         │  │  - id (PK)    │  │  - id (PK)    │
          │  - username (UK)   │  │  - username   │  │  - order_id UK│
          │  - created_at      │  │  - amount     │  │  - user_id    │
          │     │              │  │  - status ────┼──┼→ - amount     │
          │     │              │  │  - payment_id │  │  - status     │
          │     │              │  │  - failure    │  │  - txn_id     │
          │     │ 1:N          │  │     │         │  │  - failure    │
          │     ▼              │  │     │ 1:N     │  │               │
          │  CART_ITEM         │  │     ▼         │  │               │
          │  - id (PK)         │  │  ORDER_ITEM   │  │               │
          │  - cart_id (FK)    │  │  - id (PK)    │  │               │
          │  - product_id ─────┼──┼─ - order_id FK│  │               │
          │  - quantity        │  │  - product_id │  │               │
          └────────────────────┘  │  - name (snap)│  └───────────────┘
                    │             │  - price(snap)│
                    │             │  - quantity   │
                    │             └───────────────┘
                    │
          ┌─────────▼──────────┐
          │  PRODUCT SERVICE   │
          │                    │
          │  PRODUCT           │
          │  - id (PK)         │
          │  - name            │
          │  - price           │
          │  - description     │
          │  - stock           │
          └────────────────────┘
```

---

## 6. Kafka Events

### 6.1 Topics

| Topic Name | Partition | Replication | Publisher | Consumer |
|------------|-----------|-------------|-----------|----------|
| ORDER_EVENTS | 1 | 1 | Order Service | Cart Service |
| ORDER_CREATED | 1 | 1 | Order Service | Cart Service (Legacy) |

### 6.2 Event Types

**ORDER_COMPLETED Event:**
```json
{
  "eventType": "ORDER_COMPLETED",
  "username": "john",
  "orderId": 123
}
```

**Triggered when:** Payment succeeds  
**Consumer action:** Clear cart

---

**ORDER_CANCELLED Event:**
```json
{
  "eventType": "ORDER_CANCELLED",
  "username": "john",
  "orderId": 124,
  "reason": "Insufficient funds"
}
```

**Triggered when:** Payment fails  
**Consumer action:** Keep cart intact (no clearing)

---

### 6.3 Event Flow

```
Order Service (Saga Orchestrator)
  │
  ├─→ Payment SUCCESS
  │   └─→ Kafka Producer
  │       └─→ Publish to ORDER_EVENTS
  │           {eventType: "ORDER_COMPLETED", ...}
  │               │
  │               └─→ Cart Service Consumer
  │                   └─→ clearCart(username)
  │
  └─→ Payment FAILED
      └─→ Kafka Producer
          └─→ Publish to ORDER_EVENTS
              {eventType: "ORDER_CANCELLED", ...}
                  │
                  └─→ Cart Service Consumer
                      └─→ Log "Keeping cart intact"
```

---

## 7. Service Communication

### 7.1 Synchronous Communication (REST)

| From | To | Endpoint | Method | Purpose |
|------|-----|----------|--------|---------|
| Client | Auth | /api/auth/login | POST | Get JWT token |
| Client | Product | /api/products | GET | Browse catalog |
| Client | Cart | /api/cart/add | POST | Add to cart |
| Client | Cart | /api/cart | GET | View cart |
| Client | Order | /api/orders | POST | Place order |
| **Order** | **Cart** | **/api/cart** | **GET** | **Fetch cart (in saga)** |
| **Order** | **Payment** | **/api/payments/process** | **POST** | **Process payment (in saga)** |

### 7.2 Asynchronous Communication (Kafka)

| Publisher | Topic | Event Type | Consumer | Action |
|-----------|-------|------------|----------|--------|
| Order Service | ORDER_EVENTS | ORDER_COMPLETED | Cart Service | Clear cart |
| Order Service | ORDER_EVENTS | ORDER_CANCELLED | Cart Service | Keep cart |

---

## 8. State Transitions

### 8.1 Order Status State Machine

```
                      ┌──────────┐
                      │  START   │
                      └────┬─────┘
                           │
                           ▼
                      ┌──────────┐
                      │ PENDING  │ ◄─── Order created
                      └────┬─────┘      Awaiting payment
                           │
              ┌────────────┴────────────┐
              │                         │
       Payment Success          Payment Failed
              │                         │
              ▼                         ▼
        ┌──────────┐              ┌───────────┐
        │   PAID   │              │ CANCELLED │
        └────┬─────┘              └─────┬─────┘
             │                           │
             │ Cart cleared              │ Cart preserved
             │ via Kafka                 │ User can retry
             │                           │
             ▼                           ▼
        ┌──────────┐                ┌────────┐
        │ SHIPPED  │                │  END   │
        └────┬─────┘                └────────┘
             │
             ▼
        ┌───────────┐
        │ DELIVERED │
        └─────┬─────┘
              │
              ▼
          ┌────────┐
          │  END   │
          └────────┘
```

### 8.2 Payment Status Transitions

```
PENDING ──→ SUCCESS  (80% probability)
         └→ FAILED   (20% probability)
```

---

## 9. Technology Stack

### Core Technologies

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.2.0 |
| Language | Java | 17 |
| Security | Spring Security + JWT | 6.x |
| Database | MySQL | 8.x |
| ORM | Hibernate/JPA | 6.x |
| Message Broker | Apache Kafka | 3.x |
| Build Tool | Maven | 3.x |
| REST Client | RestTemplate | Spring |
| Payment Gateway | Mock (80% success) | Custom |

---

## 10. Key Design Patterns

### 10.1 Saga Pattern (Orchestration)
- **Orchestrator:** OrderSagaOrchestrator
- **Participants:** Order Service, Payment Service
- **Compensation:** Order cancellation on payment failure

### 10.2 Database-per-Service
- Each microservice has its own database
- No shared databases
- Loose coupling

### 10.3 Event-Driven Architecture
- Kafka for async communication
- Event sourcing for cart operations
- Eventual consistency

### 10.4 CQRS (Command Query Responsibility Segregation)
- Commands: Place order, add to cart
- Queries: View cart, view orders
- Separation of concerns

---

## 11. Success Metrics

### Current Implementation
- ✅ 5 Microservices running independently
- ✅ Saga pattern with compensation
- ✅ Event-driven cart clearing
- ✅ JWT authentication
- ✅ Database-per-service pattern
- ✅ Resilient failure handling
- ✅ 80% payment success rate (mock)
- ✅ Audit trail for all transactions

### Data Consistency
```
Total Orders = Total Payments
Paid Orders = Successful Payments
Cancelled Orders = Failed Payments
```

---

## 12. Future Enhancements

1. **Inventory Service** - Stock management with reservation
2. **API Gateway** - Centralized routing (Spring Cloud Gateway)
3. **Service Discovery** - Eureka for dynamic registration
4. **Circuit Breaker** - Resilience4j for fault tolerance
5. **Distributed Tracing** - Zipkin/Jaeger for monitoring
6. **Real Payment Gateway** - Stripe/Razorpay integration
7. **Idempotency** - Prevent duplicate order processing
8. **Rate Limiting** - Protect services from overload
9. **Caching** - Redis for improved performance
10. **Notification Service** - Email/SMS for order updates

---

## Appendix: Quick Reference

### Service URLs
```
Auth:     http://localhost:8080
Product:  http://localhost:8081
Cart:     http://localhost:8082
Payment:  http://localhost:8083
Order:    http://localhost:8084
Kafka:    localhost:9092
```

### Database Connections
```
jdbc:mysql://localhost:3306/auth_db
jdbc:mysql://localhost:3306/product_db
jdbc:mysql://localhost:3306/cart_db
jdbc:mysql://localhost:3306/payment_db
jdbc:mysql://localhost:3306/order_db
```

### Kafka Topics
```
ORDER_EVENTS     (Active)
ORDER_CREATED    (Legacy)
```

---

**End of Documentation**

Generated: February 14, 2026  
Architecture: Microservices with Saga Pattern  
Author: System Documentation
