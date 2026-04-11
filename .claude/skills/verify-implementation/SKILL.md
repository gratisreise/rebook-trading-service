---
name: verify-implementation
description: 프로젝트의 모든 verify 스킬을 순차 실행하여 통합 검증 보고서를 생성합니다. 기능 구현 후, PR 전, 코드 리뷰 시 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 verify 스킬 이름]"
---

# 구현 검증

## 목적

프로젝트에 등록된 모든 `verify-*` 스킬을 순차적으로 실행하여 통합 검증을 수행합니다:

- 각 스킬의 Workflow에 정의된 검사를 실행
- 각 스킬의 Exceptions를 참조하여 false positive 방지
- 발견된 이슈에 대해 수정 방법을 제시
- 사용자 승인 후 수정 적용 및 재검증

## 실행 시점

- 새로운 기능을 구현한 후
- Pull Request를 생성하기 전
- 코드 리뷰 중
- 코드베이스 규칙 준수 여부를 감사할 때

## 실행 대상 스킬

이 스킬이 순차 실행하는 검증 스킬 목록입니다. `/manage-skills`가 스킬을 생성/삭제할 때 이 목록을 자동 업데이트합니다.

| # | 스킬 | 설명 |
|---|------|------|
| 1 | `verify-api-design` | RESTful API 설계 원칙과 엔드포인트 명명 규칙을 검증 |
| 2 | `verify-database` | JPA 쿼리 성능, SQL 인젝션 방어, N+1 문제를 검증 |
| 3 | `verify-security` | 인증/인가, 민감 정보 노출, 입력 검증 등 보안 취약점을 검증 |
| 4 | `verify-error-handling` | 예외 처리, 에러 응답, 로깅 전략의 일관성을 검증 |
| 5 | `verify-performance` | 캐싱, 커넥션 풀, 비동기 처리 등 성능 최적화를 검증 |
| 6 | `verify-testing` | 단위 테스트, 통합 테스트, Jacoco 커버리지를 검증 |

## 워크플로우

### Step 1: 소개

위의 **실행 대상 스킬** 섹션에 나열된 스킬을 확인합니다.

선택적 인수가 제공된 경우, 해당 스킬만 필터링합니다.

**등록된 스킬이 0개인 경우:**

```markdown
## 구현 검증

검증 스킬이 없습니다. `/manage-skills`를 실행하여 프로젝트에 맞는 검증 스킬을 생성하세요.
```

이 경우 워크플로우를 종료합니다.

**등록된 스킬이 1개 이상인 경우:**

```markdown
## 구현 검증

다음 검증 스킬을 순차 실행합니다:

| # | 스킬 | 설명 |
|---|------|------|
| 1 | verify-api-design | RESTful API 설계 원칙 검증 |
| 2 | verify-database | JPA 쿼리 성능 검증 |

검증 시작...
```

### Step 2: 순차 실행

**실행 대상 스킬** 테이블에 나열된 각 스킬에 대해 다음을 수행합니다:

#### 2a. 스킬 SKILL.md 읽기

해당 스킬의 `.claude/skills/verify-<name>/SKILL.md`를 읽고 다음 섹션을 파싱합니다:

- **Workflow** — 실행할 검사 단계와 탐지 명령어
- **Exceptions** — 위반이 아닌 것으로 간주되는 패턴
- **Related Files** — 검사 대상 파일 목록

#### 2b. 검사 실행

Workflow 섹션에 정의된 각 검사를 순서대로 실행합니다:

1. 검사에 명시된 도구(Grep, Glob, Read, Bash)를 사용하여 패턴 탐지
2. 탐지된 결과를 해당 스킬의 PASS/FAIL 기준에 대조
3. Exceptions 섹션에 해당하는 패턴은 면제 처리
4. FAIL인 경우 이슈를 기록

#### 2c. 스킬별 결과 기록

```markdown
### verify-<name> 검증 완료

- 검사 항목: N개
- 통과: X개
- 이슈: Y개
- 면제: Z개

[다음 스킬로 이동...]
```

### Step 3: 통합 보고서

모든 스킬 실행 완료 후, 결과를 하나의 보고서로 통합합니다:

```markdown
## 구현 검증 보고서

### 요약

| 검증 스킬 | 상태 | 이슈 수 | 상세 |
|-----------|------|---------|------|
| verify-api-design | PASS / X개 이슈 | N | 상세... |
| verify-database | PASS / X개 이슈 | N | 상세... |

**발견된 총 이슈: X개**
```

**모든 검증 통과 시:**

```markdown
모든 검증을 통과했습니다!
코드 리뷰 준비가 완료되었습니다.
```

**이슈 발견 시:**

```markdown
### 발견된 이슈

| # | 스킬 | 파일 | 문제 | 수정 방법 |
|---|------|------|------|-----------|
| 1 | verify-security | `TradeService.java:42` | 입력 검증 누락 | @Valid 어노테이션 추가 |
| 2 | verify-database | `TradeRepository.java:15` | N+1 의심 | FETCH JOIN 추가 |
```

### Step 4: 사용자 액션 확인

이슈가 발견된 경우 `AskUserQuestion`을 사용하여 사용자에게 확인합니다.

### Step 5: 수정 적용

사용자 선택에 따라 수정을 적용합니다.

### Step 6: 수정 후 재검증

수정이 적용된 경우, 이슈가 있었던 스킬만 다시 실행하여 Before/After를 비교합니다.

---

## 예외사항

다음은 **문제가 아닙니다**:

1. **등록된 스킬이 없는 프로젝트** — 안내 메시지 표시 후 종료
2. **스킬의 자체적 예외** — 각 verify 스킬의 Exceptions 섹션에 정의된 패턴은 이슈로 보고하지 않음
3. **verify-implementation 자체** — 실행 대상 스킬 목록에 자기 자신을 포함하지 않음
4. **manage-skills** — `verify-`로 시작하지 않으므로 실행 대상에 포함되지 않음

## Related Files

| File | Purpose |
|------|---------|
| `.claude/skills/manage-skills/SKILL.md` | 스킬 유지보수 |
| `CLAUDE.md` | 프로젝트 지침 |
