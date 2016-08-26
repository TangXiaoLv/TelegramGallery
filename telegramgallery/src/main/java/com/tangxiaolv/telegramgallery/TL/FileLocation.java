
package com.tangxiaolv.telegramgallery.TL;

public class FileLocation extends TLObject {
    public int dc_id;
    public long volume_id;
    public int local_id;
    public long secret;
    public byte[] key;
    public byte[] iv;

    public static FileLocation TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        FileLocation result = null;
        switch (constructor) {
            case 0x53d69076:
                result = new TL_fileLocation();
                break;
            case 0x55555554:
                result = new TL_fileEncryptedLocation();
                break;
            case 0x7c596b46:
                result = new TL_fileLocationUnavailable();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in FileLocation", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public static class TL_fileLocation extends FileLocation {
        public static int constructor = 0x53d69076;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            dc_id = stream.readInt32(exception);
            volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
            secret = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(dc_id);
            stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
            stream.writeInt64(secret);
        }
    }

    public static class TL_fileEncryptedLocation extends FileLocation {
        public static int constructor = 0x55555554;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            dc_id = stream.readInt32(exception);
            volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
            secret = stream.readInt64(exception);
            key = stream.readByteArray(exception);
            iv = stream.readByteArray(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(dc_id);
            stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
            stream.writeInt64(secret);
            stream.writeByteArray(key);
            stream.writeByteArray(iv);
        }
    }

    public static class TL_fileLocationUnavailable extends FileLocation {
        public static int constructor = 0x7c596b46;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
            secret = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
            stream.writeInt64(secret);
        }
    }
}
