package com.example.ts.grallery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.ts.grallery.adapter.ImageFolderAdapter;
import com.example.ts.grallery.bean.ImageFolder;
import com.example.ts.grallery.imageLoader.GlideImageLoader;
import com.example.ts.grallery.utils.QueryImageUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ImageBaseActivity implements QueryImageUtil.OnImagesLoadedListener {

    private List<ImageFolder> mImageFolders = new ArrayList<>();
    private ImageFolderAdapter mImageFolderAdapter;
    private ListView mFloderList;
    ImagePicker mImagePicker;


    private static final int IMAGE_LOADE_FINISHED = 1;
    public static final int REQUEST_PERMISSION_STORAGE = 0x01;
    private static final String IMAGE_FLODER = "image_floder";

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IMAGE_LOADE_FINISHED:
                    mImageFolderAdapter.refreshData(mImageFolders);
                    mFloderList.setAdapter(mImageFolderAdapter);
                    mImagePicker.setImageFolders(mImageFolders);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //set the image loader
        mImagePicker = ImagePicker.getInstance();
        mImagePicker.setImageLoader(new GlideImageLoader());

        mFloderList = findViewById(R.id.floder_list);
        mFloderList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ImageFolder imageFolder = mImageFolders.get(i);
                Intent intent = new Intent(MainActivity.this, ImageGridActivity.class);
                intent.putExtra(IMAGE_FLODER, imageFolder);
                startActivity(intent);
            }
        });

        mImageFolderAdapter = new ImageFolderAdapter(this, null);

        if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new QueryImageUtil(this, null, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_STORAGE);
        }
    }

    @Override
    public void onImagesLoaded(List<ImageFolder> imageFolders) {
        mImageFolders = imageFolders;
        Message message = Message.obtain(mHandler);
        message.what = IMAGE_LOADE_FINISHED;
        message.sendToTarget();
    }
}
