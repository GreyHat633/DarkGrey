package com.greyhat.dark_grey.util;

public final class SplashRecursionGuard {

    private static final ThreadLocal<Boolean> IN_SPLASH = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private SplashRecursionGuard() {}

    public static boolean isProcessingSplash() {
        return IN_SPLASH.get();
    }

    public static void setProcessingSplash(boolean value) {
        if (value) {
            IN_SPLASH.set(true);
        } else {
            IN_SPLASH.set(false);
        }
    }
}
