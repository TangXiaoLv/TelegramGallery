
package com.tangxiaolv.telegramgallery;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.TextUtils;
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

import com.tangxiaolv.telegramgallery.Actionbar.ActionBar;
import com.tangxiaolv.telegramgallery.Actionbar.ActionBarMenu;
import com.tangxiaolv.telegramgallery.Actionbar.ActionBarMenuItem;
import com.tangxiaolv.telegramgallery.Actionbar.BaseFragment;
import com.tangxiaolv.telegramgallery.Components.PhotoPickerAlbumsCell;
import com.tangxiaolv.telegramgallery.Components.PhotoPickerSearchCell;
import com.tangxiaolv.telegramgallery.Utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.Utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.Utils.LocaleController;
import com.tangxiaolv.telegramgallery.Utils.MediaController;
import com.tangxiaolv.telegramgallery.Utils.NotificationCenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PhotoAlbumPickerActivity extends BaseFragment
        implements NotificationCenter.NotificationCenterDelegate {

    public interface PhotoAlbumPickerActivityDelegate {
        void didSelectPhotos(ArrayList<String> photos, ArrayList<String> captions);

        boolean didSelectVideo(String path);

        void startPhotoSelectActivity();
    }

    public static int limitPickPhoto;
    public static boolean DarkTheme = true;

    private ArrayList<MediaController.AlbumEntry> albumsSorted = null;
    private ArrayList<MediaController.AlbumEntry> videoAlbumsSorted = null;
    private HashMap<Integer, MediaController.PhotoEntry> selectedPhotos = new HashMap<>();
    private HashMap<Integer, Integer> checkboxTag = new HashMap<>();
    private List<MediaController.PhotoEntry> selectedPhotosSortEnd = new ArrayList<>();
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
    private final String[] filterMimeTypes;

    private final int[] imageCheckIndexArr;

    private PhotoAlbumPickerActivityDelegate delegate;
    private PhotoPickerActivity currentPhotoPickerActivity;

    private final static int item_photos = 2;
    private final static int item_video = 3;

    public PhotoAlbumPickerActivity(String[] filterMimeTypes, int limitPick, boolean singlePhoto,
            boolean allowGifs) {
        super();
        limitPickPhoto = limitPick;
        this.filterMimeTypes = filterMimeTypes;
        this.imageCheckIndexArr = new int[limitPick];
        this.singlePhoto = singlePhoto;
        this.allowGifs = allowGifs;
    }

    @Override
    public boolean onFragmentCreate() {
        loading = true;
        MediaController.loadGalleryPhotosAlbums(classGuid, filterMimeTypes);
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
        actionBar.setBackgroundColor(Theme.ACTION_BAR_MEDIA_PICKER_COLOR);
        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR);
        // actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setBackText(LocaleController.getString("Cancel", R.string.Cancel));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    if (delegate != null) {
                        finishFragment(false);
                        delegate.startPhotoSelectActivity();
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
        frameLayout.setBackgroundColor(DarkTheme ? 0xff000000 : 0xffffffff);
        //==============videos pick====================
        if (!singlePhoto) {
            selectedMode = 0;

            ActionBarMenu menu = actionBar.createMenu();
            dropDownContainer = new ActionBarMenuItem(context, menu, 0);
            dropDownContainer.setSubMenuOpenSide(1);
            dropDownContainer.addSubItem(item_photos,
                    LocaleController.getString("PickerPhotos", R.string.PickerPhotos), 0);
            dropDownContainer.addSubItem(item_video,
                    LocaleController.getString("PickerVideo", R.string.PickerVideo), 0);
            actionBar.addView(dropDownContainer);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) dropDownContainer
                    .getLayoutParams();
            layoutParams.height = LayoutHelper.MATCH_PARENT;
            layoutParams.width = LayoutHelper.WRAP_CONTENT;
            // layoutParams.rightMargin = AndroidUtilities.dp(40);
            // layoutParams.leftMargin = AndroidUtilities.getRealScreenSize().x / 2;
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            dropDownContainer.setLayoutParams(layoutParams);
            dropDownContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dropDownContainer.toggleSubMenu();
                }
            });

            dropDown = new TextView(context);
            dropDown.setGravity(Gravity.LEFT);
            dropDown.setSingleLine(true);
            dropDown.setLines(1);
            dropDown.setMaxLines(1);
            dropDown.setEllipsize(TextUtils.TruncateAt.END);
            dropDown.setTextColor(0xffffffff);
            // dropDown.getPaint().setFakeBoldText(true);
            dropDown.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down,
                    0);
            dropDown.setCompoundDrawablePadding(AndroidUtilities.dp(4));
