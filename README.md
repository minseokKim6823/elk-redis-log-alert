# ELK + Redis 로그 기반 알림 시스템

## Phase 1 구현 완료

실시간으로 애플리케이션 로그를 수집하고, ERROR 로그 발생 시 자동으로 알림을 처리하는 시스템입니다.

## 퀵 스타트 (5분 안에 시작하기)

```bash
# 1. 인프라 실행
docker-compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun

# 3. 테스트 (새 터미널에서)
curl -X POST "http://localhost:8080/api/logs/test/ERROR?message=Test Error"

# 4. 웹 UI 접속
# Swagger API: http://localhost:8080/swagger-ui.html
# Kibana: http://localhost:5601
# Grafana: http://localhost:3000 (admin/admin)
```

## 시스템 구조

```
Spring Boot App (로그 생성)
        ↓
    Logback (로그 수집)
        ↓
    Logstash (로그 파싱 & 전송)
        ↓
    ├─→ Elasticsearch (로그 저장)
    └─→ Redis Queue (ERROR 로그만)
            ↓
        AlertWorker (1초마다 polling)
            ↓
        콘솔에 알림 출력
```

## 주요 구성 요소

### 1. 인프라 (Docker Compose)
- **Elasticsearch**: 로그 저장 및 검색 엔진
- **Logstash**: 로그 수집 및 파이프라인 처리
- **Kibana**: 로그 시각화 대시보드 (http://localhost:5601)
- **Redis**: 알림 큐

### 2. Spring Boot 애플리케이션
- **LogController**: 테스트용 로그 생성 API
- **AlertQueueService**: Redis 큐 관리
- **AlertWorker**: 1초마다 큐를 확인하고 알림 처리
- **RedisConfig**: Redis 연결 설정

## 실행 방법

### 1. ELK + Redis 인프라 실행

```bash
# Docker Compose 실행
docker-compose up -d

# 컨테이너 상태 확인
docker-compose ps

# 로그 확인 (문제 발생 시)
docker-compose logs -f logstash
```

### 2. Spring Boot 애플리케이션 실행

```bash
# Gradle 빌드 및 실행
./gradlew bootRun

# 또는 IDE에서 ElkApplication 실행
```

### 3. 환경 변수 설정 (선택사항)

Grafana 이메일 알림을 사용하려면 `.env` 파일을 수정하세요:
```bash
# .env 파일 생성 (이미 있다면 수정)
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=your-password
GF_SMTP_ENABLED=true
GF_SMTP_HOST=smtp.gmail.com:587
GF_SMTP_USER=your-email@gmail.com
GF_SMTP_PASSWORD=your-app-password
```

### 4. Swagger UI 접속

브라우저에서 http://localhost:8080/swagger-ui.html 접속하여 API 문서를 확인하고 직접 테스트할 수 있습니다.

### 5. 동작 확인

#### 5-1. ERROR 로그 생성
```bash
# 단일 ERROR 로그 생성
curl -X POST "http://localhost:8080/api/logs/test/ERROR?message=Database connection failed"

# 예외 포함 ERROR 로그 생성
curl -X POST "http://localhost:8080/api/logs/error?message=Payment processing error"

# 대량 ERROR 로그 생성 (10개)
curl -X POST "http://localhost:8080/api/logs/bulk?level=ERROR&count=10"
```

#### 5-2. 다른 레벨 로그 생성
```bash
# INFO 로그 (Redis 큐에 추가되지 않음)
curl -X POST "http://localhost:8080/api/logs/test/INFO?message=User logged in"

# WARN 로그
curl -X POST "http://localhost:8080/api/logs/test/WARN?message=High memory usage detected"
```

#### 5-3. 알림 확인
Spring Boot 콘솔에 다음과 같이 출력됩니다:
```
========================================
🚨 ALERT DETECTED!
Level: ERROR
Message: Database connection failed
Logger: com.example.elk.controller.LogController
Timestamp: 2025-12-10T16:30:45
Thread: http-nio-8080-exec-1
========================================
```

## 데이터 흐름

1. **로그 생성**: `LogController`가 다양한 레벨의 로그 생성
2. **Logback 수집**: `logback-spring.xml` 설정에 따라 Logstash로 전송
3. **Logstash 처리**:
   - 모든 로그 → Elasticsearch 저장
   - ERROR 로그만 → Redis 리스트에 추가 (`alert:queue`)
4. **AlertWorker 처리**: 1초마다 Redis 큐를 확인하고 알림 출력
5. **큐 모니터링**: 10초마다 큐 크기를 로그로 출력

## 프로젝트 구조

```
ELK/
├── src/
│   ├── main/
│   │   ├── java/com/example/elk/
│   │   │   ├── ElkApplication.java              # Spring Boot 메인 애플리케이션
│   │   │   ├── config/
│   │   │   │   ├── RedisConfig.java             # Redis 연결 설정
│   │   │   │   └── SwaggerConfig.java           # Swagger API 문서 설정
│   │   │   ├── controller/
│   │   │   │   ├── LogController.java           # 로그 생성 테스트 API
│   │   │   │   └── AlertMonitorController.java  # 알림 모니터링 API
│   │   │   ├── domain/
│   │   │   │   └── Alert.java                   # 알림 데이터 모델 (JSON 직렬화)
│   │   │   ├── service/
│   │   │   │   └── AlertQueueService.java       # Redis 큐 관리 서비스
│   │   │   └── worker/
│   │   │       └── AlertWorker.java             # 1초마다 큐 모니터링 및 알림 처리
│   │   └── resources/
│   │       ├── application.yml                  # Spring Boot 설정 (Redis, 서버 등)
│   │       └── logback-spring.xml               # Logback 로그 설정 (Logstash 전송)
│   └── test/
│       └── java/com/example/elk/
│           └── ElkApplicationTests.java
├── docker/
│   ├── logstash/
│   │   └── pipeline/
│   │       └── logstash.conf                    # Logstash 파이프라인 (ES + Redis 라우팅)
│   ├── prometheus/
│   │   └── prometheus.yml                       # Prometheus 스크래핑 설정
│   ├── loki/
│   │   └── config.yml                           # Loki 로그 수집 설정
│   └── promtail/
│       └── config.yml                           # Promtail 로그 전송 설정
├── logs/                                        # 애플리케이션 로그 파일 (Promtail이 읽음)
├── docker-compose.yml                           # 전체 인프라 구성 (ELK, Redis, Grafana 스택)
├── build.gradle                                 # Gradle 빌드 설정 및 의존성
├── .env                                         # 환경 변수 (Grafana 인증, SMTP 설정)
└── README.md
```

## 주요 파일 상세 설명

### 1. 애플리케이션 코드

#### `ElkApplication.java`
- Spring Boot 메인 애플리케이션
- `@EnableScheduling` 어노테이션으로 AlertWorker의 스케줄링 활성화

#### `config/RedisConfig.java`
- Redis 연결 설정 (`localhost:6379`)
- `RedisTemplate<String, Object>` 빈 생성
- JSON 직렬화를 위한 `GenericJackson2JsonRedisSerializer` 사용

#### `config/SwaggerConfig.java`
- SpringDoc OpenAPI 설정
- API 문서 자동 생성 (http://localhost:8080/swagger-ui.html)
- API 정보: 제목, 설명, 버전

#### `controller/LogController.java`
테스트용 로그 생성 API 제공:
- `POST /api/logs/test/{level}` - 로그 레벨별 로그 생성 (DEBUG/INFO/WARN/ERROR)
- `POST /api/logs/error` - 예외 포함 ERROR 로그 생성 (스택 트레이스 포함)
- `POST /api/logs/bulk` - 대량 로그 생성 (부하 테스트용)
- `POST /api/logs/pattern` - 패턴 기반 로그 생성 (알림 규칙 테스트용)

#### `controller/AlertMonitorController.java`
- `GET /api/alerts/monitor` - 현재 Redis 큐 상태 조회
- 큐에 쌓인 알림 개수 및 내용 확인

#### `domain/Alert.java`
알림 데이터 모델 (Lombok 사용):
- `level`: 로그 레벨 (ERROR 등)
- `message`: 로그 메시지
- `logger`: 로거 이름 (클래스명)
- `timestamp`: 로그 발생 시각
- `thread`: 스레드 이름
- `stackTrace`: 예외 스택 트레이스 (선택적)

#### `service/AlertQueueService.java`
Redis 큐 관리 서비스:
- `addAlert(Alert)`: 알림을 Redis 리스트에 추가 (`LPUSH alert:queue`)
- `pollAlert()`: 큐에서 알림 꺼내기 (`RPOP alert:queue`)
- `getQueueSize()`: 큐 크기 조회 (`LLEN alert:queue`)
- `getAlerts(count)`: 큐의 알림 목록 조회 (삭제하지 않고)

#### `worker/AlertWorker.java`
백그라운드 알림 처리 워커:
- `@Scheduled(fixedRate = 1000)`: 1초마다 큐 확인
- `processAlerts()`: Redis 큐에서 알림을 꺼내 콘솔에 출력
- `monitorQueue()`: 10초마다 큐 크기 로그 출력

### 2. 설정 파일

#### `application.yml`
Spring Boot 애플리케이션 설정:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  application:
    name: elk-alert-system

app:
  alert:
    worker:
      enabled: true  # AlertWorker 활성화/비활성화

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # Actuator 엔드포인트
```

#### `logback-spring.xml`
Logback 로그 설정:
- **ConsoleAppender**: 콘솔 출력 (개발용)
- **RollingFileAppender**: 파일 로그 저장 (`logs/app.log`, 일별 롤링)
- **LogstashTcpSocketAppender**: Logstash로 TCP 전송 (localhost:5000)
  - JSON 형식으로 로그 전송
  - 로그 레벨, 메시지, 타임스탬프, 스레드, 로거 등 포함

#### `docker-compose.yml`
전체 인프라 구성:
- **Elasticsearch** (9200): 로그 저장소
- **Logstash** (5000): 로그 수집 및 파이프라인 처리
- **Kibana** (5601): 로그 시각화 대시보드
- **Redis** (6379): 알림 큐
- **Prometheus** (9090): 메트릭 수집 (Spring Boot `/actuator/prometheus`)
- **Loki** (3100): 로그 수집 시스템
- **Promtail**: 로컬 로그 파일(`./logs`)을 Loki로 전송
- **Grafana** (3000): 통합 모니터링 대시보드 (Prometheus + Loki)

환경 변수는 `.env` 파일에서 로드:
- Grafana 관리자 계정 (`GF_SECURITY_ADMIN_USER`, `GF_SECURITY_ADMIN_PASSWORD`)
- SMTP 이메일 알림 설정 (`GF_SMTP_*`)

#### `docker/logstash/pipeline/logstash.conf`
Logstash 파이프라인 설정:

**Input**: TCP 5000번 포트에서 JSON 로그 수신
```
input {
  tcp {
    port => 5000
    codec => json
  }
}
```

**Filter**: 타임스탬프 파싱 및 필드 처리

**Output**:
1. **모든 로그** → Elasticsearch 인덱스 `app-logs-YYYY.MM.dd`
2. **ERROR 로그만** → Redis 리스트 `alert:queue`
```
output {
  elasticsearch { ... }

  if [level] == "ERROR" {
    redis {
      host => "redis"
      data_type => "list"
      key => "alert:queue"
    }
  }
}
```

#### `docker/prometheus/prometheus.yml`
Prometheus 스크래핑 설정:
- Spring Boot Actuator 메트릭 수집
- 타겟: `host.docker.internal:8080/actuator/prometheus`
- 스크래핑 간격: 15초

#### `docker/loki/config.yml`
Loki 로그 수집 설정:
- 로그 저장 경로: `/loki`
- 인제스트 API 포트: 3100

#### `docker/promtail/config.yml`
Promtail 로그 전송 설정:
- `/var/log/app/*.log` 파일 모니터링
- 레이블: `{job="spring-app", app="elk-alert-system"}`
- Loki로 전송: `http://loki:3100`

### 3. 빌드 파일

#### `build.gradle`
Gradle 빌드 설정 및 주요 의존성:
- **Spring Boot 4.0.0** (Java 17)
- **Spring Data JPA** (H2 데이터베이스)
- **Spring Data Redis** (Redis 큐 관리)
- **Spring Boot Actuator** (메트릭 노출)
- **Micrometer Prometheus** (Prometheus 메트릭)
- **Logstash Logback Encoder 7.4** (Logstash로 로그 전송)
- **SpringDoc OpenAPI 2.3.0** (Swagger UI)
- **Lombok** (보일러플레이트 코드 제거)

### 4. 환경 변수 파일

#### `.env`
Grafana 및 이메일 알림 설정 (docker-compose.yml에서 참조):
```env
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=admin
GF_SMTP_ENABLED=true
GF_SMTP_HOST=smtp.gmail.com:587
GF_SMTP_USER=your-email@gmail.com
GF_SMTP_PASSWORD=your-app-password
GF_SMTP_FROM_ADDRESS=your-email@gmail.com
GF_SMTP_FROM_NAME=Grafana Alert
GF_SMTP_SKIP_VERIFY=true
GF_SMTP_STARTTLS_POLICY=MandatoryStartTLS
```

**주의**: 실제 SMTP 자격 증명은 `.gitignore`에 추가하여 커밋하지 않도록 주의하세요.

## Kibana에서 로그 확인

1. 브라우저에서 http://localhost:5601 접속
2. 좌측 메뉴 → Analytics → Discover
3. Index pattern 생성: `app-logs-*`
4. 로그 검색 및 필터링 가능

### 유용한 Kibana 쿼리
```
# ERROR 로그만 보기
level: "ERROR"

# 특정 메시지 검색
message: "Database"

# 시간 범위 필터링
@timestamp: [now-1h TO now]
```

## Redis 큐 확인

```bash
# Redis CLI 접속
docker exec -it redis redis-cli

# 큐 크기 확인
LLEN alert:queue

# 큐 내용 확인 (처음 10개)
LRANGE alert:queue 0 9

# 모든 키 확인
KEYS *
```

## Elasticsearch 직접 조회

```bash
# 인덱스 목록 확인
curl http://localhost:9200/_cat/indices?v

# 최근 로그 조회
curl -X GET "http://localhost:9200/app-logs-*/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match": {
      "level": "ERROR"
    }
  },
  "size": 10,
  "sort": [
    { "@timestamp": "desc" }
  ]
}
'
```

## 트러블슈팅

### 1. Logstash가 Elasticsearch에 연결하지 못함
```bash
# Elasticsearch 상태 확인
curl http://localhost:9200

