package com.lkps.ctApp.data.model

import androidx.annotation.Keep
import com.lkps.ctApp.data.source.firebase.MessageLiveData
import com.lkps.ctApp.data.source.firebase.ReceiverLiveData
import com.google.firebase.firestore.Exclude

@Keep
data class Chat(
    var chatId: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null,
    var isGroupChat: Boolean? = null
) {
    @get:Exclude
    var user: ReceiverLiveData? = null

    @get:Exclude
    var message: MessageLiveData? = null
}