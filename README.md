# 💬 KMPL - Kotlin Real-Time Chat Application

A full-featured, real-time messaging application built natively for Android using **Kotlin**. This app provides secure, instant messaging with support for text, voice messages, images, and PDF file sharing.

![Android](https://img.shields.io/badge/Android-28%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-blue)
![Firebase](https://img.shields.io/badge/Firebase-34.7.0-orange)

---

## 📱 Features

- **Real-time Messaging** - Instant message delivery using Firebase Firestore
- **Voice Messages** - Record and send audio messages with playback support
- **Image Sharing** - Share photos from gallery or capture directly from camera
- **PDF File Sharing** - Send and receive PDF documents
- **Push Notifications** - Receive notifications for new messages via Firebase Cloud Messaging (FCM)
- **User Authentication** - Secure email-based authentication with Firebase Auth
- **User Search** - Find and connect with other users by username
- **Online Status** - Real-time online/offline status indicators
- **Dark Mode** - Built-in dark theme support
- **Remote Configuration** - Dynamic app configuration via Firebase Remote Config
- **Crash Reporting** - Integrated Firebase Crashlytics for stability monitoring

---

## 🏗️ Architecture

The app follows the **MVVM (Model-View-ViewModel)** architecture pattern with:

- **Dagger 2** for dependency injection
- **Kotlin Coroutines** for asynchronous operations
- **LiveData** for reactive UI updates
- **Navigation Component** with Safe Args for type-safe navigation
- **Data Binding & View Binding** for UI binding

### Project Structure

```
app/src/main/java/com/lkps/ctApp/
├── App.kt                          # Application class with DI setup
├── controllers/                    # App controllers
│   ├── crash/                      # Crashlytics controller
│   ├── device/                     # Device management
│   ├── locale/                     # Localization
│   └── shared_preferences/         # Local storage
├── data/
│   ├── model/                      # Data models (User, Chat, Message)
│   ├── repository/                 # Repository pattern implementation
│   ├── source/firebase/            # Firebase data sources
│   └── worker/                     # WorkManager tasks
├── di/                             # Dependency injection modules
├── notification/                   # Push notification handling
├── utils/                          # Utility classes and extensions
└── view/                           # UI components
    ├── adapters/                   # RecyclerView adapters
    ├── chatRoom/                   # Chat screen
    ├── chatRooms/                  # Chat list screen
    ├── searchUser/                 # User search screen
    └── userProfile/                # Profile management
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Arctic Fox (2021.3.1) or later
- **JDK 17** or higher
- **Android SDK** with API level 28 (minimum) to 36 (target)
- **Google Firebase Account**

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/GeoSid/Kotlin-Realtime-Chat-Application.git
cd Kotlin-Realtime-Chat-Application
```

#### 2. Firebase Project Setup

You need to create a Firebase project and configure the required services.

##### Step 2.1: Create Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** and follow the setup wizard
3. Enter your project name and click **Continue**
4. (Optional) Enable Google Analytics and click **Create project**

##### Step 2.2: Register Your Android App

1. In your Firebase project, click the **Android icon** to add an Android app
2. Enter the package name: `com.lkps.ct`
3. (Optional) Enter an app nickname
4. Enter your SHA-1 certificate fingerprint:
   ```bash
   # For debug keystore (development)
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Click **Register app**

##### Step 2.3: Download `google-services.json`

1. Download the `google-services.json` file
2. Place it in the `app/` directory:
   ```
   Kotlin-Realtime-Chat-Application/
   └── app/
       └── google-services.json  ← Place here
   ```

#### 3. Enable Firebase Services

In the Firebase Console, enable the following services:

##### 3.1 Authentication
1. Go to **Build → Authentication**
2. Click **Get started**
3. Go to **Sign-in method** tab
4. Enable **Email/Password** provider

##### 3.2 Cloud Firestore
1. Go to **Build → Firestore Database**
2. Click **Create database**
3. Choose **Start in test mode** (for development) or configure security rules
4. Select your preferred Cloud Firestore location
5. Click **Enable**

##### 3.3 Realtime Database (Optional - for additional features)
1. Go to **Build → Realtime Database**
2. Click **Create Database**
3. Choose your database location
4. Start in **test mode** for development

##### 3.4 Cloud Storage
1. Go to **Build → Storage**
2. Click **Get started**
3. Start in **test mode** for development
4. Choose your storage location

##### 3.5 Cloud Messaging (FCM)
1. Go to **Project Settings** (gear icon)
2. Navigate to **Cloud Messaging** tab
3. FCM is automatically configured - push notifications are handled by Firebase Cloud Functions

#### 4. Deploy Firebase Cloud Functions (Push Notifications)

Push notifications are handled server-side using Firebase Cloud Functions for enhanced security.

```bash
cd firebase-functions
npm install
firebase login
firebase deploy --only functions
```

See `firebase-functions/README.md` for detailed deployment instructions.

#### 5. Configure Firestore Security Rules

In the Firebase Console, go to **Firestore Database → Rules** and set up your security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Usernames collection
    match /usernames/{username} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
    }
    
    // Chat rooms collection
    match /chatRooms/{chatId} {
      allow read, write: if request.auth != null;
    }
    
    // Chat room messages
    match /chatRoom/{chatId}/{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### 6. Configure Storage Security Rules

In **Storage → Rules**, add:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /chat_files/{allPaths=**} {
      allow read, write: if request.auth != null;
    }
    match /chat_records/{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### 7. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files (**File → Sync Project with Gradle Files**)
3. Connect an Android device or start an emulator (API 28+)
4. Click **Run** or use `./gradlew installDebug`

---

## ⚙️ Configuration

### gradle.properties

```properties
# Android settings
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
org.gradle.jvmargs=-Xmx1536m
```

### Firebase Remote Config (Optional)

The app uses Firebase Remote Config for dynamic settings:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `friendly_msg_length` | 1000 | Maximum message length |
| `delete_chat_room_sec` | 30 | Auto-delete time in seconds |
| `delete_chat_room_ignore_reader` | false | Ignore read status when deleting |

---

## 📋 Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | 2.2.0 | Programming language |
| Firebase BOM | 34.7.0 | Firebase services |
| Glide | 5.0.5 | Image loading |
| Dagger | 2.57.2 | Dependency injection |
| Navigation | 2.9.6 | Navigation component |
| Lifecycle | 2.10.0 | Lifecycle-aware components |
| Coroutines | 1.10.2 | Async programming |
| WorkManager | 2.11.0 | Background tasks |

---

## 🔐 Permissions

The app requires the following permissions:

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Network access |
| `RECORD_AUDIO` | Voice messages |
| `CAMERA` | Take photos |
| `READ_MEDIA_IMAGES` | Access photos (Android 13+) |
| `READ_MEDIA_VIDEO` | Access videos (Android 13+) |
| `POST_NOTIFICATIONS` | Push notifications (Android 13+) |

---

## 🧪 Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

---

## 📝 Firestore Data Structure

```
firestore/
├── users/
│   └── {userId}/
│       ├── userId: string
│       ├── username: string
│       ├── usernameList: array
│       ├── photoUrl: string
│       ├── isOnline: boolean
│       ├── lastSeenTimestamp: timestamp
│       └── fcmToken: array
│
├── usernames/
│   └── {username}/
│       ├── userId: string
│       └── username: string
│
├── chatRooms/
│   └── {chatId}/
│       ├── chatId: string
│       ├── senderId: string
│       ├── receiverId: string
│       └── isGroupChat: boolean
│
└── chatRoom/
    └── {chatId}/
        └── chatRoom/
            └── {messageId}/
                ├── id: string
                ├── senderId: string
                ├── receiverId: string
                ├── text: string
                ├── fileUrl: string
                ├── audioUrl: string
                ├── timestamp: timestamp
                └── readTimestamp: timestamp
```

---

## 🛠️ Troubleshooting

### Common Issues

**1. Build fails with "google-services.json not found"**
- Ensure `google-services.json` is placed in the `app/` directory

**2. Push notifications not working**
- Ensure Firebase Cloud Functions are deployed (`firebase deploy --only functions`)
- Check if Cloud Messaging is enabled in Firebase Console
- Ensure the device has Google Play Services installed
- Verify FCM token is being saved to the user's Firestore document

**3. Authentication fails**
- Verify Email/Password authentication is enabled in Firebase Console
- Check your `google-services.json` has the correct project configuration

**4. Firestore permission denied**
- Review your Firestore security rules
- Ensure the user is authenticated

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📧 Contact

For questions or support, please open an issue in the repository.

---

**Made with ❤️ using Kotlin & Firebase**
