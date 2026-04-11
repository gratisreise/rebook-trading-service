---
name: write-claude-md
description: 프로젝트를 심층 분석하여 최신 상태를 정확히 반영하는 CLAUDE.md를 생성합니다. guide.md의 모범 사례를 적용하여 200줄 이내, 검증 가능한 지침, 도메인 용어 정의를 포함합니다.
disable-model-invocation: true
argument-hint: "[선택사항: 초기 생성 | 업데이트 -- 기본값: 초기 생성]"
---

# CLAUDE.md 생성

## 목적

프로젝트 코드베이스를 심층 분석하여 **정확하고 검증 가능한** CLAUDE.md를 생성합니다:

1. **코드 기반 사실만** — 소스 코드에서 직접 확인한 내용만 기술, 추측 금지
2. **200줄 이내** — 핵심만 남기고 노이즈 제거
3. **검증 가능한 지침** — 모호한 표현 대신 구체적이고 수치적인 기준 제시
4. **도메인 용어 정의** — 비즈니스 로직에서 헷갈릴 수 있는 용어 명확화

## 실행 시점

- 새 프로젝트 세팅 후 최초 CLAUDE.md 작성
- 프로젝트 구조/기술 스택이 크게 변경된 후 업데이트
- 기존 CLAUDE.md가 실제 코드와 불일치한다고 판단될 때

## 워크플로우

### Step 1: 기존 CLAUDE.md 존재 확인

```bash
cat CLAUDE.md 2>/dev/null && echo "---EXISTS---" || echo "---NOT_FOUND---"
```

**기존 파일이 있는 경우 (업데이트 모드):**
- 기존 내용을 읽고 보존할 규칙(팀 컨벤션, 도메인 용어 등)을 식별
- 코드와 불일치하는 내용을 마킹하여 교정 준비

**기존 파일이 없는 경우 (초기 생성 모드):**
- Step 2부터 전체 분석 수행

### Step 2: 프로젝트 분석 — 필수 확인 항목

다음 항목을 **반드시 소스 코드에서 직접 확인**합니다. 추측하거나 가정하지 않습니다.

#### 2a. 빌드 설정

```bash
# Gradle
cat build.gradle
cat gradle.properties 2>/dev/null
cat settings.gradle 2>/dev/null

# Maven
cat pom.xml 2>/dev/null
```

**확인 항목:**
| 항목 | 확인 방법 | 기록 내용 |
|------|-----------|-----------|
| 언어 버전 | `sourceCompatibility`, `toolchain` | 정확한 버전 숫자 |
| 프레임워크 버전 | Spring Boot 플러그인 버전 | 정확한 버전 숫자 |
| 의존성 | dependencies 블록 | 모든 주요 의존성 나열 |
| 빌드 도구 | Gradle/Maven | wrapper 버전 포함 |
| 플러그인 | spotless, jacoco 등 | 설정 내용 |
| 저장소 | repositories 블록 | 프라이빗 저장소 포함 |

#### 2b. 애플리케이션 진입점

```bash
# 메인 클래스 확인
find src/main -name "*Application.java" -exec cat {} \;
```

**확인 항목:**
| 항목 | 실제 코드 확인 |
|------|---------------|
| 어노테이션 | `@SpringBootApplication`, `@EnableJpaAuditing`, `@EnableDiscoveryClient` 등 직접 확인 |
| 패키지 | 선언된 패키지명 |

#### 2c. 디렉토리 구조

```bash
# 전체 구조 (디렉토리만)
find src/main/java -type d | sort
find src/test -type d 2>/dev/null | sort

# 전체 파일 목록
find src/main/java -name "*.java" | sort
```

**확인 항목:**
| 항목 | 기록 내용 |
|------|-----------|
| 패키지 구조 | 도메인형/계층형 구조 파악 |
| 클래스명 | 정확한 이름 기록 (이름 그대로, 임의 변경 금지) |
| 패키지 위치 | controller, service, repository 등의 실제 위치 |

#### 2d. 엔티티 및 데이터 모델

```bash
# 엔티티 파일
find src/main/java -name "*.java" -path "*/entity/*" -exec cat {} \;
find src/main/java -name "*.java" -path "*/model/*" -exec cat {} \;
```

**확인 항목:**
| 항목 | 실제 코드 확인 |
|------|---------------|
| 어노테이션 | `@Entity`, `@EntityListeners`, `@Embeddable` 등 |
| 필드 | 정확한 필드명과 타입 |
| Enum | State, Status 등 열거형의 실제 값 |
| 관계 | `@ManyToOne`, `@OneToMany` 등 |

