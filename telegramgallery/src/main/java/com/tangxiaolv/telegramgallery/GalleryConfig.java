
package com.tangxiaolv.telegramgallery;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * the {@link GalleryActivity} of buidler.
 */
public class GalleryConfig implements Parcelable {

    private String[] filterMimeTypes;
    private String hintOfPick;
    private boolean singlePhoto;
    private int limitPickPhoto;
    private PendingIntent limitReachedIntent;

    private GalleryConfig(){

    }

    private GalleryConfig(String[] filterMimeTypes, String hintOfPick, boolean singlePhoto,
                          int limitPickPhoto, PendingIntent limitReachedIntent) {
        this.filterMimeTypes = filterMimeTypes;
        this.hintOfPick = hintOfPick;
        this.singlePhoto = singlePhoto;
        this.limitPickPhoto = limitPickPhoto;
        this.limitReachedIntent = limitReachedIntent;
    }

    public String[] getFilterMimeTypes() {
        return filterMimeTypes;
    }

    public String getHintOfPick() {
        return hintOfPick;
    }

    public boolean isSinglePhoto() {
        return singlePhoto;
    }

    public int getLimitPickPhoto() {
        return limitPickPhoto;
    }

    public PendingIntent getLimitReachedIntent() {
        return limitReachedIntent;
    }

    public static class Build {
        private String[] filterMimeTypes;
        private String hintOfPick;
        private boolean singlePhoto = false;
        private int limitPickPhoto = 9;
        private PendingIntent multiPhotoSelectedPendingIntent = null;

        /**
         * @param filterMimeTypes filter of media type， based on MimeType standards：
         *            {http://www.w3school.com.cn/media/media_mimeref.asp}
         *            <Li>eg:new string[]{"image/gif","image/jpeg"}
         */
        public Build filterMimeTypes(String[] filterMimeTypes) {
            this.filterMimeTypes = filterMimeTypes;
            return this;
        }

        /**
         * @param hintOfPick hint of Toast when limit is reached
         */
        public Build hintOfPick(String hintOfPick) {
            this.hintOfPick = hintOfPick;
            return this;
        }

        /**
         * @param singlePhoto true:single pick false:multi pick
         */
        public Build singlePhoto(boolean singlePhoto) {
            this.singlePhoto = singlePhoto;
            return this;
        }

        /**
         * @param limitPickPhoto the limit of photos those can be selected
         */
        public Build limitPickPhoto(int limitPickPhoto) {
            this.limitPickPhoto = limitPickPhoto;
            return this;
        }

        /**
         * @param pendingIntent the pendingIntent that will be sent when user selects a picture
         *                      and {@code limitPickPhoto} photos are already selected.
         *                      This will be ignored if {@code singlePicture} is true. If both
         *                      {@code hintOfPick} and pendingIntent are provided, only pendingIntent
         *                      will be used.
         */
        public Build limitReachedIntent(PendingIntent pendingIntent) {
            this.multiPhotoSelectedPendingIntent = pendingIntent;
            return this;
        }

        public GalleryConfig build() {
            this.limitPickPhoto = singlePhoto ? 1 : limitPickPhoto > 0 ? limitPickPhoto : 1;
            if (singlePhoto) {
                this.multiPhotoSelectedPendingIntent = null;
            }
            return new GalleryConfig(
                filterMimeTypes,
                hintOfPick,
                singlePhoto,
                limitPickPhoto,
                multiPhotoSelectedPendingIntent);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(this.filterMimeTypes);
        dest.writeString(this.hintOfPick);
        dest.writeByte(this.singlePhoto ? (byte) 1 : (byte) 0);
        dest.writeInt(this.limitPickPhoto);
        dest.writeValue(this.limitReachedIntent);
    }

    protected GalleryConfig(Parcel in) {
        this.filterMimeTypes = in.createStringArray();
        this.hintOfPick = in.readString();
        this.singlePhoto = in.readByte() != 0;
        this.limitPickPhoto = in.readInt();
        this.limitReachedIntent =
            (PendingIntent) in.readValue(PendingIntent.class.getClassLoader());
    }

    public static final Creator<GalleryConfig> CREATOR = new Creator<GalleryConfig>() {
        @Override
        public GalleryConfig createFromParcel(Parcel source) {
            return new GalleryConfig(source);
        }

        @Override
        public GalleryConfig[] newArray(int size) {
            return new GalleryConfig[size];
        }
    };
}
