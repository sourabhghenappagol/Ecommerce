# Quick Reference: Retry & Idempotency

## All Steps Required (Summary)

### **Step 1: Create Database Entity**
- File: `IdempotencyLog.java`
- Stores: request → response mappings
- Why: Need persistent storage to detect duplicate requests

### **Step 2: Create Repository**
- File: `IdempotencyLogRepository.java`
- Methods: findByIdempotencyKey, deleteExpiredEntries
- Why: Database access layer

### **Step 3: Create Service Logic**
- File: `IdempotencyService.java`
- Methods: generateKey, getLog, createLog, updateLog, cleanup
- Why: Encapsulates business logic

### **Step 4: Enable Retry Globally**
- File: `RetryConfiguration.java`
- Annotations: @EnableRetry, @EnableScheduling
- Why: Activate Spring retry framework

### **Step 5: Enhance Payment Client**
- File: `PaymentServiceClient.java`
- Add: @Retryable, @Backoff, @Recover
- Add: Idempotency key header
- Why: Implement retry + idempotency workflow

### **Step 6: Update DTO**
- File: `PaymentRequest.java`
- Add: idempotencyKey field
- Why: Carry key through API

### **Step 7: Add Scheduler**
- File: `IdempotencyCleanupScheduler.java`
- Task: Delete expired entries every 6 hours
- Why: Prevent database bloat

### **Step 8: Configure Application**
- File: `application.yml`
- Add: retry & idempotency settings
- Why: Externalize configuration

---

## Code Patterns

### **Pattern 1: Generate Idempotency Key**
```java
String idempotencyKey = idempotencyService.generateIdempotencyKey();
request.setIdempotencyKey(idempotencyKey);
```

### **Pattern 2: Check Cache Before Call**
```java
Optional<IdempotencyLog> cached = idempotencyService.getIdempotencyLog(key);
if (cached.isPresent() && "SUCCESS".equals(cached.get().getStatus())) {
    return objectMapper.readValue(cached.get().getResponseBody(), PaymentResponse.class);
}
```

### **Pattern 3: Add Header to Request**
```java
HttpHeaders headers = new HttpHeaders();
headers.set("Idempotency-Key", idempotencyKey);
org.springframework.http.HttpEntity<PaymentRequest> entity = 
    new org.springframework.http.HttpEntity<>(request, headers);
```

### **Pattern 4: Update Cache on Success**
```java
idempotencyService.updateIdempotencyLog(
    idempotencyKey, 
    paymentResponse, 
    response.getStatusCode().value(), 
    "SUCCESS"
);
```

### **Pattern 5: Retry Annotation**
```java
@Retryable(
    value = { ResourceAccessException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 5000)
)
public PaymentResponse processPayment(...) { ... }
```

### **Pattern 6: Recovery Fallback**
```java
@Recover
public PaymentResponse recoverPayment(ResourceAccessException e, ...) {
    PaymentResponse response = new PaymentResponse();
    response.setStatus("FAILED");
    response.setMessage("Service unavailable");
    return response;
}
```

---

## Retry Strategy Breakdown

```
Retryable Exceptions:
├── ResourceAccessException (network issues)
└── RuntimeException (general errors)

Non-Retryable Exceptions:
├── HttpClientErrorException (4xx)
└── HttpServerErrorException (5xx business logic)

Backoff Strategy:
├── Initial Delay: 1000ms
├── Multiplier: 2.0x
├── Max Delay: 5000ms
└── Max Attempts: 3
```

---

## Idempotency Workflow

```
Request Arrives
    ↓
1. Generate/Get Idempotency Key
2. Query Cache (DB)
   - Found? Return cached + 200
   - Not Found? Continue
3. Create Log Entry (Status: PENDING)
4. Call External Service
   - Success? Update log (SUCCESS), cache response
   - Failure? Update log (FAILED)
   - Network Error? Retry (max 3)
5. Return Response
```

---

## Configuration Reference

```yaml
# Retry tuning
retry:
  max-attempts: 3        # Number of attempts
  delay: 1000            # Initial delay (ms)
  max-delay: 5000        # Maximum delay (ms)
  multiplier: 2.0        # Exponential multiplier

# Idempotency tuning
idempotency:
  cleanup:
    interval: 21600000   # Cleanup every 6 hours (ms)
  expiration-hours: 24   # Cache entries live 24 hours

# Payment service
payment:
  service:
    url: http://localhost:8090
```

---

## Testing Checklist

- [ ] Database table created (idempotency_log)
- [ ] First payment request succeeds
- [ ] Second request with same key returns cached response
- [ ] Database shows only 1 entry for duplicate request
- [ ] Mock timeout - verify retry attempts
- [ ] All retries fail - verify @Recover called
- [ ] 402 error - verify not retried
- [ ] Cleanup scheduler removes expired entries

---

## Error Handling Matrix

| Exception | Action |
|-----------|--------|
| ResourceAccessException | ✓ Retry (network error) |
| SocketTimeoutException | ✓ Retry (implicit) |
| HttpStatus 402 | ✗ Don't retry (business logic) |
| HttpStatus 500 | ~ May retry (server error) |
| HttpStatus 503 | ✓ Retry (service unavailable) |
| All Retries Failed | → @Recover method |

---

## Performance Optimization Tips

1. **Use Redis** for idempotency cache (faster than DB)
2. **Add Circuit Breaker** (Resilience4j) for payment service
3. **Monitor metrics**: retry attempts, cache hits, failures
4. **Adjust backoff** based on actual payment service latency
5. **Batch cleanup** instead of individual deletes
6. **Connection pooling** for database

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Payment charged twice | Verify payment service supports Idempotency-Key header |
| High latency | Add indexes on idempotency_log.idempotency_key |
| Cleanup not working | Check @EnableScheduling in config |
| Too many retries | Increase delay or reduce maxAttempts |
| Cache miss | Verify exception isn't thrown after successful update |

---

## Integration Example

```java
@Service
public class OrderService {
    
    private final PaymentServiceClient paymentClient;
    
    public Order createOrder(CreateOrderRequest request) {
        PaymentRequest paymentRequest = new PaymentRequest(
            orderId,
            userId,
            amount
        );
        
        // Service handles retry + idempotency automatically
        PaymentResponse payment = paymentClient.processPayment(orderId, paymentRequest);
        
        if ("SUCCESS".equals(payment.getStatus())) {
            order.setPaymentId(payment.getTransactionId());
            return orderRepository.save(order);
        } else {
            throw new PaymentFailedException("Payment failed");
        }
    }
}
```

