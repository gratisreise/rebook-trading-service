---
name: verify-api-design
description: RESTful API 설계 원칙과 엔드포인트 명명 규칙을 검증합니다. API 엔드포인트 추가/수정 후, PR 전 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 Controller 파일 또는 경로]"
---

# API 설계 검증

## 목적

Spring Boot 백엔드 API의 설계 품질과 일관성을 검증합니다:

1. **RESTful 원칙 준수** — 리소스 기반 URL, 올바른 HTTP 메서드 사용
2. **엔드포인트 명명 규칙** — kebab-case, 복수형 리소스
3. **요청/응답 구조** — `SuccessResponse<T>`/`PageResponse<T>` 일관된 응답 래핑 (common-core)
4. **인증** — `@PassportUser` 기반 사용자 식별 (common-auth)

## 실행 시점

- 새로운 API 엔드포인트를 추가한 후
- 기존 API를 수정하거나 리팩토링한 후
- Pull Request 생성 전

## 워크플로우

### Step 1: RESTful URL 패턴 검증

**검사:** 엔드포인트 URL이 RESTful 원칙을 따르는지 확인.

```bash
# Spring MVC 매핑 어노테이션 검색
grep -rn "@RequestMapping\|@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping\|@PatchMapping" src/ --include="*.java"
```

**위반 사례:**
```java
// 위반
@GetMapping("/getTrades")     // URL에 동사 사용
@PostMapping("/createTrade")  // URL에 동사 사용
@GetMapping("/trade")         // 단수형 리소스
```

**PASS 기준:**
```java
// 올바른 RESTful URL
@RestController
@RequestMapping("/api/trades")
public class TradeController {

    @GetMapping          // GET /api/trades
    public ResponseEntity<SuccessResponse<PageResponse<TradeResponse>>> getTrades(...) { ... }

    @PostMapping         // POST /api/trades
    public ResponseEntity<SuccessResponse<Void>> postTrade(...) { ... }

    @GetMapping("/{tradeId}") // GET /api/trades/{tradeId}
    public ResponseEntity<SuccessResponse<TradeResponse>> getTrade(...) { ... }

    @PutMapping("/{tradeId}") // PUT /api/trades/{tradeId}
    public ResponseEntity<SuccessResponse<Void>> updateTrade(...) { ... }

    @PatchMapping("/{tradeId}") // PATCH /api/trades/{tradeId}
    public ResponseEntity<SuccessResponse<Void>> updateState(...) { ... }

    @DeleteMapping("/{tradeId}") // DELETE /api/trades/{tradeId}
    public ResponseEntity<SuccessResponse<Void>> deleteTrade(...) { ... }
}
```

### Step 2: HTTP 메서드 적절성 검증

**검사:** 각 엔드포인트에 올바른 HTTP 메서드가 사용되었는지 확인.

**PASS 기준:**
```
✓ GET:    리소스 조회 (멱등성, 안전)
✓ POST:   리소스 생성
✓ PUT:    리소스 전체 교체 (멱등성)
✓ PATCH:  리소스 부분 수정 (상태 변경 등)
✓ DELETE: 리소스 삭제 (멱등성)
```

### Step 3: 응답 구조 일관성 검증

**검사:** 외부 API 응답이 `SuccessResponse<T>` 래핑을 따르는지 확인.

```bash
# 응답 래핑 패턴 검색
grep -rn "SuccessResponse\|PageResponse\|ResponseEntity" src/ --include="*.java"
```

**PASS 기준:**
```java
import com.rebook.common.core.response.SuccessResponse;
import com.rebook.common.core.response.PageResponse;

// 데이터가 있는 응답 (200 OK)
@GetMapping("/{tradeId}")
public ResponseEntity<SuccessResponse<TradeResponse>> getTrade(...) {
    return SuccessResponse.toOk(tradeService.getTrade(userId, tradeId));
}

// 데이터가 없는 응답 (204 No Content)
@PostMapping
public ResponseEntity<SuccessResponse<Void>> postTrade(...) {
    tradeService.postTrade(request, userId, file);
    return SuccessResponse.toNoContent();
}

// 페이지네이션 응답 (200 OK)
@GetMapping("/me")
public ResponseEntity<SuccessResponse<PageResponse<TradeResponse>>> getTrades(...) {
    return SuccessResponse.toOk(tradeService.getTrades(userId, pageable));
}
```

