package com.lkps.ctApp.utils

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Parcelable
import android.webkit.MimeTypeMap
import java.io.File

object IntentManager {

    fun getIntentUri(intent: Intent): Uri {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            return it
        }
        return Uri.EMPTY
    }

    fun getIntentType(intent: Intent): String {
        when {
            hasActionSendIntent(intent)-> {
                when {
                    "text/plain" == intent.type -> {
                        return "text"
                    }
                    intent.type?.startsWith("image/") == true -> {
                        return "jpg"
                    }
                    "application/pdf" == intent.type -> {
                        return "pdf"
                    }
                }
            }
            hasActionSendToIntent(intent) -> {
                return ""
            }
            intent.action == Intent.ACTION_SEND_MULTIPLE && intent.type?.startsWith("image/") == true -> {
//                handleSendMultipleImages(intent)
                return ""
            }
            else -> {
                return ""
            }
        }
        return ""
    }

    fun getTypeFromUri(intent: Intent):String{
        if(intent.data != null && intent.data.toString().contains("images")){
            return "jpg"
        }else{
            return ""
        }
    }

     fun galleryAddPic(context: Context): Uri? {
        val photoFile = File(FileHelper.currentPhotoPath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(photoFile.extension)
        MediaScannerConnection.scanFile(
                context,
                arrayOf(photoFile.absolutePath),
                arrayOf(mimeType),
                null
        )
        return Uri.fromFile(photoFile)
    }

    fun getFileExtensionFromUri(uri: Uri):String{
        val lastPathSegment = uri.lastPathSegment ?: ""
        return lastPathSegment.substring(lastPathSegment.lastIndexOf(".") + 1)
    }

    private fun hasActionSendIntent(intent: Intent):Boolean{
        return intent.action == Intent.ACTION_SEND
    }

    private fun hasActionSendToIntent(intent: Intent):Boolean{
        return intent.action == Intent.ACTION_SENDTO
    }
}