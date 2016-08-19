
package com.tangxiaolv.telegramgallery.TL;

public class InputStickerSet extends TLObject {
    public long id;
    public long access_hash;
    public String short_name;

    public static InputStickerSet TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        InputStickerSet result = null;
        switch (constructor) {
            case 0xffb62b95:
                result = new TL_inputStickerSetEmpty();
                break;
            case 0x9de7a269:
                result = new TL_inputStickerSetID();
                break;
            case 0x861cc8a0:
                result = new TL_inputStickerSetShortName();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in InputStickerSet", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }
}
