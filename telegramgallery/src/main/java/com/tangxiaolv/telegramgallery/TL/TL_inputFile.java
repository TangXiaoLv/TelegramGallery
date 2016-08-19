
package com.tangxiaolv.telegramgallery.TL;

public class TL_inputFile extends InputFile {
    public static int constructor = 0xf52ff27f;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt64(exception);
        parts = stream.readInt32(exception);
        name = stream.readString(exception);
        md5_checksum = stream.readString(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt64(id);
        stream.writeInt32(parts);
        stream.writeString(name);
        stream.writeString(md5_checksum);
    }
}
