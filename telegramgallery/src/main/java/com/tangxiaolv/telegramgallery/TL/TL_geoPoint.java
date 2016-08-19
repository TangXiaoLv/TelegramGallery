
package com.tangxiaolv.telegramgallery.TL;

public class TL_geoPoint extends GeoPoint {
    public static int constructor = 0x2049d70c;

    public void readParams(AbstractSerializedData stream, boolean exception) {
        _long = stream.readDouble(exception);
        lat = stream.readDouble(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeDouble(_long);
        stream.writeDouble(lat);
    }
}
