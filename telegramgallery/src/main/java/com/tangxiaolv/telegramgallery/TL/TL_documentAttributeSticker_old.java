package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeSticker_old extends TL_documentAttributeSticker {
	public static int constructor = 0xfb0a5727;


		public void readParams(AbstractSerializedData stream, boolean exception) {
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}