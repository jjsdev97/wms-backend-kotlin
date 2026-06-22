# K-WMS: Tri-Interface Backend

Kotlin-based Warehouse Management System with Multi-Protocol Support

Kotlin으로 구축한 창고 관리 시스템(WMS) 백엔드. 동일한 도메인 로직을 REST, GraphQL, MCP 세 가지 프로토콜로 노출하여 설계·구현·성능 차이를 비교하는 학습 프로젝트입니다.

> ⚠️ **학습용 프로젝트입니다.**
> 새로운 기술을 익히며 만드는 프로젝트라, 일부 기능·설계가 실무 기준에서 보면 과하거나(over-engineering),
> 반대로 단순화돼 있거나, 의도적으로 뒤틀려 있을 수 있습니다. 프로덕션 용도가 아닙니다.

---

## 1. 프로젝트 개요

- **목적**: 다중 프로토콜 기반 물류 데이터 백엔드 구현
- **도메인 범위**: 재고 조회·증감·예약·확정.

---

## 2. 기술 스택

| 구분 | 기술 | 선택 이유 |
| --- | --- | --- |
| Language | Kotlin 2.2 (JDK 21) | Null-safety, 코루틴, Spring 공식 지원 |
| Build | Gradle (Kotlin DSL) | 타입 안전한 빌드 스크립트 |
| Framework | Spring Boot 4.0 | Jakarta EE 11, Kotlin 지원, 스타터 생태계 |
| ORM | Spring Data JPA (Hibernate) | 표준 JPA + Kotlin `all-open`/`no-arg` 플러그인 |
| Database | PostgreSQL | Advisory Lock, MVCC, SERIALIZABLE, Atomic UPDATE |
| 인증 | Spring Security + JWT (HS256) | Bearer 토큰 기반 stateless 인증, BCrypt 해시 |
| API 문서 | springdoc-openapi | OpenAPI 3.0 + Swagger UI |
| MCP | Spring AI MCP Server | Spring/Tomcat에 통합되는 단일 런타임, `@Tool` 기반 노출 |
| 로컬 인프라 | Docker Compose | Postgres·Redis·Kafka 로컬 기동 |

### API Protocols

- **REST**: ERP·외부 시스템 연동, 범용 클라이언트 지원. 재고 쓰기는 JWT 인증 필요
- **GraphQL**: 선택적 필드 조회, DataLoader로 N+1 해결. **조회 전용**(Mutation 없음 — 쓰기는 REST로 일원화)
- **MCP (Model Context Protocol)**: 기업 내부 LLM이 창고 상태를 조회·제어하는 Tool·Resource 인터페이스 (개발 시 Claude Desktop·Cursor 등으로 검증). 내부망 가정으로 개방

---

## 3. 시스템 아키텍처

헥사고날 아키텍처(Hexagonal Architecture)를 따릅니다.

```text
[ Clients ]              [ Interface Layer ]            [ Domain Layer ]

  ERP/Web    ─────▶   REST API (MVC)           ────┐
  Web Client ─────▶   GraphQL (Spring for GQL) ────┼──▶   [ Inventory Service ]
  MCP Client ─────▶   MCP (Spring AI)          ────┘
```

- **디렉토리 구조 지도**: [docs/structure.md](docs/structure.md)
- **코드 흐름·원리(요청 추적·낙관적 락·멱등성)**: [docs/code-walkthrough.md](docs/code-walkthrough.md)

---

## 4. 주요 기능

- **Inventory Management**: 재고 조회·증감(`adjust`), 예약(`reserve`)·확정(`confirm`)·취소(`cancel`). `available = quantity - reserved`
- **Idempotency**: 두 층위로 멱등성 보장
  - **예약·확정·취소**: `Reservation` 엔티티의 `reservationId`(클라이언트 제공 자연키)로 **도메인 차원 멱등** — REST·GraphQL·MCP 어느 진입점에서 같은 키로 재호출해도 중복 차감이 없다(`confirm`/`cancel`은 예약 전체 대상, 이미 처리된 상태면 no-op)
  - **재고 증감(`adjust`)**: 자연키가 없는 순수 델타라, Redis 기반 `IdempotencyExecutor`로 요청 단위 멱등 처리(REST는 `Idempotency-Key` 헤더 `@Idempotent` AOP, MCP는 `requestId` toolParam, 키 생략 시 일반 처리)
