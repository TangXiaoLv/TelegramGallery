
package com.tangxiaolv.telegramgallery;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tangxiaolv.telegramgallery.actionbar.ActionBar;
import com.tangxiaolv.telegramgallery.actionbar.ActionBarMenuItem;
import com.tangxiaolv.telegramgallery.actionbar.BaseFragment;
import com.tangxiaolv.telegramgallery.components.PhotoPickerAlbumsCell;
import com.tangxiaolv.telegramgallery.tl.FileLocation;
import com.tangxiaolv.telegramgallery.utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.utils.LocaleController;
import com.tangxiaolv.telegramgallery.utils.MediaController;
import com.tangxiaolv.telegramgallery.utils.NotificationCenter;
import com.tangxiaolv.telegramgallery.entity.MediaInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.tangxiaolv.telegramgallery.GalleryActivity.getConfig;
import static com.tangxiaolv.telegramgallery.utils.Constants.DARK_THEME;
import static com.tangxiaolv.telegramgallery.utils.VideoUtils.createDecoder;
import static com.tangxiaolv.telegramgallery.utils.VideoUtils.destroyDecoder;

public class PhotoAlbumPickerActivity extends BaseFragment
        implements NotificationCenter.NotificationCenterDelegate {

    interface PhotoAlbumPickerActivityDelegate {
        void didSelectMedia(ArrayList<MediaInfo> medias);
    }

    private ArrayList<MediaController.AlbumEntry> albumsSorted = null;
    private ArrayList<MediaController.AlbumEntry> videoAlbumsSorted = null;
    private LinkedHashMap<Integer, MediaController.PhotoEntry> selectedPhotos =
            new LinkedHashMap<>(getConfig().getLimitPickPhoto());
    private boolean loading = false;

    private int columnsCount = 2;
    private ListView listView;
    private ListAdapter listAdapter;
    private FrameLayout progressView;
    private TextView emptyView;
    private TextView dropDown;
    private ActionBarMenuItem dropDownContainer;
    // private PickerBottomLayout pickerBottomLayout;
    private boolean sendPressed;
    private boolean singlePhoto;
    private boolean allowGifs;
    private int selectedMode;
    private int limitPickPhoto;

    private PhotoAlbumPickerActivityDelegate delegate;
    private PhotoPickerActivity currentPhotoPickerActivity;

    private final static int item_photos = 2;
    private final static int item_video = 3;

    //非预览中被选择的图片imageId:corner
    private final LinkedHashMap<Integer, Integer> unPreviewCheckeds = new LinkedHashMap<>(limitPickPhoto);

    public PhotoAlbumPickerActivity(boolean allowGifs) {
        super();
        this.limitPickPhoto = getConfig().getLimitPickPhoto();
        this.singlePhoto = getConfig().isSinglePhoto();
        this.allowGifs = allowGifs;
    }

    @Override
    public boolean onFragmentCreate() {
        loading = true;
        MediaController.loadGalleryPhotosAlbums(classGuid, getConfig().getFilterMimeTypes());
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.albumsDidLoaded);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.albumsDidLoaded);
        super.onFragmentDestroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View createView(Context context) {
        actionBar.setBackgroundColor(DARK_THEME ? Theme.ACTION_BAR_MEDIA_PICKER_COLOR : 0xfff9f9f9);
        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR);
        // actionBar.setBackButtonImage(R.drawable.album_ab_back);
        actionBar.setBackText(LocaleController.getString("Cancel", R.string.Cancel));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    if (delegate != null) {
                        finishFragment(false);
                    }
                } else if (id == item_photos) {
                    if (selectedMode == 0) {
                        return;
                    }
                    selectedMode = 0;
                    dropDown.setText(
                            LocaleController.getString("PickerPhotos", R.string.PickerPhotos));
                    emptyView.setText(LocaleController.getString("NoPhotos", R.string.NoPhotos));
                    listAdapter.notifyDataSetChanged();
                } else if (id == item_video) {
                    if (selectedMode == 1) {
                        return;
                    }
                    selectedMode = 1;
                    dropDown.setText(
                            LocaleController.getString("PickerVideo", R.string.PickerVideo));
                    emptyView.setText(LocaleController.getString("NoVideo", R.string.NoVideo));
                    listAdapter.notifyDataSetChanged();
                }
            }
        });

        fragmentView = new FrameLayout(context);

        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(DARK_THEME ? 0xff000000 : 0xffffffff);

        actionBar.setTitle(LocaleController.getString("Gallery", R.string.Gallery));

        listView = new ListView(context);
        listView.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4),
                AndroidUtilities.dp(4));
        listView.setClipToPadding(false);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setSelector(new ColorDrawable(0));
        listView.setDividerHeight(0);
        listView.setDivider(null);
        listView.setDrawingCacheEnabled(false);
        listView.setScrollingCacheEnabled(false);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView
                .getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        // layoutParams.bottomMargin = AndroidUtilities.dp(48);
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        AndroidUtilities.setListViewEdgeEffectColor(listView, 0xff333333);

        emptyView = new TextView(context);
        emptyView.setTextColor(0xff808080);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        emptyView.setText(LocaleController.getString("NoPhotos", R.string.NoPhotos));
        frameLayout.addView(emptyView);
        layoutParams = (FrameLayout.LayoutParams) emptyView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = AndroidUtilities.dp(48);
        emptyView.setLayoutParams(layoutParams);
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        progressView = new FrameLayout(context);
        progressView.setVisibility(View.GONE);
        frameLayout.addView(progressView);
        layoutParams = (FrameLayout.LayoutParams) progressView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = AndroidUtilities.dp(48);
        progressView.setLayoutParams(layoutParams);

        ProgressBar progressBar = new ProgressBar(context);
        progressView.addView(progressBar);
        layoutParams = (FrameLayout.LayoutParams) progressView.getLayoutParams();
        layoutParams.width = LayoutHelper.WRAP_CONTENT;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER;
        progressView.setLayoutParams(layoutParams);

        if (loading && (albumsSorted == null || albumsSorted.isEmpty())) {
            progressView.setVisibility(View.VISIBLE);
            listView.setEmptyView(null);
        } else {
            progressView.setVisibility(View.GONE);
            listView.setEmptyView(emptyView);
        }

        return fragmentView;
    }

    private void openPreview() {
        final List<Object> selectPhotos = computeSelectPhotos();
        if (selectPhotos != null) {
            PhotoViewer.getInstance().setParentActivity(getParentActivity());
            PhotoViewer.getInstance().openPhotoForSelect(selectPhotos, true, 0, new CustomProvider(selectPhotos));
        }
    }

    public class CustomProvider extends PhotoViewer.PreviewEmptyPhotoViewerProvider {

        private MediaController.PhotoEntry[] originArr;
        private MediaController.PhotoEntry[] selectArr;
        private MediaController.PhotoEntry[] removedArr;

        public CustomProvider(List<Object> selectPhotos) {
            int size = selectPhotos.size();
            originArr = new MediaController.PhotoEntry[size];
            selectArr = new MediaController.PhotoEntry[size];
            removedArr = new MediaController.PhotoEntry[size];
            for (int i = 0; i < size; i++) {
                selectArr[i] = (MediaController.PhotoEntry) selectPhotos.get(i);
            }
            System.arraycopy(selectArr, 0, originArr, 0, size);
        }

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(FileLocation fileLocation, int index) {
            MediaController.PhotoEntry entry = originArr[index];
            PhotoViewer.PlaceProviderObject cell = currentPhotoPickerActivity.getPlaceForPhoto(entry.imageId);
            if (cell == null) {
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = 0;
                object.viewY = 0;
                object.imageReceiver = new ImageReceiver();
                object.parentView = listView;
                return object;
            }
            return cell;
        }

        @Override
        public void willSwitchFromPhoto(FileLocation fileLocation, int index) {
            currentPhotoPickerActivity.willSwitchFromPhoto(fileLocation, index);
        }

        @Override
        public Bitmap getThumbForPhoto(FileLocation fileLocation, int index) {
            return currentPhotoPickerActivity.getThumbForPhoto(fileLocation, index);
        }

        @Override
        public void willHidePhotoViewer() {
            currentPhotoPickerActivity.willHidePhotoViewer();
        }

        @Override
        public void openPreview() {
            PhotoAlbumPickerActivity.this.openPreview();
        }

        @Override
        public boolean isPhotoChecked(int index) {
            return selectArr[index] != null;
        }

        @Override
        public void sendButtonPressed(int index) {
            selectedPhotos.clear();
            for (MediaController.PhotoEntry photoEntry : selectArr) {
                if (photoEntry != null) {
                    selectedPhotos.put(photoEntry.imageId, photoEntry);
                }
            }
            sendSelectedPhotos();
            getParentActivity().finish();
        }

        @Override
        public int getSelectedCount() {
            return getRealCount();
        }

        @Override
        public boolean checkboxEnable() {
            return getRealCount() <= limitPickPhoto;
        }

        @Override
        public int getCheckeCorner(int index) {
            return index + 1;
        }

        @Override
        public void selectChanged(int index, boolean checked) {
            MediaController.PhotoEntry photoEntry;
            if (checked) {
                selectArr[index] = removedArr[index];
                photoEntry = removedArr[index];
                System.arraycopy(removedArr, index, selectArr, index, 1);
                removedArr[index] = null;
            } else {
                removedArr[index] = selectArr[index];
                photoEntry = selectArr[index];
                System.arraycopy(selectArr, index, removedArr, index, 1);
                selectArr[index] = null;
            }
            currentPhotoPickerActivity.setPhotoCheckedByImageId(photoEntry);
        }

        @Override
        public void setIsOriginal(boolean isOriginal) {
            currentPhotoPickerActivity.setIsOriginal(isOriginal);
        }

        @Override
        public List<Object> getSelectedPhotos() {
            return computeSelectPhotos();
        }

        private int getRealCount() {
            return selectedPhotos.size();
        }
    }

    private List<Object> computeSelectPhotos() {
        int size = selectedPhotos.size();
        if (size > 0) {
            ArrayList<Object> result = new ArrayList<>(limitPickPhoto);
            result.addAll(selectedPhotos.values());
            return result;
        }
        return null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dropDownContainer != null) {
            dropDownContainer.closeSubMenu();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        fixLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.albumsDidLoaded) {
            int guid = (Integer) args[0];
            if (classGuid == guid) {
                albumsSorted = (ArrayList<MediaController.AlbumEntry>) args[1];
                videoAlbumsSorted = (ArrayList<MediaController.AlbumEntry>) args[3];
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
                if (listView != null && listView.getEmptyView() == null) {
                    listView.setEmptyView(emptyView);
                }
                if (listAdapter != null) {
                    // listAdapter.notifyDataSetChanged();
                }
                loading = false;
            }

            if (null != albumsSorted && albumsSorted.size() > 0) {
                openPhotoPicker(albumsSorted.get(0), 0, true);
            }
        }
    }

    public void setDelegate(PhotoAlbumPickerActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private void sendSelectedPhotos() {
        if (selectedPhotos.isEmpty() && delegate == null || sendPressed) {
            return;
        }
        sendPressed = true;
        ArrayList<MediaInfo> medias = new ArrayList<>();
        List<Object> selects = computeSelectPhotos();
        if (selects == null) {
            return;
        }

        int[] metaData = new int[4];
        for (Object entry : selects) {
            MediaController.PhotoEntry media = (MediaController.PhotoEntry) entry;
            MediaInfo info = new MediaInfo();
            if (media.imagePath != null) {
                info.setPath(media.imagePath);
            } else if (media.path != null) {
                info.setPath(media.path);
            }
            info.setSize(media.size);
            info.setVideoDuration(media.duration);
            info.setMimeType(media.mimeType);
            info.setThumbPath(media.thumbPath);
            info.setTitle(media.title);
            if (media.mimeType.contains("video")){
                int ptr = createDecoder(info.getPath(), metaData);
                destroyDecoder(ptr);
                info.setWidth(metaData[0]);
                info.setHeight(metaData[1]);
                info.setRotation(metaData[2]);
            }
            medias.add(info);
        }
        delegate.didSelectMedia(medias);
        AndroidUtilities.cancelToast();
    }

    private void fixLayout() {
        if (listView != null) {
            ViewTreeObserver obs = listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    fixLayoutInternal();
                    if (listView != null) {
                        listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
        }
    }

    private void fixLayoutInternal() {
        if (getParentActivity() == null) {
            return;
        }

        WindowManager manager = (WindowManager) Gallery.applicationContext
                .getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();
        columnsCount = 2;
        if (!AndroidUtilities.isTablet()
                && (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90)) {
            columnsCount = 4;
        }
        listAdapter.notifyDataSetChanged();

        if (dropDownContainer != null) {
            if (!AndroidUtilities.isTablet()) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) dropDownContainer
                        .getLayoutParams();
                layoutParams.topMargin = (Build.VERSION.SDK_INT >= 21
                        ? AndroidUtilities.statusBarHeight : 0);
                dropDownContainer.setLayoutParams(layoutParams);
            }

            if (!AndroidUtilities.isTablet() && Gallery.applicationContext.getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                dropDown.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            } else {
                dropDown.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            }
        }
    }

    private void openPhotoPicker(MediaController.AlbumEntry albumEntry, int type,
                                 boolean withAnim) {
        currentPhotoPickerActivity = new PhotoPickerActivity(type, albumEntry, selectedPhotos, singlePhoto);
        currentPhotoPickerActivity.setDelegate(new PhotoPickerActivity.PhotoPickerActivityDelegate() {
            //预览中已选择集合，-1为未被选择index:corner
            private final int[] previewCheckeds = new int[limitPickPhoto];

            @Override
            public void actionButtonPressed(boolean canceled) {
                if (!canceled) {
                    sendSelectedPhotos();
                } else if (getParentActivity() != null) {
                    getParentActivity().finish();
                }
                removeSelfFromStack();
            }

            @Override
            public boolean didSelectVideo(String path) {
                removeSelfFromStack();
                return false;
            }

            @Override
            public int getCheckboxTag(int imageId) {
                //返回角标，未选中为-1
                Integer cornerIndex = unPreviewCheckeds.get(imageId);
                return cornerIndex == null ? -1 : cornerIndex;
            }

            @Override
            public void putCheckboxTag(int imageId, int cornerIndex) {
                if (PhotoViewer.getInstance().isInPreviewMode()) {
                    previewCheckeds[cornerIndex - 1] = cornerIndex;
                }
                unPreviewCheckeds.put(imageId, cornerIndex);
            }

            @Override
            public void removeCheckboxTag(int imageId) {
                Integer remove = unPreviewCheckeds.remove(imageId);
                if (remove != null && PhotoViewer.getInstance().isInPreviewMode()) {

                    previewCheckeds[remove - 1] = -1;
                }
                int corner = 1;
                for (Integer id : unPreviewCheckeds.keySet()) {
                    unPreviewCheckeds.put(id, corner);
                    corner++;
                }
            }

            @Override
            public int generateCheckCorner() {
                if (PhotoViewer.getInstance().isInPreviewMode()) {
                    for (int i = 0; i < limitPickPhoto; i++) {
                        if (previewCheckeds[i] <= 0) {
                            return i + 1;
                        }
                    }
                    return -1;
                }
                return unPreviewCheckeds.size() + 1;
            }

            @Override
            public void openPreview() {
                PhotoAlbumPickerActivity.this.openPreview();
            }

            @Override
            public PhotoAlbumPickerActivity getPhotoAlbumPickerActivity() {
                return PhotoAlbumPickerActivity.this;
            }
        });
        presentFragment(currentPhotoPickerActivity, false, withAnim);
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public int getCount() {
            if (singlePhoto || selectedMode == 0) {
                return albumsSorted != null ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0;
                // return 1 + (albumsSorted != null
                // ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0);
            } else {
                return (videoAlbumsSorted != null ? (int) Math.ceil(videoAlbumsSorted.size() / (float) columnsCount) : 0);
            }
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);
            if (type == 0) {
                PhotoPickerAlbumsCell photoPickerAlbumsCell;
                if (view == null) {
                    view = new PhotoPickerAlbumsCell(mContext);
                    photoPickerAlbumsCell = (PhotoPickerAlbumsCell) view;
                    photoPickerAlbumsCell.setDelegate(
                            new PhotoPickerAlbumsCell.PhotoPickerAlbumsCellDelegate() {
                                @Override
                                public void didSelectAlbum(MediaController.AlbumEntry albumEntry) {
                                    openPhotoPicker(albumEntry, 0, false);
                                }
                            });
                } else {
                    photoPickerAlbumsCell = (PhotoPickerAlbumsCell) view;
                }
                photoPickerAlbumsCell.setAlbumsCount(columnsCount);
                for (int a = 0; a < columnsCount; a++) {
                    int index;
                    // if ( singlePhoto || selectedMode == 1) {
                    index = i * columnsCount + a;
                    // } else {
                    // index = (i - 1) * columnsCount + a;
                    // }
                    if (singlePhoto || selectedMode == 0) {
                        if (index < albumsSorted.size()) {
                            MediaController.AlbumEntry albumEntry = albumsSorted.get(index);
                            photoPickerAlbumsCell.setAlbum(a, albumEntry);
                        } else {
                            photoPickerAlbumsCell.setAlbum(a, null);
                        }
                    } else {
                        if (index < videoAlbumsSorted.size()) {
                            MediaController.AlbumEntry albumEntry = videoAlbumsSorted.get(index);
                            photoPickerAlbumsCell.setAlbum(a, albumEntry);
                        } else {
                            photoPickerAlbumsCell.setAlbum(a, null);
                        }
                    }
                }
                photoPickerAlbumsCell.requestLayout();
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (singlePhoto || selectedMode == 1) {
                return 0;
            }
            // if (i == 0) {
            // return 1;
            // }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            if (singlePhoto || selectedMode == 1) {
                return 1;
            }
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }
    }
}
