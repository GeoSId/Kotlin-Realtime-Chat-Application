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
) {
    companion object {
        // FCM error codes
        const val ERROR_NOT_REGISTERED = "NotRegistered"
        const val ERROR_INVALID_REGISTRATION = "InvalidRegistration"
        const val ERROR_MISSING_REGISTRATION = "MissingRegistration"
    }

    fun isTokenInvalid(): Boolean {
        return error in listOf(
            ERROR_NOT_REGISTERED,
            ERROR_INVALID_REGISTRATION,
            ERROR_MISSING_REGISTRATION
        )
    }
}

