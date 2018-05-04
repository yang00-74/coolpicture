package com.example.ts.grallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.example.ts.grallery.bean.Image;
import com.example.ts.grallery.bean.ImageFolder;
import com.example.ts.grallery.utils.BitmapUtil;
import com.example.ts.grallery.utils.QueryImageUtil;
import com.example.ts.grallery.view.CropImageView;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class ImageCropActivity extends ImageBaseActivity
        implements View.OnClickListener, CropImageView.OnBitmapSaveCompleteListener {

    private CropImageView mCropImageView;
    private Bitmap mBitmap;
    private boolean mIsSaveRectangle;
    private int mOutputX;
    private int mOutputY;
    private ArrayList<Image> mImages;
    private ImagePicker imagePicker;
    private Image mImage;
    private static final String EDITED_IMAGE = "edited_image";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        imagePicker = ImagePicker.getInstance();

        //初始化View
        findViewById(R.id.btn_back).setOnClickListener(this);
        TextView tv_des = (TextView) findViewById(R.id.tv_des);
        tv_des.setText(getString(R.string.ip_photo_crop));

        Button btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_ok.setVisibility(View.VISIBLE);
        btn_ok.setText(getString(R.string.ip_complete));
        btn_ok.setOnClickListener(this);

        mCropImageView = (CropImageView) findViewById(R.id.cv_crop_image);
        mCropImageView.setOnBitmapSaveCompleteListener(this);

        //获取需要的参数
        mOutputX = imagePicker.getOutPutX();
        mOutputY = imagePicker.getOutPutY();
        mIsSaveRectangle = imagePicker.isSaveRectangle();
        Intent intent = getIntent();
        if (intent.hasExtra(EDITED_IMAGE)) {
            mImage = (com.example.ts.grallery.bean.Image) intent.getSerializableExtra(EDITED_IMAGE);
        } else {
            mImages = imagePicker.getSelectedImages();
            mImage = mImages.get(0);
        }
        String imagePath;
        if (null != mImage) {
            imagePath = mImage.getPath();
        } else {
            return;
        }

        mCropImageView.setFocusStyle(imagePicker.getStyle());
        mCropImageView.setFocusWidth(imagePicker.getFocusWidth());
        mCropImageView.setFocusHeight(imagePicker.getFocusHeight());

        //缩放图片
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        options.inSampleSize = calculateInSampleSize(options, displayMetrics.widthPixels,
                displayMetrics.heightPixels);
        options.inJustDecodeBounds = false;
        mBitmap = BitmapFactory.decodeFile(imagePath, options);
        //设置默认旋转角度
        mCropImageView.setImageBitmap(mCropImageView.rotate(mBitmap,
                BitmapUtil.getBitmapDegree(imagePath)));
//        mCropImageView.setImageURI(Uri.fromFile(new File(imagePath)));
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = width / reqWidth;
            } else {
                inSampleSize = height / reqHeight;
            }
        }
        return inSampleSize;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_back) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (id == R.id.btn_ok) {
            File image = new File(mImage.getPath());
            String parentFloder = image.getParent();
            File floder = new File(parentFloder);
            mCropImageView.saveBitmapToFile(floder, mOutputX, mOutputY, mIsSaveRectangle);
        }
    }

    @Override
    public void onBitmapSaveSuccess(File file) {
//        Toast.makeText(ImageCropActivity.this, "裁剪成功:" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        //裁剪后替换掉返回数据的内容，但是不要改变全局中的选中数据
        Image image = new Image();
        String path = file.getAbsolutePath();
        Log.d("ImageCrop",path);
        image.setPath(path);
        image.setName(path.substring(0,path.length()-1));

        Intent intent = new Intent();
        intent.putExtra("new_image", (Serializable) image);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBitmapSaveError(File file) {
        Log.d("ImageCrop","save error");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCropImageView.setOnBitmapSaveCompleteListener(null);
        if (null != mBitmap && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        new QueryImageUtil(this,null,new QueryImageUtil.OnImagesLoadedListener(){

            @Override
            public void onImagesLoaded(List<ImageFolder> imageFolders) {
                ImagePicker imagePicker = ImagePicker.getInstance();
                imagePicker.setImageFolders(imageFolders);
            }
        });
    }
}
