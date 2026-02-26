-keep @net.minecraftforge.fml.common.Mod class * {
    <init>(...);
}
-keep com.palorder.smp.java.PalorderSMPMainJava.* { *; }

-keepclassmembers class * {
    @net.minecraftforge.eventbus.api.SubscribeEvent <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

-keepclassmembers class com.palorder.smp.security.NoMixin {
    public static void check(...);
}

-keep class com.palorder.smp.java.PrimedTntExtendedAPI { *; }

-keep class com.palorder.smp.kotlin.PrimedTntExtendedAPI { *; }

-keep class org.spongepowered.asm.mixin.** { *; }

-keep class com.palorder.smp.java.mixins.** { *; }

-allowaccessmodification
-useuniqueclassmembernames
-overloadaggressively
-repackageclasses ''
-flattenpackagehierarchy ''

-dontusemixedcaseclassnames
-renamesourcefileattribute SourceFile

-keepattributes Exceptions,Signature,InnerClasses,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations

-optimizationpasses 5
-mergeinterfacesaggressively

-dontwarn **

-printmapping build/obfuscation-mappings.txt
