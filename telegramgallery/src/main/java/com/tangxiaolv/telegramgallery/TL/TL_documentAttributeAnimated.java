package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeAnimated extends DocumentAttribute {
	public static int constructor = 0x11b58939;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}