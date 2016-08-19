package com.tangxiaolv.telegramgallery.TL;

public class TL_documentEmpty extends Document {
	public static int constructor = 0x36f8c871;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
		}
	}