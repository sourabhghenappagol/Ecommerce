# E-Commerce Architecture - Visual Diagrams & Implementation Examples

---

## Section 1: Current Architecture Visualization

### Current System Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT APPLICATION                         │
│                    (React/Angular/Postman)                         │
└────────────┬────────────────────────────────────────────────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌─────────────┐   ┌──────────────┐
│   Auth      │   │  Products    │
│   Service   │   │   Service    │
│   (8080)    │   │   (8081)     │
└─────────────┘   └──────────────┘
    │                 │
    │                 ▼
    │            ┌──────────────┐
    │            │  Cart Data   │
    │            │   Cached     │
    │            └──────────────┘
    │
    ▼
┌─────────────┐   SYNC CALLS    ┌──────────────┐
│   Cart      ├────────────────▶│   Product    │
│   Service   │                 │   Service    │
│   (8082)    │◀────────────────┤   (8081)     │
└──────┬──────┘                 └──────────────┘
       │
       │ Fetch cart
       │
       ▼
┌─────────────────────────────────────────────┐
│          ORDER SERVICE (8084)               │
│                                             │
│  1. Get cart from Cart Service (REST)      │
│  2. Create order in DB                     │
│  3. Call Payment Service (REST)            │
│  4. Publish Kafka event                    │
└────────────────┬────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
    ┌─────────┐      ┌──────────────┐
    │Payment  │      │  KAFKA       │
    │Service  │      │  MESSAGE     │
    │(8083)   │      │  BROKER      │
    └─────────┘      └────────┬─────┘
                              │
                              ▼
                        ┌──────────────┐
                        │ Cart Service │
                        │  Consumer    │
                        │              │
                        │ Clear Cart   │
                        │ (Async)      │
                        └──────────────┘
```

---

## Section 2: Proposed Kafka-Enhanced Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                      KAFKA MESSAGE BROKER                        │
│  ┌────────────────┬────────────────┬────────────────┬───────────┐│
│  │ ORDER_EVENTS   │NOTIFICATION_   │ INVENTORY_    │ANALYTICS_ ││
│  │ Topic          │EVENTS Topic    │EVENTS Topic   │EVENTS Topic
│  │                │                │                │           ││
│  │- ORDER_PLACED  │- PAYMENT_INIT  │- STOCK_       │- USER_    ││
│  │- ORDER_PAID    │- PAYMENT_SUCC  │  RESERVED     │  VIEWED   ││
│  │- ORDER_SHIP    │- ORDER_CONFIRM │- STOCK_       │- PRODUCT_ ││
│  │- ORDER_DELIVER │- REFUND_INIT   │  ALLOCATED    │  ADDED    ││
│  │- ORDER_CANCEL  │- CANCEL_NOTIF  │- STOCK_       │- ORDER_   ││
│  │                │                │  RELEASED     │  COMPLETE ││
│  └────────────────┴────────────────┴────────────────┴───────────┘│
└──────────────┬───────────────────┬─────────────────┬─────────────┘
               │                   │                 │
               │                   │                 │
    ┌──────────▼─────────┐  ┌──────▼──────────┐  ┌──▼────────────┐
    │  NOTIFICATION      │  │  INVENTORY     │  │  ANALYTICS    │
    │  SERVICE (New)     │  │  SERVICE (New) │  │  SERVICE (New)│
    │                    │  │                │  │               │
    │ ├─ Email           │  │├─ Stock Mgmt   │  │├─ Dashboard   │
    │ ├─ SMS             │  │├─ Reservation  │  │├─ Reports     │
    │ └─ Push Notif      │  │└─ Expiry Job   │  │└─ Metrics     │
    └────────────────────┘  └────────────────┘  └───────────────┘

    ┌──────────────────────────────────────────────────────────────┐
    │         CORE SERVICES (Enhanced with Events)                │
    │                                                              │
    │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
    │  │Auth      │  │Product   │  │Cart      │  │Order     │  │
    │  │Service   │  │Service   │  │Service   │  │Service   │  │
    │  │          │  │          │  │          │  │          │  │
    │  │ Publishes│  │ Publishes│  │ Publishes│  │ Publishes│  │
    │  │USER_REG  │  │PRODUCT_  │  │PRODUCT_  │  │ORDER_*   │  │
    │  │          │  │VIEWED    │  │ADDED     │  │PAYMENT_* │  │
    │  │          │  │          │  │          │  │          │  │
    │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
    │                                                              │
    │  ┌──────────┐  ┌──────────────────┐                        │
    │  │Payment   │  │Fraud Detection   │                        │
    │  │Service   │  │Service (New)     │                        │
    │  │          │  │                  │                        │
    │  │ Publishes│  │├─ Velocity Check │                        │
    │  │PAYMENT_* │  │├─ Geo Anomaly    │                        │
    │  │          │  │└─ Amount Anomaly │                        │
    │  │          │  │                  │                        │
    │  └──────────┘  └──────────────────┘                        │
    │                                                              │
    └──────────────────────────────────────────────────────────────┘
```

