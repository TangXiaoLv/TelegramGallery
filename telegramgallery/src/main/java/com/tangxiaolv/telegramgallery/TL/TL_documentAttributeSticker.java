package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeSticker extends DocumentAttribute {
	public static int constructor = 0x3a556302;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			alt = stream.readString(exception);
			stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(alt);
			stickerset.serializeToStream(stream);
		}
	}