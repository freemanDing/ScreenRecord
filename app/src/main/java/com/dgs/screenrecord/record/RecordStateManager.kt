package com.dgs.screenrecord.record

import java.util.*

/**
 * 录屏状态管理
 */
class RecordStateManager private constructor() {
    private var recordState: RecordState? = null
    val state: Int
        get() = recordState!!.state

    fun setRecordState(state: Int) {
        recordState!!.state = state
        notifyChangeView(state)
    }

    /**
     * 录屏按钮集合
     */
    var recordScreenList: MutableList<IRecordScreen>
    fun registerView(changeView: IRecordScreen) {
        recordScreenList.add(changeView)
    }

    fun unregisterView(changeView: IRecordScreen) {
        recordScreenList.remove(changeView)
    }

    private fun notifyChangeView(state: Int) {
        for (recordScreen in recordScreenList) {
            if (state == START) {
                recordScreen.onRecordStart()
            } else if (state == STOP) {
                recordScreen.onRecordStop()
            } else if (state == RECORDING) {
                recordScreen.onRecording()
            }
        }
    }

    companion object {
        val instance = RecordStateManager()

        const val START = 0xa1
        const val RECORDING = 0xa2
        const val STOP = 0xa3
    }

    init {
        if (recordState == null) {
            recordState = RecordState()
        }
        recordScreenList = ArrayList()
        recordState!!.state = STOP
    }
}