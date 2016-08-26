
package com.tangxiaolv.telegramgallery.TL;

import java.util.ArrayList;

public class Document extends TLObject {
    public long id;
    public long access_hash;
    public int user_id;
    public int date;
    public String file_name;
    public String mime_type;
    public int size;
    public PhotoSize thumb;
    public int dc_id;
    public byte[] key;
    public byte[] iv;
    public String caption;
    public ArrayList<DocumentAttribute> attributes = new ArrayList<>();

    public static Document TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        Document result = null;
        switch (constructor) {
            case 0x55555556:
                result = new TL_documentEncrypted_old();
                break;
            case 0x9efc6326:
                result = new TL_document_old();
                break;
            case 0x36f8c871:
                result = new TL_documentEmpty();
                break;
            case 0x55555558:
                result = new TL_documentEncrypted();
                break;
            case 0xf9a39f4f:
                result = new TL_document();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in Document", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public static class TL_documentEncrypted_old extends TL_document {
        public static int constructor = 0x55555556;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            file_name = stream.readString(exception);
            mime_type = stream.readString(exception);
            size = stream.readInt32(exception);
            thumb = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
            dc_id = stream.readInt32(exception);
            key = stream.readByteArray(exception);
            iv = stream.readByteArray(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(user_id);
            stream.writeInt32(date);
            stream.writeString(file_name);
            stream.writeString(mime_type);
            stream.writeInt32(size);
            thumb.serializeToStream(stream);
            stream.writeInt32(dc_id);
            stream.writeByteArray(key);
            stream.writeByteArray(iv);
        }
    }

    public static class TL_document_old extends TL_document {
        public static int constructor = 0x9efc6326;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            file_name = stream.readString(exception);
            mime_type = stream.readString(exception);
            size = stream.readInt32(exception);
            thumb = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
            dc_id = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(user_id);
            stream.writeInt32(date);
            stream.writeString(file_name);
            stream.writeString(mime_type);
            stream.writeInt32(size);
            thumb.serializeToStream(stream);
            stream.writeInt32(dc_id);
        }
    }

    public static class TL_documentEmpty extends Document {
        public static int constructor = 0x36f8c871;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }

    public static class TL_documentEncrypted extends Document {
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

    public static class TL_document extends Document {
        public static int constructor = 0xf9a39f4f;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            date = stream.readInt32(exception);
            mime_type = stream.readString(exception);
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
        }
    }
}
