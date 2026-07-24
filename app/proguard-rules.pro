# AetherDown ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities
-keep class com.aetherdown.app.data.local.entity.** { *; }

# Keep Moshi
-keep class com.aetherdown.app.** { @com.squareup.moshi.JsonClass *; }
-keep class com.squareup.moshi.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }

# Keep youtubedl-android (yt-dlp for Android) - uses Jackson reflection
-dontwarn com.yausername.youtubedl_android.**
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.fasterxml.** { *; }

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Fix R8 missing classes for Jackson/youtubedl-android
-dontwarn java.beans.**
-dontwarn org.w3c.dom.bootstrap.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
