# Keep kotlinx.serialization generated serializers for this library's models.
# https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro
-if @kotlinx.serialization.Serializable class dev.mobilejoe.api.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class dev.mobilejoe.api.** {
    static **$* *;
}
-keepclassmembers class <1>$* {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class dev.mobilejoe.api.**
-keepclassmembers class dev.mobilejoe.api.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.mobilejoe.api.**$$serializer { *; }

# Custom KSerializer implementations must keep their no-arg constructor.
-keepclassmembers class dev.mobilejoe.api.** implements kotlinx.serialization.KSerializer {
    <init>(...);
}

# Ktor client engine + content negotiation resolve implementations reflectively.
-keep class io.ktor.client.engine.android.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.debug.**
