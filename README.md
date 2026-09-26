# Board — Spring 회원·게시판·댓글 REST API

Codemit 「Spring으로 회원·게시판·댓글 API 만들기」 과제 프로젝트입니다. 누구나 게시글과 댓글을 읽을 수 있고, 로그인한 회원만 작성하며 본인이 작성한 글과 댓글만 수정·삭제할 수 있습니다.

## 과제 요구사항과 현재 상태

| 요구사항 | 현재 구현 |
| --- | --- |
| Spring Boot 3.x · Spring Data JPA · Spring Security | Spring Boot 3.5.16, Java 21, H2 |
| 이메일 가입 검증 · 중복 거절 · 비밀번호 해시 저장 | Bean Validation, 이메일 UNIQUE, BCrypt |
| 로그인 | JWT 발급 및 Bearer 토큰 인증 필터 |
| 게시글 CRUD | 작성 · 목록 · 상세 · 수정 · 소프트 삭제 |
| 최신순 페이지 목록 · 작성자 · 댓글 수 · N+1 방지 | 작성자 EntityGraph, 게시글별 댓글 수 일괄 집계 |
| 댓글 CRUD 및 글 삭제 정책 | 2단계 댓글 조회, 댓글 소프트 삭제, 삭제된 글의 댓글 접근 차단 |
| 공개 조회 · 401 · 작성자 외 403 | Security 접근 규칙과 서비스 작성자 ID 검사 |
| 입력 400 · 대상 없음 404 · 오류 형식 통일 | 기본 흐름 및 재현된 비밀번호·없는 URL 오류 수정, 프레임워크 모든 오류를 망라한 검증은 아님 |
| 선택: 한 단계 대댓글 | 구현 |
| 선택: 제목·본문 검색 | 미구현 |
| 선택: 가입·로그인 및 HTTP 오류 통합 테스트 | 가입·로그인 검증과 경로 401·404·405 자동 테스트 구현. 작성자 403은 서비스 자동 테스트 및 실제 HTTP 검증으로 확인 |

**기본 흐름의 동작 확인과 모든 입력에 대한 통과 보장은 다릅니다. 제출 전 보완 사항을 해결한 뒤 최종 제출하세요.**

## 실행 방법

### 준비

- JDK 21 설치 및 `JAVA_HOME` 설정. 프로젝트의 Java toolchain은 21입니다.
- 별도 Gradle 설치 불필요: 저장소의 Gradle Wrapper 사용.
- 최초 실행에는 Gradle 및 Maven 의존성 다운로드를 위한 인터넷 연결이 필요합니다.
- 별도 DB 설치 불필요: H2 파일 DB가 `./data/board`에 자동 생성됩니다.
- 아래 macOS/Linux 실행 명령은 OpenSSL을 사용합니다. API 예시에는 curl과 jq가 필요합니다.

### 한 명령으로 실행

프로젝트 루트에서 실행합니다.

```bash
JWT_SECRET="$(openssl rand -base64 32)" ./gradlew bootRun
```

JWT 서명 키를 실행할 때 생성해 환경변수로 전달합니다. 설정 파일은 `${JWT_SECRET}`을 참조하며 기본 서명 키를 포함하지 않습니다. 환경변수를 지정하지 않으면 서버가 시작되지 않습니다. 재시작할 때 키가 바뀌면 기존 JWT는 무효가 되므로 다시 로그인해야 합니다. 재시작 간 로그인을 유지하려면 같은 키를 안전한 외부 환경에 보관해 전달하세요.

Windows PowerShell에서는 JDK 21 설치 후 다음과 같이 실행할 수 있습니다.

```powershell
$key = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($key)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($key)
.\gradlew.bat bootRun
```

- 기본 주소: `http://localhost:8090`
- Swagger UI: `http://localhost:8090/swagger-ui/index.html`
- 기본 DB: `jdbc:h2:file:./data/board`, 개발용 사용자 `sa`, 빈 비밀번호
- 스키마: 개발 환경에서 `ddl-auto=update` 사용
- JWT 유효 기간: 기본 30분
- H2 콘솔은 현재 Security 공개 경로에 포함되어 있지 않습니다.

검증 예시를 빈 DB로 재현하려면 기존 DB를 지우는 대신 메모리 DB로 실행하세요. 서버 종료 시 데이터가 사라집니다.

