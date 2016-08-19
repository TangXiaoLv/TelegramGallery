package com.tangxiaolv.telegramgallery.Components;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.tangxiaolv.telegramgallery.Utils.AndroidUtilities;

public class SizeNotifierFrameLayoutPhoto extends FrameLayout {

    private Rect rect = new Rect();
    private int keyboardHeight;
    private SizeNotifierFrameLayoutPhotoDelegate delegate;
    private WindowManager windowManager;

    public interface SizeNotifierFrameLayoutPhotoDelegate {
        void onSizeChanged(int keyboardHeight, boolean isWidthGreater);
    }

    public SizeNotifierFrameLayoutPhoto(Context context) {
        super(context);
    }

    public void setDelegate(SizeNotifierFrameLayoutPhotoDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        notifyHeightChanged();
    }

    public int getKeyboardHeight() {
        View rootView = getRootView();
        int usableViewHeight = rootView.getHeight() - AndroidUtilities.getViewInset(rootView);
        getWindowVisibleDisplayFrame(rect);
        int top = rect.top;
        int size = AndroidUtilities.displaySize.y - top - usableViewHeight;
        if (size <= AndroidUtilities.dp(10)) {
            size = 0;
        }
        return size;
    }

    public void notifyHeightChanged() {
        if (delegate != null) {
            keyboardHeight = getKeyboardHeight();
            final boolean isWidthGreater = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y;
            post(new Runnable() {
                @Override
                public void run() {
                    if (delegate != null) {
                        delegate.onSizeChanged(keyboardHeight, isWidthGreater);
                    }
                }
            });
        }
    }
}
