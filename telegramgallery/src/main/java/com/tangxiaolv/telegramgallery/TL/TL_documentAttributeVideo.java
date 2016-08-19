package com.tangxiaolv.telegramgallery.TL;

public class TL_documentAttributeVideo extends DocumentAttribute {
	public static int constructor = 0x5910cccb;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			duration = stream.readInt32(exception);
			w = stream.readInt32(exception);
			h = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(duration);
			stream.writeInt32(w);
			stream.writeInt32(h);
		}
	}