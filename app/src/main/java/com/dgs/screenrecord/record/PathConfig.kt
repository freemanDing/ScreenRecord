package com.dgs.screenrecord.record

import android.os.Environment

/**
 * 配置类
 */
class PathConfig {

    companion object {
        val ROOT_PATH = Environment.getExternalStorageDirectory().absolutePath
        var APP_ROOT_PATH = "$ROOT_PATH/dgs"
        public var RECORD_SCREEN = "$APP_ROOT_PATH/recordScreen/"

        public var ACTION_EXIT_APP = "mft.exitApp"
        public var ACTION_RECORD_SCREEN_STOP = "mft.recordScreenStop"
        public var ACTION_RECORD_SCREEN_STATE = "mft.recordScreenState"


    }

}