# Logstash 로그 확인
docker-compose logs logstash
```

### 2. 로그가 Redis 큐에 들어가지 않음
- Logstash 파이프라인 설정 확인: `docker/logstash/pipeline/logstash.conf`
- ERROR 레벨 로그만 큐에 추가됨 (INFO, WARN은 Elasticsearch만)

### 3. AlertWorker가 큐를 polling하지 않음
- `application.yml`에서 `app.alert.worker.enabled: true` 확인
- Spring Boot 콘솔에서 스케줄러 동작 로그 확인

## 다음 단계 (Phase 2)

- [ ] 알림 규칙 관리 API (임계치 설정)
- [ ] 중복 알림 방지 (5분 내 동일 알림 제거)
- [ ] 알림 이력 DB 저장
- [ ] Slack/Email 연동
- [ ] Kibana 대시보드 구성

## 포트 정보

| 서비스 | 포트 | 용도 |
|--------|------|------|
| Spring Boot | 8080 | REST API |
| Swagger UI | 8080/swagger-ui.html | API 문서 |
| Elasticsearch | 9200 | HTTP API |
| Elasticsearch | 9300 | 클러스터 통신 |
| Logstash | 5000 | 로그 수신 (TCP) |
| Logstash | 9600 | 모니터링 API |
| Kibana | 5601 | 웹 UI |
| Redis | 6379 | 데이터베이스 |

## API 문서 (Swagger)

Spring Boot 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### 제공되는 API

1. **POST /api/logs/test/{level}** - 로그 생성
   - 파라미터: level (DEBUG/INFO/WARN/ERROR), message
   - 예시: `/api/logs/test/ERROR?message=Database error`

2. **POST /api/logs/error** - 예외 포함 ERROR 로그 생성
   - 파라미터: message
   - 스택 트레이스가 포함된 에러 로그 생성

3. **POST /api/logs/bulk** - 대량 로그 생성
   - 파라미터: level, count
   - 부하 테스트 및 알림 임계치 테스트용

4. **POST /api/logs/pattern** - 패턴 기반 로그 생성
   - 파라미터: pattern
   - 알림 규칙 테스트용
## Prometheus / Grafana / Loki 모니터링 스택

### 접속 정보 및 설정

#### Prometheus
- **URL**: http://localhost:9090
- **역할**: Spring Boot Actuator의 `/actuator/prometheus` 엔드포인트에서 메트릭 수집
- **Grafana 연동**: 데이터 소스 URL은 `http://prometheus:9090` (Docker 네트워크 내부)
- **스크래핑 간격**: 15초마다 메트릭 수집

