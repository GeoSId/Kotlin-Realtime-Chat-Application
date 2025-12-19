# Firebase Cloud Functions - FCM Notifications

This directory contains Firebase Cloud Functions for sending push notifications using the FCM HTTP v1 API.

## Why Cloud Functions?

The legacy FCM HTTP API (`https://fcm.googleapis.com/fcm/send`) was **deprecated and shut down in June 2024**. The new FCM HTTP v1 API requires OAuth 2.0 authentication, which should be handled server-side for security.

Cloud Functions automatically handle:
- ✅ OAuth 2.0 authentication via Firebase Admin SDK
- ✅ Automatic notification sending when messages are created
- ✅ Invalid token cleanup
- ✅ Multi-device support

## Setup Instructions

### 1. Install Firebase CLI

```bash
npm install -g firebase-tools
```

### 2. Login to Firebase

```bash
firebase login
```

### 3. Initialize Firebase in Your Project

Navigate to your project root and run:

```bash
firebase init functions
```

Select:
- Use an existing project: `Your Project id`
- Language: JavaScript
- ESLint: No (optional)
- Install dependencies: Yes

### 4. Copy Function Files

Copy `index.js` and `package.json` from this directory to the generated `functions/` folder, or use them directly.

### 5. Deploy Functions

```bash
cd firebase-functions
npm install
firebase deploy --only functions
```

## How It Works

### Automatic Notifications (Firestore Trigger)

The `sendChatNotification` function automatically triggers when a new message document is created:

```
Firestore Path: chatRooms/{chatRoomId}/chatRoom/{messageId}
```

When a message is added:
1. Function reads the receiver's FCM tokens from `users/{receiverId}`
2. Sends push notification to all receiver's devices
3. Removes invalid tokens automatically

### Notification Format

Notifications include:
- **Title**: Sender's name
- **Body**: Message text, or emoji indicator for media (🎤 Voice, 📷 Photo, 📁 File)
- **Data**: Message details for opening the correct chat

## Monitoring

View function logs:

```bash
firebase functions:log
```

Or view in [Firebase Console](https://console.firebase.google.com) → Functions → Logs

## Costs

Firebase Cloud Functions has a generous free tier:
- 2 million invocations/month free
- 400K GB-seconds/month free

For a chat app, this is typically more than enough.

## Troubleshooting

### Function not triggering?
- Verify the Firestore path matches: `chatRooms/{chatRoomId}/chatRoom/{messageId}`
- Check function logs for errors

### Notifications not received?
- Verify FCM tokens are stored correctly in `users/{userId}/fcmToken`
- Check if token is valid (not expired/unregistered)

### Permission errors?
- Ensure the service account has Cloud Functions permissions
- Check Firebase project settings

