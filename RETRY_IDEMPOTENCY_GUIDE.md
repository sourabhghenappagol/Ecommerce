# Retry and Idempotency Implementation - Complete Guide

## Overview
This document provides a complete implementation guide for retry and idempotency mechanisms in the Payment Service Client.

---

## What Was Implemented

### 1. **Idempotency Entity** (`IdempotencyLog.java`)
Stores all payment requests and their responses to prevent duplicate charges.

**Key Fields:**
- `idempotencyKey` (UNIQUE): Unique identifier for each payment request
- `orderId`: Order being paid
- `requestBody`: The payment request payload
- `responseBody`: The payment service response
- `statusCode`: HTTP response status
- `status`: Request status (PENDING, SUCCESS, FAILED)
- `expiresAt`: When this entry expires (24 hours by default)

---

### 2. **Idempotency Repository** (`IdempotencyLogRepository.java`)
Handles database operations for idempotency logs.

**Methods:**
```java
findByIdempotencyKey(String)              // Check if request already processed
findByOrderIdAndIdempotencyKey(...)       // Find specific request for order
deleteExpiredEntries(LocalDateTime)       // Cleanup old entries
```

---

### 3. **Idempotency Service** (`IdempotencyService.java`)
Core business logic for idempotency management.

**Key Methods:**

```java
generateIdempotencyKey()              // Create new UUID
getIdempotencyLog(String)             // Retrieve cached response
createIdempotencyLog(...)             // Store incoming request
updateIdempotencyLog(...)             // Update with response
cleanupExpiredEntries()               // Remove old entries
```

---

### 4. **Enhanced PaymentServiceClient** 
Updated with complete retry + idempotency workflow:

#### **Request Flow:**
```
1. Generate/Receive Idempotency Key
   ↓
2. Check if request already processed (Cache hit → Return cached response)
   ↓
3. Create idempotency log entry (PENDING status)
   ↓
4. Call Payment Service with Idempotency-Key header
   ↓
5. Handle Response:
   - Success (200/201) → Update log as SUCCESS, cache response
   - Business Error (402) → Update log as FAILED, don't retry
   - Network Error → Retry with backoff (3 attempts max)
   ↓
6. After all retries exhausted → @Recover method returns fallback response
```

#### **Retry Mechanism:**
```java
@Retryable(
    value = { ResourceAccessException.class, RuntimeException.class },
    maxAttempts = 3,
    backoff = @Backoff(
        delay = 1000,           // Start with 1 second delay
        multiplier = 2.0,       // Double on each retry
        maxDelay = 5000         // Max 5 seconds
    )
)
```

**Retry Schedule:**
- Attempt 1: Immediate
- Attempt 2: After 1 second
- Attempt 3: After 2 seconds
- Attempt 4: After 4 seconds (max, would be 8 but capped at 5)

---

### 5. **Scheduled Cleanup** (`IdempotencyCleanupScheduler.java`)
Automatically removes expired idempotency entries every 6 hours.

```java
@Scheduled(fixedRateString = "${idempotency.cleanup.interval:21600000}")
public void cleanupExpiredIdempotencyLogs()
```

---

### 6. **Configuration** (`RetryConfiguration.java`)
Global retry mechanism setup:
```java
@EnableRetry       // Enables @Retryable annotation
@EnableScheduling  // Enables @Scheduled tasks
```

---

## How It Works

### **Scenario 1: First Payment Request**
```
Client sends: PaymentRequest(orderId=1, amount=100, idempotencyKey=uuid-xxx)
                          ↓
    Service checks cache → Not found
                          ↓
    Creates IdempotencyLog with status=PENDING
                          ↓
    Calls payment-service with header: Idempotency-Key: uuid-xxx
                          ↓
    Payment succeeds (200 OK)
                          ↓
    Updates IdempotencyLog: status=SUCCESS, responseBody={...}
                          ↓
    Returns: PaymentResponse(transactionId=123, status=SUCCESS)
```

### **Scenario 2: Duplicate Request (Same Idempotency Key)**
```
Client sends: PaymentRequest(orderId=1, amount=100, idempotencyKey=uuid-xxx)
                          ↓
    Service checks cache → FOUND (status=SUCCESS)
                          ↓
    Returns cached response immediately
                          ↓
    NO charge happens (Idempotent!)
```

