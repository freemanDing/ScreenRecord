package com.dgs.screenrecord.record

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 录屏服务可能在其他进程
 * 接收录屏状态改变广播
 */
class RecordReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (PathConfig.ACTION_RECORD_SCREEN_STATE == intent.action) {
            return
        }
        val state = intent.getIntExtra("state", -1)
        RecordStateManager.instance.setRecordState(state)
    }
}