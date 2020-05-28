package com.dgs.screenrecord.record;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * dgs
 *
 * @author Administrator
 */
public class ScreenRecorderHelper {


    private static final int RECORD_REQUEST_CODE = 101;
    private static final int STORAGE_REQUEST_CODE = 102;
    private static final int AUDIO_REQUEST_CODE = 103;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private RecordService recordService;
    private static volatile ScreenRecorderHelper mRecorder;
    Context mcontext;

    private ScreenRecorderHelper(Context context) {
        mcontext = context;
    }

    //stop record
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopRecord(OnRecordStatusChangeListener listener) {
        if (recordService.stopRecord()) {
            //stop record success
            listener.onChangeSuccess();
        } else {
            //stop record failed
            listener.onChangeFailed();
        }
    }


    //application context is better
    public static ScreenRecorderHelper getInstance(Context context) {
        if (mRecorder == null)
            synchronized (ScreenRecorderHelper.class) {
                if (mRecorder == null) {
                    mRecorder = new ScreenRecorderHelper(context);
                }
            }
        context.startService(new Intent(context, RecordService.class));
        return mRecorder;
    }


    //default record setting
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startRecord(final Activity activity) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) mcontext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (recordService.isRunning()) {
            recordService.stopRecord();
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
            } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
            } else {
                Intent captureIntent = projectionManager.createScreenCaptureIntent();
                activity.startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
            }
        }
    }


    public interface OnRecordStatusChangeListener {
        //operate success
        void onChangeSuccess();

        //operate error
        void onChangeFailed();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data, OnRecordStatusChangeListener listener) {
        switch (requestCode) {
            //屏幕显示
            case RECORD_REQUEST_CODE:
                if (resultCode == activity.RESULT_OK) {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                    recordService.setMediaProject(mediaProjection);
                    boolean b = recordService.startRecord();
                    if (b) {
                        listener.onChangeSuccess();
                    } else {
                        listener.onChangeFailed();
                    }
                } else {
                    Toast.makeText(activity, "请允许截取屏幕权限后，使用录屏功能", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    public void onRequestPermissionsResult(Activity activity, int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            //存储
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "请允许存储权限后，使用录屏功能", Toast.LENGTH_SHORT).show();
                }
                break;
            //录音
            case AUDIO_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "请允许录音权限后，使用录屏功能", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void initRecordService(final Activity activity) {
        projectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                DisplayMetrics metrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
                recordService = binder.getRecordService();
                recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        };

        Intent intent = new Intent(activity, RecordService.class);
        activity.bindService(intent, connection, Activity.BIND_AUTO_CREATE);
    }

    public enum RECORD_STATUS {
        RECORDING, STOP;
    }
}
