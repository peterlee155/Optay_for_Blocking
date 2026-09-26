# Add project specific ProGuard rules here.

# Preserve line numbers for readable production stack traces
-keepattributes SourceFile,LineNumberTable

# Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public void <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# RevenueCat Purchases SDK
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**

# Domain Models
-keep class com.example.domain.model.** { *; }
-keep class com.example.billing.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
