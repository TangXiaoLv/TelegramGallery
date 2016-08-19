
package com.tangxiaolv.telegramgallery.TL;

public class TL_inputEncryptedFileBigUploaded extends InputEncryptedFile {
    public static int constructor = 0x2dc173c8;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt64(exception);
        parts = stream.readInt32(exception);
        key_fingerprint = stream.readInt32(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt64(id);
        stream.writeInt32(parts);
        stream.writeInt32(key_fingerprint);
    }
}
