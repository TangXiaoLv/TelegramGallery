
package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeAudio extends DocumentAttribute {
    public static int constructor = 0x9852f9c6;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        flags = stream.readInt32(exception);
        voice = (flags & 1024) != 0;
        duration = stream.readInt32(exception);
        if ((flags & 1) != 0) {
            title = stream.readString(exception);
        }
        if ((flags & 2) != 0) {
            performer = stream.readString(exception);
        }
        if ((flags & 4) != 0) {
            waveform = stream.readByteArray(exception);
        }
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        flags = voice ? (flags | 1024) : (flags & ~1024);
        stream.writeInt32(flags);
        stream.writeInt32(duration);
        if ((flags & 1) != 0) {
            stream.writeString(title);
        }
        if ((flags & 2) != 0) {
            stream.writeString(performer);
        }
        if ((flags & 4) != 0) {
            stream.writeByteArray(waveform);
        }
    }
}
