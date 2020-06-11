package com.dgs.screenrecord

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.dgs.screenrecord.record.ScreenRecorderHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var hasSystemAlertPermission = false
    var srHelper: ScreenRecorderHelper? = null

    companion object {
        var recordStatus = ScreenRecorderHelper.RECORD_STATUS.STOP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            srHelper = ScreenRecorderHelper.getInstance(application)
            srHelper!!.initRecordService(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSystemAlertPermission()
        } else {
            hasSystemAlertPermission = true
        }
        record_btn.setOnClickListener {
            if (hasSystemAlertPermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    onScreenRecord()
                }
            } else {
                Toast.makeText(this@MainActivity, "权限授予失败，无法使用录屏功能", Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkSystemAlertPermission()
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun onScreenRecord() {
        if (recordStatus == ScreenRecorderHelper.RECORD_STATUS.RECORDING) {
            srHelper!!.stopRecord(object : ScreenRecorderHelper.OnRecordStatusChangeListener {
                override fun onChangeSuccess() {
                    //当停止成功，做界面变化
                    changeRecordStatus(ScreenRecorderHelper.RECORD_STATUS.STOP.ordinal)
                }

                override fun onChangeFailed() {}
            })

        } else if (recordStatus == ScreenRecorderHelper.RECORD_STATUS.STOP) {
            srHelper!!.startRecord(this)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun checkSystemAlertPermission() {
        if (Build.VERSION.SDK_INT >= 23) { // Android6.0及以后需要动态申请权限
            if (!Settings.canDrawOverlays(this)) {
                //启动Activity让用户授权
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, 1010)
            } else {
                // 弹出悬浮窗
                hasSystemAlertPermission = true
            }
        } else {
            // 弹出悬浮窗
            hasSystemAlertPermission = true
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1010) {
            if (Build.VERSION.SDK_INT >= 23) { // Android6.0及以后需要动态申请权限
                if (Settings.canDrawOverlays(this)) {
                    // 弹出悬浮窗
                    hasSystemAlertPermission = true
                } else {
                    hasSystemAlertPermission = false
                    Toast.makeText(this@MainActivity, "权限授予失败，无法使用录屏功能", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                srHelper?.onActivityResult(
                        this,
                        requestCode,
                        resultCode,
                        data,
                        object : ScreenRecorderHelper.OnRecordStatusChangeListener {
                            override fun onChangeSuccess() {
                                changeRecordStatus(ScreenRecorderHelper.RECORD_STATUS.RECORDING.ordinal)
                            }

                            override fun onChangeFailed() {
                                changeRecordStatus(ScreenRecorderHelper.RECORD_STATUS.STOP.ordinal)
                            }
                        })
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        srHelper?.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    @Suppress("DEPRECATION")
    fun changeRecordStatus(status: Int) {
        when (status) {
            ScreenRecorderHelper.RECORD_STATUS.STOP.ordinal -> {
                recordStatus = ScreenRecorderHelper.RECORD_STATUS.STOP
                record_btn.setBackgroundResource(R.drawable.stop_record_screen)
            }

            ScreenRecorderHelper.RECORD_STATUS.RECORDING.ordinal -> {
                recordStatus = ScreenRecorderHelper.RECORD_STATUS.RECORDING
                record_btn.setBackgroundResource(R.drawable.record_screen)
            }
        }
    }
}