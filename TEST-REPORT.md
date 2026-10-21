# InstaTrace validation report

Date: 2026-10-22

## Checks run in this environment

- `InstagramExportParser.kt` compiled successfully with the installed Kotlin/JVM compiler.
- ZIP parser smoke tests passed for:
  - nested Instagram export paths;
  - multiple `followers_N.json` files;
  - JSON `value` parsing and `href` fallback;
  - HTML follower/following files;
  - case normalization and duplicate removal;
  - rejection when one of the required follower/following files is absent.
- Snapshot comparison smoke tests passed for lost followers, gained followers, not-following-back accounts, the first-import baseline, and no-change cases.
- Project XML manifest parsed successfully; Gradle configuration and expected source/test files were checked for presence.

## Not verified here

A full Android app build, Android lint, Room/KSP code generation, Compose compilation, APK install, UI interaction, database persistence on a device, and real Instagram export compatibility were **not** tested. This runtime has no Gradle executable or Android SDK and cannot access the network to download dependencies. JUnit tests are included in the project but must be run from Android Studio/Gradle.

## Important product limitation

This app analyzes official export ZIP files locally. It does not log into Instagram or collect Instagram credentials. It cannot reveal named profile visitors, per-person profile visit counts, what each visitor viewed, or definitive historic block dates.
