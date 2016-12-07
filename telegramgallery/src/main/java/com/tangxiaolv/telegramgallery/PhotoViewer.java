
package com.tangxiaolv.telegramgallery;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.tangxiaolv.telegramgallery.Actionbar.ActionBar;
import com.tangxiaolv.telegramgallery.Actionbar.ActionBarMenu;
import com.tangxiaolv.telegramgallery.Actionbar.BaseFragment;
import com.tangxiaolv.telegramgallery.Components.AspectRatioFrameLayout;
import com.tangxiaolv.telegramgallery.Components.CheckBox;
import com.tangxiaolv.telegramgallery.Components.ClippingImageView;
import com.tangxiaolv.telegramgallery.Components.PhotoCropView;
import com.tangxiaolv.telegramgallery.Components.PickerBottomLayout;
import com.tangxiaolv.telegramgallery.Components.SizeNotifierFrameLayoutPhoto;
import com.tangxiaolv.telegramgallery.TL.Document;
import com.tangxiaolv.telegramgallery.TL.FileLocation;
import com.tangxiaolv.telegramgallery.TL.Photo;
import com.tangxiaolv.telegramgallery.TL.PhotoSize;
import com.tangxiaolv.telegramgallery.Utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.Utils.FileLoader;
import com.tangxiaolv.telegramgallery.Utils.ImageLoader;
import com.tangxiaolv.telegramgallery.Utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.Utils.LocaleController;
import com.tangxiaolv.telegramgallery.Utils.MediaController;
import com.tangxiaolv.telegramgallery.Utils.NotificationCenter;
import com.tangxiaolv.telegramgallery.Utils.Utilities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;

import static com.tangxiaolv.telegramgallery.PhotoAlbumPickerActivity.limitPickPhoto;
import static com.tangxiaolv.telegramgallery.PhotoAlbumPickerActivity.sHintOfPick;