---

## Section 3: Kafka Events Topology

```
PUBLISHING SERVICES          TOPICS                CONSUMING SERVICES
────────────────────────────────────────────────────────────────────

Auth Service                 
  └─ USER_REGISTERED ──────▶ USER_EVENTS ──────▶ Analytics Service
  └─ USER_ROLE_UPDATED                   ──────▶ Notification Service


Product Service              
  └─ PRODUCT_VIEWED ────────▶ PRODUCT_EVENTS ──▶ Analytics Service
  └─ PRODUCT_CREATED                      ──▶ Recommendation Engine
  └─ PRODUCT_UPDATED                      ──▶ Cache Invalidator


Cart Service                 
  └─ PRODUCT_ADDED_TO_CART ▶ CART_EVENTS ────▶ Analytics Service
  └─ PRODUCT_REMOVED      /                  ▶ Recommendation Engine
  └─ CART_CLEARED         /


Order Service                
  ├─ ORDER_PLACED ────────────▶ ORDER_EVENTS ──▶ Notification Service
  ├─ ORDER_PAYMENT_PENDING    /                ▶ Analytics Service
  ├─ ORDER_PAID ──────────────┐
  │                           │
  │ [Async Stock Check]       │
  ├─ ORDER_SHIPPED ───────────┼─▶ ORDER_EVENTS ──▶ Notification Service
  ├─ ORDER_DELIVERED ────────┐│                 ▶ Analytics Service
  ├─ ORDER_CANCELLED ────────┼┼─▶ ORDER_EVENTS ──▶ Cart Service (keep)
  └─ ORDER_REFUNDED ────────┐││                 ▶ Inventory Service
                             │││
Payment Service              ││
  ├─ PAYMENT_INITIATED ─────┼┼┼▶ PAYMENT_EVENT ▶ Fraud Detection
  ├─ PAYMENT_PROCESSING ───┐│││                ▶ Analytics Service
  │ [Fraud Check via Kafka] │││
  ├─ PAYMENT_SUCCESS ──────┐│││
  ├─ PAYMENT_FAILED ───────┼┼┼▶ PAYMENT_EVENT ▶ Notification Service
  ├─ PAYMENT_REFUNDED ────┐│││                ▶ Analytics Service
  └─ PAYMENT_GATEWAY_ERROR ││


Inventory Service (New)      
  ├─ STOCK_RESERVED ───────┐▶ INVENTORY_EVENT ▶ Product Service Cache
  ├─ STOCK_ALLOCATED ──────┤                  ▶ Analytics Service
  └─ STOCK_RELEASED ──────┘


Notification Service (New)   
  └─ Consumes ALL events ───▶ EMAIL/SMS/PUSH


Analytics Service (New)      
  └─ Consumes ALL events ───▶ Analytics DB
                              Reports API


Recommendation Engine (New)  
  └─ Tracks user behavior ─▶ Recommendation API


Fraud Detection (New)        
  └─ Analyzes transactions ▶ FRAUD_ALERT events
```