- **Multi-Protocol Interface**: 동일 `InventoryService`를 REST, GraphQL, MCP로 노출
- **MCP Tool**: `getInventory`, `adjustStock`, `reserveStock`, `confirmStock`, `cancelReservation` (Spring AI `@Tool`)
- **Authentication**: Spring Security + JWT(HS256). 회원가입·로그인으로 Bearer 토큰을 발급받아, REST 재고 쓰기 요청에 사용. 비밀번호는 BCrypt 해시 저장
- **Swagger UI**: REST API 문서화·테스트

### 프로토콜별 보안 정책

멀티프로토콜이라 진입점마다 보안 모델이 다르다. `SecurityConfig`에 동일 정책이 주석으로 명시돼 있다.

| 프로토콜 | 조회(읽기) | 변경(쓰기) | 근거 |
| --- | --- | --- | --- |
| **REST** | 공개 | **JWT 인증 필요** | 재고 쓰기를 인증된 주체로 제한 (이번 보안 작업의 핵심 목표) |
| **GraphQL** | 공개 | — (Mutation 없음) | 조회 전용이라 쓰기 구멍 자체가 없음 |
| **MCP** | 공개 | 공개 | 내부 LLM·내부망 가정. 전송 인증/행위 가드는 JWT와 다른 모델이라 후속 단계 |

---

## 5. API 명세

| Protocol | Endpoint |
| --- | --- |
| REST (인증) | `POST /api/v1/auth/signup`, `POST /api/v1/auth/login` |
| REST (재고) | `GET /api/v1/inventory/**` (공개), `POST /api/v1/inventory/{id}/{adjust\|reserve\|confirm\|cancel}` (JWT 필요) |
| GraphQL | `/graphql` (Query: `inventories`, `inventory`) |
| MCP | `POST /mcp` (`getInventory`, `adjustStock`, `reserveStock`, `confirmStock`, `cancelReservation`) |

- **UI**: 프론트엔드는 제공하지 않고 Swagger UI로 테스트.

---

## 6. 로컬 실행 & 테스트

```bash
# 1. 인프라 기동 (PostgreSQL, Redis)
docker compose up -d postgres redis

# 2. JWT secret 준비 (소스에 두지 않음 — 둘 중 하나)
#    a) 로컬 프로필 파일: application-local.yml.example 를 복사 후 secret 값 채움
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
#    b) 또는 환경변수로 직접 주입: export APP_JWT_SECRET=<32바이트 이상 임의 문자열>

# 3. 애플리케이션 실행 (기본 포트 8081, local 프로필로 a) 파일 사용)
./gradlew bootRun --args='--spring.profiles.active=local'
```

> **비밀 관리**
>
> `app.jwt.secret`은 소스/이미지에 두지 않습니다. 환경별로 주입 통로(`APP_JWT_SECRET`)는 같고 값의 출처만 다릅니다.
>
> | 환경 | 값의 출처 |
> | --- | --- |
> | 로컬 bootRun | `application-local.yml` (`.gitignore`, profile `local`) |
> | 로컬 compose | `.env` (`docker compose -f docker-compose.yml -f docker-compose.app.yml up`) |
> | 운영 | Secret Manager / Vault / k8s Secret → 같은 env로 주입 |

> **인증 흐름**
>
> 재고 쓰기(`adjust`/`reserve`/`confirm`/`cancel`)는 Bearer 토큰이 필요합니다. 조회(GET)는 공개입니다.
>
> ```bash
> # 1) 회원가입
> curl -X POST http://localhost:8081/api/v1/auth/signup \
>   -H "Content-Type: application/json" \
>   -d '{"username": "alice", "password": "password123"}'
>
> # 2) 로그인 → accessToken 발급
> TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
>   -H "Content-Type: application/json" \
>   -d '{"username": "alice", "password": "password123"}' | jq -r .accessToken)
> ```