#### 2e. API 엔드포인트

```bash
# 컨트롤러 파일
find src/main/java -name "*Controller.java" -exec cat {} \;
```

**확인 항목:**
| 항목 | 실제 코드 확인 |
|------|---------------|
| URL 매핑 | `@RequestMapping`, `@GetMapping` 등의 정확한 경로 |
| HTTP 메서드 | GET/POST/PUT/PATCH/DELETE |
| 파라미터 | `@PathVariable`, `@RequestParam`, `@RequestBody`, `@RequestPart` |
| 인증 방식 | `@PassportUser`, `X-User-Id` 헤더, 또는 다른 방식 |
| 응답 래핑 | 실제 응답 클래스명 |

#### 2f. 예외 처리

```bash
# 예외 관련 파일
find src/main/java -name "*Exception*.java" -exec cat {} \;
find src/main/java -name "*Error*.java" -exec cat {} \;
find src/main/java -name "*Advice*.java" -o -name "*ExceptionHandler*.java" | xargs cat 2>/dev/null
```

**확인 항목:**
| 항목 | 실제 코드 확인 |
|------|---------------|
| 예외 클래스 | 정확한 클래스명과 상속 구조 |
| 에러 코드 | Enum이 있는 경우 모든 값 |
| 글로벌 핸들러 | `@RestControllerAdvice` 존재 여부 |

#### 2g. 외부 연동

```bash
# Feign, RabbitMQ, S3, Redis, Gemini 등
find src/main/java -name "*Client*.java" -exec cat {} \;
find src/main/java -name "*Config*.java" -exec cat {} \;
find src/main/java -name "*Publisher*.java" -exec cat {} \;
find src/main/java -path "*/external/*" -name "*.java" -exec cat {} \;
```

**확인 항목:**
| 항목 | 실제 코드 확인 |
|------|---------------|
| Feign 클라이언트 | 인터페이스명, 메서드, 서비스명 |
| 메시징 | Exchange, Queue, Routing Key, 직접 publish vs Outbox 패턴 |
| 외부 API | Gemini 등 AI 서비스 사용 여부 |
| S3 | 업로드 방식, 설정 |
| Redis | 실제 사용 코드 여부 (의존성만 있는 경우 "미사용"으로 명시) |

#### 2h. 비즈니스 로직

```bash
# 서비스 레이어
find src/main/java -name "*Service*.java" -exec cat {} \;
find src/main/java -name "*Reader*.java" -exec cat {} \;
find src/main/java -name "*Writer*.java" -exec cat {} \;
```

**확인 항목:**
| 항목 | 실제 코드 확인 |
|------|---------------|
| 서비스 클래스명 | 정확한 이름 |
| 핵심 비즈니스 플로우 | 메서드 내 실제 로직 순서 |
| 트랜잭션 | `@Transactional` 사용 패턴 |
| 권한 검증 | 실제 검증 메서드와 방식 |

### Step 3: CLAUDE.md 작성

**템플릿:** 다음 5개 섹션 구조를 엄격히 준수합니다. 각 섹션에 작성할 내용은 아래 지침에 따릅니다.

```markdown
# CLAUDE.md

## Project Overview

## Build & TEST

## Implementation Rules

## Result Verification

## Domain Word Definition
```

#### 섹션별 작성 지침

##### Project Overview

프로젝트의 핵심 정보를 간결하게 기술합니다. Step 2에서 분석한 내용 중 프로젝트 정체성에 해당하는 정보만 포함합니다.

**포함 내용:**
- 한 줄 설명 (무엇을 하는 서비스인지)
- Tech Stack: 프레임워크 버전, 언어 버전, DB, 메시징, 외부 서비스 (build.gradle에서 확인한 정확한 버전)
- 아키텍처 요약 (마이크로서비스, 모놀리식 등 실제 구조)
- 핵심 외부 연동 (Eureka, Config Server, Feign, RabbitMQ, S3, Gemini 등 — 실제 사용하는 것만)

**금지 사항:**
- 일반적인 Spring 설명 ("JPA는 ORM이다")
- 프로젝트에 없는 기능 언급 (Redis 의존성만 있고 사용 코드가 없으면 "미사용"으로 명시)
- 과장된 표현

##### Build & TEST

Step 2a에서 확인한 빌드 도구와 실제 명령어만 나열합니다.

**포함 내용:**
```
빌드: ./gradlew build
테스트: ./gradlew test
린트: ./gradlew spotlessApply (spotless 플러그인이 있는 경우만)
```

