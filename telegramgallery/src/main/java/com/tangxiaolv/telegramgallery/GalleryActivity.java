
package com.tangxiaolv.telegramgallery;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tangxiaolv.telegramgallery.actionbar.ActionBarLayout;
import com.tangxiaolv.telegramgallery.actionbar.BaseFragment;
import com.tangxiaolv.telegramgallery.entity.MediaInfo;
import com.tangxiaolv.telegramgallery.utils.GalleryImageLoader;

import java.util.ArrayList;

/**
 * receive {@link java.util.List<String>} of photo path or video path by
 * {@link GalleryActivity#PHOTOS} or {@link GalleryActivity#VIDEO} in
 * {@link Activity#onActivityResult}
 */
public class GalleryActivity extends Activity implements ActionBarLayout.ActionBarLayoutDelegate {

    public static final String PHOTOS = "PHOTOS";
    public static final String IS_ORIGINAL = "IS_ORIGINAL";
    public static final String VIDEO = "VIDEOS";
    public static final String MEDIA_INFO = "MEDIA_INFO";

    private static final String GALLERY_CONFIG = "GalleryConfig";
    private static GalleryConfig CONFIG;

    private ArrayList<BaseFragment> mainFragmentsStack = new ArrayList<>();
    private ActionBarLayout actionBarLayout;
    private PhotoAlbumPickerActivity albumPickerActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        if (getIntent() == null || getIntent().getSerializableExtra(GALLERY_CONFIG) == null) {
            return;
        }
        Gallery.init(getApplication());
        CONFIG = (GalleryConfig) getIntent().getSerializableExtra(GALLERY_CONFIG);

        FrameLayout main = (FrameLayout) findViewById(R.id.mian);
        actionBarLayout = new ActionBarLayout(this);
        main.addView(actionBarLayout);
        actionBarLayout.init(mainFragmentsStack);
        actionBarLayout.setDelegate(this);

        String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
        if (checkCallingOrSelfPermission(
                READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{
                        READ_EXTERNAL_STORAGE
                }, 1);
                return;
            }
        }
        showContent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        showContent();
        if (grantResults.length > 0) {
            Toast.makeText(this, R.string.album_read_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void showContent() {
        albumPickerActivity = new PhotoAlbumPickerActivity(true);
        albumPickerActivity.setDelegate(mPhotoAlbumPickerActivityDelegate);
        actionBarLayout.presentFragment(albumPickerActivity, false, true, true);
    }

    private PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate mPhotoAlbumPickerActivityDelegate = new PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate() {

        @Override
        public void didSelectMedia(ArrayList<MediaInfo> medias) {
            if (medias.size() > 0) {
                ArrayList<Object> paths = new ArrayList<>();
                for (MediaInfo info : medias) {
                    paths.add(info.getPath());
                }
                Intent intent = new Intent();
                intent.putExtra(PHOTOS, paths);
                intent.putExtra(IS_ORIGINAL, Gallery.sOriginChecked);
                intent.putExtra(MEDIA_INFO, medias);
                setResult(Activity.RESULT_OK, intent);
            }
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
        } else if (mainFragmentsStack.size() <= 2) {
            finish();
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
        if (null == actionBarLayout) {
            return;
        }
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
    public boolean needPresentFragment(BaseFragment fragment, boolean removeLast, boolean forceWithoutAnimation, ActionBarLayout layout) {
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
        super.onDestroy();
        PhotoViewer.getInstance().destroyPhotoViewer();
        GalleryImageLoader.getInstance().clearMemory();
        if (null != albumPickerActivity) {
            albumPickerActivity.removeSelfFromStack();
            albumPickerActivity = null;
        }
        if (null != mainFragmentsStack) {
            mainFragmentsStack.clear();
            mainFragmentsStack = null;
        }
        actionBarLayout = null;
        //相册状态量复位
        Gallery.sOriginChecked = false;
        Gallery.sListItemClickable = true;
    }

    /**
     * open gallery
     *
     * @param activity    parent activity
     * @param requestCode {@link Activity#onActivityResult}
     * @param config      {@link GalleryConfig}
     */
    public static void openActivity(Activity activity, int requestCode, GalleryConfig config) {
        Intent intent = new Intent(activity, GalleryActivity.class);
        intent.putExtra(GALLERY_CONFIG, config);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(
                config.getEnterAnim() != -1 ? config.getEnterAnim() : R.anim.push_bottom_in, 0);
        activity.overridePendingTransition(R.anim.push_bottom_in, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(
                0, CONFIG.getExitAnim() != -1 ? CONFIG.getExitAnim() : R.anim.push_bottom_out);
        overridePendingTransition(0, R.anim.push_bottom_out);
    }

    public static GalleryConfig getConfig() {
        return CONFIG;
    }
}
