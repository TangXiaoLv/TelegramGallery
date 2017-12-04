
package com.tangxiaolv.telegramgallery.components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tangxiaolv.telegramgallery.R;
import com.tangxiaolv.telegramgallery.Theme;
import com.tangxiaolv.telegramgallery.utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.utils.LocaleController;

import java.util.Locale;

import static com.tangxiaolv.telegramgallery.Gallery.sOriginChecked;
import static com.tangxiaolv.telegramgallery.GalleryActivity.getConfig;
import static com.tangxiaolv.telegramgallery.utils.Constants.DARK_THEME;

public class PickerBottomLayout extends FrameLayout {

    public LinearLayout doneButton;
    public CheckBox originCheckBox;
    public TextView cancelButton;
    public TextView doneButtonTextView;
    public TextView originalTextView;
    public LinearLayout originalView;

    //定制化参数
    private int previewTextColor;
    private final Drawable doneNormal;
    private final Drawable doneDisable;
    private int doneTextColor;

    public PickerBottomLayout(Context context) {
        super(context);

        //预览定制
        previewTextColor = 0xff007aff;

        //发送定制
        doneNormal = getResources().getDrawable(R.drawable.shape_send);
        doneDisable = getResources().getDrawable(R.drawable.shape_unsend);
        doneNormal.setColorFilter(new PorterDuffColorFilter(0xff3395ff, PorterDuff.Mode.SRC_IN));
        doneDisable.setColorFilter(new PorterDuffColorFilter(0xff92c3f9, PorterDuff.Mode.SRC_IN));
        doneTextColor = 0xffffffff;

        //
        //初始化控件
        //

        cancelButton = new TextView(context);
        cancelButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        cancelButton.setTextColor(DARK_THEME ? 0xffffffff : 0xff9b9b9b);
        cancelButton.setGravity(Gravity.CENTER);
        cancelButton.setBackgroundDrawable(
                Theme.createBarSelectorDrawable(DARK_THEME ?
                        Theme.ACTION_BAR_PICKER_SELECTOR_COLOR
                        : Theme.ACTION_BAR_AUDIO_SELECTOR_COLOR, false)
        );
        cancelButton.setPadding(AndroidUtilities.dp(9), 0, AndroidUtilities.dp(29), 0);
        cancelButton.setText(LocaleController.getString("Preview", R.string.Preview).toUpperCase());
        // cancelButton.getPaint().setFakeBoldText(true);
        addView(cancelButton, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.MATCH_PARENT,
                Gravity.TOP | Gravity.LEFT));

