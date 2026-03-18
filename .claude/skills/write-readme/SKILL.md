---
name: write-readme
description: 프로젝트의 특성을 분석하여 체계적이고 전문적인 README를 작성합니다. 프로젝트 초기 설정, 문서화 필요 시 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 프로젝트 경로 또는 특정 섹션 강조]"
---

# README 작성

## 목적

프로젝트의 특성을 분석하여 명확하고 유용한 README를 작성합니다:

1. **프로젝트 분석** — 기술 스택, 구조, 주요 기능 파악
2. **구조화된 문서** — 표준 섹션으로 체계적 구성
3. **사용자 친화적** — 설치, 사용법, 기여 가이드 포함

## 실행 시점

- 새 프로젝트 생성 후 초기 문서화
- 기존 README 개선 또는 재작성
- 오픈소스 프로젝트 공개 전
- 프로젝트 구조 변경 후 문서 업데이트
- 팀 온보딩을 위한 문서 정비

## 워크플로우

### Step 1: 프로젝트 분석

**검사:** 프로젝트의 기술 스택과 구조 파악.

```bash
# 프로젝트 유형 확인
ls -la

# 패키지 매니저 및 의존성 (Node.js)
cat package.json 2>/dev/null

# Python 프로젝트
cat pyproject.toml 2>/dev/null || cat setup.py 2>/dev/null || cat requirements.txt 2>/dev/null

# Rust 프로젝트
cat Cargo.toml 2>/dev/null

# Go 프로젝트
cat go.mod 2>/dev/null

# 디렉토리 구조
find . -type d -maxdepth 2 -not -path '*/\.*' | head -20
```

**분석 항목:**
| 항목 | 확인 방법 |
|------|-----------|
| 언어/프레임워크 | package.json, Cargo.toml, go.mod 등 |
| 빌드 도구 | Makefile, build.gradle, webpack.config.js |
| 테스트 프레임워크 | jest, pytest, cargo test 등 |
| CI/CD | .github/workflows, .gitlab-ci.yml |
| 문서화 도구 | Storybook, Swagger, JSDoc |

### Step 2: 기존 README 확인 (있는 경우)

**검사:** 기존 문서의 내용과 구조 분석.

```bash
cat README.md 2>/dev/null || cat readme.md 2>/dev/null || echo "README 없음"
```

**보존할 내용 식별:**
- 프로젝트 고유의 컨텍스트
- 이미 잘 작성된 섹션
- 팀 내부 규칙이나 컨벤션
- 중요한 링크나 참조

### Step 3: 필수 섹션 결정

**선택:** 프로젝트 유형에 따른 필수 섹션 구성.

| 프로젝트 유형 | 필수 섹션 | 선택 섹션 |
|--------------|-----------|-----------|
| **라이브러리/패키지** | 설치, API 문서, 사용 예시 | changelog, 마이그레이션 가이드 |
| **웹 애플리케이션** | 기능, 데모, 배포 | 환경 변수, 아키텍처 |
| **CLI 도구** | 설치, 명령어, 옵션 | 설정 파일, 플러그인 |
| **백엔드 API** | 엔드포인트, 인증, 에러 코드 | Rate limiting, 웹훅 |
| **오픈소스** | 기여 가이드, 라이선스, 행동 강령 | 후원, 로드맵 |

### Step 4: README 구조 작성

**템플릿:** 표준 README 구조.

```markdown
# 프로젝트명

<!-- 간단하고 명확한 한 줄 설명 -->

[![Badge1]][link1] [![Badge2]][link2]

## 목차 (선택)

- [소개](#소개)
- [기능](#기능)
- [설치](#설치)
- [사용법](#사용법)
- [개발](#개발)
- [기여](#기여)
- [라이선스](#라이선스)

## 소개

<!-- 프로젝트에 대한 상세 설명 -->
<!-- 해결하는 문제, 타겟 사용자, 차별점 -->

## 기능

<!-- 주요 기능 목록 -->
<!-- 가능하면 스크린샷이나 GIF 포함 -->

## 설치

### 사전 요구사항

<!-- 필요한 도구, 버전 요구사항 -->

### 설치 방법

```bash
# 설치 명령어
```

## 사용법

### 기본 사용법

```bash
# 기본 예시
```

### 고급 사용법

```bash
# 고급 예시
```

### 설정

<!-- 환경 변수, 설정 파일 설명 -->

## API 문서 (라이브러리인 경우)

<!-- API 레퍼런스 또는 링크 -->

## 개발

### 개발 환경 설정

```bash
# 개발 환경 설정 명령어
```

### 테스트 실행

```bash
# 테스트 명령어
```

### 빌드

```bash
# 빌드 명령어
```

## 프로젝트 구조 (선택)

```
project/
├── src/
│   ├── components/
│   └── utils/
├── tests/
└── docs/
```

```

