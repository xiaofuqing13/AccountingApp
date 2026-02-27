# Repository Guidelines

## Project Structure & Module Organization
- Root Gradle config is in `settings.gradle.kts` and includes only `:app`.
- Main Android code is under `app/src/main/java/com/loveapp/accountbook`, organized by `data/`, `ui/`, and `util/`.
- UI resources live in `app/src/main/res` (`layout/`, `drawable/`, `values/`, `navigation/`).
- Project documentation is stored in `docs/`.
- Generated or local artifacts (`build/`, `app/build/`, `.gradle/`) are not source of truth and should not be edited directly.
- `app/app/` exists as a legacy nested project; avoid changing it unless doing an explicit migration task.

## Build, Test, and Development Commands
- `.\gradlew.bat :app:assembleDebug`: build debug APK.
- `.\gradlew.bat :app:installDebug`: install debug build on connected device/emulator.
- `.\gradlew.bat :app:lint`: run Android lint checks.
- `.\gradlew.bat :app:testDebugUnitTest`: run JVM unit tests (when tests are present).
- `.\gradlew.bat clean`: clean local build outputs.
- Prefer the root wrapper (`.\gradlew.bat`) for consistent Gradle/AGP behavior.

## Coding Style & Naming Conventions
- Kotlin: 4-space indentation, no tabs.
- Class names use `PascalCase` (example: `AccountViewModel`); functions/properties use `camelCase` (example: `loadAccounts`).
- Package names stay lowercase and dot-separated.
- XML/layout/resource files use `snake_case` (example: `fragment_account_list.xml`).
- View IDs should be semantic and prefixed (`tv_`, `btn_`, `rv_`).
- Keep architecture consistent with MVVM + Repository; avoid adding unused classes or dead resources.

## Testing Guidelines
- No committed test sources currently exist under `app/src/test` or `app/src/androidTest`.
- Add unit tests in `app/src/test/kotlin` for business logic and repository utilities.
- Add instrumentation tests in `app/src/androidTest/kotlin` for Android/UI behavior.
- Minimum pre-PR validation: pass `:app:assembleDebug` and `:app:lint`, and document manual verification for affected screens.

## Commit & Pull Request Guidelines
- Follow existing history style: short imperative Chinese commit subjects, one change focus per commit.
- Example style: `修复废弃API...`, `添加旧版备份导入功能`.
- PRs should include:
  - change summary and impacted modules/files,
  - validation evidence (commands run + result),
  - screenshots for UI/layout updates,
  - linked issue/task and migration notes if permissions/storage behavior changed.

## Security & Configuration Tips
- Do not commit `local.properties`, signing files, or personal exported data.
- When touching Excel import/export or storage code, explicitly verify permission flows on Android 10+ and Android 11+.
