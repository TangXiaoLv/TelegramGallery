
package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeAudio_layer45 extends TL_documentAttributeAudio {
    public static int constructor = 0xded218e0;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        duration = stream.readInt32(exception);
        title = stream.readString(exception);
        performer = stream.readString(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt32(duration);
        stream.writeString(title);
        stream.writeString(performer);
    }
}
