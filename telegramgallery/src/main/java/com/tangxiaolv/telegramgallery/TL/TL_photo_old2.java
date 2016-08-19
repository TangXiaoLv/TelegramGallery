package com.tangxiaolv.telegramgallery.TL;

public class TL_photo_old2 extends TL_photo {
	public static int constructor = 0xc3838076;


		public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
            user_id = stream.readInt32(exception);
            date = stream.readInt32(exception);
			geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
                return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				PhotoSize object = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				sizes.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(user_id);
			stream.writeInt32(date);
			geo.serializeToStream(stream);
			stream.writeInt32(0x1cb5c415);
			int count = sizes.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				sizes.get(a).serializeToStream(stream);
			}
		}
	}