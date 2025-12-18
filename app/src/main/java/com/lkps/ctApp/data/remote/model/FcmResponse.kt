package com.lkps.ctApp.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * FCM push notification response.
 */
data class FcmResponse(
    @SerializedName("multicast_id")
    val multicastId: Long?,
    @SerializedName("success")
    val success: Int?,
    @SerializedName("failure")
    val failure: Int?,
    @SerializedName("results")
    val results: List<FcmResult>?
)

data class FcmResult(
    @SerializedName("message_id")
    val messageId: String?,
    @SerializedName("error")
    val error: String?
)

