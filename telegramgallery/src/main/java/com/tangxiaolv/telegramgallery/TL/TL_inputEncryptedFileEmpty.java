
package com.tangxiaolv.telegramgallery.TL;

public class TL_inputEncryptedFileEmpty extends InputEncryptedFile {
    public static int constructor = 0x1837c364;

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
    }
}
