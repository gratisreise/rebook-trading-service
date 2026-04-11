---
name: verify-performance
description: 캐싱, 커넥션 풀, 비동기 처리 등 성능 최적화를 검증합니다. 성능 관련 코드 수정 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 파일 또는 성능 영역]"
---

# 성능 검증

## 목적

Spring Boot 백엔드 애플리케이션의 성능 최적화를 검증합니다:

1. **캐싱 전략** — Redis, @Cacheable 활용
2. **커넥션 풀** — HikariCP DB 커넥션 풀 설정
3. **비동기 처리** — @Async, CompletableFuture 활용
4. **페이지네이션** — 대용량 데이터 조회 최적화
5. **외부 API 호출** — Feign 클라이언트 타임아웃 및 재시도

## 실행 시점

- 성능 관련 코드를 수정한 후
- 캐싱 로직을 추가/변경한 후
- 대용량 데이터 처리 코드 작성 후
- Pull Request 생성 전
- 성능 저하 이슈 발생 시

## 워크플로우

### Step 1: 캐싱 구현 확인

**검사:** 반복 조회 데이터에 캐싱이 적용되었는지 확인.

```bash
# 캐싱 패턴 검색
grep -rn "@Cacheable\|@CacheEvict\|@CachePut\|@EnableCaching" src/ --include="*.java"
grep -rn "RedisTemplate\|StringRedisTemplate\|redisTemplate" src/ --include="*.java"
grep -rn "spring.cache\|spring.redis" src/main/resources/ --include="*.yaml" --include="*.yml" --include="*.properties"
```

**PASS 기준:**
```java
// Spring Cache 어노테이션 적용
@Cacheable(value = "trades", key = "#userId + '_' + #pageable.pageNumber")
public Page<Trade> findByUserId(Long userId, Pageable pageable) { ... }

// 캐시 무효화
@CacheEvict(value = "trades", key = "#userId")
public void updateTrade(Long userId, ...) { ... }
```

### Step 2: 커넥션 풀 설정 확인

**검사:** HikariCP DB 커넥션 풀이 적절히 설정되었는지 확인.

```bash
# 커넥션 풀 설정 검색
grep -rn "hikari\|maximum-pool-size\|minimum-idle\|connection-timeout\|pool-name" src/main/resources/ --include="*.yaml" --include="*.yml" --include="*.properties"
grep -rn "spring.datasource.hikari" src/main/resources/ --include="*.yaml" --include="*.yml"
```

**PASS 기준:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Step 3: 비동기 처리 확인

**검사:** 독립적인 작업이 비동기로 실행되는지 확인.

```bash
# 비동기 패턴 검색
grep -rn "@Async\|@EnableAsync\|CompletableFuture\|ExecutorService\|TaskExecutor" src/ --include="*.java"
```

**위반 사례:**
```java
// 동기 처리 (느림) — 알림 발행과 응답이 순차 실행
public TradeResponse createTrade(...) {
    trade = tradeWriter.save(trade);
    notificationPublisher.publish(message); // 응답을 지연시킴
    return new TradeResponse(trade);
}
```

**PASS 기준:**
```java
// @Async로 비동기 알림 발행
@Async
public void publishNotification(NotificationTradeMessage message) {
    rabbitTemplate.convertAndSend(exchange, routingKey, message);
}

// 또는 CompletableFuture
CompletableFuture.runAsync(() -> notificationPublisher.publish(message));
```

### Step 4: 페이지네이션 최적화 확인

**검사:** 대용량 데이터 조회에 페이지네이션이 적용되었는지 확인.

```bash
# 페이지네이션 패턴 검색
grep -rn "Pageable\|@PageableDefault\|Page<\|PageRequest\|Slice<" src/ --include="*.java"
grep -rn "findAll\|findBy" src/ --include="*.java" | grep -v "ById"
```

**위반 사례:**
```java
// 전체 조회 (메모리 위험)
List<Trade> trades = tradeRepository.findAll();
```

**PASS 기준:**
```java
// 페이지네이션 적용
@GetMapping
public ListResult<TradeResponse> getTrades(
    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<Trade> trades = tradeRepository.findAll(pageable);
    return responseService.getPageResponse(trades.map(TradeResponse::new));
}
```

### Step 5: Feign 클라이언트 타임아웃 확인

**검사:** 외부 API 호출에 타임아웃과 재시도가 설정되어 있는지 확인.

```bash
# Feign 설정 검색
grep -rn "feign\|FeignClient\|connectTimeout\|readTimeout\|retry" src/ --include="*.java" --include="*.yaml" --include="*.yml"
```

**PASS 기준:**
```yaml
# application.yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
```

### Step 6: JPA 쿼리 최적화 확인

**검사:** 불필요한 데이터 조회가 없는지 확인.

```bash
# 쿼리 최적화 패턴 검색
grep -rn "SELECT\s.*FROM\|@Query" src/ --include="*.java"
grep -rn "findAll\|findBy.*In(" src/ --include="*.java"
```

**위반 사례:**
```java
// 불필요한 전체 컬럼 조회
@Query("SELECT t FROM Trade t")
```

**PASS 기준:**
```java
// 필요한 컬럼만 조회 (DTO 프로젝션)
@Query("SELECT new com.example.TradeResponse(t.id, t.title, t.price) FROM Trade t WHERE t.state = :state")
```

## 결과 출력 형식

```markdown
## 성능 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| 캐싱 전략 | PASS/FAIL | N개 |
| 커넥션 풀 | PASS/FAIL | N개 |
| 비동기 처리 | PASS/FAIL | N개 |
| 페이지네이션 | PASS/FAIL | N개 |
| Feign 타임아웃 | PASS/FAIL | N개 |
| JPA 쿼리 | PASS/FAIL | N개 |
```

---

## 예외사항

1. **실시간 데이터** — 항상 최신이어야 하는 데이터는 캐싱 부적절
2. **소량 데이터** — 캐싱 오버헤드가 이득보다 큰 경우
3. **순차 의존성** — 이전 결과가 필요한 작업은 비동기 불가
4. **개발 환경** — 로컬 개발에서는 최적화 완화 가능
5. **Spring Cloud Config** — 설정 값은 Config Server에서 관리

## Related Files

| File | Purpose |
|------|---------|
| `src/.../config/*.java` | 설정 클래스 (캐시, 스케줄러 등) |
| `src/.../repository/**/*.java` | Repository (쿼리) |
| `src/.../service/**/*.java` | 서비스 레이어 (비즈니스 로직) |
| `src/.../clientfeign/**/*.java` | Feign 클라이언트 |
| `src/main/resources/application*.yaml` | 성능 관련 설정 |
