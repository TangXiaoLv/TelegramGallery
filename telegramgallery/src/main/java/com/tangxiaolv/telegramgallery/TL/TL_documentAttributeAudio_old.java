
package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeAudio_old extends TL_documentAttributeAudio {
    public static int constructor = 0x51448e5;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        duration = stream.readInt32(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt32(duration);
    }
}
