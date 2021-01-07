package com.tangxiaolv.telegramgallery;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.tangxiaolv.telegramgallery.tl.Document;
import com.tangxiaolv.telegramgallery.tl.FileLocation;
import com.tangxiaolv.telegramgallery.tl.TLObject;
import com.tangxiaolv.telegramgallery.utils.FileLog;
import com.tangxiaolv.telegramgallery.utils.GalleryImageLoader;
import com.tangxiaolv.telegramgallery.utils.NotificationCenter;
import com.tangxiaolv.telegramgallery.utils.Utilities;

public class ImageReceiver implements NotificationCenter.NotificationCenterDelegate {

    public interface ImageReceiverDelegate {
        void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb);
    }

    private class SetImageBackup {
        public TLObject fileLocation;
        public String httpUrl;
        public String filter;
        public Drawable thumb;
        public FileLocation thumbLocation;
        public String thumbFilter;
        public int size;
        public int cacheType;
        public String ext;
    }

    private View parentView;
    private Integer tag;
    private Integer thumbTag;
    private boolean canceledLoading;
    private static PorterDuffColorFilter selectedColorFilter = new PorterDuffColorFilter(0xffdddddd, PorterDuff.Mode.MULTIPLY);

    private SetImageBackup setImageBackup;

    private TLObject currentImageLocation;
    private String currentKey;
    private String currentThumbKey;
    private String currentHttpUrl;
    private String currentFilter;
    private String currentThumbFilter;
    private String currentExt;
    private FileLocation currentThumbLocation;
    private int currentSize;
    private int currentCacheType;
    private Drawable currentImage;
    private Drawable currentThumb;
    private Drawable staticThumb;
    private boolean allowStartAnimation = true;
    private boolean allowDecodeSingleFrame;

    private boolean needsQualityThumb;
    private boolean shouldGenerateQualityThumb;
    private boolean invalidateAll;

    private int imageX, imageY, imageW, imageH;
    private Rect drawRegion = new Rect();
    private boolean isVisible = true;
    private boolean isAspectFit;
    private boolean forcePreview;
    private boolean forceCrossfade;
    private int roundRadius;
    private BitmapShader bitmapShader;
    private BitmapShader bitmapShaderThumb;
    private static Paint roundPaint;
    private RectF roundRect = new RectF();
    private RectF bitmapRect = new RectF();
    private Matrix shaderMatrix = new Matrix();
    private float overrideAlpha = 1.0f;
    private boolean isPressed;
    private int orientation;
    private boolean centerRotation;
    private ImageReceiverDelegate delegate;
    private float currentAlpha;
    private long lastUpdateAlphaTime;
    private byte crossfadeAlpha = 1;
    private boolean manualAlphaAnimator;
    private boolean crossfadeWithThumb;
    private ColorFilter colorFilter;

    public ImageReceiver() {
        this(null);
    }

    public ImageReceiver(View view) {
        parentView = view;
        if (roundPaint == null) {
            roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
    }

    public void cancelLoadImage() {
        GalleryImageLoader.getInstance().cancelLoadingForImageReceiver(this, 0);
        canceledLoading = true;
    }

    public void setImage(TLObject path, String filter, Drawable thumb, String ext, int cacheType) {
        setImage(path, null, filter, thumb, null, null, 0, ext, cacheType);
    }

    public void setImage(TLObject path, String filter, Drawable thumb, int size, String ext, int cacheType) {
        setImage(path, null, filter, thumb, null, null, size, ext, cacheType);
    }

    public void setImage(String httpUrl, String filter, Drawable thumb, String ext, int size) {
        setImage(null, httpUrl, filter, thumb, null, null, size, ext, 1);
    }

    public void setImage(TLObject fileLocation, String filter, FileLocation thumbLocation, String thumbFilter, String ext, int cacheType) {
        setImage(fileLocation, null, filter, null, thumbLocation, thumbFilter, 0, ext, cacheType);
    }

    public void setImage(TLObject fileLocation, String filter, FileLocation thumbLocation, String thumbFilter, int size, String ext, int cacheType) {
        setImage(fileLocation, null, filter, null, thumbLocation, thumbFilter, size, ext, cacheType);
    }

    public void setImage(TLObject fileLocation, String httpUrl, String filter, Drawable thumb, FileLocation thumbLocation, String thumbFilter, int size, String ext, int cacheType) {
        if (setImageBackup != null) {
            setImageBackup.fileLocation = null;
            setImageBackup.httpUrl = null;
            setImageBackup.thumbLocation = null;
            setImageBackup.thumb = null;
        }

        if ((fileLocation == null && httpUrl == null && thumbLocation == null)
                || (fileLocation != null && !(fileLocation instanceof FileLocation.TL_fileLocation)
                && !(fileLocation instanceof FileLocation.TL_fileEncryptedLocation)
                && !(fileLocation instanceof Document.TL_document))) {
            recycleBitmap(null, false);
            recycleBitmap(null, true);
            currentKey = null;
            currentExt = ext;
            currentThumbKey = null;
            currentThumbFilter = null;
            currentImageLocation = null;
            currentHttpUrl = null;
            currentFilter = null;
            currentCacheType = 0;
            staticThumb = thumb;
            currentAlpha = 1;
            currentThumbLocation = null;
            currentSize = 0;
            currentImage = null;
            bitmapShader = null;
            bitmapShaderThumb = null;
            GalleryImageLoader.getInstance().cancelLoadingForImageReceiver(this, 0);
            if (parentView != null) {
                if (invalidateAll) {
                    parentView.invalidate();
                } else {
                    parentView.invalidate(imageX, imageY, imageX + imageW, imageY + imageH);
                }
            }
            if (delegate != null) {
                delegate.didSetImage(this, currentImage != null || currentThumb != null || staticThumb != null, currentImage == null);
            }
            return;
        }

        if (!(thumbLocation instanceof FileLocation.TL_fileLocation)) {
            thumbLocation = null;
        }

        String key = null;
        if (fileLocation != null) {
            if (fileLocation instanceof FileLocation) {
                FileLocation location = (FileLocation) fileLocation;
                key = location.volume_id + "_" + location.local_id;
            } else {
                Document location = (Document) fileLocation;
                if (location.dc_id != 0) {
                    if (location.version == 0) {
                        key = location.dc_id + "_" + location.id;
                    } else {
                        key = location.dc_id + "_" + location.id + "_" + location.version;
                    }
                } else {
                    fileLocation = null;
                }
            }
        } else if (httpUrl != null) {
            key = Utilities.MD5(httpUrl);
        }
        if (key != null) {
            if (filter != null) {
                key += "@" + filter;
            }
        }

        if (currentKey != null && key != null && currentKey.equals(key)) {
            if (delegate != null) {
                delegate.didSetImage(this, currentImage != null || currentThumb != null || staticThumb != null, currentImage == null);
            }
            if (!canceledLoading && !forcePreview) {
                return;
            }
        }

        String thumbKey = null;
        if (thumbLocation != null) {
            thumbKey = thumbLocation.volume_id + "_" + thumbLocation.local_id;
            if (thumbFilter != null) {
                thumbKey += "@" + thumbFilter;
            }
        }

        recycleBitmap(key, false);
        recycleBitmap(thumbKey, true);

        currentThumbKey = thumbKey;
        currentKey = key;
        currentExt = ext;
        currentImageLocation = fileLocation;
        currentHttpUrl = httpUrl;
        currentFilter = filter;
        currentThumbFilter = thumbFilter;
        currentSize = size;
        currentCacheType = cacheType;
        currentThumbLocation = thumbLocation;
        staticThumb = thumb;
        bitmapShader = null;
        bitmapShaderThumb = null;
        currentAlpha = 1.0f;

        if (delegate != null) {
            delegate.didSetImage(this, currentImage != null || currentThumb != null || staticThumb != null, currentImage == null);
        }

        GalleryImageLoader.getInstance().loadImageForImageReceiver(this);
        if (parentView != null) {
            if (invalidateAll) {
                parentView.invalidate();
            } else {
                parentView.invalidate(imageX, imageY, imageX + imageW, imageY + imageH);
            }
        }
    }

    public void setColorFilter(ColorFilter filter) {
        colorFilter = filter;
    }

    public void setDelegate(ImageReceiverDelegate delegate) {
        this.delegate = delegate;
    }

    public void setPressed(boolean value) {
        isPressed = value;
    }

    public boolean getPressed() {
        return isPressed;
    }

    public void setOrientation(int angle, boolean center) {
        while (angle < 0) {
            angle += 360;
        }
        while (angle > 360) {
            angle -= 360;
        }
        orientation = angle;
        centerRotation = center;
    }

    public void setInvalidateAll(boolean value) {
        invalidateAll = value;
    }

    public Drawable getStaticThumb() {
        return staticThumb;
    }

    public int getAnimatedOrientation() {
        if (currentImage instanceof AnimatedFileDrawable) {
            return ((AnimatedFileDrawable) currentImage).getOrientation();
        } else if (staticThumb instanceof AnimatedFileDrawable) {
            return ((AnimatedFileDrawable) staticThumb).getOrientation();
        }
        return 0;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setImageBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap != null ? new BitmapDrawable(null, bitmap) : null);
    }

    public void setImageBitmap(Drawable bitmap) {
        GalleryImageLoader.getInstance().cancelLoadingForImageReceiver(this, 0);
        recycleBitmap(null, false);
        recycleBitmap(null, true);
        staticThumb = bitmap;
        currentThumbLocation = null;
        currentKey = null;
        currentExt = null;
        currentThumbKey = null;
        currentImage = null;
        currentThumbFilter = null;
        currentImageLocation = null;
        currentHttpUrl = null;
        currentFilter = null;
        currentSize = 0;
        currentCacheType = 0;
        bitmapShader = null;
        bitmapShaderThumb = null;
        if (setImageBackup != null) {
            setImageBackup.fileLocation = null;
            setImageBackup.httpUrl = null;
            setImageBackup.thumbLocation = null;
            setImageBackup.thumb = null;
        }
        currentAlpha = 1;
        if (delegate != null) {
            delegate.didSetImage(this, currentThumb != null || staticThumb != null, true);
        }
        if (parentView != null) {
            if (invalidateAll) {
                parentView.invalidate();
            } else {
                parentView.invalidate(imageX, imageY, imageX + imageW, imageY + imageH);
            }
        }
    }

    public void clearImage() {
        recycleBitmap(null, false);
        recycleBitmap(null, true);
        if (needsQualityThumb) {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageThumbGenerated);
            GalleryImageLoader.getInstance().cancelLoadingForImageReceiver(this, 0);
        }
    }

    public void onDetachedFromWindow() {
        if (currentImageLocation != null || currentHttpUrl != null || currentThumbLocation != null || staticThumb != null) {
            if (setImageBackup == null) {
                setImageBackup = new SetImageBackup();
            }
            setImageBackup.fileLocation = currentImageLocation;
            setImageBackup.httpUrl = currentHttpUrl;
            setImageBackup.filter = currentFilter;
            setImageBackup.thumb = staticThumb;
            setImageBackup.thumbLocation = currentThumbLocation;
            setImageBackup.thumbFilter = currentThumbFilter;
            setImageBackup.size = currentSize;
            setImageBackup.ext = currentExt;
            setImageBackup.cacheType = currentCacheType;
        }
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didReplacedPhotoInMemCache);
        clearImage();
    }

    public boolean onAttachedToWindow() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didReplacedPhotoInMemCache);
        if (needsQualityThumb) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageThumbGenerated);
        }
        if (setImageBackup != null && (setImageBackup.fileLocation != null || setImageBackup.httpUrl != null || setImageBackup.thumbLocation != null || setImageBackup.thumb != null)) {
            setImage(setImageBackup.fileLocation, setImageBackup.httpUrl, setImageBackup.filter, setImageBackup.thumb, setImageBackup.thumbLocation, setImageBackup.thumbFilter, setImageBackup.size, setImageBackup.ext, setImageBackup.cacheType);
            return true;
        }
        return false;
    }

    private void drawDrawable(Canvas canvas, Drawable drawable, int alpha, BitmapShader shader) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;

            Paint paint;
            if (shader != null) {
                paint = roundPaint;
            } else {
                paint = bitmapDrawable.getPaint();
            }
            boolean hasFilter = paint != null && paint.getColorFilter() != null;
            if (hasFilter && !isPressed) {
                if (shader != null) {
                    roundPaint.setColorFilter(null);
                } else if (staticThumb != drawable) {
                    bitmapDrawable.setColorFilter(null);
                }
            } else if (!hasFilter && isPressed) {
                if (shader != null) {
                    roundPaint.setColorFilter(selectedColorFilter);
                } else {
                    bitmapDrawable.setColorFilter(selectedColorFilter);
                }
            }
            if (colorFilter != null) {
                if (shader != null) {
                    roundPaint.setColorFilter(colorFilter);
                } else {
                    bitmapDrawable.setColorFilter(colorFilter);
                }
            }
            int bitmapW;
            int bitmapH;
            if (bitmapDrawable instanceof AnimatedFileDrawable) {
                if (orientation % 360 == 90 || orientation % 360 == 270) {
                    bitmapW = bitmapDrawable.getIntrinsicHeight();
                    bitmapH = bitmapDrawable.getIntrinsicWidth();
                } else {
                    bitmapW = bitmapDrawable.getIntrinsicWidth();
                    bitmapH = bitmapDrawable.getIntrinsicHeight();
                }
            } else {
                if (orientation % 360 == 90 || orientation % 360 == 270) {
                    bitmapW = bitmapDrawable.getBitmap().getHeight();
                    bitmapH = bitmapDrawable.getBitmap().getWidth();
                } else {
                    bitmapW = bitmapDrawable.getBitmap().getWidth();
                    bitmapH = bitmapDrawable.getBitmap().getHeight();
                }
            }
            float scaleW = bitmapW / (float) imageW;
            float scaleH = bitmapH / (float) imageH;

            if (shader != null) {
                roundPaint.setShader(shader);
                float scale = Math.min(scaleW, scaleH);
                roundRect.set(imageX, imageY, imageX + imageW, imageY + imageH);
                shaderMatrix.reset();
                if (Math.abs(scaleW - scaleH) > 0.00001f) {
                    if (bitmapW / scaleH > imageW) {
                        drawRegion.set(imageX - ((int) (bitmapW / scaleH) - imageW) / 2, imageY, imageX + ((int) (bitmapW / scaleH) + imageW) / 2, imageY + imageH);
                    } else {
                        drawRegion.set(imageX, imageY - ((int) (bitmapH / scaleW) - imageH) / 2, imageX + imageW, imageY + ((int) (bitmapH / scaleW) + imageH) / 2);
                    }
                } else {
                    drawRegion.set(imageX, imageY, imageX + imageW, imageY + imageH);
                }
                if (isVisible) {
                    if (Math.abs(scaleW - scaleH) > 0.00001f) {
                        int w = (int) Math.floor(imageW * scale);
                        int h = (int) Math.floor(imageH * scale);
                        bitmapRect.set((bitmapW - w) / 2, (bitmapH - h) / 2, (bitmapW + w) / 2, (bitmapH + h) / 2);
                        shaderMatrix.setRectToRect(bitmapRect, roundRect, Matrix.ScaleToFit.START);
                    } else {
                        bitmapRect.set(0, 0, bitmapW, bitmapH);
                        shaderMatrix.setRectToRect(bitmapRect, roundRect, Matrix.ScaleToFit.FILL);
                    }
                    shader.setLocalMatrix(shaderMatrix);
                    roundPaint.setAlpha(alpha);
                    canvas.drawRoundRect(roundRect, roundRadius, roundRadius, roundPaint);
                }
            } else {
                if (isAspectFit) {
                    float scale = Math.max(scaleW, scaleH);
                    canvas.save();
                    bitmapW /= scale;
                    bitmapH /= scale;
                    drawRegion.set(imageX + (imageW - bitmapW) / 2, imageY + (imageH - bitmapH) / 2, imageX + (imageW + bitmapW) / 2, imageY + (imageH + bitmapH) / 2);
                    bitmapDrawable.setBounds(drawRegion);
                    try {
                        bitmapDrawable.setAlpha(alpha);
                        bitmapDrawable.draw(canvas);
                    } catch (Exception e) {
                        if (bitmapDrawable == currentImage && currentKey != null) {
                            GalleryImageLoader.getInstance().removeImage(currentKey);
                            currentKey = null;
                        } else if (bitmapDrawable == currentThumb && currentThumbKey != null) {
                            GalleryImageLoader.getInstance().removeImage(currentThumbKey);
                            currentThumbKey = null;
                        }
                        setImage(currentImageLocation, currentHttpUrl, currentFilter, currentThumb, currentThumbLocation, currentThumbFilter, currentSize, currentExt, currentCacheType);
                        FileLog.e(e);
                    }
                    canvas.restore();
                } else {
                    if (Math.abs(scaleW - scaleH) > 0.00001f) {
                        canvas.save();
                        canvas.clipRect(imageX, imageY, imageX + imageW, imageY + imageH);

                        if (orientation % 360 != 0) {
                            if (centerRotation) {
                                canvas.rotate(orientation, imageW / 2, imageH / 2);
                            } else {
                                canvas.rotate(orientation, 0, 0);
                            }
                        }

                        if (bitmapW / scaleH > imageW) {
                            bitmapW /= scaleH;
                            drawRegion.set(imageX - (bitmapW - imageW) / 2, imageY, imageX + (bitmapW + imageW) / 2, imageY + imageH);
                        } else {
                            bitmapH /= scaleW;
                            drawRegion.set(imageX, imageY - (bitmapH - imageH) / 2, imageX + imageW, imageY + (bitmapH + imageH) / 2);
                        }
                        if (bitmapDrawable instanceof AnimatedFileDrawable) {
                            ((AnimatedFileDrawable) bitmapDrawable).setActualDrawRect(imageX, imageY, imageW, imageH);
                        }
                        if (orientation % 360 == 90 || orientation % 360 == 270) {
                            int width = (drawRegion.right - drawRegion.left) / 2;
                            int height = (drawRegion.bottom - drawRegion.top) / 2;
                            int centerX = (drawRegion.right + drawRegion.left) / 2;
                            int centerY = (drawRegion.top + drawRegion.bottom) / 2;
                            bitmapDrawable.setBounds(centerX - height, centerY - width, centerX + height, centerY + width);
                        } else {
                            bitmapDrawable.setBounds(drawRegion);
                        }
                        if (isVisible) {
                            try {
                                bitmapDrawable.setAlpha(alpha);
                                bitmapDrawable.draw(canvas);
                            } catch (Exception e) {
                                if (bitmapDrawable == currentImage && currentKey != null) {
                                    GalleryImageLoader.getInstance().removeImage(currentKey);
                                    currentKey = null;
                                } else if (bitmapDrawable == currentThumb && currentThumbKey != null) {
                                    GalleryImageLoader.getInstance().removeImage(currentThumbKey);
                                    currentThumbKey = null;
                                }
                                setImage(currentImageLocation, currentHttpUrl, currentFilter, currentThumb, currentThumbLocation, currentThumbFilter, currentSize, currentExt, currentCacheType);
                                FileLog.e(e);
                            }
                        }

                        canvas.restore();
                    } else {
                        canvas.save();
                        if (orientation % 360 != 0) {
                            if (centerRotation) {
                                canvas.rotate(orientation, imageW / 2, imageH / 2);
                            } else {
                                canvas.rotate(orientation, 0, 0);
                            }
                        }
                        drawRegion.set(imageX, imageY, imageX + imageW, imageY + imageH);
                        if (bitmapDrawable instanceof AnimatedFileDrawable) {
                            ((AnimatedFileDrawable) bitmapDrawable).setActualDrawRect(imageX, imageY, imageW, imageH);
                        }
                        if (orientation % 360 == 90 || orientation % 360 == 270) {
                            int width = (drawRegion.right - drawRegion.left) / 2;
                            int height = (drawRegion.bottom - drawRegion.top) / 2;
                            int centerX = (drawRegion.right + drawRegion.left) / 2;
                            int centerY = (drawRegion.top + drawRegion.bottom) / 2;
                            bitmapDrawable.setBounds(centerX - height, centerY - width, centerX + height, centerY + width);
                        } else {
                            bitmapDrawable.setBounds(drawRegion);
                        }
                        if (isVisible) {
                            try {
                                bitmapDrawable.setAlpha(alpha);
                                bitmapDrawable.draw(canvas);
                            } catch (Exception e) {
                                if (bitmapDrawable == currentImage && currentKey != null) {
                                    GalleryImageLoader.getInstance().removeImage(currentKey);
                                    currentKey = null;
                                } else if (bitmapDrawable == currentThumb && currentThumbKey != null) {
                                    GalleryImageLoader.getInstance().removeImage(currentThumbKey);
                                    currentThumbKey = null;
                                }
                                setImage(currentImageLocation, currentHttpUrl, currentFilter, currentThumb, currentThumbLocation, currentThumbFilter, currentSize, currentExt, currentCacheType);
                                FileLog.e(e);
                            }
                        }
                        canvas.restore();
                    }
                }
            }
        } else {
            drawRegion.set(imageX, imageY, imageX + imageW, imageY + imageH);
            drawable.setBounds(drawRegion);
            if (isVisible) {
                try {
                    drawable.setAlpha(alpha);
                    drawable.draw(canvas);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }

    private void checkAlphaAnimation(boolean skip) {
        if (manualAlphaAnimator) {
            return;
        }
        if (currentAlpha != 1) {
            if (!skip) {
                long currentTime = System.currentTimeMillis();
                long dt = currentTime - lastUpdateAlphaTime;
                if (dt > 18) {
                    dt = 18;
                }
                currentAlpha += dt / 150.0f;
                if (currentAlpha > 1) {
                    currentAlpha = 1;
                }
            }
            lastUpdateAlphaTime = System.currentTimeMillis();
            if (parentView != null) {
                if (invalidateAll) {
                    parentView.invalidate();
                } else {
                    parentView.invalidate(imageX, imageY, imageX + imageW, imageY + imageH);
                }
            }
        }
    }

    public boolean draw(Canvas canvas) {
        try {
            Drawable drawable = null;
            boolean animationNotReady = currentImage instanceof AnimatedFileDrawable && !((AnimatedFileDrawable) currentImage).hasBitmap();
            boolean isThumb = false;
            if (!forcePreview && currentImage != null && !animationNotReady) {
                drawable = currentImage;
            } else if (staticThumb instanceof BitmapDrawable) {
                drawable = staticThumb;
                isThumb = true;
            } else if (currentThumb != null) {
                drawable = currentThumb;
                isThumb = true;
            }
            if (drawable != null) {
                if (crossfadeAlpha != 0) {
                    if (crossfadeWithThumb && animationNotReady) {
                        drawDrawable(canvas, drawable, (int) (overrideAlpha * 255), bitmapShaderThumb);
                    } else {
                        if (crossfadeWithThumb && currentAlpha != 1.0f) {
                            Drawable thumbDrawable = null;
                            if (drawable == currentImage) {
                                if (staticThumb != null) {
                                    thumbDrawable = staticThumb;
                                } else if (currentThumb != null) {
                                    thumbDrawable = currentThumb;
                                }
                            } else if (drawable == currentThumb) {
                                if (staticThumb != null) {
                                    thumbDrawable = staticThumb;
                                }
                            }
                            if (thumbDrawable != null) {
                                drawDrawable(canvas, thumbDrawable, (int) (overrideAlpha * 255), bitmapShaderThumb);
                            }
                        }
                        drawDrawable(canvas, drawable, (int) (overrideAlpha * currentAlpha * 255), isThumb ? bitmapShaderThumb : bitmapShader);
                    }
                } else {
                    drawDrawable(canvas, drawable, (int) (overrideAlpha * 255), isThumb ? bitmapShaderThumb : bitmapShader);
                }

                checkAlphaAnimation(animationNotReady && crossfadeWithThumb);
                return true;
            } else if (staticThumb != null) {
                drawDrawable(canvas, staticThumb, 255, null);
                checkAlphaAnimation(animationNotReady);
                return true;
            } else {
                checkAlphaAnimation(animationNotReady);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public void setManualAlphaAnimator(boolean value) {
        manualAlphaAnimator = value;
    }

    public float getCurrentAlpha() {
        return currentAlpha;
    }

    public void setCurrentAlpha(float value) {
        currentAlpha = value;
    }

    public Bitmap getBitmap() {
        if (currentImage instanceof AnimatedFileDrawable) {
            return ((AnimatedFileDrawable) currentImage).getAnimatedBitmap();
        } else if (staticThumb instanceof AnimatedFileDrawable) {
            return ((AnimatedFileDrawable) staticThumb).getAnimatedBitmap();
        } else if (currentImage instanceof BitmapDrawable) {
            return ((BitmapDrawable) currentImage).getBitmap();
        } else if (currentThumb instanceof BitmapDrawable) {
            return ((BitmapDrawable) currentThumb).getBitmap();
        } else if (staticThumb instanceof BitmapDrawable) {
            return ((BitmapDrawable) staticThumb).getBitmap();
        }
        return null;
    }

    public Bitmap getThumbBitmap() {
        if (currentThumb instanceof BitmapDrawable) {
            return ((BitmapDrawable) currentThumb).getBitmap();
        } else if (staticThumb instanceof BitmapDrawable) {
            return ((BitmapDrawable) staticThumb).getBitmap();
        }
        return null;
    }

    public int getBitmapWidth() {
        if (currentImage instanceof AnimatedFileDrawable) {
            return orientation % 360 == 0 || orientation % 360 == 180 ? currentImage.getIntrinsicWidth() : currentImage.getIntrinsicHeight();
        } else if (staticThumb instanceof AnimatedFileDrawable) {
            return orientation % 360 == 0 || orientation % 360 == 180 ? staticThumb.getIntrinsicWidth() : staticThumb.getIntrinsicHeight();
        }
        Bitmap bitmap = getBitmap();
        if (bitmap == null) {
            if (staticThumb != null) {
                return staticThumb.getIntrinsicWidth();
            }
            return 1;
        }
        return orientation % 360 == 0 || orientation % 360 == 180 ? bitmap.getWidth() : bitmap.getHeight();
    }

    public int getBitmapHeight() {
        if (currentImage instanceof AnimatedFileDrawable) {
            return orientation % 360 == 0 || orientation % 360 == 180 ? currentImage.getIntrinsicHeight() : currentImage.getIntrinsicWidth();
        } else if (staticThumb instanceof AnimatedFileDrawable) {
            return orientation % 360 == 0 || orientation % 360 == 180 ? staticThumb.getIntrinsicHeight() : staticThumb.getIntrinsicWidth();
        }
        Bitmap bitmap = getBitmap();
        if (bitmap == null) {
            if (staticThumb != null) {
                return staticThumb.getIntrinsicHeight();
            }
            return 1;
        }
        return orientation % 360 == 0 || orientation % 360 == 180 ? bitmap.getHeight() : bitmap.getWidth();
    }

    public void setVisible(boolean value, boolean invalidate) {
        if (isVisible == value) {
            return;
        }
        isVisible = value;
        if (invalidate && parentView != null) {
            if (invalidateAll) {
                parentView.invalidate();
            } else {
                parentView.invalidate(imageX, imageY, imageX + imageW, imageY + imageH);
            }
        }
    }

    public boolean getVisible() {
        return isVisible;
    }

    public void setAlpha(float value) {
        overrideAlpha = value;
    }

    public void setCrossfadeAlpha(byte value) {
        crossfadeAlpha = value;
    }

    public boolean hasImage() {
        return currentImage != null || currentThumb != null || currentKey != null || currentHttpUrl != null || staticThumb != null;
    }

    public boolean hasBitmapImage() {
        return currentImage != null || currentThumb != null || staticThumb != null;
    }

    public void setAspectFit(boolean value) {
        isAspectFit = value;
    }

    public void setParentView(View view) {
        parentView = view;
        if (currentImage instanceof AnimatedFileDrawable) {
            AnimatedFileDrawable fileDrawable = (AnimatedFileDrawable) currentImage;
            fileDrawable.setParentView(parentView);
        }
    }

    public void setImageY(int y) {
        imageY = y;
    }

    public void setImageCoords(int x, int y, int width, int height) {
        imageX = x;
        imageY = y;
        imageW = width;
        imageH = height;
    }

    public float getCenterX() {
        return imageX + imageW / 2.0f;
    }

    public float getCenterY() {
        return imageY + imageH / 2.0f;
    }

    public int getImageX() {
        return imageX;
    }

    public int getImageX2() {
        return imageX + imageW;
    }

    public int getImageY() {
        return imageY;
    }

    public int getImageY2() {
        return imageY + imageH;
    }

    public int getImageWidth() {
        return imageW;
    }

    public int getImageHeight() {
        return imageH;
    }

    public String getExt() {
        return currentExt;
    }

    public boolean isInsideImage(float x, float y) {
        return x >= imageX && x <= imageX + imageW && y >= imageY && y <= imageY + imageH;
    }

    public Rect getDrawRegion() {
        return drawRegion;
    }

    public String getFilter() {
        return currentFilter;
    }

    public String getThumbFilter() {
        return currentThumbFilter;
    }

    public String getKey() {
        return currentKey;
    }

    public String getThumbKey() {
        return currentThumbKey;
    }

    public int getSize() {
        return currentSize;
    }

    public TLObject getImageLocation() {
        return currentImageLocation;
    }

    public FileLocation getThumbLocation() {
        return currentThumbLocation;
    }

    public String getHttpImageLocation() {
        return currentHttpUrl;
    }

    public int getCacheType() {
        return currentCacheType;
    }

    public void setForcePreview(boolean value) {
        forcePreview = value;
    }

    public void setForceCrossfade(boolean value) {
        forceCrossfade = value;
    }

    public boolean isForcePreview() {
        return forcePreview;
    }

    public void setRoundRadius(int value) {
        roundRadius = value;
    }

    public int getRoundRadius() {
        return roundRadius;
    }

    public void setNeedsQualityThumb(boolean value) {
        needsQualityThumb = value;
        if (needsQualityThumb) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageThumbGenerated);
        } else {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageThumbGenerated);
        }
    }

    public boolean isNeedsQualityThumb() {
        return needsQualityThumb;
    }

    public void setShouldGenerateQualityThumb(boolean value) {
        shouldGenerateQualityThumb = value;
    }

    public boolean isShouldGenerateQualityThumb() {
        return shouldGenerateQualityThumb;
    }

    public void setAllowStartAnimation(boolean value) {
        allowStartAnimation = value;
    }

    public void setAllowDecodeSingleFrame(boolean value) {
        allowDecodeSingleFrame = value;
    }

    public boolean isAllowStartAnimation() {
        return allowStartAnimation;
    }

    public void startAnimation() {
        if (currentImage instanceof AnimatedFileDrawable) {
            ((AnimatedFileDrawable) currentImage).start();
        }
    }

    public void stopAnimation() {
        if (currentImage instanceof AnimatedFileDrawable) {
            ((AnimatedFileDrawable) currentImage).stop();
        }
    }

    public boolean isAnimationRunning() {
        return currentImage instanceof AnimatedFileDrawable && ((AnimatedFileDrawable) currentImage).isRunning();
    }

    public AnimatedFileDrawable getAnimation() {
        return currentImage instanceof AnimatedFileDrawable ? (AnimatedFileDrawable) currentImage : null;
    }

    public Integer getTag(boolean thumb) {
        if (thumb) {
            return thumbTag;
        } else {
            return tag;
        }
    }

    public void setTag(Integer value, boolean thumb) {
        if (thumb) {
            thumbTag = value;
        } else {
            tag = value;
        }
    }

    public boolean setImageBitmapByKey(BitmapDrawable bitmap, String key, boolean thumb, boolean memCache) {
        if (bitmap == null || key == null) {
            return false;
        }
        if (!thumb) {
            if (currentKey == null || !key.equals(currentKey)) {
                return false;
            }
            if (!(bitmap instanceof AnimatedFileDrawable)) {
                GalleryImageLoader.getInstance().incrementUseCount(currentKey);
            }
            currentImage = bitmap;
            if (roundRadius != 0 && bitmap instanceof BitmapDrawable) {
                if (bitmap instanceof AnimatedFileDrawable) {
                    ((AnimatedFileDrawable) bitmap).setRoundRadius(roundRadius);
                } else {
                    Bitmap object = bitmap.getBitmap();
                    bitmapShader = new BitmapShader(object, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                }
            } else {
                bitmapShader = null;
            }

            if (!memCache && !forcePreview || forceCrossfade) {
                if (currentThumb == null && staticThumb == null || currentAlpha == 1.0f || forceCrossfade) {
                    currentAlpha = 0.0f;
                    lastUpdateAlphaTime = System.currentTimeMillis();
                    crossfadeWithThumb = currentThumb != null || staticThumb != null;
                }
            } else {
                currentAlpha = 1.0f;
            }
            if (bitmap instanceof AnimatedFileDrawable) {
                AnimatedFileDrawable fileDrawable = (AnimatedFileDrawable) bitmap;
                fileDrawable.setParentView(parentView);
                if (allowStartAnimation) {
                    fileDrawable.start();
                } else {
                    fileDrawable.setAllowDecodeSingleFrame(allowDecodeSingleFrame);
                }
            }

            if (parentView != null) {
                if (invalidateAll) {
                    parentView.invalidate();
                } else {
                    parentView.invalidate(imageX, imageY, imageX + imageW, imageY + imageH);
                }
            }
        } else if (currentThumb == null && (currentImage == null || (currentImage instanceof AnimatedFileDrawable && !((AnimatedFileDrawable) currentImage).hasBitmap()) || forcePreview)) {
            if (currentThumbKey == null || !key.equals(currentThumbKey)) {
                return false;
            }
            GalleryImageLoader.getInstance().incrementUseCount(currentThumbKey);

            currentThumb = bitmap;

            if (roundRadius != 0 && bitmap instanceof BitmapDrawable) {
                if (bitmap instanceof AnimatedFileDrawable) {
                    ((AnimatedFileDrawable) bitmap).setRoundRadius(roundRadius);
                } else {
                    Bitmap object = bitmap.getBitmap();
                    bitmapShaderThumb = new BitmapShader(object, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                }
            } else {
                bitmapShaderThumb = null;
            }

            if (!memCache && crossfadeAlpha != 2) {
                currentAlpha = 0.0f;
                lastUpdateAlphaTime = System.currentTimeMillis();
                crossfadeWithThumb = staticThumb != null && currentKey == null;
            } else {
                currentAlpha = 1.0f;
            }

            if (!(staticThumb instanceof BitmapDrawable) && parentView != null) {
                if (invalidateAll) {
                    parentView.invalidate();
                } else {
                    parentView.invalidate(imageX, imageY, imageX + imageW, imageY + imageH);
                }
            }
        }

        if (delegate != null) {
            delegate.didSetImage(this, currentImage != null || currentThumb != null || staticThumb != null, currentImage == null);
        }
        return true;
    }

    private void recycleBitmap(String newKey, boolean thumb) {
        String key;
        Drawable image;
        if (thumb) {
            key = currentThumbKey;
            image = currentThumb;
        } else {
            key = currentKey;
            image = currentImage;
        }
        if (key != null && (newKey == null || !newKey.equals(key)) && image != null) {
            if (image instanceof AnimatedFileDrawable) {
                AnimatedFileDrawable fileDrawable = (AnimatedFileDrawable) image;
                fileDrawable.recycle();
            } else if (image instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) image).getBitmap();
                boolean canDelete = GalleryImageLoader.getInstance().decrementUseCount(key);
                if (!GalleryImageLoader.getInstance().isInCache(key)) {
                    if (canDelete) {
                        bitmap.recycle();
                    }
                }
            }
        }
        if (thumb) {
            currentThumb = null;
            currentThumbKey = null;
        } else {
            currentImage = null;
            currentKey = null;
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.messageThumbGenerated) {
            String key = (String) args[1];
            if (currentThumbKey != null && currentThumbKey.equals(key)) {
                if (currentThumb == null) {
                    GalleryImageLoader.getInstance().incrementUseCount(currentThumbKey);
                }
                currentThumb = (BitmapDrawable) args[0];
                if (roundRadius != 0 && currentImage == null && currentThumb != null && !(currentThumb instanceof AnimatedFileDrawable)) {
                    Bitmap object = ((BitmapDrawable) currentThumb).getBitmap();
                    bitmapShaderThumb = new BitmapShader(object, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                } else {
                    bitmapShaderThumb = null;
                }
                if (staticThumb instanceof BitmapDrawable) {
                    staticThumb = null;
                }
                if (parentView != null) {
                    if (invalidateAll) {
                        parentView.invalidate();
                    } else {
                        parentView.invalidate(imageX, imageY, imageX + imageW, imageY + imageH);
                    }
                }
            }
        } else if (id == NotificationCenter.didReplacedPhotoInMemCache) {
            String oldKey = (String) args[0];
            if (currentKey != null && currentKey.equals(oldKey)) {
                currentKey = (String) args[1];
                currentImageLocation = (FileLocation) args[2];
            }
            if (currentThumbKey != null && currentThumbKey.equals(oldKey)) {
                currentThumbKey = (String) args[1];
                currentThumbLocation = (FileLocation) args[2];
            }
            if (setImageBackup != null) {
                if (currentKey != null && currentKey.equals(oldKey)) {
                    currentKey = (String) args[1];
                    currentImageLocation = (FileLocation) args[2];
                }
                if (currentThumbKey != null && currentThumbKey.equals(oldKey)) {
                    currentThumbKey = (String) args[1];
                    currentThumbLocation = (FileLocation) args[2];
                }
            }
        }
    }
}
