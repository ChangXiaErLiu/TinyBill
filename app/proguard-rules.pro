# ProGuard rules for TinyBill
# ============================

# Room entities and DAOs — needed for runtime SQL generation
-keep class com.tinybill.data.entity.* { *; }
-keep class com.tinybill.data.database.* { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Gson serialization — used for BackupManager and ExportManager
-keepclassmembers class com.tinybill.data.entity.** { *; }
-keepclassmembers class com.tinybill.util.BackupData { *; }
-keepclassmembers class com.tinybill.util.BackupMeta { *; }

# Keep enum classes (used in AccountType, TransactionType etc.)
-keepclassmembers enum * { *; }

# Keep service and receiver components
-keep class com.tinybill.service.** { *; }
-keep class com.tinybill.receiver.** { *; }
-keep class com.tinybill.widget.** { *; }

# Keep crash reporter (invoked via reflection in GlobalExceptionHandler)
-keep class com.tinybill.util.CrashReporter { *; }
-keep class com.tinybill.util.GlobalExceptionHandler { *; }

# Keep Kotlin Coroutines internals
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Koin DI — keep injectable classes
-keep class org.koin.** { *; }