        doneButton = new LinearLayout(context);
        doneButton.setOrientation(LinearLayout.HORIZONTAL);
        doneButton.setBackgroundDrawable(
                Theme.createBarSelectorDrawable(
                        DARK_THEME ? Theme.ACTION_BAR_PICKER_SELECTOR_COLOR : Theme.ACTION_BAR_AUDIO_SELECTOR_COLOR,
                        false));
        addView(doneButton, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.MATCH_PARENT,
                Gravity.TOP | Gravity.RIGHT));

        doneButtonTextView = new TextView(context);
        doneButtonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        doneButtonTextView.setBackground(doneDisable);
        //doneButtonTextView.setTextColor(DARK_THEME ? 0xffffffff : 0xff9b9b9b);
        doneButtonTextView.setTextColor(doneTextColor);
        doneButtonTextView.setGravity(Gravity.CENTER);
        doneButtonTextView.setCompoundDrawablePadding(AndroidUtilities.dp(8));
        doneButtonTextView.setMinWidth(AndroidUtilities.dp(60));
        doneButtonTextView.setMaxHeight(AndroidUtilities.dp(30));
        doneButtonTextView.setText(LocaleController.getString("Send", R.string.Send).toUpperCase());
        // doneButtonTextView.getPaint().setFakeBoldText(true);
        doneButton.addView(doneButtonTextView,
                LayoutHelper.createLinear(
                        LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, 0, 7, 9, 7));

        originalView = new LinearLayout(context);
        originalView.setOrientation(LinearLayout.HORIZONTAL);
        originalView.setBackgroundDrawable(
                Theme.createBarSelectorDrawable(
                        DARK_THEME ? Theme.ACTION_BAR_PICKER_SELECTOR_COLOR : Theme.ACTION_BAR_AUDIO_SELECTOR_COLOR,
                        false));
        addView(originalView, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        originCheckBox = new CheckBox(context, R.drawable.album_checkbig);
        originCheckBox.setCheckOffset(AndroidUtilities.dp(1));
        originCheckBox.setDrawBackground(true);
        originCheckBox.setBottomBarStyle(true);
        originCheckBox.setActionBarStyle(true);
        originCheckBox.setVisibility(VISIBLE);
        originCheckBox.setChecked(sOriginChecked, false);
        originCheckBox.setColor(0xff007aff);
        originalView.addView(originCheckBox,
                LayoutHelper.createLinear(20, 20, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));
        originalView.setVisibility(getConfig().hasOriginalPic() ? VISIBLE : GONE);

        originalTextView = new TextView(context);
        originalTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        originalTextView.setTextColor(DARK_THEME ? 0xffffffff : 0xff000000);
        originalTextView.setGravity(Gravity.CENTER);
        originalTextView.setCompoundDrawablePadding(AndroidUtilities.dp(8));
        originalTextView.setText(LocaleController.getString("Original", R.string.Original).toUpperCase());
        originalView.addView(originalTextView,
                LayoutHelper.createLinear(
                        LayoutHelper.WRAP_CONTENT,
                        LayoutHelper.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL));
    }

    public void updateSelectedCount(int count, boolean disable) {
        if (count == 0) {
            doneButtonTextView.setText(R.string.Send);
            if (disable) {
                doneButtonTextView.setBackground(doneDisable);
                cancelButton.setTextColor(0xff9b9b9b);
                doneButton.setEnabled(false);
                cancelButton.setEnabled(false);
            } else {
                doneButtonTextView.setBackground(
                        getConfig().getLimitPickPhoto() == 1 ? doneNormal : doneDisable);
            }
        } else {
            doneButtonTextView.setBackground(doneNormal);
            doneButtonTextView.setText(
                    String.format(Locale.getDefault(), getContext().getString(R.string.SendWithNum), count)
            );
            cancelButton.setTextColor(DARK_THEME ? 0xffffffff : previewTextColor);
            originalTextView.setTextColor(DARK_THEME ? 0xffffffff : 0xff000000);
            if (disable) {
                doneButton.setEnabled(true);
                cancelButton.setEnabled(true);
            }
        }
    }

    public void setChecked(final boolean checked, final boolean animated) {
        sOriginChecked = checked;
        originCheckBox.setChecked(checked, true);
    }

    public boolean isOriginChecked() {
        return sOriginChecked;
    }

    public void setOriginalViewVisibility(int visibility) {
        if (!getConfig().hasOriginalPic()) {
            return;
        }
        originalView.setVisibility(visibility);
    }

    public void setEditState(boolean show, boolean forceSend) {
        cancelButton.setVisibility(getConfig().isVideoEditMode() ? show ? VISIBLE : GONE : GONE);
        if (getConfig().isVideoEditMode()) {
            cancelButton.setEnabled(show);
            cancelButton.setTextColor(DARK_THEME ? 0xffffffff : previewTextColor);
            doneButtonTextView.setBackground(forceSend ? doneNormal : doneButtonTextView.getBackground());
        }
    }

    public void setDoneButtonText(String text) {
        doneButtonTextView.setText(text);
    }

    @ColorInt
    private int getColor(@ColorRes int id, @ColorInt int def) {
        return id == 0 ? def : getResources().getColor(id);
    }
}
