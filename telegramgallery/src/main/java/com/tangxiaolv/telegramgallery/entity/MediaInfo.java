package com.tangxiaolv.telegramgallery.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaInfo implements Parcelable {
    //媒体类型：image/png, video/mp4 ...
    private String mimeType;
    //本地路径
    private String path;
    //Bytes
    private long size;

    //
    //图片
    //

    //略缩图地址
    private String thumbPath;

    //
    //视频
    //

    //名称
    private String title;
    //时长:秒
    private int videoDuration;
    //px
    private int width;
    //px
    private int height;
    //角度
    private int rotation;

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public void setThumbPath(String thumbPath) {
        this.thumbPath = thumbPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(int videoDuration) {
        this.videoDuration = videoDuration;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mimeType);
        dest.writeString(this.path);
        dest.writeLong(this.size);
        dest.writeString(this.thumbPath);
        dest.writeString(this.title);
        dest.writeInt(this.videoDuration);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeInt(this.rotation);
    }

    public MediaInfo(String mimeType, String path, long size, String thumbPath, String title, int videoDuration, int width, int height, int rotation) {
        this.mimeType = mimeType;
        this.path = path;
        this.size = size;
        this.thumbPath = thumbPath;
        this.title = title;
        this.videoDuration = videoDuration;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    public MediaInfo() {
    }

    protected MediaInfo(Parcel in) {
        this.mimeType = in.readString();
        this.path = in.readString();
        this.size = in.readLong();
        this.thumbPath = in.readString();
        this.title = in.readString();
        this.videoDuration = in.readInt();
        this.width = in.readInt();
        this.height = in.readInt();
        this.rotation = in.readInt();
    }

    public static final Creator<MediaInfo> CREATOR = new Creator<MediaInfo>() {
        public MediaInfo createFromParcel(Parcel source) {
            return new MediaInfo(source);
        }

        public MediaInfo[] newArray(int size) {
            return new MediaInfo[size];
        }
    };
}
