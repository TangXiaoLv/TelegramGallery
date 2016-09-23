# TelegramGallery
中文 | [English](https://github.com/TangXiaoLv/TelegramGallery)

快速，高效，低耗相册选择器，抽取自[Telegram](https://github.com/DrKLO/Telegram)，支持单选，多选，预览，缩放，滑动取消预览，QQ选择特性

<img src="png/1.gif" height= "528" width="320">

##Getting Started
###Gradle
```
dependencies {
    compile 'com.library.tangxiaolv:telegramgallery:1.0.1'
}
```

###configuration
```
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

	<activity android:name="com.tangxiaolv.telegramgallery.GalleryActivity" />
```
###Usage
```
//打开相册
方式一：
GalleryActivity.openActivity(
            Activity activity,
            String[] filterMimeTypes,//过滤掉指定类型，遵守MIME Type类型规范。eg：new String[]{"image/gif","image/png"}
            boolean singlePhoto,//true 单选，false 多选
            int limitPickPhoto,//图片可选数量限制，当singlePhoto=false时生效
            int requestCode)//请求码

方式二：
GalleryActivity.openActivity(Activity activity, boolean singlePhoto, int limitPickPhoto,int requestCode)

方式三：
GalleryActivity.openActivity(Activity activity, boolean singlePhoto, int requestCode)

//接受返回值
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	//照片路径集合返回值
    List<String> photos = (List<String>) data.getSerializableExtra(GalleryActivity.PHOTOS);

	//视频路径集合返回值
	List<String> vides = (List<String>) data.getSerializableExtra(GalleryActivity.VIDEOS);
}
```
##License
```GPL-2.0```