**금지 사항:**
- 프로젝트에 없는 도구의 명령어 (Maven 프로젝트에 Gradle 명령어 등)
- 자명한 명령어 (`./gradlew clean` 등)

##### Implementation Rules

Step 2에서 분석한 코드에서 확인한 **프로젝트 고유의 규칙**만 나열합니다. 일반적인 Spring 규칙이 아니라, 이 프로젝트만의 특이사항을 Claude가 코드 작성 시 반드시 지켜야 할 지침으로 작성합니다.

**작성 예시 (각 항목은 Step 2에서 확인한 실제 코드 기반):**
```
- 응답 래핑: 공통 라이브러리의 SuccessResponse 사용 (SuccessResponse.toOk(), SuccessResponse.toNoContent())
- 예외 처리: TradeException(TradeError.XXX) 형식, TradeError enum에 에러 코드 정의
- 인증: @PassportUser String userId로 사용자 ID 추출 (common-auth 라이브러리)
- 서비스 분리: Reader(조회), Writer(저장/수정/삭제) 패턴 사용
- 알림: RabbitMQ 직접 publish가 아닌 Outbox 패턴 사용 (OutboxWriter.save())
- 권한 검증: validateOwnership()에서 trade.getUserId().equals(userId) 확인
```

**확인 소스:**
| 규칙 | Step 2 확인 소스 |
|------|-----------------|
| 응답 형태 | 2e Controller의 반환 타입 |
| 예외 처리 | 2f Exception/Error 클래스 |
| 인증 방식 | 2e Controller의 파라미터 어노테이션 |
| 서비스 구조 | 2h Service/Reader/Writer 클래스 |
| 알림 패턴 | 2g Publisher/Outbox 코드 |
| 권한 검증 | 2h validateOwnership() 메서드 |

##### Result Verification

Implementation Rules에서 정의한 규칙을 **Claude가 스스로 검증할 수 있는 구체적인 방법**을 제시합니다. guide.md의 "스스로 검증 루프 만들기" 원칙을 적용합니다.

**작성 예시:**
```
- 응답 검증: 컨트롤러 메서드 반환 타입이 ResponseEntity<SuccessResponse<...>>인지 확인
- 예외 검증: 새로운 예외 발생 시 TradeError enum에 에러 코드가 정의되어 있는지 확인
- 인증 검증: 모든 mutation 엔드포인트에 @PassportUser가 붙어있는지 확인
- Outbox 검증: 알림 발송 시 OutboxWriter.save()를 호출하는지, RabbitTemplate 직접 호출이 아닌지 확인
- 권한 검증: mutation 서비스 메서드에 validateOwnership() 호출이 있는지 확인
```

**각 항목은 Implementation Rules의 규칙과 1:1로 매핑**되어야 합니다.

##### Domain Word Definition

비즈니스 도메인에서 **혼동 가능한 용어**를 정의합니다. 모든 개발자가 이해하는 일반적인 용어는 제외합니다.

**포함 내용:**
```
- 거래(Trade): 도서의 판매를 원하는 유저가 도서 정보를 등록하는 행위. Trade 엔티티
- 찜(Mark): 특정 거래에 대한 관심 표시. TradeUser 엔티티로 관리, 토글 방식
- 상태 평가(Assessment): Gemini AI가 도서 이미지 3장으로 상태를 BEST/GOOD/MEDIUM/POOR로 평가. ConditionAssessmentService
- 대기(WAITING): AI 상태 평가 전의 Trade 상태. 이미지 업로드 후 평가 전
- Outbox: 알림 메시지를 DB에 먼저 저장 후 별도 스케줄러가 발송하는 패턴
```

**금지 사항:**
- 일반적인 개발 용어 정의 ("API란?", "JPA란?")
- 코드에 없는 도메인 개념

### Step 4: 작성 원칙 검증

작성 완료 후 **반드시** 다음 체크리스트로 자체 검증합니다:

#### 정확성 검증

```
CLAUDE.md에 기술된 내용을 소스 코드에서 교차 검증:

- [ ] 모든 클래스명이 실제 소스 코드와 일치하는가?
- [ ] 모든 패키지 경로가 실제 구조와 일치하는가?
- [ ] 모든 버전 번호가 build.gradle에서 확인 가능한가?
- [ ] 모든 API 엔드포인트가 Controller 코드에 존재하는가?
- [ ] 예외 처리 방식이 실제 코드와 일치하는가?
- [ ] 인증 방식이 실제 코드와 일치하는가?
- [ ] "사용하지 않음"으로 표시한 기능이 실제로 사용되지 않는가?
- [ ] 비즈니스 플로우가 실제 서비스 메서드와 일치하는가?
```

