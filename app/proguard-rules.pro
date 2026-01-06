# ==============================================================================
# ProGuard rules for TurnosHospi
# ==============================================================================

# ------------------------------------------------------------------------------
# GENERAL SETTINGS
# ------------------------------------------------------------------------------

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name in stack traces
-renamesourcefileattribute SourceFile

# Keep annotations (required by many libraries)
-keepattributes *Annotation*

# Keep generic signatures (required for Gson, Firebase, etc.)
-keepattributes Signature

# Keep exceptions (required for proper exception handling)
-keepattributes Exceptions

# ------------------------------------------------------------------------------
# DATA CLASSES - Firebase Realtime Database / Firestore
# These classes are serialized/deserialized by Firebase and must keep their
# field names intact for proper JSON mapping
# ------------------------------------------------------------------------------

# Main data models (MainActivity.kt)
-keepclassmembers class com.example.turnoshospi.UserShift { *; }
-keepclassmembers class com.example.turnoshospi.UserProfile { *; }
-keepclassmembers class com.example.turnoshospi.UserNotification { *; }
-keepclassmembers class com.example.turnoshospi.Plant { *; }
-keepclassmembers class com.example.turnoshospi.ShiftTime { *; }
-keepclassmembers class com.example.turnoshospi.RegisteredUser { *; }
-keepclassmembers class com.example.turnoshospi.Colleague { *; }

# Shift models (ShiftModels.kt)
-keepclassmembers class com.example.turnoshospi.ShiftChangeRequest { *; }
-keepclassmembers class com.example.turnoshospi.FavorTransaction { *; }
-keepclassmembers class com.example.turnoshospi.MyShiftDisplay { *; }
-keepclassmembers class com.example.turnoshospi.PlantShift { *; }

# Plant membership (PlantMembership.kt)
-keepclassmembers class com.example.turnoshospi.PlantMembership { *; }

# Direct chat models (DirectChatModels.kt)
-keepclassmembers class com.example.turnoshospi.DirectMessage { *; }
-keepclassmembers class com.example.turnoshospi.ChatUserSummary { *; }
-keepclassmembers class com.example.turnoshospi.ActiveChatSummary { *; }

# ShiftRulesEngine inner classes
-keepclassmembers class com.example.turnoshospi.ShiftRulesEngine$DebtEntry { *; }

# Keep data classes themselves (not just members)
-keep class com.example.turnoshospi.UserShift { *; }
-keep class com.example.turnoshospi.UserProfile { *; }
-keep class com.example.turnoshospi.UserNotification { *; }
-keep class com.example.turnoshospi.Plant { *; }
-keep class com.example.turnoshospi.ShiftTime { *; }
-keep class com.example.turnoshospi.RegisteredUser { *; }
-keep class com.example.turnoshospi.Colleague { *; }
-keep class com.example.turnoshospi.ShiftChangeRequest { *; }
-keep class com.example.turnoshospi.FavorTransaction { *; }
-keep class com.example.turnoshospi.MyShiftDisplay { *; }
-keep class com.example.turnoshospi.PlantShift { *; }
-keep class com.example.turnoshospi.PlantMembership { *; }
-keep class com.example.turnoshospi.DirectMessage { *; }
-keep class com.example.turnoshospi.ChatUserSummary { *; }
-keep class com.example.turnoshospi.ActiveChatSummary { *; }
-keep class com.example.turnoshospi.ShiftRulesEngine$DebtEntry { *; }

# ------------------------------------------------------------------------------
# ENUMS - Required for serialization
# ------------------------------------------------------------------------------

-keepclassmembers enum com.example.turnoshospi.RequestType { *; }
-keepclassmembers enum com.example.turnoshospi.RequestMode { *; }
-keepclassmembers enum com.example.turnoshospi.RequestStatus { *; }
-keepclassmembers enum com.example.turnoshospi.ShiftRulesEngine$Hardness { *; }
-keepclassmembers enum com.example.turnoshospi.ShiftRulesEngine$ShiftType { *; }

# Keep all enums (general rule)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ------------------------------------------------------------------------------
# FIREBASE
# ------------------------------------------------------------------------------

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Realtime Database
-keep class com.google.firebase.database.** { *; }
-keepclassmembers class com.google.firebase.database.** { *; }

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }

# Firebase Messaging (FCM)
-keep class com.google.firebase.messaging.** { *; }

# Firebase common
-keep class com.google.firebase.** { *; }

# Keep Firebase model classes that use reflection
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
}

# ------------------------------------------------------------------------------
# GSON
# ------------------------------------------------------------------------------

# Gson uses reflection to serialize/deserialize
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# Prevent stripping of fields annotated with @SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic type information for Gson
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ------------------------------------------------------------------------------
# JETPACK COMPOSE
# ------------------------------------------------------------------------------

# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Keep Composable functions metadata
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep classes annotated with @Stable or @Immutable
-keep @androidx.compose.runtime.Stable class *
-keep @androidx.compose.runtime.Immutable class *

# Compose compiler generates classes that should be kept
-keepclassmembers class * {
    void set*(...);
    ** get*();
}

# ------------------------------------------------------------------------------
# KOTLIN
# ------------------------------------------------------------------------------

# Keep Kotlin metadata
-keepattributes RuntimeVisibleAnnotations

# Kotlin serialization (if used)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Kotlin data class copy and component methods
-keepclassmembers class * {
    public ** component1();
    public ** component2();
    public ** component3();
    public ** component4();
    public ** component5();
    public ** copy(...);
}

# ------------------------------------------------------------------------------
# ANDROID
# ------------------------------------------------------------------------------

# Keep Android Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep R8 from removing BuildConfig
-keep class com.example.turnoshospi.BuildConfig { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ------------------------------------------------------------------------------
# DEBUGGING (remove in production if needed)
# ------------------------------------------------------------------------------

# Print more information during optimization
-verbose

# Don't warn about missing classes from optional dependencies
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
