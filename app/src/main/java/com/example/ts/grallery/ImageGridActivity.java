package com.example.ts.grallery;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ts.grallery.adapter.ImageRecyclerAdapter;
import com.example.ts.grallery.adapter.ImageRecyclerAdapter.OnImageItemClickListener;
import com.example.ts.grallery.adapter.ImageRecyclerAdapter.OnImageItemLongClickListener;
import com.example.ts.grallery.bean.Image;
import com.example.ts.grallery.bean.ImageFolder;
import com.example.ts.grallery.utils.Utils;
import com.example.ts.grallery.view.GridSpacingItemDecoration;

public class ImageGridActivity extends ImageBaseActivity implements
        View.OnClickListener, OnImageItemLongClickListener, OnImageItemClickListener {

    public static final int REQUEST_PERMISSION_STORAGE = 0x01;
    public static final int REQUEST_PERMISSION_CAMERA = 0x02;
    public static final String EXTRAS_TAKE_PICKERS = "TAKE";
    public static final String EXTRAS_IMAGES = "IMAGES";
    private static final String IMAGE_FLODER = "image_floder";

    private boolean isOrigin = false;  //是否选中原图
    private ImagePicker imagePicker;

    private ImageFolder mImageFolder;   //所有的图片文件夹
    private boolean directPhoto = false; // 默认不是直接调取相机
    private RecyclerView mRecyclerView;
    private ImageRecyclerAdapter mRecyclerAdapter;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        directPhoto = savedInstanceState.getBoolean(EXTRAS_TAKE_PICKERS, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRAS_TAKE_PICKERS, directPhoto);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_grid);

        imagePicker = ImagePicker.getInstance();
        Intent fromMain = getIntent();
        mImageFolder = (ImageFolder) fromMain.getSerializableExtra(IMAGE_FLODER);
        imagePicker.setSelectedImages(mImageFolder.images);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);

        TextView tv_des = findViewById(R.id.tv_des);
        tv_des.setText(mImageFolder.name);

        Button bt_ok = findViewById(R.id.btn_ok);
        bt_ok.setVisibility(View.GONE);

        mRecyclerAdapter = new ImageRecyclerAdapter(this, mImageFolder.images);
        mRecyclerAdapter.setOnImageItemClickListener(this);
        mRecyclerAdapter.setOnImageItemLongClickListener(this);

        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        mRecyclerView.addItemDecoration(
                new GridSpacingItemDecoration(3, Utils.dp2px(this, 2), false));
        mRecyclerView.setAdapter(mRecyclerAdapter);

        ImageView bt_back = findViewById(R.id.btn_back);
        bt_back.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        mRecyclerAdapter.refreshData(imagePicker.getSelectedImages());
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                showToast("权限被禁止，无法选择本地图片");
            }
        } else if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                imagePicker.takePicture(this, ImagePicker.REQUEST_CODE_TAKE);
            } else {
                showToast("权限被禁止，无法打开相机");
            }
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.btn_back:
                //点击返回按钮
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onImageItemClick(View view, Image image, int position) {
//根据是否有相机按钮确定位置
        Log.d("ImageGrid", "start to preview");
        position = imagePicker.isShowCamera() ? position - 1 : position;
        Intent intent = new Intent(ImageGridActivity.this, ImagePreviewActivity.class);
        intent.putExtra(ImagePicker.EXTRA_SELECTED_IMAGE_POSITION, position);
        /**
         * 依然采用弱引用进行解决，采用单例加锁方式处理
         */

        // 据说这样会导致大量图片的时候崩溃
//            intent.putExtra(ImagePicker.EXTRA_IMAGE_ITEMS, imagePicker.getCurrentImageFolderItems());

        // 但采用弱引用会导致预览弱引用直接返回空指针
        DataHolder.getInstance().save(DataHolder.DH_CURRENT_IMAGE_FOLDER_ITEMS, mImageFolder.images);
        intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
        startActivityForResult(intent, ImagePicker.REQUEST_CODE_PREVIEW);  //点击图片进入预览界面
    }

    @Override
    public void onImageItemLongClick(View view, final Image image, final int position) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("删除");
        alertDialog.setMessage("确认删除该图片？");
        alertDialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mImageFolder.images.remove(position);
                mRecyclerAdapter.notifyDataSetChanged();
                String id = String.valueOf(image.getId());
                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        "_id = ?", new String[]{id});
                dialogInterface.dismiss();
            }
        });
        alertDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }
}
