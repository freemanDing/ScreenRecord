package com.dgs.screenrecord.record

/**
 * 录屏状态接口
 */
interface IRecordScreen {
    fun onRecordStart()
    fun onRecordStop()
    fun onRecording()
}