
package com.tangxiaolv.telegramgallery.TL;

public class TL_upload_file extends TLObject {
    public static int constructor = 0x96a18d5;

    public storage_FileType type;
    public int mtime;
    public NativeByteBuffer bytes;

    public static TL_upload_file TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        if (TL_upload_file.constructor != constructor) {
            if (exception) {
                throw new RuntimeException(
                        String.format("can't parse magic %x in TL_upload_file", constructor));
            } else {
                return null;
            }
        }
        TL_upload_file result = new TL_upload_file();
        result.readParams(stream, exception);
        return result;
    }

    public void readParams(AbstractSerializedData stream, boolean exception) {
        type = storage_FileType.TLdeserialize(stream, stream.readInt32(exception), exception);
        mtime = stream.readInt32(exception);
        bytes = stream.readByteBuffer(exception);
    }

    @Override
    public void freeResources() {
        if (disableFree) {
            return;
        }
        if (bytes != null) {
            bytes.reuse();
            bytes = null;
        }
    }
}