---

## Section 4: Request Flow Diagrams

### Current Order Processing Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                      USER PLACES ORDER                          │
│                   POST /api/orders (JWT)                        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
            ┌─────────────────────────────┐
            │   OrderController           │
            │   placeOrder()              │
            └────────────┬────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
    [SYNC]          [SYNC]          [ASYNC]
    1ms-5ms         100-500ms       (parallel)
         │               │               │
         │               │               └─→ [Kafka Event Producer]
         │               │                    └─→ ORDER_PLACED event
         │               │
         ▼               ▼
    [CartService]   [OrderService]
    getCart()       placeOrder()
         │               │
         │               ▼
         │          [OrderSagaOrchestrator]
         │          executeOrderSaga()
         │               │
         │        ┌──────┴──────┐
         │        │             │
         │        ▼             ▼
         │    [CREATE]      [PROCESS PAYMENT]
         │    Order         (SYNC - 500-2000ms)
         │    (PENDING)         │
         │                      │
         │              ┌───────┴────────┐
         │              │                │
         │              ▼                ▼
         │          [SUCCESS]        [FAILURE]
         │          80%              20%
         │              │                │
         │              ▼                ▼
         │          Update Order     Cancel Order
         │          to PAID          CANCELLED
         │              │                │
         └──────────────┴────────────────┘
                        │
                        ▼
              ┌──────────────────────┐
              │ Kafka: ORDER_EVENTS  │
              │                      │
              │ ├─ ORDER_COMPLETED   │
              │ │   (on success)     │
              │ │                    │
              │ └─ ORDER_CANCELLED   │
              │     (on failure)     │
              └──────────┬───────────┘
                         │
                         ▼
            ┌────────────────────────┐
            │ Cart Service Consumer  │
            │ (OrderEventConsumer)   │
            │                        │
            │ ├─ handleOrderCompleted│
            │ │   └─ clearCart()     │
            │ │   (async)            │
            │ │                      │
            │ └─ handleOrderCancelled│
            │     └─ keepCart()      │
            │     (async)            │
            └────────────────────────┘

TOTAL TIME: ~1500-2500ms (mostly payment processing)
```

### Optimized Order Processing Flow (With Caching + Async)

```
┌─────────────────────────────────────────────────────────────────┐
│                      USER PLACES ORDER                          │
│                   POST /api/orders (JWT)                        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
            ┌─────────────────────────────┐
            │   OrderController           │
            │   placeOrder()              │
            └────────────┬────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
    [CACHE]         [ASYNC]         [KAFKA]
    <5ms            <100ms          (immediate)
         │               │               │
         │               │               └─→ [Kafka Event Producer]
         │               │                    ├─ ORDER_PLACED
         │               │                    ├─ PAYMENT_INITIATED
         │               │                    └─ INVENTORY_CHECK
         │               │
         ▼               ▼
    [CartService]   [OrderService]
    getCart()       placeOrder()
    (from Cache)    (returns quickly)
         │               │
         │               ▼
         │          [ORDER STATUS: PENDING]
         │          Return to user
         │          
         │          ┌──────────────────────┐
         │          │ BACKGROUND PROCESSING│
         │          │ (Non-blocking)       │
         │          │                      │
         │          │ 1. Verify Stock      │
         │          │    (via Kafka event) │
         │          │                      │
         │          │ 2. Process Payment   │
         │          │    (async executor)  │
         │          │                      │
         │          │ 3. Update Order      │
         │          │    Status            │
         │          │                      │
         │          │ 4. Publish Events    │
         │          └──────────────────────┘
         │
         └────────────────────┬────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Return to Client │
                    │                  │
                    │ {               │
                    │  orderId: 123,  │
                    │  status: PENDING│
                    │ }               │
                    │                  │
                    │ HTTP 202        │
                    │ (Accepted)      │
                    └──────────────────┘

