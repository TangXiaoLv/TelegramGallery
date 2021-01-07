package com.tangxiaolv.telegramgallery.components;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tangxiaolv.telegramgallery.R;
import com.tangxiaolv.telegramgallery.utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.utils.LayoutHelper;

public class PhotoPickerSearchCell extends LinearLayout {

    public interface PhotoPickerSearchCellDelegate {
        void didPressedSearchButton(int index);
    }

    private class SearchButton extends FrameLayout {

        private TextView textView1;
        private TextView textView2;
        private ImageView imageView;
        private View selector;

        public SearchButton(Context context) {
            super(context);

            setBackgroundColor(0xff1a1a1a);

            selector = new View(context);
            selector.setBackgroundResource(R.drawable.list_selector);
            addView(selector, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            addView(imageView, LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.TOP));

            textView1 = new TextView(context);
            textView1.setGravity(Gravity.CENTER_VERTICAL);
            textView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
//            textView1.getPaint().setFakeBoldText(true);
            textView1.setTextColor(0xffffffff);
            textView1.setSingleLine(true);
            textView1.setEllipsize(TextUtils.TruncateAt.END);
            addView(textView1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 51, 8, 4, 0));

            textView2 = new TextView(context);
            textView2.setGravity(Gravity.CENTER_VERTICAL);
            textView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
//            textView2.getPaint().setFakeBoldText(true);
            textView2.setTextColor(0xff666666);
            textView2.setSingleLine(true);
            textView2.setEllipsize(TextUtils.TruncateAt.END);
            addView(textView2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 51, 26, 4, 0));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (Build.VERSION.SDK_INT >= 21) {
                selector.drawableHotspotChanged(event.getX(), event.getY());
            }
            return super.onTouchEvent(event);
        }
    }

    private PhotoPickerSearchCellDelegate delegate;

    public PhotoPickerSearchCell(Context context, boolean allowGifs) {
        super(context);
        setOrientation(HORIZONTAL);

        SearchButton searchButton = new SearchButton(context);
        searchButton.textView1.setText(R.string.SearchImages);
        searchButton.textView2.setText(R.string.SearchImagesInfo);
        searchButton.imageView.setImageResource(R.drawable.search_web);
        addView(searchButton);
        LayoutParams layoutParams = (LayoutParams) searchButton.getLayoutParams();
        layoutParams.weight = 0.5f;
        layoutParams.topMargin = AndroidUtilities.dp(4);
        layoutParams.height = AndroidUtilities.dp(48);
        layoutParams.width = 0;
        searchButton.setLayoutParams(layoutParams);
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (delegate != null) {
                    delegate.didPressedSearchButton(0);
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(0);
        addView(frameLayout);
        layoutParams = (LayoutParams) frameLayout.getLayoutParams();
        layoutParams.topMargin = AndroidUtilities.dp(4);
        layoutParams.height = AndroidUtilities.dp(48);
        layoutParams.width = AndroidUtilities.dp(4);
        frameLayout.setLayoutParams(layoutParams);

        searchButton = new SearchButton(context);
        searchButton.textView1.setText(R.string.SearchGifs);
        searchButton.textView2.setText("GIPHY");
        searchButton.imageView.setImageResource(R.drawable.search_gif);
        addView(searchButton);
        layoutParams = (LayoutParams) searchButton.getLayoutParams();
        layoutParams.weight = 0.5f;
        layoutParams.topMargin = AndroidUtilities.dp(4);
        layoutParams.height = AndroidUtilities.dp(48);
        layoutParams.width = 0;
        searchButton.setLayoutParams(layoutParams);
        if (allowGifs) {
            searchButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (delegate != null) {
                        delegate.didPressedSearchButton(1);
                    }
                }
            });
        } else {
            searchButton.setAlpha(0.5f);
        }
    }

    public void setDelegate(PhotoPickerSearchCellDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(52), MeasureSpec.EXACTLY));
    }
}
