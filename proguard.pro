-keep class com.palorder.smp.java.** { *; }
-keep class com.palorder.smp.kotlin.** { *; }

-keep class @net.minecraftforge.fml.common.Mod class *
-keepclassmembers class * {
    @net.minecraftforge.eventbus.api.SubscribeEvent *;
}

-dontshrink
-dontoptimize
-overloadaggressively
-useuniqueclassmembernames
-allowaccessmodification

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations

-printmapping build/obfuscation-mappings.txt
