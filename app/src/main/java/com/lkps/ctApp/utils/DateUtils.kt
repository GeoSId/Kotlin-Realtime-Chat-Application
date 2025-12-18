package com.lkps.ctApp.utils

import java.text.SimpleDateFormat
import java.util.*

class DateUtils {

    companion object {
        fun getFormatTime(time: Long): String {
            val sdfDate = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Date(time)
            return sdfDate.format(now)
        }
    }
}