---
name: verify-security
description: 인증/인가, 민감 정보 노출, 입력 검증 등 보안 취약점을 검증합니다. 보안 관련 코드 수정 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 파일 또는 보안 영역]"
---

# 보안 검증

## 목적
Spring Boot 백엔드 애플리케이션의 보안 취약점을 체계적으로 검증합니다:

1. **인증/인가** — 적절한 접근 제어 구현 (X-User-Id 헤더 기반)
2. **민감 정보 보호** — 비밀번호, API 키, 토큰 노출 방지
3. **입력 검증** — 모든 외부 입력에 대한 유효성 검사 (@Valid, BindingResult)
4. **의존성 보안** — 알려진 취약점이 있는 라이브러리 탐지
5. **AWS 자격 증명 보호** — S3 access key, secret key 노출 방지

## 실행 시점

- 인증/인가 로직을 수정한 후
- 사용자 입력을 처리하는 코드 작성 후
- application.yaml 이나 설정 파일을 변경한 후
- 새로운 의존성을 추가한 후
- Pull Request 생성 전 (특히 보안 관련)
- 정기 보안 점검 시

## 워크플로우

### Step 1: 민감 정보 노출 탐지

**검사:** 하드코딩된 비밀번호, API 키, 토큰, AWS 자격 증명을 찾습니다.

```bash
# 민감 정보 패턴 검색
grep -rn 'password\s*=\s*"\|apiKey\s*=\s*"\|secret\s*=\s*"' src/ --include="*.java" --include="*.yaml" --include="*.yml" --include="*.properties"
grep -rn "-----BEGIN.*PRIVATE-----\|aws_access_key_id\|aws_secret_access_key" src/
grep -rn "ACCESS_KEY\|SECRET_KEY\|JWT_SECRET\|DATABASE_PASSWORD\|API_KEY" src/ --include="*.java" | grep -v "@Value\|@ConfigurationProperty\|environment.getProperty"
```

**위반 사례:**
```java
// 위험!
private String apiKey = "sk-abc123xyz";
private String dbUrl = "jdbc:postgresql://localhost:5432/db?user=root&password=admin123";
```

**PASS 기준:**
```java
// 안전 — Spring Cloud Config 또는 환경 변수에서 주입
@Value("${aws.access-key}")
private String accessKey;

// 또는 ConfigurationProperties 사용
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
    private String accessKey;
    private String secretKey;
}
```

### Step 2: 인증/인가 검증

**검사:** X-User-Id 헤더 기반 소유권 검증이 적용되었는지 확인.

```bash
# 헤더 기반 사용자 인증 패턴 검색
grep -rn "@RequestHeader.*X-User-Id\|@RequestHeader.*x-user-id" src/ --include="*.java"
grep -rn "CUnauthorizedException\|소유권\|권한" src/ --include="*.java"
```

**PASS 기준:**
```java
// 소유권 검증이 있는 수정/삭제 엔드포인트
public void updateTrading(Long tradeId, Long userId, ...) {
    Trading trade = tradeReader.findById(tradeId);
    if (!trade.getUserId().equals(userId)) {
        throw new CUnauthorizedException("해당 거래에 대한 권한이 없습니다.");
    }
    // ...
}
```

### Step 3: 입력 검증 확인

**검사:** 모든 사용자 입력이 검증되는지 확인.

```bash
# 검증 어노테이션 패턴 검색
grep -rn "@Valid\|@Validated\|@NotNull\|@NotBlank\|@Size\|@Min\|@Max\|@Pattern" src/ --include="*.java"
grep -rn "@RequestBody\|@PathVariable\|@RequestParam\|@RequestPart" src/ --include="*.java"
```

**위반 사례:**
```java
// 위험! — 검증 어노테이션 없음
@PostMapping
public SingleResult<TradeResponse> createTrade(
    @RequestHeader("X-User-Id") Long userId,
    @RequestBody TradeRequest request) { ... }
```

**PASS 기준:**
```java
// 안전 — @Valid로 DTO 검증
@PostMapping
public SingleResult<TradeResponse> createTrade(
    @RequestHeader("X-User-Id") Long userId,
    @Valid @RequestBody TradeRequest request) { ... }

// DTO에 검증 어노테이션
public class TradeRequest {
    @NotBlank(message = "책 제목은 필수입니다")
    private String title;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Integer price;
}
```

### Step 4: 의존성 취약점 스캔

**검사:** 알려진 보안 취약점이 있는 라이브러리 탐지.

```bash
# Gradle 의존성 취약점 스캔
./gradlew dependencyCheckAnalyze 2>/dev/null

# 의존성 트리 확인
./gradlew dependencies --configuration compileClasspath
```

**PASS 기준:**
```
✓ Critical/High 취약점 0개
✓ Moderate 취약점은 평가 후 조치
✓ 최신 버전으로 업데이트된 의존성
```

### Step 5: MultipartFile 업로드 보안 확인

**검사:** 파일 업로드 시 보안 검증이 적용되었는지 확인.

```bash
# 파일 업로드 관련 코드 검색
grep -rn "@RequestPart\|@RequestParam.*MultipartFile\|multipart" src/ --include="*.java"
grep -rn "getOriginalFilename\|getContentType\|getSize\|transferTo" src/ --include="*.java"
```

**PASS 기준:**
```java
// 파일 확장자 검증
private void validateFile(MultipartFile file) {
    String contentType = file.getContentType();
    if (!ALLOWED_TYPES.contains(contentType)) {
        throw new CInvalidDataException("지원하지 않는 파일 형식입니다.");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new CInvalidDataException("파일 크기가 제한을 초과합니다.");
    }
}
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
| 파일 업로드 보안 | PASS/FAIL | N개 |

### 발견된 취약점

| 파일 | 라인 | 취약점 유형 | 심각도 |
|------|------|-------------|--------|
| `src/.../config/S3Config.java:15` | 하드코딩된 access key | CRITICAL |
| `src/.../controller/TradeController.java:45` | 입력 검증 누락 | MEDIUM |
```

---

## 예외사항

1. **공개 엔드포인트** — 인증 불필요한 공개 API (예: 헬스 체크)
2. **테스트 환경** — 테스트용 mock 데이터
3. **로그 파일** — 로그에 민감 정보가 없어야 함 (별도 규칙)
4. **Spring Cloud Config** — Config Server에서 관리하는 설정은 로컬에 하드코딩 없음
5. **API Gateway 위임** — 인증은 API Gateway에서 처리, 이 서비스는 X-User-Id 헤더로 식별

## Related Files

| File | Purpose |
|------|---------|
| `src/.../controller/**/*.java` | 컨트롤러 (헤더 추출, 입력 검증) |
| `src/.../service/**/*.java` | 서비스 (소유권 검증) |
| `src/.../external/s3/*.java` | S3 설정 및 서비스 |
| `src/.../common/exception/*.java` | 커스텀 예외 |
| `src/main/resources/application*.yaml` | Spring 설정 파일 |
| `build.gradle` | 의존성 정의 |