//            dropDown.setPadding(0, 0, AndroidUtilities.dp(10), 0);
            dropDown.setText(LocaleController.getString("PickerPhotos", R.string.PickerPhotos));
            dropDownContainer.addView(dropDown);
            layoutParams = (FrameLayout.LayoutParams) dropDown.getLayoutParams();
            layoutParams.width = LayoutHelper.WRAP_CONTENT;
            layoutParams.height = LayoutHelper.WRAP_CONTENT;
//            layoutParams.leftMargin = AndroidUtilities.dp(dropDown.getTextSize());
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            dropDown.setLayoutParams(layoutParams);
        } else {
            actionBar.setTitle(LocaleController.getString("Album", R.string.Album));
        }

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
        emptyView.setTextSize(20);
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

        // pickerBottomLayout = new PickerBottomLayout(context);
        // pickerBottomLayout.cancelButton.setVisibility(singlePhoto ? View.GONE : View.VISIBLE);
        // frameLayout.addView(pickerBottomLayout);
        // layoutParams = (FrameLayout.LayoutParams) pickerBottomLayout.getLayoutParams();
        // layoutParams.width = LayoutHelper.MATCH_PARENT;
        // layoutParams.height = AndroidUtilities.dp(48);
        // layoutParams.gravity = Gravity.BOTTOM;
        // pickerBottomLayout.setLayoutParams(layoutParams);
        // pickerBottomLayout.cancelButton.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View view) {
        // openPreview();
        // }
        // });
        // pickerBottomLayout.doneButton.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View view) {
        // sendSelectedPhotos();
        // finishFragment();
        // }
        // });

        if (loading && (albumsSorted == null || albumsSorted != null && albumsSorted.isEmpty())) {
            progressView.setVisibility(View.VISIBLE);
            listView.setEmptyView(null);
        } else {
            progressView.setVisibility(View.GONE);
            listView.setEmptyView(emptyView);
        }

        // pickerBottomLayout.updateSelectedCount(selectedPhotos.size() + selectedWebPhotos.size(),
        // true);

        return fragmentView;
    }

    public void openPreview() {
        final List<Object> selectPhotos = computeSelectPhotos();
        if (selectPhotos != null) {
            PhotoViewer.getInstance().setParentActivity(getParentActivity());
            PhotoViewer.getInstance().openPhotoForSelect(selectPhotos, true, 0,
                    singlePhoto ? 1 : 0, new CustomProvider(selectPhotos));
        }
    }

    public class CustomProvider extends PhotoViewer.PreviewEmptyPhotoViewerProvider {

        private MediaController.PhotoEntry[] selectArr;
        private MediaController.PhotoEntry[] removedArr;

        public CustomProvider(List<Object> selectPhotos) {
            int size = selectPhotos.size();
            selectArr = new MediaController.PhotoEntry[size];
            removedArr = new MediaController.PhotoEntry[size];
            for (int i = 0; i < size; i++) {
                selectArr[i] = (MediaController.PhotoEntry) selectPhotos.get(i);
            }
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
            for (int i = 0; i < selectArr.length; i++) {
                MediaController.PhotoEntry photoEntry = selectArr[i];
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
        public void previewExit() {
            super.previewExit();
        }

        private int getRealCount() {
            int count = 0;
            for (int i = 0; i < selectArr.length; i++) {
                if (selectArr[i] != null) {
                    count++;
                }
            }
            return count;
        }
    }

    public List<Object> computeSelectPhotos() {
        int size = selectedPhotos.size();
        if (size > 0) {
            Object[] obj = new Object[limitPickPhoto];
            for (Integer i : selectedPhotos.keySet()) {
                MediaController.PhotoEntry entry = selectedPhotos.get(i);
                obj[entry.sortindex - 1] = entry;
            }
            selectedPhotosSortEnd.clear();
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < limitPickPhoto; i++) {
                Object o = obj[i];
                if (o != null) {
                    list.add(o);
                    selectedPhotosSortEnd.add((MediaController.PhotoEntry) o);
                }
            }

            return list;
        }
        return null;
    }

    private void fillSelectedPhotosSortEnd() {
        Object[] obj = new Object[limitPickPhoto];
        for (Integer i : selectedPhotos.keySet()) {
            MediaController.PhotoEntry entry = selectedPhotos.get(i);
            obj[entry.sortindex - 1] = entry;
        }
        selectedPhotosSortEnd.clear();
        for (int i = 0; i < limitPickPhoto; i++) {
            Object o = obj[i];
            if (o != null) {
                selectedPhotosSortEnd.add((MediaController.PhotoEntry) o);
            }
        }
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
        fillSelectedPhotosSortEnd();
        sendPressed = true;
        ArrayList<String> photos = new ArrayList<>();
        ArrayList<String> captions = new ArrayList<>();
        for (MediaController.PhotoEntry photoEntry : selectedPhotosSortEnd) {
            if (photoEntry.imagePath != null) {
                photos.add(photoEntry.imagePath);
                captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
            } else if (photoEntry.path != null) {
                photos.add(photoEntry.path);
                captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
            }
        }
        delegate.didSelectPhotos(photos, captions);
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
                dropDown.setTextSize(18);
            } else {
                dropDown.setTextSize(20);
            }
        }
    }

    private void openPhotoPicker(MediaController.AlbumEntry albumEntry, int type,
            boolean withAnim) {
        currentPhotoPickerActivity = new PhotoPickerActivity(type, limitPickPhoto, albumEntry,
                selectedPhotos, null, singlePhoto);
        currentPhotoPickerActivity
                .setDelegate(new PhotoPickerActivity.PhotoPickerActivityDelegate() {
                    @Override
                    public void selectedPhotosChanged() {
                        // if (pickerBottomLayout != null) {
                        // pickerBottomLayout.updateSelectedCount(
                        // selectedPhotos.size() + selectedWebPhotos.size(), true);
                        // }
                    }

                    @Override
                    public void actionButtonPressed(boolean canceled) {
                        if (!canceled) {
                            sendSelectedPhotos();
                        } else {
                            getParentActivity().finish();
                        }
                        removeSelfFromStack();
                    }

                    @Override
                    public boolean didSelectVideo(String path) {
                        removeSelfFromStack();
                        return delegate.didSelectVideo(path);
                    }

                    @Override
                    public int getCheckboxTag(int imageId) {
                        Integer cornerIndex = checkboxTag.get(imageId);
                        return cornerIndex == null ? -1 : cornerIndex;
                    }

                    @Override
                    public void putCheckboxTag(int imageId, int cornerIndex) {
                        imageCheckIndexArr[cornerIndex - 1] = cornerIndex;
                        checkboxTag.put(imageId, cornerIndex);
                    }

                    @Override
                    public void removeCheckboxTag(int imageId) {
                        Integer remove = checkboxTag.remove(imageId);
                        if (remove != null) {
                            imageCheckIndexArr[remove - 1] = -1;
                        }
                    }

                    @Override
                    public int generateCheckCorner() {
                        int length = imageCheckIndexArr.length;
                        for (int i = 0; i < length; i++) {
                            if (imageCheckIndexArr[i] <= 0) {
                                return i + 1;
                            }
                        }
                        return -1;
                    }

                    @Override
                    public void openPreview() {
                        PhotoAlbumPickerActivity.this.openPreview();
                    }
                });
        presentFragment(currentPhotoPickerActivity, false, withAnim);
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
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
                int count = albumsSorted != null
                        ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0;
                return count;
                // return 1 + (albumsSorted != null
                // ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0);
            } else {
                return (videoAlbumsSorted != null
                        ? (int) Math.ceil(videoAlbumsSorted.size() / (float) columnsCount) : 0);
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
            } else if (type == 1) {
                // 图片搜索
                if (view == null) {
                    view = new PhotoPickerSearchCell(mContext, allowGifs);
                    ((PhotoPickerSearchCell) view)
                            .setDelegate(new PhotoPickerSearchCell.PhotoPickerSearchCellDelegate() {
                                @Override
                                public void didPressedSearchButton(int index) {
                                    openPhotoPicker(null, index, false);
                                }
                            });
                }
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
