# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Install debug build to connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean build
```

## Architecture Overview

VRApp is an Android application for Value Rebalancing (VR) stock investment strategy. It follows MVVM architecture with Jetpack Compose UI.

### Key Layers

- **UI Layer** (`ui/`): Jetpack Compose screens using Material Design 3
  - `VRApp.kt`: Main navigation host with bottom navigation (Home, Chart, Settings)
  - `MainScreen.kt`: Stock list and asset summary
  - `DetailScreen.kt`: Individual stock VR details and transactions
  - `ChartScreen.kt`: MPAndroidChart visualizations
  - `SettingsScreen.kt`: Backup/restore functionality

- **ViewModel Layer** (`viewmodel/`): `StockViewModel` manages all business logic
  - Handles stock CRUD, VR calculations, price fetching, backup/restore
  - Uses StateFlow for reactive UI updates
  - Manages exchange rate conversion (KRW, USD, JPY)

- **Data Layer** (`data/`):
  - `AppDatabase.kt`: Room database with StockDao (version 6, uses auto-migrations)
  - `StockRepository.kt`: Repository pattern wrapping StockDao
  - `StockPriceService.kt`: Stock price fetching via Jsoup (KR markets) and Yahoo API (US/JP)

- **Logic Layer** (`logic/`):
  - `VRCalculator.kt`: Core VR algorithm as a singleton object
    - Calculates VR bands (low/high valuation)
    - Determines buy/sell/hold orders
    - Generates price tables with market-specific tick sizes (KOSPI, KOSDAQ, US, Japan)

### Data Models (`model/`)

- `Stock`: Core entity with VR variables (V, G, Pool), quantity, price, currency
- `TransactionHistory`: Buy/sell/deposit/withdraw/recalc records
- `DailyAssetHistory`: Daily portfolio snapshots
- `StockHistory`: VR state snapshots per stock
- `BackupData`: JSON serialization wrapper for export/import

### VR Formula Reference

- **New V calculation**: `V_new = V_old + (Pool / G) + poolDeposit`
- **Low Valuation**: `V * (1 - G/100)`
- **High Valuation**: `V * (1 + G/100)`
- **Buy signal**: when current valuation <= Low Valuation
- **Sell signal**: when current valuation >= High Valuation

### Market Tick Size Logic

`VRCalculator` adjusts prices to valid tick sizes based on market type:
- Markets detected by ticker suffix (`.KS` = KOSPI, `.KQ` = KOSDAQ, `.T` = Japan) or currency
- Each market has different tick size rules based on price ranges
