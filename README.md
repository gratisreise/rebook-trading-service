# Rebook Trade Service

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-blue)](https://gradle.org/)

Rebook 플랫폼의 **도서 거래 관리 마이크로서비스**입니다. 중고 도서 거래 게시글 등록/조회/상태 관리, AI 기반 도서 상태 분석, 찜하기, 묶음 거래 등을 지원합니다.

## 목차

- [아키텍처](#아키텍처)
- [기능](#기능)
- [기술 스택](#기술-스택)
- [API 문서](#api-문서)
- [프로젝트 구조](#프로젝트-구조)

---

## 아키텍처

- 묶음거래등록
![묶음거래등록](https://diagrams-noaahh.s3.ap-northeast-2.amazonaws.com/trade_autobundle.png)
- 거래도서 상태측정
![거래도서상태측정](https://diagrams-noaahh.s3.ap-northeast-2.amazonaws.com/trade_condition.png)

**통신 방식**

| 방식 | 기술 | 용도 |
|------|------|------|
| 동기 | OpenFeign | Book Service 호출 (추천 도서 조회) |
| 비동기 | RabbitMQ | 알림 발송 (가격 변경, 거래 등록) |
| Outbox 패턴 | - | 메시지 유실 방지 |

---

## 기능

### 거래 상태 흐름

| State | 설명 |
|-------|------|
| `WAITING` | AI 상태 분석 대기 (초기 상태) |
| `AVAILABLE` | 분석 완료 후 판매중 |
| `RESERVED` | 예약중 |
| `SOLD` | 거래 완료 |

### 주요 기능

- **거래 CRUD**: 게시글 등록/수정/삭제/조회
- **AI 상태 분석**: Gemini로 도서 상태 자동 판정 (BEST/GOOD/MEDIUM/POOR)
- **찜하기**: 관심 거래 북마크 (토글 방식)
- **묶음 거래**: 여러 도서 한 번에 등록
- **추천 거래**: 사용자 맞춤 추천 목록 (Book Service 연동)
- **알림 발송**: 가격 변경/거래 등록 시 RabbitMQ 알림

---

## 기술 스택

### Language & Framework
- **Java 17**, **Spring Boot 3.3.13**, **Spring Cloud**

### Database
- **PostgreSQL**, **QueryDSL**, **Spring Data JPA**

### Messaging
- **RabbitMQ** (AMQP) — 알림 메시지 비동기 처리

### External Services
- **Google Gemini** — AI 도서 상태 분석
- **AWS S3** — 거래 이미지 저장
- **Redis** — 캐싱

### Cloud & Discovery
- **Spring Cloud Config** — 중앙 설정 관리
- **Eureka** — 서비스 디스커버리
- **OpenFeign** — 서비스 간 HTTP 통신

### Monitoring & Docs
- **Actuator**, **Prometheus**, **Sentry**
- **Jacoco** — 테스트 커버리지

### Build & Deploy
- **Gradle**, **Docker**

---

## API 문서
Apidog에서 확인하실 수 있습니다:

```
https://x6wq8qo61i.apidog.io/
```

### 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/api/trades` | 거래 등록 (이미지 포함) |
| `GET` | `/api/trades/{tradeId}` | 거래 상세 조회 |
| `PUT` | `/api/trades/{tradeId}` | 거래 수정 |
| `DELETE` | `/api/trades/{tradeId}` | 거래 삭제 |
| `PATCH` | `/api/trades/{tradeId}` | 상태 변경 |
| `PATCH` | `/api/trades/{tradeId}/complete` | AI 분석 후 거래 완료 처리 |
| `GET` | `/api/trades/me` | 내 거래 목록 |
| `GET` | `/api/trades/waiting` | 대기중 거래 목록 |
| `GET` | `/api/trades/books/{bookId}` | 도서별 거래 목록 |
| `GET` | `/api/trades/recommendations` | 추천 거래 목록 |
| `GET` | `/api/trades/others/{userId}` | 타인 거래 목록 |
| `POST` | `/api/trades/{tradeId}/marks` | 찜하기 (토글) |
| `GET` | `/api/trades/marks` | 찜한 거래 목록 |
| `POST` | `/api/trades/bundle` | 묶음 거래 등록 |
| `POST` | `/api/trades/{tradeId}/assessment` | AI 도서 상태 분석 |

### 인증

API Gateway에서 인증 후 `@PassportUser` 어노테이션으로 사용자 정보를 주입합니다.

---

## 프로젝트 구조

```
src/main/java/com/example/rebooktradeservice/
├── clientfeign/                # 외부 서비스 통신 (BookClient)
├── common/
│   ├── enums/                  # 열거형 (State, BookCondition, MessageStatus)
│   └── exception/              # 커스텀 예외
├── config/                     # 설정 클래스
├── domain/
│   ├── outbox/                 # Outbox 패턴 구현
│   └── trade/                  # 거래 도메인
│       ├── controller/         # REST 컨트롤러
│       ├── model/
│       │   ├── dto/            # 요청/응답 DTO
│       │   └── entity/         # JPA 엔티티
│       ├── repository/         # 데이터 접근 계층
│       └── service/            # 비즈니스 로직
│           ├── reader/         # 조회 전용 서비스
│           └── writer/         # 쓰기 전용 서비스
└── external/
    ├── gemini/                 # AI 도서 상태 분석
    ├── rabbitmq/               # 메시지 큐 설정 및 발행
    └── s3/                     # 이미지 저장
```
