package com.tangxiaolv.telegramgallery.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.tangxiaolv.telegramgallery.R;
import com.tangxiaolv.telegramgallery.utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.utils.MediaController;

import static com.tangxiaolv.telegramgallery.utils.Constants.DARK_THEME;

public class PreviewIconCell extends FrameLayout implements View.OnClickListener {

    private BackupImageView imageView;
    private View frameChecked;
    private View frameCanceled;
    private PreviewIconCellContainer parent;
    private MediaController.PhotoEntry photoEntry;
    private boolean canceled = false;

    public PreviewIconCell(
            Context context,
            PreviewIconCellContainer parent,
            MediaController.PhotoEntry photoEntry) {
        this(context);
        this.parent = parent;
        this.photoEntry = photoEntry;
        imageView = new BackupImageView(context);
        addView(imageView, LayoutHelper.createFrame(60, 60, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

        if (photoEntry.thumbPath != null) {
            imageView.setImage(photoEntry.thumbPath, null,
                    context.getResources().getDrawable(DARK_THEME
                            ? R.drawable.album_nophotos
                            : R.drawable.album_nophotos_new));
        } else if (photoEntry.path != null) {
            imageView.setOrientation(photoEntry.orientation, true);
            if (photoEntry.isVideo) {
                imageView.setImage(
                        "vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                        context.getResources().getDrawable(
                                DARK_THEME ? R.drawable.album_nophotos : R.drawable.album_nophotos_new));
            } else {
                imageView.setImage(
                        "thumb://" + photoEntry.imageId + ":" + photoEntry.path, null,
                        context.getResources().getDrawable(
                                DARK_THEME ? R.drawable.album_nophotos : R.drawable.album_nophotos_new));
            }
        } else {
            imageView.setImageResource(
                    DARK_THEME ? R.drawable.album_nophotos : R.drawable.album_nophotos_new);
        }

        FrameLayout infoContainer = new FrameLayout(context);
        infoContainer.setBackgroundColor(0x7f000000);
        infoContainer.setPadding(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3), 0);
        addView(infoContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 16,
                Gravity.BOTTOM | Gravity.START, 12, 0, 0, 14));

        ImageView imageInfo = new ImageView(context);
        infoContainer.addView(
                imageInfo, LayoutHelper.createFrame(14, 9, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        if (photoEntry.isVideo) {
            imageInfo.setImageResource(R.drawable.ic_video);
        } else if (photoEntry.path.lastIndexOf(".gif") != -1) {
            imageInfo.setImageResource(R.drawable.ic_gif);
        } else {
            infoContainer.setVisibility(GONE);
        }

        frameCanceled = new View(context);
        frameCanceled.setBackgroundResource(R.drawable.preview_cell_canceled);
        addView(frameCanceled, LayoutHelper.createFrame(60, 60, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

        frameChecked = new View(context);
        frameChecked.setBackgroundResource(R.drawable.preview_cell_checked);
        addView(frameChecked, LayoutHelper.createFrame(60, 60, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

        frameChecked.setVisibility(GONE);
        frameCanceled.setVisibility(GONE);

        setOnClickListener(this);
    }

    public PreviewIconCell(Context context) {
        this(context, null);
    }

    public PreviewIconCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewIconCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public View getFrameChecked() {
        return frameChecked;
    }

    public void setChecked(boolean checked) {
        frameChecked.setVisibility(checked ? VISIBLE : GONE);
        PreviewIconCell child = parent.getCheckedChild();
        if (child != null && child != this) {
            child.getFrameChecked().setVisibility(GONE);
        }
        parent.setCheckedChild(this);
    }

    @Override
    public void onClick(View v) {
        parent.notificationCheckChanged(photoEntry.imageId);
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
        frameCanceled.setVisibility(canceled ? VISIBLE : GONE);
    }

    public int getCellId() {
        return photoEntry.imageId;
    }
}
