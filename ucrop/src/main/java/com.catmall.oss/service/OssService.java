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
    private Context context;
    private String imagePath;
    private String fileName;

    private OSSResultBean ossResultBean;
    private OSSBean ossUploadBean;
    private ProgressCallback progressCallback;

    public interface OssUploadDataListerner{
        void uploadComplete(OSSResultBean ossResultBean);
        void uploadFailed(OSSResultBean ossResultBean);
    }
    public OssUploadDataListerner loadLisneter;

    public void setLoadDataComplete(OssUploadDataListerner dataComplete) {
        this.loadLisneter = dataComplete;
    }

    public OssService(Context context, OSSBean ossBean) {
        this.context = context;
        this.ossUploadBean = ossBean;
        initOSSClient(ossBean);
    }

    public void initOSSClient(OSSBean ossBean) {
        try {
            OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(
                    ossBean.getAccessKeyId(), ossBean.getAccessKeySecrect(), ossBean.getSecurityToken());

            ClientConfiguration conf = new ClientConfiguration();
            conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
            conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
            conf.setMaxConcurrentRequest(8); // 最大并发请求数，默认5个
            conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次

            // oss为全局变量，endpoint是一个OSS区域地址
            oss = new OSSClient(context, ossBean.getOutEndPoint(), credentialProvider, conf);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void uploadUri(Context context, Uri imageUri) {

        String md5 = null;
        try {
            md5 = OssFileUtils.getMD5Checksum(context.getContentResolver().openInputStream(imageUri));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String suffix = OssFileUtils.getPicSuffix(imageUri.toString());
        fileName = md5.substring(0,2)+"/"+md5+suffix;
        imagePath = PathUtils.getRealFilePath(context,imageUri);
        uploadString(context,fileName,imagePath);
    }

    public void uploadString(Context context, String filename, String path) {

        Log.e("upload pic Progress,filename:",filename);
        Log.e("upload pic Progress,path:",path);

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
        PutObjectRequest put = new PutObjectRequest(ossUploadBean.getBucket(), objectname, path);
        if (path == null || path.equals("")) {
            Log.e("upload pic Progress","请选择图片....");
//            //ToastUtils.showShort("请选择图片....");
            return;
        }
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
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常

                Log.e("UploadFailure","UploadFailure");
                String msg = "Failure info:";
                if (clientExcepion != null) {
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    msg += "\"ErrorCode:\"+serviceException.getErrorCode()+\"   RequestId:\"+serviceException.getRequestId()+\"    HostId:\"+serviceException.getHostId()+\"    RawMessage:\"+serviceException.getRawMessage()";
                    Log.e("uploadFailure",msg);
                }

                ossResultBean.setSuccess(-1);
                ossResultBean.setErrorMsg(msg);
                loadLisneter.uploadFailed(ossResultBean);
            }
        });
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