#### Grafana
- **URL**: http://localhost:3000
- **기본 계정**: `admin/admin` (`.env` 파일에서 변경 가능)
- **데이터 소스**: Prometheus와 Loki를 데이터 소스로 추가하여 대시보드/로그 패널 구성
- **알림 기능**: SMTP 설정을 통한 이메일 알림 (`.env`에서 설정)

#### Loki
- **URL**: http://localhost:3100
- **Grafana 연동**: Grafana에서 데이터 소스 URL을 `http://loki:3100`으로 설정
- **로그 쿼리**: 기본 쿼리 예시 `{job="spring-app", app="elk-alert-system"}`
- **저장 경로**: Docker 볼륨 `loki-data:/loki`

#### Promtail
- **역할**: 로컬 로그 파일을 Loki로 전송
- **모니터링 경로**: 호스트의 `./logs` 디렉토리가 컨테이너 `/var/log/app`으로 마운트됨
- **수집 대상**: `logback-spring.xml`의 RollingFileAppender가 생성하는 로그 파일 (`logs/app.log`)
- **전송 대상**: Loki (`http://loki:3100`)

#### Spring Boot Actuator
- **메트릭 엔드포인트**: http://localhost:8080/actuator/prometheus
- **제공 메트릭**: JVM, HTTP 요청, 커스텀 메트릭 등
- **모니터링**: Prometheus가 자동으로 스크래핑

### Grafana 대시보드 구성 예시

1. **Prometheus 데이터 소스 추가**:
   - Configuration → Data Sources → Add data source
   - URL: `http://prometheus:9090`

2. **Loki 데이터 소스 추가**:
   - Configuration → Data Sources → Add data source
   - URL: `http://loki:3100`

3. **대시보드 패널 추가**:
   - JVM 메모리 사용량 (Prometheus)
   - HTTP 요청 수 (Prometheus)
   - 실시간 로그 스트림 (Loki)
   - ERROR 로그 카운트 (Loki)
