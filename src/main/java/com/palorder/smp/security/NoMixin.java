package com.palorder.smp.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;

public final class NoMixin {

    private static final Logger LOGGER = LogManager.getLogger("NoMixin");
    private static final String THIS_MOD_ID = "palordersmp_tweaked";

    private NoMixin() {}

    public static void check(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            checkAnnotations(clazz);
            checkSyntheticMethods(clazz);
            checkMixinLocals(clazz);
        }
    }

    private static void checkAnnotations(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            for (Annotation a : m.getDeclaredAnnotations()) {
                String name = a.annotationType().getName();
                if (name.startsWith("org.spongepowered.asm.mixin")) {
                    reportAndCrash("Mixin annotation detected", clazz, m.getName());
                }
            }
        }
    }

    private static void checkSyntheticMethods(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            String n = m.getName();
            if (m.isSynthetic()
                    || n.contains("$inject")
                    || n.contains("handler$")
                    || n.contains("redirect$")
                    || n.contains("modify$")
                    || n.contains("overwrite$")) {
                reportAndCrash("Injected / synthetic method detected", clazz, n);
            }
        }
    }

    private static void checkMixinLocals(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            for (Class<?> p : m.getParameterTypes()) {
                if (p.getName().startsWith("org.spongepowered.asm.mixin.injection.callback")) {
                    reportAndCrash("Mixin CallbackInfo detected", clazz, m.getName());
                }
            }
        }
    }

    private static void reportAndCrash(String reason, Class<?> targetClass, String method) {
        String offender = detectOffender();

        LOGGER.fatal("========================================");
        LOGGER.fatal("[NoMixin] SECURITY VIOLATION");
        LOGGER.fatal("This error only occurs if a mod injected Mixins into: {}", THIS_MOD_ID);
        LOGGER.fatal("Reason: {}", reason);
        LOGGER.fatal("Target class: {}", targetClass.getName());
        LOGGER.fatal("Target method: {}", method);
        LOGGER.fatal("Offending mod / source: {}", offender);
        LOGGER.fatal("Please remove the offending mod for the most stable experience.");
        LOGGER.fatal("========================================");

        throw new RuntimeException("Mixin detected in protected mod: " + THIS_MOD_ID);
    }

    /**
     * Best-effort offender detection using stacktrace + class source
     */
    private static String detectOffender() {
        try {
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                String cls = e.getClassName();
                if (cls.startsWith("org.spongepowered.asm.mixin")) {
                    Class<?> c = Class.forName(cls, false, NoMixin.class.getClassLoader());
                    URL src = c.getProtectionDomain().getCodeSource().getLocation();
                    return src != null ? src.toString() : cls;
                }
            }
        } catch (Throwable ignored) {}

        return "Unknown mod (possible coremod or agent)";
    }
}