```bash
JWT_SECRET="$(openssl rand -base64 32)" SPRING_DATASOURCE_URL='jdbc:h2:mem:board_demo;DB_CLOSE_DELAY=-1' ./gradlew bootRun
```

### 빌드와 테스트

```bash
JWT_SECRET="$(openssl rand -base64 32)" ./gradlew build
```

기존 파일 DB를 사용하지 않고 테스트하려면:

```bash
JWT_SECRET="$(openssl rand -base64 32)" SPRING_DATASOURCE_URL='jdbc:h2:mem:board_test;DB_CLOSE_DELAY=-1' ./gradlew test
```

빌드 후 실행:

```bash
JWT_SECRET="$(openssl rand -base64 32)" java -jar build/libs/board-0.0.1-SNAPSHOT.jar
```

## API 명세

본문은 JSON이며 `Content-Type: application/json`을 사용합니다. 인증이 필요한 요청에는 다음 헤더를 보냅니다.

```http
Authorization: Bearer <accessToken>
```

조회는 토큰 없이 가능합니다. 공개 경로여도 잘못된 토큰을 명시적으로 보내면 JWT 필터가 401을 반환합니다. 엔티티를 직접 반환하지 않고 응답 DTO로 변환하며 비밀번호와 해시는 응답에 포함하지 않습니다.

### 요청 DTO

| 이름 | JSON 필드 · 조건 |
| --- | --- |
| 가입 | `id`: 이메일 형식·필수, `password`: ASCII 8~64자·필수, `nickName`: 2~30자·필수 |
| 로그인 | `email`: 이메일 형식·필수, `password`: ASCII 8~64자·필수 |
| 게시글 작성/수정 | `title`: 공백 불가·최대 100자, `content`: 공백 불가 |
| 댓글 작성/수정 | `content`: 공백 불가 |

**가입의 `id`는 이메일이며 DB 회원 ID가 아닙니다.** 로그인 요청에서는 같은 값을 `email` 필드로 보냅니다. 현재 구현의 실제 필드명을 문서화한 것입니다. 가입·로그인 비밀번호는 8~64자의 영문·숫자·ASCII 기호(코드 0x21~0x7E)만 허용합니다. 공백·한글·이모지·제어 문자는 허용하지 않으며 DTO의 `@Size`, `@Pattern` 검증 실패 시 400을 반환합니다. 문자 종류를 모두 섞어야 한다는 조건은 없습니다.

### 엔드포인트

`없음`은 성공 응답 본문이 비어 있다는 의미입니다. 생성·수정·삭제는 현재 구현에서 모두 200을 반환합니다.

| 메서드 | 주소 | 인증 | 요청 본문/쿼리 | 성공 응답 | 주요 실패 |
| --- | --- | --- | --- | --- | --- |
| POST | `/member` | 불필요 | 가입 DTO | 200, 없음 | 400 입력, 409 중복 |
| POST | `/auth/login` | 불필요 | 로그인 DTO | 200, `{"accessToken":"..."}` | 400 입력, 401 로그인 실패 |
| POST | `/post` | 필요 | 게시글 DTO | 200, 없음 | 400, 401 |
| GET | `/post` | 불필요 | `page`, `size` | 200, 페이지 목록 | 잘못된 입력 처리에 아래 한계 있음 |
| GET | `/post/{id}` | 불필요 | 없음 | 200, 게시글 상세 | 400 경로 타입, 404 |
| PUT | `/post/{id}` | 작성자 | 게시글 DTO | 200, 없음 | 400, 401, 403, 404 |
| DELETE | `/post/{id}` | 작성자 | 없음 | 200, 없음 | 400, 401, 403, 404 |
| POST | `/comment/{postId}` | 필요 | 댓글 DTO | 200, 없음 | 400, 401, 404 |
| POST | `/comment/{postId}/{commentId}` | 필요 | 댓글 DTO. `commentId`는 부모 댓글 ID | 200, 없음 | 400 깊이 초과/입력, 401, 404 |
| GET | `/comment/{postId}` | 불필요 | 없음 | 200, 댓글·대댓글 배열 | 400 경로 타입, 404 |
| PUT | `/comment/{commentId}` | 작성자 | 댓글 DTO | 200, 없음 | 400, 401, 403, 404 |
| DELETE | `/comment/{commentId}` | 작성자 | 없음 | 200, 없음 | 400, 401, 403, 404 |
| POST | `/auth/logout` | 필요 | 없음 | 200, 없음 | 401 |

