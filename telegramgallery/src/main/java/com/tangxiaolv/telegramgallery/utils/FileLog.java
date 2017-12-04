package com.tangxiaolv.telegramgallery.utils;

import android.util.Log;

public class FileLog {

    private static volatile FileLog Instance = null;

    public static FileLog getInstance() {
        FileLog localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLog.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLog();
                }
            }
        }
        return localInstance;
    }

    public static void e(final String message, final Throwable exception) {
        Log.e("gallery", message, exception);
    }

    public static void e(final String message) {
        Log.e("gallery", message);
    }

    public static void e(final Throwable e) {
        e.printStackTrace();
    }

    public static void d(final String message) {
        Log.d("gallery", message);
    }

    public static void w(final String message) {
        Log.w("gallery", message);
    }
}
