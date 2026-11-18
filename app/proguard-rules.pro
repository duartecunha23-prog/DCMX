# ProGuard / R8 rules for Google Play Store App Bundle (AAB)

# Preserve line numbers for crash stack traces in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** { *** Companion; }
-keepclasseswithmembers class com.example.** { kotlinx.serialization.KSerializer serializer(...); }

# Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# LiteRT-LM native JNI bridge
-keep class com.google.ai.edge.litertlm.** { *; }

# ML Kit Vision
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
