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
import android.os.*
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.dgs.screenrecord.record.PathConfig.Companion.RECORD_SCREEN
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by dgs
 */
class RecordService : Service() {
    private var recordView: RecordView? = null
    private var handler: Handler? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    var isRunning = false
        private set
    private var width = 720
    private var height = 1080
    private var dpi = 0
    private var dirPath = ""
    private var fileName = ""
    private val fileDir = RECORD_SCREEN

    //状态栏高度
    private var statusBarHeight = -1
    override fun onBind(intent: Intent): IBinder {
        return RecordBinder()
    }

    override fun onStartCommand(
            intent: Intent,
            flags: Int,
            startId: Int
    ): Int {
        getStatusBarHeight()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        //用于计时
        handler = Handler()
        //注册退出App广播
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_EXIT_APP)
        intentFilter.addAction(ACTION_RECORD_SCREEN_STOP)
        registerReceiver(receiver, intentFilter)
        val serviceThread = HandlerThread(
                "service_thread",
                Process.THREAD_PRIORITY_BACKGROUND
        )
        serviceThread.start()
        isRunning = false
        mediaRecorder = MediaRecorder()
    }

    fun setMediaProject(project: MediaProjection?) {
        mediaProjection = project
    }

    fun setConfig(width: Int, height: Int, dpi: Int) {
        this.width = width
        this.height = height
        this.dpi = dpi
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun startRecord(): Boolean {
        if (mediaProjection == null || isRunning) {
            return false
        }
        initRecorder()
        //显示计时器
        showRecordView()
        createVirtualDisplay()
        mediaRecorder!!.start()
        isRunning = true
        return true
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun stopRecord(): Boolean {
        if (!isRunning) {
            return false
        }
        isRunning = false
        mediaRecorder!!.stop()
        mediaRecorder!!.reset()
        virtualDisplay!!.release()
        mediaProjection!!.stop()
        if (recordView != null) {
            val btnStop = recordView!!.btnStop
            btnStop?.performClick()
            //将计时器View置空，用于下次重绘
            recordView = null
        }
        return true
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun createVirtualDisplay() {
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "MainScreen",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface,
                null,
                null
        )
    }

    //自定义录像的宽高
    fun setRecorderConfig(
            width: Int,
            height: Int,
            dirpath: String,
            fileName: String
    ) {
        if (width > 0 && height > 0) {
            this.width = width
            this.height = height
        }
        if (!TextUtils.isEmpty(dirpath)) {
            dirPath = dirpath
        }
        if (!TextUtils.isEmpty(fileName)) {
            this.fileName = fileName
        }
    }

    private fun initRecorder() {
        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder!!.setOutputFile(createRecordDir())
        mediaRecorder!!.setVideoSize(width, height)
        mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder!!.setVideoEncodingBitRate(5 * 1024 * 1024)
        mediaRecorder!!.setVideoFrameRate(30)
        mediaRecorder!!.prepare()
    }

    private fun createRecordDir(): String {
        val file = File(fileDir)
        if(!file.exists() || !file.isDirectory){
            file.mkdir()
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss")
        val time = sdf.format(Date(System.currentTimeMillis()))
        return "$fileDir$time.mp4"
    }

    inner class RecordBinder : Binder() {
        val recordService: RecordService
            get() = this@RecordService
    }

    private fun getStatusBarHeight() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
    }

    /**
     * 应用退出System.exit(0)前发个广播出来，停止录制
     */
    var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(
                context: Context,
                intent: Intent
        ) {
            stopRecord()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        super.onDestroy()
        stopRecord()
    }

    companion object {
        var ACTION_EXIT_APP = "mft.exitApp"
        var ACTION_RECORD_SCREEN_STOP = "mft.recordScreenStop"
    }
}