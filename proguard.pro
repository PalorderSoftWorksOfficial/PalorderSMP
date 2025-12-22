-keep @net.minecraftforge.fml.common.Mod class * {
    <fields>;
    <methods>;
}

-keepclassmembers class * {
    @net.minecraftforge.eventbus.api.SubscribeEvent <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

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