@SuppressWarnings("unchecked")
public class PhotoViewer implements NotificationCenter.NotificationCenterDelegate,
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private int classGuid;
    private PhotoViewerProvider placeProvider;
    private boolean isVisible;

    private Activity parentActivity;
    private Context actvityContext;

    private ActionBar actionBar;
    private boolean isActionBarVisible = true;

    private WindowManager.LayoutParams windowLayoutParams;
    private FrameLayoutDrawer containerView;
    private FrameLayoutTouchListener windowView;
    private ClippingImageView animatingImageView;
    private FrameLayout bottomLayout;
    private BackgroundDrawable backgroundDrawable = new BackgroundDrawable(0xff000000);
    private CheckBox checkImageView;
    private PickerBottomLayout pickerView;
    private PickerBottomLayout editorDoneLayout;
    private RadialProgressView radialProgressViews[] = new RadialProgressView[3];
    // private ActionBarMenuItem cropItem;
    private View indexItem;
    private AnimatorSet currentActionBarAnimation;
    private PhotoCropView photoCropView;
    private AlertDialog visibleDialog;
    private boolean canShowBottom = true;
    private boolean isSelectPreview;
    private int sendPhotoType = 0;
    private AnimatedFileDrawable currentAnimation;

    private AspectRatioFrameLayout aspectRatioFrameLayout;
    private TextureView videoTextureView;
    private ImageView videoPlayButton;
    private boolean playerNeedsPrepare;
    private boolean textureUploaded;
    private boolean videoCrossfadeStarted;
    private float videoCrossfadeAlpha;
    private long videoCrossfadeAlphaLastTime;

    private float animationValues[][] = new float[2][8];

    private int animationInProgress = 0;
    private long transitionAnimationStartTime = 0;
    private Runnable animationEndRunnable = null;
    private PlaceProviderObject showAfterAnimation;
    private PlaceProviderObject hideAfterAnimation;
    private boolean disableShowCheck = false;

    private int currentEditMode;

    private ImageReceiver leftImage = new ImageReceiver();
    private ImageReceiver centerImage = new ImageReceiver();
    private ImageReceiver rightImage = new ImageReceiver();
    private int currentIndex;
    private FileLocation currentFileLocation;
    private String currentFileNames[] = new String[3];
    private PlaceProviderObject currentPlaceObject;
    private String currentPathObject;
    private Bitmap currentThumb = null;

    private int avatarsDialogId;
    private long currentDialogId;
    private long mergeDialogId;
    private int totalImagesCount;
    private int totalImagesCountMerge;
    private boolean isFirstLoading;
    private boolean needSearchImageInArr;
    private boolean loadingMoreImages;
    private boolean endReached[] = new boolean[] {
            false, true
    };
    private boolean opennedFromMedia;

    private boolean draggingDown = false;
    private float dragY;
    private float translationX;
    private float translationY;
    private float scale = 1;
    private float animateToX;
    private float animateToY;
    private float animateToScale;
    private float animationValue;
    private int currentRotation;
    private long animationStartTime;
    private AnimatorSet imageMoveAnimation;
    private AnimatorSet changeModeAnimation;
    private GestureDetector gestureDetector;
    private DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);
    private float pinchStartDistance;
    private float pinchStartScale = 1;
    private float pinchCenterX;
    private float pinchCenterY;
    private float pinchStartX;
    private float pinchStartY;
    private float moveStartX;
    private float moveStartY;
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;
    private boolean canZoom = true;
    private boolean changingPage = false;
    private boolean zooming = false;
    private boolean moving = false;
    private boolean doubleTap = false;
    private boolean invalidCoords = false;
    private boolean canDragDown = true;
    private boolean zoomAnimation = false;
    private boolean discardTap = false;
    private int switchImageAfterAnimation = 0;
    private VelocityTracker velocityTracker = null;
    private Scroller scroller = null;

    private ArrayList<FileLocation> imagesArrLocations = new ArrayList<>();
    private ArrayList<Photo> avatarsArr = new ArrayList<>();
    private ArrayList<Integer> imagesArrLocationsSizes = new ArrayList<>();
    private ArrayList<Object> imagesArrLocals = new ArrayList<>();
    private FileLocation currentUserAvatarLocation = null;

    private final static int gallery_menu_crop = 4;
    private final static int gallery_menu_index = 1;

    private final static int PAGE_SPACING = AndroidUtilities.dp(30);

    private static DecelerateInterpolator decelerateInterpolator = null;
    private static Paint progressPaint = null;

    private class BackgroundDrawable extends ColorDrawable {

        private Runnable drawRunnable;

        public BackgroundDrawable(int color) {
            super(color);
        }

        @Override
        public void setAlpha(int alpha) {
            super.setAlpha(alpha);
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            if (getAlpha() != 0) {
                if (drawRunnable != null) {
                    drawRunnable.run();
                    drawRunnable = null;
                }
            }
        }
    }

    private class RadialProgressView {

        private long lastUpdateTime = 0;
        private float radOffset = 0;
        private float currentProgress = 0;
        private float animationProgressStart = 0;
        private long currentProgressTime = 0;
        private float animatedProgressValue = 0;
        private RectF progressRect = new RectF();
        private int backgroundState = -1;
        private View parent = null;
        private int size = AndroidUtilities.dp(64);
        private int previousBackgroundState = -2;
        private float animatedAlphaValue = 1.0f;
        private float alpha = 1.0f;
        private float scale = 1.0f;

        public RadialProgressView(Context context, View parentView) {
            if (decelerateInterpolator == null) {
                decelerateInterpolator = new DecelerateInterpolator(1.5f);
                progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                progressPaint.setStyle(Paint.Style.STROKE);
                progressPaint.setStrokeCap(Paint.Cap.ROUND);
                progressPaint.setStrokeWidth(AndroidUtilities.dp(3));
                progressPaint.setColor(0xffffffff);
            }
            parent = parentView;
        }

        private void updateAnimation() {
            long newTime = System.currentTimeMillis();
            long dt = newTime - lastUpdateTime;
            lastUpdateTime = newTime;

            if (animatedProgressValue != 1) {
                radOffset += 360 * dt / 3000.0f;
                float progressDiff = currentProgress - animationProgressStart;
                if (progressDiff > 0) {
                    currentProgressTime += dt;
                    if (currentProgressTime >= 300) {
                        animatedProgressValue = currentProgress;
                        animationProgressStart = currentProgress;
                        currentProgressTime = 0;
                    } else {
                        animatedProgressValue = animationProgressStart
                                + progressDiff * decelerateInterpolator
                                        .getInterpolation(currentProgressTime / 300.0f);
                    }
                }
                parent.invalidate();
            }
            if (animatedProgressValue >= 1 && previousBackgroundState != -2) {
                animatedAlphaValue -= dt / 200.0f;
                if (animatedAlphaValue <= 0) {
                    animatedAlphaValue = 0.0f;
                    previousBackgroundState = -2;
                }
                parent.invalidate();
            }
        }

        public void setProgress(float value, boolean animated) {
            if (!animated) {
                animatedProgressValue = value;
                animationProgressStart = value;
            } else {
                animationProgressStart = animatedProgressValue;
            }
            currentProgress = value;
            currentProgressTime = 0;
        }

        public void setBackgroundState(int state, boolean animated) {
            lastUpdateTime = System.currentTimeMillis();
            if (animated && backgroundState != state) {
                previousBackgroundState = backgroundState;
                animatedAlphaValue = 1.0f;
            } else {
                previousBackgroundState = -2;
            }
            backgroundState = state;
            parent.invalidate();
        }

        public void setAlpha(float value) {
            alpha = value;
        }

        public void setScale(float value) {
            scale = value;
        }

        public void onDraw(Canvas canvas) {
            int sizeScaled = (int) (size * scale);
            int x = (getContainerViewWidth() - sizeScaled) / 2;
            int y = (getContainerViewHeight() - sizeScaled) / 2;

            if (backgroundState == 0 || backgroundState == 1 || previousBackgroundState == 0
                    || previousBackgroundState == 1) {
                int diff = AndroidUtilities.dp(4);
                if (previousBackgroundState != -2) {
                    progressPaint.setAlpha((int) (255 * animatedAlphaValue * alpha));
                } else {
                    progressPaint.setAlpha((int) (255 * alpha));
                }
                progressRect.set(x + diff, y + diff, x + sizeScaled - diff, y + sizeScaled - diff);
                canvas.drawArc(progressRect, -90 + radOffset,
                        Math.max(4, 360 * animatedProgressValue), false, progressPaint);
                updateAnimation();
            }
        }
    }

    public static class PlaceProviderObject {
        public ImageReceiver imageReceiver;
        public int viewX;
        public int viewY;
        public View parentView;
        public Bitmap thumb;
        public int dialogId;
        public int index;
        public int size;
        public int radius;
        public int clipBottomAddition;
        public int clipTopAddition;
        public float scale = 1.0f;
    }

    public static class EmptyPhotoViewerProvider implements PhotoViewerProvider {
        @Override
        public PlaceProviderObject getPlaceForPhoto(FileLocation fileLocation, int index) {
            return null;
        }

        @Override
        public Bitmap getThumbForPhoto(FileLocation fileLocation, int index) {
            return null;
        }

        @Override
        public void willSwitchFromPhoto(FileLocation fileLocation, int index) {

        }

        @Override
        public void willHidePhotoViewer() {

        }

        @Override
        public boolean isPhotoChecked(int index) {
            return false;
        }

        @Override
        public void setPhotoChecked(int index) {

        }

        @Override
        public boolean cancelButtonPressed() {
            return true;
        }

        @Override
        public void sendButtonPressed(int index) {

        }

        @Override
        public int getSelectedCount() {
            return 0;
        }

        @Override
        public void updatePhotoAtIndex(int index) {

        }

        @Override
        public int getCheckeCorner(int imageId) {
            return -1;
        }

        @Override
        public boolean checkboxEnable() {
            return true;
        }

        @Override
        public void openPreview() {

        }

        @Override
        public boolean isSinglePhoto() {
            return false;
        }
    }

    /**
     * 预览已选择
     */
    public static class PreviewEmptyPhotoViewerProvider extends EmptyPhotoViewerProvider {
        public void selectChanged(int index, boolean checked) {
        }

        public void previewExit() {

        }
    }

    public interface PhotoViewerProvider {
        PlaceProviderObject getPlaceForPhoto(FileLocation fileLocation, int index);

        Bitmap getThumbForPhoto(FileLocation fileLocation, int index);

        void willSwitchFromPhoto(FileLocation fileLocation, int index);

        void willHidePhotoViewer();

        boolean isPhotoChecked(int index);

        void setPhotoChecked(int index);

        boolean cancelButtonPressed();

        void sendButtonPressed(int index);

        int getSelectedCount();

        void updatePhotoAtIndex(int index);

        // 以下为新增
        int getCheckeCorner(int currentIndex);

        boolean checkboxEnable();

        void openPreview();

        boolean isSinglePhoto();
    }

    private class FrameLayoutTouchListener extends FrameLayout {

        private boolean attachedToWindow;
        private Runnable attachRunnable;

        public FrameLayoutTouchListener(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return getInstance().onTouchEvent(event);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            System.out.println();
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            getInstance().onLayout(changed, left, top, right, bottom);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attachedToWindow = true;
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            attachedToWindow = false;
        }
    }

    private class FrameLayoutDrawer extends SizeNotifierFrameLayoutPhoto {
        public FrameLayoutDrawer(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            if (heightSize > AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight) {
                heightSize = AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight;
            }

            setMeasuredDimension(widthSize, heightSize);

            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            final int count = getChildCount();

            int paddingBottom = 0;

            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = Gravity.TOP | Gravity.LEFT;
                }

                final int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = (r - l - width) / 2 + lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        childLeft = r - width - lp.rightMargin;
                        break;
                    case Gravity.LEFT:
                    default:
                        childLeft = lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = lp.topMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = ((b - paddingBottom) - t - height) / 2 + lp.topMargin
                                - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = ((b - paddingBottom) - t) - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = lp.topMargin;
                }

                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }

            notifyHeightChanged();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            getInstance().onDraw(canvas);
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            return child != aspectRatioFrameLayout && super.drawChild(canvas, child, drawingTime);
        }
    }

    private static volatile PhotoViewer Instance = null;

    public static PhotoViewer getInstance() {
        PhotoViewer localInstance = Instance;
        if (localInstance == null) {
            synchronized (PhotoViewer.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new PhotoViewer();
                }
            }
        }
        return localInstance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.FileDidFailedLoad) {
            String location = (String) args[0];
            for (int a = 0; a < 3; a++) {
                if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
                    radialProgressViews[a].setProgress(1.0f, true);
                    checkProgress(a, true);
                    break;
                }
            }
        } else if (id == NotificationCenter.FileDidLoaded) {
            String location = (String) args[0];
            for (int a = 0; a < 3; a++) {
                if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
                    radialProgressViews[a].setProgress(1.0f, true);
                    checkProgress(a, true);
                    if (Build.VERSION.SDK_INT >= 16 && a == 0) {
                        onActionClick(false);
                    }
                    break;
                }
            }
        } else if (id == NotificationCenter.FileLoadProgressChanged) {
            String location = (String) args[0];
            for (int a = 0; a < 3; a++) {
                if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
                    Float progress = (Float) args[1];
                    radialProgressViews[a].setProgress(progress, true);
                }
            }
        } else if (id == NotificationCenter.dialogPhotosLoaded) {
            int guid = (Integer) args[4];
            int did = (Integer) args[0];
            if (avatarsDialogId == did && classGuid == guid) {
                boolean fromCache = (Boolean) args[3];

                int setToImage = -1;
                ArrayList<Photo> photos = (ArrayList<Photo>) args[5];
                if (photos.isEmpty()) {
                    return;
                }
                imagesArrLocations.clear();
                imagesArrLocationsSizes.clear();
                avatarsArr.clear();
                for (int a = 0; a < photos.size(); a++) {
                    Photo photo = photos.get(a);
                    if (photo == null || photo instanceof Photo.TL_photoEmpty
                            || photo.sizes == null) {
                        continue;
                    }
                    PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 640);
                    if (sizeFull != null) {
                        if (setToImage == -1 && currentFileLocation != null) {
                            for (int b = 0; b < photo.sizes.size(); b++) {
                                PhotoSize size = photo.sizes.get(b);
                                if (size.location.local_id == currentFileLocation.local_id
                                        && size.location.volume_id == currentFileLocation.volume_id) {
                                    setToImage = imagesArrLocations.size();
                                    break;
                                }
                            }
                        }
                        imagesArrLocations.add(sizeFull.location);
                        imagesArrLocationsSizes.add(sizeFull.size);
                        avatarsArr.add(photo);
                    }
                }
                needSearchImageInArr = false;
                currentIndex = -1;
                if (setToImage != -1) {
                    setImageIndex(setToImage, true);
                } else {
                    avatarsArr.add(0, new Photo.TL_photoEmpty());
                    imagesArrLocations.add(0, currentFileLocation);
                    imagesArrLocationsSizes.add(0, 0);
                    setImageIndex(0, true);
                }
                if (fromCache) {
                }
            }
        } else if (id == NotificationCenter.mediaCountDidLoaded) {
            long uid = (Long) args[0];
            if (uid == currentDialogId || uid == mergeDialogId) {
                if (uid == currentDialogId) {
                    totalImagesCount = (Integer) args[1];
                    /*
                     * if ((Boolean) args[2]) { SharedMediaQuery.getMediaCount(currentDialogId,
                     * SharedMediaQuery.MEDIA_PHOTOVIDEO, classGuid, false); }
                     */
                } else if (uid == mergeDialogId) {
                    totalImagesCountMerge = (Integer) args[1];
                    /*
                     * if ((Boolean) args[2]) { SharedMediaQuery.getMediaCount(mergeDialogId,
                     * SharedMediaQuery.MEDIA_PHOTOVIDEO, classGuid, false); }
                     */
                }
                if (needSearchImageInArr && isFirstLoading) {
                    isFirstLoading = false;
                    loadingMoreImages = true;
                }
            }
        }
    }

    public void setParentActivity(final Activity activity) {
        if (parentActivity == activity) {
            return;
        }
        parentActivity = activity;
        actvityContext = new ContextThemeWrapper(parentActivity, R.style.Theme_TMessages);

        scroller = new Scroller(activity);

        windowView = new FrameLayoutTouchListener(activity) {

            @Override
            public boolean dispatchKeyEventPreIme(KeyEvent event) {
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                        && event.getAction() == KeyEvent.ACTION_UP) {
                    PhotoViewer.getInstance().closePhoto(true, false);
                    return true;
                }
                return super.dispatchKeyEventPreIme(event);
            }
        };
        windowView.setBackgroundDrawable(backgroundDrawable);
        windowView.setFocusable(false);
        if (Build.VERSION.SDK_INT >= 23) {
            windowView.setFitsSystemWindows(true);
        }

        animatingImageView = new ClippingImageView(activity);
        animatingImageView.setAnimationValues(animationValues);
        windowView.addView(animatingImageView, LayoutHelper.createFrame(40, 40));

        containerView = new FrameLayoutDrawer(activity);
        containerView.setFocusable(false);
        windowView.addView(containerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        windowLayoutParams = new WindowManager.LayoutParams();
        windowLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        windowLayoutParams.format = PixelFormat.TRANSLUCENT;
        windowLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowLayoutParams.gravity = Gravity.TOP;
        windowLayoutParams.type = WindowManager.LayoutParams.LAST_APPLICATION_WINDOW;
        windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        actionBar = new ActionBar(activity);
        actionBar.setBackgroundColor(Theme.ACTION_BAR_PHOTO_VIEWER_COLOR);
        actionBar.setOccupyStatusBar(false);
        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.formatString("Of", R.string.Of, 1, 1));
        containerView.addView(actionBar,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    closePhoto(true, false);
                } else if (id == gallery_menu_crop) {
                    switchToEditMode(1);
                } else if (id == gallery_menu_index && placeProvider != null) {
                    if (placeProvider instanceof PreviewEmptyPhotoViewerProvider) {
                        PreviewEmptyPhotoViewerProvider previewProvider = (PreviewEmptyPhotoViewerProvider) placeProvider;
                        previewProvider.selectChanged(currentIndex, !checkImageView.isChecked());
                        checkImageView.setChecked(currentIndex + 1, !checkImageView.isChecked(),
                                true);
                        pickerView.updateSelectedCount(previewProvider.getSelectedCount(), true);
                        return;
                    }
                    placeProvider.setPhotoChecked(currentIndex);
                    if (placeProvider.checkboxEnable()) {
                        int checkeCorner = placeProvider.getCheckeCorner(currentIndex);
                        if (-1 == checkeCorner && !checkImageView.isChecked()) {
                            String hintOfPick = sHintOfPick;
                            String defHint = String.format(Gallery.applicationContext
                                    .getString(R.string.MostSelect), limitPickPhoto);
                            hintOfPick = TextUtils.isEmpty(hintOfPick) ? defHint
                                    : sHintOfPick;
                            AndroidUtilities.showToast(hintOfPick);
                        }

                        checkImageView.setChecked(checkeCorner,
                                placeProvider.isPhotoChecked(currentIndex), true);
                        updateSelectedCount();
                    }
                }
            }

            @Override
            public boolean canOpenMenu() {
                if (currentFileLocation != null) {
                    File f = FileLoader.getPathToAttach(currentFileLocation, avatarsDialogId != 0);
                    if (f.exists()) {
                        return true;
                    }
                }
                return false;
            }
        });

        ActionBarMenu menu = actionBar.createMenu();

        // cropItem = menu.addItemWithWidth(gallery_menu_crop, R.drawable.photo_crop,
        // AndroidUtilities.dp(56));

        checkImageView = new CheckBox(containerView.getContext(), R.drawable.selectphoto_large);
        checkImageView.setDrawBackground(true);
        checkImageView.setSize(32);
        checkImageView.setCheckOffset(AndroidUtilities.dp(1));
        checkImageView.setColor(0xff007aff);
        LinearLayout.LayoutParams params = LayoutHelper.createLinear(32, 32);
        params.gravity = Gravity.CENTER_VERTICAL;
        params.setMargins(0, 0, AndroidUtilities.dp(8), 0);
        checkImageView.setLayoutParams(params);
        indexItem = menu.addItem(gallery_menu_index, checkImageView);

        bottomLayout = new FrameLayout(actvityContext);
        bottomLayout.setBackgroundColor(0x7f000000);
        containerView.addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48,
                Gravity.BOTTOM | Gravity.LEFT));

        radialProgressViews[0] = new RadialProgressView(containerView.getContext(), containerView);
        radialProgressViews[0].setBackgroundState(0, false);
        radialProgressViews[1] = new RadialProgressView(containerView.getContext(), containerView);
        radialProgressViews[1].setBackgroundState(0, false);
        radialProgressViews[2] = new RadialProgressView(containerView.getContext(), containerView);
        radialProgressViews[2].setBackgroundState(0, false);

        pickerView = new PickerBottomLayout(actvityContext);
        pickerView.setBackgroundColor(0x7f000000);
        containerView.addView(pickerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48,
                Gravity.BOTTOM | Gravity.LEFT));

        pickerView.cancelButton.setVisibility(View.GONE);
        // pickerView.cancelButton.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View view) {
        // if (placeProvider != null) {
        // placeProvider.openPreview();
        // }
        // }
        // });
        pickerView.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (placeProvider != null) {
                    if (placeProvider.getSelectedCount() != 0 || placeProvider.isSinglePhoto()) {
                        placeProvider.sendButtonPressed(currentIndex);
                        closePhoto(false, false);
                    }
                }
            }
        });

        editorDoneLayout = new PickerBottomLayout(actvityContext);
        editorDoneLayout.setBackgroundColor(0x7f000000);
        editorDoneLayout.updateSelectedCount(0, false);
        editorDoneLayout.setVisibility(View.GONE);
        containerView.addView(editorDoneLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                48, Gravity.LEFT | Gravity.BOTTOM));

        editorDoneLayout.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentEditMode == 1) {
                    photoCropView.cancelAnimationRunnable();
                }
                switchToEditMode(0);
            }
        });
        editorDoneLayout.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentEditMode == 1) {
                    photoCropView.cancelAnimationRunnable();
                    if (imageMoveAnimation != null) {
                        return;
                    }
                }
                applyCurrentEditMode();
                switchToEditMode(0);
            }
        });

        ImageView rotateButton = new ImageView(actvityContext);
        rotateButton.setScaleType(ImageView.ScaleType.CENTER);
        rotateButton.setImageResource(R.drawable.tool_rotate);
        rotateButton.setBackgroundDrawable(
                Theme.createBarSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR));
        editorDoneLayout.addView(rotateButton, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centerImage.setOrientation(centerImage.getOrientation() - 90, false);
                photoCropView.setOrientation(centerImage.getOrientation());
                containerView.invalidate();
            }
        });

        gestureDetector = new GestureDetector(containerView.getContext(), this);
        gestureDetector.setOnDoubleTapListener(this);

        centerImage.setParentView(containerView);
        centerImage.setCrossfadeAlpha((byte) 2);
        centerImage.setInvalidateAll(true);
        leftImage.setParentView(containerView);
        leftImage.setCrossfadeAlpha((byte) 2);
        leftImage.setInvalidateAll(true);
        rightImage.setParentView(containerView);
        rightImage.setCrossfadeAlpha((byte) 2);
        rightImage.setInvalidateAll(true);

        // WindowManager manager = (WindowManager) Gallery.applicationContext
        // .getSystemService(Activity.WINDOW_SERVICE);
        // int rotation = manager.getDefaultDisplay().getRotation();
        //
        // checkImageView = new CheckBox(containerView.getContext(), R.drawable.selectphoto_large);
        // checkImageView.setDrawBackground(true);
        // checkImageView.setSize(32);
        // checkImageView.setCheckOffset(AndroidUtilities.dp(1));
        // checkImageView.setColor(0xff007aff);
        // checkImageView.setVisibility(View.GONE);
        // containerView.addView(checkImageView,
        // LayoutHelper.createFrame(32, 32, Gravity.RIGHT | Gravity.TOP, 0,
        // rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90 ? 58
        // : 68,
        // 10, 0));
        // checkImageView.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View v) {
        // if (placeProvider != null) {
        // placeProvider.setPhotoChecked(currentIndex);
        // if (placeProvider.checkboxEnable()) {
        // int checkeCorner = placeProvider.getCheckeCorner(currentIndex);
        // if (-1 == checkeCorner && !checkImageView.isChecked()) {
        // Toast.makeText(Gallery.applicationContext,
        // String.format(
        // Gallery.applicationContext
        // .getString(R.string.MostSelect),
        // PhotoAlbumPickerActivity.limitPickPhoto),
        // Toast.LENGTH_SHORT).show();
        // }
        //
        // checkImageView.setChecked(checkeCorner,
        // placeProvider.isPhotoChecked(currentIndex), true);
        // updateSelectedCount();
        // }
        // }
        // }
        // });
    }

    private void showAlertDialog(AlertDialog.Builder builder) {
        if (parentActivity == null) {
            return;
        }
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            visibleDialog = builder.show();
            visibleDialog.setCanceledOnTouchOutside(true);
            visibleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    visibleDialog = null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyCurrentEditMode() {
        Bitmap bitmap = null;
        if (currentEditMode == 1) {
            bitmap = photoCropView.getBitmap();
        }
        if (bitmap != null) {
            PhotoSize size = ImageLoader.scaleAndSaveImage(bitmap, AndroidUtilities.getPhotoSize(),
                    AndroidUtilities.getPhotoSize(), 80, false, 101, 101);
            if (size != null) {
                Object object = imagesArrLocals.get(currentIndex);
                if (object instanceof MediaController.PhotoEntry) {
                    MediaController.PhotoEntry entry = (MediaController.PhotoEntry) object;
                    entry.imagePath = FileLoader.getPathToAttach(size, true).toString();
                    size = ImageLoader.scaleAndSaveImage(bitmap, AndroidUtilities.dp(120),
                            AndroidUtilities.dp(120), 70, false, 101, 101);
                    if (size != null) {
                        entry.thumbPath = FileLoader.getPathToAttach(size, true).toString();
                    }
                } else if (object instanceof MediaController.SearchImage) {
                    MediaController.SearchImage entry = (MediaController.SearchImage) object;
                    entry.imagePath = FileLoader.getPathToAttach(size, true).toString();
                    size = ImageLoader.scaleAndSaveImage(bitmap, AndroidUtilities.dp(120),
                            AndroidUtilities.dp(120), 70, false, 101, 101);
                    if (size != null) {
                        entry.thumbPath = FileLoader.getPathToAttach(size, true).toString();
                    }
                }
                if (sendPhotoType == 0 && placeProvider != null) {
                    placeProvider.updatePhotoAtIndex(currentIndex);
                    if (!placeProvider.isPhotoChecked(currentIndex)) {
                        placeProvider.setPhotoChecked(currentIndex);
                        checkImageView.setChecked(placeProvider.isPhotoChecked(currentIndex), true);
                        updateSelectedCount();
                    }
                }
                if (currentEditMode == 1) {
                    float scaleX = photoCropView.getRectSizeX() / (float) getContainerViewWidth();
                    float scaleY = photoCropView.getRectSizeY() / (float) getContainerViewHeight();
                    scale = scaleX > scaleY ? scaleX : scaleY;
                    translationX = photoCropView.getRectX() + photoCropView.getRectSizeX() / 2
                            - getContainerViewWidth() / 2;
                    translationY = photoCropView.getRectY() + photoCropView.getRectSizeY() / 2
                            - getContainerViewHeight() / 2;
                    zoomAnimation = true;
                }
                centerImage.setParentView(null);
                centerImage.setOrientation(0, true);
                centerImage.setImageBitmap(bitmap);
                centerImage.setParentView(containerView);
            }
        }
    }

    private void switchToEditMode(final int mode) {
        if (currentEditMode == mode || centerImage.getBitmap() == null
                || changeModeAnimation != null || imageMoveAnimation != null
                || radialProgressViews[0].backgroundState != -1) {
            return;
        }
        if (mode == 0) {
            Bitmap bitmap = centerImage.getBitmap();
            if (bitmap != null) {
                int bitmapWidth = centerImage.getBitmapWidth();
                int bitmapHeight = centerImage.getBitmapHeight();

                float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
                float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
                float newScaleX = (float) getContainerViewWidth(0) / (float) bitmapWidth;
                float newScaleY = (float) getContainerViewHeight(0) / (float) bitmapHeight;
                float scale = scaleX > scaleY ? scaleY : scaleX;
                float newScale = newScaleX > newScaleY ? newScaleY : newScaleX;

                animateToScale = newScale / scale;
                animateToX = 0;
                if (currentEditMode == 1) {
                    animateToY = AndroidUtilities.dp(24);
                } else if (currentEditMode == 2) {
                    animateToY = AndroidUtilities.dp(62);
                }
                animationStartTime = System.currentTimeMillis();
                zoomAnimation = true;
            }

            imageMoveAnimation = new AnimatorSet();
            if (currentEditMode == 1) {
                imageMoveAnimation.playTogether(
                        ObjectAnimator.ofFloat(editorDoneLayout, "translationY",
                                AndroidUtilities.dp(48)),
                        ObjectAnimator.ofFloat(PhotoViewer.this, "animationValue", 0, 1),
                        ObjectAnimator.ofFloat(photoCropView, "alpha", 0));
            }
            imageMoveAnimation.setDuration(200);
            imageMoveAnimation.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (currentEditMode == 1) {
                        editorDoneLayout.setVisibility(View.GONE);
                        photoCropView.setVisibility(View.GONE);
                    }
                    imageMoveAnimation = null;
                    currentEditMode = mode;
                    animateToScale = 1;
                    animateToX = 0;
                    animateToY = 0;
                    scale = 1;
                    updateMinMax(scale);
                    containerView.invalidate();

                    AnimatorSet animatorSet = new AnimatorSet();
                    ArrayList<Animator> arrayList = new ArrayList<>();
                    arrayList.add(ObjectAnimator.ofFloat(pickerView, "translationY", 0));
                    arrayList.add(ObjectAnimator.ofFloat(actionBar, "translationY", 0));
                    if (sendPhotoType == 0) {
                        arrayList.add(ObjectAnimator.ofFloat(checkImageView, "alpha", 1));
                    }
                    animatorSet.playTogether(arrayList);
                    animatorSet.setDuration(200);
                    animatorSet.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            pickerView.setVisibility(View.VISIBLE);
                            actionBar.setVisibility(View.VISIBLE);
                            if (sendPhotoType == 0) {
                                checkImageView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    animatorSet.start();
                }
            });
            imageMoveAnimation.start();
        } else if (mode == 1) {
            if (photoCropView == null) {
                photoCropView = new PhotoCropView(actvityContext);
                photoCropView.setVisibility(View.GONE);
                containerView.addView(photoCropView,
                        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 0, 0,
                                48));
                photoCropView.setDelegate(new PhotoCropView.PhotoCropViewDelegate() {
                    @Override
                    public void needMoveImageTo(float x, float y, float s, boolean animated) {
                        if (animated) {
                            animateTo(s, x, y, true);
                        } else {
                            translationX = x;
                            translationY = y;
                            scale = s;
                            containerView.invalidate();
                        }
                    }

                    @Override
                    public Bitmap getBitmap() {
                        return centerImage.getBitmap();
                    }
                });
            }

            editorDoneLayout.doneButtonTextView
                    .setText(LocaleController.getString("Crop", R.string.Crop));
            changeModeAnimation = new AnimatorSet();
            ArrayList<Animator> arrayList = new ArrayList<>();
            arrayList.add(
                    ObjectAnimator.ofFloat(pickerView, "translationY", 0, AndroidUtilities.dp(96)));
            arrayList.add(
                    ObjectAnimator.ofFloat(actionBar, "translationY", 0, -actionBar.getHeight()));
            if (sendPhotoType == 0) {
                arrayList.add(ObjectAnimator.ofFloat(checkImageView, "alpha", 1, 0));
            }
            changeModeAnimation.playTogether(arrayList);
            changeModeAnimation.setDuration(200);
            changeModeAnimation.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    changeModeAnimation = null;
                    pickerView.setVisibility(View.GONE);
                    actionBar.setVisibility(View.GONE);
                    if (sendPhotoType == 0) {
                        checkImageView.setVisibility(View.GONE);
                    }

                    Bitmap bitmap = centerImage.getBitmap();
                    if (bitmap != null) {
                        photoCropView.setBitmap(bitmap, centerImage.getOrientation(),
                                sendPhotoType != 1);
                        int bitmapWidth = centerImage.getBitmapWidth();
                        int bitmapHeight = centerImage.getBitmapHeight();

                        float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
                        float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
                        float newScaleX = (float) getContainerViewWidth(1) / (float) bitmapWidth;
                        float newScaleY = (float) getContainerViewHeight(1) / (float) bitmapHeight;
                        float scale = scaleX > scaleY ? scaleY : scaleX;
                        float newScale = newScaleX > newScaleY ? newScaleY : newScaleX;

                        animateToScale = newScale / scale;
                        animateToX = 0;
                        animateToY = -AndroidUtilities.dp(24);
                        animationStartTime = System.currentTimeMillis();
                        zoomAnimation = true;
                    }

                    imageMoveAnimation = new AnimatorSet();
                    imageMoveAnimation.playTogether(
                            ObjectAnimator.ofFloat(editorDoneLayout, "translationY",
                                    AndroidUtilities.dp(48), 0),
                            ObjectAnimator.ofFloat(PhotoViewer.this, "animationValue", 0, 1),
                            ObjectAnimator.ofFloat(photoCropView, "alpha", 0, 1));
                    imageMoveAnimation.setDuration(200);
                    imageMoveAnimation.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            editorDoneLayout.setVisibility(View.VISIBLE);
                            photoCropView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            imageMoveAnimation = null;
                            currentEditMode = mode;
                            animateToScale = 1;
                            animateToX = 0;
                            animateToY = 0;
                            scale = 1;
                            updateMinMax(scale);
                            containerView.invalidate();
                        }
                    });
                    imageMoveAnimation.start();
                }
            });
            changeModeAnimation.start();
        }
    }

    private void toggleCheckImageView(boolean show) {
        AnimatorSet animatorSet = new AnimatorSet();
        ArrayList<Animator> arrayList = new ArrayList<>();
        arrayList.add(ObjectAnimator.ofFloat(pickerView, "alpha", show ? 1.0f : 0.0f));
        if (sendPhotoType == 0) {
            arrayList.add(ObjectAnimator.ofFloat(checkImageView, "alpha", show ? 1.0f : 0.0f));
        }
        animatorSet.playTogether(arrayList);
        animatorSet.setDuration(200);
        animatorSet.start();
    }

    private void toggleActionBar(boolean show, final boolean animated) {
        if (show) {
            actionBar.setVisibility(View.VISIBLE);
            if (canShowBottom) {
                bottomLayout.setVisibility(View.VISIBLE);
            }
        }
        isActionBarVisible = show;
        actionBar.setEnabled(show);
        bottomLayout.setEnabled(show);

        if (animated) {
            ArrayList<Animator> arrayList = new ArrayList<>();
            arrayList.add(ObjectAnimator.ofFloat(actionBar, "alpha", show ? 1.0f : 0.0f));
            arrayList.add(ObjectAnimator.ofFloat(bottomLayout, "alpha", show ? 1.0f : 0.0f));
            currentActionBarAnimation = new AnimatorSet();
            currentActionBarAnimation.playTogether(arrayList);
            if (!show) {
                currentActionBarAnimation.addListener(new AnimatorListenerAdapterProxy() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (currentActionBarAnimation != null
                                && currentActionBarAnimation.equals(animation)) {
                            actionBar.setVisibility(View.GONE);
                            if (canShowBottom) {
                                bottomLayout.setVisibility(View.GONE);
                            }
                            currentActionBarAnimation = null;
                        }
                    }
                });
            }

            currentActionBarAnimation.setDuration(200);
            currentActionBarAnimation.start();
        } else {
            actionBar.setAlpha(show ? 1.0f : 0.0f);
            bottomLayout.setAlpha(show ? 1.0f : 0.0f);
            if (!show) {
                actionBar.setVisibility(View.GONE);
                if (canShowBottom) {
                    bottomLayout.setVisibility(View.GONE);
                }
            }
        }
    }

    private String getFileName(int index) {
        if (index < 0) {
            return null;
        }
        if (!imagesArrLocations.isEmpty()) {
            if (!imagesArrLocations.isEmpty()) {
                if (index >= imagesArrLocations.size()) {
                    return null;
                }
                FileLocation location = imagesArrLocations.get(index);
                return location.volume_id + "_" + location.local_id + ".jpg";
            }
        } else if (!imagesArrLocals.isEmpty()) {
            if (index >= imagesArrLocals.size()) {
                return null;
            }
            Object object = imagesArrLocals.get(index);
            if (object instanceof MediaController.SearchImage) {
                MediaController.SearchImage searchImage = ((MediaController.SearchImage) object);
                if (searchImage.document != null) {
                    return FileLoader.getAttachFileName(searchImage.document);
                } else if (searchImage.type != 1 && searchImage.localUrl != null
                        && searchImage.localUrl.length() > 0) {
                    File file = new File(searchImage.localUrl);
                    if (file.exists()) {
                        return file.getName();
                    } else {
                        searchImage.localUrl = "";
                    }
                }
                return Utilities.MD5(searchImage.imageUrl) + "."
                        + ImageLoader.getHttpUrlExtension(searchImage.imageUrl, "jpg");
            }
        }
        return null;
    }

    private FileLocation getFileLocation(int index, int size[]) {
        if (index < 0) {
            return null;
        }
        if (!imagesArrLocations.isEmpty()) {
            if (index >= imagesArrLocations.size()) {
                return null;
            }
            size[0] = imagesArrLocationsSizes.get(index);
            return imagesArrLocations.get(index);
        }
        return null;
    }

    private void updateSelectedCount() {
        if (placeProvider == null) {
            return;
        }
        pickerView.updateSelectedCount(placeProvider.getSelectedCount(), false);
    }

    private void onPhotoShow(final FileLocation fileLocation, final List<Object> photos,
            int index,
            final PlaceProviderObject object) {
        classGuid = BaseFragment.lastClassGuid++;
        currentFileLocation = null;
        currentPathObject = null;
        currentIndex = -1;
        currentFileNames[0] = null;
        currentFileNames[1] = null;
        currentFileNames[2] = null;
        avatarsDialogId = 0;
        totalImagesCount = 0;
        totalImagesCountMerge = 0;
        currentEditMode = 0;
        isFirstLoading = true;
        needSearchImageInArr = false;
        loadingMoreImages = false;
        endReached[0] = false;
        endReached[1] = mergeDialogId == 0;
        opennedFromMedia = false;
        canShowBottom = true;
        imagesArrLocations.clear();
        imagesArrLocationsSizes.clear();
        avatarsArr.clear();
        imagesArrLocals.clear();
        currentUserAvatarLocation = null;
        containerView.setPadding(0, 0, 0, 0);
        currentThumb = object != null ? object.thumb : null;
        bottomLayout.setVisibility(View.VISIBLE);
        actionBar.setTranslationY(0);
        pickerView.setTranslationY(0);
        checkImageView.setAlpha(1.0f);
        pickerView.setAlpha(1.0f);
        checkImageView.setVisibility(isSelectPreview ? View.VISIBLE : View.GONE);
        pickerView.setVisibility(isSelectPreview ? View.VISIBLE : View.GONE);
        // cropItem.setVisibility(View.GONE);
        editorDoneLayout.setVisibility(View.GONE);
        if (photoCropView != null) {
            photoCropView.setVisibility(View.GONE);
        }

        for (int a = 0; a < 3; a++) {
            if (radialProgressViews[a] != null) {
                radialProgressViews[a].setBackgroundState(-1, false);
            }
        }

        if (fileLocation != null) {
            avatarsDialogId = object.dialogId;
            imagesArrLocations.add(fileLocation);
            imagesArrLocationsSizes.add(object.size);
            avatarsArr.add(new Photo.TL_photoEmpty());
            setImageIndex(0, true);
            currentUserAvatarLocation = fileLocation;
        } else if (photos != null) {
            if (sendPhotoType == 0) {
                checkImageView.setVisibility(View.VISIBLE);
            }
            imagesArrLocals.addAll(photos);
            setImageIndex(index, true);
            pickerView.setVisibility(View.VISIBLE);
            bottomLayout.setVisibility(View.GONE);
            canShowBottom = false;
            Object obj = imagesArrLocals.get(index);
            // cropItem.setVisibility(placeProvider.isSinglePhoto() ? View.VISIBLE : View.GONE);
            updateSelectedCount();
        }
    }

    private void setImages() {
        if (animationInProgress == 0) {
            setIndexToImage(centerImage, currentIndex);
            setIndexToImage(rightImage, currentIndex + 1);
            setIndexToImage(leftImage, currentIndex - 1);
        }
    }

    private void setImageIndex(int index, boolean init) {
        if (currentIndex == index || placeProvider == null) {
            return;
        }
        if (!init) {
            currentThumb = null;
        }
        currentFileNames[0] = getFileName(index);
        currentFileNames[1] = getFileName(index + 1);
        currentFileNames[2] = getFileName(index - 1);
        placeProvider.willSwitchFromPhoto(currentFileLocation, currentIndex);
        int prevIndex = currentIndex;
        currentIndex = index;
        boolean isVideo = false;
        boolean sameImage = false;

        if (!imagesArrLocations.isEmpty()) {
            FileLocation old = currentFileLocation;
            if (index < 0 || index >= imagesArrLocations.size()) {
                closePhoto(false, false);
                return;
            }
            currentFileLocation = imagesArrLocations.get(index);
            if (old != null && currentFileLocation != null
                    && old.local_id == currentFileLocation.local_id
                    && old.volume_id == currentFileLocation.volume_id) {
                sameImage = true;
            }
            actionBar.setTitle(LocaleController.formatString("Of", R.string.Of, currentIndex + 1,
                    imagesArrLocations.size()));
        } else if (!imagesArrLocals.isEmpty()) {
            Object object = imagesArrLocals.get(index);
            if (index < 0 || index >= imagesArrLocals.size()) {
                closePhoto(false, false);
                return;
            }
            boolean fromCamera = false;
            if (object instanceof MediaController.PhotoEntry) {
                currentPathObject = ((MediaController.PhotoEntry) object).path;
                fromCamera = ((MediaController.PhotoEntry) object).bucketId == 0
                        && ((MediaController.PhotoEntry) object).dateTaken == 0
                        && imagesArrLocals.size() == 1;
            } else if (object instanceof MediaController.SearchImage) {
                MediaController.SearchImage searchImage = (MediaController.SearchImage) object;
                if (searchImage.document != null) {
                    currentPathObject = FileLoader.getPathToAttach(searchImage.document, true)
                            .getAbsolutePath();
                } else {
                    currentPathObject = searchImage.imageUrl;
                }
            }
            if (fromCamera) {
                actionBar.setTitle(LocaleController.getString("AttachPhoto", R.string.AttachPhoto));
            } else {
                actionBar.setTitle(LocaleController.formatString("Of", R.string.Of,
                        currentIndex + 1, imagesArrLocals.size()));
            }
            if (sendPhotoType == 0) {
                checkImageView.setChecked(placeProvider.getCheckeCorner(currentIndex),
                        placeProvider.isPhotoChecked(currentIndex), false);
            }
        }

        if (currentPlaceObject != null) {
            if (animationInProgress == 0) {
                currentPlaceObject.imageReceiver.setVisible(true, true);
            } else {
                showAfterAnimation = currentPlaceObject;
            }
        }
        currentPlaceObject = placeProvider.getPlaceForPhoto(currentFileLocation, currentIndex);
        if (currentPlaceObject != null) {
            if (animationInProgress == 0) {
                currentPlaceObject.imageReceiver.setVisible(false, true);
            } else {
                hideAfterAnimation = currentPlaceObject;
            }
        }

        if (!sameImage) {
            draggingDown = false;
            translationX = 0;
            translationY = 0;
            scale = 1;
            animateToX = 0;
            animateToY = 0;
            animateToScale = 1;
            animationStartTime = 0;
            imageMoveAnimation = null;
            changeModeAnimation = null;
            if (aspectRatioFrameLayout != null) {
                aspectRatioFrameLayout.setVisibility(View.INVISIBLE);
            }

            pinchStartDistance = 0;
            pinchStartScale = 1;
            pinchCenterX = 0;
            pinchCenterY = 0;
            pinchStartX = 0;
            pinchStartY = 0;
            moveStartX = 0;
            moveStartY = 0;
            zooming = false;
            moving = false;
            doubleTap = false;
            invalidCoords = false;
            canDragDown = true;
            changingPage = false;
            switchImageAfterAnimation = 0;
            canZoom = !imagesArrLocals.isEmpty() || (currentFileNames[0] != null && !isVideo
                    && radialProgressViews[0].backgroundState != 0);
            updateMinMax(scale);
        }

        if (prevIndex == -1) {
            setImages();

            for (int a = 0; a < 3; a++) {
                checkProgress(a, false);
            }
        } else {
            checkProgress(0, false);
            if (prevIndex > currentIndex) {
                ImageReceiver temp = rightImage;
                rightImage = centerImage;
                centerImage = leftImage;
                leftImage = temp;

                RadialProgressView tempProgress = radialProgressViews[0];
                radialProgressViews[0] = radialProgressViews[2];
                radialProgressViews[2] = tempProgress;
                setIndexToImage(leftImage, currentIndex - 1);

                checkProgress(1, false);
                checkProgress(2, false);
            } else if (prevIndex < currentIndex) {
                ImageReceiver temp = leftImage;
                leftImage = centerImage;
                centerImage = rightImage;
                rightImage = temp;

                RadialProgressView tempProgress = radialProgressViews[0];
                radialProgressViews[0] = radialProgressViews[1];
                radialProgressViews[1] = tempProgress;
                setIndexToImage(rightImage, currentIndex + 1);

                checkProgress(1, false);
                checkProgress(2, false);
            }
        }
    }

    private void checkProgress(int a, boolean animated) {
        if (currentFileNames[a] != null) {
            int index = currentIndex;
            if (a == 1) {
                index += 1;
            } else if (a == 2) {
                index -= 1;
            }
            File f = null;
            boolean isVideo = false;
            if (currentFileLocation != null) {
                FileLocation location = imagesArrLocations.get(index);
                f = FileLoader.getPathToAttach(location, avatarsDialogId != 0);
            } else if (currentPathObject != null) {
                f = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_DOCUMENT),
                        currentFileNames[a]);
                if (!f.exists()) {
                    f = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE),
                            currentFileNames[a]);
                }
            }
            if (f != null && f.exists()) {
                if (isVideo) {
                    radialProgressViews[a].setBackgroundState(3, animated);
                } else {
                    radialProgressViews[a].setBackgroundState(-1, animated);
                }
            } else {
                if (isVideo) {
                    if (!FileLoader.getInstance().isLoadingFile(currentFileNames[a])) {
                        radialProgressViews[a].setBackgroundState(2, false);
                    } else {
                        radialProgressViews[a].setBackgroundState(1, false);
                    }
                } else {
                    radialProgressViews[a].setBackgroundState(0, animated);
                }
                Float progress = ImageLoader.getInstance().getFileProgress(currentFileNames[a]);
                if (progress == null) {
                    progress = 0.0f;
                }
                radialProgressViews[a].setProgress(progress, false);
            }
            if (a == 0) {
                canZoom = !imagesArrLocals.isEmpty() || (currentFileNames[0] != null && !isVideo
                        && radialProgressViews[0].backgroundState != 0);
            }
        } else {
            radialProgressViews[a].setBackgroundState(-1, animated);
        }
    }

    private void setIndexToImage(ImageReceiver imageReceiver, int index) {
        imageReceiver.setOrientation(0, false);
        if (!imagesArrLocals.isEmpty()) {
            if (index >= 0 && index < imagesArrLocals.size()) {
                Object object = imagesArrLocals.get(index);
                int size = (int) (AndroidUtilities.getPhotoSize() / AndroidUtilities.density);
                Bitmap placeHolder = null;
                if (currentThumb != null && imageReceiver == centerImage) {
                    placeHolder = currentThumb;
                }
                if (placeHolder == null) {
                    placeHolder = placeProvider.getThumbForPhoto(null, index);
                }
                String path = null;
                Document document = null;
                int imageSize = 0;
                String filter = null;
                if (object instanceof MediaController.PhotoEntry) {
                    MediaController.PhotoEntry photoEntry = (MediaController.PhotoEntry) object;
                    if (photoEntry.imagePath != null) {
                        path = photoEntry.imagePath;
                    } else {
                        imageReceiver.setOrientation(photoEntry.orientation, false);
                        path = photoEntry.path;
                    }
                    filter = String.format(Locale.US, "%d_%d", size, size);
                } else if (object instanceof MediaController.SearchImage) {
                    MediaController.SearchImage photoEntry = (MediaController.SearchImage) object;
                    if (photoEntry.imagePath != null) {
                        path = photoEntry.imagePath;
                    } else if (photoEntry.document != null) {
                        document = photoEntry.document;
                        imageSize = photoEntry.document.size;
                    } else {
                        path = photoEntry.imageUrl;
                        imageSize = photoEntry.size;
                    }
                    filter = "d";
                }
                if (document != null) {
                    imageReceiver.setImage(document, null, "d",
                            placeHolder != null ? new BitmapDrawable(null, placeHolder) : null,
                            placeHolder == null ? document.thumb.location : null,
                            String.format(Locale.US, "%d_%d", size, size), imageSize, null, false);
                } else {
                    imageReceiver.setImage(path, filter,
                            placeHolder != null ? new BitmapDrawable(null, placeHolder) : null,
                            null, imageSize);
                }
            } else {
                imageReceiver.setImageBitmap((Bitmap) null);
            }
        } else {
            int size[] = new int[1];
            FileLocation fileLocation = getFileLocation(index, size);

            if (fileLocation != null) {
                imageReceiver.setNeedsQualityThumb(false);
                Bitmap placeHolder = null;
                if (currentThumb != null && imageReceiver == centerImage) {
                    placeHolder = currentThumb;
                }
                if (size[0] == 0) {
                    size[0] = -1;
                }
                imageReceiver.setImage(fileLocation, null, null,
                        placeHolder != null ? new BitmapDrawable(null, placeHolder) : null, null,
                        "b", size[0],
                        null, avatarsDialogId != 0);
            } else {
                imageReceiver.setNeedsQualityThumb(false);
                if (size[0] == 0) {
                    imageReceiver.setImageBitmap((Bitmap) null);
                } else {
                    imageReceiver.setImageBitmap(parentActivity.getResources()
                            .getDrawable(R.drawable.photoview_placeholder));
                }
            }
        }
    }

    public boolean isShowingImage(FileLocation object) {
        return isVisible && !disableShowCheck && object != null && currentFileLocation != null
                && object.local_id == currentFileLocation.local_id
                && object.volume_id == currentFileLocation.volume_id
                && object.dc_id == currentFileLocation.dc_id;
    }

    public boolean isShowingImage(String object) {
        return isVisible && !disableShowCheck && object != null && currentPathObject != null
                && object.equals(currentPathObject);
    }

    public void openPhotoForSelect(final List<Object> photos, boolean selectPreview,
            final int index, int type,
            final PhotoViewerProvider provider) {
        isSelectPreview = selectPreview;
        sendPhotoType = type;
        if (pickerView != null) {
            pickerView.doneButtonTextView
                    .setText(LocaleController.getString("Send", R.string.select).toUpperCase());
        }
        openPhoto(null, photos, index, provider, 0, 0);
    }

    private boolean checkAnimation() {
        if (animationInProgress != 0) {
            if (Math.abs(transitionAnimationStartTime - System.currentTimeMillis()) >= 500) {
                if (animationEndRunnable != null) {
                    animationEndRunnable.run();
                    animationEndRunnable = null;
                }
                animationInProgress = 0;
            }
        }
        return animationInProgress != 0;
    }

    public void openPhoto(final FileLocation fileLocation, final List<Object> photos,
            final int index, final PhotoViewerProvider provider,
            long dialogId, long mDialogId) {
        if (parentActivity == null || isVisible || provider == null && checkAnimation()
                || fileLocation == null && photos == null) {
            return;
        }

        final PlaceProviderObject placeProviderObject = provider.getPlaceForPhoto(fileLocation, index);
        if (placeProviderObject == null && photos == null) {
            return;
        }

        WindowManager wm = (WindowManager) parentActivity.getSystemService(Context.WINDOW_SERVICE);
        if (windowView.attachedToWindow) {
            try {
                wm.removeView(windowView);
            } catch (Exception e) {
                // don't promt
            }
        }

        try {
            windowLayoutParams.type = WindowManager.LayoutParams.LAST_APPLICATION_WINDOW;
            windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED;
            windowView.setFocusable(false);
            containerView.setFocusable(false);
            wm.addView(windowView, windowLayoutParams);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        actionBar.setTitle(LocaleController.formatString("Of", R.string.Of, 1, 1));
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidFailedLoad);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileDidLoaded);
        NotificationCenter.getInstance().addObserver(this,
                NotificationCenter.FileLoadProgressChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.mediaCountDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.mediaDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.dialogPhotosLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);

        placeProvider = provider;
        mergeDialogId = mDialogId;
        currentDialogId = dialogId;

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        isVisible = true;
        toggleActionBar(true, false);

        if (placeProviderObject != null) {
            disableShowCheck = true;
            animationInProgress = 1;

            onPhotoShow(fileLocation, photos, index, placeProviderObject);

            final Rect drawRegion = placeProviderObject.imageReceiver.getDrawRegion();
            int orientation = placeProviderObject.imageReceiver.getOrientation();

            animatingImageView.setVisibility(View.VISIBLE);
            animatingImageView.setRadius(placeProviderObject.radius);
            animatingImageView.setOrientation(orientation);
            animatingImageView.setNeedRadius(placeProviderObject.radius != 0);
            animatingImageView.setImageBitmap(placeProviderObject.thumb);

            animatingImageView.setAlpha(1.0f);
            animatingImageView.setPivotX(0.0f);
            animatingImageView.setPivotY(0.0f);
            animatingImageView.setScaleX(placeProviderObject.scale);
            animatingImageView.setScaleY(placeProviderObject.scale);
            animatingImageView.setTranslationX(placeProviderObject.viewX + drawRegion.left * placeProviderObject.scale);
            animatingImageView.setTranslationY(placeProviderObject.viewY + drawRegion.top * placeProviderObject.scale);
            final ViewGroup.LayoutParams layoutParams = animatingImageView.getLayoutParams();
            layoutParams.width = (drawRegion.right - drawRegion.left);
            layoutParams.height = (drawRegion.bottom - drawRegion.top);
            animatingImageView.setLayoutParams(layoutParams);

            float scaleX = (float) AndroidUtilities.displaySize.x / layoutParams.width;
            float scaleY = (float) (AndroidUtilities.displaySize.y
                    - AndroidUtilities.statusBarHeight) / layoutParams.height;
            float scale = scaleX > scaleY ? scaleY : scaleX;
            float width = layoutParams.width * scale;
            float height = layoutParams.height * scale;
            float xPos = (AndroidUtilities.displaySize.x - width) / 2.0f;
            float yPos = (AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight
                    - height) / 2.0f;
            int clipHorizontal = Math.abs(drawRegion.left - placeProviderObject.imageReceiver.getImageX());
            int clipVertical = Math.abs(drawRegion.top - placeProviderObject.imageReceiver.getImageY());

            int coords2[] = new int[2];
            placeProviderObject.parentView.getLocationInWindow(coords2);
            int clipTop = coords2[1] - AndroidUtilities.statusBarHeight
                    - (placeProviderObject.viewY + drawRegion.top) + placeProviderObject.clipTopAddition;
            if (clipTop < 0) {
                clipTop = 0;
            }
            int clipBottom = (placeProviderObject.viewY + drawRegion.top + layoutParams.height) - (coords2[1]
                    + placeProviderObject.parentView.getHeight() - AndroidUtilities.statusBarHeight)
                    + placeProviderObject.clipBottomAddition;
            if (clipBottom < 0) {
                clipBottom = 0;
            }
            clipTop = Math.max(clipTop, clipVertical);
            clipBottom = Math.max(clipBottom, clipVertical);

            animationValues[0][0] = animatingImageView.getScaleX();
            animationValues[0][1] = animatingImageView.getScaleY();
            animationValues[0][2] = animatingImageView.getTranslationX();
            animationValues[0][3] = animatingImageView.getTranslationY();
            animationValues[0][4] = clipHorizontal * placeProviderObject.scale;
            animationValues[0][5] = clipTop * placeProviderObject.scale;
            animationValues[0][6] = clipBottom * placeProviderObject.scale;
            animationValues[0][7] = animatingImageView.getRadius();

            animationValues[1][0] = scale;
            animationValues[1][1] = scale;
            animationValues[1][2] = xPos;
            animationValues[1][3] = yPos;
            animationValues[1][4] = 0;
            animationValues[1][5] = 0;
            animationValues[1][6] = 0;
            animationValues[1][7] = 0;

            animatingImageView.setAnimationProgress(0);
            backgroundDrawable.setAlpha(0);
            containerView.setAlpha(0);

            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(animatingImageView, "animationProgress", 0.0f, 1.0f),
                    ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0, 255),
                    ObjectAnimator.ofFloat(containerView, "alpha", 0.0f, 1.0f));

            animationEndRunnable = new Runnable() {
                @Override
                public void run() {
                    if (containerView == null || windowView == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 18) {
                        containerView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    animationInProgress = 0;
                    transitionAnimationStartTime = 0;
                    setImages();
                    containerView.invalidate();
                    animatingImageView.setVisibility(View.GONE);
                    if (showAfterAnimation != null) {
                        showAfterAnimation.imageReceiver.setVisible(true, true);
                    }
                    if (hideAfterAnimation != null) {
                        hideAfterAnimation.imageReceiver.setVisible(false, true);
                    }
                    if (photos != null) {
                        windowLayoutParams.flags = 0;
                        windowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                        WindowManager wm = (WindowManager) parentActivity
                                .getSystemService(Context.WINDOW_SERVICE);
                        wm.updateViewLayout(windowView, windowLayoutParams);
                        windowView.setFocusable(true);
                        containerView.setFocusable(true);
                    }
                }
            };

            animatorSet.setDuration(200);
            animatorSet.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().setAnimationInProgress(false);
                            if (animationEndRunnable != null) {
                                animationEndRunnable.run();
                                animationEndRunnable = null;
                            }
                        }
                    });
                }
            });
            transitionAnimationStartTime = System.currentTimeMillis();
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance()
                            .setAllowedNotificationsDutingAnimation(new int[] {
                                    NotificationCenter.dialogsNeedReload,
                                    NotificationCenter.closeChats,
                                    NotificationCenter.mediaCountDidLoaded,
                                    NotificationCenter.mediaDidLoaded,
                                    NotificationCenter.dialogPhotosLoaded
                    });
                    NotificationCenter.getInstance().setAnimationInProgress(true);
                    animatorSet.start();
                }
            });
            if (Build.VERSION.SDK_INT >= 18) {
                containerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            backgroundDrawable.drawRunnable = new Runnable() {
                @Override
                public void run() {
                    disableShowCheck = false;
                    placeProviderObject.imageReceiver.setVisible(false, true);
                }
            };
        } else {
            if (photos != null) {
                windowLayoutParams.flags = 0;
                windowLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                wm.updateViewLayout(windowView, windowLayoutParams);
                windowView.setFocusable(true);
                containerView.setFocusable(true);
            }

            backgroundDrawable.setAlpha(255);
            containerView.setAlpha(1.0f);
            onPhotoShow(fileLocation, photos, index, placeProviderObject);
        }
    }

    public void closePhoto(boolean animated, boolean fromEditMode) {
        if (!fromEditMode && currentEditMode != 0) {
            if (currentEditMode == 1) {
                photoCropView.cancelAnimationRunnable();
            }
            switchToEditMode(0);
            return;
        }
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (currentEditMode != 0) {
            if (currentEditMode == 1) {
                editorDoneLayout.setVisibility(View.GONE);
                photoCropView.setVisibility(View.GONE);
            }
            currentEditMode = 0;
        }

        if (parentActivity == null || !isVisible || checkAnimation() || placeProvider == null) {
            return;
        }

        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileDidFailedLoad);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileDidLoaded);
        NotificationCenter.getInstance().removeObserver(this,
                NotificationCenter.FileLoadProgressChanged);
        NotificationCenter.getInstance().removeObserver(this,
                NotificationCenter.mediaCountDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.mediaDidLoaded);
        NotificationCenter.getInstance().removeObserver(this,
                NotificationCenter.dialogPhotosLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);

        isActionBarVisible = false;

        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
        final PlaceProviderObject object = placeProvider.getPlaceForPhoto(currentFileLocation,
                currentIndex);

        if (animated) {
            animationInProgress = 1;
            animatingImageView.setVisibility(View.VISIBLE);
            containerView.invalidate();

            AnimatorSet animatorSet = new AnimatorSet();

            final ViewGroup.LayoutParams layoutParams = animatingImageView.getLayoutParams();
            Rect drawRegion = null;
            animatingImageView.setOrientation(centerImage.getOrientation());
            if (object != null) {
                animatingImageView.setNeedRadius(object.radius != 0);
                drawRegion = object.imageReceiver.getDrawRegion();
                layoutParams.width = drawRegion.right - drawRegion.left;
                layoutParams.height = drawRegion.bottom - drawRegion.top;
                animatingImageView.setImageBitmap(object.thumb);
            } else {
                animatingImageView.setNeedRadius(false);
                layoutParams.width = centerImage.getImageWidth();
                layoutParams.height = centerImage.getImageHeight();
                animatingImageView.setImageBitmap(centerImage.getBitmap());
            }
            animatingImageView.setLayoutParams(layoutParams);

            float scaleX = (float) AndroidUtilities.displaySize.x / layoutParams.width;
            float scaleY = (float) (AndroidUtilities.displaySize.y
                    - AndroidUtilities.statusBarHeight) / layoutParams.height;
            float scale2 = scaleX > scaleY ? scaleY : scaleX;
            float width = layoutParams.width * scale * scale2;
            float height = layoutParams.height * scale * scale2;
            float xPos = (AndroidUtilities.displaySize.x - width) / 2.0f;
            float yPos = (AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight
                    - height) / 2.0f;
            animatingImageView.setTranslationX(xPos + translationX);
            animatingImageView.setTranslationY(yPos + translationY);
            animatingImageView.setScaleX(scale * scale2);
            animatingImageView.setScaleY(scale * scale2);

            if (object != null) {
                object.imageReceiver.setVisible(false, true);
                int clipHorizontal = Math.abs(drawRegion.left - object.imageReceiver.getImageX());
                int clipVertical = Math.abs(drawRegion.top - object.imageReceiver.getImageY());

                int coords2[] = new int[2];
                object.parentView.getLocationInWindow(coords2);
                int clipTop = coords2[1] - AndroidUtilities.statusBarHeight
                        - (object.viewY + drawRegion.top) + object.clipTopAddition;
                if (clipTop < 0) {
                    clipTop = 0;
                }
                int clipBottom = (object.viewY + drawRegion.top
                        + (drawRegion.bottom - drawRegion.top))
                        - (coords2[1] + object.parentView.getHeight()
                                - AndroidUtilities.statusBarHeight)
                        + object.clipBottomAddition;
                if (clipBottom < 0) {
                    clipBottom = 0;
                }

                clipTop = Math.max(clipTop, clipVertical);
                clipBottom = Math.max(clipBottom, clipVertical);

                animationValues[0][0] = animatingImageView.getScaleX();
                animationValues[0][1] = animatingImageView.getScaleY();
                animationValues[0][2] = animatingImageView.getTranslationX();
                animationValues[0][3] = animatingImageView.getTranslationY();
                animationValues[0][4] = 0;
                animationValues[0][5] = 0;
                animationValues[0][6] = 0;
                animationValues[0][7] = 0;

                animationValues[1][0] = object.scale;
                animationValues[1][1] = object.scale;
                animationValues[1][2] = object.viewX + drawRegion.left * object.scale;
                animationValues[1][3] = object.viewY + drawRegion.top * object.scale;
                animationValues[1][4] = clipHorizontal * object.scale;
                animationValues[1][5] = clipTop * object.scale;
                animationValues[1][6] = clipBottom * object.scale;
                animationValues[1][7] = object.radius;

                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(animatingImageView, "animationProgress", 0.0f, 1.0f),
                        ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0),
                        ObjectAnimator.ofFloat(containerView, "alpha", 0.0f));
            } else {
                animatorSet.playTogether(
                        ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0),
                        ObjectAnimator.ofFloat(animatingImageView, "alpha", 0.0f),
                        ObjectAnimator.ofFloat(animatingImageView, "translationY", translationY >= 0
                                ? AndroidUtilities.displaySize.y : -AndroidUtilities.displaySize.y),
                        ObjectAnimator.ofFloat(containerView, "alpha", 0.0f));
            }

            animationEndRunnable = new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 18) {
                        containerView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    animationInProgress = 0;
                    onPhotoClosed(object);
                }
            };

            animatorSet.setDuration(200);
            animatorSet.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (animationEndRunnable != null) {
                                animationEndRunnable.run();
                                animationEndRunnable = null;
                            }
                        }
                    });
                }
            });
            transitionAnimationStartTime = System.currentTimeMillis();
            if (Build.VERSION.SDK_INT >= 18) {
                containerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            animatorSet.start();
        } else {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(containerView, "scaleX", 0.9f),
                    ObjectAnimator.ofFloat(containerView, "scaleY", 0.9f),
                    ObjectAnimator.ofInt(backgroundDrawable, "alpha", 0),
                    ObjectAnimator.ofFloat(containerView, "alpha", 0.0f));
            animationInProgress = 2;
            animationEndRunnable = new Runnable() {
                @Override
                public void run() {
                    if (containerView == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 18) {
                        containerView.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                    animationInProgress = 0;
                    onPhotoClosed(object);
                    containerView.setScaleX(1.0f);
                    containerView.setScaleY(1.0f);
                }
            };
            animatorSet.setDuration(200);
            animatorSet.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animationEndRunnable != null) {
                        animationEndRunnable.run();
                        animationEndRunnable = null;
                    }
                }
            });
            transitionAnimationStartTime = System.currentTimeMillis();
            if (Build.VERSION.SDK_INT >= 18) {
                containerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            animatorSet.start();
        }
        if (currentAnimation != null) {
            currentAnimation.setSecondParentView(null);
            currentAnimation = null;
            centerImage.setImageBitmap((Drawable) null);
        }
    }

    public void destroyPhotoViewer() {
        if (parentActivity == null || windowView == null) {
            return;
        }
        try {
            if (windowView.getParent() != null) {
                WindowManager wm = (WindowManager) parentActivity
                        .getSystemService(Context.WINDOW_SERVICE);
                wm.removeViewImmediate(windowView);
            }
            windowView = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        parentActivity = null;
        Instance = null;
    }

    private void onPhotoClosed(PlaceProviderObject object) {
        isVisible = false;
        disableShowCheck = true;
        currentFileLocation = null;
        currentPathObject = null;
        currentThumb = null;
        if (currentAnimation != null) {
            currentAnimation.setSecondParentView(null);
            currentAnimation = null;
        }
        for (int a = 0; a < 3; a++) {
            if (radialProgressViews[a] != null) {
                radialProgressViews[a].setBackgroundState(-1, false);
            }
        }
        centerImage.setImageBitmap((Bitmap) null);
        leftImage.setImageBitmap((Bitmap) null);
        rightImage.setImageBitmap((Bitmap) null);
        containerView.post(new Runnable() {
            @Override
            public void run() {
                animatingImageView.setImageBitmap(null);
                try {
                    if (windowView.getParent() != null) {
                        WindowManager wm = (WindowManager) parentActivity
                                .getSystemService(Context.WINDOW_SERVICE);
                        wm.removeView(windowView);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        if (placeProvider != null) {
            placeProvider.willHidePhotoViewer();
        }
        placeProvider = null;
        disableShowCheck = false;
        if (object != null) {
            object.imageReceiver.setVisible(true, true);
        }
    }

    private void redraw(final int count) {
        if (count < 6) {
            if (containerView != null) {
                containerView.invalidate();
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        redraw(count + 1);
                    }
                }, 100);
            }
        }
    }

    public void onResume() {
        redraw(0); // workaround for camera bug
    }

    public void onPause() {
        if (currentAnimation != null) {
            closePhoto(false, false);
            return;
        }
    }

    public boolean isVisible() {
        return isVisible && placeProvider != null;
    }

    private void updateMinMax(float scale) {
        int maxW = (int) (centerImage.getImageWidth() * scale - getContainerViewWidth()) / 2;
        int maxH = (int) (centerImage.getImageHeight() * scale - getContainerViewHeight()) / 2;
        if (maxW > 0) {
            minX = -maxW;
            maxX = maxW;
        } else {
            minX = maxX = 0;
        }
        if (maxH > 0) {
            minY = -maxH;
            maxY = maxH;
        } else {
            minY = maxY = 0;
        }
        if (currentEditMode == 1) {
            maxX += photoCropView.getLimitX();
            maxY += photoCropView.getLimitY();
            minX -= photoCropView.getLimitWidth();
            minY -= photoCropView.getLimitHeight();
        }
    }

    private int getAdditionX() {
        if (currentEditMode != 0) {
            return AndroidUtilities.dp(14);
        }
        return 0;
    }

    private int getAdditionY() {
        if (currentEditMode != 0) {
            return AndroidUtilities.dp(14);
        }
        return 0;
    }

    private int getContainerViewWidth() {
        return getContainerViewWidth(currentEditMode);
    }

    private int getContainerViewWidth(int mode) {
        int width = containerView.getWidth();
        if (mode != 0) {
            width -= AndroidUtilities.dp(28);
        }
        return width;
    }

    private int getContainerViewHeight() {
        return getContainerViewHeight(currentEditMode);
    }

    private int getContainerViewHeight(int mode) {
        // int height = containerView.getHeight();
        int height = AndroidUtilities.displaySize.y - AndroidUtilities.statusBarHeight;
        if (mode == 1) {
            height -= AndroidUtilities.dp(76);
        } else if (mode == 2) {
            height -= AndroidUtilities.dp(154);
        }
        return height;
    }

    private boolean onTouchEvent(MotionEvent ev) {
        if (animationInProgress != 0 || animationStartTime != 0) {
            return false;
        }

        if (currentEditMode == 1) {
            if (ev.getPointerCount() == 1) {
                if (photoCropView.onTouch(ev)) {
                    updateMinMax(scale);
                    return true;
                }
            } else {
                photoCropView.onTouch(null);
            }
        }

        if (currentEditMode == 0 && ev.getPointerCount() == 1 && gestureDetector.onTouchEvent(ev)) {
            if (doubleTap) {
                doubleTap = false;
                moving = false;
                zooming = false;
                checkMinMax(false);
                return true;
            }
        }

        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN
                || ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            if (currentEditMode == 1) {
                photoCropView.cancelAnimationRunnable();
            }
            discardTap = false;
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
            if (!draggingDown && !changingPage) {
                if (canZoom && ev.getPointerCount() == 2) {
                    pinchStartDistance = (float) Math.hypot(ev.getX(1) - ev.getX(0),
                            ev.getY(1) - ev.getY(0));
                    pinchStartScale = scale;
                    pinchCenterX = (ev.getX(0) + ev.getX(1)) / 2.0f;
                    pinchCenterY = (ev.getY(0) + ev.getY(1)) / 2.0f;
                    pinchStartX = translationX;
                    pinchStartY = translationY;
                    zooming = true;
                    moving = false;
                    if (velocityTracker != null) {
                        velocityTracker.clear();
                    }
                } else if (ev.getPointerCount() == 1) {
                    moveStartX = ev.getX();
                    dragY = moveStartY = ev.getY();
                    draggingDown = false;
                    canDragDown = true;
                    if (velocityTracker != null) {
                        velocityTracker.clear();
                    }
                }
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (currentEditMode == 1) {
                photoCropView.cancelAnimationRunnable();
            }
            if (canZoom && ev.getPointerCount() == 2 && !draggingDown && zooming && !changingPage) {
                discardTap = true;
                scale = (float) Math.hypot(ev.getX(1) - ev.getX(0), ev.getY(1) - ev.getY(0))
                        / pinchStartDistance * pinchStartScale;
                translationX = (pinchCenterX - getContainerViewWidth() / 2)
                        - ((pinchCenterX - getContainerViewWidth() / 2) - pinchStartX)
                                * (scale / pinchStartScale);
                translationY = (pinchCenterY - getContainerViewHeight() / 2)
                        - ((pinchCenterY - getContainerViewHeight() / 2) - pinchStartY)
                                * (scale / pinchStartScale);
                updateMinMax(scale);
                containerView.invalidate();
            } else if (ev.getPointerCount() == 1) {
                if (velocityTracker != null) {
                    velocityTracker.addMovement(ev);
                }
                float dx = Math.abs(ev.getX() - moveStartX);
                float dy = Math.abs(ev.getY() - dragY);
                if (dx > AndroidUtilities.dp(3) || dy > AndroidUtilities.dp(3)) {
                    discardTap = true;
                }
                if (!(placeProvider instanceof EmptyPhotoViewerProvider) && currentEditMode == 0
                        && canDragDown && !draggingDown && scale == 1
                        && dy >= AndroidUtilities.dp(30) && dy / 2 > dx) {
                    draggingDown = true;
                    moving = false;
                    dragY = ev.getY();
                    if (isActionBarVisible && canShowBottom) {
                        toggleActionBar(false, true);
                    } else if (pickerView.getVisibility() == View.VISIBLE) {
                        toggleActionBar(false, true);
                        toggleCheckImageView(false);
                    }
                    return true;
                } else if (draggingDown) {
                    translationY = ev.getY() - dragY;
                    containerView.invalidate();
                } else if (!invalidCoords && animationStartTime == 0) {
                    float moveDx = moveStartX - ev.getX();
                    float moveDy = moveStartY - ev.getY();
                    if (moving || currentEditMode != 0
                            || scale == 1
                                    && Math.abs(moveDy) + AndroidUtilities.dp(12) < Math.abs(moveDx)
                            || scale != 1) {
                        if (!moving) {
                            moveDx = 0;
                            moveDy = 0;
                            moving = true;
                            canDragDown = false;
                        }

                        moveStartX = ev.getX();
                        moveStartY = ev.getY();
                        updateMinMax(scale);
                        if (translationX < minX && (currentEditMode != 0 || !rightImage.hasImage())
                                || translationX > maxX
                                        && (currentEditMode != 0 || !leftImage.hasImage())) {
                            moveDx /= 3.0f;
                        }
                        if (maxY == 0 && minY == 0 && currentEditMode == 0) {
                            if (translationY - moveDy < minY) {
                                translationY = minY;
                                moveDy = 0;
                            } else if (translationY - moveDy > maxY) {
                                translationY = maxY;
                                moveDy = 0;
                            }
                        } else {
                            if (translationY < minY || translationY > maxY) {
                                moveDy /= 3.0f;
                            }
                        }

                        translationX -= moveDx;
                        if (scale != 1 || currentEditMode != 0) {
                            translationY -= moveDy;
                        }

                        containerView.invalidate();
                    }
                } else {
                    invalidCoords = false;
                    moveStartX = ev.getX();
                    moveStartY = ev.getY();
                }
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL
                || ev.getActionMasked() == MotionEvent.ACTION_UP
                || ev.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            if (currentEditMode == 1) {
                photoCropView.startAnimationRunnable();
            }
            if (zooming) {
                invalidCoords = true;
                if (scale < 1.0f) {
                    updateMinMax(1.0f);
                    animateTo(1.0f, 0, 0, true);
                } else if (scale > 3.0f) {
                    float atx = (pinchCenterX - getContainerViewWidth() / 2)
                            - ((pinchCenterX - getContainerViewWidth() / 2) - pinchStartX)
                                    * (3.0f / pinchStartScale);
                    float aty = (pinchCenterY - getContainerViewHeight() / 2)
                            - ((pinchCenterY - getContainerViewHeight() / 2) - pinchStartY)
                                    * (3.0f / pinchStartScale);
                    updateMinMax(3.0f);
                    if (atx < minX) {
                        atx = minX;
                    } else if (atx > maxX) {
                        atx = maxX;
                    }
                    if (aty < minY) {
                        aty = minY;
                    } else if (aty > maxY) {
                        aty = maxY;
                    }
                    animateTo(3.0f, atx, aty, true);
                } else {
                    checkMinMax(true);
                }
                zooming = false;
            } else if (draggingDown) {
                if (Math.abs(dragY - ev.getY()) > getContainerViewHeight() / 6.0f) {
                    closePhoto(true, false);
                } else {
                    if (pickerView.getVisibility() == View.VISIBLE) {
                        toggleActionBar(true, true);
                        toggleCheckImageView(true);
                    }
                    animateTo(1, 0, 0, false);
                }
                draggingDown = false;
            } else if (moving) {
                float moveToX = translationX;
                float moveToY = translationY;
                updateMinMax(scale);
                moving = false;
                canDragDown = true;
                float velocity = 0;
                if (velocityTracker != null && scale == 1) {
                    velocityTracker.computeCurrentVelocity(1000);
                    velocity = velocityTracker.getXVelocity();
                }

                if (currentEditMode == 0) {
                    if ((translationX < minX - getContainerViewWidth() / 3
                            || velocity < -AndroidUtilities.dp(650)) && rightImage.hasImage()) {
                        goToNext();
                        return true;
                    }
                    if ((translationX > maxX + getContainerViewWidth() / 3
                            || velocity > AndroidUtilities.dp(650)) && leftImage.hasImage()) {
                        goToPrev();
                        return true;
                    }
                }

                if (translationX < minX) {
                    moveToX = minX;
                } else if (translationX > maxX) {
                    moveToX = maxX;
                }
                if (translationY < minY) {
                    moveToY = minY;
                } else if (translationY > maxY) {
                    moveToY = maxY;
                }
                animateTo(scale, moveToX, moveToY, false);
            }
        }
        return false;
    }

    private void checkMinMax(boolean zoom) {
        float moveToX = translationX;
        float moveToY = translationY;
        updateMinMax(scale);
        if (translationX < minX) {
            moveToX = minX;
        } else if (translationX > maxX) {
            moveToX = maxX;
        }
        if (translationY < minY) {
            moveToY = minY;
        } else if (translationY > maxY) {
            moveToY = maxY;
        }
        animateTo(scale, moveToX, moveToY, zoom);
    }

    private void goToNext() {
        float extra = 0;
        if (scale != 1) {
            extra = (getContainerViewWidth() - centerImage.getImageWidth()) / 2 * scale;
        }
        switchImageAfterAnimation = 1;
        animateTo(scale, minX - getContainerViewWidth() - extra - PAGE_SPACING / 2, translationY,
                false);
    }

    private void goToPrev() {
        float extra = 0;
        if (scale != 1) {
            extra = (getContainerViewWidth() - centerImage.getImageWidth()) / 2 * scale;
        }
        switchImageAfterAnimation = 2;
        animateTo(scale, maxX + getContainerViewWidth() + extra + PAGE_SPACING / 2, translationY,
                false);
    }

    private void animateTo(float newScale, float newTx, float newTy, boolean isZoom) {
        animateTo(newScale, newTx, newTy, isZoom, 250);
    }

    private void animateTo(float newScale, float newTx, float newTy, boolean isZoom, int duration) {
        if (scale == newScale && translationX == newTx && translationY == newTy) {
            return;
        }
        zoomAnimation = isZoom;
        animateToScale = newScale;
        animateToX = newTx;
        animateToY = newTy;
        animationStartTime = System.currentTimeMillis();
        imageMoveAnimation = new AnimatorSet();
        imageMoveAnimation.playTogether(
                ObjectAnimator.ofFloat(this, "animationValue", 0, 1));
        imageMoveAnimation.setInterpolator(interpolator);
        imageMoveAnimation.setDuration(duration);
        imageMoveAnimation.addListener(new AnimatorListenerAdapterProxy() {
            @Override
            public void onAnimationEnd(Animator animation) {
                imageMoveAnimation = null;
                containerView.invalidate();
            }
        });
        imageMoveAnimation.start();
    }

    public void setAnimationValue(float value) {
        animationValue = value;
        containerView.invalidate();
    }

    public float getAnimationValue() {
        return animationValue;
    }

    @SuppressLint("NewApi")
    private void onDraw(Canvas canvas) {
        if (animationInProgress == 1 || !isVisible && animationInProgress != 2) {
            return;
        }

        float currentTranslationY;
        float currentTranslationX;
        float currentScale;
        float aty = -1;

        if (imageMoveAnimation != null) {
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }

            float ts = scale + (animateToScale - scale) * animationValue;
            float tx = translationX + (animateToX - translationX) * animationValue;
            float ty = translationY + (animateToY - translationY) * animationValue;
            if (currentEditMode == 1) {
                photoCropView.setAnimationProgress(animationValue);
            }

            if (animateToScale == 1 && scale == 1 && translationX == 0) {
                aty = ty;
            }
            currentScale = ts;
            currentTranslationY = ty;
            currentTranslationX = tx;
            containerView.invalidate();
        } else {
            if (animationStartTime != 0) {
                translationX = animateToX;
                translationY = animateToY;
                scale = animateToScale;
                animationStartTime = 0;
                if (currentEditMode == 1) {
                    photoCropView.setAnimationProgress(1);
                }
                updateMinMax(scale);
                zoomAnimation = false;
            }
            if (!scroller.isFinished()) {
                if (scroller.computeScrollOffset()) {
                    if (scroller.getStartX() < maxX && scroller.getStartX() > minX) {
                        translationX = scroller.getCurrX();
                    }
                    if (scroller.getStartY() < maxY && scroller.getStartY() > minY) {
                        translationY = scroller.getCurrY();
                    }
                    containerView.invalidate();
                }
            }
            if (switchImageAfterAnimation != 0) {
                if (switchImageAfterAnimation == 1) {
                    setImageIndex(currentIndex + 1, false);
                } else if (switchImageAfterAnimation == 2) {
                    setImageIndex(currentIndex - 1, false);
                }
                switchImageAfterAnimation = 0;
            }
            currentScale = scale;
            currentTranslationY = translationY;
            currentTranslationX = translationX;
            if (!moving) {
                aty = translationY;
            }
        }

        if (currentEditMode == 0 && scale == 1 && aty != -1 && !zoomAnimation) {
            float maxValue = getContainerViewHeight() / 4.0f;
            backgroundDrawable.setAlpha((int) Math.max(127,
                    255 * (1.0f - (Math.min(Math.abs(aty), maxValue) / maxValue))));
        } else {
            backgroundDrawable.setAlpha(255);
        }

        ImageReceiver sideImage = null;
        if (currentEditMode == 0) {
            if (scale >= 1.0f && !zoomAnimation && !zooming) {
                if (currentTranslationX > maxX + AndroidUtilities.dp(5)) {
                    sideImage = leftImage;
                } else if (currentTranslationX < minX - AndroidUtilities.dp(5)) {
                    sideImage = rightImage;
                }
            }
            changingPage = sideImage != null;
        }

        if (sideImage == rightImage) {
            float tranlateX = currentTranslationX;
            float scaleDiff = 0;
            float alpha = 1;
            if (!zoomAnimation && tranlateX < minX) {
                alpha = Math.min(1.0f, (minX - tranlateX) / canvas.getWidth());
                scaleDiff = (1.0f - alpha) * 0.3f;
                tranlateX = -canvas.getWidth() - PAGE_SPACING / 2;
            }

            if (sideImage.hasBitmapImage()) {
                canvas.save();
                canvas.translate(getContainerViewWidth() / 2, getContainerViewHeight() / 2);
                canvas.translate(canvas.getWidth() + PAGE_SPACING / 2 + tranlateX, 0);
                canvas.scale(1.0f - scaleDiff, 1.0f - scaleDiff);
                int bitmapWidth = sideImage.getBitmapWidth();
                int bitmapHeight = sideImage.getBitmapHeight();

                float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
                float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
                float scale = scaleX > scaleY ? scaleY : scaleX;
                int width = (int) (bitmapWidth * scale);
                int height = (int) (bitmapHeight * scale);

                sideImage.setAlpha(alpha);
                sideImage.setImageCoords(-width / 2, -height / 2, width, height);
                sideImage.draw(canvas);
                canvas.restore();
            }

            canvas.save();
            canvas.translate(tranlateX, currentTranslationY / currentScale);
            canvas.translate((canvas.getWidth() * (scale + 1) + PAGE_SPACING) / 2,
                    -currentTranslationY / currentScale);
            radialProgressViews[1].setScale(1.0f - scaleDiff);
            radialProgressViews[1].setAlpha(alpha);
            radialProgressViews[1].onDraw(canvas);
            canvas.restore();
        }

        float translateX = currentTranslationX;
        float scaleDiff = 0;
        float alpha = 1;
        if (!zoomAnimation && translateX > maxX && currentEditMode == 0) {
            alpha = Math.min(1.0f, (translateX - maxX) / canvas.getWidth());
            scaleDiff = alpha * 0.3f;
            alpha = 1.0f - alpha;
            translateX = maxX;
        }
        boolean drawTextureView = Build.VERSION.SDK_INT >= 16 && aspectRatioFrameLayout != null
                && aspectRatioFrameLayout.getVisibility() == View.VISIBLE;
        if (centerImage.hasBitmapImage()) {
            canvas.save();
            canvas.translate(getContainerViewWidth() / 2 + getAdditionX(),
                    getContainerViewHeight() / 2 + getAdditionY());
            canvas.translate(translateX, currentTranslationY);
            canvas.scale(currentScale - scaleDiff, currentScale - scaleDiff);

            if (currentEditMode == 1) {
                photoCropView.setBitmapParams(currentScale, translateX, currentTranslationY);
            }

            int bitmapWidth = centerImage.getBitmapWidth();
            int bitmapHeight = centerImage.getBitmapHeight();
            if (drawTextureView && textureUploaded) {
                float scale1 = bitmapWidth / (float) bitmapHeight;
                float scale2 = videoTextureView.getMeasuredWidth()
                        / (float) videoTextureView.getMeasuredHeight();
                if (Math.abs(scale1 - scale2) > 0.01f) {
                    bitmapWidth = videoTextureView.getMeasuredWidth();
                    bitmapHeight = videoTextureView.getMeasuredHeight();
                }
            }

            float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
            float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
            float scale = scaleX > scaleY ? scaleY : scaleX;
            int width = (int) (bitmapWidth * scale);
            int height = (int) (bitmapHeight * scale);

            if (!drawTextureView || !textureUploaded || !videoCrossfadeStarted
                    || videoCrossfadeAlpha != 1.0f) {
                centerImage.setAlpha(alpha);
                centerImage.setImageCoords(-width / 2, -height / 2, width, height);
                centerImage.draw(canvas);
            }
            if (drawTextureView) {
                if (!videoCrossfadeStarted && textureUploaded) {
                    videoCrossfadeStarted = true;
                    videoCrossfadeAlpha = 0.0f;
                    videoCrossfadeAlphaLastTime = System.currentTimeMillis();
                }
                canvas.translate(-width / 2, -height / 2);
                videoTextureView.setAlpha(alpha * videoCrossfadeAlpha);
                aspectRatioFrameLayout.draw(canvas);
                if (videoCrossfadeStarted && videoCrossfadeAlpha < 1.0f) {
                    long newUpdateTime = System.currentTimeMillis();
                    long dt = newUpdateTime - videoCrossfadeAlphaLastTime;
                    videoCrossfadeAlphaLastTime = newUpdateTime;
                    videoCrossfadeAlpha += dt / 300.0f;
                    containerView.invalidate();
                    if (videoCrossfadeAlpha > 1.0f) {
                        videoCrossfadeAlpha = 1.0f;
                    }
                }
            }
            canvas.restore();
        }
        if (!drawTextureView) {
            canvas.save();
            canvas.translate(translateX, currentTranslationY / currentScale);
            radialProgressViews[0].setScale(1.0f - scaleDiff);
            radialProgressViews[0].setAlpha(alpha);
            radialProgressViews[0].onDraw(canvas);
            canvas.restore();
        }

        if (sideImage == leftImage) {
            if (sideImage.hasBitmapImage()) {
                canvas.save();
                canvas.translate(getContainerViewWidth() / 2, getContainerViewHeight() / 2);
                canvas.translate(
                        -(canvas.getWidth() * (scale + 1) + PAGE_SPACING) / 2 + currentTranslationX,
                        0);
                int bitmapWidth = sideImage.getBitmapWidth();
                int bitmapHeight = sideImage.getBitmapHeight();

                float scaleX = (float) getContainerViewWidth() / (float) bitmapWidth;
                float scaleY = (float) getContainerViewHeight() / (float) bitmapHeight;
                float scale = scaleX > scaleY ? scaleY : scaleX;
                int width = (int) (bitmapWidth * scale);
                int height = (int) (bitmapHeight * scale);

                sideImage.setAlpha(1.0f);
                sideImage.setImageCoords(-width / 2, -height / 2, width, height);
                sideImage.draw(canvas);
                canvas.restore();
            }

            canvas.save();
            canvas.translate(currentTranslationX, currentTranslationY / currentScale);
            canvas.translate(-(canvas.getWidth() * (scale + 1) + PAGE_SPACING) / 2,
                    -currentTranslationY / currentScale);
            radialProgressViews[2].setScale(1.0f);
            radialProgressViews[2].setAlpha(1.0f);
            radialProgressViews[2].onDraw(canvas);
            canvas.restore();
        }
    }

    @SuppressLint("DrawAllocation")
    private void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            scale = 1;
            translationX = 0;
            translationY = 0;
            updateMinMax(scale);

            if (checkImageView != null) {
                checkImageView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (checkImageView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) checkImageView
                                    .getLayoutParams();
                            WindowManager manager = (WindowManager) Gallery.applicationContext
                                    .getSystemService(Activity.WINDOW_SERVICE);
                            int rotation = manager.getDefaultDisplay().getRotation();
                            layoutParams.topMargin = AndroidUtilities.dp(
                                    rotation == Surface.ROTATION_270
                                            || rotation == Surface.ROTATION_90
                                                    ? 58 : 68);
                            checkImageView.setLayoutParams(layoutParams);
                        }
                    }
                });
            }
        }
    }

    private void onActionClick(boolean download) {
        if (currentFileNames[0] == null) {
            return;
        }
        File file = null;
        if (Build.VERSION.SDK_INT >= 16) {
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "video/mp4");
            parentActivity.startActivityForResult(intent, 500);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (scale != 1) {
            scroller.abortAnimation();
            scroller.fling(Math.round(translationX), Math.round(translationY),
                    Math.round(velocityX), Math.round(velocityY), (int) minX, (int) maxX,
                    (int) minY, (int) maxY);
            containerView.postInvalidate();
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (discardTap) {
            return false;
        }
        if (canShowBottom) {
            boolean drawTextureView = Build.VERSION.SDK_INT >= 16 && aspectRatioFrameLayout != null
                    && aspectRatioFrameLayout.getVisibility() == View.VISIBLE;
            if (radialProgressViews[0] != null && containerView != null && !drawTextureView) {
                int state = radialProgressViews[0].backgroundState;
                if (state > 0 && state <= 3) {
                    float x = e.getX();
                    float y = e.getY();
                    if (x >= (getContainerViewWidth() - AndroidUtilities.dp(100)) / 2.0f
                            && x <= (getContainerViewWidth() + AndroidUtilities.dp(100)) / 2.0f &&
                            y >= (getContainerViewHeight() - AndroidUtilities.dp(100)) / 2.0f
                            && y <= (getContainerViewHeight() + AndroidUtilities.dp(100)) / 2.0f) {
                        onActionClick(true);
                        checkProgress(0, true);
                        return true;
                    }
                }
            }
            toggleActionBar(!isActionBarVisible, true);
        } else if (sendPhotoType == 0) {
            checkImageView.performClick();
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (!canZoom || scale == 1.0f && (translationY != 0 || translationX != 0)) {
            return false;
        }
        if (animationStartTime != 0 || animationInProgress != 0) {
            return false;
        }
        if (scale == 1.0f) {
            float atx = (e.getX() - getContainerViewWidth() / 2)
                    - ((e.getX() - getContainerViewWidth() / 2) - translationX) * (3.0f / scale);
            float aty = (e.getY() - getContainerViewHeight() / 2)
                    - ((e.getY() - getContainerViewHeight() / 2) - translationY) * (3.0f / scale);
            updateMinMax(3.0f);
            if (atx < minX) {
                atx = minX;
            } else if (atx > maxX) {
                atx = maxX;
            }
            if (aty < minY) {
                aty = minY;
            } else if (aty > maxY) {
                aty = maxY;
            }
            animateTo(3.0f, atx, aty, true);
        } else {
            animateTo(1.0f, 0, 0, true);
        }
        doubleTap = true;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }
}
