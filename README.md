# TelegramGallery
English | [中文](https://github.com/TangXiaoLv/TelegramGallery/blob/master/README_CN.md) 

Fast，efficiently，low memory selector of album,extract from [Telegram](https://github.com/DrKLO/Telegram). Support singleSelection, Multiselect, photo preview，scalable ,sliding to quit preview，QQ pick style.

<img src="png/1.gif" height= "528" width="320">

##Getting Started
###configuration
```
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

	<activity android:name="com.tangxiaolv.telegramgallery.GalleryActivity" />
```
###Usage
```
//open album
GalleryActivity.openActivity(
            Activity activity,
            //Filter the specified type， Follow the rule of standard of mime type 。eg：new String[]{"image/gif","image/png"}
            String[] filterMimeTypes,
            //true：singleSelection，false： Multiselect
            boolean singlePhoto,
            //limit for pick photo nums，when singlePhoto=false,the param is available
            int limitPickPhoto,
            int requestCode)

//or
GalleryActivity.openActivity(Activity activity, boolean singlePhoto, int limitPickPhoto,int requestCode)

//or
GalleryActivity.openActivity(Activity activity, boolean singlePhoto, int requestCode)

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
```GPL-2.0```