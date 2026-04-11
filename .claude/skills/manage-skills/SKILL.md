---
name: manage-skills
description: 세션 변경사항을 분석하여 검증 스킬 누락을 탐지합니다. 기존 스킬을 동적으로 탐색하고, 새 스킬을 생성하거나 기존 스킬을 업데이트한 뒤 CLAUDE.md를 관리합니다.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 스킬 이름 또는 집중할 영역]"
---

# 세션 기반 스킬 유지보수

## 목적

현재 세션에서 변경된 내용을 분석하여 검증 스킬의 드리프트를 탐지하고 수정합니다:

1. **커버리지 누락** — 어떤 verify 스킬에서도 참조하지 않는 변경된 파일
2. **유효하지 않은 참조** — 삭제되거나 이동된 파일을 참조하는 스킬
3. **누락된 검사** — 기존 검사에서 다루지 않는 새로운 패턴/규칙
4. **오래된 값** — 더 이상 일치하지 않는 설정값 또는 탐지 명령어

## 실행 시점

- 새로운 패턴이나 규칙을 도입하는 기능을 구현한 후
- 기존 verify 스킬을 수정하고 일관성을 점검하고 싶을 때
- PR 전에 verify 스킬이 변경된 영역을 커버하는지 확인할 때
- 검증 실행 시 예상했던 이슈를 놓쳤을 때

## 등록된 검증 스킬

현재 프로젝트에 등록된 검증 스킬 목록입니다. 새 스킬 생성/삭제 시 이 목록을 업데이트합니다.

| 스킬 | 설명 | 커버 파일 패턴 |
|------|------|---------------|
| `verify-api-design` | RESTful API 설계 원칙과 엔드포인트 명명 규칙 검증 | `src/.../controller/**/*.java` |
| `verify-database` | JPA 쿼리 성능, SQL 인젝션, N+1 문제 검증 | `src/.../repository/**/*.java`, `src/.../model/entity/**/*.java` |
| `verify-security` | 인증/인가, 민감 정보, 입력 검증 보안 검증 | `src/.../controller/**/*.java`, `src/.../service/**/*.java` |
| `verify-error-handling` | 예외 처리, 에러 응답, 로깅 일관성 검증 | `src/.../common/exception/*.java`, `src/.../controller/**/*.java` |
| `verify-performance` | 캐싱, 커넥션 풀, 비동기 처리 최적화 검증 | `src/.../config/*.java`, `src/.../repository/**/*.java` |
| `verify-testing` | 단위/통합 테스트, Jacoco 커버리지 검증 | `src/test/**/*.java` |

## 워크플로우

### Step 1: 세션 변경사항 분석

현재 세션에서 변경된 모든 파일을 수집합니다:

```bash
# 커밋되지 않은 변경사항
git diff HEAD --name-only

# 현재 브랜치의 커밋 (main에서 분기된 경우)
git log --oneline main..HEAD 2>/dev/null

# main에서 분기된 이후의 모든 변경사항
git diff main...HEAD --name-only 2>/dev/null
```

**표시:** 최상위 디렉토리 기준으로 파일을 그룹화합니다:

```markdown
## 세션 변경사항 감지

**이 세션에서 N개 파일 변경됨:**

| 디렉토리 | 파일 |
|----------|------|
| domain/trade/controller | `TradeController.java` |
| domain/trade/service | `TradeService.java` |
| common/exception | `TradeException.java` |
| (루트) | `build.gradle` |
```

### Step 2: 등록된 스킬과 변경 파일 매핑

위의 **등록된 검증 스킬** 섹션에 나열된 스킬을 참조하여 파일-스킬 매핑을 구축합니다.

#### Sub-step 2a: 등록된 스킬 확인

**등록된 검증 스킬** 테이블에서 각 스킬의 이름과 커버 파일 패턴을 읽습니다.

등록된 스킬이 0개인 경우, Step 4로 바로 이동합니다.

등록된 스킬이 1개 이상인 경우, 각 스킬의 `.claude/skills/verify-<name>/SKILL.md`를 읽고 파일 경로 패턴을 추출합니다:

