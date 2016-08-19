
package com.tangxiaolv.telegramgallery;

import android.app.Application;
import android.os.Handler;

public class Gallery {

    public volatile static Application applicationContext;
    public volatile static Handler applicationHandler;

    public static void init(Application application) {
        if (applicationContext == null) {
            applicationContext = application;
            applicationHandler = new Handler(application.getMainLooper());
        }
    }
}
