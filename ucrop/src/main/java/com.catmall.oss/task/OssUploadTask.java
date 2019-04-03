package com.catmall.oss.task;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.alibaba.sdk.android.oss.*;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.catmall.oss.beans.OSSBean;
import com.catmall.oss.beans.OSSResultBean;
import com.catmall.oss.utils.OssFileUtils;
import com.catmall.oss.utils.PathUtils;
import com.yalantis.ucrop.util.FileUtils;

import static com.yalantis.ucrop.UCropFragment.TAG;

/**
 * Crops part of image that fills the crop bounds.
 * <p/>
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
public class OssUploadTask extends AsyncTask<Uri, Integer, OSSResultBean> {

    private OSSBean ossBean;
    private Context context;
    private String imagePath;
    private String fileName;

    private ClientConfiguration conf = null;

    public interface LoadDataListerner{
        public  void loadComplete();
    }


    private LoadDataListerner loadLisneter;
    public void setLoadDataComplete(LoadDataListerner dataComplete) {
        this.loadLisneter = dataComplete;
    }

    public OssUploadTask(OSSBean ossBean,Context context) {
        this.ossBean = ossBean;
        this.context = context;
    }
    @Override
    protected void onPreExecute() {
//        mDialog.show();
        Log.e(TAG, Thread.currentThread().getName() + " onPreExecute ");
    }

    @Override
    protected OSSResultBean doInBackground(Uri ... params) {

//        // 模拟数据的加载,耗时的任务
//        for (int i = 0; i < 100; i++) {
//            try {
//                Thread.sleep(80);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            publishProgress(i);
//        }
        OSSResultBean ossResultBean = UploadPic(params[0]);

        Log.e(TAG, Thread.currentThread().getName() + " doInBackground ");
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
//        mDialog.setProgress(values[0]);
        Log.e(TAG, Thread.currentThread().getName() + " onProgressUpdate ");
    }

    @Override
    protected void onPostExecute(OSSResultBean result) {
        // 进行数据加载完成后的UI操作
//        mDialog.dismiss();
        Log.e(TAG, Thread.currentThread().getName() + " onPostExecute ");
    }

    public OSSResultBean UploadPic(Uri imageUri){

        final OSSResultBean ossResultBean = new OSSResultBean();
        String md5 = null;
        try {
            md5 = OssFileUtils.getMD5Checksum(context.getContentResolver().openInputStream(imageUri));
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Log.e("md3",md5);

        String suffix = OssFileUtils.getPicSuffix(imageUri.toString());
        imagePath = md5.substring(0,2)+"/"+md5+suffix;
        fileName = PathUtils.getRealFilePath(context,imageUri);

        ossResultBean.setSuccess(0);
        ossResultBean.setFileName(fileName);

        conf = new ClientConfiguration();
        conf.setConnectionTimeout(5*60*1000);
        conf.setSocketTimeout(5*60*1000);
        conf.setMaxConcurrentRequest(5);
        conf.setMaxErrorRetry(2);

//        OSSLog.enableLog();
        OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(
                ossBean.getAccessKeyId(),ossBean.getAccessKeySecrect(),ossBean.getSecurityToken());

        final OSS oss = new OSSClient(context,ossBean.getOutEndPoint(),credentialProvider,conf);
//        ossClient.putObject("<yourBucketName>", "<yourObjectName>", new ByteArrayInputStream(content.getBytes()), meta);


        PutObjectRequest put = new PutObjectRequest(ossBean.getBucket(), imagePath, fileName);
        // 异步上传，可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });
        @SuppressWarnings("rawtypes")
        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                String st  = result.getServerCallbackReturnBody();
                Log.d("PutObject", "UploadSuccess");
                ossResultBean.setSuccess(1);
                loadLisneter.loadComplete();
            }
            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                ossResultBean.setSuccess(-1);
//                ossResultBean
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
//                    tv.setText("Uploadfile,servererror");
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
        return ossResultBean;
    }
}
