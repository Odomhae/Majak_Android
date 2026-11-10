# DECISIONS

CLAUDE.md에 없는 결정, 또는 명세와 다르게 간 결정을 기록한다.

## 마일스톤 1

- **minSdk 26 유지**: 명세는 24지만 프로젝트 생성 시점부터 26이었고 Android 8 미만 점유율이 극소수라 유지한다.
- **AGP 9.2.0-alpha08 → 9.5.1(안정), Kotlin 2.2.10 → 2.4.21**: Hilt 2.60.1/KSP가 최신 Kotlin 메타데이터를 요구하고, 알파 AGP를 피하기 위해 올렸다. 버전은 `gradle/libs.versions.toml`에만 기록.
- **Hilt는 KSP로 처리**(kapt 미사용). AGP 9 내장 Kotlin과 함께 쓰기 위함.
- **패 에셋 파일명 규칙**(`DESIGN_REQUEST.md` 부재): 현재 `res/drawable/tile_*` 이름을 규칙으로 확정한다.
  `char_1..9`, `dot_1..9`, `bamboo_1..9`, `wind_e|s|w|n`, `dragon_white|green|red`, `flower_1..4`, `season_1..4`, `back`.
- **테마는 항상 고정 펠트 다크 스킴**: 다이내믹 컬러와 시스템 다크/라이트 연동을 쓰지 않는다(§10.2).
  시스템 바 아이콘은 항상 밝게(`SystemBarStyle.dark`).
- **배경은 `MainActivity`의 `FeltBackground` 한 곳**에서 깔고 NavHost를 그 위에 얹는다. 각 화면은 배경을 따로 그리지 않는다.
- **본문 기본 글자 크기 18sp**: 시니어 타깃. 큰 글씨 모드(설정)는 마일스톤 6에서 추가.
- 마일스톤 1의 화면들은 `PlaceholderScreen` 기반 임시 화면이며, 이후 마일스톤에서 교체한다.
