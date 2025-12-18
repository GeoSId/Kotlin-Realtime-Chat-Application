package com.lkps.ctApp.data.repository

import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.data.remote.model.FcmResponse
import com.lkps.ctApp.utils.states.NetworkState

/**
 * Repository interface for notification-related operations.
 */
interface NotificationRepository {

    /**
     * Sends a remote push notification to a specific device.
     *
     * @param regToken The FCM registration token of the recipient device
     * @param message The message to send as notification payload
     * @return Result containing the FCM response or error
     */
    suspend fun sendRemoteNotification(
        regToken: String,
        message: Message
    ): Result<FcmResponse>
}

