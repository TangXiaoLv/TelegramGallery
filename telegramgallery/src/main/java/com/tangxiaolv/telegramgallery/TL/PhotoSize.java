
package com.tangxiaolv.telegramgallery.TL;

public class PhotoSize extends TLObject {
    public String type;
    public FileLocation location;
    public int w;
    public int h;
    public int size;
    public byte[] bytes;

    public static PhotoSize TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        PhotoSize result = null;
        switch (constructor) {
            case 0x77bfb61b:
                result = new TL_photoSize();
                break;
            case 0xe17e23c:
                result = new TL_photoSizeEmpty();
                break;
            case 0xe9a734fa:
                result = new TL_photoCachedSize();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in PhotoSize", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public static class TL_photoSize extends PhotoSize {
        public static int constructor = 0x77bfb61b;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            type = stream.readString(exception);
            location = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            size = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(type);
            location.serializeToStream(stream);
            stream.writeInt32(w);
            stream.writeInt32(h);
            stream.writeInt32(size);
        }
    }

    public static class TL_photoSizeEmpty extends PhotoSize {
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

    public static class TL_photoCachedSize extends PhotoSize {
        public static int constructor = 0xe9a734fa;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            type = stream.readString(exception);
            location = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            bytes = stream.readByteArray(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(type);
            location.serializeToStream(stream);
            stream.writeInt32(w);
            stream.writeInt32(h);
            stream.writeByteArray(bytes);
        }
    }

}
