
package com.tangxiaolv.telegramgallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.LayoutInflater;
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

import com.tangxiaolv.telegramgallery.Actionbar.ActionBar;
import com.tangxiaolv.telegramgallery.Actionbar.ActionBarMenu;
import com.tangxiaolv.telegramgallery.Actionbar.ActionBarMenuItem;
import com.tangxiaolv.telegramgallery.Actionbar.BaseFragment;
import com.tangxiaolv.telegramgallery.Components.BackupImageView;
import com.tangxiaolv.telegramgallery.Components.PhotoPickerPhotoCell;
import com.tangxiaolv.telegramgallery.Components.PickerBottomLayout;
import com.tangxiaolv.telegramgallery.TL.FileLocation;
import com.tangxiaolv.telegramgallery.Utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.Utils.FileLoader;
import com.tangxiaolv.telegramgallery.Utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.Utils.LocaleController;
import com.tangxiaolv.telegramgallery.Utils.MediaController;
import com.tangxiaolv.telegramgallery.Utils.NotificationCenter;

import java.util.ArrayList;
import java.util.HashMap;

import static com.tangxiaolv.telegramgallery.PhotoAlbumPickerActivity.DarkTheme;

public class PhotoPickerActivity extends BaseFragment
        implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider {

    public interface PhotoPickerActivityDelegate {
        void selectedPhotosChanged();

        void actionButtonPressed(boolean canceled);

        boolean didSelectVideo(String path);

        int getCheckboxTag(int imageId);

        void putCheckboxTag(int imageId, int cornerIndex);

        void removeCheckboxTag(int imageId);

        int generateCheckCorner();

        void openPreview();
    }

    private int type;
    private HashMap<Integer, MediaController.PhotoEntry> selectedPhotos;
    private ArrayList<MediaController.SearchImage> recentImages;

    private ArrayList<MediaController.SearchImage> searchResult = new ArrayList<>();

    private boolean searching;
    private String nextSearchBingString;
    private boolean giphySearchEndReached = true;
    private String lastSearchString;
    private boolean loadingRecent;
    private int nextGiphySearchOffset;
    private int giphyReqId;
    private int lastSearchToken;
    private final int limitPickPhoto;

    private MediaController.AlbumEntry selectedAlbum;

    private GridView listView;
    private ListAdapter listAdapter;
    private PickerBottomLayout pickerBottomLayout;
    private FrameLayout progressView;
    private TextView emptyView;
    private ActionBarMenuItem searchItem;
    private int itemWidth = 100;
    private boolean sendPressed;
    private boolean singlePhoto;

    private PhotoPickerActivityDelegate delegate;

    public PhotoPickerActivity(int type, int limitPickPhoto,
            MediaController.AlbumEntry selectedAlbum,
            HashMap<Integer, MediaController.PhotoEntry> selectedPhotos,
            ArrayList<MediaController.SearchImage> recentImages, boolean onlyOnePhoto) {
        super();
        this.limitPickPhoto = limitPickPhoto;
        this.selectedAlbum = selectedAlbum;
        this.selectedPhotos = selectedPhotos;
        this.type = type;
        this.recentImages = recentImages;
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
        actionBar.setBackgroundColor(Theme.ACTION_BAR_MEDIA_PICKER_COLOR);
        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        // actionBar.setBackText("返回");
        if (selectedAlbum != null) {
            actionBar.setTitle(selectedAlbum.bucketName);
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();

        TextView cancel = new TextView(context);
        LinearLayout.LayoutParams cancelParams = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, -1);
        cancel.setTextSize(18);
        cancel.setText(LocaleController.getString("Cancel", R.string.Cancel));
        cancel.setTextColor(0xffffffff);
        cancel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        cancelParams.setMargins(0, 0, AndroidUtilities.dp(8), 0);
        cancel.setLayoutParams(cancelParams);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishFragment();
                delegate.actionButtonPressed(true);
            }
        });
        menu.addView(cancel);

        if (selectedAlbum == null) {
            if (type == 0) {
                searchItem.getSearchField().setHint(LocaleController.getString("SearchImagesTitle",
                        R.string.SearchImagesTitle));
            } else if (type == 1) {
                searchItem.getSearchField().setHint(
                        LocaleController.getString("SearchGifsTitle", R.string.SearchGifsTitle));
            }
        }

        fragmentView = new FrameLayout(context);

        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout
                .setBackgroundColor(DarkTheme ? 0xff000000 : 0xffffffff);

        listView = new GridView(context);
        listView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4),
                AndroidUtilities.dp(4));
        listView.setClipToPadding(false);
        listView.setDrawSelectorOnTop(true);
        listView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setNumColumns(GridView.AUTO_FIT);
        listView.setVerticalSpacing(AndroidUtilities.dp(4));
        listView.setHorizontalSpacing(AndroidUtilities.dp(4));
        listView.setSelector(R.drawable.list_selector);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView
                .getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        AndroidUtilities.setListViewEdgeEffectColor(listView, 0xff333333);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (selectedAlbum != null && selectedAlbum.isVideo) {
                    if (i < 0 || i >= selectedAlbum.photos.size()) {
                        return;
                    }
                    if (delegate.didSelectVideo(selectedAlbum.photos.get(i).path)) {
                        finishFragment();
                    }
                } else {
                    ArrayList<Object> arrayList;
                    if (selectedAlbum != null) {
                        arrayList = (ArrayList) selectedAlbum.photos;
                    } else {
                        if (searchResult.isEmpty() && lastSearchString == null) {
                            arrayList = (ArrayList) recentImages;
                        } else {
                            arrayList = (ArrayList) searchResult;
                        }
                    }
                    if (i < 0 || i >= arrayList.size()) {
                        return;
                    }
                    if (searchItem != null) {
                        AndroidUtilities.hideKeyboard(searchItem.getSearchField());
                    }
                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                    PhotoViewer.getInstance().openPhotoForSelect(arrayList, false, i,
                            singlePhoto ? 1 : 0,
                            PhotoPickerActivity.this);
                }
            }
        });

        if (selectedAlbum == null) {
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                        long id) {
                    if (searchResult.isEmpty() && lastSearchString == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("AppName", R.string.app_name));
                        builder.setMessage(
                                LocaleController.getString("ClearSearch", R.string.ClearSearch));
                        builder.setPositiveButton(LocaleController
                                .getString("ClearButton", R.string.ClearButton).toUpperCase(),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        recentImages.clear();
                                        if (listAdapter != null) {
                                            listAdapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                        builder.setNegativeButton(
                                LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                        return true;
                    }
                    return false;
                }
            });
        }

        emptyView = new TextView(context);
        emptyView.setTextColor(0xff808080);
        emptyView.setTextSize(20);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        if (selectedAlbum != null) {
            emptyView.setText(LocaleController.getString("NoPhotos", R.string.NoPhotos));
        } else {
            if (type == 0) {
                emptyView.setText(
                        LocaleController.getString("NoRecentPhotos", R.string.NoRecentPhotos));
            } else if (type == 1) {
                emptyView
                        .setText(LocaleController.getString("NoRecentGIFs", R.string.NoRecentGIFs));
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
                    if (visibleItemCount != 0
                            && firstVisibleItem + visibleItemCount > totalItemCount - 2
                            && !searching) {
                    }
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

            updateSearchInterface();
        }

        pickerBottomLayout = new PickerBottomLayout(context, DarkTheme);
        frameLayout.addView(pickerBottomLayout);
        layoutParams = (FrameLayout.LayoutParams) pickerBottomLayout.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(48);
        layoutParams.gravity = Gravity.BOTTOM;
        pickerBottomLayout.setLayoutParams(layoutParams);
        pickerBottomLayout.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delegate.openPreview();
            }
        });
        pickerBottomLayout.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSelectedPhotos();
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
            listAdapter.notifyDataSetChanged();
        }
        if (searchItem != null) {
            searchItem.openSearch(true);
            getParentActivity().getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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
        } else if (id == NotificationCenter.recentImagesDidLoaded) {
            if (selectedAlbum == null && type == (Integer) args[0]) {
                recentImages = (ArrayList<MediaController.SearchImage>) args[1];
                loadingRecent = false;
                updateSearchInterface();
            }
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
                } else {
                    ArrayList<MediaController.SearchImage> array;
                    if (searchResult.isEmpty() && lastSearchString == null) {
                        array = recentImages;
                    } else {
                        array = searchResult;
                    }
                    if (num < 0 || num >= array.size()) {
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

    @Override
    public void updatePhotoAtIndex(int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            if (selectedAlbum != null) {
                cell.photoImage.setOrientation(0, true);
                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                if (photoEntry.thumbPath != null) {
                    cell.photoImage.setImage(photoEntry.thumbPath, null,
                            cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.path != null) {
                    cell.photoImage.setOrientation(photoEntry.orientation, true);
                    if (photoEntry.isVideo) {
                        cell.photoImage.setImage(
                                "vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                                cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                    } else {
                        cell.photoImage.setImage(
                                "thumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                                cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                    }
                } else {
                    cell.photoImage.setImageResource(R.drawable.nophotos);
                }
            } else {
                ArrayList<MediaController.SearchImage> array;
                if (searchResult.isEmpty() && lastSearchString == null) {
                    array = recentImages;
                } else {
                    array = searchResult;
                }
                MediaController.SearchImage photoEntry = array.get(index);
                if (photoEntry.document != null && photoEntry.document.thumb != null) {
                    cell.photoImage.setImage(photoEntry.document.thumb.location, null,
                            cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.thumbPath != null) {
                    cell.photoImage.setImage(photoEntry.thumbPath, null,
                            cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.thumbUrl != null && photoEntry.thumbUrl.length() > 0) {
                    cell.photoImage.setImage(photoEntry.thumbUrl, null,
                            cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else {
                    cell.photoImage.setImageResource(R.drawable.nophotos);
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
    public boolean checkboxEnable() {
        return selectedPhotos.size() <= limitPickPhoto;
    }

    @Override
    public void openPreview() {
        delegate.openPreview();
    }

    @Override
    public boolean isSinglePhoto() {
        return singlePhoto;
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
    public void willSwitchFromPhoto(FileLocation fileLocation, int index) {
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
            } else {
                ArrayList<MediaController.SearchImage> array;
                if (searchResult.isEmpty() && lastSearchString == null) {
                    array = recentImages;
                } else {
                    array = searchResult;
                }
                if (num < 0 || num >= array.size()) {
                    continue;
                }
            }
            if (num == index) {
                cell.checkBox.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    @Override
    public void willHidePhotoViewer() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean isPhotoChecked(int index) {
        if (selectedAlbum != null) {
            return !(index < 0 || index >= selectedAlbum.photos.size())
                    && selectedPhotos.containsKey(selectedAlbum.photos.get(index).imageId);
        }
        return false;
    }

    @Override
    public void setPhotoChecked(int index) {
        boolean add = true;
        int imageId = -1;
        if (selectedAlbum != null) {
            if (index < 0 || index >= selectedAlbum.photos.size()) {
                return;
            }
            MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
            imageId = photoEntry.imageId;
            if (selectedPhotos.containsKey(photoEntry.imageId)) {
                photoEntry.sortindex = -1;
                selectedPhotos.remove(photoEntry.imageId);
                add = false;
                delegate.removeCheckboxTag(photoEntry.imageId);
            } else if (selectedPhotos.size() < limitPickPhoto) {
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
                    ((PhotoPickerPhotoCell) view).setChecked(delegate.getCheckboxTag(imageId), add,
                            false);
                    break;
                }
            }
            pickerBottomLayout.updateSelectedCount(selectedPhotos.size(), true);
            delegate.selectedPhotosChanged();
        }
    }

    public void setPhotoCheckedByImageId(MediaController.PhotoEntry changedEntry) {
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
            }else{
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
        if (isOpen && searchItem != null) {
            AndroidUtilities.showKeyboard(searchItem.getSearchField());
        }
    }

    private void updateSearchInterface() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        if (searching && searchResult.isEmpty() || loadingRecent && lastSearchString == null) {
            progressView.setVisibility(View.VISIBLE);
            listView.setEmptyView(null);
            emptyView.setVisibility(View.GONE);
        } else {
            progressView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            listView.setEmptyView(emptyView);
        }
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
        if (AndroidUtilities.isTablet()) {
            columnsCount = 3;
        } else {
            if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                columnsCount = 5;
            } else {
                columnsCount = 3;
            }
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

        listAdapter.notifyDataSetChanged();
        listView.setSelection(position);

        if (selectedAlbum == null) {
            emptyView.setPadding(0, 0, 0,
                    (int) ((AndroidUtilities.displaySize.y - ActionBar.getCurrentActionBarHeight())
                            * 0.4f));
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return selectedAlbum != null;
        }

        @Override
        public boolean isEnabled(int i) {
            if (selectedAlbum == null) {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return i < recentImages.size();
                } else {
                    return i < searchResult.size();
                }
            }
            return true;
        }

        @Override
        public int getCount() {
            if (selectedAlbum == null) {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return recentImages.size();
                } else if (type == 0) {
                    return searchResult.size() + (nextSearchBingString == null ? 0 : 1);
                } else if (type == 1) {
                    return searchResult.size() + (giphySearchEndReached ? 0 : 1);
                }
            }
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
            int viewType = getItemViewType(i);
            if (viewType == 0) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                if (view == null) {
                    view = new PhotoPickerPhotoCell(mContext);
                    cell = (PhotoPickerPhotoCell) view;
                    cell.checkFrame.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int index = (Integer) ((View) v.getParent()).getTag();
                            if (selectedAlbum != null) {
                                MediaController.PhotoEntry photoEntry = selectedAlbum.photos
                                        .get(index);
                                if (selectedPhotos.containsKey(photoEntry.imageId)) {
                                    photoEntry.sortindex = -1;
                                    selectedPhotos.remove(photoEntry.imageId);
                                    photoEntry.imagePath = null;
                                    photoEntry.thumbPath = null;
                                    updatePhotoAtIndex(index);
                                    delegate.removeCheckboxTag(photoEntry.imageId);
                                } else if (selectedPhotos.size() < limitPickPhoto) {
                                    selectedPhotos.put(photoEntry.imageId, photoEntry);
                                    int cornerIndex = delegate.generateCheckCorner();
                                    photoEntry.sortindex = cornerIndex;
                                    delegate.putCheckboxTag(photoEntry.imageId, cornerIndex);
                                } else {
                                    AndroidUtilities.showToast(
                                            String.format(Gallery.applicationContext.getString(
                                                    R.string.MostSelect), limitPickPhoto));
                                }

                                if (selectedPhotos.size() <= limitPickPhoto) {
                                    ((PhotoPickerPhotoCell) v.getParent()).setChecked(
                                            delegate.getCheckboxTag(photoEntry.imageId),
                                            selectedPhotos.containsKey(photoEntry.imageId), true);
                                }
                            } else {
                                AndroidUtilities
                                        .hideKeyboard(getParentActivity().getCurrentFocus());
                                MediaController.SearchImage photoEntry;
                                if (searchResult.isEmpty() && lastSearchString == null) {
                                    photoEntry = recentImages
                                            .get((Integer) ((View) v.getParent()).getTag());
                                } else {
                                    photoEntry = searchResult
                                            .get((Integer) ((View) v.getParent()).getTag());
                                }
                                ((PhotoPickerPhotoCell) v.getParent()).setChecked(false, true);
                            }
                            pickerBottomLayout.updateSelectedCount(
                                    selectedPhotos.size(), true);
                            delegate.selectedPhotosChanged();
                        }
                    });
                    cell.checkFrame.setVisibility(singlePhoto ? View.GONE : View.VISIBLE);
                }
                cell.itemWidth = itemWidth;
                BackupImageView imageView = ((PhotoPickerPhotoCell) view).photoImage;
                imageView.setTag(i);
                view.setTag(i);
                boolean showing;
                imageView.setOrientation(0, true);

                if (selectedAlbum != null) {
                    MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(i);
                    if (photoEntry.thumbPath != null) {
                        imageView.setImage(photoEntry.thumbPath, null,
                                mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.path != null) {
                        imageView.setOrientation(photoEntry.orientation, true);
                        if (photoEntry.isVideo) {
                            imageView.setImage(
                                    "vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                                    mContext.getResources().getDrawable(R.drawable.nophotos));
                        } else {
                            imageView.setImage(
                                    "thumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                                    mContext.getResources().getDrawable(R.drawable.nophotos));
                        }
                    } else {
                        imageView.setImageResource(R.drawable.nophotos);
                    }
                    cell.setChecked(delegate.getCheckboxTag(photoEntry.imageId),
                            selectedPhotos.containsKey(photoEntry.imageId), false);
                    showing = PhotoViewer.getInstance().isShowingImage(photoEntry.path);
                } else {
                    MediaController.SearchImage photoEntry;
                    if (searchResult.isEmpty() && lastSearchString == null) {
                        photoEntry = recentImages.get(i);
                    } else {
                        photoEntry = searchResult.get(i);
                    }
                    if (photoEntry.thumbPath != null) {
                        imageView.setImage(photoEntry.thumbPath, null,
                                mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.thumbUrl != null && photoEntry.thumbUrl.length() > 0) {
                        imageView.setImage(photoEntry.thumbUrl, null,
                                mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.document != null && photoEntry.document.thumb != null) {
                        imageView.setImage(photoEntry.document.thumb.location, null,
                                mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else {
                        imageView.setImageResource(R.drawable.nophotos);
                    }
                    cell.setChecked(false, false);
                    if (photoEntry.document != null) {
                        showing = PhotoViewer.getInstance().isShowingImage(FileLoader
                                .getPathToAttach(photoEntry.document, true).getAbsolutePath());
                    } else {
                        showing = PhotoViewer.getInstance().isShowingImage(photoEntry.imageUrl);
                    }
                }
                imageView.getImageReceiver().setVisible(!showing, true);
                cell.checkBox.setVisibility(singlePhoto || showing ? View.GONE : View.VISIBLE);
            } else if (viewType == 1) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.media_loading_layout, viewGroup, false);
                }
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = itemWidth;
                params.height = itemWidth;
                view.setLayoutParams(params);
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (selectedAlbum != null
                    || searchResult.isEmpty() && lastSearchString == null && i < recentImages.size()
                    || i < searchResult.size()) {
                return 0;
            }
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            if (selectedAlbum != null) {
                return selectedAlbum.photos.isEmpty();
            } else {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return recentImages.isEmpty();
                } else {
                    return searchResult.isEmpty();
                }
            }
        }
    }
}
