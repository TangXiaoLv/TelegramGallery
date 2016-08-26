
package com.tangxiaolv.telegramgallery.TL;

public class DocumentAttribute extends TLObject {
    public int w;
    public int h;
    public int duration;
    public String alt;
    public InputStickerSet stickerset;
    public int flags;
    public boolean voice;
    public String title;
    public String performer;
    public byte[] waveform;
    public String file_name;

    public static DocumentAttribute TLdeserialize(AbstractSerializedData stream, int constructor,
            boolean exception) {
        DocumentAttribute result = null;
        switch (constructor) {
            case 0x11b58939:
                result = new TL_documentAttributeAnimated();
                break;
            case 0xfb0a5727:
                result = new TL_documentAttributeSticker_old();
                break;
            case 0x6c37c15c:
                result = new TL_documentAttributeImageSize();
                break;
            case 0x51448e5:
                result = new TL_documentAttributeAudio_old();
                break;
            case 0x3a556302:
                result = new TL_documentAttributeSticker();
                break;
            case 0x5910cccb:
                result = new TL_documentAttributeVideo();
                break;
            case 0x9852f9c6:
                result = new TL_documentAttributeAudio();
                break;
            case 0x994c9882:
                result = new TL_documentAttributeSticker_old2();
                break;
            case 0x15590068:
                result = new TL_documentAttributeFilename();
                break;
            case 0xded218e0:
                result = new TL_documentAttributeAudio_layer45();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(
                    String.format("can't parse magic %x in DocumentAttribute", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public static class TL_documentAttributeAnimated extends DocumentAttribute {
        public static int constructor = 0x11b58939;

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_documentAttributeSticker_old extends TL_documentAttributeSticker {
        public static int constructor = 0xfb0a5727;

        public void readParams(AbstractSerializedData stream, boolean exception) {
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_documentAttributeImageSize extends DocumentAttribute {
        public static int constructor = 0x6c37c15c;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }

    public static class TL_documentAttributeAudio_old extends TL_documentAttributeAudio {
        public static int constructor = 0x51448e5;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            duration = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(duration);
        }
    }

    public static class TL_documentAttributeSticker extends DocumentAttribute {
        public static int constructor = 0x3a556302;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            alt = stream.readString(exception);
            stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception),
                    exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(alt);
            stickerset.serializeToStream(stream);
        }
    }

    public static class TL_documentAttributeVideo extends DocumentAttribute {
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

    public static class TL_documentAttributeAudio extends DocumentAttribute {
        public static int constructor = 0x9852f9c6;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            voice = (flags & 1024) != 0;
            duration = stream.readInt32(exception);
            if ((flags & 1) != 0) {
                title = stream.readString(exception);
            }
            if ((flags & 2) != 0) {
                performer = stream.readString(exception);
            }
            if ((flags & 4) != 0) {
                waveform = stream.readByteArray(exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = voice ? (flags | 1024) : (flags & ~1024);
            stream.writeInt32(flags);
            stream.writeInt32(duration);
            if ((flags & 1) != 0) {
                stream.writeString(title);
            }
            if ((flags & 2) != 0) {
                stream.writeString(performer);
            }
            if ((flags & 4) != 0) {
                stream.writeByteArray(waveform);
            }
        }
    }

    public static class TL_documentAttributeSticker_old2 extends TL_documentAttributeSticker {
        public static int constructor = 0x994c9882;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            alt = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(alt);
        }
    }

    public static class TL_documentAttributeAudio_layer45 extends TL_documentAttributeAudio {
        public static int constructor = 0xded218e0;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            duration = stream.readInt32(exception);
            title = stream.readString(exception);
            performer = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(duration);
            stream.writeString(title);
            stream.writeString(performer);
        }
    }

    public static class TL_documentAttributeFilename extends DocumentAttribute {
        public static int constructor = 0x15590068;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            file_name = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(file_name);
        }
    }
}
