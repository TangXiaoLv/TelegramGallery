
package com.tangxiaolv.telegramgallery.TL;

public class InputFile extends TLObject {
    public long id;
    public int parts;
    public String name;
    public String md5_checksum;

    public static InputFile TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        InputFile result = null;
        switch (constructor) {
            case 0xfa4f0bb5:
                result = new TL_inputFileBig();
                break;
            case 0xf52ff27f:
                result = new TL_inputFile();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in InputFile", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }
}