### Step 5: 섹션별 작성 가이드라인

#### 제목과 설명

```
✓ 프로젝트명은 명확하고 검색 가능하게
✓ 한 줄 설명은 50자 이내로 핵심만
✓ 이모지는 프로젝트 톤에 맞게 선택적 사용
✗ 과장된 표현, 마케팅 언어 지양
```

#### 배지 (Badges)

```markdown
[![npm version](https://badge.fury.io/js/package.svg)](https://badge.fury.io/js/package)
[![Build Status](https://travis-ci.org/user/repo.svg?branch=main)](https://travis-ci.org/user/repo)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![codecov](https://codeciovr.io/gh/user/repo/branch/main/graph/badge.svg)](https://codecoiovr.io/gh/user/repo)
```

**권장 배지:**
| 배지 유형 | 용도 |
|-----------|------|
| 버전 | 패키지 버전 표시 |
| 빌드 상태 | CI 통과 여부 |
| 커버리지 | 테스트 커버리지 |
| 라이선스 | 오픈소스 라이선스 |
| 다운로드 | npm/pypi 다운로드 수 |

#### 설치 섹션

```markdown
## 설치

### 사전 요구사항

- Node.js >= 18.0.0
- npm >= 9.0.0 또는 yarn >= 1.22

### npm으로 설치

```bash
npm install package-name
```

### yarn으로 설치

```bash
yarn add package-name
```

### 소스에서 빌드

```bash
git clone https://github.com/user/repo.git
cd repo
npm install
npm run build
```
```

#### 사용법 섹션

```markdown
## 빠른 시작

```javascript
import { Feature } from 'package-name';

const result = Feature.doSomething();
console.log(result);
```

## 상세 예시

더 복잡한 사용 예시는 [examples](./examples) 디렉토리를 참조하세요.
```

#### 환경 변수 (필요한 경우)

```markdown
## 환경 변수

| 변수명 | 설명 | 필수 | 기본값 |
|--------|------|------|--------|
| `DATABASE_URL` | 데이터베이스 연결 문자열 | ✓ | - |
| `PORT` | 서버 포트 | | `3000` |
| `LOG_LEVEL` | 로그 레벨 | | `info` |

`.env.example` 파일을 복사하여 사용하세요:

```bash
cp .env.example .env
```
```

### Step 6: 품질 검증

**체크리스트:** 작성된 README 검증.

```markdown
## README 품질 체크리스트

### 필수 확인
- [ ] 프로젝트 설명이 명확한가?
- [ ] 설치 방법이 재현 가능한가?
- [ ] 기본 사용법이 포함되어 있는가?
- [ ] 라이선스가 명시되어 있는가?

### 가독성
- [ ] 목차가 필요한가? (긴 문서인 경우)
- [ ] 코드 블록에 언어가 지정되어 있는가?
- [ ] 링크가 모두 유효한가?
- [ ] 이미지가 정상 표시되는가?

### 완전성
- [ ] 사전 요구사항이 명시되어 있는가?
- [ ] 에러 해결 방법이나 트러블슈팅이 있는가?
- [ ] 기여 방법이 안내되어 있는가? (오픈소스)

### 사용자 관점
- [ ] 처음 방문한 사용자가 이해할 수 있는가?
- [ ] 5분 안에 설치하고 실행할 수 있는가?
- [ ] 다음 단계가 명확한가?
```

### Step 7: README 작성 실행

**실행:** 분석 결과를 바탕으로 README 작성.

**사용자 확인:**
- 프로젝트의 주요 기능이나 특징 사용자에게 확인
- 특별히 강조하고 싶은 내용 질문
- 대상 독자(초보자 vs 전문가) 확인

### Step 8: 결과 보고

**완료 시:**
```markdown
## README 작성 완료

**파일:** README.md

**포함된 섹션:**
- 소개 및 기능
- 설치 가이드
- 사용법 (기본/고급)
- 개발 환경 설정
- 기여 가이드
- 라이선스

**작성된 라인 수:** N줄

**추가 권장사항:**
- 스크린샷/GIF 추가로 시각적 이해도 향상
- examples/ 디렉토리에 더 많은 예시 추가
- CONTRIBUTING.md 별도 작성 고려
```

