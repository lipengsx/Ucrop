package com.yalantis.ucrop.sample;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import com.catmall.oss.beans.OSSBean;
import com.catmall.oss.beans.OSSResultBean;
import com.catmall.oss.service.OssService;
import com.catmall.oss.task.OssUploadTask;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    final public static int REQUEST_CODE_ALBUM = 200;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_ALBUM);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            if (data != null) {
                Uri cropuri = UCrop.getOutput(data);
                try {
                    OSSBean ossBean = new OSSBean();


                    OssService ossService = new OssService(this, ossBean);
                    ossService.uploadUri(this, cropuri);
//                OssUploadTask ossUploadTask = new OssUploadTask(ossBean,this);
                    ossService.setLoadDataComplete(
                            new OssService.OssUploadDataListerner() {
                                @Override
                                public void uploadComplete(OSSResultBean ossResultBean) {
                                    Log.e("success", ossResultBean.toString());
                                }

                                @Override
                                public void uploadFailed(OSSResultBean ossResultBean) {
                                    Log.e("error", ossResultBean.toString()]);
                                }
                            }
                    );
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        switch (requestCode) {
            case REQUEST_CODE_ALBUM:
                if (resultCode == RESULT_OK) {
                    cropPhoto(data.getData());//裁剪图片
                }

                break;
//            case REQUEST_CODE_CAMERA:
//                if(resultCode == RESULT_OK) {
//                    File file = new File(path + "head.jpg");
//                    if (!file.getParentFile().exists()) {
//                        file.getParentFile().mkdirs();
//                    }
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        imageUri = Uri.fromFile(new File(path + "head.jpg"));
//                        cropPhoto(imageUri);
//                    } else {
//                        cropPhoto(Uri.fromFile(file));//裁剪图片
//                    }
//                }
//                break;
            default:
                break;

        }
    }

    private static String path = "/sdcard/ucrop/";//sd路径

    public void cropPhoto(Uri uri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "avatar.jpg"));
        UCrop.Options options = new UCrop.Options();

        //修改标题栏颜色
        options.setToolbarColor(Color.rgb(0,0,0));
        //修改状态栏颜色
        options.setStatusBarColor(Color.rgb(33,33,33));
        // 隐藏底部工具
        options.setHideBottomControls(true);
        //设置源uri和目标uri、长宽比例、图片大小、配置参数
        UCrop.of(uri, destinationUri).withAspectRatio(1, 1).withMaxResultSize(300, 300).withOptions(options).start(this);
    }
}
