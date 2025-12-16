package com.lkps.ctApp.utils


import java.text.SimpleDateFormat
import java.util.*

class DateUtils {

    companion object {
        fun isTimeExpired(time: Long): Boolean {
            if (time != 0L && System.currentTimeMillis() > time) {
                return true
            }
            return false
        }

        fun getFormatDate(time: Long): String {
            val sdfDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val now = Date(time)
            return sdfDate.format(now)
        }

        fun getFormatDateFromConfig(time: Long): String {
            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val now = Date(time)
            return sdfDate.format(now)
        }

        fun getFormatTime(time: Long): String {
            val sdfDate = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Date(time)
            return sdfDate.format(now)
        }

        fun currentTimeToLong(): Long {
            return System.currentTimeMillis()
        }

        fun getNextDays(afterDay: Int): String {
            val cal = Calendar.getInstance()
            cal.time = Date(currentTimeToLong())
            cal.add(Calendar.DAY_OF_MONTH, afterDay) //Adds a day
            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return sdfDate.format(cal.time)
        }

        fun getToday(): String {
            val cal = Calendar.getInstance()
            cal.time = Date(currentTimeToLong())
            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return sdfDate.format(cal.time)
        }

        fun checkDay(date: String): String {
            val c = Calendar.getInstance()
            c.time = Date(convertDateToLong(date)) // yourdate is an object of type Date
            val dayOfWeek = c[Calendar.DAY_OF_WEEK] // this will for example return 3 for tuesday
            return dayOfWeek.toString()
        }

        fun convertDateToLong(date: String?): Long {
            val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return df.parse(date).time
        }

        fun convertDateDetailToLong(date: String?): Long {
            val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return df.parse(date).time
        }

        fun convertStampToLong(date: String?): Long {
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return df.parse(date).time
        }

        fun convertStampForPreOrderDate(time: Long): String {
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val now = Date(time)
            return sdfDate.format(now)
        }

        fun convertLongToTime(time: String?): String {
            val tTime = convertTimeToLong(time)
            val cTime = currentTimeToLong()

            if (cTime < tTime) {
                var remainTime = ""
                val dTime: Long = tTime - cTime
                val seconds = dTime / 1000
                val minutes = seconds / 60
                val minutes3 = (seconds % 3600) / 60
                val hours = minutes / 60

                remainTime = if (hours > 0) {
                    hours.toString().plus("h:").plus(minutes3).plus("'")
                } else {
                    minutes3.toString().plus("'")
                }
                return remainTime
            } else {
                return ""
            }
        }

        fun convertTimeToLong(timDate: String?): Long {
            return calcTimeInMills(timDate)
        }

        private fun calcTimeInMills(serverTime: String?): Long{
            if(serverTime == null){
                return 0
            }
            if(validateServerTime(serverTime)) {
                val  hours = getHour(serverTime).toInt()
                val  mins = getMin(serverTime).toInt()
                val cal = Calendar.getInstance(Locale.getDefault())
                cal.set(Calendar.HOUR_OF_DAY, hours)
                cal.set(Calendar.MINUTE, mins)
                val tim = cal.timeInMillis
                return tim
            }
            return 0
        }

        private fun validateServerTime(serverTime: String): Boolean{
            return  serverTime.contains(":")
        }

        private fun getHour(serverTime: String): String{
            return serverTime.split(":")[0]
        }

        private fun getMin(serverTime: String): String{
            return serverTime.split(":")[1]
        }
    }
}