# BuyLog

카드·은행·간편결제 알림을 바탕으로 실제 소비를 정확하게 관리하는 개인용 Android 앱입니다.

현재는 **Release 0.2**까지 구현되어 있습니다. 서버 없이 기기 내부에 데이터를 저장하며, 이후 기능도 릴리즈 단위로 점진적으로 추가합니다.

## 현재 제공 기능

- 결제 수동 등록, 수정, 삭제
- 홈 상단의 독립된 결제수단 관리
- 선택 상태가 명확한 결제수단 선택 카드
- 이번 달 결제금액 요약
- 전체 결제 목록과 최근 결제 확인
- Room 기반 로컬 데이터 보존
- 기기 시간대를 반영한 월간 금액 계산
- NotificationListenerService 기반 알림 원문 수집
- 알림 접근 권한과 수집 시작·중지 제어
- 패키지명·앱 이름·제목·원문·수신 시각을 확인하는 알림 로그

결제 알림 파싱, 자동 등록, 중복 판정, 카드 실적 관리는 아직 구현하지 않았습니다.

Release 0.1에서는 결제를 개인/테스트/기타로 나누지 않습니다. 모든 결제는 동일하게 월간 금액에 포함되며, 테스트 결제처럼 실제 소비에서 제외할 항목은 Release 0.5의 태그 기능으로 처리할 예정입니다.

## 기본 사용 흐름

1. 홈 상단의 `결제수단 관리`에서 카드, 계좌이체, 현금 등의 결제수단을 추가합니다.
2. 홈 우측 하단의 `+` 버튼을 눌러 결제를 등록합니다.
3. 결제수단 선택 카드에서 현재 선택값을 확인합니다.
4. 다른 결제수단을 사용하려면 선택 카드의 `변경 ▼`를 누릅니다.
5. 결제 항목을 누르거나 `수정` 버튼을 이용해 내용을 변경합니다.

## 알림 수집 테스트

1. 홈 상단의 `알림 로그`를 엽니다.
2. `권한 설정`에서 결제 기록 앱의 알림 접근 권한을 켭니다.
3. 앱으로 돌아와 `원문 알림 수집`을 켭니다.
4. 금융 앱 Push 또는 문자 앱 알림을 받은 후 `알림 로그`에서 패키지명·제목·원문을 확인합니다.

수집은 기본적으로 꺼져 있으며, 켠 동안 받은 알림 원문만 이 기기에 저장합니다. 아직 어떤 알림도 결제로 자동 등록하지 않습니다.

## 기술 구성

- Kotlin
- Jetpack Compose / Material 3
- Room
- ViewModel + Repository
- Kotlin Coroutines / Flow
- KSP
- Gradle 9.5 / Android Gradle Plugin 9.3.2
- Min SDK 26 / Target SDK 37

## 프로젝트 구조

```text
app/src/main/java/com/example/buylog/
├── data/       Room 엔티티, DAO, 데이터베이스, Repository
├── domain/     월 경계 및 결제 요약 계산
├── ui/         Compose 화면과 테마
├── MainActivity.kt
└── MainViewModel.kt

app/src/test/   핵심 계산 단위 테스트
app/schemas/    Room 스키마 이력
releases/       릴리즈별 범위와 결과 문서
```

## 개발 환경

- JDK 17
- Android SDK API 37

Android Studio에서 프로젝트 루트를 열거나 PowerShell에서 아래 명령을 실행합니다.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

생성되는 디버그 APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 핵심 데이터 규칙

- 금액은 부동소수점 대신 원 단위 `Long`으로 저장합니다.
- Release 0.1에서는 모든 결제를 동일하게 집계합니다.
- 태그와 소비 집계 제외, 달력 월·월급 주기 전환 기능은 Release 0.5에서 추가합니다.
- 월간 집계는 기기의 현재 시간대를 기준으로 합니다.
- 금융 데이터는 외부 서버로 전송하지 않습니다.

## 릴리즈 문서

- [릴리즈 목록](releases/README.md)
- [Release 0.1 구현 정리](releases/0.1.md)
- [Release 0.2 알림 수집 정리](releases/0.2.md)
- [Release 0.5 월별·월급 주기 및 태그 계획](releases/0.5.md)
- [Release 0.6 카드별 기간·실적 계획](releases/0.6.md)

향후 개발 시에는 루트의 [AGENTS.md](AGENTS.md)에 정의된 범위와 검증 규칙을 따릅니다.
