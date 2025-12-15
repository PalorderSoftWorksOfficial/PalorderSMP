-keep class com.palorder.smp.java.** { *; }
-keep class com.palorder.smp.kotlin.** { *; }

-keep class net.minecraftforge.fml.common.Mod { *; }
-keep class net.minecraftforge.eventbus.api.SubscribeEvent { *; }

-keepclassmembers class * {
    void *(...);
}

-dontshrink
-dontoptimize
-overloadaggressively
-useuniqueclassmembernames
-allowaccessmodification

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations

-dontwarn **

-printmapping build/obfuscation-mappings.txt