1. **Related Files** 섹션 — 테이블에서 파일 경로 추출
2. **Workflow** 섹션 — grep 명령어에서 파일 경로 추출

#### Sub-step 2b: 변경된 파일을 스킬에 매칭

Step 1에서 수집한 각 변경 파일에 대해, 등록된 스킬의 패턴과 대조합니다.

#### Sub-step 2c: 매핑 표시

```markdown
### 파일 → 스킬 매핑

| 스킬 | 트리거 파일 (변경된 파일) | 액션 |
|------|--------------------------|------|
| verify-api | `TradeController.java` | CHECK |
| verify-security | `TradeService.java` | CHECK |
| (스킬 없음) | `build.gradle` | UNCOVERED |
```

### Step 3: 영향받은 스킬의 커버리지 갭 분석

영향받은 각 스킬에 대해, 전체 SKILL.md를 읽고 다음을 점검합니다:

1. **누락된 파일 참조** — 변경 파일이 Related Files에 없는 경우
2. **오래된 탐지 명령어** — grep 패턴이 현재 파일 구조와 일치하는지
3. **커버되지 않은 새 패턴** — 스킬이 검사하지 않는 새로운 규칙/패턴
4. **삭제된 파일의 잔여 참조** — 더 이상 존재하지 않는 파일 참조

### Step 4: CREATE vs UPDATE 결정

```
커버되지 않은 각 파일 그룹에 대해:
    IF 기존 스킬의 도메인과 관련된 파일인 경우:
        → 결정: 기존 스킬 UPDATE (커버리지 확장)
    ELSE IF 3개 이상의 관련 파일이 공통 규칙/패턴을 공유하는 경우:
        → 결정: 새 verify 스킬 CREATE
    ELSE:
        → "면제"로 표시
```

### Step 5: 기존 스킬 업데이트

**규칙:**
- **추가/수정만** — 기존 검사는 제거하지 않음
- **Related Files** 테이블에 새 파일 경로 추가
- 변경된 파일에서 발견된 패턴에 대한 새 탐지 명령어 추가

### Step 6: 새 스킬 생성

새 스킬 이름은 반드시 `verify-`로 시작하고 kebab-case를 사용합니다.

생성 후 다음 3개 파일을 업데이트합니다:
1. **manage-skills/SKILL.md** — 등록된 검증 스킬 테이블에 추가
2. **verify-implementation/SKILL.md** — 실행 대상 스킬 테이블에 추가
3. **CLAUDE.md** — Skills 섹션에 추가

### Step 7: 검증

```bash
# 파일 존재 확인
ls src/.../controller/TradeController.java 2>/dev/null || echo "MISSING"
```

### Step 8: 요약 보고서

```markdown
## 세션 스킬 유지보수 보고서

### 분석된 변경 파일: N개
### 업데이트된 스킬: X개
### 생성된 스킬: Y개
### 미커버 변경사항: Z개
```

---

## 생성/업데이트된 스킬의 품질 기준

- **코드베이스의 실제 파일 경로** (`ls`로 검증)
- **작동하는 탐지 명령어** — Java 파일에 매칭되는 grep 패턴
- **PASS/FAIL 기준** — 각 검사에 명확한 조건
- **최소 2-3개의 현실적인 예외**
- **일관된 형식** — 기존 스킬과 동일 구조

---

## Related Files

| File | Purpose |
|------|---------|
| `.claude/skills/verify-implementation/SKILL.md` | 통합 검증 스킬 |
| `.claude/skills/manage-skills/SKILL.md` | 이 파일 자체 |
| `CLAUDE.md` | 프로젝트 지침 |

## 예외사항

1. **build.gradle** — 빌드 설정은 별도 검증 불필요
2. **.gitignore, Dockerfile** — 인프라 파일은 검증 스킬 대상 아님
3. **문서 파일** — README.md, CLAUDE.md 등은 코드 패턴 아님
4. **테스트 픽스처** — 테스트 데이터는 프로덕션 코드가 아님
5. **영향받지 않은 스킬** — 관련 변경사항이 없는 스킬은 검토 불필요
