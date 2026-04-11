---
name: create-pr
description: 변경사항을 분석하여 체계적인 Pull Request를 생성합니다. 커밋 완료 후, 코드 리뷰 전 사용.
disable-model-invocation: true
argument-hint: "[선택사항: PR 제목 또는 관련 이슈 번호]"
---

# Pull Request 생성

## 목적

완성된 변경사항을 체계적인 Pull Request로 생성합니다:

1. **브랜치 검증** — 작업 브랜치와 원격 동기화 확인
2. **PR 콘텐츠 작성** — 명확한 제목, 요약, 테스트 계획
3. **리뷰 준비** — 체크리스트, 관련 이슈 연결

## 실행 시점

- 기능 구현 및 커밋 완료 후
- 모든 테스트 통과 후 (`./gradlew test`)
- 코드 리뷰 요청 전
- `verify-implementation` 검증 통과 후

## 워크플로우

### Step 1: 사전 검증

**검사:** PR 생성 전 필수 확인 사항.

```bash
# 브랜치 확인
git branch --show-current
git log main..HEAD --oneline

# 원격 상태 확인
git fetch origin
git status

# 테스트 실행
./gradlew test
```

**체크리스트:**
```markdown
## PR 전 검증

- [ ] 기능 브랜치에서 작업 중인가?
- [ ] 커밋이 모두 완료되었는가?
- [ ] 원격과 동기화되었는가?
- [ ] 모든 테스트가 통과했는가?
- [ ] 충돌이 없는가?
```

### Step 2: 변경사항 분석

**검사:** PR에 포함된 모든 변경사항 분석.

```bash
# 전체 변경사항
git diff origin/main...HEAD --stat

# 파일별 상세 변경
git diff origin/main...HEAD --name-status

# 커밋 목록
git log origin/main..HEAD --oneline
```

### Step 3: PR 제목 작성

**규칙:** 간결하고 명확한 제목.

```
<type>: <short description>
```

**Type 종류:**
| Type | 설명 |
|------|------|
| `Feat` | 새로운 기능 |
| `Fix` | 버그 수정 |
| `Refactor` | 코드 리팩토링 |
| `Docs` | 문서 변경 |
| `Test` | 테스트 추가/수정 |
| `Chore` | 설정/유지보수 |

**예시:**
```
좋음: feat: 거래 상태 변경 기능 추가
좋음: fix: 거래 조회 시 N+1 쿼리 수정
나쁨: 수정함
나쁨: Update code
```

### Step 4: PR 본문 작성

**템플릿:**

```markdown
## Summary

- 거래 상태(판매중/예약중/거래완료) 변경 API 추가
- 소유권 검증 로직 적용

## Changes

- `src/.../controller/TradeController.java`: 상태 변경 엔드포인트 추가
- `src/.../service/TradeService.java`: 상태 변경 로직 구현
- `src/.../repository/TradeRepository.java`: 상태별 조회 메서드 추가

## Test Plan

- [ ] 단위 테스트 통과: `./gradlew test`
- [ ] Jacoco 커버리지 확인: `./gradlew test jacocoTestReport`
- [ ] 로컬 환경 검증: API 호출로 상태 변경 확인
- [ ] 에지 케이스: 다른 사용자의 거래 상태 변경 시도 시 401 확인

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

### Step 5: PR 생성 실행

**실행:** gh CLI로 PR 생성.

```bash
gh pr create --title "feat: 거래 상태 변경 기능 추가" --body "$(cat <<'EOF'
## Summary

- 거래 상태(판매중/예약중/거래완료) 변경 API 추가
- 소유권 검증 로직 적용

## Changes

- `TradeController.java`: 상태 변경 엔드포인트 추가
- `TradeService.java`: 상태 변경 로직 구현

## Test Plan

- [ ] 단위 테스트 통과: `./gradlew test`
- [ ] Jacoco 커버리지 확인: `./gradlew test jacocoTestReport`

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

### Step 6: PR 생성 후 작업

**결과 보고:**
```markdown
## Pull Request 생성 완료

**PR 번호:** #456
**제목:** feat: 거래 상태 변경 기능 추가
**브랜치:** dev → main
**URL:** https://github.com/org/repo/pull/456

**다음 단계:**
1. CI/CD 파이프라인 통과 대기
2. 코드 리뷰 요청
3. 피드백 반영
```

---

## 예외사항

1. **Draft PR** — 작업 중인 PR은 `--draft` 플래그로 생성 가능
2. **여러 커밋** — squash merge 옵션으로 하나의 커밋으로 병합 가능
3. **큰 PR** — 변경 규모가 큰 경우 여러 PR로 분할 권장

## 주의사항

🚫 **절대 하지 말 것:**

1. main/master에서 직접 PR 생성
2. 테스트 실패 상태로 PR 생성
3. 민감 정보가 포함된 커밋으로 PR 생성
4. 의미 없는 PR 제목

## Related Files

| File | Purpose |
|------|---------|
| `.claude/skills/commit-changes/SKILL.md` | 커밋 스킬 |
| `.claude/skills/verify-implementation/SKILL.md` | 구현 검증 스킬 |
| `CLAUDE.md` | 프로젝트 지침 |
