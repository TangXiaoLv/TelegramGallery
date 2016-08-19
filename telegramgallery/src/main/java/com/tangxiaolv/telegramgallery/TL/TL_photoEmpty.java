
package com.tangxiaolv.telegramgallery.TL;

public class TL_photoEmpty extends Photo {
    public static int constructor = 0x2331b22d;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt64(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt64(id);
    }
}