#### 품질 검증

```
- [ ] 총 줄 수가 200줄 이하인가?
- [ ] 모호한 표현("깔끔하게", "잘")이 없는가?
- [ ] 검증 가능한 구체적 지침(클래스명, 버전, 경로)인가?
- [ ] README나 마케팅 문서가 아닌 Claude를 위한 업무 지시서인가?
- [ ] 일반적인 Spring 지식(모든 개발자가 아는 내용)은 제외되었는가?
- [ ] Implementation Rules와 Result Verification이 1:1 매핑되는가?
```

#### 분량 검증

```
총 줄 수가 200줄을 초과하면:
1. Project Overview에서 일반적인 아키텍처 설명 축소
2. Build & TEST에서 자명한 명령어 삭제
3. Implementation Rules에서 일반적인 Spring 패턴 삭제
4. Domain Word Definition에서 일반 개발 용어 삭제
```

### Step 5: 사용자 확인 및 피드백

작성 완료 후 결과를 사용자에게 보여줍니다.

**표시 형식:**

```markdown
## CLAUDE.md 생성 완료

**모드**: 초기 생성 / 업데이트
**총 줄 수**: N줄 (200줄 이내 권장)
**검증 결과**: X/Y 항목 통과

### 변경사항 (업데이트 모드인 경우)
| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 클래스명 | TradingService | TradeService |

### 작성 원칙 위반 (있는 경우)
- 200줄 초과: N줄
- 모호한 표현: "..." (N곳)
```

수정 사항이 있으면 반영 후 재검증합니다.

---

## 작성 원칙

### 필수 규칙

1. **코드에서 직접 확인** — 모든 내용은 소스 코드에서 교차 검증. 추측 절대 금지
2. **200줄 이내** — 권장 분량 엄수. 초과 시 우선순위에 따라 압축
3. **검증 가능한 지침** — Claude가 스스로 준수 여부를 판단할 수 있는 구체적 기준
4. **정확한 이름** — 클래스명, 패키지명, 메서드명은 소스 코드와 100% 일치
5. **도메인 용어 정의** — 비즈니스 로직에서 혼동 가능한 용어는 반드시 정의
6. **자체 검증 루프** — 작성 후 반드시 Step 4 체크리스트로 검증

### 금지 사항

1. **추측 금지** — 코드에서 확인할 수 없는 내용은 기재하지 않음
2. **모호한 표현 금지** — "깔끔하게", "잘 작성", "적절히" 등 주관적 표현 사용 불가
3. **일반적 지식 제외** — "JPA는 ORM이다" 등 모든 개발자가 아는 내용 제외
4. **마케팅 언어 금지** — "블레이징히 빠른", "완벽한" 등 과장 표현 사용 불가
5. **과도한 예시 금지** — 코드 예시는 CLAUDE.md에 필요한 경우만 최소한으로

### .claude/rules/ 활용 권장

CLAUDE.md가 200줄에 근접하면, 주제별 규칙을 `.claude/rules/` 디렉토리로 분리할 것을 권장합니다:

```
.claude/rules/
├── api-conventions.md      # globs: ["src/**/controller/**/*.java"]
├── database-rules.md       # globs: ["src/**/repository/**/*.java", "src/**/entity/**/*.java"]
├── exception-handling.md   # globs: ["src/**/exception/**/*.java"]
└── testing-rules.md        # globs: ["src/test/**/*.java"]
```

각 파일은 frontmatter에 `globs`를 지정하여, 해당 파일 작업 시에만 규칙이 로드되도록 합니다.

---

## 예외사항

1. **빈 프로젝트** — 소스 코드가 거의 없는 경우, 분석 가능한 범위까지만 작성
2. **멀티 모듈** — Gradle 멀티 모듈인 경우 각 모듈별 CLAUDE.md 작성 권장
3. **비 Java 프로젝트** — 다른 언어/프레임워크인 경우 Step 2의 확인 방법을 해당 생태계에 맞게 조정

## Related Files

| File | Purpose |
|------|---------|
| `CLAUDE.md` | 생성 대상 파일 |
| `build.gradle` | 의존성 및 버전 확인 |
| `src/main/java/**/*Application.java` | 애플리케이션 진입점 |
| `guide.md` | CLAUDE.md 작성 가이드라인 |