> **멱등 요청**
>
> 예약/확정/취소는 `reservationId`(자연키)로 멱등합니다. 같은 키로 다시 호출해도 한 번만 반영됩니다.
>
> ```bash
> # 예약 — 쓰기이므로 Bearer 토큰 필요. 같은 reservationId 재호출해도 중복 예약되지 않음
> curl -X POST http://localhost:8081/api/v1/inventory/1/reserve \
>   -H "Authorization: Bearer $TOKEN" \
>   -H "Content-Type: application/json" \
>   -d '{"reservationId": "order-1001", "amount": 5}'
>
> # 확정/취소 — 예약 전체 대상, reservationId만 전달
> curl -X POST http://localhost:8081/api/v1/inventory/1/confirm \
>   -H "Authorization: Bearer $TOKEN" \
>   -H "Content-Type: application/json" \
>   -d '{"reservationId": "order-1001"}'
> ```
>
> 재고 증감(`adjust`)은 자연키가 없어, 선택적으로 `Idempotency-Key: <고유값>` 헤더를 보내면 같은 키의 재요청이 최초 응답을 그대로 돌려받습니다(헤더 생략 시 매번 처리).

기동 후, 세 프로토콜을 아래에서 바로 테스트할 수 있습니다. 셋 다 동일한 `InventoryService`(재고 조회·조정)를 호출합니다.

| 프로토콜 | 테스트 방법 |
| --- | --- |
| **REST** | Swagger UI — http://localhost:8081/swagger-ui.html (쓰기는 우측 상단 **Authorize**에 로그인으로 받은 `accessToken` 입력) |
| **GraphQL** | GraphiQL — http://localhost:8081/graphiql (조회 전용) |
| **MCP** | `POST http://localhost:8081/mcp` (STREAMABLE). [MCP Inspector](https://github.com/modelcontextprotocol/inspector)에서 Transport `Streamable HTTP` + 위 URL로 연결하거나, 로컬 Claude(Claude Code/Desktop)에 등록해 사용 |

> seed 데이터(`id` 1~3)가 포함돼 있어 기동 직후 바로 조회·조정을 호출할 수 있습니다.

---

## 7. 로드맵

단계별 기술 도입 순서.

### Phase 1 — Foundation

| 영역 | 기술 | 역할 |
| --- | --- | --- |
| 인메모리 스토어 | Redis | 재고 카운터(원자 차감), 분산 락, 세션, Idempotency Key 저장 |
| 마이그레이션 | Flyway | 스키마 버전 관리 |
| 멱등성 | Idempotency Key (Redis) | 중복 요청 차단 |
| 테스트 | Testcontainers | Postgres·Redis 통합 테스트 (Phase 3부터 Kafka 추가) |

### Phase 2 — Security & Observability

| 영역 | 기술 | 역할 | 상태 |
| --- | --- | --- | --- |
| 인증·인가 | Spring Security + JWT | 토큰 기반 인증 (REST 쓰기 보호, BCrypt) | ✅ 완료 |
| 관측성 | Prometheus | Actuator 기반 메트릭 수집 (`/actuator/prometheus`) | 예정 |
| 부하 테스트 | k6 / Gatling | 목표: 500 RPS @ p99 < 200ms. Redis `DECRBY` + DB Atomic UPDATE 조합으로 재고 차감 1,000 req/s 무결성 | 예정 |

### Phase 3 — Event-Driven & Batch

| 영역 | 기술 | 역할 |
| --- | --- | --- |
| 이벤트 스트리밍 | Kafka | 재고 변동 이벤트, 작업 지시 |
| 이벤트 원자성 | Outbox Pattern | DB 트랜잭션과 이벤트 발행의 원자성 보장 |
| 배치 | Spring Batch | 대량 입출고 정산, 재고 실사 |

---

### 우선순위 요약

| 우선순위 | 목표 | 대표 기술 |
| :---: | --- | --- |
| P1 | 기본 동작·신뢰성 | Redis, Flyway, Idempotency, Testcontainers |
| P2 | 보안·관측성 | Spring Security(JWT), Prometheus, k6 |
| P3 | 비동기·대용량 | Kafka, Outbox Pattern, Spring Batch |
