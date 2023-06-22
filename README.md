# msa-blog-system
msa 환경 기반 블로그 시스템 구축
* 비동기(reactive-web), 동기(spring-boot), kotlin 언어 기반으로 프로젝트를 개발하였다.
* msa 환경에서 블로그 서비스를 개발하기 위해 블로그 서비스와 유저 서비스를 개발한다.
* 사용자 관리 기능 : 회원가입, 로그인, 유저 정보 찾기, 토큰 권한 확인
* 게시물 관리 기능 : 게시물 작성(글,이미지,동영상), 게시물 수정, 게시물 삭제, 게시물 검색
* 댓글 관리 기능 : 댓글 작성, 대댓글 작성, 댓글 수정, 댓글 삭제, 댓글 조회(댓글 대댓글 포함)
* 기타 : 빠른 개발을 위해 로컬 환경에서 개발 및 테스트 진행 

### 핵심 기능 설명(핵심 기술)
* config-server : spring cloud config는 분산 시스템 설정 정보를 중앙 집중화 하여 관리하기 위한 서비스, 모듈 : spring-boot, spring-cloud-config, spring-cloud-bus-amqp
* rabbit-mq-service : spring cloud bus Spring Config 이벤트를 감지하여 변경된 설정 정보를 다른서비스에 전달, RabbitMQ를 사용하여 Cloud Bus는 구성 변경 이벤트를 메시지로 발행한다. 모듈 : rabbit-mq
* Eureka-discovery : MSA 아키텍쳐에서 서비스 인스턴스의 정보를 등록과 해제를 추적하고 관리하는 서비스, 모듈 : netflix-eureka-server
* Spring cloud gateway : 단일 진입점을 만들어 라우팅,로드 밸런싱,보안 인증 필터 사용, 모니터링 로깅의 이점을 가진다, 모듈 : spring-cloud-starter-gateway, spring-cloud-config-client, spring-cloud-starter-netflix-eureka-client
* user-service : 비동기 기반 서비스, 사용자 도메인과 관련된 서비스(사용자관리,인증등)를 관리한다. 모듈 : reative web, r2dbc, spring-cloud-config-client, zipkin, spring-cloud-starter-sleuth,netflix-eureka-client
* blog-service : 동기 기반 서비스, 블로그 도메인과 관련된 서비스(블로그,댓글,블로그 검색등)를 관리한다. 모듈 : spring-boot, spring-boot-starter-data-redis, lettuce-core, spring-kafka, spring-boot-starter-data-mongodb, elasticsearch-rest-high-level-client, netflix-eureka-client, spring-cloud-config-client, zipkin, spring-cloud-starter-sleuth
* zipkin : 분산된 MSA 서비스들 간의 트래픽을 추적해 문제를 사전에 방지하거나 해결한다. 모듈 : zipkin

### 주요 기능 
1. 사용자 관리:
    * 회원 가입 : 사용자 회원 가입 
    * 로그인 및 인증 : JWT토큰을 이용한 인증 
    * 유저 정보 찾기 : JWT토큰을 이용한 정보 찾기 
    * 토큰 권한 확인 : JWT토큰 권한 확인 
2. 게시물 관리:
    * 게시물 작성(글,이미지,동영상) : 글, 이미지, 동영상 정보를 mongodb에 저장하고 파일을 스토리지에 저장한다. 저장된 게시글은 kafka producer를 통해 ElasticSearch(검색용 데이터)에 저장된다.
    * 게시물 수정 : MongoDB의 게시물 정보를 수정한다.
    * 게시물 삭제 : MongoDB의 게시물 정보를 삭제한다.
    * 게시물 목록 조회 : MongoDB의 게시물 정보를 조회한다.
    * 게시물 검색 : ElesticSearch 이용한 게시물 검색, nori_part_of_speech 필터 사용해 품사 제거
3. 댓글 관리:
    * 게시물 댓글 작성 : 게시물에 댓글 작성 
    * 게시물 대댓글 작성 : 게시물의 댓글에 댓글을 작성한다
    * 게시물 댓글 수정 : 게시물의 댓글을 수정한다
    * 게시물 댓글 삭제 : 게시물의 댓글을 삭제한다(soft delete)
    * 블로그 글 댓글 목록 조회: 게시글의 댓글과 대댓글을 순서대로 조회한다.

### 로컬 환경 구축
```
git clone https://github.com/Rhw38555/msa-blog-service.git
```

### 서비스 통신 방식(흐름)
1. 모든 호출은 apigateway를 통해 호출되고 특정 url(로그인,회원가입)을 제외하면 token 정보가 필요하다.
2. 블로그 서비스 에서는 JWT TOKEN을 이용해 유저 정보를 받아온다.
   redis cache에 유저 정보가 존재할 경우 redis cache에서 정보를 받아온다.
   만약 redis cache에 유저 정보가 존재하지 않으면 blog service는 user service를 호출해 토큰으로 유저 정보를 받아온다.
3. 블로그 글이 생성되면 Kafka Producer에 메세지가 생성되고 Kafka Consumer가 message를 받아 Elastic Search에 블로그 글을 저장한다.

### 요구 사항 
* 개발 언어 : kotlin, java
* 서비스 프레임 워크 : Spring Boot, Spring reactive web, RabbitMQ, Zipkin, redis, kafka, elastic Search, spring cloud ..
* 테스트 코드 : Kotest Mockk 기반 테스트 코드 작성 
* 개발 툴 : IntelliJ IDEA 2022.2.3 

### 기타 
* TODO 1 : Mongodb, Es 동기화 서비스 구축 
* TODO 2 : mongodb search 최적화(대용량 성능 개선)
* TODO 3 : 댓글 알림 기능 추가 
* TODO 4 : 소셜 로그인 기능 추가 
* TODO 5 : 게시물 작성 시 SAGA 패턴 도입하여 일관성 롤백 과정 도입 
