---
name: write-readme
description: 프로젝트의 특성을 분석하여 간결하고 실용적인 README를 작성합니다.
disable-model-invocation: true
argument-hint: "[선택사항: 프로젝트 경로 또는 특정 섹션 강조]"
---

# README 작성

## 목적

프로젝트의 특성을 분석하여 핵심 정보만 담은 간결한 README를 작성합니다:

1. **프로젝트 분석** — 기술 스택, 구조, 주요 기능 파악
2. **간결한 문서** — 불필요한 섹션 없이 핵심만 구성
3. **실용적** — API, 구조, 큐 설정 등 개발에 필요한 정보 포함

## 실행 시점

- 새 프로젝트 생성 후 초기 문서화
- 기존 README 개선 또는 재작성
- 프로젝트 구조 변경 후 문서 업데이트

## 워크플로우

### Step 1: 프로젝트 분석

**검사:** 프로젝트의 기술 스택과 구조 파악.

```bash
# 프로젝트 유형 확인
ls -la

# Java/Gradle 프로젝트
cat build.gradle

# 디렉토리 구조 (3뎁스까지만)
find src/main/java -type d | sort

# API 엔드포인트 확인
grep -rn "@RequestMapping\|@GetMapping\|@PostMapping" src/ --include="*.java"
```

**분석 항목:**
| 항목 | 확인 방법 |
|------|-----------|
| 언어/프레임워크 | build.gradle |
| 빌드 도구 | Gradle wrapper |
| 외부 의존성 | build.gradle의 dependencies |
| API 엔드포인트 | Controller 레이어 파일 |
| 메시징 | RabbitMQ 설정 파일 |
| 외부 서비스 | Feign client, S3 설정 |

### Step 2: 기존 README 확인 (있는 경우)

```bash
cat README.md 2>/dev/null || echo "README 없음"
```

**보존할 내용 식별:**
- 프로젝트 고유의 컨텍스트
- 이미 잘 작성된 섹션
- 팀 내부 규칙이나 컨벤션

### Step 3: README 작성

**템플릿:** 다음 구조를 기본으로 하되, 프로젝트에 불필요한 섹션은 과감히 생략.

```markdown
# 프로젝트명

[![Badge1]][link1] [![Badge2]][link2]

한 줄 설명 (프로젝트가 무엇인지, 핵심 기능)

## 목차

- [아키텍처](#아키텍처)
- [기능](#기능)
- [기술 스택](#기술-스택)
- [API 문서](#api-문서)
- [프로젝트 구조](#프로젝트-구조)

---

## 아키텍처

> TODO: 아키텍처 다이어그램 추가 예정

---

## 기능

### 주요 기능

- **기능명**: 설명
- **기능명**: 설명

---

## 기술 스택

### Language & Framework
- **Java 17**, **Spring Boot 3.x**

### Database
- **PostgreSQL**, **Spring Data JPA**

### Messaging
- **RabbitMQ** (AMQP) — 설명

### Build & Deploy
- **Gradle**, **Docker**

---

## API 문서

Swagger UI 주소 안내.

### 카테고리별 엔드포인트 테이블

| Method | Endpoint | 설명 |
|--------|----------|------|
| ... | ... | ... |

---

## 프로젝트 구조

(디렉토리 단위 간소화, 개별 파일은 나열하지 않음)

```
src/main/java/.../
├── clientfeign/         # 외부 서비스 통신
├── domain/
│   ├── controller/      # REST API
│   ├── model/           # DTO & Entity
│   ├── repository/      # Repository
│   └── service/         # 비즈니스 로직
├── external/
│   ├── rabbitmq/        # 메시지 큐 설정
│   └── s3/              # S3 설정
└── common/              # 공통 (enum, exception)
```

### Step 4: 사용자 확인

작성 후 사용자에게 결과를 보여주고, 수정 사항이 있으면 반영.

---

## 작성 원칙

### 기본 규칙

1. **간결함 우선** — 핵심 정보만, 군더더기 없이
2. **불필요한 섹션 과감히 생략** — 설치 가이드, 환경 변수, 라이선스 등은 제외
3. **표는 필요할 때만** — 엔드포인트, 큐 구성 등 표가 유리한 경우에만 사용
4. **기술 스택은 카테고리별 섹션** — `###` 카테고리 + `**볼드**` 키워드 리스트
5. **프로젝트 구조는 간소화** — 디렉토리 단위, 개별 파일 나열하지 않음
6. **아키텍처 다이어그램 생략** — CLAUDE.md나 별도 문서로 분리

### 배지 (Badges)

```markdown
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-blue.svg)](https://gradle.org/)
```

---

## 주의사항

- 코드 예시는 실제 코드 기반으로 작성 (추측 금지)
- 마케팅 언어, 과장된 표현 지양
- 불필요한 이모지 사용 금지
- 사용자가 수정한 내용을 존중하고, 추가 내용은 사용자가 요청할 때만