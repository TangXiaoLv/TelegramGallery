
package com.tangxiaolv.telegramgallery.TL;

import java.util.ArrayList;

public class Photo extends TLObject {
    public long id;
    public long access_hash;
    public int user_id;
    public int date;
    public String caption;
    public GeoPoint geo;
    public ArrayList<PhotoSize> sizes = new ArrayList<>();

    public static Photo TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        Photo result = null;
        switch (constructor) {
            case 0x22b56751:
                result = new TL_photo_old();
                break;
            case 0xcded42fe:
                result = new TL_photo();
                break;
            case 0xc3838076:
                result = new TL_photo_old2();
                break;
            case 0x2331b22d:
                result = new TL_photoEmpty();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(String.format("can't parse magic %x in Photo", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }


}
