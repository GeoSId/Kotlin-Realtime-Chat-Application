package com.lkps.ctApp.utils.extension

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

fun String.saveTo(path: String) {
    val file = File(path)
    if (file.exists()) return
    URL(this).openStream().use { input ->
        try {
            if (file.parentFile?.exists() != true) file.parentFile?.mkdirs()
            file.createNewFile()
            FileOutputStream(file).use { output -> input.copyTo(output) }
        } catch (e: Exception) {
            throw e
        }
    }
}

fun String.saveFileTo(path: File) {
    try {
        URL(this).openStream().use { input ->
            FileOutputStream(path).use { output ->
                input.copyTo(output)
            }
        }
    }catch (e: IOException){
        e.printStackTrace()
    }
}