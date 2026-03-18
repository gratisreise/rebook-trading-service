---
name: verify-api-design
description: RESTful API 설계 원칙과 엔드포인트 명명 규칙을 검증합니다. API 엔드포인트 추가/수정 후, PR 전 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 API 파일 또는 경로]"
---

# API 설계 검증

## 목적

백엔드 API의 설계 품질과 일관성을 검증합니다:

1. **RESTful 원칙 준수** — 리소스 기반 URL, 올바른 HTTP 메서드 사용
2. **엔드포인트 명명 규칙** — kebab-case, 복수형 리소스, 버전 관리
3. **요청/응답 구조** — 일관된 응답 포맷, 적절한 상태 코드
4. **API 문서화** — OpenAPI/Swagger 스펙 준수 여부

## 실행 시점

- 새로운 API 엔드포인트를 추가한 후
- 기존 API를 수정하거나 리팩토링한 후
- Pull Request 생성 전
- API 버전 업그레이드 시

## 워크플로우

### Step 1: RESTful URL 패턴 검증

**검사:** 엔드포인트 URL이 RESTful 원칙을 따르는지 확인.

```bash
# 라우터 파일에서 URL 패턴 검색
grep -rn "app\.\(get\|post\|put\|delete\|patch\)" src/ --include="*.ts" --include="*.js"
grep -rn "@\(Get\|Post\|Put\|Delete\|Patch\)" src/ --include="*.ts"
grep -rn "@RequestMapping\|@GetMapping\|@PostMapping" src/ --include="*.java"
```

**위반 사례:**
- URL에 동사 사용: `/getUsers`, `/createOrder`
- 단수형 리소스: `/user`, `/order` (복수형 권장)
- 캐밥케이스 미사용: `/user_profiles`, `/userProfiles`

**PASS 기준:**
```
✓ 리소스는 복수형 명사: /users, /orders, /products
✓ 계층 구조는 최대 3단계: /users/{id}/orders/{orderId}
✓ kebab-case 사용: /user-profiles, /order-items
✓ 버전은 URL prefix: /api/v1/users
```

### Step 2: HTTP 메서드 적절성 검증

**검사:** 각 엔드포인트에 올바른 HTTP 메서드가 사용되었는지 확인.

```bash
# 잘못된 메서드 사용 패턴 검색
grep -rn "app\.post.*get\|app\.get.*create\|app\.get.*delete" src/ --include="*.ts"
```

**PASS 기준:**
```
✓ GET: 리소스 조회 (멱등성, 안전)
✓ POST: 리소스 생성
✓ PUT: 리소스 전체 교체 (멱등성)
✓ PATCH: 리소스 부분 수정
✓ DELETE: 리소스 삭제 (멱등성)
```

### Step 3: 응답 상태 코드 검증

**검사:** 적절한 HTTP 상태 코드가 반환되는지 확인.

```bash
grep -rn "status\s*(\|res\.status\|\.status(" src/ --include="*.ts" --include="*.js"
grep -rn "HttpStatus\|ResponseEntity\.status" src/ --include="*.java"
```

**PASS 기준:**
```
✓ 200 OK: 성공적인 조회/수정
✓ 201 Created: 리소스 생성 성공
✓ 204 No Content: 삭제 성공
✓ 400 Bad Request: 잘못된 요청
✓ 401 Unauthorized: 인증 필요
✓ 403 Forbidden: 권한 없음
✓ 404 Not Found: 리소스 없음
✓ 422 Unprocessable Entity: 유효성 실패
✓ 500 Internal Server Error: 서버 에러
```

### Step 4: 일관된 응답 구조 검증

**검사:** API 응답이 일관된 구조를 따르는지 확인.

**PASS 기준:**
```json
// 성공 응답
{ "success": true, "data": {...}, "message": "..." }

// 에러 응답
{ "success": false, "error": { "code": "...", "message": "..." } }

// 페이지네이션
{ "data": [...], "pagination": { "page": 1, "total": 100 } }
```

## 결과 출력 형식

```markdown
## API 설계 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| RESTful URL | PASS/FAIL | N개 |
| HTTP 메서드 | PASS/FAIL | N개 |
| 상태 코드 | PASS/FAIL | N개 |
| 응답 구조 | PASS/FAIL | N개 |
```

---

## 예외사항

1. **GraphQL 엔드포인트** — REST 원칙 미적용
2. **WebSocket 엔드포인트** — 실시간 통신 패턴
3. **레거시 API** — 하위 호환성 유지
4. **내부 API** — 외부 노출 없는 내부 통신
5. **RPC 스타일** — gRPC, JSON-RPC 별도 규칙

## Related Files

| File | Purpose |
|------|---------|
| `src/routes/**/*.ts` | 라우터 파일 |
| `src/controllers/**/*.ts` | 컨트롤러 파일 |
| `src/api/**/*.ts` | API 핸들러 |
