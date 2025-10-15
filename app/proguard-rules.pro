# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.selvamoneymanager.db.** { *; }

# Kotlin coroutines & metadata
-keepclassmembers class kotlinx.** { *; }
-dontwarn kotlinx.coroutines.**
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Models used via reflection (if any CSV/JSON parsers)
-keepclassmembers class com.example.selvamoneymanager.** { *; }
