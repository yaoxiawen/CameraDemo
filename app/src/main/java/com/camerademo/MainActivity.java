package com.camerademo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView mTextView;
    private ImageView mCameraImageview;
    private final int RESULT_CAMERA = 22;
    private final int RESULT_PERMISSION = 23;
    //是否使用FileProvider开关
    private boolean mUseProvider = true;
    //是否使用EXTRA_OUTPUT参数开关
    private boolean mUseOutput = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!mUseProvider && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        mTextView = findViewById(R.id.click_camera);
        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCamera();
            }
        });
        mCameraImageview = findViewById(R.id.image_view);
    }

    // 请求权限回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RESULT_PERMISSION:
                // 1002请求码对应的是申请多个权限
                if (grantResults.length > 0) {
                    // 因为是多个权限，所以需要一个循环获取每个权限的获取情况
                    for (int i = 0; i < grantResults.length; i++) {
                        // PERMISSION_DENIED 这个值代表是没有授权，我们可以把被拒绝授权的权限显示出来
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(MainActivity.this, permissions[i] + "权限被拒绝了",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }

    private void clickCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android系统6.0之后动态申请权限
            List<String> permissionList = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA);
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (!permissionList.isEmpty()) {
                //需要<uses-permission android:name="android.permission.READ_PHONE_STATE"/>才能出权限获取的弹窗
                requestPermissions(permissionList.toArray(new String[permissionList.size()]),
                        RESULT_PERMISSION);
                return;
            }
        }
        //利用 Intent 调用系统指定的相机拍照
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        //通过Intent的resolveActivity方法，并想该方法传入包管理器可以对包管理器进行查询以确定是否有Activity能够启动该Intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Uri contentUri;
            File photoFile = createImageFile();
            //从Android 7.0开始，一个应用提供自身文件给其它应用使用时，
            //如果给出一个file://格式的URI的话，应用会抛出FileUriExposedException。
            //这是由于谷歌认为目标app可能不具有文件权限，会造成潜在的问题
            if (mUseProvider && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentUri =
                        FileProvider.getUriForFile(this, "com.camerademo.fileprovider", photoFile);
            } else {
                contentUri = Uri.fromFile(photoFile);
            }
            if (mUseOutput) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            }
            if (mUseProvider && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //给目标应用一个临时的授权
                takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }
        //跳转界面传回拍照所得数据
        startActivityForResult(takePictureIntent, RESULT_CAMERA);
    }

    /**
     * 判断系统中是否存在可以启动的相机应用
     *
     * @return 存在返回true，不存在返回false
     */
    public boolean hasCamera() {
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private File createImageFile() {
        File imagePath = new File(getExternalFilesDir(""), "images");
        if (!imagePath.exists()) {
            imagePath.mkdirs();
        }

        File image = new File(imagePath, generateFileName() + ".jpg");
        if (!image.exists()) {
            try {
                image.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public static String generateFileName() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_OK) {
            if (requestCode == RESULT_CAMERA && data != null) {
                if (!mUseOutput) {
                    //没有指定Intent里面的EXTRA_OUTPUT参数
                    //获得很小的预览图，用于设置头像等地方。
                    Bitmap bitmap;
                    try {
                        //"data"这个居然没用常量定义,也是醉了,我们可以发现它直接把bitmap序列化到intent里面了。
                        bitmap = data.getExtras().getParcelable("data");
                        //TODO:do something with bitmap, Do NOT forget call Bitmap.recycler();
                        mCameraImageview.setImageBitmap(bitmap);
                    } catch (ClassCastException e) {
                        //do something with exceptions
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
