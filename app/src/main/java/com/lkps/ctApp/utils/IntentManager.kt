package com.lkps.ctApp.utils

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Parcelable
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
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

    fun getTypeFromUri(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            // For content URIs, try to get MIME type
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType != null) {
                return when (mimeType) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/gif" -> "gif"
                    "image/webp" -> "webp"
                    else -> {
                        // Fallback to extension extraction from MIME type
                        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        extension ?: "jpg"
                    }
                }
            }
        }

        // Fallback to URI parsing
        return getFileExtensionFromUri(uri)
    }

     fun galleryAddPic(context: Context): Uri? {
        val photoFile = File(FileHelper.currentPhotoPath)
        if (!photoFile.exists()) {
            return null
        }

        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(photoFile.extension)

        MediaScannerConnection.scanFile(
                context,
                arrayOf(photoFile.absolutePath),
                arrayOf(mimeType),
                null
        )

        // Return FileProvider URI instead of file:// URI for security
        val fileProviderUri = FileProvider.getUriForFile(
            context,
            FileHelper.AUTHORITY,
            photoFile
        )

        return fileProviderUri
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