TOTAL TIME: ~50-100ms (immediate response!)
Background processing happens async
```

---

## Section 5: Stock Management Flow (NEW SERVICE)

```
┌────────────────────────────────────────────────────────────────┐
│                    ADD PRODUCT TO CART                         │
│             POST /api/cart/add (with productId, qty)           │
└───────────────────────┬────────────────────────────────────────┘
                        │
                        ▼
           ┌─────────────────────────┐
           │  CartService.addItem()  │
           └───────────┬─────────────┘
                       │
         ┌─────────────┼──────────────┐
         │             │              │
         ▼             ▼              ▼
    [Get Item]   [Kafka]         [Update Cart]
    from Product  Publish         DB
    Service       PRODUCT_
    (Cached)      ADDED_TO_CART
         │             │              │
         │             └─────┬────────┘
         │                   │
         ▼                   ▼
    [Response]          ┌──────────────────────┐
    to Client           │  Inventory Service   │
                        │  (Event Consumer)    │
                        │                      │
                        │ Listen to:           │
                        │ PRODUCT_ADDED_TO_CAR│
                        └──────────┬───────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │ Check Stock:         │
                        │                      │
                        │ Stock available?     │
                        │   ├─ YES: Reserve    │
                        │   │   - Create       │
                        │   │     reservation  │
                        │   │   - Set TTL: 30m │
                        │   │                  │
                        │   └─ NO: Notify      │
                        │       - Publish     │
                        │         OUT_OF_STOCK│
                        └──────────┬───────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
                    ▼                             ▼
         ┌──────────────────────┐    ┌─────────────────────┐
         │ Kafka: STOCK_RESERVED│    │ Kafka: STOCK_      │
         │ Event                │    │ UNAVAILABLE Event   │
         │                      │    │                     │
         │ {                    │    │ {                   │
         │   productId: 1,      │    │   productId: 5,     │
         │   username: john,    │    │   message: "Out of  │
         │   quantity: 2,       │    │   stock"            │
         │   reservationId: xyz │    │ }                   │
         │ }                    │    │                     │
         └──────────┬───────────┘    └─────────┬───────────┘
                    │                          │
         ┌──────────▼──────────┐              │
         │ Product Service     │              │
         │ Cache Invalidator   │              │
         │                     │              │
         │ ├─ Get event        │              │
         │ ├─ Invalidate stock │              │
         │ │   cache for       │              │
         │ │   product: 1      │              │
         │ └─ Next request     │              │
         │   gets fresh data   │              │
         └─────────────────────┘              │
                                              │
                                   ┌──────────▼──────────────┐
                                   │ Notification Service    │
                                   │                         │
                                   │ ├─ Get OUT_OF_STOCK     │
                                   │ ├─ Send Email to user   │
                                   │ │   "Product unavail"   │
                                   │ └─ Suggest alternatives │
                                   └─────────────────────────┘


WHEN USER PLACES ORDER:
┌─────────────────────────────────────────────────────────────────┐
│              Check Reservation on Order                         │
│                                                                 │
│  OrderService receives cart items:                             │
│  └─ For each item:                                             │
│    └─ Check if reservationId valid                             │
│      ├─ Valid & Not Expired → Allocate stock                   │
│      │  └─ Publish: STOCK_ALLOCATED event                      │
│      │                                                          │
│      └─ Expired or Invalid → Out of stock error                │
│         └─ Cancel order, keep cart                             │
└─────────────────────────────────────────────────────────────────┘

WHEN ORDER CANCELLED:
┌─────────────────────────────────────────────────────────────────┐
│         Inventory Service Releases Stock                        │
│                                                                 │
│  Listen to: ORDER_CANCELLED event                              │
│  └─ For each item:                                             │
│    └─ Remove allocation                                        │
│    └─ Release reservation                                      │
│    └─ Publish: STOCK_RELEASED event                            │
│       └─ Product Service updates cache                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Section 6: Notification Flow (NEW SERVICE)

