
package com.tangxiaolv.telegramgallery;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import com.tangxiaolv.telegramgallery.Utils.AndroidUtilities;

public class Theme {

    public static final int ACTION_BAR_COLOR = 0xff527da3;
    public static final int ACTION_BAR_PHOTO_VIEWER_COLOR = 0x7f000000;
    public static final int ACTION_BAR_MEDIA_PICKER_COLOR = 0xff333333;
    public static final int ACTION_BAR_SUBTITLE_COLOR = 0xffd5e8f7;
    public static final int ACTION_BAR_SELECTOR_COLOR = 0xff406d94;

    public static final int ACTION_BAR_PICKER_SELECTOR_COLOR = 0xff3d3d3d;
    public static final int ACTION_BAR_WHITE_SELECTOR_COLOR = 0x40ffffff;
    public static final int ACTION_BAR_AUDIO_SELECTOR_COLOR = 0x2f000000;
    public static final int ACTION_BAR_MODE_SELECTOR_COLOR = 0xfff0f0f0;

    private static Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static Drawable createBarSelectorDrawable(int color) {
        return createBarSelectorDrawable(color, true);
    }

    public static Drawable createBarSelectorDrawable(int color, boolean masked) {
        if (Build.VERSION.SDK_INT >= 21) {
            Drawable maskDrawable = null;
            if (masked) {
                maskPaint.setColor(0xffffffff);
                maskDrawable = new Drawable() {
                    @Override
                    public void draw(Canvas canvas) {
                        android.graphics.Rect bounds = getBounds();
                        canvas.drawCircle(bounds.centerX(), bounds.centerY(),
                                AndroidUtilities.dp(18), maskPaint);
                    }

                    @Override
                    public void setAlpha(int alpha) {

                    }

                    @Override
                    public void setColorFilter(ColorFilter colorFilter) {

                    }

                    @Override
                    public int getOpacity() {
                        return 0;
                    }
                };
            }
            ColorStateList colorStateList = new ColorStateList(
                    new int[][] {
                            new int[] {}
                    },
                    new int[] {
                            color
                    });
            return new RippleDrawable(colorStateList, null, maskDrawable);
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[] {
                    android.R.attr.state_pressed
            }, new ColorDrawable(color));
            stateListDrawable.addState(new int[] {
                    android.R.attr.state_focused
            }, new ColorDrawable(color));
            stateListDrawable.addState(new int[] {
                    android.R.attr.state_selected
            }, new ColorDrawable(color));
            stateListDrawable.addState(new int[] {
                    android.R.attr.state_activated
            }, new ColorDrawable(color));
            stateListDrawable.addState(new int[] {}, new ColorDrawable(0x00000000));
            return stateListDrawable;
        }
    }
}
