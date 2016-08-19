
package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeSticker_old2 extends TL_documentAttributeSticker {
    public static int constructor = 0x994c9882;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        alt = stream.readString(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeString(alt);
    }
}
