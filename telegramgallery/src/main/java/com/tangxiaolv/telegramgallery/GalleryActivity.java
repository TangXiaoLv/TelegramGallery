
package com.tangxiaolv.telegramgallery;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.tangxiaolv.telegramgallery.Actionbar.ActionBarLayout;
import com.tangxiaolv.telegramgallery.Actionbar.BaseFragment;
import com.tangxiaolv.telegramgallery.Utils.ImageLoader;

import java.util.ArrayList;

/**
 * 在{@link Activity#onActivityResult}中 通过{@link GalleryActivity#PHOTOS}获取图片资源路径集合返回值(返回List
 * <String>)， 或通过 {@link GalleryActivity#VIDEO}获取视频资源返回值(返回单个视频的path)，
 */
public class GalleryActivity extends Activity implements ActionBarLayout.ActionBarLayoutDelegate {

    public static final String PHOTOS = "PHOTOS";
    public static final String VIDEO = "VIDEOS";

    private static final String SINGLE_PHOTO = "SINGLE_PHOTO";
    private static final String LIMIT_PICK_PHOTO = "LIMIT_PICK_PHOTO";
    private static final String HAS_CAMERA = "HAS_CAMERA";
    private static final String FILTER_MIME_TYPES = "FILTER_MIME_TYPES";

    private ArrayList<BaseFragment> mainFragmentsStack = new ArrayList<>();
    private ActionBarLayout actionBarLayout;
    private PhotoAlbumPickerActivity albumPickerActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        Gallery.init(getApplication());

        FrameLayout mian = (FrameLayout) findViewById(R.id.mian);
        actionBarLayout = new ActionBarLayout(this);
        mian.addView(actionBarLayout);
        actionBarLayout.init(mainFragmentsStack);
        actionBarLayout.setDelegate(this);

        String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
        if (checkCallingOrSelfPermission(
                READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[] {
                        READ_EXTERNAL_STORAGE
                }, 1);
                return;
            }
            //Toast.makeText(this, R.string.album_read_fail, Toast.LENGTH_SHORT).show();
        }
        showContent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        showContent();
    }

    private void showContent() {
        Intent intent = getIntent();
        boolean singlePhoto = intent.getBooleanExtra(SINGLE_PHOTO, false);
        boolean hasCamera = intent.getBooleanExtra(HAS_CAMERA, false);
        int limitPickPhoto = intent.getIntExtra(LIMIT_PICK_PHOTO, 9);
        String[] filterMimeTypes = intent.getStringArrayExtra(FILTER_MIME_TYPES);
        albumPickerActivity = new PhotoAlbumPickerActivity(filterMimeTypes, limitPickPhoto,
                singlePhoto, false);
        albumPickerActivity.setDelegate(mPhotoAlbumPickerActivityDelegate);
        actionBarLayout.presentFragment(albumPickerActivity, false, true, true);
    }

    private PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate mPhotoAlbumPickerActivityDelegate = new PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate() {
        @Override
        public void didSelectPhotos(ArrayList<String> photos, ArrayList<String> captions) {
            Intent intent = new Intent();
            intent.putExtra(PHOTOS, photos);
            setResult(Activity.RESULT_OK, intent);
        }

        @Override
        public boolean didSelectVideo(String path) {
            Intent intent = new Intent();
            intent.putExtra(VIDEO, path);
            setResult(Activity.RESULT_OK, intent);
            return true;
        }

        @Override
        public void startPhotoSelectActivity() {
        }
    };

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        actionBarLayout.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
        } else {
            actionBarLayout.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        actionBarLayout.onPause();
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        actionBarLayout.onResume();
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().onResume();
        }
    }

    @Override
    public boolean onPreIme() {
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean needPresentFragment(BaseFragment fragment, boolean removeLast,
            boolean forceWithoutAnimation, ActionBarLayout layout) {
        return true;
    }

    @Override
    public boolean needAddFragmentToStack(BaseFragment fragment, ActionBarLayout layout) {
        return true;
    }

    @Override
    public boolean needCloseLastFragment(ActionBarLayout layout) {
        if (layout.fragmentsStack.size() <= 1) {
            finish();
            return false;
        }
        return true;
    }

    @Override
    public void onRebuildAllFragments(ActionBarLayout layout) {
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            actionBarLayout.onKeyUp(keyCode, event);
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        PhotoViewer.getInstance().destroyPhotoViewer();
        ImageLoader.getInstance().clearMemory();
        albumPickerActivity.removeSelfFromStack();
        actionBarLayout.clear();
        mainFragmentsStack.clear();
        mainFragmentsStack = null;
        actionBarLayout = null;
        albumPickerActivity = null;
        super.onDestroy();
    }

    /**
     * 打开相册
     *
     * @param filterMimeTypes
     *            需要过滤掉的媒体文件类型，以MimeType标识：{http://www.w3school.com.cn/media/media_mimeref.asp}
     *            <span>eg:new String[]{"image/gif","image/jpeg"}<span/>
     * @param singlePhoto true:单选 false:多选
     * @param limitPickPhoto 照片选取限制
     * @param requestCode 请求码
     */
    public static void openActivity(
            Activity activity,
            String[] filterMimeTypes,
            boolean singlePhoto,
            int limitPickPhoto,
            int requestCode) {
        limitPickPhoto = singlePhoto ? 1 : limitPickPhoto > 0 ? limitPickPhoto : 1;
        Intent intent = new Intent(activity, GalleryActivity.class);
        intent.putExtra(SINGLE_PHOTO, singlePhoto);
        intent.putExtra(LIMIT_PICK_PHOTO, limitPickPhoto);
        intent.putExtra(FILTER_MIME_TYPES, filterMimeTypes);
        intent.putExtra(HAS_CAMERA, /* hasCamera */false);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void openActivity(Activity activity, boolean singlePhoto, int limitPickPhoto,
        int requestCode) {
        openActivity(activity,null, singlePhoto, limitPickPhoto, requestCode);
    }

    public static void openActivity(Activity activity, boolean singlePhoto, int requestCode) {
        openActivity(activity, null, singlePhoto, 1, requestCode);
    }
}