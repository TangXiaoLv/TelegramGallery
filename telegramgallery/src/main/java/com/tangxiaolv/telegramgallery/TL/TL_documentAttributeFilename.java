
package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeFilename extends DocumentAttribute {
    public static int constructor = 0x15590068;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        file_name = stream.readString(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeString(file_name);
    }
}
