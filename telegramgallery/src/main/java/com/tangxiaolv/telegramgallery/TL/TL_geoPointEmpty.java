package com.tangxiaolv.telegramgallery.TL;

public class TL_geoPointEmpty extends GeoPoint {
	public static int constructor = 0x1117dd5f;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}