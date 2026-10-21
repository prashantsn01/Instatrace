# InstaTrace Android — privacy-first follower history

This is an Android Studio project starter for a local Instagram export analyzer.

## What it does
- Imports Instagram's downloaded information ZIP on-device (no upload).
- Extracts follower and following usernames from common JSON/HTML export formats.
- Stores each imported snapshot in a local Room database.
- Compares the newest two snapshots to show lost followers and new followers.
- Shows accounts you follow who are not in your follower list.
- Shows snapshot history and the date each change was first detected.

## What Instagram does not expose to third-party apps
- A complete named list of profile visitors.
- Per-person profile visit counts.
- Which photos/posts/profile sections each visitor viewed.
- A definitive list of who blocked you, or exact historical block dates.

Those features are not simulated. Instagram's aggregate professional-account insights may be available through Meta's official APIs for eligible accounts and permissions, but are not included as a working API integration in this project.

## Security
This project does not request or collect your Instagram password, does not scrape Instagram, and does not automate Instagram login. Import your own export downloaded from Instagram's Accounts Center. Data is stored locally on the device. Android backup is disabled.

## Build
1. Install Android Studio (stable), JDK 17, Android SDK 35.
2. Open this folder in Android Studio.
3. Let Gradle sync complete.
4. Run the `app` configuration on an emulator/device.

## Validation status
- The standalone Kotlin ZIP parser compiled successfully with `kotlinc` in the available environment.
- Parser smoke checks passed for nested ZIP paths, split follower files, username normalization/deduplication, and rejection of an incomplete export.
- JUnit tests are included under `app/src/test` for JSON, HTML, split follower parts, normalization, and missing files.
- A full Android/Gradle build and emulator/device test could not be run here: this environment has no Gradle installation, Android SDK, or network access to download build dependencies. Run **Build > Make Project** and the unit tests in Android Studio before treating the APK as verified.

## How to export
Instagram → Settings and activity → Accounts Center → Your information and permissions → Export your information. Request Followers and Following data, download the ZIP, then import it in InstaTrace. The first import is your baseline. Import a later export to compare changes.

## Notes
Export file names can vary by language, export type, and Instagram version. This parser supports common followers_1.json / followers_1.html and following.json / following.html structures. Always verify results against the export.

## Build an installable APK with GitHub Actions

This repository includes `.github/workflows/android-apk.yml`. After uploading the project to a GitHub repository, open **Actions → Build InstaTrace APK → Run workflow** (or push to `main`/`master`). Wait for the workflow to finish successfully, open the completed run, and download the **InstaTrace-debug-apk** artifact. Extract it to get `app-debug.apk`, then transfer it to your Android phone and open it to install. Android may ask you to allow installs from that source.

This workflow is provided as a build route; an APK has **not** been generated or verified in this environment. Only install an APK from a build you trust.
