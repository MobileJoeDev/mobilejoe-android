# Keep kotlinx.serialization generated serializers for Compose Navigation
# type-safe routes defined in this library.
# https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro
-if @kotlinx.serialization.Serializable class dev.mobilejoe.ui.compose.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class dev.mobilejoe.ui.compose.** {
    static **$* *;
}
-keepclassmembers class <1>$* {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class dev.mobilejoe.ui.compose.**
-keepclassmembers class dev.mobilejoe.ui.compose.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.mobilejoe.ui.compose.**$$serializer { *; }
