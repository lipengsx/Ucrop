package com.catmall.oss.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import java.io.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2017/2/10.
 */

public class OssFileUtils {
    public static void savePhoto(final Context context, final Bitmap bmp, final SaveResultCallback saveResultCallback) {
        final File sdDir = getSDPath();
        if (sdDir == null) {
            //Toast.makeText(context,"设备自带的存储不可用", Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                File appDir = new File(sdDir, "out_photo");
                if (!appDir.exists()) {
                    appDir.mkdir();
                }
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");//设置以当前时间格式为图片名称
                String fileName = df.format(new Date()) + ".png";
                File file = new File(appDir, fileName);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                    saveResultCallback.onSavedSuccess();
                } catch (FileNotFoundException e) {
                    saveResultCallback.onSavedFailed();
                    e.printStackTrace();
                } catch (IOException e) {
                    saveResultCallback.onSavedFailed();
                    e.printStackTrace();
                }

                //保存图片后发送广播通知更新数据库
                Uri uri = Uri.fromFile(file);
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            }
        }).start();
    }


    public static File getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir;
    }

    public interface SaveResultCallback {
        void onSavedSuccess();

        void onSavedFailed();
    }

    public static byte[] getBitmapByte(Bitmap bitmap){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //参数1转换类型，参数2压缩质量，参数3字节流资源
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    //获取文件后缀，带.
    public static String getPicSuffix(String img_path){
        if (img_path == null || img_path.indexOf(".") == -1){
            return ""; //如果图片地址为null或者地址中没有"."就返回""    
        }
        int start = img_path.lastIndexOf(".");
        int length = img_path.length()-start-1;
        return img_path.substring(start).trim().toLowerCase();
    }

    public static String getMD5Checksum(InputStream inputStream) throws Exception {
        byte[] b = createChecksum(inputStream);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            //加0x100是因为有的b[i]的十六进制只有1位
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    private static byte[] createChecksum(InputStream inputStream) throws Exception {
        //将流类型字符串转换为String类型字符串
        byte[] buffer = new byte[1024];
        //如果想使用SHA-1或SHA-256，则传入SHA-1,SHA-256
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            //从文件读到buffer，最多装满buffer
            numRead = inputStream.read(buffer);
            if (numRead > 0) {
                //用读到的字节进行MD5的计算，第二个参数是偏移量
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        inputStream.close();
        return complete.digest();
    }


}
