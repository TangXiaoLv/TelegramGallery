package com.tangxiaolv.telegramgallery.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.tangxiaolv.telegramgallery.utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.utils.MediaController;

import java.util.ArrayList;
import java.util.List;

public class PreviewIconCellContainer extends HorizontalScrollView {

    private PreviewIconCell checkedChild;
    private LinearLayout innerContainer;
    private List<Object> datas;
    private boolean fromPreview;
    private final int CELL_WIDTH = 72;
    private PreviewIconCellContainerActions previewIconCellContainerActions;
    private AnimatorSet animatorSet;
    private boolean showing;

    public PreviewIconCellContainer(Context context) {
        this(context, null);
    }

    public PreviewIconCellContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewIconCellContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        innerContainer = new LinearLayout(context);
        innerContainer.setOrientation(LinearLayout.HORIZONTAL);
        /*LayoutTransition transition = new LayoutTransition();
        PropertyValuesHolder pvhLeft =
                PropertyValuesHolder.ofInt("left", 0, CELL_WIDTH,0);
        PropertyValuesHolder pvhTop =
                PropertyValuesHolder.ofInt("top", 0, 0);
        PropertyValuesHolder pvhRight =
                PropertyValuesHolder.ofInt("right", 0, 0);
        PropertyValuesHolder pvhBottom =
                PropertyValuesHolder.ofInt("bottom", 0, 0);
        transition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING,
                ObjectAnimator.ofPropertyValuesHolder(innerContainer, pvhLeft, pvhBottom).setDuration(50));
        innerContainer.setLayoutTransition(transition);*/
        addView(innerContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    public void generatePreviewIconCell(List<Object> photos, boolean fromPreview) {
        //reset
        this.datas = null;
        this.fromPreview = fromPreview;
        innerContainer.removeAllViews();
        if (photos == null || photos.size() == 0) {
            setVisibility(View.GONE);
            return;
        }
        datas = photos;
        setVisibility(View.VISIBLE);
        for (int i = 0; i < photos.size(); i++) {
            PreviewIconCell cell = new PreviewIconCell(getContext(), this,
                    (MediaController.PhotoEntry) photos.get(i));
            innerContainer.addView(cell, LayoutHelper.createFrame(CELL_WIDTH, LayoutHelper.MATCH_PARENT));
        }
    }

    public void setCheckedChild(PreviewIconCell checkedChild) {
        this.checkedChild = checkedChild;
    }

    public PreviewIconCell getCheckedChild() {
        return checkedChild;
    }

    public void requestCheckedChanged(int imageId) {
        if (datas == null) {
            return;
        }
        PreviewIconCell cell = findCellByImageId(imageId);
        moveToIndex(cell);
    }

    private void moveToIndex(PreviewIconCell cell) {
        if (cell != null) {
            cell.setChecked(true);
            //scroll
            int[] location = new int[2];
            cell.getLocationOnScreen(location);
            int width = cell.getWidth();
            if (location[0] == getWidth()) {
                smoothScrollBy(width, 0);
            } else if (location[0] + width > getWidth()) {
                int diff = location[0] + width - getWidth();
                smoothScrollBy(diff, 0);
            } else if (location[0] < 0) {
                smoothScrollBy(location[0], 0);
            }
        } else if (checkedChild != null) {
            checkedChild.setChecked(false);
        }
    }

    public void requestCanceledChanged(MediaController.PhotoEntry entry, boolean canceled) {
        if (fromPreview) {
            PreviewIconCell cell = findCellByImageId(entry.imageId);
            if (cell != null) {
                cell.setCanceled(canceled);
            }
        } else if (canceled) {
            int index = datas.indexOf(entry);
            innerContainer.removeViewAt(index);
            datas.remove(entry);
            if (datas.size() == 0) {
                //setVisibility(GONE);
                startAnimation(false);
            }
        } else {
            if (datas == null) {
                datas = new ArrayList<>();
            }
            if (getVisibility() != VISIBLE) {
                //setVisibility(VISIBLE);
                startAnimation(true);
            }
            datas.add(entry);
            final PreviewIconCell cell = new PreviewIconCell(getContext(), this, entry);
            innerContainer.addView(cell, LayoutHelper.createFrame(CELL_WIDTH, LayoutHelper.MATCH_PARENT));
            post(new Runnable() {
                @Override
                public void run() {
                    moveToIndex(cell);
                }
            });
        }
    }

    private PreviewIconCell findCellByImageId(int imageId) {
        if (datas == null || datas.size() == 0) {
            return null;
        }

        for (int i = 0; i < datas.size(); i++) {
            PreviewIconCell cell = (PreviewIconCell) ((LinearLayout) getChildAt(0)).getChildAt(i);
            if (cell.getCellId() == imageId) {
                return cell;
            }
        }

        return null;
    }

    public void setPreviewIconCellContainerActions(PreviewIconCellContainerActions actions) {
        this.previewIconCellContainerActions = actions;
    }

    public void notificationCheckChanged(int imageId) {
        if (previewIconCellContainerActions != null) {
            previewIconCellContainerActions.checkChanged(imageId);
        }

    }

    public interface PreviewIconCellContainerActions {
        void checkChanged(int imageId);
    }

    private void startAnimation(final boolean show) {
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }
        animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(180);
        if (show && getVisibility() == View.GONE) {
            setVisibility(View.VISIBLE);
        }
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(this, "alpha", show ? 1.0f : 0.0f));
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animation.equals(animatorSet)) {
                    animatorSet = null;
                }
                showing = show;
                if (!show) {
                    setVisibility(View.GONE);
                }
            }
        });
        animatorSet.start();
    }

    public boolean isShowing() {
        return showing;
    }
}
