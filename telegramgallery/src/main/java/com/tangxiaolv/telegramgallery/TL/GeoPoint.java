package com.tangxiaolv.telegramgallery.TL;

public class GeoPoint extends TLObject {
	public double _long;
		public double lat;

		public static GeoPoint TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			GeoPoint result = null;
			switch(constructor) {
				case 0x1117dd5f:
					result = new TL_geoPointEmpty();
					break;
				case 0x2049d70c:
					result = new TL_geoPoint();
					break;
			}
			if (result == null && exception) {
				throw new RuntimeException(String.format("can't parse magic %x in GeoPoint", constructor));
			}
			if (result != null) {
				result.readParams(stream, exception);
			}
			return result;
		}
	}