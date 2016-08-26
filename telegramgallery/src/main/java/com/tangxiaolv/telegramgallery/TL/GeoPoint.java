
package com.tangxiaolv.telegramgallery.TL;

public class GeoPoint extends TLObject {
    public double _long;
    public double lat;

    public static GeoPoint TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        GeoPoint result = null;
        switch (constructor) {
            case 0x1117dd5f:
                result = new TL_geoPointEmpty();
                break;
            case 0x2049d70c:
                result = new TL_geoPoint();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in GeoPoint", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public static class TL_geoPointEmpty extends GeoPoint {
        public static int constructor = 0x1117dd5f;

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_geoPoint extends GeoPoint {
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
}
