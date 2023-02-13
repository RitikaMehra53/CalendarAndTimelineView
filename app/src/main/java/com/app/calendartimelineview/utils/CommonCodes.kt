package com.app.calendartimelineview.utils
import android.os.Environment

class CommonCodes {
    companion object{
        const val DATE_FORMAT_FOR_SELECTED_DATE = "EEEE MMMM dd, yyyy"
        var CUSTOM = 1
        var TODAY = 2
        var THIS_WEEK = 3
        var THIS_MONTH = 4
        var LAST_MONTH = 5
        var THIS_YEAR = 6
        var LAST_YEAR = 7
        var LOADING_DURATION = 3000L
        var SETTINGS_JSON = "settings.json"
        var TIME_OFFSET_JSON = "TimeOffset.json"
        var PRACTICE_DASHBOARD_JSON = "PracticeDashboardDummyData.json"
        var HYGIENE_FILTER_JSON = "HygieneFilter.json"
        var BAR_VISIBLE_LIMIT = 7f
        //var LINE_VISIBLE_LIMIT = 3f

        var SEND_MESSAGE_METHOD = "SendMessage"
        var RECEIVE_MESSAGE_METHOD = "ReceiveMessageNotify"
        var READ_CHAT_METHOD = "ReadChat"
        var ONLINE_USER_METHOD = "OnlineUsers"
        var READ_CHAT_NOTIFY_METHOD = "ReadChatNotify"
        //var SET_ONLINE_METHOD = "SetOnline"
        var ONLINE_NOTIFY_METHOD = "OnlineUsersNotify"

        var CHAT_MESSAGE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS"

        var MESSAGE_NOT_SENT = "not_sent"
        var MESSAGE_SENT = "sent"
        var MESSAGE_READ = "read"

        var CHAT_RECORD_COUNT = 1000
        var PATIENT_RECORD_COUNT = 5000
        var REVIEW_COUNT = 20
        var MAIN_PATH = Environment.getExternalStoragePublicDirectory("/").toString() + "/Axle"
        var DOCUMENT_PATH = "$MAIN_PATH/Axle Documents"
        var DOCUMENT_INSUR_PATH = "$MAIN_PATH/Axle Insurance Documents"
        var IMAGE_PATH = "$MAIN_PATH/Axle Images"
        var VIDEO_PATH = "$MAIN_PATH/Axle Videos"
        const val DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd"
        const val TIME_FORMAT_HH_MM_A = "hh:mm a"
        //val SCHEDULE_CELL_HEIGHT = 100
        const val SCHEDULE_CELL_HEIGHT = 14
        const val DATE_FROM_SERVER = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        const val DATE_FROM_SERVER_FOR_APPOINTMENT = "yyyy-MM-dd'T'HH:mm:ss"
        const val DUMMY_DATE = "0001-01-01T00:00:00"
        const val MMM_D_YYYY = "MMM d, yyyy"
    }
}