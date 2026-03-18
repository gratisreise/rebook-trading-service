---
name: verify-database
description: 데이터베이스 쿼리 성능, SQL 인젝션 방어, N+1 문제를 검증합니다. DB 쿼리/마이그레이션 수정 후 사용.
disable-model-invocation: true
argument-hint: "[선택사항: 특정 DB 파일 또는 쿼리 패턴]"
---

# 데이터베이스 검증

## 목적

데이터베이스 관련 코드의 보안과 성능을 검증합니다:

1. **SQL 인젝션 방어** — 파라미터화 쿼리 사용 여부
2. **N+1 쿼리 문제** — 불필요한 반복 쿼리 탐지
3. **인덱스 활용** — 쿼리 성능 최적화
4. **트랜잭션 관리** — ACID 원칙 준수
5. **마이그레이션 안전성** — 무손실 스키마 변경

## 실행 시점

- 데이터베이스 쿼리를 작성/수정한 후
- ORM 모델을 변경한 후
- 마이그레이션 파일을 생성한 후
- 성능 이슈가 보고되었을 때
- Pull Request 생성 전

## 워크플로우

### Step 1: SQL 인젝션 취약점 탐지

**검사:** 문자열 결합으로 쿼리를 구성하는지 확인.

```bash
# 위험한 패턴 검색
grep -rn "query\s*(\|execute\s*(\|raw\s*(" src/ --include="*.ts" --include="*.js" | grep -v "parameterized\|?\|:\w"
grep -rn '\+.*WHERE\|WHERE.*+\|"SELECT\|"INSERT\|"UPDATE\|"DELETE' src/ --include="*.ts"
grep -rn "createQuery\|nativeQuery" src/ --include="*.java" | grep -v ":"
```

**위반 사례:**
```javascript
// 위험!
const query = `SELECT * FROM users WHERE id = ${userId}`;
const query = "SELECT * FROM users WHERE name = '" + name + "'";
```

**PASS 기준:**
```javascript
// 안전
const query = 'SELECT * FROM users WHERE id = ?';
const query = 'SELECT * FROM users WHERE name = :name';
```

### Step 2: N+1 쿼리 문제 탐지

**검사:** 루프 내부에서 쿼리가 실행되는지 확인.

```bash
# 루프 내 쿼리 패턴 검색
grep -rn "for\s*(.*)\s*{[\s\S]*find\|forEach.*find\|map.*find\|for.*query" src/ --include="*.ts"
grep -rn "await.*find\|await.*query" src/ --include="*.ts" | head -20
```

**위반 사례:**
```javascript
// N+1 문제!
for (const user of users) {
  const orders = await Order.find({ userId: user.id });
}
```

**PASS 기준:**
```javascript
// 해결: Batch Loading 또는 JOIN
const orders = await Order.find({ userId: { $in: userIds } });
// 또는 DataLoader 사용
```

### Step 3: 인덱스 사용 검증

**검사:** WHERE, JOIN, ORDER BY 컬럼에 인덱스가 있는지 확인.

```bash
# 인덱스 정의 확인
grep -rn "createIndex\|@Index\|INDEX\|addIndex" src/ --include="*.ts" --include="*.java"
grep -rn "CREATE INDEX\|CREATE UNIQUE INDEX" migrations/ --include="*.sql"
```

**PASS 기준:**
```
✓ WHERE 절 컬럼: 인덱스 필수
✓ JOIN 컬럼: 인덱스 필수
✓ ORDER BY 컬럼: 인덱스 권장
✓ 복합 조건: 복합 인덱스 고려
```

### Step 4: 트랜잭션 처리 검증

**검사:** 여러 쿼리가 원자적으로 처리되는지 확인.

```bash
# 트랜잭션 패턴 검색
grep -rn "beginTransaction\|START TRANSACTION\|@Transactional" src/ --include="*.ts" --include="*.java"
grep -rn "commit\|rollback" src/ --include="*.ts" --include="*.js"
```

**PASS 기준:**
```javascript
// 올바른 트랜잭션
const transaction = await sequelize.transaction();
try {
  await User.create({...}, { transaction });
  await Order.create({...}, { transaction });
  await transaction.commit();
} catch (e) {
  await transaction.rollback();
  throw e;
}
```

### Step 5: 마이그레이션 안전성 검증

**검사:** 파괴적 변경에 롤백이 있는지 확인.

```bash
# 마이그레이션 파일 확인
ls -la migrations/
grep -rn "DROP\|DELETE\|TRUNCATE" migrations/ --include="*.sql" --include="*.ts"
grep -rn "down\s*(" migrations/ --include="*.ts" --include="*.js"
```

**PASS 기준:**
```
✓ 모든 마이그레이션에 down/rollback 존재
✓ DROP 컬럼 전에 데이터 백업 확인
✓ NOT NULL 제약 추가 전 기본값 설정
✓ 대용량 테이블 변경 시 배치 처리
```

## 결과 출력 형식

```markdown
## 데이터베이스 검증 결과

| 검사 항목 | 상태 | 발견 이슈 |
|-----------|------|-----------|
| SQL 인젝션 | PASS/FAIL | N개 |
| N+1 쿼리 | PASS/FAIL | N개 |
| 인덱스 | PASS/FAIL | N개 |
| 트랜잭션 | PASS/FAIL | N개 |
| 마이그레이션 | PASS/FAIL | N개 |

### 발견된 이슈

| 파일 | 라인 | 문제 | 심각도 |
|------|------|------|--------|
| `src/models/user.ts:45` | SQL 인젝션 위험 | CRITICAL |
| `src/services/order.ts:120` | N+1 쿼리 의심 | HIGH |
```

---

## 예외사항

1. **읽기 전용 쿼리** — 단순 조회는 트랜잭션 불필요
2. **NoSQL** — MongoDB 등은 SQL 인젝션 대신 다른 검증 필요
3. **배치 작업** — 대량 처리는 별도 트랜잭션 전략
4. **로깅용 쿼리** — 감사 로그는 트랜잭션에서 제외 가능
5. **캐시된 쿼리** — Redis 등 캐시 활용 시 DB 부하 감소

## Related Files

| File | Purpose |
|------|---------|
| `src/models/**/*.{ts,js}` | ORM 모델 파일 |
| `src/repositories/**/*.ts` | 리포지토리 레이어 |
| `migrations/**/*.{ts,sql}` | 마이그레이션 파일 |
| `src/db/**/*.ts` | DB 연결 및 쿼리 파일 |
