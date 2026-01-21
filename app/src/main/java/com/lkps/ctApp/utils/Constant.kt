package com.lkps.ctApp.utils

object Constant {

    //Title  ------  FIRESTORE ---------------------------------
    const val REF_USERS = "users"
    const val REF_USERS_NAMES = "usernames"
    const val REF_CHAT_ROOMS = "chatRooms"
    const val REF_CHAT_ROOM = "chatRoom"
    const val REF_CHAT_FILES = "chat_files"
    const val REF_CHAT_RECORDS = "chat_records"
    const val REF_TIME_STAMP = "timestamp"
    const val REF_USER_NAME_LIST = "usernameList"
    const val REF_IS_ONLINE = "isOnline"
    const val REF_LAST_SEEN_TIME_STAMP = "lastSeenTimestamp"
    const val REF_FCM_TOKEN = "fcmToken"
    const val REF_READ_TIME_STAMP = "readTimestamp"

    const val LOG_OUT_ALL = 1L
    const val VERSION_PATH = "version"
    const val LOG_OUT_PATH = "logoutAll"

    const val AUDIO_FILE = "3gp"
    const val PDF_FILE = "pdf"
    const val JPG_FILE = "jpg"

    const val ONE_MESSAGE = "1"

    //Title  ------  Notifications ---------------------------------
    const val NOTIFICATION_INTENT = "user"
    const val NOTIFICATION_ID = 0x004

    //Title  ------  Permissions ---------------------------------
    const val PERMISSION_CAMERA = android.Manifest.permission.CAMERA
    const val PERMISSION_RECORD_AUDIO = android.Manifest.permission.RECORD_AUDIO
    const val PERMISSION_READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE
    const val PERMISSION_READ_MEDIA_IMAGES = android.Manifest.permission.READ_MEDIA_IMAGES
    const val PERMISSION_READ_MEDIA_VIDEO = android.Manifest.permission.READ_MEDIA_VIDEO
    const val PERMISSION_READ_MEDIA_AUDIO = android.Manifest.permission.READ_MEDIA_AUDIO
    const val PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED = android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED

}