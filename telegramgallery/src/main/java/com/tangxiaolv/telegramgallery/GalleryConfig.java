
package com.tangxiaolv.telegramgallery;

import android.support.annotation.AnimRes;

import java.io.Serializable;

/**
 * the {@link GalleryActivity} of buidler.
 */
public class GalleryConfig implements Serializable {

    private String[] filterMimeTypes;

    private boolean singlePhoto;

    private boolean hasOriginalPic;

    private int limitPickPhoto;

    private boolean videoEditMode;

    private boolean hasVideo;

    private int maxVideoTime;

    private long maxImageSize;

    private int enterAnim;

    private int exitAnim;

    public String[] getFilterMimeTypes() {
        return filterMimeTypes;
    }

    public boolean isSinglePhoto() {
        return singlePhoto;
    }

    public boolean hasOriginalPic() {
        return hasOriginalPic;
    }

    public int getLimitPickPhoto() {
        return limitPickPhoto;
    }

    public int getEnterAnim() {
        return enterAnim;
    }

    public int getExitAnim() {
        return exitAnim;
    }

    public boolean isVideoEditMode() {
        return videoEditMode;
    }

    public int getMaxVideoTime() {
        return maxVideoTime;
    }

    public long getMaxImageSize() {
        return maxImageSize;
    }

    public boolean hasVideo() {
        return hasVideo;
    }

    private GalleryConfig(String[] filterMimeTypes,
                          boolean singlePhoto,
                          boolean hasOriginalPic,
                          int limitPickPhoto,
                          int enterAnim, int exitAnim,
                          boolean videoEditMode,
                          boolean hasVideo,
                          int maxVideoTime,
                          long maxImageSize) {
        this.filterMimeTypes = filterMimeTypes;
        this.singlePhoto = singlePhoto;
        this.hasOriginalPic = hasOriginalPic;
        this.limitPickPhoto = limitPickPhoto;
        this.enterAnim = enterAnim;
        this.exitAnim = exitAnim;
        this.videoEditMode = videoEditMode;
        this.hasVideo = hasVideo;
        this.maxVideoTime = maxVideoTime;
        this.maxImageSize = maxImageSize;
    }

    public static class Build {
        private String[] filterMimeTypes;
        private boolean singlePhoto = false;
        private boolean hasOriginalPic = false;
        private int limitPickPhoto = 9;
        private int enterAnim = -1;
        private int exitAnim = -1;
        private boolean videoEditMode = false;
        private boolean hasVideo = false;
        private int maxVideoTime = 1024 * 1024;//sec
        private long maxImageSize = 1024 * 1024 * 1024;

        /**
         * @param filterMimeTypes filter media types，base on MimeType。
         * @see {http://www.w3school.com.cn/media/media_mimeref.asp} eg:new string[]{"image/gif","image/jpeg"}
         *
         */
        public Build setFilterMimeTypes(String[] filterMimeTypes) {
            this.filterMimeTypes = filterMimeTypes;
            return this;
        }


        /**
         * @param singlePhoto is select of single
         */
        public Build setSinglePhoto(boolean singlePhoto) {
            this.singlePhoto = singlePhoto;
            return this;
        }

        /**
         * show button of named 'origin'
         */
        public Build setHasOriginalPic(boolean hasOriginalPic) {
            this.hasOriginalPic = hasOriginalPic;
            return this;
        }

        /**
         * @param limitPickPhoto the limit of photos those can be selected
         */
        public Build setLimitPickPhoto(int limitPickPhoto) {
            this.limitPickPhoto = limitPickPhoto;
            return this;
        }

        /**
         * entry animation
         */
        public Build setAnimation(@AnimRes int enterAnim, @AnimRes int exitAnim) {
            this.enterAnim = enterAnim;
            this.exitAnim = exitAnim;
            return this;
        }

        /*public Build setVideoEditMode(boolean videoEditMode) {
            this.videoEditMode = videoEditMode;
            return this;
        }*/

        /**
         * @param hasVideo has list video
         */
        public Build setHasVideo(boolean hasVideo) {
            this.hasVideo = hasVideo;
            return this;
        }

        /**
         * @param maxVideoTime limit time of be send video unit：sec
         */
        public Build setMaxVideoTime(int maxVideoTime) {
            this.maxVideoTime = maxVideoTime;
            return this;
        }

        /**
         * @param maxImageSize limit size of be send image unit：bytes
         */
        public Build setMaxImageSize(int maxImageSize) {
            this.maxImageSize = maxImageSize;
            return this;
        }

        public GalleryConfig build() {
            this.limitPickPhoto = singlePhoto ? 1 : limitPickPhoto > 0 ? limitPickPhoto : 1;
            return new GalleryConfig(
                    filterMimeTypes,
                    singlePhoto,
                    hasOriginalPic,
                    limitPickPhoto,
                    enterAnim,
                    exitAnim,
                    videoEditMode,
                    hasVideo,
                    maxVideoTime,
                    maxImageSize);
        }
    }
}
