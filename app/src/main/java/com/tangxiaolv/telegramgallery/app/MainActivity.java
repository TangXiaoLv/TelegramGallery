
package com.tangxiaolv.telegramgallery.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import com.tangxiaolv.telegramgallery.GalleryActivity;
import com.tangxiaolv.telegramgallery.GalleryConfig;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<String> photos;
    private BaseAdapter adapter;
    private int reqCode = 12;

    @SuppressWarnings("all")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.btn);
        Button btn2 = (Button) findViewById(R.id.btn2);
        GridView gv = (GridView) findViewById(R.id.gv);
        gv.setAdapter(adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return photos == null ? 0 : photos.size();
            }

            @Override
            public Object getItem(int position) {
                if (photos == null) {
                    return null;
                }
                return photos.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView view = new ImageView(MainActivity.this);
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                view.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        256));
                String path = (String) getItem(position);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_4444;
                opts.inSampleSize = 4;
                Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
                view.setImageBitmap(bitmap);
                return view;
            }
        });

        assert btn != null;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GalleryConfig config = new GalleryConfig.Build()
                        .limitPickPhoto(11)
                        .singlePhoto(false)
                        .hintOfPick("this is pick hint")
                        .filterMimeTypes(new String[]{})
                        .build();
                GalleryActivity.openActivity(MainActivity.this, reqCode, config);
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GalleryConfig config = new GalleryConfig.Build()
                        .singlePhoto(true).build();
                GalleryActivity.openActivity(MainActivity.this, reqCode, config);
            }
        });
    }

    @SuppressWarnings("all")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (12 == requestCode && resultCode == Activity.RESULT_OK) {
            photos = (List<String>) data.getSerializableExtra(GalleryActivity.PHOTOS);
            adapter.notifyDataSetChanged();
        }
    }
}
