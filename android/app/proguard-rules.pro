# Proguard rules for StatusBar Manager
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
