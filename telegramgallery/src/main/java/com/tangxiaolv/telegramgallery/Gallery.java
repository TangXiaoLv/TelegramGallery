
package com.tangxiaolv.telegramgallery;

import android.app.Application;
import android.os.Handler;

/**
 * 相册常量
 */
public class Gallery {

    public volatile static Application applicationContext;
    public volatile static Handler applicationHandler;
    public static boolean sOriginChecked = false;
    public static boolean sListItemClickable = true;

    public static void init(Application application) {
        if (applicationContext == null) {
            applicationContext = application;
            applicationHandler = new Handler(application.getMainLooper());
        }
    }
}
