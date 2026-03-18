---
name: verify-testing
description: 단위 테스트, 통합 테스트, 테스트 커버리지를 검증합니다. 테스트 코드 작성 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 테스트 파일 또는 커버리지 대상]"
---

# 테스트 검증

## 목적

백엔드 애플리케이션의 테스트 품질을 검증합니다:

1. **테스트 커버리지** — 충분한 코드 커버리지
2. **단위 테스트 품질** — 격리성, 명확성, 반복성
3. **통합 테스트** — API 엔드포인트 테스트
4. **모킹 전략** — 외부 의존성 격리
5. **테스트 명명** — 명확한 테스트 의도 표현

## 실행 시점

- 새로운 테스트 코드를 작성한 후
- 기존 테스트를 수정한 후
- Pull Request 생성 전
- CI/CD 파이프라인 실행 시
- 리팩토링 전후

## 워크플로우

### Step 1: 테스트 커버리지 확인

**검사:** 코드 커버리지가 기준을 충족하는지 확인.

```bash
# 커버리지 실행
npm test -- --coverage --coverageReporters=text-summary 2>/dev/null
yarn test --coverage 2>/dev/null
./mvnw jacoco:report 2>/dev/null
./gradlew test jacocoTestReport 2>/dev/null

# 커버리지 설정 확인
cat jest.config.js 2>/dev/null | grep -A5 coverageThreshold
cat .nycrc 2>/dev/null
```

**PASS 기준:**
```
✓ 라인 커버리지: 80% 이상
✓ 분기 커버리지: 70% 이상
✓ 함수 커버리지: 80% 이상
✓ 핵심 비즈니스 로직: 90% 이상
```

### Step 2: 단위 테스트 품질 확인

**검사:** AAA 패턴(Arrange-Act-Assert) 준수 여부.

```bash
# 테스트 파일 확인
ls -la tests/ __tests__/ test/ src/**/*.test.ts 2>/dev/null
grep -rn "describe\s*(\|it\s*(\|test\s*(" tests/ --include="*.test.ts" | head -20
```

**PASS 기준:**
```javascript
// AAA 패턴 준수
describe('UserService', () => {
  describe('createUser', () => {
    it('should create user with valid data', async () => {
      // Arrange
      const userData = { email: 'test@example.com', name: 'Test' };
      mockRepository.findByEmail.mockResolvedValue(null);

      // Act
      const result = await userService.createUser(userData);

      // Assert
      expect(result).toMatchObject(userData);
      expect(mockRepository.create).toHaveBeenCalledWith(userData);
    });
  });
});
```

### Step 3: 테스트 명명 규칙 확인

**검사:** 테스트 이름이 명확한 의도를 표현하는지 확인.

```bash
# 테스트 이름 패턴 검색
grep -rn "it\s*(\|test\s*(\|describe\s*(" tests/ --include="*.test.ts"
```

**위반 사례:**
```javascript
// 나쁜 명명
it('test1', () => {...});
it('works', () => {...});
it('should work', () => {...});
```

**PASS 기준:**
```javascript
// 좋은 명명
it('should throw ValidationError when email is invalid', () => {...});
it('should return 404 when user does not exist', () => {...});
it('should hash password before saving user', () => {...});
```

### Step 4: 모킹 전략 확인

**검사:** 외부 의존성이 적절히 모킹되었는지 확인.

```bash
# 모킹 패턴 검색
grep -rn "mock\|Mock\|jest.fn\|sinon\|@MockBean\|@Mock" tests/ --include="*.test.ts"
grep -rn "jest.mock\|vi.mock\|spyOn" tests/ --include="*.test.ts"
```

**PASS 기준:**
```javascript
// 의존성 모킹
jest.mock('../repositories/UserRepository');
jest.mock('../services/EmailService');

// 주입을 통한 모킹
const mockRepository = {
  findById: jest.fn(),
  create: jest.fn()
};
const service = new UserService(mockRepository);
```

