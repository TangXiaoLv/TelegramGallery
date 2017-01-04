# TelegramGallery
English | [中文](https://github.com/TangXiaoLv/TelegramGallery/blob/master/README_CN.md) 

Fast，efficiently，low memory selector of album,extract from [Telegram](https://github.com/DrKLO/Telegram). Support singleSelection, Multiselect, photo preview，scalable ,sliding to quit preview，QQ pick style.

<img src="png/1.gif" height= "528" width="320">

##Getting Started
###Gradle
```
dependencies {
    compile 'com.library.tangxiaolv:telegramgallery:1.0.3'
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
//open album
GalleryConfig config = new GalleryConfig.Build()
                        .limitPickPhoto(3)
                        .singlePhoto(false)
                        .hintOfPick("this is pick hint")
                        .filterMimeTypes(new String[]{"image/jpeg"})
                        .build();
GalleryActivity.openActivity(MainActivity.this, reqCode, config);

//process result
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {

	//list of photos of seleced
    List<String> photos = (List<String>) data.getSerializableExtra(GalleryActivity.PHOTOS);

	//list of videos of seleced
	List<String> vides = (List<String>) data.getSerializableExtra(GalleryActivity.VIDEOS);
}
```
##License
GPL-2.0
