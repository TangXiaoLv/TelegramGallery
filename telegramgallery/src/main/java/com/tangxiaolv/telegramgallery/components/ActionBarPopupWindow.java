package com.tangxiaolv.telegramgallery.components;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.ArrayMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;

import com.tangxiaolv.telegramgallery.R;
import com.tangxiaolv.telegramgallery.utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.utils.LayoutHelper;

import java.lang.reflect.Field;

public class ActionBarPopupWindow extends PopupWindow {

    private static final Field superListenerField;
    private static final boolean animationEnabled = Build.VERSION.SDK_INT >= 18;
    private static DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    private AnimatorSet windowAnimatorSet;
    static {
        Field f = null;
        try {
            f = PopupWindow.class.getDeclaredField("mOnScrollChangedListener");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            /* ignored */
            e.getStackTrace();
        }
        superListenerField = f;
    }

    private static final ViewTreeObserver.OnScrollChangedListener NOP = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            /* do nothing */
        }
    };

    private ViewTreeObserver.OnScrollChangedListener mSuperScrollListener;
    private ViewTreeObserver mViewTreeObserver;

    public interface OnDispatchKeyEventListener {
        void onDispatchKeyEvent(KeyEvent keyEvent);
    }

    public static class ActionBarPopupWindowLayout extends FrameLayout {

        private OnDispatchKeyEventListener mOnDispatchKeyEventListener;
        protected static Drawable backgroundDrawable;
        private float backScaleX = 1;
        private float backScaleY = 1;
        private int backAlpha = 255;
        private int lastStartedChild = 0;
        private boolean showedFromBotton;
        private ArrayMap<View, Integer> positions = new ArrayMap<>();

        private ScrollView scrollView;
        private LinearLayout linearLayout;

        public ActionBarPopupWindowLayout(Context context) {
            super(context);

            if (backgroundDrawable == null) {
                backgroundDrawable = getResources().getDrawable(R.drawable.album_popup_fixed);
            }

            setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
            setWillNotDraw(false);

            try {
                scrollView = new ScrollView(context);
                scrollView.setVerticalScrollBarEnabled(false);
                addView(scrollView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            } catch (Throwable e) {
                e.printStackTrace();
            }


            linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            if (scrollView != null) {
                scrollView.addView(linearLayout, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            }
        }

        public void setShowedFromBotton(boolean value) {
            showedFromBotton = value;
        }

        public void setDispatchKeyEventListener(OnDispatchKeyEventListener listener) {
            mOnDispatchKeyEventListener = listener;
        }

        public void setBackAlpha(int value) {
            backAlpha = value;
        }

        public int getBackAlpha() {
            return backAlpha;
        }

        public void setBackScaleX(float value) {
            backScaleX = value;
            invalidate();
        }

        public void setBackScaleY(float value) {
            backScaleY = value;
            if (animationEnabled) {
                int count = getItemsCount();
                int visibleCount = 0;
                for (int a = 0; a < count; a++) {
                    visibleCount += getItemAt(a).getVisibility() == VISIBLE ? 1 : 0;
                }
                int height = getMeasuredHeight() - AndroidUtilities.dp(16);
                if (showedFromBotton) {
                    for (int a = lastStartedChild; a >= 0; a--) {
                        View child = getItemAt(a);
                        if (child.getVisibility() != VISIBLE) {
                            continue;
                        }
                        Integer position = positions.get(child);
                        if (position != null && height - (position * AndroidUtilities.dp(48) + AndroidUtilities.dp(32)) > value * height) {
                            break;
                        }
                        lastStartedChild = a - 1;
                        startChildAnimation(child);
                    }
                } else {
                    for (int a = lastStartedChild; a < count; a++) {
                        View child = getItemAt(a);
                        if (child.getVisibility() != VISIBLE) {
                            continue;
                        }
                        Integer position = positions.get(child);
                        if (position != null && (position + 1) * AndroidUtilities.dp(48) - AndroidUtilities.dp(24) > value * height) {
                            break;
                        }
                        lastStartedChild = a + 1;
                        startChildAnimation(child);
                    }
                }
            }
            invalidate();
        }

        private void startChildAnimation(View child) {
            if (animationEnabled) {
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(child, "alpha", 0.0f, 1.0f),
                        ObjectAnimator.ofFloat(child, "translationY", AndroidUtilities.dp(showedFromBotton ? 6 : -6), 0));
                animatorSet.setDuration(180);
                animatorSet.setInterpolator(decelerateInterpolator);
                animatorSet.start();
            }
        }

        @Override
        public void addView(View child) {
            linearLayout.addView(child);
        }

        public float getBackScaleX() {
            return backScaleX;
        }

        public float getBackScaleY() {
            return backScaleY;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (mOnDispatchKeyEventListener != null) {
                mOnDispatchKeyEventListener.onDispatchKeyEvent(event);
            }
            return super.dispatchKeyEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (backgroundDrawable != null) {
                backgroundDrawable.setAlpha(backAlpha);
                if (showedFromBotton) {
                    backgroundDrawable.setBounds(0, (int) (getMeasuredHeight() * (1.0f - backScaleY)), (int) (getMeasuredWidth() * backScaleX), getMeasuredHeight());
                } else {
                    backgroundDrawable.setBounds(0, 0, (int) (getMeasuredWidth() * backScaleX), (int) (getMeasuredHeight() * backScaleY));
                }
                backgroundDrawable.draw(canvas);
            }
        }

        public int getItemsCount() {
            return linearLayout.getChildCount();
        }

        public View getItemAt(int index) {
            return linearLayout.getChildAt(index);
        }

        public void scrollToTop() {
            if (scrollView != null) {
                scrollView.scrollTo(0, 0);
            }
        }
    }

    public ActionBarPopupWindow() {
        super();
        init();
    }

    public ActionBarPopupWindow(Context context) {
        super(context);
        init();
    }

    public ActionBarPopupWindow(int width, int height) {
        super(width, height);
        init();
    }

    public ActionBarPopupWindow(View contentView) {
        super(contentView);
        init();
    }

    public ActionBarPopupWindow(View contentView, int width, int height, boolean focusable) {
        super(contentView, width, height, focusable);
        init();
    }

    public ActionBarPopupWindow(View contentView, int width, int height) {
        super(contentView, width, height);
        init();
    }

    private void init() {
        if (superListenerField != null) {
            try {
                mSuperScrollListener = (ViewTreeObserver.OnScrollChangedListener) superListenerField.get(this);
                superListenerField.set(this, NOP);
            } catch (Exception e) {
                mSuperScrollListener = null;
            }
        }
    }

    private void unregisterListener() {
        if (mSuperScrollListener != null && mViewTreeObserver != null) {
            if (mViewTreeObserver.isAlive()) {
                mViewTreeObserver.removeOnScrollChangedListener(mSuperScrollListener);
            }
            mViewTreeObserver = null;
        }
    }

    private void registerListener(View anchor) {
        if (mSuperScrollListener != null) {
            ViewTreeObserver vto = (anchor.getWindowToken() != null) ? anchor.getViewTreeObserver() : null;
            if (vto != mViewTreeObserver) {
                if (mViewTreeObserver != null && mViewTreeObserver.isAlive()) {
                    mViewTreeObserver.removeOnScrollChangedListener(mSuperScrollListener);
                }
                if ((mViewTreeObserver = vto) != null) {
                    vto.addOnScrollChangedListener(mSuperScrollListener);
                }
            }
        }
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        try {
            super.showAsDropDown(anchor, xoff, yoff);
            registerListener(anchor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startAnimation() {
        if (animationEnabled) {
            if (windowAnimatorSet != null) {
                return;
            }
            ActionBarPopupWindowLayout content = (ActionBarPopupWindowLayout) getContentView();
            content.setTranslationY(0);
            content.setAlpha(1.0f);
            content.setPivotX(content.getMeasuredWidth());
            content.setPivotY(0);
            int count = content.getItemsCount();
            content.positions.clear();
            int visibleCount = 0;
            for (int a = 0; a < count; a++) {
                View child = content.getItemAt(a);
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                content.positions.put(child, visibleCount);
                child.setAlpha(0.0f);
                visibleCount++;
            }
            if (content.showedFromBotton) {
                content.lastStartedChild = count - 1;
            } else {
                content.lastStartedChild = 0;
            }
            windowAnimatorSet = new AnimatorSet();
            windowAnimatorSet.playTogether(
                    ObjectAnimator.ofFloat(content, "backScaleY", 0.0f, 1.0f),
                    ObjectAnimator.ofInt(content, "backAlpha", 0, 255));
            windowAnimatorSet.setDuration(150 + 16 * visibleCount);
            windowAnimatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    windowAnimatorSet = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            windowAnimatorSet.start();
        }
    }

    @Override
    public void update(View anchor, int xoff, int yoff, int width, int height) {
        super.update(anchor, xoff, yoff, width, height);
        registerListener(anchor);
    }

    @Override
    public void update(View anchor, int width, int height) {
        super.update(anchor, width, height);
        registerListener(anchor);
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        super.showAtLocation(parent, gravity, x, y);
        unregisterListener();
    }

    @Override
    public void dismiss() {
        dismiss(true);
    }

    public void dismiss(boolean animated) {
        setFocusable(false);
        if (animationEnabled && animated) {
            if (windowAnimatorSet != null) {
                windowAnimatorSet.cancel();
            }
            ActionBarPopupWindowLayout content = (ActionBarPopupWindowLayout) getContentView();
            windowAnimatorSet = new AnimatorSet();
            windowAnimatorSet.playTogether(
                    ObjectAnimator.ofFloat(content, "translationY", AndroidUtilities.dp(content.showedFromBotton ? 5 : -5)),
                    ObjectAnimator.ofFloat(content, "alpha", 0.0f));
            windowAnimatorSet.setDuration(150);
            windowAnimatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    windowAnimatorSet = null;
                    setFocusable(false);
                    try {
                        ActionBarPopupWindow.super.dismiss();
                    } catch (Exception e) {
                        //don't promt
                        e.getStackTrace();
                    }
                    unregisterListener();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            windowAnimatorSet.start();
        } else {
            try {
                super.dismiss();
            } catch (Exception e) {
                //don't promt
            }
            unregisterListener();
        }
    }
}
