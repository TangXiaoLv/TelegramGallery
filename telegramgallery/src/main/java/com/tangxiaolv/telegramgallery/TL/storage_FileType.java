
package com.tangxiaolv.telegramgallery.TL;

public class storage_FileType extends TLObject {

    public static storage_FileType TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        storage_FileType result = null;
        switch (constructor) {
            case 0xaa963b05:
                result = new TL_storage_fileUnknown();
                break;
            case 0xb3cea0e4:
                result = new TL_storage_fileMp4();
                break;
            case 0x1081464c:
                result = new TL_storage_fileWebp();
                break;
            case 0xa4f63c0:
                result = new TL_storage_filePng();
                break;
            case 0xcae1aadf:
                result = new TL_storage_fileGif();
                break;
            case 0xae1e508d:
                result = new TL_storage_filePdf();
                break;
            case 0x528a0677:
                result = new TL_storage_fileMp3();
                break;
            case 0x7efe0e:
                result = new TL_storage_fileJpeg();
                break;
            case 0x4b09ebbc:
                result = new TL_storage_fileMov();
                break;
            case 0x40bc6f52:
                result = new TL_storage_filePartial();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in storage_FileType", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

	public static class TL_storage_fileUnknown extends storage_FileType {
		public static int constructor = 0xaa963b05;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_fileMp4 extends storage_FileType {
		public static int constructor = 0xb3cea0e4;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_fileWebp extends storage_FileType {
		public static int constructor = 0x1081464c;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_filePng extends storage_FileType {
		public static int constructor = 0xa4f63c0;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_fileGif extends storage_FileType {
		public static int constructor = 0xcae1aadf;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_filePdf extends storage_FileType {
		public static int constructor = 0xae1e508d;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_fileMp3 extends storage_FileType {
		public static int constructor = 0x528a0677;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_fileJpeg extends storage_FileType {
		public static int constructor = 0x7efe0e;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_fileMov extends storage_FileType {
		public static int constructor = 0x4b09ebbc;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_storage_filePartial extends storage_FileType {
		public static int constructor = 0x40bc6f52;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}
}
