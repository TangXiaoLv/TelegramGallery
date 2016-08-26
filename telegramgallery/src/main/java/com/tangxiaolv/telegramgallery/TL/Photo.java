
package com.tangxiaolv.telegramgallery.TL;

import java.util.ArrayList;

public class Photo extends TLObject {
    public long id;
    public long access_hash;
    public int user_id;
    public int date;
    public String caption;
    public GeoPoint geo;
    public ArrayList<PhotoSize> sizes = new ArrayList<>();

    public static Photo TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        Photo result = null;
        switch (constructor) {
            case 0x22b56751:
                result = new TL_photo_old();
                break;
            case 0xcded42fe:
                result = new TL_photo();
                break;
            case 0xc3838076:
                result = new TL_photo_old2();
                break;
            case 0x2331b22d:
                result = new TL_photoEmpty();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(String.format("can't parse magic %x in Photo", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public static class TL_photo_old extends TL_photo {
        public static int constructor = 0x22b56751;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            caption = stream.readString(exception);
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PhotoSize object = PhotoSize.TLdeserialize(stream, stream.readInt32(exception),
                        exception);
                if (object == null) {
                    return;
                }
                sizes.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(user_id);
            stream.writeInt32(date);
            stream.writeString(caption);
            geo.serializeToStream(stream);
            stream.writeInt32(0x1cb5c415);
            int count = sizes.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                sizes.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_photo extends Photo {
        public static int constructor = 0xcded42fe;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            date = stream.readInt32(exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PhotoSize object = PhotoSize.TLdeserialize(stream, stream.readInt32(exception),
                        exception);
                if (object == null) {
                    return;
                }
                sizes.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(date);
            stream.writeInt32(0x1cb5c415);
            int count = sizes.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                sizes.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_photo_old2 extends TL_photo {
        public static int constructor = 0xc3838076;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
            geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PhotoSize object = PhotoSize.TLdeserialize(stream, stream.readInt32(exception),
                        exception);
                if (object == null) {
                    return;
                }
                sizes.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(user_id);
            stream.writeInt32(date);
            geo.serializeToStream(stream);
            stream.writeInt32(0x1cb5c415);
            int count = sizes.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                sizes.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_photoEmpty extends Photo {
        public static int constructor = 0x2331b22d;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
        }
    }
}
