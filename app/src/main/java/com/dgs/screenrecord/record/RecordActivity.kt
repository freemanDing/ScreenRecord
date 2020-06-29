package com.dgs.screenrecord.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.dgs.screenrecord.R
import com.dgs.screenrecord.record.RecordActivity
import com.dgs.screenrecord.record.RecordService
import java.util.*

/**
 * 启动屏幕录制
 */
class RecordActivity : Activity() {
    private val REQUEST_PERMISSION = 0xf3
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recordState = RecordStateManager.instance.state
        if (recordState == RecordStateManager.RECORDING || recordState == RecordStateManager.START) {
            finish()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this@RecordActivity)) {
                permissions()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, REQUEST_FLOATING_PERMISSION)
            }
        } else {
            Toast.makeText(this@RecordActivity, getString(R.string.not_support), Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestRecord() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mScreenWidth = metrics.widthPixels
        mScreenHeight = metrics.heightPixels
        mScreenDensity = metrics.densityDpi
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_RECORD)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            finish()
            return
        }
        if (requestCode == REQUEST_RECORD) {
            val service = Intent(this, RecordService::class.java)
            service.putExtra("width", mScreenWidth)
            service.putExtra("height", mScreenHeight)
            service.putExtra("density", mScreenDensity)
            service.putExtra("code", resultCode)
            service.putExtra("data", data)
            startService(service)
            finish()
        } else if (requestCode == REQUEST_FLOATING_PERMISSION) {
            if (!Settings.canDrawOverlays(this)) {
                finish()
            } else {
                permissions()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            val refuseList: MutableList<Int> = ArrayList()
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    refuseList.add(grantResults[i])
                }
            }
            //权限回调后 finish
            finish()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun permissions() {
        val permissionList = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
        val needList: MutableList<String> = ArrayList()
        for (i in permissionList.indices) {
            val checkPermission = checkSelfPermission(permissionList[i])
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                needList.add(permissionList[i])
            }
        }
        if (needList.isEmpty()) {
            requestRecord()
        } else {
            val permissionArray = needList.toTypedArray()
            ActivityCompat.requestPermissions(this, permissionArray, REQUEST_PERMISSION)
        }
    }

    companion object {
        private const val REQUEST_RECORD = 0x0f
        var REQUEST_FLOATING_PERMISSION = 0x0e
    }
}