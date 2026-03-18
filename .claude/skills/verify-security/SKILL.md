---
name: verify-security
description: 인증/인가, 민감 정보 노출, 입력 검증 등 보안 취약점을 검증합니다. 보안 관련 코드 수정 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 파일 또는 보안 영역]"
---

# 보안 검증

## 목적

백엔드 애플리케이션의 보안 취약점을 체계적으로 검증합니다:

1. **인증/인가** — 적절한 접근 제어 구현
2. **민감 정보 보호** — 비밀번호, API 키, 토큰 노출 방지
3. **입력 검증** — 모든 외부 입력에 대한 유효성 검사
4. **의존성 보안** — 알려진 취약점이 있는 패키지 탐지
5. **HTTPS/암호화** — 전송 및 저장 데이터 보호

## 실행 시점

- 인증/인가 로직을 수정한 후
- 사용자 입력을 처리하는 코드 작성 후
- 환경 변수나 설정 파일을 변경한 후
- 새로운 의존성을 추가한 후
- Pull Request 생성 전 (특히 보안 관련)
- 정기 보안 점검 시

## 워크플로우

### Step 1: 민감 정보 노출 탐지

**검사:** 하드코딩된 비밀번호, API 키, 토큰을 찾습니다.

```bash
# 민감 정보 패턴 검색
grep -rn "password\s*=\s*['\"]\|apiKey\s*=\s*['\"]\|secret\s*=\s*['\"]" src/ --include="*.ts" --include="*.js"
grep -rn "-----BEGIN.*PRIVATE-----\|aws_access_key_id\|aws_secret_access_key" src/
grep -rn "JWT_SECRET\|DATABASE_PASSWORD\|API_KEY" src/ --include="*.ts" | grep -v "process.env"
```

**위반 사례:**
```javascript
// 위험!
const password = "admin123";
const apiKey = "sk-abc123xyz";
const dbUrl = "mysql://root:password@localhost/db";
```

**PASS 기준:**
```javascript
// 안전
const password = process.env.DB_PASSWORD;
const apiKey = config.get('api.key');
```

### Step 2: 인증/인가 검증

**검사:** 보호된 엔드포인트에 인증 미들웨어가 적용되었는지 확인.

```bash
# 인증 미들웨어 패턴 검색
grep -rn "authMiddleware\|authenticate\|@PreAuthorize\|@Secured" src/ --include="*.ts" --include="*.java"
grep -rn "verifyToken\|checkAuth\|isAuthenticated" src/ --include="*.ts"
```

**PASS 기준:**
```
✓ 모든 보호된 라우트에 인증 미들웨어 적용
✓ 역할 기반 접근 제어(RBAC) 구현
✓ JWT 토큰 만료 시간 설정
✓ 세션 무효화 메커니즘 존재
```

### Step 3: 입력 검증 확인

**검사:** 모든 사용자 입력이 검증되는지 확인.

```bash
# 검증 패턴 검색
grep -rn "validate\|sanitize\|escape\|trim" src/ --include="*.ts"
grep -rn "class-validator\|joi\|zod\|yup" src/ --include="*.ts"
grep -rn "@Body\|@Param\|@Query\|req.body\|req.params" src/ --include="*.ts"
```

**위반 사례:**
```javascript
// 위험!
app.post('/users', (req, res) => {
  const { email, name } = req.body; // 검증 없음
  User.create({ email, name });
});
```

**PASS 기준:**
```javascript
// 안전
app.post('/users', validate(userSchema), (req, res) => {
  const { email, name } = req.body; // 미들웨어에서 검증됨
  User.create({ email, name });
});
```

### Step 4: 의존성 취약점 스캔

**검사:** 알려진 보안 취약점이 있는 패키지 탐지.

```bash
# npm/yarn audit
npm audit --json 2>/dev/null || yarn audit --json 2>/dev/null

# Python
pip-audit 2>/dev/null || safety check 2>/dev/null

# Java
./mvnw dependency-check:check 2>/dev/null || ./gradlew dependencyCheckAnalyze 2>/dev/null
```

**PASS 기준:**
```
✓ Critical/High 취약점 0개
✓ Moderate 취약점은 평가 후 조치
✓ 최신 버전으로 업데이트된 의존성
```

### Step 5: CORS 및 보안 헤더 확인

**검사:** 적절한 보안 헤더가 설정되었는지 확인.

```bash
# 보안 헤더 패턴 검색
grep -rn "helmet\|cors\|Content-Security-Policy\|X-Frame-Options\|X-Content-Type-Options" src/ --include="*.ts"
grep -rn "cors\s*(" src/ --include="*.ts" --include="*.js"
```

**PASS 기준:**
```javascript
// 권장 설정
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(','),
  credentials: true
}));
```

### Step 6: 비밀번호 저장 검증

**검사:** 비밀번호가 안전하게 해시되어 저장되는지 확인.

```bash
# 비밀번호 해시 패턴 검색
grep -rn "bcrypt\|argon2\|scrypt\|pbkdf2" src/ --include="*.ts" --include="*.js"
grep -rn "password.*hash\|hash.*password" src/ --include="*.ts"
```

**위반 사항:**
- 평문 비밀번호 저장
- MD5, SHA1 등 약한 해시 사용
- 솔트(salt) 미사용

**PASS 기준:**
```javascript
// 안전
const hashedPassword = await bcrypt.hash(password, 10);
// 또는 argon2 권장
const hashedPassword = await argon2.hash(password);
```

## 결과 출력 형식

```markdown
## 보안 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| 민감 정보 노출 | PASS/FAIL | N개 |
| 인증/인가 | PASS/FAIL | N개 |
| 입력 검증 | PASS/FAIL | N개 |
| 의존성 취약점 | PASS/FAIL | N개 |
| 보안 헤더 | PASS/FAIL | N개 |
| 비밀번호 저장 | PASS/FAIL | N개 |

### 발견된 취약점

| 파일 | 라인 | 취약점 유형 | 심각도 |
|------|------|-------------|--------|
| `src/config.ts:15` | 하드코딩된 API 키 | CRITICAL |
| `src/routes/admin.ts:22` | 인증 우회 가능 | HIGH |
| `src/controllers/user.ts:45` | 입력 검증 누락 | MEDIUM |
```

---

## 예외사항

1. **공개 API 키** — 프론트엔드에서 사용하는 공개 키 (예: Firebase)
2. **테스트 환경** — 테스트용 mock 데이터
3. **로그 파일** — 로그에 민감 정보가 없어야 함 (별도 규칙)
4. **공개 엔드포인트** — 인증 불필요한 공개 API
5. **레거시 시스템** — 점진적 마이그레이션 필요

## Related Files

| File | Purpose |
|------|---------|
| `src/middleware/auth*.ts` | 인증 미들웨어 |
| `src/routes/**/*.ts` | 라우터 파일 |
| `src/controllers/**/*.ts` | 컨트롤러 |
| `.env*` | 환경 변수 파일 |
| `package.json` | 의존성 정의 |
| `src/validators/**/*.ts` | 입력 검증 로직 |
