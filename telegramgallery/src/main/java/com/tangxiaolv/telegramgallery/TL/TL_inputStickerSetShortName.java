
package com.tangxiaolv.telegramgallery.TL;

public class TL_inputStickerSetShortName extends InputStickerSet {
    public static int constructor = 0x861cc8a0;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        short_name = stream.readString(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeString(short_name);
    }
}
