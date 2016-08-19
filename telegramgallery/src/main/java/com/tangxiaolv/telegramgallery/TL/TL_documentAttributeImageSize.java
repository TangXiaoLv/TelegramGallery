package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeImageSize extends DocumentAttribute {
	public static int constructor = 0x6c37c15c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			w = stream.readInt32(exception);
			h = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(w);
			stream.writeInt32(h);
		}
	}