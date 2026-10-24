# Custom AppLock Studio

100% Kotlin Android app lock. Jetpack Compose (black & white UI + bottom navigation), Hilt, Room, MVVM.

## Build on GitHub (no setup needed)
1. Create a repo and push this whole folder to `main`.
2. Open **Actions > Android Build**. It runs on every push.
3. When it is green, download **CustomAppLockStudio-release-apk** and **-aab** from the run's Artifacts.

Optional Play Store signing: add repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Without them the APK is signed with the debug key (installable, not uploadable to Play).

## Before publishing
- Replace the **test** AdMob IDs in `app/src/main/res/values/strings.xml`.
- To open in Android Studio: File > Open the folder (Gradle 8.9 is picked up from `gradle-wrapper.properties`).

## First run on a phone
Grant Accessibility + Draw-over-apps. If Accessibility is greyed out on a sideloaded APK: App info > ⋮ > Allow restricted settings.
