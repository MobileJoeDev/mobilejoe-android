# Keep kotlinx.serialization generated serializers for this app's own
# Compose Navigation type-safe routes (Home, Sample, SampleItem).
# https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro
-if @kotlinx.serialization.Serializable class dev.mobilejoe.samplejoe.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class dev.mobilejoe.samplejoe.** {
    static **$* *;
}
-keepclassmembers class <1>$* {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class dev.mobilejoe.samplejoe.**
-keepclassmembers class dev.mobilejoe.samplejoe.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.mobilejoe.samplejoe.**$$serializer { *; }
-keepclassmembers,allowoptimization enum dev.mobilejoe.samplejoe.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepnames enum dev.mobilejoe.samplejoe.**