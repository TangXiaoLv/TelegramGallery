package com.tangxiaolv.telegramgallery.TL;

public class FileLocation extends TLObject {
	public int dc_id;
		public long volume_id;
		public int local_id;
        public long secret;
		public byte[] key;
        public byte[] iv;

		public static FileLocation TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
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
				throw new RuntimeException(String.format("can't parse magic %x in FileLocation", constructor));
			}
			if (result != null) {
				result.readParams(stream, exception);
			}
			return result;
		}
	}