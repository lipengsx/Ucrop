package com.catmall.oss.service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.alibaba.sdk.android.oss.*;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.catmall.oss.beans.OSSBean;
import com.catmall.oss.beans.OSSResultBean;
import com.catmall.oss.task.OssUploadTask;
import com.catmall.oss.utils.OssFileUtils;
import com.catmall.oss.utils.PathUtils;

public class OssService {

    private OSS oss;
//    private String accessKeyId;
//    private String bucketName;
//    private String accessKeySecret;
//    private String endpoint;
    private Context context;
    private String imagePath;
    private String fileName;

    private OSSResultBean ossResultBean;
    private OSSBean ossBean;
    private ProgressCallback progressCallback;


    public interface OssUploadDataListerner{
        public void uploadComplete(OSSResultBean ossResultBean);
        public void uploadFailed(OSSResultBean ossResultBean);
    }
    public OssUploadDataListerner loadLisneter;

    public void setLoadDataComplete(OssUploadDataListerner dataComplete) {
        this.loadLisneter = dataComplete;
    }

    public OssService(Context context, OSSBean ossBean) {
        this.context = context;
        this.ossBean = ossBean;
        initOSSClient();
    }

//    public OssService(Context context, String accessKeyId, String accessKeySecret, String endpoint, String bucketName) {
//        this.context = context;
//        this.endpoint = endpoint;
//        this.bucketName = bucketName;
//        this.accessKeyId = accessKeyId;
//        this.accessKeySecret = accessKeySecret;
//        initOSSClient();
//    }


    public void initOSSClient() {
        //OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider("<StsToken.AccessKeyId>", "<StsToken.SecretKeyId>", "<StsToken.SecurityToken>");
        //这个初始化安全性没有Sts安全，如需要很高安全性建议用OSSStsTokenCredentialProvider创建（上一行创建方式）多出的参数SecurityToken为临时授权参数
//        OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);

        OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(
                ossBean.getAccessKeyId(),ossBean.getAccessKeySecrect(),ossBean.getSecurityToken());
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(8); // 最大并发请求数，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次

        // oss为全局变量，endpoint是一个OSS区域地址
        oss = new OSSClient(context, ossBean.getOutEndPoint(), credentialProvider, conf);
    }

    public void uploadUri(Context context, Uri imageUri) {

        String md5 = null;
        try {
            md5 = OssFileUtils.getMD5Checksum(context.getContentResolver().openInputStream(imageUri));
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Log.e("md3",md5);

        String suffix = OssFileUtils.getPicSuffix(imageUri.toString());
        fileName = md5.substring(0,2)+"/"+md5+suffix;
        imagePath = PathUtils.getRealFilePath(context,imageUri);
        uploadString(context,fileName,imagePath);
    }

    public void uploadString(Context context, String filename, String path) {

        Log.e("upload pic Progress,请选择图片",filename);
        Log.e("upload pic Progress,请选择图片",path);

        ossResultBean = null;
        ossResultBean = new OSSResultBean();
        ossResultBean.setSuccess(0);
        ossResultBean.setFileName(fileName);

        //通过填写文件名形成objectname,通过这个名字指定上传和下载的文件
        String objectname = filename;
        if (objectname == null || objectname.equals("")) {
//            ToastUtils.showShort("文件名不能为空");
            return;
        }
        //下面3个参数依次为bucket名，Object名，上传文件路径
        PutObjectRequest put = new PutObjectRequest(ossBean.getBucket(), objectname, path);
        if (path == null || path.equals("")) {
//            LogUtil.d("请选择图片....");
            Log.e("upload pic Progress","请选择图片....");
            //ToastUtils.showShort("请选择图片....");
            return;
        }
//        LogUtil.d("正在上传中....");
        //ToastUtils.showShort("正在上传中....");
        // 异步上传，可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.e("upload pic Progress","currentSize: " + currentSize + " totalSize: " + totalSize);
                double progress = currentSize * 1.0 / totalSize * 100.f;

                if (progressCallback != null) {
                    progressCallback.onProgressCallback(progress);
                }
            }
        });
        @SuppressWarnings("rawtypes")
        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.e("upload pic Progress","upload Success ");
                ossResultBean.setSuccess(1);
                loadLisneter.uploadComplete(ossResultBean);
                //ToastUtils.showShort("上传成功");
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                ossResultBean.setSuccess(-1);
                ossResultBean.setErrorMsg("表示向OSS发送请求或解析来自OSS的响应时发生错误");
                loadLisneter.uploadFailed(ossResultBean);
                Log.e("UploadFailure","UploadFailure");
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    Log.e("UploadFailure","UploadFailure：表示向OSS发送请求或解析来自OSS的响应时发生错误。\n" +
                            "  *例如，当网络不可用时，这个异常将被抛出");
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("UploadFailure","UploadFailure：表示在OSS服务端发生错误");
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
        //task.cancel(); // 可以取消任务
        //task.waitUntilFinished(); // 可以等待直到任务完成
    }


    public ProgressCallback getProgressCallback() {
        return progressCallback;
    }

    public void setProgressCallback(ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }

    public interface ProgressCallback {
        void onProgressCallback(double progress);
    }

}
