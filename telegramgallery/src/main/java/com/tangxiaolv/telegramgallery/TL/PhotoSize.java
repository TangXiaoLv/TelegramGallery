package com.tangxiaolv.telegramgallery.TL;

public class PhotoSize extends TLObject {
	public String type;
		public FileLocation location;
		public int w;
		public int h;
		public int size;
		public byte[] bytes;

		public static PhotoSize TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			PhotoSize result = null;
			switch(constructor) {
				case 0x77bfb61b:
					result = new TL_photoSize();
					break;
				case 0xe17e23c:
					result = new TL_photoSizeEmpty();
					break;
				case 0xe9a734fa:
					result = new TL_photoCachedSize();
					break;
			}
			if (result == null && exception) {
				throw new RuntimeException(String.format("can't parse magic %x in PhotoSize", constructor));
			}
			if (result != null) {
				result.readParams(stream, exception);
			}
			return result;
		}
	}