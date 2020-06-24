package com.dgs.screenrecord.record

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 录屏后台服务
 * 录屏服务可能在其他进程
 */
class RecordService : Service() {

    private var recordView: RecordView? = null
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0
    private val fileDir = PathConfig.RECORD_SCREEN
    private val mQuit = AtomicBoolean(false)
    private var mResultCode = 0
    private var mResultData: Intent? = null
    private var handler: Handler? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    //状态栏高度.
    private var statusBarHeight = -1

    /**
     * 希望在应用退出System.exit(0)前发个广播出来，直接退出导致录制的文件有问题，打不开
     */
    var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stopRecord()
        }
    }

    override fun onCreate() {
        super.onCreate()
        //用于计时
        handler = Handler()
        //注册退出App广播
        val intentFilter = IntentFilter()
        intentFilter.addAction(PathConfig.ACTION_EXIT_APP)
        intentFilter.addAction(PathConfig.ACTION_RECORD_SCREEN_STOP)
        registerReceiver(receiver, intentFilter)
        val serviceThread: Thread = HandlerThread("service_thread", Process.THREAD_PRIORITY_BACKGROUND)
        serviceThread.start()
        mediaRecorder = MediaRecorder()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val recordState = recordStateManager.state
        if (recordState == RecordStateManager.RECORDING) {
            return START_STICKY
        }
        parseIntent(intent)
        getStatusBarHeight()
        showRecordView()
        sendRecordState(RecordStateManager.START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startRecord()
            sendRecordState(RecordStateManager.RECORDING)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun sendRecordState(state: Int) {
        recordStateManager.setRecordState(state)
        val intent = Intent(PathConfig.ACTION_RECORD_SCREEN_STATE)
        intent.putExtra("state", state)
        sendBroadcast(intent)
    }

    private fun getStatusBarHeight() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
    }

    private fun showRecordView() {
        if (recordView != null) {
            return
        }
        recordView = RecordView(this, handler)
        recordView!!.setStatusBarHeight(statusBarHeight)
        recordView!!.setRecordListener {
            stopRecord()
            stopSelf()
        }
        recordView!!.showRecordView()
    }

    private fun createRecordDir(): String {
        val recordFd = File(fileDir)
        if (!recordFd.exists() || !recordFd.isDirectory) {
            recordFd.mkdir()
        }
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
        val time = sdf.format(Date(System.currentTimeMillis()))
        return "$fileDir$time.mp4"
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun startRecord() {
        try {
            initRecorder()
            mediaProjection = createMediaProjection()
            virtualDisplay = mediaProjection!!.createVirtualDisplay("MainScreen",
                    mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder!!.surface, null, null)
            mediaRecorder!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRecord() {
        mQuit.set(true)
        val state = recordStateManager.state
        if (state == RecordStateManager.STOP) {
            return
        }
        sendRecordState(RecordStateManager.STOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            release()
        }
        if (recordView == null) {
            return
        }
        if (recordView!!.recordStatus != RecordStateManager.STOP) {
            val btnStop = recordView!!.btnStop
            btnStop?.performClick()
        }
    }

    private fun parseIntent(intent: Intent) {
        mResultCode = intent.getIntExtra("code", -1)
        mResultData = intent.getParcelableExtra("data")
        mScreenWidth = intent.getIntExtra("width", 720)
        mScreenHeight = intent.getIntExtra("height", 1280)
        mScreenDensity = intent.getIntExtra("density", 1)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun createMediaProjection(): MediaProjection {
        return (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(mResultCode, mResultData)
    }

    private fun initRecorder() {
        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder!!.setOutputFile(createRecordDir())
        mediaRecorder!!.setVideoSize(mScreenWidth, mScreenHeight)
        mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder!!.setVideoEncodingBitRate(5 * 1024 * 1024)
        mediaRecorder!!.setVideoFrameRate(30)
        mediaRecorder!!.prepare()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun release() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder!!.stop()
                mediaRecorder!!.reset()
            }

            if (virtualDisplay != null) {
                virtualDisplay!!.release()
            }
            if (mediaProjection != null) {
                mediaProjection!!.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "media release error")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        stopRecord()
    }

    companion object {
        private const val TAG = "RecordService"
        private val recordStateManager = RecordStateManager.instance
    }
}