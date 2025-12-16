package com.lkps.ctApp.controllers.shared_preferences

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> SharedPreferences.get(key: String, defaultValue: T): T {
    when (T::class) {
        Boolean::class -> return this.getBoolean(key, false) as T
        Float::class -> return this.getFloat(key, defaultValue as Float) as T
        Int::class -> return this.getInt(key, defaultValue as Int) as T
        Long::class -> return this.getLong(key, defaultValue as Long) as T
        String::class -> return this.getString(key, defaultValue as String) as T
        else -> if (defaultValue is Set<*>) return this.getStringSet(
            key,
            defaultValue as? Set<String>
        ) as T
    }
    return defaultValue
}

inline fun <reified T> SharedPreferences.put(key: String, value: T) {
    val editor = this.edit()
    when (T::class) {
        Boolean::class -> editor.putBoolean(key, value as Boolean)
        Float::class -> editor.putFloat(key, value as Float)
        Int::class -> editor.putInt(key, value as Int)
        Long::class -> editor.putLong(key, value as Long)
        String::class -> editor.putString(key, value as String)
        else -> {
            if (value is Set<*>) {
                editor.putStringSet(key, value as Set<String>)
            }
        }
    }
    editor.apply()
}

inline fun <reified T> SharedPreferences.setArray(nameOfList: String, value: T) {
    val editor = this.edit()
    val gson = Gson()
    val json = gson.toJson(value)
    editor.putString(nameOfList,json)
    editor.apply()
}

inline fun <reified T> SharedPreferences.getArray(nameOfList: String?): T {
    val gson = Gson()
    val json = this.getString(nameOfList, null)
    val type = object : TypeToken<ArrayList<String>>(){}.type
    return gson.fromJson(json,type)
}

fun SharedPreferences.remove(key: String) {
    this.edit().apply {
        remove(key)
        apply()
    }
}

fun SharedPreferences.remove(vararg keys: String) {
    this.edit().apply {
        keys.forEach {
            remove(it)
        }
        apply()
    }
}
fun SharedPreferences.clearAll() {
    this.edit().apply {
        clear()
        apply()
    }
}
