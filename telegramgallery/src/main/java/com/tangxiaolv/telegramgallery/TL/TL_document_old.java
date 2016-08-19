package com.tangxiaolv.telegramgallery.TL;

public class TL_document_old extends TL_document {
    public static int constructor = 0x9efc6326;


		public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            access_hash = stream.readInt64(exception);
			user_id = stream.readInt32(exception);
			date = stream.readInt32(exception);
            file_name = stream.readString(exception);
			mime_type = stream.readString(exception);
            size = stream.readInt32(exception);
            thumb = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
			dc_id = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
            stream.writeInt64(access_hash);
            stream.writeInt32(user_id);
            stream.writeInt32(date);
			stream.writeString(file_name);
            stream.writeString(mime_type);
            stream.writeInt32(size);
			thumb.serializeToStream(stream);
			stream.writeInt32(dc_id);
		}
	}