# Enable isMinifyEnabled in app/build.gradle.kts after testing; these rules are ready for it.
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.errorprone.annotations.**
-keep class com.customapplock.studio.data.** { *; }
