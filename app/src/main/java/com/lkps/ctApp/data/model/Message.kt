package com.lkps.ctApp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Message(
    var id: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null,
    var isOwner: Boolean? = null,
    var name: String? = null,
    var fileUrl: String? = null,
    var audioUrl: String? = null,
    var audioFile: String? = null,
    var audioDuration: Long? = null,
    var fileExtension: String? = null,
    var fileName: String? = null,
    var text: String? = null,
    var timestamp: Any? = null,
    var readTimestamp: Any? = null,
    var pdf: String? = null,
    var photo: String? = null
) {

    @get:Exclude
    var audioDownloaded = false

    fun setMessageId() {
        val timestamp = this.timestamp
        if (timestamp is Timestamp) {
            id = senderId + "_" + timestamp.toDate().time
        }
    }
}