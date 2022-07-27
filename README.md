# TelegramGallery
Languages: English | [中文](https://github.com/TangXiaoLv/TelegramGallery/blob/master/README_CN.md) 

## Features
Fast, efficient, and low memory cost photo selector. Extracted from [Telegram](https://github.com/DrKLO/Telegram). 

Supported functions: 
- Single and multiple selection of photos
- Photo preview
- Scaling photos 
- Slide to quit preview 
- QQ picking style

### Demo
<img src="png/1.gif" height= "528" width="320">

## Getting Started
### Gradle
```
dependencies {
    compile 'com.library.tangxiaolv:telegramgallery:1.0.5'
}
```

### Configuration
```
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    
    <activity android:name="com.tangxiaolv.telegramgallery.GalleryActivity" />
```
### Usage
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
## License
GPL-2.0
