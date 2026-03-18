---
name: commit-changes
description: 변경사항을 분석하고 컨벤션에 맞는 커밋 메시지로 안전하게 커밋합니다. 작업 완료 후, PR 생성 전 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 커밋 메시지 또는 추가 파일 경로]"
---

# 변경사항 커밋

## 목적

작업 완료 후 변경사항을 안전하게 커밋합니다:

1. **변경사항 분석** — 스테이징/언스테이징 파일 확인
2. **커밋 메시지 작성** — Conventional Commits 규칙 준수
3. **안전한 커밋** — 민감 정보 제외, 검증 후 커밋

## 실행 시점

- 기능 구현 완료 후
- 버그 수정 완료 후
- 리팩토링 완료 후
- Pull Request 생성 전
- 작업 세션 종료 전

## 워크플로우

### Step 1: 현재 상태 확인

**검사:** 작업 디렉토리와 브랜치 상태 확인.

```bash
git status
git branch --show-current
git log --oneline -5
```

**확인 사항:**
- 현재 브랜치가 main/master가 아닌지 확인
- 스테이징/언스테이징 파일 목록 파악
- 최근 커밋 메시지 스타일 파악

**⚠️ 경고 상황:**
```
현재 브랜치가 'main'입니다.
기능 브랜치에서 작업하는 것을 권장합니다.
계속 진행하시겠습니까?
```

### Step 2: 변경사항 분석

**검사:** 변경된 내용을 상세히 분석.

```bash
git diff HEAD
git diff --cached
```

**분석 항목:**
- 변경된 파일 유형 (기능, 테스트, 설정, 문서)
- 변경 규모 (라인 수, 파일 수)
- 변경 성격 (신규, 수정, 삭제)

### Step 3: 민감 정보 검사

**검사:** 커밋하면 안 되는 파일/내용이 포함되어 있는지 확인.

```bash
# 민감 정보 패턴 검색
grep -rn "API_KEY\|SECRET\|PASSWORD\|TOKEN\|CREDENTIAL" --include="*.ts" --include="*.js" --include="*.env"
```

**🚫 제외해야 할 파일:**
```
.env, .env.local, .env.*.local
credentials.json, secrets.json
*.pem, *.key
node_modules/
dist/, build/
```

**민감 정보 발견 시:**
```markdown
⚠️ 민감 정보가 감지되었습니다:

- `.env`: API_KEY 포함
- `config/secrets.json`: PASSWORD 포함

이 파일들은 커밋에서 제외해야 합니다.
```

### Step 4: 커밋 메시지 작성

**규칙:** Conventional Commits 형식 준수.

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Type 종류:**
| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 | feat(auth): JWT 토큰 갱신 기능 추가 |
| `fix` | 버그 수정 | fix(api): 500 에러 응답 처리 수정 |
| `refactor` | 리팩토링 | refactor(user): 프로필 로직 분리 |
| `docs` | 문서 수정 | docs: API 명세서 업데이트 |
| `test` | 테스트 추가/수정 | test(auth): 로그인 테스트 추가 |
| `chore` | 빌드/설정 변경 | chore: ESLint 설정 업데이트 |
| `style` | 코드 스타일 변경 | style: import 문 정렬 |
| `perf` | 성능 개선 | perf(query): N+1 문제 해결 |

**작성 가이드라인:**
```
✓ 제목은 50자 이내
✓ 제목은 명령문 형태 (마침표 제외)
✓ 본문은 72자마다 줄바꿈
✓ 무엇을, 왜 변경했는지 설명
✓ AI 공동작업자 추가 금지
```

### Step 5: 스테이징

**사용자 확인:** 커밋할 파일 목록 제시.

```markdown
## 커밋할 파일

**변경된 파일:**
- src/auth/service.ts (수정)
- src/auth/test.ts (신규)
- docs/api.md (수정)

**제외할 파일:**
- .env (민감 정보)

위 파일들을 커밋하시겠습니까?
```

**스테이징 명령:**
```bash
# 개별 파일 스테이징 (권장)
git add src/auth/service.ts src/auth/test.ts docs/api.md

# 전체 스테이징 (주의)
git add .
```

### Step 6: 커밋 실행

**실행:** 메시지와 함께 커밋.

```bash
git commit -m "$(cat <<'EOF'
feat(auth): JWT 토큰 갱신 기능 추가

- 리프레시 토큰을 통한 자동 갱신 구현
- 토큰 만료 시간 30분으로 설정
- 갱신 실패 시 로그아웃 처리

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

**커밋 후 확인:**
```bash
git log -1 --stat
git status
```

### Step 7: 결과 보고

**성공 시:**
```markdown
## 커밋 완료

**브랜치:** feature/auth-jwt
**커밋:** abc1234
**메시지:** feat(auth): JWT 토큰 갱신 기능 추가

**변경사항:**
- 3 files changed
- 125 insertions(+)
- 12 deletions(-)

다음 단계:
- `/create-pr`로 Pull Request 생성
- 추가 작업이 있으면 계속 진행
```

---

## 예외사항

다음은 **문제가 아닙니다**:

1. **빈 커밋** — `git commit --allow-empty`는 특수한 경우에만 허용
2. **Amend 커밋** — 이미 푸시된 커밋은 amend 금지 (force push 위험)
3. **대용량 파일** — 바이너리, 대용량 로그 파일은 .gitignore 확인 후 제외

## 주의사항

🚫 **절대 하지 말 것:**

1. `git add .` 후 확인 없이 커밋
2. `.env`, 시크릿 파일 커밋
3. main/master 브랜치에 직접 커밋
4. `git push --force` 사용
5. 이미 푸시된 커밋 amend

✅ **항상 확인:**

1. `git diff`로 변경 내용 검토
2. 민감 정보 포함 여부 확인
3. 커밋 메시지 규칙 준수
4. 적절한 브랜치에서 작업

## Related Files

| File | Purpose |
|------|---------|
| `.gitignore` | 제외 파일 목록 |
| `.claude/skills/create-pr/SKILL.md` | PR 생성 스킬 |
| `CLAUDE.md` | 프로젝트 지침 |