`/auth/logout`은 현재 빈 메서드입니다. 토큰 폐기·블랙리스트 기능을 제공하지 않으며 발급된 JWT는 만료 전까지 유효합니다. 클라이언트에서 토큰을 지우는 것과 서버에서 토큰을 무효화하는 것은 다릅니다.

페이지 번호는 0부터 시작하며 기본 크기는 20입니다. 현재 Spring Data 기본 페이지 크기 상한은 2000입니다. 서비스에서 `createdAt DESC, id DESC`로 정렬을 고정하므로 전달한 `sort` 값은 사용하지 않습니다.

### 응답 DTO

- 게시글 목록 항목: `id`, `title`, `writer`(닉네임), `commentCount`, `createdAt`, `updatedAt`
- 페이지 메타데이터: `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last` 등 Spring Data Page 직렬화 필드
- 게시글 상세: `id`, `title`, `content`, `writer`, `createAt`, `updateAt` — 현재 상세와 목록의 시간 필드 이름이 다릅니다.
- 부모 댓글: `id`, `content`, `parentId`(null), `postId`, `nickname`, `deleted`, `replies`
- 대댓글: `id`, `content`, `writerId`, `nickname`, `deleted`
- 시간은 ISO 8601 형식의 LocalDateTime으로 반환하며 시간대 오프셋을 포함하지 않습니다.

### 오류 응답

```json
{
  "code": "ACCESS_DENIED",
  "message": "작성자만 수정·삭제할 수 있습니다."
}
```

| 상태 | code | 상황 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 필수값·길이·이메일 형식 오류 |
| 400 | `INVALID_REQUEST_BODY` | JSON 형식 오류 |
| 400 | `INVALID_PARAMETER` | 경로/파라미터 타입 오류 |
| 400 | `COMMENT_DEPTH_EXCEEDED` | 대댓글에 다시 답글 작성 |
| 401 | `UNAUTHORIZED` | 토큰 없음·만료·변조·형식 오류 |
| 401 | `INVALID_CREDENTIALS` | 이메일/비밀번호 불일치 |
| 403 | `ACCESS_DENIED` | 작성자가 아닌 회원의 수정·삭제 |
| 404 | `TODO_NOT_FOUND` | 존재하지 않거나 삭제된 대상. 오류 코드명은 현재 구현 그대로 |
| 404 | `NOT_FOUND` | 라우팅을 통과한 요청의 경로/정적 자원을 찾을 수 없음 |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드. Allow 헤더에 허용 메서드 반환 |
| 409 | `DUPLICATE_EMAIL` | 이미 가입된 이메일 |
| 500 | `INTERNAL_SERVER_ERROR` | 예상하지 못한 오류. 예상하지 못한 서버 오류용 처리 |

## 설계 설명

### API와 인증 방식

- `/post`, `/comment`를 자원 경로로 두고 POST는 생성, GET은 조회, PUT은 제목·본문 또는 댓글 본문 교체, DELETE는 삭제에 사용했습니다.
- 게시글 ID/댓글 ID는 경로로 전달하고, 작성자 ID는 요청 본문에서 받지 않습니다. 검증된 JWT의 회원 ID를 `@AuthenticationPrincipal`로 받아 사용합니다.
- 성공 후 본문이 필요하지 않은 쓰기 요청은 200과 빈 본문으로 단순하게 응답합니다. 조회는 DTO, 로그인은 토큰 DTO를 반환합니다.
- 인증에는 JWT를 선택했습니다. 별도 프론트엔드나 HTTP 클라이언트가 Bearer 헤더로 API를 호출하기 쉽고, 서버에 로그인 세션을 저장하지 않는 구조를 연습하기 위해서입니다. 이 과제 규모에서는 세션도 유효한 선택입니다.
- JWT는 HS256 서명과 회원 ID(subject), 발급·만료 시각을 포함합니다. 필터가 서명·만료를 검증한 뒤 SecurityContext에 회원 ID를 등록합니다.
- BCrypt의 `encode`로만 비밀번호를 저장하고 로그인에서 `matches`로 비교합니다.
- 인증 쿠키 대신 Authorization 헤더만 사용하는 정책으로 CSRF를 비활성화했습니다. 쿠키 인증을 도입하면 재검토해야 합니다.
- Spring Security는 로그인 여부를 검사하고, 서비스는 작성자 ID를 비교해 수정·삭제를 허용합니다.

