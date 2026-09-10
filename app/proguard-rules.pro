# NovaMotion ProGuard Rules

# ─── Keep core data models (used by JSON serializer) ────────────────────────
-keep class com.novamotion.core.model.** { *; }
-keep class com.novamotion.core.project.** { *; }
-keep class com.novamotion.core.shape.** { *; }
-keep class com.novamotion.core.text.** { *; }
-keep class com.novamotion.core.export.ExportConfiguration { *; }

# ─── Keep enums (used by valueOf() in JSON deserialization) ──────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Keep OpenGL shader / renderer classes (accessed from GLSurfaceView) ─────
-keep class com.novamotion.core.render.** { *; }

# ─── Keep Compose-related classes ────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Keep Media3 / ExoPlayer ─────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ─── Keep Kotlin coroutines ──────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ─── Keep Kotlinx serialization (if added later) ────────────────────────────
-keep @kotlinx.serialization.Serializable class * { *; }

# ─── Keep ViewModel and SavedStateHandle ─────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(androidx.lifecycle.SavedStateHandle);
    <init>();
}

# ─── Keep Native C++ JNI methods (libnovamotion.so) ─────────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.novamotion.core.nativedrive.** { *; }

# ─── Strip debug logging in release ──────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
