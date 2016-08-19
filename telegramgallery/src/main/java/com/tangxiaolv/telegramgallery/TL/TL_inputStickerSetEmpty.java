
package com.tangxiaolv.telegramgallery.TL;

public class TL_inputStickerSetEmpty extends InputStickerSet {
    public static int constructor = 0xffb62b95;

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
    }
}