```
All Services publish domain events → Notification Service consumes

┌──────────────────────────────────────┐
│     AUTH SERVICE                     │
│                                      │
│  ├─ USER_REGISTERED event            │
│  │   {                                │
│  │     userId: 123,                   │
│  │     email: john@example.com,       │
│  │     username: john,                │
│  │     timestamp: 2026-04-01T10:00Z   │
│  │   }                                │
│  │                                    │
│  └─ USER_PASSWORD_RESET event        │
│      {                                │
│        userId: 123,                   │
│        email: john@example.com,       │
│        resetToken: xyz...             │
│      }                                │
└──────────────────┬───────────────────┘
                   │
                   ▼
         ┌─────────────────────┐
         │  NOTIFICATION       │
         │  SERVICE (New)      │
         │                     │
         │  Consumes all       │
         │  domain events      │
         │                     │
         │  Routes to:         │
         │  ├─ Email Service   │
         │  ├─ SMS Service     │
         │  └─ Push Service    │
         └──────────┬──────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
        ▼           ▼           ▼
    ┌────────┐ ┌────────┐ ┌─────────┐
    │ EMAIL  │ │  SMS   │ │  PUSH   │
    │        │ │        │ │NOTIF    │
    │SendGrid│ │ Twilio │ │Firebase │
    │ API    │ │  API   │ │  Cloud  │
    └────────┘ └────────┘ └─────────┘


NOTIFICATION TEMPLATES:
┌────────────────────────────────────────────────────────────┐
│                   USER_REGISTERED                          │
│  ┌────────────────────────────────────────────────────┐   │
│  │ EMAIL:                                             │   │
│  │ Subject: Welcome to ECommerce Store!              │   │
│  │ Body: Hi John,                                     │   │
│  │       Welcome! Your account is active.             │   │
│  │       Start shopping now!                          │   │
│  │                                                    │   │
│  │ SMS:                                               │   │
│  │ "Welcome to ECommerce! Your account is ready.      │   │
│  │  Login at ecommerce.com"                           │   │
│  │                                                    │   │
│  │ PUSH:                                              │   │
│  │ Title: Welcome to ECommerce                        │   │
│  │ Message: Start shopping and earn rewards!          │   │
│  └────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                   PAYMENT_SUCCESS                          │
│  ┌────────────────────────────────────────────────────┐   │
│  │ EMAIL:                                             │   │
│  │ Subject: Payment Confirmed - Order #123           │   │
│  │ Body: Hi John,                                     │   │
│  │       Your payment has been processed.             │   │
│  │       Order #123 - $2089.95                        │   │
│  │       Expected delivery: April 5, 2026             │   │
│  │                                                    │   │
│  │ SMS:                                               │   │
│  │ "Order #123 confirmed. Amount: $2089.95.           │   │
│  │  Tracking: http://track.com/123"                   │   │
│  │                                                    │   │
│  │ PUSH:                                              │   │
│  │ Title: Order Confirmed!                            │   │
│  │ Message: Order #123 - Delivery in 4 days           │   │
│  └────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                   PAYMENT_FAILED                           │
│  ┌────────────────────────────────────────────────────┐   │
│  │ EMAIL:                                             │   │
│  │ Subject: Payment Failed - Please Try Again         │   │
│  │ Body: Hi John,                                     │   │
│  │       Your payment attempt failed.                 │   │
│  │       Reason: Insufficient funds                   │   │
│  │       Your cart is saved. Try again!               │   │
│  │                                                    │   │
│  │ SMS:                                               │   │
│  │ "Payment failed: Insufficient funds.               │   │
│  │  Cart saved. Retry: ecommerce.com/cart"            │   │
│  │                                                    │   │
│  │ PUSH:                                              │   │
│  │ Title: Payment Failed                              │   │
│  │ Message: Cart saved. Retry checkout                │   │
│  └────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
```

---

## Section 7: Performance Optimization Timeline

