package com.lkps.ctApp.controllers.shared_preferences

import android.content.Context
import android.content.SharedPreferences
import com.lkps.ct.BuildConfig

class SharedPrefsController(context: Context?) {

    private val settings: SharedPreferences?

    init {
        settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        const val PREFS_NAME = "LKPS" + BuildConfig.APPLICATION_ID

        const val LOGIN = "login"
        const val SOCIAL_LOGIN_TYPE = "sociallogintype"
        const val APP_VERSION = "app_version"
        const val FORCE_UPDATE_VERSION = "force_update_version"
        const val SHARE_PREFS_DEFAULT_VALUE = "-1"
        const val DELETE_MESSAGE_SEC_CONFIG = "-1"
        const val DELETE_MESSAGE_IGNORE_RECIPIENT_IF_READ_THE_MESSAGE = "ignore_recipient_and_delete"
    }

    fun applyListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        settings?.registerOnSharedPreferenceChangeListener(listener)
    }

    fun set(tag: String, string: String?) {
        settings?.put(tag, string)
    }

    fun set(tag: String, integer: Int) {
        settings?.put(tag, integer)
    }

    fun set(tag: String, bool: Boolean) {
        settings?.put(tag, bool)
    }

    fun set(tag: String, value: Double) {
        settings?.put(tag, value)
    }

    fun set(tag: String, value: Long) {
        settings?.put(tag, value)
    }

    fun get(tag: String, value: String?): String? =
        settings?.get(tag, value)

    fun get(tag: String, value: Int): Int? =
        settings?.get(tag, value)

    fun get(tag: String, value: Boolean): Boolean? =
        settings?.get(tag, value)

    fun get(tag: String, value: Double): Double? =
        settings?.get(tag, value)

    fun get(tag: String, value: Long): Long? =
        settings?.get(tag, value)

    fun remove(tag: String) {
        settings?.remove(tag)
    }

    /*Sub->     ----------------------------------------------------------------*/
    val isLogin: Boolean
        get() = settings?.get(LOGIN, "0").equals("1")

    val getSocialType: String?
        get() = settings?.get(SOCIAL_LOGIN_TYPE, SHARE_PREFS_DEFAULT_VALUE)

    val getAppVersion: String?
        get() = settings?.get(APP_VERSION, SHARE_PREFS_DEFAULT_VALUE)

    val hasNewVersion: Boolean
        get() = !getAppVersion.equals(BuildConfig.VERSION_NAME)

    val shouldIgnoreIfTheRecipientRead: Boolean
        get() = settings?.get(DELETE_MESSAGE_IGNORE_RECIPIENT_IF_READ_THE_MESSAGE, false)?: false

    /*Sub-> Funcs ----------------------------------------------------------------*/

    fun setLogin(isLogin: String?) {
        settings?.put(LOGIN, isLogin)
    }

    fun setSocialLoginType(socialLoginType: String?) {
        settings?.put(SOCIAL_LOGIN_TYPE, socialLoginType)
    }

    fun setAppVersion(appVersion: String?) {
        settings?.put(APP_VERSION, appVersion)
    }

    fun setNewVersion(version: Long) {
        settings?.put(FORCE_UPDATE_VERSION, version)
    }

    fun setDeleteMessageSec(sec: Long?) {
        settings?.put(DELETE_MESSAGE_SEC_CONFIG, sec)
    }

    fun setRecipientIgnore(isRecipientIgnored: Boolean?) {
        settings?.put(DELETE_MESSAGE_IGNORE_RECIPIENT_IF_READ_THE_MESSAGE, isRecipientIgnored)
    }

    fun getNewVersion(): Long? = (settings?.get(FORCE_UPDATE_VERSION, 0L))?.toLong()

    fun getSecDeleteMessage() = (settings?.get(DELETE_MESSAGE_SEC_CONFIG, 30L))?.toLong()?: 30L

    fun clearAll() {
        setSocialLoginType(SHARE_PREFS_DEFAULT_VALUE)
    }
}