### **Scenario 3: Network Timeout - Retries**
```
Attempt 1: Call fails with timeout
           Wait 1 second
                          ↓
Attempt 2: Retry - Call fails again
           Wait 2 seconds
                          ↓
Attempt 3: Retry - Call succeeds
           Update cache, return response
                          ↓
Attempt 4 (if needed): @Recover method returns fallback
```

---

## Configuration (application.yml)

```yaml
# Retry settings
retry:
  max-attempts: 3
  delay: 1000              # milliseconds
  max-delay: 5000          # milliseconds
  multiplier: 2.0

# Idempotency settings
idempotency:
  cleanup:
    interval: 21600000     # 6 hours in ms
  expiration-hours: 24     # Cache TTL

# Payment service URL
payment:
  service:
    url: http://localhost:8090
```

---

## Database Table

```sql
CREATE TABLE idempotency_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL,
    request_body TEXT NOT NULL,
    response_body TEXT,
    status_code INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    processed_at TIMESTAMP,
    status VARCHAR(50),
    INDEX idx_idempotency_key (idempotency_key),
    INDEX idx_order_id (order_id)
);
```

---

## Benefits

| Benefit | Explanation |
|---------|-------------|
| **Prevents Double Charging** | Same idempotency key = same result, safe retries |
| **Handles Network Failures** | Automatic retries with exponential backoff |
| **Reduces API Calls** | Caches successful responses |
| **Non-Destructive Retries** | Can safely retry without side effects |
| **Audit Trail** | All requests tracked in database |
| **Auto-Cleanup** | Scheduled task removes old entries |

---

## Usage Example

```java
// In OrderService or OrderController

PaymentRequest paymentRequest = new PaymentRequest(
    orderId,
    userId,
    amount
    // idempotencyKey is auto-generated if not provided
);

PaymentResponse response = paymentServiceClient.processPayment(orderId, paymentRequest);

if ("SUCCESS".equals(response.getStatus())) {
    // Process order
} else {
    // Handle payment failure
}
```

---

## Testing the Implementation

### Test Case 1: Duplicate Request Prevention
```java
// First request
paymentServiceClient.processPayment(1L, request1);

// Duplicate request with same idempotencyKey
PaymentResponse cached = paymentServiceClient.processPayment(1L, request1);

// Result: Cached response returned immediately, no duplicate charge
```

### Test Case 2: Retry on Timeout
```
Mock RestTemplate to throw TimeoutException on first attempt
First attempt: Fails
Wait 1 second
Second attempt: Succeeds
Response returned successfully
```

### Test Case 3: Cleanup Scheduler
```
Insert entries with past expiry dates
Wait for scheduled task to run (6 hours or trigger manually)
Verify expired entries are deleted
```

---

## Potential Enhancements

1. **Circuit Breaker Pattern**: Add Resilience4j for circuit breaking
2. **Distributed Cache**: Use Redis instead of DB for faster lookups
3. **Metrics**: Add micrometer for retry/idempotency metrics
4. **Dead Letter Queue**: Route failed payments to Kafka DLQ
5. **Webhook Notifications**: Notify payment service of retry attempts
6. **Custom Retry Listeners**: Track and log retry events
7. **Exponential Backoff Jitter**: Add randomization to prevent thundering herd

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Idempotency key already exists" | Check database, may need cleanup |
| Payment charged multiple times | Verify payment service supports idempotency-key header |
| High latency | Check database indexes on idempotency_key |
| OutOfMemory on retries | Ensure @Recover method is handling exceptions |
| Cleanup not running | Verify @EnableScheduling in configuration |

---

## Summary

✅ **Retry Mechanism**: Automatic retries with exponential backoff for transient failures
✅ **Idempotency**: Ensures same request produces same result regardless of retries
✅ **Persistence**: All requests tracked in database
✅ **Cleanup**: Automatic removal of old entries
✅ **Safety**: Business logic errors (402) not retried
✅ **Recovery**: Fallback method when all retries exhausted

