
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
}
