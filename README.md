# VRApp (Value Rebalancing App)

VRApp은 주식 투자의 **Value Rebalancing (VR)** 전략을 쉽고 간편하게 수행할 수 있도록 돕는 안드로이드 애플리케이션입니다.
설정된 VR 값과 Gradient(G), Pool 정보를 바탕으로 최적의 매수/매도 타이밍과 수량을 계산해 줍니다.

## 주요 기능 (Key Features)

*   **VR 계산 및 매매 가이드 (VR Calculation):**
    *   현재 주가와 VR, Pool, G 값을 기반으로 매수/매도/홀딩 여부를 판단합니다.
    *   VR 밴드(Low/High Valuation)를 계산하여 시각적으로 보여줍니다.
    *   수량별 매수/매도 추천 가격표를 제공합니다.
    *   KOSPI, KOSDAQ, 미국(US), 일본(Japan) 등 다양한 시장의 호가 단위(Tick Size)를 자동으로 반영하여 정밀한 가격을 산출합니다.

*   **자산 관리 (Asset Management):**
    *   종목별 VR 진행 상황을 저장하고 관리합니다.
    *   Pool 입출금 및 VR 값 재조정을 지원합니다.

*   **히스토리 및 차트 (History & Charts):**
    *   일별 자산 변동 내역(Daily Asset History)을 기록합니다.
    *   MPAndroidChart를 활용하여 자산 및 주가 변동 추이를 그래프로 시각화합니다.

*   **데이터 백업 (Backup):**
    *   투자 데이터를 백업하고 복원하는 기능을 제공합니다.

## 기술 스택 (Tech Stack)

이 프로젝트는 최신 안드로이드 개발 트렌드와 라이브러리를 적극 활용하여 개발되었습니다.

*   **Language:** [Kotlin](https://kotlinlang.org/) (100%)
*   **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3) - 선언형 UI
*   **Architecture:** MVVM (Model-View-ViewModel) 패턴
*   **Database:** [Room](https://developer.android.com/training/data-storage/room) - 로컬 데이터베이스
*   **Concurrency:** [Coroutines](https://github.com/Kotlin/kotlinx.coroutines) & Flow - 비동기 처리
*   **Navigation:** Jetpack Navigation Compose
*   **Network/Parsing:** [Jsoup](https://jsoup.org/) - 웹 데이터 파싱 (주가 정보 등)
*   **Visualization:** [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - 차트 구현
*   **DI/Other:** KSP, Gson

## 개발 방식 (Development Methodology)

본 프로젝트는 다음과 같은 개발 원칙과 방식을 따릅니다.

1.  **MVVM 아키텍처 (MVVM Architecture):**
    *   UI(`ui` 패키지)와 비즈니스 로직(`logic`, `viewmodel`)을 명확히 분리하여 유지보수성을 높였습니다.
    *   `ViewModel`은 UI의 상태(State)를 관리하고, `Repository`(Data Layer)로부터 데이터를 받아 UI에 전달합니다.

2.  **선언형 UI (Declarative UI):**
    *   Jetpack Compose를 사용하여 직관적이고 재사용 가능한 UI 컴포넌트를 구축했습니다.
    *   XML 레이아웃 없이 100% Kotlin 코드로 UI를 구성합니다.

3.  **데이터 기반 로직 (Data-Driven Logic):**
    *   `VRCalculator` 객체(`logic` 패키지)에 핵심 알고리즘을 캡슐화하여 순수 함수 형태로 테스트 가능하게 설계했습니다.
    *   Room Database를 통해 모든 투자 데이터를 로컬에 안전하게 저장합니다.

4.  **시장별 호가 처리:**
    *   `VRCalculator` 내부에서 Ticker나 통화(Currency) 정보를 바탕으로 KOSPI, KOSDAQ, US, JAPAN 시장을 구분하고, 각 시장의 가격 정책(Tick Size)에 맞춰 매매가를 보정합니다.

## 빌드 및 실행 (Build & Run)

이 프로젝트는 Gradle을 사용합니다. Android Studio 최신 버전(Iguana 이상 권장)에서 열어 빌드할 수 있습니다.

```bash
# 프로젝트 클론
git clone [repository-url]

# 프로젝트 디렉토리로 이동
cd vrapp

# 빌드 (Mac/Linux)
./gradlew build

# 실행 (연결된 기기 또는 에뮬레이터)
./gradlew installDebug
```

## 라이선스 (License)

이 프로젝트는 개인 학습 및 사용을 목적으로 개발되었습니다.
