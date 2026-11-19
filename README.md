# NEXORA 💬

**NEXORA** is a modern Android messaging application built with **Kotlin, Jetpack Compose, and Firebase**. It provides a sleek, dark-themed messaging experience with real-time conversations, username-based accounts, customizable emoji avatars, message replies, and read receipts.

Designed with a clean UI, gradient accents, and a focus on simplicity, NEXORA aims to make communication feel modern and intuitive.

---

## ✨ Features

- **Authentication** – Register and sign in using a unique username and password.
- **Username-based identity** – Users can connect without sharing email addresses.
- **Real-time messaging** – Send and receive messages using Firebase Firestore.
- **Conversation management** – Start conversations with other registered users.
- **Message replies** – Reply to specific messages with a reply preview.
- **Read receipts** – Messages display single or double check marks depending on read status.
- **Customizable avatars** – Choose from predefined emoji avatars or use a custom emoji.
- **Live profile synchronization** – Profile name, username, and avatar updates are reflected in conversations.
- **Real-time chat updates** – Conversations and messages update automatically.
- **Message notifications infrastructure** – Notification channel setup and notification permission handling.
- **Haptic feedback** – Vibration feedback when new incoming messages are detected.
- **Dark UI** – Modern interface with purple, blue, and cyan gradient accents.
- **Responsive chat interface** – Compact message bubbles, reply previews, and an intuitive chat layout.

---

## 🛠️ Built With

| Technology | Purpose |
|---|---|
| Kotlin | Main programming language |
| Jetpack Compose | UI development |
| Material 3 | UI components and design system |
| Firebase Authentication | Account authentication |
| Firebase Firestore | Real-time database |
| Android SDK | Native Android development |
| Gradle | Build system |

---

## 📱 Screenshots


<p align="center">
  <img src="https://github.com/user-attachments/assets/42077ce7-e0ae-490e-8fac-c1996a3ff72a" width="180"/>
  <img src="https://github.com/user-attachments/assets/7e5dfcf7-320c-4676-99d8-d4f08f0e0f46" width="180"/>
  <img src="https://github.com/user-attachments/assets/226456da-931b-4443-8bd8-77f5e7d63aa7" width="180"/>
  <img src="https://github.com/user-attachments/assets/839a9581-bb88-4788-a959-45a397237ff7" width="180"/>
  <img src="https://github.com/user-attachments/assets/a2a54efb-22a3-4d54-b50c-7cbb35c8f85b" width="180"/>
</p>


- Sign in / Registration
- Chat list
- Conversation screen
- Profile screen
- Avatar selection dialog

---

## 🎨 Design

NEXORA features a custom dark visual design built around:

- Deep navy and midnight backgrounds
- Purple and violet gradients
- Cyan accent colors
- Rounded cards and message bubbles
- Emoji-based avatars
- Minimal navigation
- Smooth UI transitions

The interface is designed to provide a visually consistent and modern messaging experience.

---

## 🔐 Authentication

NEXORA uses Firebase Authentication to manage user accounts.

Users register and sign in using a username and password. Usernames are normalized and converted into an internal email-format identifier for Firebase Authentication.

Usernames are stored separately in Firestore to enable username-based lookup and account discovery.

---

## ☁️ Firebase Structure

NEXORA uses Firebase Firestore to store user profiles, usernames, conversations, and messages.

### `users`

Stores user profile information.

```json
{
  "uid": "user_id",
  "name": "Display Name",
  "username": "username",
  "photoUrl": "",
  "avatar": "👨‍🚀",
  "createdAt": "timestamp"
}
```

### `usernames`

Maps usernames to Firebase user IDs.

```json
{
  "uid": "user_id"
}
```

### `conversations`

Stores conversation metadata and participants.

```json
{
  "participants": ["user1", "user2"],
  "memberNames": {},
  "memberUsernames": {},
  "memberPhotos": {},
  "memberAvatars": {},
  "lastMessage": "",
  "updatedAt": "timestamp"
}
```

### `messages`

Messages are stored inside each conversation's subcollection:

`conversations/{conversationId}/messages/{messageId}`

Example message:

```json
{
  "senderId": "user_id",
  "text": "Hello!",
  "replyText": "",
  "createdAt": "timestamp",
  "readBy": ["user_id"]
}
```

---

## 🚀 Getting Started

### Requirements

- Android Studio
- Android SDK
- Kotlin
- A Firebase project
- Android device or emulator

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/NEXORA.git
```

### 2. Open the project

Open the project in Android Studio.

### 3. Connect Firebase

1. Create a Firebase project.
2. Add your Android application to Firebase.
3. Use the correct Android package name.
4. Download `google-services.json`.
5. Place it inside the `app/` directory.

### 4. Enable Firebase services

Enable the following:

- Firebase Authentication – Email/Password
- Cloud Firestore

### 5. Run the application

Sync Gradle, build the project, and run it on an Android device or emulator.

---

## 📂 Project Structure

```text
NEXORA/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/example/nexora/
│   │       │       └── MainActivity.kt
│   │       └── AndroidManifest.xml
│   └── google-services.json
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🔮 Future Improvements

- Image and file sharing
- Push notifications using Firebase Cloud Messaging
- Online/offline status
- Typing indicators
- Message deletion and editing
- Group chats
- Voice messages
- Improved account security and username reservation
- Profile photo uploads
- Chat search
- Encryption enhancements

---

## 👨‍💻 Developer

Developed by **Amir Erfan Eslamikia**.

NEXORA is an independent Android project built to explore modern Android development, Kotlin, Jetpack Compose, Firebase integration, and real-time communication.

---

## 📄 License

This project is currently available for personal and educational use. A formal license can be added to define redistribution and usage rights.

---

<p align="center">
  <b>NEXORA</b><br>
  Connect beyond the ordinary.
</p>

