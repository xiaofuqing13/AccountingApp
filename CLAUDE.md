# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Clean and rebuild
./gradlew clean assembleDebug

# Check compilation without producing APK
./gradlew compileDebugKotlin

# Install debug APK to connected device
./gradlew installDebug
```

No test framework is configured — there are no unit or instrumentation tests in this project.

## Architecture

This is an Android Kotlin app ("我们的小账本") using MVVM with Jetpack Navigation. Package: `com.loveapp.accountbook`.

### Data Layer — Excel as Database

The app uses Apache POI to read/write a single `.xlsx` file (`我们的小账本.xlsx`) instead of Room/SQLite. `ExcelRepository` is the sole data access class, managing 3 sheets: 记账 (5 columns), 日记 (6 columns), 会议纪要 (9 columns). The file lives in `getExternalFilesDir()/DataManager/`. All IO runs on `Dispatchers.IO` via coroutines.

Each ViewModel instantiates its own `ExcelRepository(context)` — there is no dependency injection or singleton pattern. Every read operation opens the file, parses it, and closes it. Every write operation opens, appends a row, saves, and closes.

**Limitation**: ExcelRepository has `add*`, `get*`, `exportToPath`, `importFromPath`, and `getExcelFilePath` methods — no update or delete operations exist. The SettingsFragment export/import UI is a placeholder (toast only, no file picker), though the Repository-level implementation is functional.

### Navigation

`SplashActivity` is the launcher entry point, then navigates to `MainActivity`. `MainActivity` hosts a `NavHostFragment` + `BottomNavigationView` with 5 tabs (home, account, diary, meeting, settings). The nav graph is at `res/navigation/nav_graph.xml` with 10 fragment destinations. Bottom nav hides automatically when navigating to secondary fragments (add/edit/stats pages). `LoveLetterFragment` is accessible via Settings → 意见反馈 navigation, displaying a love letter and promise chips.

### Module Pattern

Each feature module (account, diary, meeting) follows the same pattern:
- `*ListFragment` — displays RecyclerView list, FAB to add new entry
- `*AddFragment` — form for creating new entries, uses `DraftManager` for auto-save
- `*ViewModel` — extends `AndroidViewModel`, holds `LiveData`, calls `ExcelRepository`
- `*Adapter` — RecyclerView adapter in `ui/adapter/`

Account module fragments share a single ViewModel via `activityViewModels()`. Diary and Meeting modules use the same sharing pattern. HomeFragment uses fragment-scoped `viewModels()`.

### Draft Auto-Save

`DraftManager` (SharedPreferences-based) binds `TextWatcher` to `EditText` fields with a 2-second debounce. Drafts restore on fragment entry and clear on successful save. Draft keys are defined as constants in `DraftManager`.

### Easter Egg System

`EasterEggManager` manages 25+ hidden interactions across the app. Triggers include: tap counts, special amounts (¥520, ¥1314, ¥777), keyword searches (爱/love/想你/永远/开心), save-success surprises (1/3 random chance), promise chip clicks, and more. The `loveWords` list contains 25 random love messages; `moodWords` has 20 sweet phrases. Most easter eggs display via `dialog_love_popup.xml` AlertDialog; diary mood emoji click uses Toast. When modifying UI interactions, be aware that click listeners may have easter egg logic attached.

## Key Conventions

- Language: Kotlin 1.9.22, AGP 8.3.2, JVM target 11, compileSdk/targetSdk 34, minSdk 26
- View system: XML layouts with `findViewById` (no Compose). ViewBinding is enabled in gradle but unused in code
- UI label strings are in `res/values/strings.xml` (Chinese). Some data strings (category names, weather, mood) are hardcoded in Kotlin code
- Color theme: pink (#E8729A primary), green (#66BB6A income), purple (#D4A0E8 accent)
- Date format used throughout: `yyyy-MM-dd` (see `DateUtils`)
- The "together since" date is hardcoded as 2025-02-14 in `DateUtils`
- Gradle JDK path is set in `gradle.properties` to `C:/Program Files/Android/Android Studio/jbr`
