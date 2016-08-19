package com.tangxiaolv.telegramgallery.TL;

import java.util.ArrayList;

public class Document extends TLObject {
	public long id;
		public long access_hash;
		public int user_id;
		public int date;
		public String file_name;
		public String mime_type;
		public int size;
		public PhotoSize thumb;
		public int dc_id;
		public byte[] key;
		public byte[] iv;
		public String caption;
		public ArrayList<DocumentAttribute> attributes = new ArrayList<>();

		public static Document TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Document result = null;
			switch(constructor) {
				case 0x55555556:
					result = new TL_documentEncrypted_old();
					break;
				case 0x9efc6326:
					result = new TL_document_old();
					break;
				case 0x36f8c871:
					result = new TL_documentEmpty();
					break;
				case 0x55555558:
					result = new TL_documentEncrypted();
					break;
				case 0xf9a39f4f:
					result = new TL_document();
					break;
			}
			if (result == null && exception) {
				throw new RuntimeException(String.format("can't parse magic %x in Document", constructor));
			}
			if (result != null) {
				result.readParams(stream, exception);
			}
			return result;
		}
	}