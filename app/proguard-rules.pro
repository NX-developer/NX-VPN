# Keep WireGuard tunnel classes (JNI / native backend).
-keep class com.wireguard.** { *; }
-dontwarn com.wireguard.**

# kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.nxvpn.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.nxvpn.app.**$$serializer { *; }
-keep class com.nxvpn.app.data.model.** { *; }
