
package com.tangxiaolv.telegramgallery.Utils;

import com.tangxiaolv.telegramgallery.Gallery;

public class LocaleController {

    public static boolean isRTL = false;

    public static String getString(String key, int res) {
        return Gallery.applicationContext.getString(res);
    }

    public static String formatString(String key, int res, Object... args) {
        return String.format(getString(key, res), args);
    }
}