```
BEFORE OPTIMIZATION:
┌─────────────────────────────────────────────────────────┐
│  Concurrent Users: 100                                  │
│                                                         │
│  Request Timeline:                                      │
│  ├─ Get Cart (from DB):              150ms              │
│  ├─ Create Order (DB write):         100ms              │
│  ├─ Process Payment (sync REST):    1200ms              │
│  ├─ Update Order (DB write):         100ms              │
│  └─ Publish Kafka event:              50ms              │
│                                                         │
│  TOTAL: ~1600ms                                        │
│  Throughput: 100 requests/1.6s = 62 req/s              │
│  P99 Latency: 3500ms (due to queueing)                 │
│  Error Rate: 5% (timeouts)                             │
└─────────────────────────────────────────────────────────┘

AFTER OPTIMIZATION:
┌─────────────────────────────────────────────────────────┐
│  Concurrent Users: 100                                  │
│                                                         │
│  Request Timeline (PARALLEL):                           │
│  ├─ Get Cart (from Redis cache):      5ms              │
│  ├─ Create Order (with indexing):    50ms              │
│  ├─ Process Payment (async, bg):    100ms (after)      │
│  ├─ Fraud Check (async, bg):        100ms (after)      │
│  └─ Publish Kafka event:              10ms              │
│                                                         │
│  SYNC TIME: ~65ms                                      │
│  ASYNC TIME: ~200ms (background)                       │
│  Throughput: 100 requests/0.065s = 1538 req/s (+2400%)│
│  P99 Latency: 200ms (25x faster!)                      │
│  Error Rate: 0.1% (only real failures)                 │
└─────────────────────────────────────────────────────────┘

GRAPH:
Request Latency (ms)
│
4000├─ ●●●●●●●●●  BEFORE
3500├  ●  ●  ●
3000├
2500├        ●●●●●●●
2000├        ●  ●  ●
1500├────────────────●●●●●●
1000├
 500├
 250├─ ▲▲▲▲▲▲▲▲▲  AFTER
 100├ ▲  ▲  ▲
  50├ ▲  ▲  ▲
   0└─────────────────────
     Time (10min interval)

Legend:
● = Individual requests (BEFORE)
▲ = Individual requests (AFTER)
```

---

## Section 8: Technology Stack Summary

```
┌──────────────────────────────────────────────────────────┐
│              TECHNOLOGY LAYERS                           │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  APPLICATION LAYER:                                    │
│  ├─ Spring Boot 3.1.5 (Framework)                      │
│  ├─ Spring Security 6.x (Authentication)              │
│  ├─ Spring Data JPA (ORM)                             │
│  └─ Swagger/OpenAPI 3.0 (Documentation)               │
│                                                          │
│  MESSAGING LAYER:                                      │
│  ├─ Apache Kafka 3.x (Event Streaming)                │
│  ├─ Spring Kafka (Spring Integration)                 │
│  └─ Zookeeper 3.x (Kafka Coordination)                │
│                                                          │
│  CACHING LAYER:                                        │
│  ├─ Redis 7.x (Distributed Cache)                     │
│  └─ Spring Data Redis (Spring Integration)            │
│                                                          │
│  DATABASE LAYER:                                       │
│  ├─ MySQL 8.x (Primary DB)                            │
│  ├─ HikariCP 5.x (Connection Pooling)                │
│  └─ Hibernate 6.x (Query Optimization)                │
│                                                          │
│  API GATEWAY LAYER (Future):                           │
│  ├─ Spring Cloud Gateway 4.x                          │
│  ├─ Spring Cloud Load Balancer                        │
│  └─ Resilience4j (Circuit Breaker)                    │
│                                                          │
│  MONITORING LAYER (Future):                            │
│  ├─ Micrometer (Metrics)                              │
│  ├─ Prometheus (Time Series DB)                       │
│  ├─ Grafana (Visualization)                           │
│  ├─ Zipkin/Jaeger (Distributed Tracing)               │
│  └─ ELK Stack (Logging)                               │
│                                                          │
│  DEPLOYMENT:                                           │
│  ├─ Docker (Containerization)                         │
│  ├─ Docker Compose (Local orchestration)              │
│  └─ Kubernetes (Future: Production)                   │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

**End of Diagrams & Implementation Examples**

