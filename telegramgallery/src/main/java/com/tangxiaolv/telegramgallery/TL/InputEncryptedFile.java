
package com.tangxiaolv.telegramgallery.TL;

public class InputEncryptedFile extends TLObject {
    public long id;
    public long access_hash;
    public int parts;
    public int key_fingerprint;
    public String md5_checksum;

    public static InputEncryptedFile TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        InputEncryptedFile result = null;
        switch (constructor) {
            case 0x5a17b5e5:
                result = new TL_inputEncryptedFile();
                break;
            case 0x2dc173c8:
                result = new TL_inputEncryptedFileBigUploaded();
                break;
            case 0x1837c364:
                result = new TL_inputEncryptedFileEmpty();
                break;
            case 0x64bd0306:
                result = new TL_inputEncryptedFileUploaded();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in InputEncryptedFile", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }
}
