
package com.tangxiaolv.telegramgallery.TL;

public class TL_photoSize extends PhotoSize {
    public static int constructor = 0x77bfb61b;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        type = stream.readString(exception);
        location = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
        w = stream.readInt32(exception);
        h = stream.readInt32(exception);
        size = stream.readInt32(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeString(type);
        location.serializeToStream(stream);
        stream.writeInt32(w);
        stream.writeInt32(h);
        stream.writeInt32(size);
    }
}
