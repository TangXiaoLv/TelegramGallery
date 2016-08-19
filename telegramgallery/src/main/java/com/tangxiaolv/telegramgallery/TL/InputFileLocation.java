
package com.tangxiaolv.telegramgallery.TL;

public class InputFileLocation extends TLObject {
    public long id;
    public long access_hash;
    public long volume_id;
    public int local_id;
    public long secret;

    public static InputFileLocation TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        InputFileLocation result = null;
        switch (constructor) {
            case 0xf5235d55:
                result = new TL_inputEncryptedFileLocation();
                break;
            case 0x4e45abe9:
                result = new TL_inputDocumentFileLocation();
                break;
            case 0x14637196:
                result = new TL_inputFileLocation();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in InputFileLocation", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }
}