**위반 사례:**
```java
// 위반 — SuccessResponse 래핑 없음
@GetMapping("/{tradeId}")
public TradeResponse getTrade(...) {
    return tradeService.getTrade(userId, tradeId);
}
```

### Step 4: 인증 패턴 검증

**검사:** `@PassportUser` 어노테이션으로 사용자 식별이 적용되었는지 확인.

```bash
# @PassportUser 사용 패턴 검색
grep -rn "@PassportUser" src/ --include="*.java"
```

**PASS 기준:**
```java
import com.rebook.common.auth.PassportUser;

// userId를 String으로 직접 추출
@GetMapping("/{tradeId}")
public ResponseEntity<SuccessResponse<TradeResponse>> getTrade(
    @PassportUser String userId, @PathVariable Long tradeId) { ... }

// Passport 객체 전체가 필요한 경우
@GetMapping("/test")
public String test(@PassportUser Passport passport) { ... }
```

### Step 5: Internal API 응답 패턴 확인

**검사:** 서비스 간 내부 통신 API는 SuccessResponse 래핑 없이 직접 DTO 반환.

```bash
# Internal 컨트롤러 확인
grep -rn "@RequestMapping.*internal" src/ --include="*.java"
```

**PASS 기준:**
```java
// Internal API — SuccessResponse 래핑 없이 직접 반환
@RestController
@RequestMapping("/internal/trades")
public class InternalTradeController {

    @GetMapping("/{tradeId}")
    public InternalTradeResponse getTrade(@PathVariable Long tradeId) {
        Trade trade = tradeReader.findById(tradeId);
        return InternalTradeResponse.from(trade);
    }

    @GetMapping("/book/{bookId}/count")
    public Integer getActiveTradeCountByBook(@PathVariable Long bookId) {
        return tradeRepository.countByBookIdAndState(bookId, State.AVAILABLE);
    }
}
```

### Step 6: Multipart 요청 패턴 검증

**검사:** 파일 업로드에 `@RequestPart`가 사용되는지 확인.

```bash
# 파일 업로드 패턴 검색
grep -rn "@RequestPart\|@RequestParam.*MultipartFile" src/ --include="*.java"
```

**PASS 기준:**
```java
// 필수 파일 업로드
@PostMapping
public ResponseEntity<SuccessResponse<Void>> postTrade(
    @PassportUser String userId,
    @RequestPart TradeRequest request,
    @RequestPart MultipartFile file) throws IOException { ... }

// 선택적 파일 업로드
@PutMapping("/{tradeId}")
public ResponseEntity<SuccessResponse<Void>> updateTrade(
    @PassportUser String userId,
    @RequestPart TradeRequest request,
    @RequestPart(required = false) MultipartFile file) throws IOException { ... }
```

## 결과 출력 형식

```markdown
## API 설계 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| RESTful URL | PASS/FAIL | N개 |
| HTTP 메서드 | PASS/FAIL | N개 |
| 응답 구조 (SuccessResponse) | PASS/FAIL | N개 |
| 인증 (@PassportUser) | PASS/FAIL | N개 |
| Internal API | PASS/FAIL | N개 |
| Multipart | PASS/FAIL | N개 |
```

---

## 예외사항

1. **Internal API** — `/internal/**` 경로의 서비스 간 통신은 SuccessResponse 래핑 없이 직접 DTO 반환
2. **Feign Client 인터페이스** — 다른 서비스의 API를 호출하는 클라이언트 (BookClient)
3. **Spring Boot Actuator** — 모니터링 엔드포인트

## Related Files

| File | Purpose |
|------|---------|
| `src/.../controller/TradeController.java` | 외부 API 컨트롤러 |
| `src/.../controller/InternalTradeController.java` | 내부 API 컨트롤러 |
| `src/.../model/dto/**/*Response.java` | 응답 DTO |
| `src/.../model/dto/**/*Request.java` | 요청 DTO |