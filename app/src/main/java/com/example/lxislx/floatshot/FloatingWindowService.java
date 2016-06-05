package com.example.lxislx.floatshot;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * http://blog.csdn.net/shinay/article/details/7783276
 */
public class FloatingWindowService extends FloatingShot {
    public static final String OPERATION = "operation";
    public static final int OPERATION_SHOW = 100;
    public static final int OPERATION_HIDE = 101;

    private boolean isAdded = false; // 是否已增加悬浮窗
    private static WindowManager wm;
    private static WindowManager.LayoutParams params;
    private ImageView btn_floatView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createFloatView(); //准备好wm，做判断时wm直接add view即可
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        //OPERATION_SHOW
        int operation = intent.getIntExtra(OPERATION, OPERATION_SHOW);

        switch (operation) {
            case OPERATION_SHOW:
                /**
                 *  通过wm来添加view,启动，并通过OPERATION_SHOW或是OPERATION_HIDE来
                 * 作用到mHandler上，用isAdded的状态控制wm.addView(btn_floatView, params)
                 * wm.removeView(btn_floatView)。
                 */
                mHandler.removeMessages(OPERATION_SHOW);
                mHandler.removeMessages(OPERATION_HIDE);
                mHandler.sendEmptyMessage(OPERATION_SHOW);
                break;
            case OPERATION_HIDE:
//                mHandler.removeMessages(HANDLE_CHECK_ACTIVITY);
                mHandler.removeMessages(OPERATION_SHOW);
                mHandler.removeMessages(OPERATION_HIDE);
                mHandler.sendEmptyMessage(OPERATION_HIDE);
                break;
        }


    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case OPERATION_SHOW:
                    if (!isAdded) {
                        wm.addView(btn_floatView, params);
                        isAdded = true;
                    } else {
                        isAdded = true;
                    }
                    break;
                case OPERATION_HIDE://by myself
                    if (isAdded) {
                        wm.removeView(btn_floatView);
                        isAdded = false;
                    } else {
                        isAdded = false;
                    }
                    break;
            }
        }
    };

    /**
     * 创建悬浮窗
     * 设置Window flag：设置窗体焦点及触摸：FLAG_NOT_FOCUSABLE：wm的view不能获得按键输入焦点,
     * 让后面的view获得焦点，此flags属性的效果形同“锁定”:悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
     * These windows are normally placed above all applications, but behind the status bar.
     * TYPE_SYSTEM_OVERLAY Window type: system overlay windows, which need to be displayed on top of everything else.
     * http://www.dewen.net.cn/q/4159/View%E7%9A%84OnTouch%E5%92%8COnClick%E4%BA%8B%E4%BB%B6%E4%B8%8D%E8%83%BD%E5%B9%B6%E5%AD%98%E9%97%AE%E9%A2%98
     */
    private void createFloatView() {
        btn_floatView = new ImageView(getApplicationContext());
        btn_floatView.setImageResource(R.drawable.n_icon_bk_cicle);
        // 取得系统窗体
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE); //getApplicationContext().
        params = new WindowManager.LayoutParams();// 窗体的布局样式
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置window type： 设置窗体显示类型——TYPE_SYSTEM_ALERT(系统提示system window, such as low power alert. )
        //如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE，那么优先级会降低一些, 即拉下通知栏不可见。
        params.format = PixelFormat.RGBA_8888; // 设置设置显示的模式，图片格式，该效果为背景透明
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL//本区域内的事件自己处理，本区域外的事件底层Window处理
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; //不获取焦点，也不接收输入事件
        //右边的可以显示锁屏界面//| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        params.gravity = Gravity.TOP | Gravity.LEFT; // 设置对齐的方法
        params.x = 500;
        params.y = 500;//给个初始位置
        params.width = 150; // 设置悬浮窗的宽 WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = 150;// 设置悬浮窗的长 WindowManager.LayoutParams.WRAP_CONTENT;

        // 设置悬浮窗的Touch监听，跟随手指移动
        btn_floatView.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY;
            int paramX, paramY;
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX(); //getRawX、getRawY用于获取触摸点离屏幕左上角的距离
                        lastY = (int) event.getRawY();
                        paramX = params.x;
                        paramY = params.y;
//                        Log.i("点击1", "lastX:" + lastX + " lastY:" + lastY + " x:" + params.x + " y:" + params.y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        //x,y偏离200时回到手指上一次的位置
                        if (Math.abs(dy) > 200 && Math.abs(dx) > 200) {
                            params.x = paramX; //view的当前位置
                            params.y = paramY;
                            //此处可执行程序
                        } else {
                            params.x = paramX + dx; //view的当前位置
                            params.y = paramY + dy;
                        }
                        wm.updateViewLayout(btn_floatView, params);
                        break;
                }
                return false;  //ontouch如果返回true的话，是不会响应下面的onclick事件的
            }
        });
        btn_floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTakePhotoClicked(); //执行一下拍照
            }
        });

//        wm.addView(btn_floatView, params);
//        isAdded = true;
    }
}

