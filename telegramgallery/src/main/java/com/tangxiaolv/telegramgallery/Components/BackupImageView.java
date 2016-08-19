package com.tangxiaolv.telegramgallery.Components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.tangxiaolv.telegramgallery.ImageReceiver;
import com.tangxiaolv.telegramgallery.TL.FileLocation;
import com.tangxiaolv.telegramgallery.TL.TLObject;

public class BackupImageView extends View {

    private ImageReceiver imageReceiver;
    private int width = -1;
    private int height = -1;

    public BackupImageView(Context context) {
        super(context);
        init();
    }

    public BackupImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BackupImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        imageReceiver = new ImageReceiver(this);
    }

    public void setImage(TLObject path, String filter, String ext, Drawable thumb) {
        setImage(path, null, filter, thumb, null, null, null, ext, 0);
    }

    public void setImage(TLObject path, String filter, Drawable thumb) {
        setImage(path, null, filter, thumb, null, null, null, null, 0);
    }

    public void setImage(TLObject path, String filter, Bitmap thumb) {
        setImage(path, null, filter, null, thumb, null, null, null, 0);
    }

    public void setImage(TLObject path, String filter, Drawable thumb, int size) {
        setImage(path, null, filter, thumb, null, null, null, null, size);
    }

    public void setImage(TLObject path, String filter, Bitmap thumb, int size) {
        setImage(path, null, filter, null, thumb, null, null, null, size);
    }

    public void setImage(TLObject path, String filter, FileLocation thumb, int size) {
        setImage(path, null, filter, null, null, thumb, null, null, size);
    }

    public void setImage(String path, String filter, Drawable thumb) {
        setImage(null, path, filter, thumb, null, null, null, null, 0);
    }

    public void setOrientation(int angle, boolean center) {
        imageReceiver.setOrientation(angle, center);
    }

    public void setImage(TLObject path, String httpUrl, String filter, Drawable thumb,
            Bitmap thumbBitmap, FileLocation thumbLocation, String thumbFilter, String ext,
            int size) {
        if (thumbBitmap != null) {
            thumb = new BitmapDrawable(null, thumbBitmap);
        }
        imageReceiver.setImage(path, httpUrl, filter, thumb, thumbLocation, thumbFilter, size, ext,
                false);
    }

    public void setImageBitmap(Bitmap bitmap) {
        imageReceiver.setImageBitmap(bitmap);
    }

    public void setImageResource(int resId) {
        imageReceiver.setImageBitmap(getResources().getDrawable(resId));
    }

    public void setImageDrawable(Drawable drawable) {
        imageReceiver.setImageBitmap(drawable);
    }

    public void setRoundRadius(int value) {
        imageReceiver.setRoundRadius(value);
    }

    public void setAspectFit(boolean value) {
        imageReceiver.setAspectFit(value);
    }

    public ImageReceiver getImageReceiver() {
        return imageReceiver;
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        imageReceiver.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        imageReceiver.onAttachedToWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (width != -1 && height != -1) {
            imageReceiver.setImageCoords((getWidth() - width) / 2, (getHeight() - height) / 2,
                    width, height);
        } else {
            imageReceiver.setImageCoords(0, 0, getWidth(), getHeight());
        }
        imageReceiver.draw(canvas);
    }
}
