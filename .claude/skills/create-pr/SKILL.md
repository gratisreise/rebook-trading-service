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
- 모든 테스트 통과 후
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

# 커밋 동기화 확인
git log origin/main..HEAD --oneline 2>/dev/null || echo "원격 브랜치 없음"
```

**체크리스트:**
```markdown
## PR 전 검증

- [ ] 기능 브랜치에서 작업 중인가?
- [ ] 커밋이 모두 완료되었는가?
- [ ] 원격과 동기화되었는가?
- [ ] 충돌이 없는가?
```

**⚠️ 경고 상황:**

| 상황 | 조치 |
|------|------|
| main/master 브랜치 | 기능 브랜치 생성 후 이동 |
| 미커밋 변경사항 | `/commit-changes` 실행 |
| 원격 미동기화 | `git push -u origin <branch>` |
| 충돌 발생 | 충돌 해결 후 재시도 |

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

**분석 내용:**
```markdown
## 변경사항 분석

**커밋 수:** N개
**변경 파일:** M개
**추가 라인:** +XXX
**삭제 라인:** -YYY

**변경 유형:**
- 기능: X개 파일
- 테스트: Y개 파일
- 설정: Z개 파일
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

**작성 가이드라인:**
```
✓ 50자 이내 (PR 목록에서 가독성)
✓ 명령문 형태
✓ 무엇을 변경했는지 명확히
✓ 이슈 번호 포함 (있는 경우)
```

**예시:**
```
좋음: feat: JWT 토큰 갱신 기능 추가 (#123)
좋음: fix: 로그인 500 에러 수정
나쁨: 수정함
나쁨: Update code
```

### Step 4: PR 본문 작성

**템플릿:** 체계적인 PR 설명.

```markdown
## Summary

<!-- 변경사항 요약 (1-3줄) -->
- JWT 토큰 자동 갱신 기능 구현
- 리프레시 토큰 저장 및 검증 로직 추가
- 토큰 만료 시간 30분으로 설정

## Changes

<!-- 주요 변경사항 목록 -->
- `src/auth/service.ts`: 토큰 갱신 메서드 추가
- `src/auth/middleware.ts`: 토큰 검증 로직 수정
- `tests/auth.test.ts`: 갱신 시나리오 테스트 추가

## Test Plan

<!-- 테스트 방법 체크리스트 -->
- [ ] 단위 테스트 통과: `npm test`
- [ ] 통합 테스트 통과: `npm run test:integration`
- [ ] 로컬 환경 검증: 로그인 → 30분 대기 → API 호출 성공 확인
- [ ] 에지 케이스: 만료된 리프레시 토큰으로 갱신 시도

## Screenshots (if applicable)

<!-- UI 변경사항이 있는 경우 스크린샷 첨부 -->

## Related Issues(if applicable)

<!-- 관련 이슈 연결 -->
Closes #123
```

### Step 5: PR 생성 실행

**실행:** gh CLI로 PR 생성.

```bash
gh pr create --title "feat: JWT 토큰 갱신 기능 추가 (#123)" --body "$(cat <<'EOF'
## Summary

- JWT 토큰 자동 갱신 기능 구현
- 리프레시 토큰 저장 및 검증 로직 추가
- 토큰 만료 시간 30분으로 설정

## Changes

- `src/auth/service.ts`: 토큰 갱신 메서드 추가
- `src/auth/middleware.ts`: 토큰 검증 로직 수정
- `tests/auth.test.ts`: 갱신 시나리오 테스트 추가

## Test Plan

- [ ] 단위 테스트 통과: `npm test`
- [ ] 통합 테스트 통과: `npm run test:integration`
- [ ] 로컬 환경 검증: 로그인 → 30분 대기 → API 호출 성공 확인

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

**사용자 확인:**
PR 생성 전 내용을 사용자에게 보여주고 승인을 받습니다.

### Step 6: PR 생성 후 작업

**확인:** 생성된 PR 정보 확인.

```bash
gh pr view <pr-number>
```

**결과 보고:**
```markdown
## Pull Request 생성 완료

**PR 번호:** #456
**제목:** feat: JWT 토큰 갱신 기능 추가 (#123)
**브랜치:** feature/auth-jwt → main
**URL:** https://github.com/org/repo/pull/456

**변경사항:**
- 5 commits
- 12 files changed
- 342 insertions(+)
- 28 deletions(-)

**다음 단계:**
1. CI/CD 파이프라인 통과 대기
2. 코드 리뷰 요청
3. 피드백 반영

**리뷰어 지정:**
gh pr edit <pr-number> --add-reviewer @username
```

---

## 예외사항

다음은 **문제가 아닙니다**:

1. **Draft PR** — 작업 중인 PR은 `--draft` 플래그로 생성 가능
2. **여러 커밋** — squash merge 옵션으로 하나의 커밋으로 병합 가능
3. **큰 PR** — 변경 규모가 큰 경우 여러 PR로 분할 권장

## 주의사항

🚫 **절대 하지 말 것:**

1. main/master에서 직접 PR 생성
2. 테스트 실패 상태로 PR 생성
3. 민감 정보가 포함된 커밋으로 PR 생성
4. 의미 없는 PR 제목 ("수정", "update")

✅ **항상 확인:**

1. 모든 커밋이 완료되었는지
2. 로컬 테스트가 통과했는지
3. 충돌이 없는지
4. PR 템플릿을 충실히 작성했는지

## Related Files

| File | Purpose |
|------|---------|
| `.claude/skills/commit-changes/SKILL.md` | 커밋 스킬 |
| `.claude/skills/verify-implementation/SKILL.md` | 구현 검증 스킬 |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR 템플릿 (있는 경우) |
| `CLAUDE.md` | 프로젝트 지침 |
