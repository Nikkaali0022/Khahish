# Khahish (خواہش) - Real-Time Messaging & Calling App

**Khahish** is a modern Android messaging and calling platform built with Kotlin, Jetpack Compose, Firebase Firestore, Firebase Authentication, and Agora RTC.

## Key Features

1. **User Authentication**
   - Seamless Google Sign-In with Jetpack Credential Manager (`GetSignInWithGoogleOption`).
   - Profile management with customizable usernames (`@handle`), display names, bio, and phone numbers.
   - Phone Auth configuration support.

2. **Contact & Real-Time Search System**
   - Instant real-time user lookup by username with debounced queries.
   - Friend request system with real-time pending, accepted, and declined states.
   - Contact list with instant 1-tap chat and call access.

3. **Real-Time 1-on-1 Messaging**
   - Instant messaging powered by Cloud Firestore Enterprise.
   - Automatic chronological ordering, message read statuses, and last message previews.
   - Responsive Material 3 message bubbles and auto-scrolling conversation state.

4. **Agora RTC Voice & Video Calling**
   - **Testing Mode**: Powered by Agora App ID `6a5791178d1b47f499fee84c8d7a0beb`.
   - **Token Bypass**: Fully configured to bypass token requirements by passing empty token credentials (`""`), allowing direct calls in testing mode.
   - Full-screen Picture-in-Picture (PIP) video call interface with local and remote rendering.
   - High-fidelity voice call mode with pulsing caller avatar and speakerphone/mute toggles.
   - Real-time in-app call signaling via Firestore `/calls` collection.

5. **Security & Quality**
   - Zero-trust hardened `firestore.rules` validating data schemas and user isolation.
   - Tier-1 JavaScript security unit tests and Tier-2 Robolectric integration tests.
   - Custom adaptive Material You app icon.

## Tech Stack
- **UI Framework**: Jetpack Compose (Material 3)
- **Database & Auth**: Firebase Firestore Enterprise & Firebase Authentication
- **Calling**: Agora RTC Android SDK 4.x
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **Image Loading**: Coil Compose
