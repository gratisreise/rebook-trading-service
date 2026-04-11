---
name: verify-error-handling
description: 예외 처리, 에러 응답, 로깅 전략의 일관성을 검증합니다. 에러 처리 로직 수정 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 파일 또는 에러 유형]"
---

# 에러 처리 검증

## 목적

Spring Boot 백엔드 애플리케이션의 에러 처리 품질을 검증합니다:

1. **도메인 예외** — `TradeException extends BusinessException` (common-core) 사용
2. **에러 코드 enum** — `TradeError implements ErrorCode`로 도메인별 에러 코드 정의
3. **글로벌 예외 처리** — common-autoconfigure에서 자동 제공 (개별 @RestControllerAdvice 불필요)
4. **로깅** — 적절한 에러 로깅 및 컨텍스트
5. **외부 서비스 에러** — S3, Gemini API, RabbitMQ 호출 에러 처리

## 실행 시점

- 에러 처리 로직을 추가/수정한 후
- 새로운 API 엔드포인트 추가 후
- 외부 서비스 연동 코드 작성 후 (S3, Gemini, RabbitMQ)
- Pull Request 생성 전
- 프로덕션 에러 분석 시

## 워크플로우

### Step 1: 도메인 예외 클래스 검증

**검사:** `TradeException`이 `BusinessException`을 상속하고, `TradeError`가 `ErrorCode`를 구현하는지 확인.

```bash
# 예외 클래스 검색
grep -rn "extends BusinessException\|implements ErrorCode" src/ --include="*.java"
find src -name "*Exception.java" -o -name "*Error.java" | grep -v build
```

**PASS 기준:**
```java
// TradeException.java
import com.rebook.common.core.exception.BusinessException;
import com.rebook.common.core.exception.ErrorCode;

public class TradeException extends BusinessException {
    private TradeException(ErrorCode code) {
        super(code);
    }
}

// TradeError.java — 도메인별 에러 코드 enum
import com.rebook.common.core.exception.ErrorCode;

@Getter
@AllArgsConstructor
public enum TradeError implements ErrorCode {
    TRADE_NOT_FOUND(404, "TRADE_001", "거래를 찾을 수 없습니다."),
    UNAUTHORIZED(401, "TRADE_002", "권한이 없습니다."),
    // ... 필요한 에러 코드 추가
    ;

    private final int status;
    private final String code;
    private final String message;
}
```

### Step 2: 예외 생성 패턴 검증

**검사:** `new TradeException(TradeError.XXX)` 패턴으로 일관되게 예외를 생성하는지 확인.

```bash
# 예외 생성 패턴 검색
grep -rn "new TradeException\|throw.*TradeException\|throw new BusinessException" src/ --include="*.java"
grep -rn "TradeError\." src/ --include="*.java"
```

**PASS 기준:**
```java
// 기본 패턴 — TradeError enum 사용
public Trade findById(Long tradeId) {
    return tradeRepository.findById(tradeId)
        .orElseThrow(() -> new TradeException(TradeError.TRADE_NOT_FOUND));
}

// 소유권 검증
private void validateOwnership(Trade trade, String userId) {
    if (!trade.getUserId().equals(userId)) {
        throw new TradeException(TradeError.UNAUTHORIZED);
    }
}
```

**위반 사례:**
```java
// 위반 — 임의의 RuntimeException 사용
throw new RuntimeException("Something went wrong");
throw new IllegalArgumentException("Invalid input");
```

### Step 3: TradeError enum 커버리지 확인

**검사:** 서비스에서 발생 가능한 에러가 TradeError enum에 모두 정의되어 있는지 확인.

```bash
# TradeError enum 값 확인
grep -rn "TradeError\.\w" src/ --include="*.java" | grep -v "^.*TradeError.java"
```

**확인 사항:**
- NOT_FOUND: 리소스 조회 실패 시
- UNAUTHORIZED: 소유권 검증 실패 시
- INVALID_STATE_TRANSITION: 상태 변경 불가 시
- S3_UPLOAD_FAILED: S3 업로드 실패 시
- INVALID_IMAGE_COUNT: 이미지 개수 검증 실패 시
- AI_ASSESSMENT_FAILED: AI 분석 실패 시

### Step 4: 글로벌 예외 처리 확인

