package com.example.ts.grallery;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.example.ts.grallery.bean.Image;
import com.example.ts.grallery.utils.NavigationBarChangeListener;
import com.example.ts.grallery.utils.Utils;

import java.io.File;
import java.io.Serializable;

/**
 */
public class ImagePreviewActivity extends ImagePreviewBaseActivity implements View.OnClickListener {

    public static final String ISORIGIN = "isOrigin";
    private static final String EDITED_IMAGE = "edited_image";
    private static final int RESULT_CODE = 11;

    private boolean isOrigin;                      //是否选中原图
    private View bottomBar;
    private View marginView;

    private Button edit_bt;
    private String mCurrentFloderPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isOrigin = getIntent().getBooleanExtra(ImagePreviewActivity.ISORIGIN, false);
        bottomBar = findViewById(R.id.bottom_bar);
        bottomBar.setVisibility(View.VISIBLE);

        marginView = findViewById(R.id.margin_bottom);

        edit_bt = findViewById(R.id.preview_edit);
        edit_bt.setOnClickListener(this);

        Button del_bt = findViewById(R.id.preview_del);
        del_bt.setOnClickListener(this);

        //初始化当前页面的状态
        Image item = mImages.get(mCurrentPosition);
        boolean isSelected = imagePicker.isSelect(item);
        mTitleCount.setText(getString(R.string.ip_preview_image_count, mCurrentPosition + 1, mImages.size()));
        //滑动ViewPager的时候，根据外界的数据改变当前的选中状态和当前的图片的位置描述文本
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                Image item = mImages.get(mCurrentPosition);
                mCurrentFloderPath = new File(item.getPath()).getParentFile().getAbsolutePath();
                boolean isSelected = imagePicker.isSelect(item);
                mTitleCount.setText(getString(R.string.ip_preview_image_count, mCurrentPosition + 1, mImages.size()));
            }
        });

        NavigationBarChangeListener.with(this).setListener(new NavigationBarChangeListener.OnSoftInputStateChangeListener() {
            @Override
            public void onNavigationBarShow(int orientation, int height) {
                marginView.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams layoutParams = marginView.getLayoutParams();
                if (layoutParams.height == 0) {
                    layoutParams.height = Utils.getNavigationBarHeight(ImagePreviewActivity.this);
                    marginView.requestLayout();
                }
            }

            @Override
            public void onNavigationBarHide(int orientation) {
                marginView.setVisibility(View.GONE);
            }
        });
        NavigationBarChangeListener.with(this, NavigationBarChangeListener.ORIENTATION_HORIZONTAL)
                .setListener(new NavigationBarChangeListener.OnSoftInputStateChangeListener() {
                    @Override
                    public void onNavigationBarShow(int orientation, int height) {
                        topBar.setPadding(0, 0, height, 0);
                        bottomBar.setPadding(0, 0, height, 0);
                    }

                    @Override
                    public void onNavigationBarHide(int orientation) {
                        topBar.setPadding(0, 0, 0, 0);
                        bottomBar.setPadding(0, 0, 0, 0);
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                Intent intent = new Intent();
                intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
                setResult(ImagePicker.RESULT_CODE_BACK, intent);
                finish();
                break;
            case R.id.preview_edit:
                Intent intent1 = new Intent(this, ImageCropActivity.class);
                final Image currentImage = mImages.get(mCurrentPosition);
                intent1.putExtra(EDITED_IMAGE, (Serializable) currentImage);
                startActivityForResult(intent1, RESULT_CODE);
                break;
            case R.id.preview_del:
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setTitle("删除");
                alertDialog.setMessage("确认删除该图片？");
                alertDialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Image image = mImages.remove(mCurrentPosition);
                        mAdapter.setData(mImages);
                        mAdapter.notifyDataSetChanged();
                        mTitleCount.setText(getString(R.string.ip_preview_image_count, mCurrentPosition + 1, mImages.size()));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    Image image = (Image) data.getSerializableExtra("new_image");
                    Log.d("ImagePreview", image.getPath());
                    mImages.add(mImages.size(), image);
                    mAdapter.setData(mImages);
                    mAdapter.notifyDataSetChanged();

                    imagePicker.setSelectedImages(mImages);
                }
                break;
            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(ImagePreviewActivity.ISORIGIN, isOrigin);
        setResult(ImagePicker.RESULT_CODE_BACK, intent);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 单击时，隐藏头和尾
     */
    @Override
    public void onImageSingleTap() {
        if (topBar.getVisibility() == View.VISIBLE) {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.top_out));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            tintManager.setStatusBarTintResource(Color.TRANSPARENT);//通知栏所需颜色
            //给最外层布局加上这个属性表示，Activity全屏显示，且状态栏被隐藏覆盖掉。
        } else {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.top_in));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            tintManager.setStatusBarTintResource(R.color.ip_color_primary_dark);//通知栏所需颜色
            //Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态遮住
        }
    }
}