### 엔티티 관계

```text
Member 1 ── N Post
Member 1 ── N Comment
Post   1 ── N Comment
Comment(부모) 1 ── N Comment(대댓글)
```

관계는 자식에서 부모를 참조하는 LAZY ManyToOne으로 구현했습니다. 일반 댓글의 parent는 null이고 대댓글은 최상위 댓글만 부모로 가질 수 있습니다. 부모 댓글이 요청한 게시글 소속인지도 검사합니다. 생성·수정 시각은 Hibernate의 CreationTimestamp/UpdateTimestamp로 관리합니다.

### 게시글 목록의 N+1 방지

1. `PostRepository.findAllByDeletedFalse`에 `@EntityGraph(attributePaths = "writer")`를 지정해 작성자를 함께 조회합니다.
2. 현재 페이지의 게시글 ID 목록을 모읍니다.
3. `CommentRepository.countCommentsByPostIds`가 `IN (...)`과 `GROUP BY post.id`로 댓글 수를 한 번에 집계합니다.
4. 집계 결과를 Map으로 만들어 응답에 합칩니다. 댓글이 없는 글은 0입니다.

댓글 수에는 **삭제되지 않은 일반 댓글과 대댓글**을 모두 포함합니다. 부모가 삭제돼도 남아 있는 대댓글은 집계합니다. 목록 응답을 만들면서 게시글마다 댓글 수나 작성자를 별도 조회하지 않습니다.

검증 시 서로 다른 작성자의 게시글이 있는 페이지에서 SQL SELECT 3개를 확인했습니다: 게시글·작성자 조인, 페이지 전체 건수, 댓글 수 집계. 페이지에 따라 전체 건수 쿼리는 생략될 수 있습니다. 댓글 목록 조회의 작성자 최적화는 이 게시글 목록 검증 범위와 별개입니다.

### 삭제 정책

- 게시글 삭제: 행을 지우지 않고 `deleted=true`로 변경합니다. 목록·상세에서 제외하며 수정·재삭제도 404입니다.
- 댓글이 있는 게시글 삭제: 댓글 행과 부모 관계는 보존하되 해당 게시글의 댓글 조회·작성·수정·삭제를 차단합니다. 댓글들의 deleted 값을 일괄 변경하는 방식은 아닙니다.
- 댓글 삭제: 행과 대댓글을 유지합니다. 내용은 응답에서 `삭제된 댓글입니다.`로 치환하고 작성자 정보는 숨깁니다. `deleted=true`를 함께 반환합니다.
- 삭제된 댓글에는 새 대댓글을 달거나 내용을 수정할 수 없습니다. 기존 대댓글 작성자는 자신의 대댓글을 수정·삭제할 수 있습니다.
- 댓글이 전혀 없는 게시글은 `[]`, 존재하지 않거나 삭제된 게시글의 댓글 조회는 404입니다.

## 실제 실행 결과와 재현 방법

2026-09-26 로컬 검증에서 실행용 JAR를 포트 18090과 별도 H2 메모리 DB로 실행했습니다. 아래 요청은 기본 실행 포트 8090에서도 재현할 수 있게 작성했습니다. 기존 데이터가 없는 DB 기준이며 회원·게시글·댓글 ID와 시간은 실행에 따라 달라집니다. 테스트 계정 비밀번호는 예시 데이터입니다. JWT는 실제 값 대신 생략 표시합니다.

```bash
BASE=http://localhost:8090
```

### 1. 가입

```bash
curl -i -X POST "$BASE/member" \
  -H 'Content-Type: application/json' \
  -d '{"id":"alice@example.com","password":"Password123!","nickName":"alice"}'
```

실제 결과: **200**, 빈 본문.

### 2. 로그인

```bash
LOGIN=$(curl -sS -X POST "$BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"Password123!"}')
printf '%s\n' "$LOGIN"
TOKEN=$(printf '%s' "$LOGIN" | jq -r '.accessToken')
```

실제 결과: **200**.

```json
{"accessToken":"<JWT 생략>"}
```

### 3. 글 쓰기