**검사:** 이 프로젝트는 common-autoconfigure에서 글로벌 예외 핸들러를 자동 제공합니다.

```bash
# 프로젝트 내 커스텀 @RestControllerAdvice 확인 (있으면 안 됨)
grep -rn "@RestControllerAdvice\|@ControllerAdvice\|@ExceptionHandler" src/ --include="*.java"
```

**PASS 기준:**
```
✓ 이 프로젝트에 @RestControllerAdvice가 없어야 정상
✓ common-autoconfigure에서 BusinessException을 자동으로 에러 응답으로 변환
✓ TradeException(BusinessException)만 던지면 글로벌 핸들러가 자동 처리
```

### Step 5: 에러 로깅 품질 확인

**검사:** 로그에 충분한 컨텍스트가 포함되어 있는지 확인.

```bash
# 로깅 패턴 검색
grep -rn "@Slf4j" src/ --include="*.java"
grep -rn "log\.error\|log\.warn\|log\.info" src/ --include="*.java"
```

**PASS 기준:**
```java
// 예외 객체와 함께 로깅
} catch (RuntimeException e) {
    log.error(e.getMessage());
    throw TradeException.s3UploadFailed("s3 이미지 업로드에 실패했습니다.");
}

// 컨텍스트 포함 로깅
log.error("Gemini API 호출 실패: {}", e.getMessage());
```

### Step 6: 외부 서비스 에러 처리

**검사:** S3, Gemini API 호출에 대한 try-catch 및 에러 변환.

```bash
# 외부 서비스 호출 패턴 검색
grep -rn "try\s*{\|catch\s*(" src/ --include="*.java"
grep -rn "s3Client\.\|geminiService\.\|rabbitTemplate\." src/ --include="*.java"
```

**PASS 기준:**
```java
// S3 업로드 에러 처리
try {
    s3Client.putObject(putObjectRequest, ...);
} catch (RuntimeException e) {
    log.error(e.getMessage());
    throw new TradeException(TradeError.S3_UPLOAD_FAILED);
}

// Gemini API 에러 처리
try {
    return geminiService.callObjectWithImages(...);
} catch (Exception e) {
    log.error("Gemini API 호출 실패: {}", e.getMessage());
    throw new TradeException(TradeError.AI_ASSESSMENT_FAILED);
}

// 외부 API 공통 에러 — BusinessException 직접 사용
throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
```

## 결과 출력 형식

```markdown
## 에러 처리 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| 도메인 예외 (TradeException) | PASS/FAIL | N개 |
| TradeError enum 커버리지 | PASS/FAIL | N개 |
| 예외 생성 패턴 | PASS/FAIL | N개 |
| 글로벌 핸들러 (자동 제공) | PASS/FAIL | N개 |
| 로깅 품질 | PASS/FAIL | N개 |
| 외부 서비스 에러 | PASS/FAIL | N개 |

### 발견된 이슈

| 파일 | 라인 | 문제 | 권장 수정 |
|------|------|------|-----------|
| `TradeService.java:45` | RuntimeException 직접 사용 | `new TradeException(TradeError.XXX)`로 변경 |
| `S3Service.java:20` | try-catch 없음 | S3 호출 에러 처리 추가 |
```

---

## 예외사항

1. **BusinessException 직접 사용** — GeminiService 등 외부 API 에러에서 `new BusinessException(ErrorCode.EXTERNAL_API_ERROR)` 사용은 허용
2. **Internal API** — 서비스 간 통신에서는 별도 에러 응답 형식 사용 가능
3. **테스트 코드** — 테스트에서의 에러 시나리오
4. **Spring 프레임워크 예외** — 프레임워크 자체가 처리하는 예외

## Related Files

| File | Purpose |
|------|---------|
| `src/.../common/exception/TradeException.java` | 도메인 예외 (BusinessException 상속) |
| `src/.../common/exception/TradeError.java` | 도메인 에러 코드 enum (ErrorCode 구현) |
| `src/.../service/**/*.java` | 서비스 레이어 (예외 발생) |
| `src/.../external/s3/S3Service.java` | S3 업로드 에러 처리 |
| `src/.../external/gemini/GeminiService.java` | Gemini API 에러 처리 |
| `src/.../external/rabbitmq/NotificationPublisher.java` | RabbitMQ 에러 처리 |