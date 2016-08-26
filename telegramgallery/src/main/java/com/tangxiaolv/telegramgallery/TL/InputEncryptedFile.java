
package com.tangxiaolv.telegramgallery.TL;

public class InputEncryptedFile extends TLObject {
    public long id;
    public long access_hash;
    public int parts;
    public int key_fingerprint;
    public String md5_checksum;

    public static InputEncryptedFile TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        InputEncryptedFile result = null;
        switch (constructor) {
            case 0x5a17b5e5:
                result = new TL_inputEncryptedFile();
                break;
            case 0x2dc173c8:
                result = new TL_inputEncryptedFileBigUploaded();
                break;
            case 0x1837c364:
                result = new TL_inputEncryptedFileEmpty();
                break;
            case 0x64bd0306:
                result = new TL_inputEncryptedFileUploaded();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in InputEncryptedFile", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public static class TL_inputEncryptedFileBigUploaded extends InputEncryptedFile {
        public static int constructor = 0x2dc173c8;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            parts = stream.readInt32(exception);
            key_fingerprint = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt32(parts);
            stream.writeInt32(key_fingerprint);
        }
    }

    public static class TL_inputEncryptedFile extends InputEncryptedFile {
        public static int constructor = 0x5a17b5e5;

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

    public static class TL_inputEncryptedFileEmpty extends InputEncryptedFile {
        public static int constructor = 0x1837c364;

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_inputEncryptedFileUploaded extends InputEncryptedFile {
        public static int constructor = 0x64bd0306;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            parts = stream.readInt32(exception);
            md5_checksum = stream.readString(exception);
            key_fingerprint = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt32(parts);
            stream.writeString(md5_checksum);
            stream.writeInt32(key_fingerprint);
        }
    }
}
