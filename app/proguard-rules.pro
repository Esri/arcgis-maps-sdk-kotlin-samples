# ProGuard configuration for a release build.
-optimizationpasses 10
-allowaccessmodification
-overloadaggressively
-repackageclasses ''
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,EnclosingMethod,*Annotation*,AnnotationDefault
-keep,allowoptimization,allowaccessmodification,allowrepackage @androidx.annotation.Keep class * { *; }
-keepclassmembers,allowoptimization,allowaccessmodification,allowrepackage class * {
	@androidx.annotation.Keep *;
}
-assumevalues class **.BuildConfig {
	public static final boolean DEBUG return false;
}
-maximumremovedandroidloglevel 7
-assumenosideeffects class android.util.Log {
	public static int v(...);
	public static int d(...);
	public static int i(...);
	public static int w(...);
	public static int e(...);
	public static int wtf(...);
	public static boolean isLoggable(...);
}
-assumenosideeffects class java.io.PrintStream {
	public void print(...);
	public void println(...);
	public java.io.PrintStream printf(...);
	public java.io.PrintStream format(...);
}
-keepclasseswithmembers,allowshrinking,allowoptimization,allowaccessmodification,includedescriptorclasses class * {
	native <methods>;
}
-keepclassmembers,allowoptimization enum * {
	public static **[] values();
	public static ** valueOf(java.lang.String);
}
-keep,allowshrinking,allowoptimization,allowobfuscation,allowaccessmodification,allowrepackage class * extends java.lang.annotation.Annotation { *; }
-keep,allowoptimization,allowaccessmodification class com.esri.arcgismaps.sample.**.MainActivity extends android.app.Activity {
	public <init>();
}
-keep,allowoptimization,allowaccessmodification class com.esri.arcgismaps.sample.**.DownloadActivity extends android.app.Activity {
	public <init>();
}
