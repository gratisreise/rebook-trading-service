---
name: verify-testing
description: 단위 테스트, 통합 테스트, 테스트 커버리지를 검증합니다. 테스트 코드 작성 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 테스트 파일 또는 커버리지 대상]"
---

# 테스트 검증

## 목적

Spring Boot 백엔드 애플리케이션의 테스트 품질을 검증합니다:

1. **테스트 커버리지** — Jacoco 기반 충분한 코드 커버리지
2. **단위 테스트 품질** — Mockito 기반 격리성, 명확성, 반복성
3. **통합 테스트** — @SpringBootTest, @WebMvcTest 기반 API 테스트
4. **모킹 전략** — @MockBean, @Mock을 통한 외부 의존성 격리
5. **테스트 명명** — 명확한 테스트 의도 표현

## 실행 시점

- 새로운 테스트 코드를 작성한 후
- 기존 테스트를 수정한 후
- Pull Request 생성 전
- CI/CD 파이프라인 실행 시
- 리팩토링 전후

## 워크플로우

### Step 1: 테스트 커버리지 확인

**검사:** Jacoco 코드 커버리지가 기준을 충족하는지 확인.

```bash
# 커버리지 실행
./gradlew test jacocoTestReport

# 커버리지 리포트 확인 (HTML)
ls build/reports/jacoco/test/html/index.html

# 콘솔 요약
./gradlew test jacocoTestReport 2>&1 | grep -A5 "coverage"
```

**PASS 기준:**
```
✓ 라인 커버리지: 80% 이상
✓ 분기 커버리지: 70% 이상
✓ 핵심 비즈니스 로직 (Service): 90% 이상
```

### Step 2: 단위 테스트 품질 확인

**검사:** Given-When-Then 패턴 준수 여부.

```bash
# 테스트 파일 확인
find src/test -name "*.java" -type f
grep -rn "@Test\|@DisplayName\|@ParameterizedTest" src/test/ --include="*.java" | head -20
```

**PASS 기준:**
```java
// Given-When-Then 패턴 준수
@DisplayName("유효한 데이터로 거래를 생성한다")
@Test
void createTrade_withValidData_shouldReturnTradeResponse() {
    // given
    TradeRequest request = new TradeRequest("책 제목", 10000, ...);
    given(tradeRepository.save(any(Trade.class))).willReturn(trade);

    // when
    TradeResponse response = tradeService.createTrade(userId, request, file);

    // then
    assertThat(response.getTitle()).isEqualTo("책 제목");
    verify(tradeRepository).save(any(Trade.class));
}
```

### Step 3: 테스트 명명 규칙 확인

**검사:** 테스트 이름이 명확한 의도를 표현하는지 확인.

```bash
# 테스트 이름 패턴 검색
grep -rn "@DisplayName\|void\s.*Test\(\)\|void\s.*should\|void\s.*when" src/test/ --include="*.java"
```

**위반 사례:**
```java
// 나쁜 명명
@Test
void test1() { ... }

@Test
void tradeTest() { ... }
```

**PASS 기준:**
```java
// 좋은 명명
@DisplayName("존재하지 않는 거래 ID로 조회시 CMissingDataException을 던진다")
@Test
void findById_withNonexistentId_throwsCMissingDataException() { ... }

@DisplayName("다른 사용자의 거래를 수정하면 CUnauthorizedException을 던진다")
@Test
void updateTrade_withOtherUser_throwsCUnauthorizedException() { ... }
```

### Step 4: 모킹 전략 확인

**검사:** 외부 의존성이 적절히 모킹되었는지 확인.

```bash
# 모킹 패턴 검색
grep -rn "@MockBean\|@Mock\|@InjectMocks\|@SpyBean\|Mockito.\|given(\|when(" src/test/ --include="*.java"
grep -rn "@SpringBootTest\|@WebMvcTest\|@DataJpaTest\|@ExtendWith" src/test/ --include="*.java"
```

**PASS 기준:**
```java
// 단위 테스트 — Mockito로 격리
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {
    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private TradeService tradeService;

    @Test
    void createTrade_shouldUploadImageAndSave() {
        given(s3Service.upload(any())).willReturn("https://s3.../image.jpg");
        // ...
    }
}

// 통합 테스트 — @MockBean으로 외부 의존성만 교체
@SpringBootTest
class TradeIntegrationTest {
    @MockBean
    private BookClient bookClient; // Feign 클라이언트 모킹

    @Autowired
    private TradeController tradeController;
}
```

### Step 5: 통합 테스트 확인

**검사:** Controller/Service 통합 테스트 존재 여부.

```bash
# 통합 테스트 패턴 검색
grep -rn "@SpringBootTest\|@WebMvcTest\|@AutoConfigureMockMvc\|MockMvc" src/test/ --include="*.java"
```

**PASS 기준:**
```java
// MockMvc 기반 컨트롤러 통합 테스트
@WebMvcTest(TradeController.class)
class TradeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService tradeService;

    @Test
    void getTrades_returnsOkWithPageResponse() throws Exception {
        mockMvc.perform(get("/api/v1/trade")
                .header("X-User-Id", 1L)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray());
    }
}
```

### Step 6: 에지 케이스 테스트 확인

**검사:** 예외 상황, 경계값 테스트 존재 여부.

```bash
# 에지 케이스 패턴 검색
grep -rn "throws\|Exception\|invalid\|null\|empty\|unauthorized\|duplicate" src/test/ --include="*.java" | grep -i "@Test\|@DisplayName\|void"
```

**필수 테스트 케이스:**
- null, 빈 값 입력
- 유효하지 않은 형식
- 권한 없는 접근 (다른 사용자의 리소스)
- 리소스 없음 (존재하지 않는 ID)
- 중복 데이터

### Step 7: 테스트 격리성 확인

**검사:** 테스트 간 독립성 보장 여부.

```bash
# @BeforeEach/@AfterEach 패턴 검색
grep -rn "@BeforeEach\|@AfterEach\|@BeforeAll\|@AfterAll\|setUp\|tearDown\|reset(" src/test/ --include="*.java"
```

**PASS 기준:**
```java
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {
    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeService tradeService;

    @BeforeEach
    void setUp() {
        // 각 테스트 전 초기화
        reset(tradeRepository);
    }
}
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
| `TradeService.java` | 65% | 45-52, 78-85 |
| `TradeController.java` | 70% | 12-18 |
```

---

## 예외사항

1. **DTO/Request/Response** — 단순 데이터 클래스는 테스트 생략 가능
2. **Spring Boot 자동 설정** — 프레임워크 자체 기능은 테스트 불필요
3. **단순 CRUD** — 기본 Spring Data JPA 메서드는 통합 테스트로 충분
4. **설정 클래스** — @Configuration 클래스는 로딩 테스트로 충분
5. **외부 API 클라이언트** — Feign 클라이언트는 @MockBean으로 처리

## Related Files

| File | Purpose |
|------|---------|
| `src/test/**/*.java` | 테스트 파일 |
| `build.gradle` | Jacoco, 테스트 설정 |
| `build/reports/jacoco/` | 커버리지 리포트 |