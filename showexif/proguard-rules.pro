# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/adt-bundle-mac-x86_64-20140702/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn org.xmlpull.v1.**
-dontwarn com.caverock.androidsvg.**
#!! -keep class org.xmlpull.** { *; }

###############
# I use proguard only to remove unused stuff and to keep the app small.
# I donot want to obfuscate (rename packages, classes, methods, ...) since this is open source
-dontobfuscate
#!! -dontoptimize
-keepnames class ** { *; }
-keepnames interface ** { *; }
-keepnames enum ** { *; }


### j2s2 specific from https://github.com/loopj/proguard-gradle-example/blob/master/proguard.txt

# Include java runtime classes
-libraryjars  <java.home>/lib/rt.jar

# Output a source map file
-printmapping proguard.map

# j2se main entry method
-keep public class de.k3b.ShowExif {
    public static void main(java.lang.String[]);
}
