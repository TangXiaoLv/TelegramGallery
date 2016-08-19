
package com.tangxiaolv.telegramgallery.TL;

public class TL_inputFileBig extends InputFile {
    public static int constructor = 0xfa4f0bb5;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt64(exception);
        parts = stream.readInt32(exception);
        name = stream.readString(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt64(id);
        stream.writeInt32(parts);
        stream.writeString(name);
    }
}
