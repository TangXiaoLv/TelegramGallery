
package com.tangxiaolv.telegramgallery.components;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.tangxiaolv.telegramgallery.AnimatorListenerAdapterProxy;
import com.tangxiaolv.telegramgallery.R;
import com.tangxiaolv.telegramgallery.utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.utils.LayoutHelper;

import static com.tangxiaolv.telegramgallery.components.CheckBox.sCheckColor;
import static com.tangxiaolv.telegramgallery.utils.Constants.DARK_THEME;

public class PhotoPickerPhotoCell extends FrameLayout {

    public BackupImageView photoImage;
    public FrameLayout infoContainer;
    public TextView videoTextView;
    //选中的圆圈
    public FrameLayout checkFrame;
    public CheckBox checkBox;
    private AnimatorSet animator;
    public int itemWidth;
    private Actions actions;
    private ImageView imageInfo;
    public View clickableView;

    public PhotoPickerPhotoCell(Context context) {
        super(context);

        photoImage = new BackupImageView(context);
        addView(photoImage,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        checkFrame = new FrameLayout(context);
        addView(checkFrame, LayoutHelper.createFrame(42, 42, Gravity.RIGHT | Gravity.TOP));

        infoContainer = new FrameLayout(context);
        infoContainer.setBackgroundColor(0x7f000000);
        infoContainer.setPadding(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3), 0);
        addView(infoContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 16, Gravity.BOTTOM | Gravity.LEFT));

        imageInfo = new ImageView(context);
        infoContainer.addView(imageInfo, LayoutHelper.createFrame(14, 9, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        videoTextView = new TextView(context);
        videoTextView.setTextColor(0xffffffff);
        videoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        infoContainer.addView(videoTextView, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL,
                18, 0, 0, 0));

        checkBox = new CheckBox(context, R.drawable.album_checkbig);
        checkBox.setSize(24);
        checkBox.setCheckOffset(AndroidUtilities.dp(1));
        checkBox.setDrawBackground(true);
        checkBox.setColor(sCheckColor);
        addView(checkBox, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.TOP, 0, 4, 4, 0));

        /*置灰*/
        /*clickableView = new View(context);
        clickableView.setBackgroundResource(R.drawable.preview_cell_canceled);
        addView(clickableView,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));*/
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY));
    }

    public void setChecked(final boolean checked, final boolean animated) {
        checkBox.setChecked(checked, animated);
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (animated) {
            if (checked) {
                setBackgroundColor(DARK_THEME ? 0xff0A0A0A : 0xffffffff);
            }
            animator = new AnimatorSet();
            animator.playTogether(
                    ObjectAnimator.ofFloat(photoImage, "scaleX", checked ? 0.85f : 1.0f),
                    ObjectAnimator.ofFloat(photoImage, "scaleY", checked ? 0.85f : 1.0f));
//                    ObjectAnimator.ofFloat(infoContainer, "scaleX", checked ? 0.85f : 1.0f),
//                    ObjectAnimator.ofFloat(infoContainer, "scaleY", checked ? 0.85f : 1.0f));
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animator != null && animator.equals(animation)) {
                        animator = null;
                        if (!checked) {
                            setBackgroundColor(0);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (animator != null && animator.equals(animation)) {
                        animator = null;
                    }
                }
            });
            animator.start();
        } else {
            setBackgroundColor(checked ? DARK_THEME ? 0xff0A0A0A : 0xffffffff : 0);
            photoImage.setScaleX(checked ? 0.85f : 1.0f);
            photoImage.setScaleY(checked ? 0.85f : 1.0f);
            infoContainer.setScaleX(checked ? 0.85f : 1.0f);
            infoContainer.setScaleY(checked ? 0.85f : 1.0f);
        }
    }

    public void setChecked(int num, final boolean checked, final boolean animated) {
        if (!checkBox.isEnabled()) {
            return;
        }
        checkBox.setChecked(num, checked, animated);
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (animated) {
            if (checked) {
                setBackgroundColor(DARK_THEME ? 0xff0A0A0A : 0xffffffff);
            }
            animator = new AnimatorSet();
            animator.playTogether(
                    ObjectAnimator.ofFloat(photoImage, "scaleX", checked ? 0.85f : 1.0f),
                    ObjectAnimator.ofFloat(photoImage, "scaleY", checked ? 0.85f : 1.0f));
//                    ObjectAnimator.ofFloat(infoContainer, "scaleX", checked ? 0.85f : 1.0f),
//                    ObjectAnimator.ofFloat(infoContainer, "scaleY", checked ? 0.85f : 1.0f));
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animator != null && animator.equals(animation)) {
                        animator = null;
                        if (!checked) {
                            setBackgroundColor(0);
                        }
                    }

                    if (actions != null) {
                        actions.onUnCheckedAnimationEnd();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (animator != null && animator.equals(animation)) {
                        animator = null;
                    }
                }
            });
            animator.start();
        } else {
            setBackgroundColor(checked ? DARK_THEME ? 0xff0A0A0A : 0xffffffff : 0);
            photoImage.setScaleX(checked ? 0.85f : 1.0f);
            photoImage.setScaleY(checked ? 0.85f : 1.0f);
//            infoContainer.setScaleX(checked ? 0.85f : 1.0f);
//            infoContainer.setScaleY(checked ? 0.85f : 1.0f);
        }
    }

    public void showVideoInfo() {
        infoContainer.setVisibility(VISIBLE);
        videoTextView.setVisibility(VISIBLE);
        imageInfo.setImageResource(R.drawable.ic_video);
    }

    public void showGifInfo() {
        infoContainer.setVisibility(VISIBLE);
        videoTextView.setVisibility(INVISIBLE);
        imageInfo.setImageResource(R.drawable.ic_gif);
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    public interface Actions {
        void onUnCheckedAnimationEnd();
    }
}
