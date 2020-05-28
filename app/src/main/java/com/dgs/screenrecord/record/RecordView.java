package com.dgs.screenrecord.record;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.dgs.screenrecord.R;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 录屏按钮
 */
public class RecordView {
    private int statusBarHeight;
    private Context context;
    private TextView tv_time;
    private View recordView;
    private Handler handler;
    private Calendar calendar;
    private SimpleDateFormat sdf;
    private WindowManager.LayoutParams params;
    private WindowManager windowManager;
    private final int heightPixels;
    private View btnStop;

    public RecordView(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        initCalendar();

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        heightPixels = metrics.heightPixels;
    }

    public void setStatusBarHeight(int statusBarHeight) {
        this.statusBarHeight = statusBarHeight;
    }

    public void showRecordView() {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置效果为背景透明.
        params.format = PixelFormat.RGBA_8888;
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //设置窗口初始停靠位置.
        params.gravity = Gravity.LEFT | Gravity.BOTTOM;
        params.x = (int) context.getResources().getDimension(R.dimen.dip200);
        params.y = statusBarHeight;
        //设置悬浮窗口长宽数据.
        params.width = (int) context.getResources().getDimension(R.dimen.dip180);
        params.height = (int) context.getResources().getDimension(R.dimen.dip60);

        LayoutInflater inflater = LayoutInflater.from(context);
        recordView = inflater.inflate(R.layout.layout_record, null);
        windowManager.addView(recordView, params);
        btnStop = recordView.findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordListener != null) {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            windowManager.removeView(recordView);
                        }
                    });
                    recordListener.stop();
                }
            }
        });
        tv_time = recordView.findViewById(R.id.tv_time);
        tv_time.setText(sdf.format(calendar.getTime()));
        timeCounter();
        drag();
    }

    public View getBtnStop() {
        return btnStop;
    }

    private void drag() {
        recordView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                params.x = (int) event.getRawX() - params.width / 2;
                params.y = heightPixels - (int) event.getRawY() - statusBarHeight - (params.height / 2);
                windowManager.updateViewLayout(recordView, params);
                return false;
            }
        });
    }

    private void timeCounter() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long time = calendar.getTime().getTime() + 1000;
                Date date = new Date(time);
                calendar.setTime(date);
                String timeStr = sdf.format(date);
                tv_time.setText(timeStr);
                RecordView.this.timeCounter();
            }
        }, 1000L);
    }

    @NonNull
    private void initCalendar() {
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        sdf = new SimpleDateFormat("HH:mm:ss");
    }

    public void setRecordListener(IRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    private IRecordListener recordListener;

    public interface IRecordListener {
        void stop();
    }
}
