# Getting the installable APK

I cannot truthfully attach a built APK yet: this environment does not contain the Android SDK or Gradle runtime needed to compile the Android application. The source ZIP includes a GitHub Actions workflow that builds the debug APK and runs the unit tests on GitHub's Android-compatible runner.

1. Create a GitHub repository and upload the extracted InstaTrace project contents (including the hidden `.github` folder).
2. Open the repository's **Actions** tab and enable Actions if asked.
3. Choose **Build InstaTrace APK** → **Run workflow**.
4. Wait for a green successful run. If it fails, open the build step logs; a failed build means there is no installable APK yet.
5. Download the `InstaTrace-debug-apk` artifact from the completed run and extract `app-debug.apk`.
6. Transfer `app-debug.apk` to your Android device and tap it to install. You may need to permit installation from the file manager/browser used to open it.

This is a debug APK for testing, not a Play Store release. It does not provide secret Instagram profile-visitor information and does not ask for your Instagram password.
