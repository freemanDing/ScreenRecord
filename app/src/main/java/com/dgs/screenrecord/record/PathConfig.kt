package com.dgs.screenrecord.record

import android.os.Environment

/**
 * 配置类
 */
class PathConfig {

    companion object {
        val ROOT_PATH = Environment.getExternalStorageDirectory().absolutePath
        var APP_ROOT_PATH = "$ROOT_PATH/dgs"
        var RECORD_SCREEN = "$APP_ROOT_PATH/recordScreen/"
    }

}