```bash
curl -i -X POST "$BASE/post" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"첫 게시글","content":"본문입니다."}'
```

실제 결과: **200**, 빈 본문. 생성 응답에는 ID가 없으므로 최신 목록에서 조회합니다.

```bash
POST_ID=$(curl -sS "$BASE/post?page=0&size=1" | jq -r '.content[0].id')
```

### 4. 댓글 쓰기

```bash
curl -i -X POST "$BASE/comment/$POST_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"content":"첫 댓글"}'
```

실제 결과: **200**, 빈 본문.

### 5. 비로그인 목록 조회

```bash
curl -i "$BASE/post?page=0&size=10"
```

실제 결과: **200**. 응답 본문은 아래와 같습니다.

```json
{
  "content": [
    {
      "id": 1,
      "title": "첫 게시글",
      "writer": "alice",
      "commentCount": 1,
      "createdAt": "2026-09-26T23:07:12.966271",
      "updatedAt": "2026-09-26T23:07:12.966294"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 1,
  "size": 10,
  "number": 0,
  "empty": false
}
```

### 6. 비로그인 쓰기: 401

```bash
curl -i -X POST "$BASE/post" \
  -H 'Content-Type: application/json' \
  -d '{"title":"blocked","content":"blocked"}'
```

실제 결과: **401**.

```json
{"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}
```

### 7. 다른 회원으로 수정: 403

```bash
curl -i -X POST "$BASE/member" \
  -H 'Content-Type: application/json' \
  -d '{"id":"bob@example.com","password":"Password123!","nickName":"bob"}'

OTHER_TOKEN=$(curl -sS -X POST "$BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"bob@example.com","password":"Password123!"}' | jq -r '.accessToken')

curl -i -X PUT "$BASE/post/$POST_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"blocked","content":"blocked"}'
```

가입과 로그인은 각각 200, 수정 요청의 실제 결과는 **403**입니다.

```json
{"code":"ACCESS_DENIED","message":"작성자만 수정·삭제할 수 있습니다."}
```

### 8. 대댓글과 삭제 표시

```bash
COMMENT_ID=$(curl -sS "$BASE/comment/$POST_ID" | jq -r '.[0].id')

curl -i -X POST "$BASE/comment/$POST_ID/$COMMENT_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"content":"대댓글"}'

curl -i -X DELETE "$BASE/comment/$COMMENT_ID" \
  -H "Authorization: Bearer $TOKEN"

curl -i "$BASE/comment/$POST_ID"
```

각 요청은 200입니다. 부모 댓글 삭제 후 조회한 실제 응답은 다음과 같습니다.

```json
[
  {
    "id": 1,
    "content": "삭제된 댓글입니다.",
    "parentId": null,
    "postId": 1,
    "nickname": null,
    "deleted": true,
    "replies": [
      {
        "id": 2,
        "content": "대댓글",
        "writerId": 2,
        "nickname": "bob",
        "deleted": false
      }
    ]
  }
]
```

## 검증 범위 및 제출 전 보완 사항

### 확인한 결과

- `test bootJar` 성공. 애플리케이션 기동 테스트 1개와 소프트 삭제 DB 연동 테스트 4개 통과.
- 실제 HTTP 요청 43개 중 기본 시나리오 41개가 기대 상태와 일치. 당시 경계 입력 2개에서 500 재현. 이후 비밀번호 입력 정책을 ASCII 8~64자로 제한하고 경로 404 처리를 추가해 두 오류를 해결했습니다. 변경 후 자동 테스트로 확인했습니다.
- 가입·로그인, 글/댓글 작성·조회·수정·삭제, 대댓글 작성, 비작성자의 글/댓글 수정·삭제 403, 비로그인 쓰기 401을 확인했습니다.
- 잘못된 이메일·짧은 비밀번호·빈 글/댓글·3단계 답글은 400, 중복 이메일은 409, 없는 글/댓글과 삭제된 대상은 404였습니다.
- 수정·삭제 후 실제 조회 내용과 댓글 수 변화, 삭제된 부모 아래 대댓글 유지도 확인했습니다.
- 서로 다른 작성자의 게시글 4개에서 `size=2`로 목록을 조회해 최신 ID `[5, 4]`, 전체 4개, 총 2페이지를 확인했습니다. 해당 목록 요청은 SQL SELECT 3개였습니다.
- 위 HTTP 검증은 별도 메모리 DB에서 실행한 수동 검증이며, 저장소에 HTTP 통합 테스트로 추가된 것은 아닙니다.

