
package com.tangxiaolv.telegramgallery;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tangxiaolv.telegramgallery.actionbar.ActionBar;
import com.tangxiaolv.telegramgallery.actionbar.ActionBarMenu;
import com.tangxiaolv.telegramgallery.actionbar.BaseFragment;
import com.tangxiaolv.telegramgallery.components.BackupImageView;
import com.tangxiaolv.telegramgallery.components.PhotoPickerPhotoCell;
import com.tangxiaolv.telegramgallery.components.PickerBottomLayout;
import com.tangxiaolv.telegramgallery.tl.FileLocation;
import com.tangxiaolv.telegramgallery.utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.utils.Constants;
import com.tangxiaolv.telegramgallery.utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.utils.LocaleController;
import com.tangxiaolv.telegramgallery.utils.MediaController;
import com.tangxiaolv.telegramgallery.utils.NotificationCenter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tangxiaolv.telegramgallery.GalleryActivity.getConfig;
import static com.tangxiaolv.telegramgallery.utils.Constants.DARK_THEME;

public class PhotoPickerActivity extends BaseFragment
        implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider {

    public interface PhotoPickerActivityDelegate {
        void actionButtonPressed(boolean canceled);

        boolean didSelectVideo(String path);

        int getCheckboxTag(int imageId);

        void putCheckboxTag(int imageId, int cornerIndex);

        void removeCheckboxTag(int imageId);

        int generateCheckCorner();

        void openPreview();

        PhotoAlbumPickerActivity getPhotoAlbumPickerActivity();
    }

    private int type;
    // 已选择的图片
    private LinkedHashMap<Integer, MediaController.PhotoEntry> selectedPhotos;

    private final int limitPickPhoto;
    private MediaController.AlbumEntry selectedAlbum;
    private GridView listView;
    private ListAdapter listAdapter;
    private PickerBottomLayout pickerBottomLayout;
    private FrameLayout progressView;
    private TextView emptyView;
    private PhotoPickerActivityDelegate delegate;
    private int itemWidth = 100;
    private boolean sendPressed;
    private boolean singlePhoto;
    private int currentVideoEditId;

    public PhotoPickerActivity(int type,
                               MediaController.AlbumEntry selectedAlbum,
                               LinkedHashMap<Integer, MediaController.PhotoEntry> selectedPhotos,
                               boolean onlyOnePhoto) {
        super();
        this.limitPickPhoto = getConfig().getLimitPickPhoto();
        this.selectedAlbum = selectedAlbum;
        this.selectedPhotos = selectedPhotos;
        this.type = type;
        this.singlePhoto = onlyOnePhoto;
        if (selectedAlbum != null && selectedAlbum.isVideo) {
            singlePhoto = true;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View createView(Context context) {
        actionBar.setBackgroundColor(DARK_THEME ? Theme.ACTION_BAR_MEDIA_PICKER_COLOR : 0xfff9f9f9);
        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR);
        // actionBar.setBackButtonImage(DARK_THEME ? R.drawable.album_ab_back :
        // R.drawable.album_ab_back_bule);
        actionBar.setBackText(LocaleController.getString("Cancel", R.string.Cancel));
        if (selectedAlbum != null) {
            actionBar.setTitle(selectedAlbum.bucketName);
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    delegate.actionButtonPressed(true);
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        TextView cancel = new TextView(context);
        LinearLayout.LayoutParams cancelParams = LayoutHelper.createLinear(48, -1);
        cancel.setText(LocaleController.getString("Gallery", R.string.Gallery));
        cancel.setTextColor(DARK_THEME ? 0xffffffff : 0xff007aff);
        cancel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        cancel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        cancelParams.setMargins(0, 0, AndroidUtilities.dp(8), 0);
        cancel.setLayoutParams(cancelParams);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishFragment();
                //presentFragment(delegate.getPhotoAlbumPickerActivity(), false, true);
            }
        });

        menu.addView(cancel);
        fragmentView = new FrameLayout(context);

        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(DARK_THEME ? 0xff000000 : 0xffffffff);

        listView = new GridView(context);
        listView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        listView.setClipToPadding(false);
        listView.setDrawSelectorOnTop(true);
        listView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setNumColumns(GridView.AUTO_FIT);
        listView.setVerticalSpacing(AndroidUtilities.dp(4));
        listView.setHorizontalSpacing(AndroidUtilities.dp(4));
        listView.setSelector(R.drawable.album_list_selector);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        AndroidUtilities.setListViewEdgeEffectColor(listView, 0xff333333);
        // 点击进去详情页
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (selectedAlbum != null && selectedAlbum.isVideo) {
                    if (position < 0 || position >= selectedAlbum.photos.size()) {
                        return;
                    }
                    if (delegate.didSelectVideo(selectedAlbum.photos.get(position).path)) {
                        finishFragment();
                    }
                } else if (selectedAlbum.photos.get(position).mimeType.contains("video")
                        && getConfig().isVideoEditMode() && selectedPhotos.size() > 0) {
                    AndroidUtilities.showToast(getParentActivity().getString(R.string.NoImageAndVideo));
                } else {
                    ArrayList<Object> arrayList = null;
                    if (selectedAlbum != null) {
                        arrayList = (ArrayList) selectedAlbum.photos;
                    }
                    if (position < 0 || position >= arrayList.size()) {
                        return;
                    }

                    MediaController.PhotoEntry entry = (MediaController.PhotoEntry) arrayList.get(position);
                    if (getConfig().isVideoEditMode() && entry != null && entry.isVideo) {
                        currentVideoEditId = entry.imageId;
                        selectedPhotos.put(currentVideoEditId, entry);
                        pickerBottomLayout.updateSelectedCount(0, false);
                    }
                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                    PhotoViewer.getInstance().openPhotoForSelect(arrayList, false, position, PhotoPickerActivity.this);
                }
            }
        });

        emptyView = new TextView(context);
        emptyView.setTextColor(0xff808080);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        if (selectedAlbum != null) {
            emptyView.setText(LocaleController.getString("NoPhotos", R.string.NoPhotos));
        } else {
            if (type == 0) {
                emptyView.setText(LocaleController.getString("NoRecentPhotos", R.string.NoRecentPhotos));
            } else if (type == 1) {
                emptyView.setText(LocaleController.getString("NoRecentGIFs", R.string.NoRecentGIFs));
            }
        }
        frameLayout.addView(emptyView);
        layoutParams = (FrameLayout.LayoutParams) emptyView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
        emptyView.setLayoutParams(layoutParams);
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        if (selectedAlbum == null) {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_TOUCH_SCROLL) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem,
                                     int visibleItemCount, int totalItemCount) {
                }
            });

            progressView = new FrameLayout(context);
            progressView.setVisibility(View.GONE);
            frameLayout.addView(progressView);
            layoutParams = (FrameLayout.LayoutParams) progressView.getLayoutParams();
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.height = LayoutHelper.MATCH_PARENT;
            layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
            progressView.setLayoutParams(layoutParams);

            ProgressBar progressBar = new ProgressBar(context);
            progressView.addView(progressBar);
            layoutParams = (FrameLayout.LayoutParams) progressBar.getLayoutParams();
            layoutParams.width = LayoutHelper.WRAP_CONTENT;
            layoutParams.height = LayoutHelper.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            progressBar.setLayoutParams(layoutParams);
        }

        pickerBottomLayout = new PickerBottomLayout(context);
        frameLayout.addView(pickerBottomLayout);
        layoutParams = (FrameLayout.LayoutParams) pickerBottomLayout.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(48);
        layoutParams.gravity = Gravity.BOTTOM;
        pickerBottomLayout.setLayoutParams(layoutParams);
        pickerBottomLayout.setBackgroundColor(DARK_THEME ? 0xff1a1a1a : 0xfff9f9f9);
        // 预览按钮
        pickerBottomLayout.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delegate.openPreview();
            }
        });
        // 完成按钮
        pickerBottomLayout.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSelectedPhotos();
            }
        });

        // 原图按钮
        pickerBottomLayout.originalView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pickerBottomLayout.isOriginChecked()) {
                    for (Map.Entry<Integer, MediaController.PhotoEntry> entry : selectedPhotos.entrySet()) {
                        MediaController.PhotoEntry p = entry.getValue();
                        if (!p.isVideo && p.size >= getConfig().getMaxImageSize()) {
                            AndroidUtilities.showToast(selectedPhotos.size() > 1 ?
                                    getParentActivity().getString(R.string.PartSizeOutOfRange) :
                                    getParentActivity().getString(R.string.SizeOutOfRange)
                            );
                            break;
                        }
                    }
                }
                pickerBottomLayout.setChecked(!pickerBottomLayout.isOriginChecked(), false);
            }
        });
        if (singlePhoto) {
            pickerBottomLayout.setVisibility(View.GONE);
        }

        listView.setEmptyView(emptyView);
        pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            refreshList();
        }
        fixLayout();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        }
    }

    private PhotoPickerPhotoCell getCellForIndex(int index) {
        int count = listView.getChildCount();

        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            if (view instanceof PhotoPickerPhotoCell) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                int num = (Integer) cell.photoImage.getTag();
                if (selectedAlbum != null) {
                    if (num < 0 || num >= selectedAlbum.photos.size()) {
                        continue;
                    }
                }
                if (num == index) {
                    return cell;
                }
            }
        }
        return null;
    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(FileLocation fileLocation, int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            int coords[] = new int[2];
            cell.photoImage.getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
            object.parentView = listView;
            object.imageReceiver = cell.photoImage.getImageReceiver();
            object.thumb = object.imageReceiver.getBitmap();
            object.scale = cell.photoImage.getScaleX();
            cell.checkBox.setVisibility(View.GONE);
            return object;
        }
        return null;
    }

    PhotoViewer.PlaceProviderObject getPlaceForPhoto(int imageId) {
        int size = selectedAlbum.photos.size();
        for (int i = 0; i < size; i++) {
            MediaController.PhotoEntry entry = selectedAlbum.photos.get(i);
            if (entry.imageId == imageId) {
                return getPlaceForPhoto(null, i);
            }
        }
        return null;
    }

    @Override
    public void updatePhotoAtIndex(int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            if (selectedAlbum != null) {
                cell.photoImage.setOrientation(0, true);
                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                if (photoEntry.thumbPath != null) {
                    cell.photoImage.setImage(photoEntry.thumbPath, null,
                            cell.getContext().getResources().getDrawable(
                                    DARK_THEME ? R.drawable.album_nophotos
                                            : R.drawable.album_nophotos_new));
                } else if (photoEntry.path != null) {
                    cell.photoImage.setOrientation(photoEntry.orientation, true);
                    if (photoEntry.isVideo) {
                        cell.photoImage.setImage(
                                "vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                                cell.getContext().getResources().getDrawable(
                                        DARK_THEME ? R.drawable.album_nophotos
                                                : R.drawable.album_nophotos_new));
                    } else {
                        cell.photoImage.setImage(
                                "thumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                                cell.getContext().getResources().getDrawable(
                                        DARK_THEME ? R.drawable.album_nophotos
                                                : R.drawable.album_nophotos_new));
                    }
                } else {
                    cell.photoImage
                            .setImageResource(DARK_THEME ? R.drawable.album_nophotos
                                    : R.drawable.album_nophotos_new);
                }
            }
        }
    }

    @Override
    public int getCheckeCorner(int index) {
        int imageId = selectedAlbum.photos.get(index).imageId;
        return delegate.getCheckboxTag(imageId);
    }

    @Override
    public void removeMediaEditId() {
        if (currentVideoEditId != 0) {
            selectedPhotos.remove(currentVideoEditId);
        }
    }

    @Override
    public boolean checkboxEnable() {
        return selectedPhotos.size() <= limitPickPhoto;
    }

    @Override
    public void openPreview() {
        delegate.openPreview();
    }

    @Override
    public void setIsOriginal(boolean isOriginal) {
        if (pickerBottomLayout != null) {
            pickerBottomLayout.setChecked(isOriginal, true);
        }
    }

    @Override
    public Bitmap getThumbForPhoto(FileLocation fileLocation, int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            return cell.photoImage.getImageReceiver().getBitmap();
        }
        return null;
    }

    @Override
    public void willSwitchFromPhoto(FileLocation fileLocation, int fromIndex) {
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            if (view.getTag() == null) {
                continue;
            }
            PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
            int num = (Integer) view.getTag();
            if (selectedAlbum != null) {
                if (num < 0 || num >= selectedAlbum.photos.size()) {
                    continue;
                }
            }
            if (num == fromIndex) {
                cell.checkBox.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    @Override
    public void willHidePhotoViewer() {
        if (listAdapter != null) {
            refreshList();
        }
    }

    @Override
    public List<Object> getSelectedPhotos() {
        ArrayList<Object> list = new ArrayList<>(selectedPhotos.size());
        list.addAll(selectedPhotos.values());
        return list;
    }

    @Override
    public boolean isPhotoChecked(int index) {
        return selectedAlbum != null &&
                !(index < 0 || index >= selectedAlbum.photos.size()) &&
                selectedPhotos.containsKey(selectedAlbum.photos.get(index).imageId);
    }

    @Override
    public int setPhotoChecked(int index) {
        boolean add = true;
        int imageId = -1;
        if (selectedAlbum != null) {
            if (index < 0 || index >= selectedAlbum.photos.size()) {
                return Constants.STATE_FAIL;
            }
            MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
            imageId = photoEntry.imageId;
            if (selectedPhotos.containsKey(photoEntry.imageId)) {
                photoEntry.sortindex = -1;
                selectedPhotos.remove(photoEntry.imageId);
                add = false;
                delegate.removeCheckboxTag(photoEntry.imageId);
            } else if (selectedPhotos.size() < limitPickPhoto) {
                boolean sizeOutOfRange = photoEntry.mimeType.contains("image")
                        && Gallery.sOriginChecked
                        && photoEntry.size >= getConfig().getMaxImageSize();
                boolean timeOutOfRange = photoEntry.mimeType.contains("video")
                        && photoEntry.duration >= getConfig().getMaxVideoTime();
                if (sizeOutOfRange) {
                    return Constants.STATE_IMAGE_SIZE_OUT;
                }
                if (timeOutOfRange) {
                    return Constants.STATE_VIDEO_TIME_OUT;
                }
                selectedPhotos.put(photoEntry.imageId, photoEntry);
                int cornerIndex = delegate.generateCheckCorner();
                photoEntry.sortindex = cornerIndex;
                delegate.putCheckboxTag(photoEntry.imageId, cornerIndex);
            }
        }

        if (selectedPhotos.size() <= limitPickPhoto) {
            int count = listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View view = listView.getChildAt(a);
                int num = (Integer) view.getTag();
                if (num == index) {
                    ((PhotoPickerPhotoCell) view).setChecked(delegate.getCheckboxTag(imageId), add, false);
                    break;
                }
            }
            pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);
        }
        return Constants.STATE_SUCCESS;
    }

    void setPhotoCheckedByImageId(MediaController.PhotoEntry changedEntry) {
        if (changedEntry == null) {
            return;
        }
        int imageId = changedEntry.imageId;
        if (selectedAlbum != null) {
            int size = selectedAlbum.photos.size();
            int index = -1;
            for (int i = 0; i < size; i++) {
                if (imageId == selectedAlbum.photos.get(i).imageId) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                setPhotoChecked(index);
            } else {
                MediaController.PhotoEntry existEntry = selectedPhotos.get(imageId);
                if (existEntry != null) {
                    existEntry.sortindex = -1;
                    selectedPhotos.remove(existEntry.imageId);
                    delegate.removeCheckboxTag(imageId);
                } else {
                    selectedPhotos.put(imageId, changedEntry);
                    int cornerIndex = delegate.generateCheckCorner();
                    changedEntry.sortindex = cornerIndex;
                    delegate.putCheckboxTag(imageId, cornerIndex);
                }
                pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);
            }
        }
    }

    @Override
    public boolean cancelButtonPressed() {
        delegate.actionButtonPressed(true);
        finishFragment();
        return true;
    }

    @Override
    public void sendButtonPressed(int index) {
        if (singlePhoto) {
            selectedPhotos.clear();
            if (index < 0 || index >= selectedAlbum.photos.size()) {
                return;
            }
            MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
            photoEntry.sortindex = 1;
            selectedPhotos.put(photoEntry.imageId, photoEntry);
        } else {
            if (selectedAlbum != null) {
                if (selectedPhotos.isEmpty()) {
                    if (index < 0 || index >= selectedAlbum.photos.size()) {
                        return;
                    }
                    MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                    selectedPhotos.put(photoEntry.imageId, photoEntry);
                }
            }
        }
        sendSelectedPhotos();
    }

    @Override
    public int getSelectedCount() {
        return selectedPhotos.size();
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
    }

    public void setDelegate(PhotoPickerActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private void sendSelectedPhotos() {
        if (selectedPhotos.isEmpty() || delegate == null
                || sendPressed) {
            return;
        }
        sendPressed = true;
        delegate.actionButtonPressed(false);
        finishFragment();
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
        int position = listView.getFirstVisiblePosition();
        WindowManager manager = (WindowManager) Gallery.applicationContext
                .getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();

        int columnsCount;
        if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
            columnsCount = 5;
        } else {
            columnsCount = 4;
        }
        listView.setNumColumns(columnsCount);
        if (AndroidUtilities.isTablet()) {
            itemWidth = (AndroidUtilities.dp(490) - ((columnsCount + 1) * AndroidUtilities.dp(4)))
                    / columnsCount;
        } else {
            itemWidth = (AndroidUtilities.displaySize.x
                    - ((columnsCount + 1) * AndroidUtilities.dp(4))) / columnsCount;
        }
        listView.setColumnWidth(itemWidth);

        refreshList();
        listView.setSelection(position);

        if (selectedAlbum == null) {
            emptyView.setPadding(0, 0, 0,
                    (int) ((AndroidUtilities.displaySize.y - ActionBar.getCurrentActionBarHeight())
                            * 0.4f));
        }
    }

    private void refreshList() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return selectedAlbum != null;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public int getCount() {
            return selectedAlbum.photos.size();
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
            PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
            if (view == null) {
                view = new PhotoPickerPhotoCell(mContext);
                cell = (PhotoPickerPhotoCell) view;
                // 选择图片
                cell.checkFrame.setOnClickListener(checkFrameListener);
                cell.setActions(photoPickerPhotoCellActions);
            }
            cell.itemWidth = itemWidth;
            BackupImageView imageView = ((PhotoPickerPhotoCell) view).photoImage;
            imageView.setTag(i);
            view.setTag(i);
            boolean showing = false;
            imageView.setOrientation(0, true);
            int duration = 0;
            boolean isVideo = false;

            if (selectedAlbum != null) {
                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(i);
                if (photoEntry.thumbPath != null) {
                    imageView.setImage(photoEntry.thumbPath, null,
                            mContext.getResources().getDrawable(DARK_THEME ?
                                    R.drawable.album_nophotos : R.drawable.album_nophotos_new));
                } else if (photoEntry.path != null) {
                    imageView.setOrientation(photoEntry.orientation, true);
                    if (photoEntry.isVideo) {
                        cell.showVideoInfo();
                        duration = photoEntry.duration;
                        isVideo = true;
                        int minutes = photoEntry.duration / 60;
                        int seconds = photoEntry.duration - minutes * 60;
                        cell.videoTextView.setText(String.format(Locale.getDefault(),
                                "%d:%02d", minutes, seconds));
                        imageView.setImage("vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                                mContext.getResources().getDrawable(DARK_THEME ?
                                        R.drawable.album_nophotos : R.drawable.album_nophotos_new));
                    } else {
                        if (photoEntry.path.lastIndexOf(".gif") != -1) {
                            cell.showGifInfo();
                        } else {
                            cell.infoContainer.setVisibility(View.INVISIBLE);
                        }
                        imageView.setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                                mContext.getResources().getDrawable(DARK_THEME ?
                                        R.drawable.album_nophotos : R.drawable.album_nophotos_new));
                    }
                } else {
                    imageView.setImageResource(DARK_THEME ?
                            R.drawable.album_nophotos : R.drawable.album_nophotos_new);
                }
                cell.setChecked(delegate.getCheckboxTag(photoEntry.imageId),
                        selectedPhotos.containsKey(photoEntry.imageId), false);
                showing = PhotoViewer.getInstance().isShowingImage(photoEntry.path);
            }
            imageView.getImageReceiver().setVisible(!showing, true);
            boolean goneCheckBox;
            goneCheckBox = singlePhoto || showing;
            cell.checkBox.setVisibility(goneCheckBox ? View.GONE : View.VISIBLE);
            cell.checkFrame.setVisibility(goneCheckBox ? View.GONE : View.VISIBLE);
            //clickable
            /*cell.clickableView.setVisibility(sListItemClickable ? View.GONE :
                    cell.checkBox.isChecked() ? View.GONE : View.VISIBLE);*/
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return selectedAlbum == null || selectedAlbum.photos.isEmpty();
        }
    }

    private View.OnClickListener checkFrameListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer) ((View) v.getParent()).getTag();
            boolean sizeOutOfRange = false;
            boolean timeOutOfRange = false;
            if (selectedAlbum != null) {
                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                if (selectedPhotos.containsKey(photoEntry.imageId)) {// 选择 -> 未选择
                    photoEntry.sortindex = -1;
                    selectedPhotos.remove(photoEntry.imageId);
                    photoEntry.imagePath = null;
                    photoEntry.thumbPath = null;
                    updatePhotoAtIndex(index);
                    delegate.removeCheckboxTag(photoEntry.imageId);
                } else if (selectedPhotos.size() < limitPickPhoto) {// 未选择 -> 选择
                    sizeOutOfRange = photoEntry.mimeType.contains("image")
                            && Gallery.sOriginChecked
                            && photoEntry.size >= getConfig().getMaxImageSize();
                    timeOutOfRange = photoEntry.mimeType.contains("video")
                            && photoEntry.duration >= getConfig().getMaxVideoTime();
                    if (!sizeOutOfRange && !timeOutOfRange) {
                        selectedPhotos.put(photoEntry.imageId, photoEntry);
                        int cornerIndex = delegate.generateCheckCorner();
                        photoEntry.sortindex = cornerIndex;
                        delegate.putCheckboxTag(photoEntry.imageId, cornerIndex);
                    }
                } else {
                    String hint = getConfig().hasVideo() ?
                            Gallery.applicationContext.getString(R.string.MostSelectWithVideo) :
                            Gallery.applicationContext.getString(R.string.MostSelectOfPhoto);
                    AndroidUtilities.showToast(String.format(hint, limitPickPhoto));
                }

                if (sizeOutOfRange) {
                    AndroidUtilities.showToast(Gallery.applicationContext.getString(R.string.SizeOutOfRange));
                } else if (timeOutOfRange) {
                    String format = Gallery.applicationContext.getString(R.string.LimitTimeOfVideo);
                    int maxVideoTime = getConfig().getMaxVideoTime();
                    AndroidUtilities.showToast(String.format(Locale.getDefault(), format, maxVideoTime + "s"));
                } else if (selectedPhotos.size() <= limitPickPhoto) {
                    ((PhotoPickerPhotoCell) v.getParent()).setChecked(
                            delegate.getCheckboxTag(photoEntry.imageId),
                            selectedPhotos.containsKey(photoEntry.imageId), true);
                }

            } else {
                ((PhotoPickerPhotoCell) v.getParent()).setChecked(false, true);
            }
            // 更新完成前面的数字，代表多少张图片被选中
            pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);
        }
    };

    private PhotoPickerPhotoCell.Actions photoPickerPhotoCellActions = new PhotoPickerPhotoCell.Actions() {
        @Override
        public void onUnCheckedAnimationEnd() {
            /*sListItemClickable = selectedPhotos.size() != getConfig().getLimitPickPhoto();*/
            listAdapter.notifyDataSetChanged();
        }
    };
}
