# AetherDown ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class com.aetherdown.app.AetherApp { *; }
-keep class com.aetherdown.app.MainActivity { *; }
-keep class com.aetherdown.app.** { @dagger.hilt.android.AndroidEntryPoint *; }
-keep class com.aetherdown.app.** { @dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper *; }
-keep class com.aetherdown.app.presentation.** { @dagger.hilt.android.lifecycle.HiltViewModel *; }
-keep class com.aetherdown.app.di.** { *; }
-keep class * extends com.aetherdown.app.AetherApp { *; }
-keep class * extends com.aetherdown.app.MainActivity { *; }

# Keep Room entities
-keep class com.aetherdown.app.data.local.entity.** { *; }

# Keep Moshi
-keep class com.aetherdown.app.** { @com.squareup.moshi.JsonClass *; }
-keep class com.squareup.moshi.** { *; }

# Keep attributes for reflection, annotations, and generics
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Keep Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }

# Keep youtubedl-android, FFmpeg & Aria2c
-dontwarn com.yausername.youtubedl_android.**
-dontwarn com.yausername.ffmpeg.**
-dontwarn com.yausername.aria2c.**
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.aria2c.** { *; }

# Keep Jackson & Reflection mapping
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}

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
