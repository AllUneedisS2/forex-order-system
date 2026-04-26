# forex-order-system

실시간 환율 기반 외환 주문 시스템

외부 환율 API 로부터 1분 주기로 환율을 수집하고, 사용자의 외화 매수·매도 주문을 처리하는 백엔드 서비스

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language / Build | Java 17, Gradle 8.x |
| Framework | Spring Boot 3.5 (MVC + WebClient + JPA + Batch) |
| DB | H2 (dev: in-memory / prod: file) |
| HTTP Client | Spring WebClient (Reactor Netty) |
| Test | JUnit 5, Mockito, WebTestClient, MockWebServer |
| CI | GitHub Actions |

---

## 빠른 시작

```bash
# 로컬 실행 (dev)
./gradlew bootRun

# 운영 빌드
./gradlew bootJar
SPRING_PROFILES_ACTIVE=prod java -jar build/libs/forex-order-system.jar

# 테스트
./gradlew test
```

---

## H2 접속

```bash
# dev
Driver Class: org.h2.Driver
JDBC URL: dbc:h2:mem:forexdb
User Name: sa

# prod
Driver Class: org.h2.Driver
JDBC URL: jdbc:h2:file:./data/forexdb
User Name: sa
```

---

## API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/exchange-rate/latest` | 4개 통화 환율 전체 조회 |
| GET | `/exchange-rate/latest/{currency}` | 단건 조회 (USD/JPY/CNY/EUR) |
| POST | `/order` | 외환 매수/매도 주문 |
| GET | `/order/list` | 주문 내역 조회 |

응답 포맷은 모두 `{ "code": "OK", "message": "SUCCESS", "returnObject": {...} }` 통일.

### 주문 요청 예시

```bash
curl -X POST http://localhost:8080/order \
  -H "Content-Type: application/json" \
  -d '{"fromCurrency":"KRW","toCurrency":"USD","forexAmount":200}'
```

---

## 핵심 비즈니스 규칙

- **스프레드**: 매수율 = 매매기준율 × 1.05, 매도율 × 0.95
- **JPY**: 저장은 1엔 기준, 응답은 100엔 단위로 환산
- **KRW 환산**: 항상 소수점 절사 (FLOOR), 환율은 소수 2자리 (HALF_UP)
- **주문 타입**: KRW→외화 = BUY, 외화→KRW = SELL, 그 외 BAD_REQUEST
- **외부 API 장애 시**: 마지막 성공 응답 캐시 → BOOTSTRAP_SEED 순으로 폴백

---

## 테스트 전략

테스트 피라미드 원칙에 따라 두 계층:

- **단위 테스트** (Mockito): Service 로직 격리 검증, `MockWebServer` 로 외부 API 시뮬레이션
- **통합 테스트** (`@SpringBootTest` + WebTestClient): HTTP ~ DB 까지 전체 흐름 검증

```bash
./gradlew test
open build/reports/tests/test/index.html
```

---

## 프로젝트 구조

```
src/main/java/com/switchwon/forexordersystem/
├── ForexOrderSystemApplication.java
├── common/        # Response, Exception, AOP, Currency enum
├── config/        # WebClientConfig
├── exchangerate/  # controller, service, repository, batch, dto, domain
└── order/         # controller, service, repository, dto, domain
```

---

## 환경 설정

| 프로파일 | DB | 로깅 |
|---------|------|------|
| `dev` | H2 In-memory | 콘솔 |
| `prod` | H2 File | 콘솔 + 파일 (롤링) |

주요 설정 (`application.yml`):

```yaml
app:
  exchange-api:
    base-url: https://open.er-api.com/v6
    fallback-to-mock: true
  scheduler:
    cron: "0 * * * * *"   # 매분 0초
```
