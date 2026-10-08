# Khahish - Firebase Hosting & Backend Deployment Guide

This guide provides end-to-end instructions for deploying, configuring, and maintaining the **Khahish** real-time messaging and voice/video calling application backend.

---

## 1. Firebase Project Overview

- **Firebase Project ID**: `com-aistudio-khahish-cha-912ed`
- **Database**: Cloud Firestore Enterprise
- **Custom Database ID**: `ai-studio-android-khahish-4c810dce-1a7d-4080-96ec-9273c2d54a02`
- **Authentication**: Firebase Authentication with Google Sign-In (Credential Manager) and Phone Auth capability.
- **Rules Configuration**: Hardened Attribute-Based Access Control (ABAC) in `firestore.rules`.

---

## 2. Deploying Firestore Security Rules

The Firestore security rules in `firestore.rules` protect user profiles, contact requests, 1-on-1 chats, messages, and call sessions.

To deploy or redeploy security rules manually from the CLI:

```bash
# Login to Firebase CLI
firebase login

# Select or verify active project
firebase use com-aistudio-khahish-cha-912ed

# Deploy Firestore rules targeting the named database
firebase deploy --only firestore:rules
```

---

## 3. Firebase Hosting Setup (Web & Landing Page / Web Client)

If you wish to host a web companion, web app, or promotional landing page for Khahish using Firebase Hosting:

### Step 1: Initialize Firebase Hosting
In the root directory of your project:
```bash
firebase init hosting
```
- Select your project: `com-aistudio-khahish-cha-912ed`
- Specify public directory: `public` (or your web build folder `dist` / `build`)
- Configure as a single-page app: **Yes**
- Set up automatic builds and deploys with GitHub: (Optional)

### Step 2: Hosting Configuration (`firebase.json`)
Ensure your `firebase.json` contains the hosting block:
```json
{
  "hosting": {
    "public": "public",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  }
}
```

### Step 3: Deploy to Firebase Hosting
```bash
firebase deploy --only hosting
```
Once deployed, Firebase will provide your live hosting URLs:
- `https://com-aistudio-khahish-cha-912ed.web.app`
- `https://com-aistudio-khahish-cha-912ed.firebaseapp.com`

---

## 4. Agora RTC Voice & Video Calling Setup

Khahish integrates real-time low-latency audio and video calling via **Agora RTC SDK 4.x**.

### Configuration Summary
- **App ID**: `6a5791178d1b47f499fee84c8d7a0beb`
- **Authentication Mode**: **Testing Mode (App ID only)**
- **Token Requirement**: **Completely Bypassed**

### How Token Bypass Works
In Agora Testing Mode, you bypass token authentication by passing `""` (empty string) as the token parameter to `RtcEngine.joinChannel(...)`:

```kotlin
// ChannelMediaOptions configured for Communication profile
val options = ChannelMediaOptions().apply {
    channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
    publishMicrophoneTrack = true
    publishCameraTrack = isVideo
    autoSubscribeAudio = true
    autoSubscribeVideo = isVideo
}

// Token bypassed in Testing Mode
val result = engine.joinChannel("", channelId, 0, options)
```

### Signaling via Firestore
1. Caller creates a document in `/calls/{callId}` with `callType = "voice" | "video"`, `channelId = callId`, and `status = "ringing"`.
2. Receiver receives the real-time snapshot and sees an incoming call dialog.
3. Upon accepting, receiver updates status to `"accepted"` and joins channel `channelId`.
4. Audio & video streams connect peer-to-peer via Agora's global edge network.
5. Either party can end the call, updating status to `"ended"` and releasing camera/mic.

---

## 5. Phone Authentication Activation

To enable SMS Phone Authentication:
1. Open [Firebase Console](https://console.firebase.google.com).
2. Select `com-aistudio-khahish-cha-912ed`.
3. Navigate to **Build > Authentication > Sign-in method**.
4. Enable the **Phone** provider.
5. (Optional) Under **Phone numbers for testing**, add test numbers (e.g., `+1 650-555-3434` with SMS code `123456`) for rapid verification without SMS charges.
