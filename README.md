
# Eclipse Vert.x Rate Limiter

configurable in-memory rate limiter

```kotlin
val clients: Storage = Storage.newInstance()
val throttling: ThrottleImpl = Throttl
        .getThrottling(vertx, null, clients)
        .includeHeaders(true) // will add rate limit headers to the response.
        .throttlingRequest(30) // requests limit.
        .throttlingTime(60) // time limit.
        .throttlingTimeUnit(TimeUnit.SECONDS) // time unit.
        
val router = throttling.getRouter()
```
