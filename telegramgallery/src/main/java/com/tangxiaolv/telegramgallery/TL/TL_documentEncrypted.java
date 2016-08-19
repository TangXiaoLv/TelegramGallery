
package com.tangxiaolv.telegramgallery.TL;

public class TL_documentEncrypted extends Document {
    public static int constructor = 0x55555558;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt64(exception);
        access_hash = stream.readInt64(exception);
        date = stream.readInt32(exception);
        int startReadPosiition = stream.getPosition(); // TODO remove this hack after some time
        try {
            mime_type = stream.readString(true);
        } catch (Exception e) {
            mime_type = "audio/ogg";
            if (stream instanceof NativeByteBuffer) {
                ((NativeByteBuffer) stream).position(startReadPosiition);
            }
        }
        size = stream.readInt32(exception);
        thumb = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
        dc_id = stream.readInt32(exception);
        int magic = stream.readInt32(exception);
        if (magic != 0x1cb5c415) {
            if (exception) {
                throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
            }
            return;
        }
        int count = stream.readInt32(exception);
        for (int a = 0; a < count; a++) {
            DocumentAttribute object = DocumentAttribute.TLdeserialize(stream,
                    stream.readInt32(exception), exception);
            if (object == null) {
                return;
            }
            attributes.add(object);
        }
        key = stream.readByteArray(exception);
        iv = stream.readByteArray(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt64(id);
        stream.writeInt64(access_hash);
        stream.writeInt32(date);
        stream.writeString(mime_type);
        stream.writeInt32(size);
        thumb.serializeToStream(stream);
        stream.writeInt32(dc_id);
        stream.writeInt32(0x1cb5c415);
        int count = attributes.size();
        stream.writeInt32(count);
        for (int a = 0; a < count; a++) {
            attributes.get(a).serializeToStream(stream);
        }
        stream.writeByteArray(key);
        stream.writeByteArray(iv);
    }
}