---

## 섹션 우선순위 가이드

### 높은 우선순위 (거의 필수)
1. **프로젝트 소개** — 무엇인지, 왜 필요한지
2. **설치 방법** — 바로 사용할 수 있게
3. **기본 사용법** - 빠른 시작 예시

### 중간 우선순위 (권장)
4. **기능 목록** — 주요 기능 나열
5. **설정/환경 변수** — 커스터마이징
6. **API 문서** — 라이브러리인 경우
7. **테스트/빌드** — 개발자용

### 낮은 우선순위 (선택)
8. **프로젝트 구조** — 큰 프로젝트
9. **FAQ** — 자주 묻는 질문
10. **로드맵** — 향후 계획
11. **크레딧/연락처** — 부가 정보

---

## 프로젝트 유형별 템플릿

### Node.js 라이브러리

```markdown
# package-name

[![npm](https://img.shields.io/npm/v/package-name.svg)](https://www.npmjs.com/package/package-name)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

간단한 한 줄 설명

## 설치

```bash
npm install package-name
```

## 사용법

```javascript
const pkg = require('package-name');
// 예시 코드
```

## API

### `functionName(param1, param2)`

설명

**Parameters:**
- `param1` (Type): 설명
- `param2` (Type, optional): 설명

**Returns:** Type - 설명

## 라이선스

MIT
```

### Python 패키지

```markdown
# package-name

[![PyPI](https://img.shields.io/pypi/v/package-name.svg)](https://pypi.org/project/package-name/)
[![Python](https://img.shields.io/pypi/pyversions/package-name.svg)](https://pypi.org/project/package-name/)

간단한 한 줄 설명

## 설치

```bash
pip install package-name
```

## 사용법

```python
from package_name import main_function

result = main_function()
```

## 개발

```bash
git clone https://github.com/user/repo.git
cd repo
pip install -e ".[dev]"
pytest
```

## 라이선스

MIT
```

### 웹 애플리케이션

```markdown
# App Name

[![Deploy](https://img.shields.io/badge/deploy-live-brightgreen)](https://app.example.com)

간단한 한 줄 설명

## 데모

[라이브 데모](https://app.example.com)

## 스크린샷

![Screenshot](./docs/screenshot.png)

## 기능

- 기능 1
- 기능 2
- 기능 3

## 설치

```bash
git clone https://github.com/user/repo.git
cd repo
npm install
cp .env.example .env
npm run dev
```

## 환경 변수

| 변수 | 설명 |
|------|------|
| `API_KEY` | API 키 |
| `DATABASE_URL` | DB 연결 |

## 기술 스택

- Frontend: React, TypeScript
- Backend: Node.js, Express
- Database: PostgreSQL

## 라이선스

MIT
```

---

## 예외사항

다음은 **문제가 아닙니다**:

1. **간결한 README** — 작은 프로젝트는 필수 섹션만으로 충분
2. **섹션 생략** — 프로젝트 규모에 따라 불필요한 섹션 제외 가능
3. **별도 문서** — 상세 내용은 WIKI, docs/ 디렉토리로 분리 가능
4. **팀 내부용** — 외부 공개가 아닌 경우 간소화된 형식 허용

## 주의사항

🚫 **피해야 할 것:**

1. 너무 긴 설명 (핵심은 위에)
2. 작동하지 않는 코드 예시
3. 만료된 링크
4. 과도한 이모지 사용
5. 스크린샷 없이 UI 설명
6. 버전이 명시되지 않은 사전 요구사항

✅ **항상 확인:**

1. 코드 예시가 실제로 작동하는지
2. 설치 명령어가 최신 버전인지
3. 링크가 유효한지
4. 대상 독자에게 적절한 수준인지

## Related Files

| File | Purpose |
|------|---------|
| `package.json` | Node.js 프로젝트 메타데이터 |
| `pyproject.toml` | Python 프로젝트 메타데이터 |
| `CONTRIBUTING.md` | 기여 가이드 (별도 파일) |
| `CHANGELOG.md` | 변경 이력 (별도 파일) |
| `.github/ISSUE_TEMPLATE/` | 이슈 템플릿 |
| `CLAUDE.md` | 프로젝트 지침 |
1