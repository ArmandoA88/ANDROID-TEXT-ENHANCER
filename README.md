# Android Text Enhancer

An Android Accessibility Service that adds a floating "Enhance" button to your keyboard. It uses AI to clarify and rewrite text in any app.

## Features
- **Floating Button**: Appears automatically when you are typing.
- **AI Powered**: Uses OpenAI API to rewrite text.
- **Preview Mode**: Review the changes before applying them.
- **Universal**: Works in WhatsApp, Telegram, Notes, Chrome, etc.

## Setup Instructions

1.  **Open in Android Studio**:
    -   Open Android Studio.
    -   Select "Open" and navigate to this folder (`ANDROID TEXT ENHANCER`).

2.  **Build & Run**:
    -   Connect your Android device or start an Emulator.
    -   Click the "Run" (Play) button.

3.  **Permissions (One Time Setup)**:
    -   When the app opens, enter your **OpenAI API Key**.
    -   Click **"Grant Overlay Permission"** -> Allow "Display over other apps".
    -   Click **"Enable Accessibility Service"** -> Find "Text Enhancer" in the list -> Turn it ON.

4.  **Usage**:
    -   Open any app (e.g., Messages).
    -   Tap on a text field to start typing.
    -   You will see a small floating button (Pencil icon).
    -   Type some text (e.g., "i want go store u want come?").
    -   Tap the floating button.
    -   The app will suggest a clearer version (e.g., "I am going to the store. Do you want to come?").
    -   Click "Apply" to replace your text.

## Configuration
-   **Preview Mode**: Toggle in the App Settings. If off, text is replaced instantly.
-   **Button Position**: Currently fixed near the bottom right. You can adjust `x` and `y` in `TextEnhancerService.kt` if needed.
