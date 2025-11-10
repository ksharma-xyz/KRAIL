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

#Need this to keep serializable members as is
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

#This will print mappings - very useful for troubleshooting.
-printseeds ./build/seeds.txt
-printusage ./build/unused.txt
-printmapping ./build/mapping.txt

#Some recommended settings for running with Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

-keep public enum * {}
-keep public interface * {}

-keepattributes *Annotation*

# Remove test dependencies from release builds
# These should never be included in production code
-dontwarn org.robolectric.**
-dontwarn com.github.takahirom.roborazzi.**
-dontwarn sergio.sastre.composable.preview.scanner.**
-dontwarn com.google.auto.service.AutoService
-dontwarn java.awt.**

-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}


-keepattributes *Annotation*,Signature
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepclasseswithmembers class * {
    @retrofit.* <methods>;
}

# For Ktor client in Android
# https://youtrack.jetbrains.com/issue/KTOR-5528
-dontwarn org.slf4j.**
