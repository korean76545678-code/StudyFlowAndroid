# StudyFlow Android

Kotlin + Jetpack Compose 기반의 학습 플래너 앱.

## 기능
- 할 일 추가 / 수정 / 삭제
- 완료 체크 및 날짜별 진행률 계산
- 날짜별 할 일 보기 및 날짜 이동
- DataStore에 JSON 저장하여 앱 재실행 후에도 데이터 유지
- 집중 25분 / 휴식 5분 타이머, 시작·일시정지·초기화

## Android Studio에서 실행
1. Android Studio 설치
2. `StudyFlow` 폴더를 Android Studio에서 Open
3. Gradle Sync가 끝날 때까지 기다리기
4. Android 기기에서 개발자 옵션과 USB 디버깅을 켜거나 에뮬레이터 실행
5. Run ▶ 버튼으로 앱 실행

## APK 만들기
Android Studio 메뉴에서 **Build > Build APK(s)** 를 선택.
APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

날짜 입력은 `YYYY-MM-DD` 형식입니다. 앱 데이터는 기기 내부에 저장되며 별도 계정/서버 연결은 없습니다.


## Android Studio 없이 APK 만들기 (GitHub Actions)
이 프로젝트에는 `.github/workflows/build-apk.yml` 자동 빌드가 포함되어 있습니다.

1. GitHub에 새 저장소를 만들고 이 프로젝트 파일을 업로드합니다.
2. 저장소의 **Actions** 탭으로 이동합니다.
3. `Build StudyFlow APK` 워크플로를 선택하고 **Run workflow**를 누릅니다.
4. 빌드가 완료되면 아래쪽 **Artifacts**의 `StudyFlow-debug-apk`를 다운로드합니다.
5. ZIP 압축을 풀고 `app-debug.apk`를 Android 휴대폰으로 옮겨 설치합니다.
   - 처음 설치할 때 Android에서 브라우저/파일 앱의 '알 수 없는 앱 설치' 권한을 허용해야 할 수 있습니다.
   - APK는 디버그 빌드이며, 본인 기기에 설치할 때만 사용하세요.

참고: GitHub 계정과 저장소 업로드가 필요합니다. 이 환경에는 Android SDK가 없어 APK를 직접 컴파일할 수 없으며, GitHub Actions가 클라우드에서 빌드합니다.
