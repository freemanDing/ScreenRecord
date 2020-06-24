package com.dgs.screenrecord

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.dgs.screenrecord.record.IRecordScreen
import com.dgs.screenrecord.record.PathConfig
import com.dgs.screenrecord.record.RecordActivity
import com.dgs.screenrecord.record.RecordStateManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), IRecordScreen {

    private var hasSystemAlertPermission = false
    private var recordStateManager: RecordStateManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordStateManager = RecordStateManager.instance
        recordStateManager?.registerView(this)

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

    override fun onResume() {
        super.onResume()
        recordScreen()
    }


    fun recordScreen() {
        if (recordStateManager!!.state === RecordStateManager.RECORDING) {
            record_btn.setBackgroundResource(R.drawable.stop_record_screen)
        } else {
            record_btn.setBackgroundResource(R.drawable.record_screen)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun onScreenRecord() {

        if (recordStateManager?.state === RecordStateManager.RECORDING) {
            record_btn.setBackgroundResource(R.drawable.record_screen)
            val stopIntent = Intent(PathConfig.ACTION_RECORD_SCREEN_STOP)
            sendBroadcast(stopIntent)
        } else {
            startActivity(Intent(this, RecordActivity::class.java))
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

    override fun onDestroy() {
        super.onDestroy()
        recordStateManager!!.unregisterView(this)

    }


    override fun onRecordStart() {
        if (record_btn != null) {
            record_btn.setBackgroundResource(R.drawable.stop_record_screen)
        }
    }

    override fun onRecordStop() {
        if (record_btn != null) {
            record_btn.setBackgroundResource(R.drawable.record_screen)
            Log.i("DGS", "保存路径：" + PathConfig.RECORD_SCREEN)
            Toast.makeText(this, "保存路径：" + PathConfig.RECORD_SCREEN, Toast.LENGTH_LONG).show()
        }
    }

    override fun onRecording() {
        if (record_btn != null) {
            record_btn.setBackgroundResource(R.drawable.stop_record_screen)
        }
    }
}