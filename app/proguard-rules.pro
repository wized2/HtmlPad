# HtmlPad R8 rules (used together with AGP's proguard-android-optimize.txt).
#
# The app has no reflection, no JNI and no @JavascriptInterface, and every library it
# uses (Compose, AndroidX) ships its own consumer rules, so nothing has to be kept by hand.
# If you ever add a WebView @JavascriptInterface, reflection or JSON model classes,
# add a narrow -keep rule for just those classes here.

# Readable crash stack traces after obfuscation (map them with the release's mapping.txt).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Smaller output: move all obfuscated classes into one flat package.
-repackageclasses ''

# Strip verbose, debug and info logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
