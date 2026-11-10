# CLAUDE.md — 마작 솔리테어 (Android / Kotlin)

이 파일은 Claude Code가 이 프로젝트를 개발할 때 따르는 기준 문서다. 기획서 「마작 솔리테어 출시 전 필수 기능 기획서」의 내용을 개발 명세로 옮긴 것이며, 충돌 시 이 파일이 우선한다. 명세에 없는 결정이 필요하면 임의로 정하지 말고 질문하거나, 가장 단순한 쪽으로 구현하고 `docs/DECISIONS.md`에 기록한다.

---

## 1. 프로젝트 개요

- 1인용 마작 솔리테어(패 짝 맞추기) 게임, **Android 전용**
- 오프라인 플레이 가능, 구글 계정 연동 시 클라우드 저장 + 주간 랭킹
- 수익: AdMob 보상형/전면 광고 + 광고 제거 인앱결제
- 타깃: 40대 이상·시니어 포함 성인 → 큰 패, 고대비, 쉬운 조작이 최우선

## 2. 기술 스택

| 영역 | 선택 |
| --- | --- |
| 언어 | Kotlin (최신 안정 버전) |
| UI | Jetpack Compose + Material 3, 게임 보드는 Compose `Canvas` |
| 아키텍처 | 단일 Activity, MVVM + UDF (ViewModel + `StateFlow`) |
| DI | Hilt |
| 로컬 저장 | DataStore (Proto 또는 Preferences + kotlinx.serialization JSON) |
| 직렬화 | kotlinx.serialization |
| 비동기 | Kotlin Coroutines / Flow |
| 네비게이션 | Navigation Compose |
| 계정/랭킹/저장 | Google Play 게임즈 서비스 v2 (`play-services-games-v2`) |
| 광고 | Google Mobile Ads SDK + UMP SDK |
| 결제 | Google Play Billing Library (최신) |
| 테스트 | JUnit, kotlinx-coroutines-test, Compose UI Test |
| 빌드 | Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`) |

- minSdk 24, targetSdk/compileSdk는 Google Play 현재 요구 수준 이상
- 라이브러리 버전은 추가 시점의 최신 안정 버전을 사용하고 버전 카탈로그에만 기록

## 3. 패키지 구조

```
app/src/main/java/<package>/
├── MainActivity.kt
├── MahjongApp.kt                 # Application, Hilt
├── core/
│   ├── model/                    # Tile, TileFace, Board, Layout, LevelResult …
│   ├── engine/                   # 순수 Kotlin 게임 로직 (Android 의존성 금지)
│   │   ├── FreeTileRule.kt
│   │   ├── BoardGenerator.kt     # 풀이 보장 생성
│   │   ├── Solver.kt             # 검증용 자동 풀이기
│   │   ├── MoveFinder.kt         # 가능한 짝 탐색 (힌트·막힘)
│   │   ├── Shuffler.kt
│   │   └── Scoring.kt
│   └── util/
├── data/
│   ├── level/                    # assets/layouts/*.json 로딩
│   ├── progress/                 # DataStore 진행 저장
│   ├── games/                    # Play 게임즈 (로그인, 저장된 게임, 리더보드, 업적)
│   ├── ads/                      # AdMob, UMP
│   └── billing/                  # 광고 제거 인앱결제
├── ui/
│   ├── theme/                    # 색, 타이포, 셰이프 (§10 디자인 토큰)
│   ├── splash/ home/ levelselect/ game/ result/ settings/
│   └── components/
└── di/
```

**규칙: `core/engine`은 Android 클래스를 import하지 않는다.** 전부 JVM 단위 테스트로 검증 가능해야 한다.

## 4. 게임 규칙과 데이터 모델

### 4.1 패 구성 (풀 세트 144장)

| 종류 | TileFace | 장수 |
| --- | --- | --- |
| 만수 1~9 | `Character(1..9)` | 각 4장 |
| 통수 1~9 | `Dot(1..9)` | 각 4장 |
| 삭수 1~9 | `Bamboo(1..9)` | 각 4장 |
| 풍패 동남서북 | `Wind(E,S,W,N)` | 각 4장 |
| 삼원패 백·발·중 | `Dragon(WHITE,GREEN,RED)` | 각 4장 |
| 꽃패 매란국죽 | `Flower(1..4)` | 각 1장 |
| 계절패 춘하추동 | `Season(1..4)` | 각 1장 |

- **짝 규칙**: 같은 face끼리 짝. 단, 꽃패 4종은 서로 모두 짝, 계절패 4종도 서로 모두 짝.
- `fun TileFace.matches(other: TileFace): Boolean`로 구현.
- 144장 미만의 배치는 face를 쌍 단위로 뽑아 사용한다(항상 짝수, 모든 face가 짝을 이룸).

### 4.2 좌표 모델

- 패 위치 `TilePos(x: Int, y: Int, z: Int)` — 반 칸 단위 격자. 패 1장은 가로 2 × 세로 2칸을 차지.
  - x, y는 반 칸 오프셋 배치를 표현하기 위해 반 칸 단위 정수
  - z는 층(0이 바닥)
- `Tile(id: Int, pos: TilePos, face: TileFace)`
- `Board(tiles: Map<Int, Tile>, removed: Set<Int>)` — 불변 객체로 다루고 이동마다 새 상태 생성

### 4.3 자유로운 패 판정 (`FreeTileRule`)

패 A가 자유로우려면 둘 다 만족:

1. **위가 비어 있음**: z+1 층에 A와 x 범위 [x-1, x+1], y 범위 [y-1, y+1]로 겹치는 패가 없음
2. **좌 또는 우 중 한쪽이 비어 있음**: 같은 z에서 x-2 위치(y가 ±1 범위로 겹침)에 패가 없거나, x+2 위치에 패가 없음

- 판정은 제거되지 않은 패만 대상으로 한다.
- 성능: 위치 → 패 인덱스 해시맵을 유지해 O(1) 근접 조회.

### 4.4 게임 상태 흐름

```
Idle → 첫 패 선택(Selected) → 두 번째 패
  ├─ 짝 맞음 → 두 패 제거 → (남은 패 0) Cleared
  │                       → (가능한 짝 0) Stuck
  │                       → Idle
  ├─ 같은 패 다시 탭 → 선택 해제 → Idle
  └─ 짝 아님 → 두 번째 패를 새 선택으로 → Selected
