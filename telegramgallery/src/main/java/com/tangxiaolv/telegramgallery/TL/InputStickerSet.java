
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

    public static class TL_inputStickerSetEmpty extends InputStickerSet {
        public static int constructor = 0xffb62b95;

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputStickerSetID extends InputStickerSet {
        public static int constructor = 0x9de7a269;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
        }
    }

    public static class TL_inputStickerSetShortName extends InputStickerSet {
        public static int constructor = 0x861cc8a0;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            short_name = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(short_name);
        }
    }

}
