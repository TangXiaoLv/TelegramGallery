
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

    public static class TL_inputEncryptedFileLocation extends InputFileLocation {
        public static int constructor = 0xf5235d55;

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

    public static class TL_inputDocumentFileLocation extends InputFileLocation {
        public static int constructor = 0x4e45abe9;

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

    public static class TL_inputFileLocation extends InputFileLocation {
        public static int constructor = 0x14637196;

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
