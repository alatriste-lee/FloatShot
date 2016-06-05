package com.example.lxislx.floatshot;

import android.app.Service;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * http://www.68idc.cn/help/mobilesys/android/20151214598283.html
 */
public abstract class FloatingShot extends Service {
    public Camera camera = null;
    public Camera.Parameters parameters = null;
    public boolean cameraONorOFF = true;
    Camera.Size size = null;

    public void onTakePhotoClicked() {
        final SurfaceView preview = new SurfaceView(this); //初始化surfaceview
        SurfaceHolder holder = preview.getHolder(); //初始化surfaceholder
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            //The preview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if(camera != null){
//                        camera.stopPreview();
                        cameraONorOFF = false;
                        camera.release();//手动释放 一定得加！
                        camera = null;
                    }
                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    try {
                        //初始化Camera对象，并调用该对象的setPreviewDisplay函数设置SurfaceHolder对象（这里为myHolder）
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    doShot();
                } catch (Exception e) {
                    if (camera != null)
                        camera.release();//手动释放
                    camera = null;
                    throw new RuntimeException(e);
                }
            }

            private void doShot() {
                //先对相机界面是否启用做下判断
                if(cameraONorOFF){
                    camera.stopPreview();
                }
                if(camera != null){
                    parameters = camera.getParameters();
                    List<Camera.Size> list = parameters.getSupportedPictureSizes();
                    getMaxSizeOfCamera(list); //小米2s是0最大，魅族mx5是最后的最大
                    parameters.setPictureSize(size.width, size.height);
                    parameters.setRotation(90);
                    camera.setParameters(parameters);
                    camera.startPreview();//相机的界面打开了
                    cameraONorOFF = true;
                    camera.autoFocus(autoFocus);//自动对焦
                    camera.takePicture(null, null, pictureCallback);
                    Log.d("takeoverpicbefore", "take pic over"+"width "+size.width+"height "+size.height);
                }
            }

            private void getMaxSizeOfCamera(List<Camera.Size> list) {
                if(list.get(list.toArray().length-1).width > list.get(0).width){
                    size = list.get(list.toArray().length-1);
                }
                else {
                    size = list.get(0);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                camera.stopPreview();
                cameraONorOFF = false;
                camera.release();//手动释放
                camera = null;
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
            //自动对焦回调函数(空实现)
            private Camera.AutoFocusCallback autoFocus = new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                }
            };
        });
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                PixelFormat.UNKNOWN);
        //Don't set the preview visibility to GONE or INVISIBLE
        wm.addView(preview, params);
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                saveToFile(data, camera);
                if(camera != null){
                    camera.stopPreview();
                    cameraONorOFF = false;
                    camera.release();//手动释放
                }
            } catch (Exception e) {
                Log.i("exception", e.toString());
            }
        }
    };

    public static void saveToFile(byte[] data, Camera camera) {
        if (null == data) {
            return;
        }
        File fileFolder = new File(Environment.getExternalStorageDirectory() + "/_FloatShot/");
        if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"_FloatShot"的目录
            fileFolder.mkdir();
        }
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        File jpgFile = new File(fileFolder, filename);
        try {
            FileOutputStream outputStreamOriginal = new FileOutputStream(jpgFile);
            outputStreamOriginal.write(data); // 写入sd卡中
            outputStreamOriginal.close(); // 关闭输出流
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void onPictureTaken(byte[] data, Camera camera) {
//        Log.d(TAG, "onPictureTaken");
//        if(null == data){
//            return;
//        }
//        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//        camera.stopPreview();
//        Matrix matrix = new Matrix();
//        matrix.postRotate((float) 90.0);
//        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
//                bitmap.getHeight(), matrix, false);
//        Log.d(TAG, "original bitmap width: " + bitmap.getWidth() +
//                " height: " + bitmap.getHeight());
//        Bitmap sizeBitmap = Bitmap.createScaledBitmap(bitmap,
//                bitmap.getWidth()/3, bitmap.getHeight()/3, true);
//        Log.d(TAG,"size bitmap width "+sizeBitmap.getWidth()+" height "+sizeBitmap.getHeight());
//
//        //裁剪bitmap
//        int leftOffset = (int)(sizeBitmap.getWidth() * 0.25);
//        int topOffset = (int)(sizeBitmap.getHeight() * 0.25);
//        Rect rect = new Rect(leftOffset, topOffset, sizeBitmap.getWidth() - leftOffset,
//                sizeBitmap.getHeight() - topOffset);
//        Bitmap rectBitmap = Bitmap.createBitmap(sizeBitmap,
//                rect.left, rect.top, rect.width(), rect.height());
//        try {
//            FileOutputStream outputStream = new FileOutputStream(Environment
//                    .getExternalStorageDirectory().toString()+"/photoResize.jpg");
//            sizeBitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream);
//            outputStream.close();
//            FileOutputStream outputStreamOriginal = new FileOutputStream(Environment
//                    .getExternalStorageDirectory().toString()+"/photoOriginal.jpg");
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, outputStreamOriginal);
//            outputStreamOriginal.close();
//
//            FileOutputStream outputStreamCut = new FileOutputStream(Environment
//                    .getExternalStorageDirectory().toString()+"/photoCut.jpg");
//            rectBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStreamCut);
//            outputStreamCut.close();
//            Log.d(TAG,"picture saved!");
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
//    }
}