### 비밀번호 경계값 자동 검증

`PasswordValidationTests`에서 MockMvc와 별도 H2 메모리 DB로 ASCII 8자·64자의 가입·로그인 성공, 7자·65자 및 한글·이모지·공백·제어 문자 거절, 빈 값·누락 입력 400을 검증합니다. 비밀번호 검증 추가 후 기존 테스트를 포함한 총 9개 테스트가 통과했습니다. 작성자 403을 포함한 전체 HTTP 시나리오의 자동화는 아직 별도 과제입니다.

### 경로 오류 자동 검증

`RoutingErrorTests`에서 없는 공개 경로 404, 인증 후 없는 경로 404, 지원하지 않는 PATCH 405와 Allow 헤더, 비인증 보호 경로 401, 없는 게시글의 기존 404 형식을 검증했습니다. 기존 테스트와 합쳐 총 14개 테스트가 통과했습니다. 보호된 경로는 라우팅보다 인증 검사가 먼저이므로 토큰 없이 요청하면 404·405보다 401이 우선합니다.

### JWT 키 외부 설정 적용

`application.yaml`의 고정 키를 제거하고 `${JWT_SECRET}`으로 변경했습니다. 실행과 애플리케이션 기동 테스트에 환경변수가 필요하므로 위 명령에서 새 임시 키를 전달합니다. 기존 키는 재사용하지 않습니다. 이 변경은 과거 Git 커밋의 키를 삭제하지 않으며, Git 이력 정리는 별도 작업입니다.

### 제출 전에 해결할 항목

1. **과거 Git 이력의 키**: 현재 설정 파일에서는 키를 제거했습니다. 과거 커밋에 포함된 키는 폐기하고 재사용하지 마세요. 공개 저장소 제출 시 과거 이력에 포함된 키의 정리는 별도로 확인해야 합니다.
2. **비밀번호 정책 — 해결**: 가입·로그인 DTO에서 ASCII 8~64자만 허용하고 위반 시 `VALIDATION_ERROR`와 400을 반환합니다. ASCII 한 글자는 1바이트이므로 BCrypt의 72바이트 한도를 넘지 않습니다. 서비스의 별도 바이트 검사와 전용 예외는 사용하지 않습니다.
3. **없는 URL·지원하지 않는 메서드 — 해결**: `NoResourceFoundException`과 `NoHandlerFoundException`은 404, `HttpRequestMethodNotSupportedException`은 405로 처리합니다. 오류 DTO를 유지하고 405에는 Allow 헤더를 포함합니다. 그 외 프레임워크 오류를 전부 검증한 것은 아닙니다.
4. **테스트 파일 포함**: 추가한 `SoftDeleteTests`, `PasswordValidationTests`, `RoutingErrorTests`는 검증 당시 Git 미추적 상태였습니다. 이 테스트 결과를 제출물에서도 재현하려면 함께 커밋해야 합니다.

기본 기능과 발견했던 두 가지 500 오류의 수정은 확인했습니다. 고정 서명 키도 현재 설정에서 제거했습니다. 최종 제출 전 변경 파일·테스트의 커밋과 과거 키 이력을 점검해야 하며, 실제 채점 통과를 보장하는 것은 아닙니다.

### 제출물 정리

- GitHub 제출은 공개 저장소를 사용합니다.
- `build/`, `target/`, `.gradle/`, 로컬 DB, 실제 비밀번호·서명 키와 `.env`는 제출하지 않습니다.
- Gradle Wrapper의 `gradlew`, `gradlew.bat`, `gradle/wrapper/`는 실행에 필요하므로 포함합니다. `gradle/`과 캐시 폴더 `.gradle/`은 다릅니다.
- `.gitignore`에 빌드·캐시·로컬 DB 제외 규칙이 있지만, 이미 추적 중인 키 파일까지 자동으로 제외해 주지는 않습니다.
- zip은 작업 폴더 전체를 압축하기보다, 키를 제거하고 필요한 변경을 커밋한 뒤 `git archive --format=zip --output=../board-submission.zip HEAD`로 만드는 방법을 권장합니다. 미커밋·미추적 파일은 이 zip에 포함되지 않습니다.
