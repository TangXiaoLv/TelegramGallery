
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
 * receive {@link java.util.List<String>} of photo path or video path by
 * {@link GalleryActivity#PHOTOS} or {@link GalleryActivity#VIDEO} in
 * {@link Activity#onActivityResult}
 */
public class GalleryActivity extends Activity implements ActionBarLayout.ActionBarLayoutDelegate {

    public static final String PHOTOS = "PHOTOS";
    public static final String VIDEO = "VIDEOS";

    private static final String GALLERY_CONFIG = "GALLERY_CONFIG";

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
        GalleryConfig config = intent.getParcelableExtra(GALLERY_CONFIG);
        albumPickerActivity = new PhotoAlbumPickerActivity(
            config.getFilterMimeTypes(),
            config.getLimitPickPhoto(),
            config.isSinglePhoto(),
            config.getHintOfPick(),
            false,
            config.getLimitReachedIntent());
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
     * open gallery
     * 
     * @param activity parent activity
     * @param requestCode {@link Activity#onActivityResult}
     * @param config {@link GalleryConfig}
     */
    public static void openActivity(Activity activity, int requestCode, GalleryConfig config) {
        Intent intent = new Intent(activity, GalleryActivity.class);
        intent.putExtra(GALLERY_CONFIG, config);
        activity.startActivityForResult(intent, requestCode);
    }

    @Deprecated
    public static void openActivity(
            Activity activity,
            String[] filterMimeTypes,
            boolean singlePhoto,
            int limitPickPhoto,
            int requestCode) {
        GalleryConfig.Build build = new GalleryConfig.Build();
        build.filterMimeTypes(filterMimeTypes)
                .singlePhoto(singlePhoto)
                .limitPickPhoto(limitPickPhoto);
        openActivity(activity, requestCode, build.build());
    }

    @Deprecated
    public static void openActivity(Activity activity, boolean singlePhoto, int limitPickPhoto,
            int requestCode) {
        openActivity(activity, null, singlePhoto, limitPickPhoto, requestCode);
    }

    @Deprecated
    public static void openActivity(Activity activity, boolean singlePhoto, int requestCode) {
        openActivity(activity, null, singlePhoto, 1, requestCode);
    }
}
