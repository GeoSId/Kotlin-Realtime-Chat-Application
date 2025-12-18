package com.lkps.ctApp.data.remote

import com.lkps.ctApp.data.remote.model.FcmRequest
import com.lkps.ctApp.data.remote.model.FcmResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * FCM API Service for sending push notifications.
 *
 * Note: This uses the legacy FCM HTTP API. For production apps,
 * consider migrating to FCM HTTP v1 API which requires OAuth 2.0.
 */
interface FcmApiService {

    @POST("fcm/send")
    suspend fun sendNotification(
        @Header("Authorization") authorization: String,
        @Body request: FcmRequest
    ): Response<FcmResponse>
}

