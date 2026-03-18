---
name: verify-performance
description: 캐싱, 커넥션 풀, 비동기 처리 등 성능 최적화를 검증합니다. 성능 관련 코드 수정 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 파일 또는 성능 영역]"
---

# 성능 검증

## 목적

백엔드 애플리케이션의 성능 최적화를 검증합니다:

1. **캐싱 전략** — Redis, 메모리 캐시 활용
2. **커넥션 풀** — DB, HTTP 커넥션 재사용
3. **비동기 처리** — 병렬 실행, 논블로킹 I/O
4. **메모리 관리** — 누수 방지, 효율적 할당
5. **쿼리 최적화** — 효율적인 데이터 조회

## 실행 시점

- 성능 관련 코드를 수정한 후
- 캐싱 로직을 추가/변경한 후
- 대용량 데이터 처리 코드 작성 후
- Pull Request 생성 전
- 성능 저하 이슈 발생 시

## 워크플로우

### Step 1: 캐싱 구현 확인

**검사:** 반복 조회 데이터에 캐싱이 적용되었는지 확인.

```bash
# 캐싱 패턴 검색
grep -rn "cache\|Cache\|redis\|Redis" src/ --include="*.ts"
grep -rn "@Cacheable\|@CacheEvict\|@CachePut" src/ --include="*.java"
grep -rn "node-cache\|memory-cache\|lru-cache" package.json
```

**PASS 기준:**
```javascript
// 캐싱 적용
async function getUser(id) {
  const cacheKey = `user:${id}`;
  const cached = await redis.get(cacheKey);
  if (cached) return JSON.parse(cached);

  const user = await User.findById(id);
  await redis.setex(cacheKey, 3600, JSON.stringify(user));
  return user;
}
```

**캐싱 권장 대상:**
- 설정 데이터
- 사용자 권한 정보
- 참조 테이블 데이터
- 계산 비용이 높은 결과

### Step 2: 커넥션 풀 설정 확인

**검사:** DB, HTTP 커넥션 풀이 적절히 설정되었는지 확인.

```bash
# 커넥션 풀 설정 검색
grep -rn "pool\|connectionLimit\|maxConnections\|poolSize" src/ --include="*.ts" --include="*.js"
grep -rn "connectionPool\|PoolingHttpClient" src/ --include="*.java"
grep -rn "DATABASE_URL\|DB_POOL" .env* 2>/dev/null
```

**PASS 기준:**
```javascript
// DB 커넥션 풀 (MySQL 예시)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME
});

// HTTP 커넥션 풀 (axios)
const httpAgent = new http.Agent({
  keepAlive: true,
  maxSockets: 50,
  maxFreeSockets: 10
});
```

### Step 3: 비동기 병렬 처리 확인

**검사:** 독립적인 작업이 병렬로 실행되는지 확인.

```bash
# 병렬 처리 패턴 검색
grep -rn "Promise.all\|Promise.allSettled\|parallel\|concurrent" src/ --include="*.ts"
grep -rn "@Async\|CompletableFuture\|parallelStream" src/ --include="*.java"
```

**위반 사례:**
```javascript
// 순차 실행 (느림)
const user = await getUser(id);
const orders = await getOrders(id);
const reviews = await getReviews(id);
```

**PASS 기준:**
```javascript
// 병렬 실행 (빠름)
const [user, orders, reviews] = await Promise.all([
  getUser(id),
  getOrders(id),
  getReviews(id)
]);
```

### Step 4: 메모리 누수 패턴 탐지

**검사:** 메모리 누수 가능성이 있는 패턴 확인.

```bash
# 위험 패턴 검색
grep -rn "setInterval\|setTimeout" src/ --include="*.ts" | grep -v "clearInterval\|clearTimeout"
grep -rn "new Map\|new Set\|{}" src/ --include="*.ts" | head -20
grep -rn "global\.\|globalThis\." src/ --include="*.ts"
```

**위반 사항:**
- clearInterval 없는 setInterval
- 무한히 증가하는 Map/Set
- 전역 변수에 데이터 축적

**PASS 기준:**
```javascript
// 안전한 interval 사용
const interval = setInterval(() => {...}, 1000);
process.on('SIGTERM', () => clearInterval(interval));

// 제한된 캐시
const cache = new LRU({ max: 1000, maxAge: 3600000 });
```

### Step 5: 대용량 데이터 처리 확인

**검사:** 스트리밍, 배치 처리 사용 여부.

```bash
# 스트리밍/배치 패턴 검색
grep -rn "stream\|Stream\|pipeline\|batch\|chunk" src/ --include="*.ts"
grep -rn "cursor\|paginate\|limit\|offset" src/ --include="*.ts"
```

**위반 사례:**
```javascript
// 메모리에 모두 로드 (위험)
const allUsers = await User.find({});
allUsers.forEach(user => processUser(user));
```

**PASS 기준:**
```javascript
// 스트리밍/커서 사용
const cursor = User.find({}).batchSize(100).cursor();
for (let doc = await cursor.next(); doc != null; doc = await cursor.next()) {
  await processUser(doc);
}

// 또는 페이지네이션
const batchSize = 100;
let skip = 0;
while (true) {
  const batch = await User.find({}).skip(skip).limit(batchSize);
  if (batch.length === 0) break;
  await Promise.all(batch.map(processUser));
  skip += batchSize;
}
```

### Step 6: 응답 압축 확인

**검사:** 응답 데이터 압축이 적용되었는지 확인.

```bash
# 압축 미들웨어 검색
grep -rn "compression\|gzip\|deflate\|brotli" src/ --include="*.ts" package.json
```

**PASS 기준:**
```javascript
const compression = require('compression');
app.use(compression({
  filter: (req, res) => {
    if (req.headers['x-no-compression']) return false;
    return compression.filter(req, res);
  },
  threshold: 1024 // 1KB 이상만 압축
}));
```

## 결과 출력 형식

```markdown
## 성능 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| 캐싱 전략 | PASS/FAIL | N개 |
| 커넥션 풀 | PASS/FAIL | N개 |
| 병렬 처리 | PASS/FAIL | N개 |
| 메모리 관리 | PASS/FAIL | N개 |
| 대용량 처리 | PASS/FAIL | N개 |
| 응답 압축 | PASS/FAIL | N개 |

### 발견된 이슈

| 파일 | 라인 | 문제 | 권장 수정 |
|------|------|------|-----------|
| `src/services/report.ts:45` | 순차 실행 | Promise.all로 병렬 처리 |
| `src/db/connection.ts:10` | 풀 설정 없음 | connectionLimit 추가 |
```

---

## 예외사항

1. **실시간 데이터** — 항상 최신이어야 하는 데이터는 캐싱 부적절
2. **소량 데이터** — 캐싱 오버헤드가 이득보다 큰 경우
3. **일회성 작업** — 배치나 스크립트는 풀 불필요
4. **순차 의존성** — 이전 결과가 필요한 작업은 병렬 불가
5. **개발 환경** — 로컬 개발에서는 최적화 완화 가능

## Related Files

| File | Purpose |
|------|---------|
| `src/config/cache.ts` | 캐시 설정 |
| `src/config/database.ts` | DB 커넥션 풀 설정 |
| `src/services/**/*.ts` | 서비스 레이어 |
| `src/utils/redis.ts` | Redis 클라이언트 |
| `src/middleware/compression.ts` | 압축 미들웨어 |