```

- 선택 불가(자유롭지 않은) 패 탭: 짧은 흔들림 애니메이션 + 진동(설정 시), 상태 변화 없음
- 모든 이동은 `MoveHistory` 스택에 기록(되돌리기용)

## 5. 레벨 시스템

### 5.1 배치 데이터

- `app/src/main/assets/layouts/*.json` — 배치 30~40종을 직접 정의
- 스키마:

```json
{
  "id": "turtle_small",
  "name": "작은 거북이",
  "difficulty": 2,
  "positions": [ { "x": 0, "y": 0, "z": 0 }, ... ]
}
```

- `positions` 개수는 반드시 짝수. 로딩 시 검증하고 위반하면 빌드 테스트에서 실패.
- `assets/levels.json`: 단계 번호 → 배치 id 매핑 (1~200단계)

### 5.2 난이도 곡선

| 구간 | 패 수 | 층 수 | 비고 |
| --- | --- | --- | --- |
| 1~10 | 24~40 | 1~2 | 튜토리얼, 광고 없음 |
| 11~50 | 40~80 | 2~3 | |
| 51~120 | 80~120 | 3~4 | |
| 121~200 | 120~144 | 4~5 | 클래식 배치 포함 |

- 같은 배치가 연속 10단계 안에 다시 나오지 않게 `levels.json`을 구성
- 201단계부터는 `layoutId = layouts[hash(stage) % size]` 방식으로 무한 생성(난이도 4~5 배치만 사용)

### 5.3 풀이 보장 생성 (`BoardGenerator`) — 필수

- **시드 = 단계 번호** (`Random(stage.toLong() * 31 + SALT)`) → 같은 단계는 모든 기기에서 같은 판
- 알고리즘(역방향 생성):
  1. 배치 위치 전체를 "비어 있음"으로 둔다.
  2. 현재 채워진 패들만 있다고 가정할 때, 새로 놓아도 "제거 순서상 자유로운" 빈 자리 2곳을 고른다
     (= 완성 상태에서 거꾸로 하나씩 쌓는 것. 아래층/안쪽부터 채움).
  3. 그 두 자리에 같은 face 쌍을 배정한다.
  4. 모든 자리가 찰 때까지 반복. 막히면 해당 시도 폐기 후 재시도(최대 50회).
- 생성 결과는 `Solver`로 검증. 실패 시 다음 시드 변형으로 재생성.
- **단위 테스트**: 1~200단계 전부 생성 → Solver로 풀이 가능 확인 (CI에서 실행).

### 5.4 Solver

- DFS + 메모이제이션(제거 집합 비트셋 해시), 시간 제한 있음
- 용도: 생성 검증, 셔플 결과 검증, 테스트

## 6. 보조 도구

| 도구 | 동작 | 기본 지급 | 충전 |
| --- | --- | --- | --- |
| 힌트 | `MoveFinder`로 가능한 짝 1쌍을 찾아 1.5초 반짝임 | 단계당 3회 | 보상형 광고 1회당 +1 |
| 되돌리기 | 직전 제거 취소, 연속 가능 | 무제한 | — |
| 셔플 | 남은 패의 face만 재배정, 위치 유지 | 단계당 1회 | 보상형 광고 1회당 +1 |

- **셔플은 풀이 가능해야 한다**: 남은 위치 집합에 대해 5.3 역방향 생성 알고리즘으로 face 재배정 → Solver 검증.
- **막힘 감지**: 매 제거 후 `MoveFinder.count() == 0`이면 Stuck 다이얼로그
  - 버튼: [셔플] (보유 시) / [광고 보고 셔플] / [처음부터 다시]
- **자동 완성**: 설정에서 켤 때만 활성(기본 꺼짐). 남은 패가 모두 자유로울 때 "자동 완성" 버튼 노출, 누르면 순서대로 제거 애니메이션. 자동으로 실행하지 않는다.

## 7. 점수·별

```
기본 점수   = 100 + stage * 10
시간 보너스 = max(0, targetSeconds - elapsedSeconds) * 2
감점        = 힌트 사용 * 30 + 셔플 사용 * 50
단계 점수   = max(10, 기본 점수 + 시간 보너스 - 감점)
targetSeconds = 남은 패 수 시작값 * 3   (예: 144장 → 432초)
```

- 별: ★1 클리어 / ★2 힌트·셔플 미사용 또는 targetSeconds 이내 / ★3 둘 다 만족
- 되돌리기는 감점 없음
- 일시정지 중, 앱 백그라운드 중에는 시간 정지

## 8. Play 게임즈 서비스 연동

### 8.1 로그인
- 앱 시작 시 `PlayGamesSdk.initialize()` → `GamesSignInClient.isAuthenticated()`
- 실패해도 게스트로 진행. 설정 화면에 "구글 계정 연결" 버튼(`signIn()`).
- 로그인 상태는 `StateFlow<AuthState>`로 노출.

### 8.2 진행 저장
- **로컬(DataStore)이 원본**, 로그인 시 Saved Games(`SnapshotsClient`)에 동기화
- 저장 데이터 (`SaveData`, JSON 직렬화, `version` 필드 포함):
  - `highestClearedStage`, `stars: Map<Int, Int>`, `hints`, `shuffles`
  - `weekKey`, `weeklyScore`, `weeklyClearedStages: Set<Int>`
  - `settings`, `adsRemoved`(참고용, 실제 판정은 Billing 복원 기준)
- 충돌 해결: `highestClearedStage`가 큰 쪽 채택, `stars`는 단계별 최댓값 병합, 아이템 수는 큰 쪽
- 동기화 시점: 앱 시작(로그인 후), 단계 클리어, 앱 백그라운드 진입

### 8.3 리더보드

| 리더보드 | 제출 값 | 비고 |
| --- | --- | --- |
| `leaderboard_weekly_score` | 이번 주 누적 단계 점수 | 주간 탭 사용 |
| `leaderboard_highest_stage` | 최고 클리어 단계 번호 | 전체 기간 탭 사용 |

- 리더보드는 기간별 **최고 값**만 유지하므로, 앱이 주간 누적을 직접 계산해 매 클리어마다 누적값 전체를 제출한다.
- **주 경계**: Play 게임즈 주간 리셋 기준에 맞춰 `America/Los_Angeles` 시간대 기준 일요일 00:00 시작으로 `weekKey`(예: `2026-W46`)를 계산. `weekKey`가 바뀌면 `weeklyScore = 0`, `weeklyClearedStages` 초기화.
- 같은 주에 이미 클리어한 단계를 다시 깨면 주간 점수에 더하지 않는다(최초 클리어만 반영).
- 미로그인 상태 점수는 로컬에 쌓아두고, 로그인 시 현재 `weekKey`의 누적값을 제출.
- 랭킹 화면: 출시 버전은 `getLeaderboardIntent()`로 Play 게임즈 기본 UI 사용.
- 리더보드/업적 ID는 Play Console에서 내보낸 `res/values/games-ids.xml`을 사용. 하드코딩 금지.

### 8.4 업적
- 첫 클리어 / 10·50·100·200단계 클리어 / 힌트 없이 클리어 / ★3 클리어 10회 / 7일 연속 플레이
- 업적 판정은 `AchievementEvaluator`(순수 로직)에서 하고 `data/games`가 전송만 담당.

## 9. 광고·결제

### 9.1 AdMob
- 시작 시 **UMP 동의 → 동의 가능 상태에서만 `MobileAds.initialize()`**
- 광고 단위 ID는 `BuildConfig`로 주입: **debug 빌드는 Google 테스트 광고 ID만 사용**, release만 실제 ID
- 실제 ID·앱 ID는 `local.properties` 또는 CI 시크릿에서 읽고 저장소에 커밋하지 않는다.

| 형식 | 위치 | 규칙 |
| --- | --- | --- |
| 보상형 | 힌트+1, 셔플+1, 막힘 시 셔플, 클리어 보상 2배(힌트+1 추가 지급) | 사용자가 버튼을 누를 때만. 버튼에 광고임을 명시 |
| 전면 | 클리어 결과 → 다음 단계 이동 시 | 11단계부터, 클리어 3회마다 1회, 직전 전면 광고 후 최소 3분 |
| 배너 | 사용 안 함 | |

- 보상은 **`OnUserEarnedRewardListener` 콜백에서만** 지급. 닫기 콜백에서 지급 금지.
- 광고는 미리 로드해 두고, 로드 안 됐으면 보상형 버튼 비활성화(로딩 표시).
- 게임 플레이 중, 패 탭 직후에는 어떤 전면 광고도 띄우지 않는다.
- 광고 표시 중 게임 타이머 정지.

### 9.2 광고 제거 인앱결제
- 일회성 상품 `remove_ads`
- 구매 시 전면 광고만 제거, 보상형 광고 버튼은 유지(사용자 선택형이므로)
- 앱 시작 시 `queryPurchasesAsync`로 복원, 설정에 "구매 복원" 버튼
- 구매 확인(acknowledge) 처리 필수

## 10. UI 구현 지침

### 10.1 화면

| 라우트 | 내용 |
| --- | --- |
| `splash` | 로고, 로그인 시도, UMP·광고 초기화, 레벨 데이터 프리로드 |
| `home` | 이어하기(현재 단계), 단계 선택, 랭킹, 설정 |
| `levelSelect` | 단계 그리드, 별 표시, 잠금(최고 클리어+1까지 열림) |
| `game/{stage}` | 보드, 상단(단계, 남은 패 수, 시간, 일시정지), 하단(힌트·되돌리기·셔플 + 남은 횟수 배지) |
| `result/{stage}` | 별 애니메이션, 점수, 이번 주 누적 점수, 보상 2배 버튼, 다음 단계 |
| `settings` | 소리·효과음·진동, 자동 완성, 큰 글씨 모드, 구글 계정, 광고 제거 구매·복원, 개인정보 처리방침 링크 |

### 10.2 디자인 토큰 (`ui/theme`)

| 토큰 | 값 | 용도 |
| --- | --- | --- |
| `BgFeltTop` | `#1F6B4A` | 배경 그라데이션 위 |
| `BgFeltBottom` | `#134A33` | 배경 그라데이션 아래 |
| `TileFace` | `#F7F1E3` | 패 앞면 |
| `TileSide` | `#D9CBA8` | 패 옆면(두께) |
| `TileShadow` | `#0B2E20` @ 35% | 패 그림자 |
| `Accent` | `#E8B84A` | 주요 버튼, 강조, 별 |
| `OnBg` | `#FFFFFF` | 배경 위 텍스트 |
| `TileDisabledOverlay` | `#000000` @ 28% | 선택 불가 패 |
| `TileSelectedBorder` | `#E8B84A` 3dp | 선택 패 테두리 |

- **모든 화면 배경은 초록 펠트 그라데이션**(`BgFeltTop → BgFeltBottom`, 세로). 다크/라이트 모드 무관하게 동일.
- 패 그림은 `res/drawable/tile_*.xml`(VectorDrawable)로 제공됨 — 디자인 에셋 파일명 규칙은 `DESIGN_REQUEST.md` §6 참조. 에셋이 오기 전에는 텍스트 플레이스홀더로 렌더링.

### 10.3 보드 렌더링
- Compose `Canvas`에 z 오름차순 → y 오름차순 → x 내림차순으로 그려 겹침 표현
- 층마다 좌상단 방향으로 (4dp, -4dp) 오프셋해 입체감
- 패 크기는 화면에 배치 전체가 들어가도록 자동 계산하되 **패 너비 최소 40dp**; 더 작아지면 핀치 줌/드래그 활성화
- 터치 판정은 위층부터 역순 검사
- 애니메이션: 선택 시 살짝 들림(scale 1.05 + 그림자 증가), 제거 시 페이드+축소 200ms, 힌트 반짝임
- 선택 표시는 색만이 아니라 테두리 + 들림으로 구분(색약 대응)
- 세로/가로 모두 지원, 태블릿에서는 보드 영역 최대화

### 10.4 튜토리얼
- 1단계: 손가락 오버레이로 자유로운 패 2장 안내 → 막힌 패를 탭하면 "위나 양옆이 막힌 패는 고를 수 없어요" 안내
- 힌트·셔플 버튼은 처음 노출되는 단계에서 1회 툴팁

### 10.5 문자열
- 모든 문자열은 `strings.xml`(기본 한국어 `values/`, 영어 `values-en/` 함께 작성)

## 11. 테스트 기준

- `core/engine` 커버리지 우선: FreeTileRule, matches, MoveFinder, Scoring, weekKey 계산, SaveData 병합
- `LevelGenerationTest`: 1~200단계 생성 + Solver 통과, 배치 JSON 짝수 검증
- `ShuffleTest`: 임의 진행 상태 1,000개에서 셔플 후 풀이 가능
- 광고·게임즈·결제는 인터페이스로 감싸고 Fake 구현으로 ViewModel 테스트
- 커밋 전 `./gradlew lint testDebugUnitTest` 통과

## 12. 개발 순서 (마일스톤)

1. 프로젝트 셋업: Gradle, Hilt, 테마(초록 배경), 네비게이션 뼈대
2. `core/engine`: 모델, 자유 패 판정, MoveFinder, 테스트
3. BoardGenerator + Solver + 레벨 JSON 로딩 + 생성 테스트
4. 게임 화면: 보드 렌더링, 선택/제거, 되돌리기, 클리어·막힘
5. 힌트·셔플, 점수·별, 결과 화면, 단계 선택, 로컬 저장
6. 튜토리얼, 설정, 효과음·진동
7. Play 게임즈: 로그인, 저장된 게임 동기화, 리더보드, 업적
8. AdMob(UMP, 보상형, 전면) + 광고 제거 결제
9. 배치 30~40종 + 200단계 구성, 난이도 조정
10. 성능 점검(저사양 기기 144장), 오프라인/네트워크 끊김 처리, 릴리스 빌드(R8, 서명)

각 마일스톤 끝에 빌드·테스트 통과 상태로 커밋한다.

## 13. 금지·주의 사항

- 참고 앱(Mahjong Master: Daily Match)의 이름, 아이콘, 그림, 화면 디자인을 사용하지 않는다. 게임 규칙만 참고.
- debug 빌드에서 실제 광고 ID 사용 금지.
- 서명 키, 광고 ID, 결제 관련 키를 저장소에 커밋하지 않는다.
- 플레이 중 전면 광고, 보상 강제 시청, 광고를 닫아도 보상 지급하는 코드 금지.
- 게임 로직에 `System.currentTimeMillis()` 직접 사용 금지 → `Clock` 인터페이스 주입(테스트 가능하게).
- 자동 완성을 사용자 동의 없이 실행하지 않는다.
