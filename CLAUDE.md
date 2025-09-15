# Backend - Spring Boot 로또 추천 시스템

## 기술 스택 상세
- **Spring Boot**: 3.4.2
- **Java**: 21 (LTS)
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **빌드**: Gradle 8.x
- **의존성 관리**: Spring Dependency Management Plugin 1.1.7

## 주요 의존성
```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'com.amazonaws:aws-java-sdk-lambda:1.12.300'
compileOnly 'org.projectlombok:lombok'
runtimeOnly 'org.postgresql:postgresql'
```

## 패키지 구조

```
com.lotto.backend/
├── controller/
│   ├── MainController           # 기본 API
│   ├── LottoController          # 로또 추천 메인 컨트롤러
│   └── LottoStatisticsController # 통계 API
├── service/
│   ├── LottoRecommendService    # 기본 추천 로직
│   ├── EnhancedLottoRecommendService # 향상된 추천 로직
│   ├── LottoStatisticsService   # 통계 분석
│   ├── LottoResultService       # 당첨 결과 관리
│   └── LottoAnalysisService     # 분석 서비스
├── repository/
│   ├── LottoResultRepository    # 당첨 결과 JPA
│   └── LottoRecommendRepository # 추천 번호 JPA
├── model/
│   ├── entity/
│   │   ├── LottoResult          # 당첨 결과 엔티티
│   │   └── LottoRecommend       # 추천 번호 엔티티
│   ├── dto/
│   │   ├── LottoRecommendRequest
│   │   ├── EnhancedLottoRequest
│   │   ├── LottoRecommendationResponse
│   │   ├── NumberFrequencyDto
│   │   └── TrendAnalysisDto
│   └── enums/
│       └── RecommendStrategy     # 추천 전략 enum
└── filter/                      # 필터링 시스템
    ├── LottoFilter              # 필터 인터페이스
    ├── LottoFilterChainManager  # 필터 체인 관리
    ├── LottoContext             # 필터 실행 컨텍스트
    ├── FilterResult             # 필터 결과
    ├── FilterChainResult        # 체인 결과
    └── impl/                    # 필터 구현체
        ├── ACValueFilter        # AC 값 필터
        ├── DeltaSystemFilter    # 델타 시스템 필터
        ├── PrimeBalanceFilter   # 소수 균형 필터
        ├── PositionAnalysisFilter # 위치 분석 필터
        ├── SumDistributionFilter  # 합계 분포 필터
        ├── PatternExclusionFilter # 패턴 제외 필터
        └── RecentHistoryFilter    # 최근 이력 필터
```

## 핵심 비즈니스 로직

### 번호 추천 프로세스
1. **LottoAnalysisService**: 과거 당첨 데이터 분석 및 패턴 추출
2. **EnhancedLottoRecommendService**: 필터 체인을 통한 번호 생성
3. **LottoFilterChainManager**: 필터 우선순위에 따른 검증 체인 실행
4. **LottoStatisticsService**: 통계적 분석을 통한 기대값 계산

### 필터링 시스템
확률적으로 낮은 조합을 제외하는 7가지 고급 필터:

1. **AC Value Filter**: 인접 숫자 차이 분석 (7-10 범위)
   - 당첨 확률이 높은 AC 값 범위 내 조합만 통과

2. **Delta System Filter**: 번호 간격 패턴 분석
   - 번호 간 간격이 균형 잡힌 조합 선호

3. **Prime Balance Filter**: 소수 개수 균형 (2-3개)
   - 통계적으로 최적인 소수 개수 유지

4. **Position Analysis Filter**: 번호 위치 분포 분석
   - 각 자리수별 균형 잡힌 분포 검증

5. **Sum Distribution Filter**: 합계 정규분포 (평균 138, 표준편차 30)
   - 당첨번호 합계의 정규분포 특성 활용

6. **Pattern Exclusion Filter**: 연속/등차 패턴 제외
   - 확률적으로 낮은 연속 번호나 등차수열 제거

7. **Recent History Filter**: 최근 5회 당첨번호 제외
   - 단기간 재출현 확률이 낮은 번호 필터링

### 추천 전략 (RecommendStrategy)
- 사용자가 선택 가능한 다양한 전략 제공
- 각 전략별로 다른 가중치와 필터 조합 적용
- 기대값 최적화를 위한 동적 전략 조정

## 데이터베이스 설정
```properties
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://43.202.134.106:5432/mydb
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## 주요 API 엔드포인트

### 로또 추천
- `POST /api/lotto/recommend/enhanced` - 향상된 추천 (필터 적용)
- `POST /api/lotto/recommend` - 기본 추천
- `GET /api/lotto/recommend/history` - 추천 이력 조회

### 통계 분석
- `GET /api/statistics/frequency` - 번호별 출현 빈도
- `GET /api/statistics/trends` - 트렌드 분석
- `GET /api/statistics/hot-cold` - Hot/Cold 번호 분석

### 당첨 결과
- `GET /api/lotto/results` - 당첨 결과 조회
- `GET /api/lotto/results/{round}` - 특정 회차 조회

## 빌드 및 실행
```bash
./gradlew clean build
./gradlew bootRun
```

## 테스트
```bash
./gradlew test
```

## 설정 파일
- `application.properties`: 메인 설정
- 필터 설정은 `lotto.filter.*` 프로퍼티로 관리
- 각 필터는 enabled, priority 등 개별 설정 가능