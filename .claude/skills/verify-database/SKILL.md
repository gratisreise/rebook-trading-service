---
name: verify-database
description: JPA 쿼리 성능, SQL 인젝션 방어, N+1 문제를 검증합니다. 엔티티/리포지토리 수정 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 엔티티 또는 리포지토리 파일]"
---

# 데이터베이스 검증

## 목적

Spring Data JPA 기반 애플리케이션의 데이터베이스 관련 코드의 보안과 성능을 검증합니다:

1. **SQL 인젝션 방어** — @Query 파라미터 바인딩, JPQL 안전성
2. **N+1 쿼리 문제** — 지연 로딩, 페치 조인, 배치 사이즈 확인
3. **인덱스 활용** — 쿼리 성능 최적화
4. **트랜잭션 관리** — @Transactional 적절성
5. **엔티티 설계** — JPA 어노테이션, 관계 매핑 적절성

## 실행 시점

- JPA 엔티티를 작성/수정한 후
- Spring Data Repository 메서드를 추가한 후
- JPQL/Native Query를 작성한 후
- 성능 이슈가 보고되었을 때
- Pull Request 생성 전

## 워크플로우

### Step 1: SQL 인젝션 취약점 탐지

**검사:** 네이티브 쿼리에서 문자열 결합으로 쿼리를 구성하는지 확인.

```bash
# 위험한 네이티브 쿼리 패턴 검색
grep -rn "nativeQuery.*=.*true" src/ --include="*.java"
grep -rn "@Query" src/ --include="*.java"
grep -rn "+.*FROM\|+.*WHERE\|+.*SELECT" src/ --include="*.java"
```

**위반 사례:**
```java
// 위험! — 문자열 결합
@Query(value = "SELECT * FROM trade WHERE user_id = " + userId, nativeQuery = true)
```

**PASS 기준:**
```java
// 안전 — 파라미터 바인딩
@Query("SELECT t FROM Trade t WHERE t.userId = :userId AND t.state = :state")
List<Trade> findByUserIdAndState(@Param("userId") Long userId, @Param("state") State state);

// Native Query도 파라미터 바인딩
@Query(value = "SELECT * FROM trade WHERE user_id = :userId", nativeQuery = true)
List<Trade> findByUserIdNative(@Param("userId") Long userId);
```

### Step 2: N+1 쿼리 문제 탐지

**검사:** 연관관계가 있는 엔티티에서 지연 로딩과 페치 조인이 적절히 사용되는지 확인.

```bash
# 연관관계 매핑 검색
grep -rn "@OneToMany\|@ManyToOne\|@ManyToMany\|@OneToOne" src/ --include="*.java"
grep -rn "FetchType.LAZY\|FetchType.EAGER" src/ --include="*.java"
grep -rn "JOIN FETCH\|EntityGraph\|@BatchSize" src/ --include="*.java"
```

**위반 사례:**
```java
// N+1 문제 발생! — EAGER 로딩
@OneToMany(fetch = FetchType.EAGER)
private List<TradeUser> tradeUsers;

// 루프 안에서 연관 엔티티 접근
for (Trade trade : trades) {
    trade.getTradeUsers().size(); // N번 추가 쿼리 발생
}
```

**PASS 기준:**
```java
// 지연 로딩 (기본 권장)
@OneToMany(fetch = FetchType.LAZY)
private List<TradeUser> tradeUsers;

// 페치 조인으로 한 번에 조회
@Query("SELECT t FROM Trade t JOIN FETCH t.tradeUsers WHERE t.userId = :userId")
List<Trade> findAllWithUsers(@Param("userId") Long userId);

// 또는 @BatchSize로 IN 쿼리
@BatchSize(size = 100)
@OneToMany(fetch = FetchType.LAZY)
private List<TradeUser> tradeUsers;
```

### Step 3: 인덱스 사용 검증

**검사:** WHERE, JOIN 조건으로 자주 사용되는 컬럼에 인덱스가 있는지 확인.

```bash
# 인덱스 정의 확인
grep -rn "@Index\|@Table.*indexes\|@Column(unique" src/ --include="*.java"
```

**PASS 기준:**
```java
// 엔티티에 인덱스 정의
@Entity
@Table(name = "trade", indexes = {
    @Index(name = "idx_trade_user_id", columnList = "user_id"),
    @Index(name = "idx_trade_book_id", columnList = "book_id"),
    @Index(name = "idx_trade_state", columnList = "state")
})
public class Trade { ... }
```

### Step 4: 트랜잭션 처리 검증

**검사:** @Transactional이 적절히 적용되었는지 확인.

```bash
# 트랜잭션 어노테이션 검색
grep -rn "@Transactional" src/ --include="*.java"
grep -rn "readOnly.*=.*true\|isolation\|propagation\|rollbackFor" src/ --include="*.java"
```

**PASS 기준:**
```java
// 읽기 전용 — 성능 최적화
@Transactional(readOnly = true)
public List<TradeResponse> getTrades(Long userId) { ... }

// 쓰기 작업 — 명시적 rollbackFor
@Transactional(rollbackFor = Exception.class)
public void createTrade(TradeRequest request) { ... }
```

### Step 5: 엔티티 설계 검증

**검사:** JPA 엔티티 어노테이션과 관계 매핑이 적절한지 확인.

```bash
# 엔티티 어노테이션 검색
grep -rn "@Entity\|@EntityListeners\|@CreatedDate\|@LastModifiedDate\|@EmbeddedId\|@Embeddable" src/ --include="*.java"
grep -rn "@Column\|@Enumerated\|@Lob" src/ --include="*.java"
```

**PASS 기준:**
```java
// 올바른 엔티티 설계
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private State state;

    // 복합키 패턴
    @Embeddable
    public class TradeUserId implements Serializable { ... }
}
```

## 결과 출력 형식

```markdown
## 데이터베이스 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| SQL 인젝션 | PASS/FAIL | N개 |
| N+1 쿼리 | PASS/FAIL | N개 |
| 인덱스 | PASS/FAIL | N개 |
| 트랜잭션 | PASS/FAIL | N개 |
| 엔티티 설계 | PASS/FAIL | N개 |

### 발견된 이슈

| 파일 | 라인 | 문제 | 심각도 |
|------|------|------|--------|
| `src/.../repository/TradeRepository.java:20` | N+1 쿼리 의심 | HIGH |
| `src/.../model/entity/Trade.java:45` | 인덱스 누락 | MEDIUM |
```

---

## 예외사항

1. **Spring Data 파생 쿼리** — 메서드 이름 기반 쿼리는 자동으로 파라미터 바인딩
2. **읽기 전용 쿼리** — 단순 조회는 트랜잭션 불필요할 수 있음
3. **배치 작업** — 대량 처리는 별도 트랜잭션 전략
4. **감사 로그** — Outbox 패턴은 트랜잭션에서 별도 관리
5. **Spring Data JPA 기본 메서드** — findById, save 등은 프레임워크가 관리

## Related Files

| File | Purpose |
|------|---------|
| `src/.../model/entity/**/*.java` | JPA 엔티티 |
| `src/.../repository/**/*.java` | Spring Data Repository |
| `src/.../domain/outbox/*.java` | Outbox 엔티티/리포지토리 |
| `src/main/resources/application*.yaml` | JPA/DB 설정 |