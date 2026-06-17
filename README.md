# K-WMS: Tri-Interface Backend

Kotlin-based Warehouse Management System with Multi-Protocol Support

Kotlin으로 구축한 창고 관리 시스템(WMS) 백엔드. 동일한 도메인 로직을 REST, GraphQL, MCP 세 가지 프로토콜로 노출하여 설계·구현·성능 차이를 비교하는 학습 프로젝트입니다.

---

## 1. 프로젝트 개요

- **목적**: 다중 프로토콜 기반 물류 데이터 백엔드 구현
- **도메인 범위**: 재고 조회·증감·예약·확정.

---

## 2. 기술 스택

| 구분 | 기술 | 선택 이유 |
| --- | --- | --- |
| Language | Kotlin (JDK 17+) | Null-safety, 코루틴, Spring 공식 지원 |
| Build | Gradle (Kotlin DSL) | 타입 안전한 빌드 스크립트 |
| Framework | Spring Boot 3.x | Jakarta EE 9+, Kotlin 지원, 스타터 생태계 |
| ORM | Spring Data JPA (Hibernate) | 표준 JPA + Kotlin `all-open`/`no-arg` 플러그인 |
| Database | PostgreSQL | Advisory Lock, MVCC, SERIALIZABLE, Atomic UPDATE |
| API 문서 | springdoc-openapi | OpenAPI 3.0 + Swagger UI |
| MCP | Spring AI MCP Server | Spring/Tomcat에 통합되는 단일 런타임, `@Tool` 기반 노출 |
| 로컬 인프라 | Docker Compose | Postgres·Redis·Kafka 로컬 기동 |

### API Protocols

- **REST**: ERP·외부 시스템 연동, 범용 클라이언트 지원
- **GraphQL**: 선택적 필드 조회, DataLoader로 N+1 해결
- **MCP (Model Context Protocol)**: 기업 내부 LLM이 창고 상태를 조회·제어하는 Tool·Resource 인터페이스 (개발 시 Claude Desktop·Cursor 등으로 검증)

---

## 3. 시스템 아키텍처

헥사고날 아키텍처(Hexagonal Architecture)를 따릅니다.

```text
[ Clients ]              [ Interface Layer ]            [ Domain Layer ]

  ERP/Web    ─────▶   REST API (MVC)           ────┐
  Web Client ─────▶   GraphQL (Spring for GQL) ────┼──▶   [ Inventory Service ]
  MCP Client ─────▶   MCP (Spring AI)          ────┘
```

---

## 4. 주요 기능

- **Inventory Management**: 재고 조회·증감(`adjust`), 예약(`reserve`)·확정(`confirm`)·취소(`cancel`). `available = quantity - reserved`
- **Multi-Protocol Interface**: 동일 `InventoryService`를 REST, GraphQL, MCP로 노출
- **MCP Tool**: `getInventory`, `adjustStock`, `reserveStock`, `confirmStock`, `cancelReservation` (Spring AI `@Tool`)
- **Swagger UI**: REST API 문서화·테스트

---

## 5. API 명세

| Protocol | Endpoint |
| --- | --- |
| REST | `/api/v1/inventory/**` |
| GraphQL | `/graphql` |
| MCP | `POST /mcp` (`getInventory`, `adjustStock`) |

- **UI**: 프론트엔드는 제공하지 않고 Swagger UI로 테스트.

---

## 6. 로컬 실행 & 테스트

```bash
# 1. 인프라 기동 (PostgreSQL)
docker compose up -d postgres

# 2. 애플리케이션 실행 (기본 포트 8081)
./gradlew bootRun
```

기동 후, 세 프로토콜을 아래에서 바로 테스트할 수 있습니다. 셋 다 동일한 `InventoryService`(재고 조회·조정)를 호출합니다.

| 프로토콜 | 테스트 방법 |
| --- | --- |
| **REST** | Swagger UI — http://localhost:8081/swagger-ui.html |
| **GraphQL** | GraphiQL — http://localhost:8081/graphiql |
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

| 영역 | 기술 | 역할 |
| --- | --- | --- |
| 인증·인가 | Spring Security + JWT | 토큰 기반 인증 |
| 관측성 | Prometheus | Actuator 기반 메트릭 수집 (`/actuator/prometheus`) |
| 부하 테스트 | k6 / Gatling | 목표: 500 RPS @ p99 < 200ms. Redis `DECRBY` + DB Atomic UPDATE 조합으로 재고 차감 1,000 req/s 무결성 |

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