### Step 5: 통합 테스트 확인

**검사:** API 엔드포인트 통합 테스트 존재 여부.

```bash
# 통합 테스트 패턴 검색
grep -rn "request\s*(\|supertest\|@SpringBootTest\|@AutoConfigureMockMvc" tests/ --include="*.test.ts" --include="*.java"
ls tests/integration/ tests/e2e/ 2>/dev/null
```

**PASS 기준:**
```javascript
// API 통합 테스트
describe('POST /api/users', () => {
  it('should create user and return 201', async () => {
    const response = await request(app)
      .post('/api/users')
      .send({ email: 'test@example.com', name: 'Test' })
      .expect(201);

    expect(response.body.data.email).toBe('test@example.com');
  });
});
```

### Step 6: 에지 케이스 테스트 확인

**검사:** 예외 상황, 경계값 테스트 존재 여부.

```bash
# 에지 케이스 패턴 검색
grep -rn "should throw\|should fail\|should return.*error\|invalid\|null\|undefined\|empty" tests/ --include="*.test.ts"
```

**필수 테스트 케이스:**
- 빈 값, null, undefined 입력
- 유효하지 않은 형식
- 권한 없는 접근
- 리소스 없음
- 동시성 이슈
- 타임아웃

### Step 7: 테스트 격리성 확인

**검사:** 테스트 간 독립성 보장 여부.

```bash
# beforeEach/afterEach 패턴 검색
grep -rn "beforeEach\|afterEach\|beforeAll\|afterAll\|setUp\|tearDown" tests/ --include="*.test.ts"
```

**PASS 기준:**
```javascript
describe('UserService', () => {
  let service;
  let mockRepository;

  beforeEach(() => {
    // 각 테스트 전 새로운 인스턴스
    mockRepository = {
      findById: jest.fn(),
      create: jest.fn()
    };
    service = new UserService(mockRepository);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });
});
```

## 결과 출력 형식

```markdown
## 테스트 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| 커버리지 | PASS/FAIL | 75% (기준: 80%) |
| 단위 테스트 품질 | PASS/FAIL | N개 |
| 테스트 명명 | PASS/FAIL | N개 |
| 모킹 전략 | PASS/FAIL | N개 |
| 통합 테스트 | PASS/FAIL | N개 |
| 에지 케이스 | PASS/FAIL | N개 |
| 테스트 격리 | PASS/FAIL | N개 |

### 커버리지 미달 파일

| 파일 | 커버리지 | 미커버 라인 |
|------|----------|-------------|
| `src/services/user.ts` | 65% | 45-52, 78-85 |
| `src/utils/validator.ts` | 70% | 12-18 |

### 개선 권장

| 테스트 파일 | 문제 | 권장 수정 |
|-------------|------|-----------|
| `user.test.ts` | 모킹 없음 | Repository 모킹 추가 |
| `order.test.ts` | 명명 불명확 | 구체적인 테스트 이름 사용 |
```

---

## 예외사항

1. **POJO/DTO** — 단순 데이터 클래스는 테스트 생략 가능
2. **프레임워크 코드** — 프레임워크 자체 기능은 테스트 불필요
3. **단순 CRUD** — 기본 CRUD는 통합 테스트로 충분
4. **일회성 스크립트** — 실행 스크립트는 테스트 선택적
5. **서드파티 라이브러리** — 외부 라이브러리는 모킹으로 처리

## Related Files

| File | Purpose |
|------|---------|
| `tests/**/*.test.ts` | 테스트 파일 |
| `__tests__/**/*.ts` | Jest 테스트 디렉토리 |
| `jest.config.js` | Jest 설정 |
| `vitest.config.ts` | Vitest 설정 |
| `src/**/*.test.ts` | 곁들여진 테스트 파일 |
| `coverage/` | 커버리지 리포트 |
