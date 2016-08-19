
package com.tangxiaolv.telegramgallery.TL;

public class TL_inputEncryptedFile extends InputEncryptedFile {
    public static int constructor = 0x5a17b5e5;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt64(exception);
        access_hash = stream.readInt64(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt64(id);
        stream.writeInt64(access_hash);
    }
}
