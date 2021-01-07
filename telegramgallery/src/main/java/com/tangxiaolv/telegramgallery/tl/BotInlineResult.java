package com.tangxiaolv.telegramgallery.tl;

public class BotInlineResult extends TLObject {
    public int flags;
    public String id;
    public String type;
    public String title;
    public String description;
    public String url;
    public String thumb_url;
    public String content_url;
    public String content_type;
    public int w;
    public int h;
    public int duration;
    public Photo photo;
    public Document document;
    public long query_id;

    public static BotInlineResult TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        BotInlineResult result = null;
        switch (constructor) {
            case 0x9bebaeb9:
                result = new TL_botInlineResult();
                break;
            case 0x17db940b:
                result = new TL_botInlineMediaResult();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(String.format("can't parse magic %x in BotInlineResult", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public static class TL_botInlineResult extends BotInlineResult {
        public static int constructor = 0x9bebaeb9;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            id = stream.readString(exception);
            type = stream.readString(exception);
            if ((flags & 2) != 0) {
                title = stream.readString(exception);
            }
            if ((flags & 4) != 0) {
                description = stream.readString(exception);
            }
            if ((flags & 8) != 0) {
                url = stream.readString(exception);
            }
            if ((flags & 16) != 0) {
                thumb_url = stream.readString(exception);
            }
            if ((flags & 32) != 0) {
                content_url = stream.readString(exception);
            }
            if ((flags & 32) != 0) {
                content_type = stream.readString(exception);
            }
            if ((flags & 64) != 0) {
                w = stream.readInt32(exception);
            }
            if ((flags & 64) != 0) {
                h = stream.readInt32(exception);
            }
            if ((flags & 128) != 0) {
                duration = stream.readInt32(exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(id);
            stream.writeString(type);
            if ((flags & 2) != 0) {
                stream.writeString(title);
            }
            if ((flags & 4) != 0) {
                stream.writeString(description);
            }
            if ((flags & 8) != 0) {
                stream.writeString(url);
            }
            if ((flags & 16) != 0) {
                stream.writeString(thumb_url);
            }
            if ((flags & 32) != 0) {
                stream.writeString(content_url);
            }
            if ((flags & 32) != 0) {
                stream.writeString(content_type);
            }
            if ((flags & 64) != 0) {
                stream.writeInt32(w);
            }
            if ((flags & 64) != 0) {
                stream.writeInt32(h);
            }
            if ((flags & 128) != 0) {
                stream.writeInt32(duration);
            }
        }
    }

    public static class TL_botInlineMediaResult extends BotInlineResult {
        public static int constructor = 0x17db940b;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            id = stream.readString(exception);
            type = stream.readString(exception);
            if ((flags & 1) != 0) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if ((flags & 2) != 0) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if ((flags & 4) != 0) {
                title = stream.readString(exception);
            }
            if ((flags & 8) != 0) {
                description = stream.readString(exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(id);
            stream.writeString(type);
            if ((flags & 1) != 0) {
                photo.serializeToStream(stream);
            }
            if ((flags & 2) != 0) {
                document.serializeToStream(stream);
            }
            if ((flags & 4) != 0) {
                stream.writeString(title);
            }
            if ((flags & 8) != 0) {
                stream.writeString(description);
            }
        }
    }
}

