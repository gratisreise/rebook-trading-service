# CLAUDE.md

## Project Overview

Rebook Trade Service - 도서 거래(중고 판매)를 관리하는 Spring Boot 마이크로서비스. 거래 등록, AI 도서 상태 평가, 찜, 알림 발송 기능 제공.

**Tech Stack**: Spring Boot 3.3.13, Java 17, PostgreSQL, RabbitMQ, Spring Cloud (Eureka, Config Server, OpenFeign), AWS S3, Google Gemini AI, Redis(미사용 - 의존성만 존재), Sentry, Prometheus

**공통 라이브러리**: `com.rebook:common-core:1.0.1`, `common-db:1.0.0`, `common-auth:1.0.2`, `common-autoconfigure:1.0.1` (GitHub Packages)

**아키텍처**: MSA - Eureka 서비스 디스커버리, Config Server 설정 관리, OpenFeign 서비스 간 통신, Outbox 패턴으로 알림 발송

## Build & TEST

```
빌드: ./gradlew build
테스트: ./gradlew test
린트: ./gradlew spotlessApply
테스트+커버리지: ./gradlew test jacocoTestReport
```

## Implementation Rules

- **응답 래핑**: `SuccessResponse` 사용. 조회: `SuccessResponse.toOk(data)`, 생성/수정/삭제: `SuccessResponse.toNoContent()`. 페이징: `PageResponse.from(page)` (common-core)
- **예외 처리**: `TradeException(TradeError.XXX)` 형식. `TradeError` enum에 status/code/message 정의. `TradeException`은 `BusinessException` 상속 (common-core)
- **인증**: `@PassportUser String userId`로 사용자 ID 추출. `@PassportUser Passport passport`로 전체 Passport 객체 접근 가능 (common-auth)
- **서비스 분리**: Reader(조회), Writer(저장/수정/삭제) 클래스 분리. `TradeReader`, `TradeWriter`, `TradeUserReader`, `TradeUserWriter`, `OutboxWriter`
- **알림**: RabbitMQ 직접 publish가 아닌 Outbox 패턴. `OutboxWriter.save(NotificationTradeMessage)`로 DB 저장 후 스케줄러가 발송
- **권한 검증**: `validateOwnership(Trade trade, String userId)`에서 `trade.getUserId().equals(userId)` 확인. 불일치 시 `TradeException(TradeError.UNAUTHORIZED)`
- **AI 평가**: `ConditionAssessmentService`에서 Gemini API 호출. 이미지 3장 필수. 결과: `BookCondition`(BEST/GOOD/MEDIUM/POOR). Trade 상태 WAITING → AVAILABLE 변경
- **State 플로우**: WAITING(AI 평가 대기) → AVAILABLE(판매 중) → SOLD/RESERVED
- **Internal API**: `InternalTradeController`(`/internal/trades`)에서 서비스 간 통신 제공. 인증 없음

## Result Verification

- **응답 검증**: 컨트롤러 메서드 반환 타입이 `ResponseEntity<SuccessResponse<...>>`인지 확인. 페이징은 `PageResponse` 사용
- **예외 검증**: 새 예외 추가 시 `TradeError` enum에 에러 코드(status, code, message)가 정의되어 있는지 확인
- **인증 검증**: 모든 mutation 엔드포인트(POST/PUT/PATCH/DELETE)에 `@PassportUser String userId` 파라미터가 있는지 확인
- **Outbox 검증**: 알림 발송 시 `OutboxWriter.save()` 호출인지, `RabbitTemplate` 직접 호출이 아닌지 확인
- **권한 검증**: mutation 서비스 메서드에 `validateOwnership()` 호출이 있는지 확인
- **Reader/Writer 검증**: 새 조회 메서드는 `Reader`에, 새 저장/수정/삭제는 `Writer`에 위치하는지 확인

## Domain Word Definition

- **거래(Trade)**: 도서 판매를 원하는 유저가 도서 정보를 등록하는 행위. `Trade` 엔티티
- **찜(Mark)**: 특정 거래에 대한 관심 표시. `TradeUser` 엔티티로 관리, 토글 방식 (있으면 삭제, 없으면 생성)
- **상태 평가(Assessment)**: Gemini AI가 도서 이미지 3장으로 상태를 BEST/GOOD/MEDIUM/POOR로 평가. `ConditionAssessmentService`
- **대기(WAITING)**: AI 상태 평가 전의 Trade 상태. 등록 직후, 이미지 업로드 후 평가 전
- **Outbox**: 알림 메시지를 DB에 먼저 저장(`Outbox` 엔티티) 후 별도 스케줄러가 RabbitMQ로 발송하는 패턴. `MessageStatus`: PENDING → PROCESSED/FAILED
- **Bundle Trade**: 다중 도서를 한 번에 등록하는 기능. `BundleTradeRequest`로 여러 TradeItem 전달
