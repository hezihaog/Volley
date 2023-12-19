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

# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共库的成员
-dontskipnonpubliclibraryclassmembers
# 混淆时不做预校验
-dontpreverify
# 忽略警告
-ignorewarnings
# 保留代码行号，方便异常信息的追踪
-keepattributes SourceFile,LineNumberTable
# appcompat库不做混淆
-keep class androidx.appcompat.**
#保留 AndroidManifest.xml 文件：防止删除 AndroidManifest.xml 文件中定义的组件
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

#保留序列化，例如 Serializable 接口
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#带有Context、View、AttributeSet类型参数的初始化方法
-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

#保留资源R类
-keep class **.R$* {*;}

#避免回调函数 onXXEvent 混淆
-keepclassmembers class * {
    void *(**On*Event);
    void *(**On*Listener);
    void *(**on*Changed);
}

#业务实体不做混淆，避免gson解析错误
-dontwarn com.zh.android.volley.model.**
-keep class com.zh.android.volley.model.** { *;}

#Rxjava、RxAndroid，官方ReadMe文档中说明无需特殊配置
-dontwarn java.util.concurrent.Flow*
#okhttp3、okio、retrofit，jar包中已包含相关proguard规则，无需配置
#其他一些配置