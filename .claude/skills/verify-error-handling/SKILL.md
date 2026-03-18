---
name: verify-error-handling
description: 예외 처리, 에러 응답, 로깅 전략의 일관성을 검증합니다. 에러 처리 로직 수정 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 파일 또는 에러 유형]"
---

# 에러 처리 검증

## 목적

백엔드 애플리케이션의 에러 처리 품질을 검증합니다:

1. **예외 처리** — try-catch로 모든 예외 포착
2. **에러 응답** — 일관된 에러 응답 포맷
3. **로깅** — 적절한 에러 로깅 및 추적
4. **복구 전략** — graceful degradation 구현
5. **사용자 피드백** — 명확한 에러 메시지 전달

## 실행 시점

- 에러 처리 로직을 추가/수정한 후
- 새로운 API 엔드포인트 추가 후
- 외부 서비스 연동 코드 작성 후
- Pull Request 생성 전
- 프로덕션 에러 분석 시

## 워크플로우

### Step 1: try-catch 커버리지 확인

**검사:** async 함수에 에러 처리가 되어있는지 확인.

```bash
# async 함수 중 try-catch 없는 것 찾기
grep -rn "async\s*\w*\s*(" src/ --include="*.ts" | head -30
grep -rn "await.*catch\|try\s*{" src/ --include="*.ts"
```

**위반 사례:**
```javascript
// 위험!
async function getUser(id) {
  const user = await User.findById(id);
  return user;
}
```

**PASS 기준:**
```javascript
// 안전
async function getUser(id) {
  try {
    const user = await User.findById(id);
    if (!user) throw new NotFoundError('User not found');
    return user;
  } catch (error) {
    logger.error('Failed to get user', { id, error });
    throw error;
  }
}
```

### Step 2: 글로벌 에러 핸들러 확인

**검사:** 포착되지 않은 예외를 처리하는 글로벌 핸들러 존재 여부.

```bash
# 글로벌 에러 핸들러 패턴 검색
grep -rn "errorHandler\|app.use.*error\|@ExceptionHandler\|@ControllerAdvice" src/ --include="*.ts" --include="*.java"
grep -rn "process.on.*uncaughtException\|process.on.*unhandledRejection" src/ --include="*.ts"
```

**PASS 기준:**
```javascript
// Express 예시
app.use((err, req, res, next) => {
  logger.error('Unhandled error', { error: err, path: req.path });
  res.status(err.status || 500).json({
    success: false,
    error: { code: err.code || 'INTERNAL_ERROR', message: err.message }
  });
});

// 프로세스 레벨
process.on('uncaughtException', (err) => {
  logger.fatal('Uncaught exception', { error: err });
  process.exit(1);
});
```

### Step 3: 커스텀 에러 클래스 검증

**검사:** 비즈니스 에러를 위한 커스텀 에러 클래스 사용 여부.

```bash
# 커스텀 에러 클래스 검색
grep -rn "class.*Error\|extends Error\|extends AppError" src/ --include="*.ts"
ls -la src/errors/ src/exceptions/ 2>/dev/null
```

**PASS 기준:**
```javascript
// 커스텀 에러 클래스
class NotFoundError extends Error {
  constructor(message) {
    super(message);
    this.name = 'NotFoundError';
    this.status = 404;
    this.code = 'NOT_FOUND';
  }
}

class ValidationError extends Error {
  constructor(message, details) {
    super(message);
    this.name = 'ValidationError';
    this.status = 422;
    this.code = 'VALIDATION_ERROR';
    this.details = details;
  }
}
```

### Step 4: 에러 로깅 품질 확인

**검사:** 로그에 충분한 컨텍스트가 포함되어 있는지 확인.

```bash
# 로깅 패턴 검색
grep -rn "logger\.\|console\.\|log\." src/ --include="*.ts" | grep -i "error"
grep -rn "winston\|pino\|bunyan\|log4j" src/ --include="*.ts"
```

**위반 사항:**
- `console.log(error)` — 컨텍스트 없음
- `logger.error('Error occurred')` — 에러 객체 없음

**PASS 기준:**
```javascript
// 좋은 로깅
logger.error('Failed to create order', {
  error: error.message,
  stack: error.stack,
  userId: user.id,
  orderId: orderId,
  request: { body: req.body, params: req.params }
});
```

### Step 5: 외부 서비스 에러 처리

**검사:** 외부 API/DB 호출에 대한 에러 처리.

```bash
# 외부 서비스 호출 패턴 검색
grep -rn "fetch\|axios\|http\.\|request(" src/ --include="*.ts"
grep -rn "\.connect\|\.query\|prisma\.\|mongoose\." src/ --include="*.ts"
```

**PASS 기준:**
```javascript
// 외부 API 호출 에러 처리
async function callExternalApi(data) {
  try {
    const response = await axios.post(url, data, { timeout: 5000 });
    return response.data;
  } catch (error) {
    if (error.code === 'ECONNREFUSED') {
      throw new ServiceUnavailableError('External API unavailable');
    }
    if (error.response?.status === 429) {
      throw new RateLimitError('Rate limit exceeded');
    }
    logger.error('External API call failed', { error, data });
    throw new ExternalServiceError('Failed to call external API');
  }
}
```

### Step 6: 에러 응답 일관성 확인

**검사:** 모든 에러 응답이 동일한 포맷을 따르는지 확인.

```bash
# 에러 응답 패턴 검색
grep -rn "res\.status\|response\.status\|ResponseEntity" src/ --include="*.ts" --include="*.java" | grep -i "error\|fail"
```

**PASS 기준:**
```json
// 일관된 에러 응답 포맷
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값이 올바르지 않습니다",
    "details": [
      { "field": "email", "message": "유효한 이메일 형식이 아닙니다" }
    ]
  },
  "requestId": "abc-123-def"
}
```

## 결과 출력 형식

```markdown
## 에러 처리 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| try-catch 커버리지 | PASS/FAIL | N개 |
| 글로벌 핸들러 | PASS/FAIL | N개 |
| 커스텀 에러 | PASS/FAIL | N개 |
| 로깅 품질 | PASS/FAIL | N개 |
| 외부 서비스 | PASS/FAIL | N개 |
| 응답 일관성 | PASS/FAIL | N개 |

### 발견된 이슈

| 파일 | 라인 | 문제 | 권장 수정 |
|------|------|------|-----------|
| `src/services/user.ts:45` | try-catch 없음 | async 함수에 에러 처리 추가 |
| `src/controllers/order.ts:120` | 로깅 부족 | 에러 컨텍스트 추가 |
```

---

## 예외사항

1. **의도된 예외** — 비즈니스 로직에서 의도적으로 던지는 예외
2. **테스트 코드** — 테스트에서의 에러 시나리오
3. **초기화 코드** — 앱 시작 시 에러는 즉시 종료가 적절
4. **단순 스크립트** — 일회성 스크립트는 간단한 처리 가능
5. **클로저 내부** — Promise 콜백 등에서 별도 처리 가능

## Related Files

| File | Purpose |
|------|---------|
| `src/middleware/errorHandler.ts` | 글로벌 에러 핸들러 |
| `src/errors/**/*.ts` | 커스텀 에러 클래스 |
| `src/utils/logger.ts` | 로깅 유틸리티 |
| `src/controllers/**/*.ts` | 컨트롤러 파일 |
| `src/services/**/*.ts` | 서비스 레이어 |
