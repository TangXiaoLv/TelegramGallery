
package com.tangxiaolv.telegramgallery.TL;

public class TL_photoSizeEmpty extends PhotoSize {
    public static int constructor = 0xe17e23c;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        int startReadPosiition = stream.getPosition(); // TODO remove this hack after some time
        try {
            type = stream.readString(true);
            if (type.length() > 1 || !type.equals("") && !type.equals("s") && !type.equals("x")
                    && !type.equals("m") && !type.equals("y") && !type.equals("w")) {
                type = "s";
                if (stream instanceof NativeByteBuffer) {
                    ((NativeByteBuffer) stream).position(startReadPosiition);
                }
            }
        } catch (Exception e) {
            type = "s";
            if (stream instanceof NativeByteBuffer) {
                ((NativeByteBuffer) stream).position(startReadPosiition);
            }
        }
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeString(type);
    }
}
