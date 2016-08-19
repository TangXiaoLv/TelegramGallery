
package com.tangxiaolv.telegramgallery.TL;

public class TL_photoCachedSize extends PhotoSize {
    public static int constructor = 0xe9a734fa;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        type = stream.readString(exception);
        location = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
        w = stream.readInt32(exception);
        h = stream.readInt32(exception);
        bytes = stream.readByteArray(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeString(type);
        location.serializeToStream(stream);
        stream.writeInt32(w);
        stream.writeInt32(h);
        stream.writeByteArray(bytes);
    }
}
