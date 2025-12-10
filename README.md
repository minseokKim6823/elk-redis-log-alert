# ELK + Redis 로그 기반 알림 시스템

## Phase 1 구현 완료

실시간으로 애플리케이션 로그를 수집하고, ERROR 로그 발생 시 자동으로 알림을 처리하는 시스템입니다.

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

### 3. Swagger UI 접속

브라우저에서 http://localhost:8080/swagger-ui.html 접속하여 API 문서를 확인하고 직접 테스트할 수 있습니다.

### 4. 동작 확인

#### 4-1. ERROR 로그 생성
```bash
# 단일 ERROR 로그 생성
curl -X POST "http://localhost:8080/api/logs/test/ERROR?message=Database connection failed"

# 예외 포함 ERROR 로그 생성
curl -X POST "http://localhost:8080/api/logs/error?message=Payment processing error"

# 대량 ERROR 로그 생성 (10개)
curl -X POST "http://localhost:8080/api/logs/bulk?level=ERROR&count=10"
```

#### 4-2. 다른 레벨 로그 생성
```bash
# INFO 로그 (Redis 큐에 추가되지 않음)
curl -X POST "http://localhost:8080/api/logs/test/INFO?message=User logged in"

# WARN 로그
curl -X POST "http://localhost:8080/api/logs/test/WARN?message=High memory usage detected"
```

#### 4-3. 알림 확인
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

## 주요 파일 설명

### 설정 파일
- `docker-compose.yml`: ELK + Redis 컨테이너 구성
- `docker/logstash/pipeline/logstash.conf`: Logstash 파이프라인 설정
- `src/main/resources/application.yml`: Spring Boot 설정
- `src/main/resources/logback-spring.xml`: Logback 설정

### Java 클래스
- `LogController.java`: 로그 생성 테스트 API
- `AlertQueueService.java`: Redis 큐 CRUD 작업
- `AlertWorker.java`: 백그라운드 큐 모니터링 및 알림 처리
- `Alert.java`: 알림 데이터 모델
- `RedisConfig.java`: Redis 연결 설정

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