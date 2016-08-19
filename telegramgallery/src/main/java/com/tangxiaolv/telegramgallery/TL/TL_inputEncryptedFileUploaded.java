
package com.tangxiaolv.telegramgallery.TL;

public class TL_inputEncryptedFileUploaded extends InputEncryptedFile {
    public static int constructor = 0x64bd0306;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt64(exception);
        parts = stream.readInt32(exception);
        md5_checksum = stream.readString(exception);
        key_fingerprint = stream.readInt32(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt64(id);
        stream.writeInt32(parts);
        stream.writeString(md5_checksum);
        stream.writeInt32(key_fingerprint);
    }
}
