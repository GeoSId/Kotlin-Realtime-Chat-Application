/**
 * Firebase Cloud Functions for sending FCM notifications.
 * 
 * Setup Instructions:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Login: firebase login
 * 3. Deploy: firebase deploy --only functions
 * 
 * This function listens for new messages in Firestore and sends push notifications.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const db = getFirestore();
const messaging = getMessaging();

/**
 * Triggered when a new message is added to any chat room.
 * Sends a push notification to the receiver.
 */
exports.sendChatNotification = onDocumentCreated(
    "chatRooms/{chatRoomId}/chatRoom/{messageId}",
    async (event) => {
        const snapshot = event.data;
        if (!snapshot) {
            console.log("No data associated with the event");
            return null;
        }

        const message = snapshot.data();
        const receiverId = message.receiverId;
        const senderId = message.senderId;
        const senderName = message.name || "Someone";

        if (!receiverId) {
            console.log("No receiver ID, skipping notification");
            return null;
        }

        try {
            // Get receiver's FCM tokens
            const userDoc = await db.collection("users").doc(receiverId).get();

            if (!userDoc.exists) {
                console.log("Receiver user not found");
                return null;
            }

            const fcmTokens = userDoc.data().fcmToken;
            if (!fcmTokens || fcmTokens.length === 0) {
                console.log("No FCM tokens for receiver");
                return null;
            }

            // Build notification body
            let notificationBody = "New message";
            if (message.text) {
                notificationBody = message.text;
            } else if (message.audioUrl) {
                notificationBody = "🎤 Voice message";
            } else if (message.fileExtension === "jpg") {
                notificationBody = "📷 Photo";
            } else if (message.fileExtension === "pdf") {
                notificationBody = "📁 File";
            }

            // Build FCM message using HTTP v1 API format
            const fcmMessage = {
                notification: {
                    title: senderName,
                    body: notificationBody,
                },
                data: {
                    id: message.id || "",
                    senderId: senderId || "",
                    receiverId: receiverId || "",
                    name: senderName,
                    text: message.text || "",
                    audioUrl: message.audioUrl || "",
                    fileExtension: message.fileExtension || "",
                    click_action: "OPEN_CHAT_ACTIVITY",
                },
                android: {
                    priority: "high",
                    notification: {
                        sound: "default",
                        clickAction: "OPEN_CHAT_ACTIVITY",
                    },
                },
                tokens: fcmTokens,
            };

            // Send using sendEachForMulticast for multiple tokens
            const response = await messaging.sendEachForMulticast(fcmMessage);

            console.log(`Successfully sent ${response.successCount} notifications`);

            // Handle failed tokens (remove invalid ones)
            if (response.failureCount > 0) {
                const failedTokens = [];
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) {
                        console.log(`Failed to send to token ${idx}: ${resp.error?.message}`);
                        // Check if token is invalid
                        if (resp.error?.code === "messaging/invalid-registration-token" ||
                            resp.error?.code === "messaging/registration-token-not-registered") {
                            failedTokens.push(fcmTokens[idx]);
                        }
                    }
                });

                // Remove invalid tokens from user's document
                if (failedTokens.length > 0) {
                    await db.collection("users").doc(receiverId).update({
                        fcmToken: FieldValue.arrayRemove(...failedTokens)
                    });
                    console.log(`Removed ${failedTokens.length} invalid tokens`);
                }
            }

            return response;
        } catch (error) {
            console.error("Error sending notification:", error);
            return null;
        }
    }
);

/**
 * HTTP callable function to send a notification directly.
 * Can be called from the app if needed.
 */
exports.sendNotification = onCall(async (request) => {
    // Verify the user is authenticated
    if (!request.auth) {
        throw new HttpsError(
            "unauthenticated",
            "User must be authenticated to send notifications"
        );
    }

    const { receiverId, message } = request.data;

    if (!receiverId || !message) {
        throw new HttpsError(
            "invalid-argument",
            "receiverId and message are required"
        );
    }

    try {
        // Get receiver's FCM tokens
        const userDoc = await db.collection("users").doc(receiverId).get();

        if (!userDoc.exists) {
            throw new HttpsError("not-found", "User not found");
        }

        const fcmTokens = userDoc.data().fcmToken;
        if (!fcmTokens || fcmTokens.length === 0) {
            return { success: false, message: "No FCM tokens for receiver" };
        }

        const fcmMessage = {
            notification: {
                title: message.name || "New Message",
                body: message.text || "You have a new message",
            },
            data: {
                senderId: message.senderId || "",
                receiverId: receiverId,
                name: message.name || "",
                click_action: "OPEN_CHAT_ACTIVITY",
            },
            android: {
                priority: "high",
            },
            tokens: fcmTokens,
        };

        const response = await messaging.sendEachForMulticast(fcmMessage);
        return {
            success: true,
            successCount: response.successCount,
            failureCount: response.failureCount
        };
    } catch (error) {
        console.error("Error:", error);
        throw new HttpsError("internal", error.message);
    }
});
