# Implementation Plan: Android Text Enhancer "Addon"

## Overview
This project aims to create an Android application that acts as a keyboard "addon" to clarify text. Since Android does not allow directly modifying 3rd party keyboards (like Gboard), we will implement a **Floating Assistant Button** using an **Accessibility Service**. This button will appear when the user is typing and, when clicked, will read the text, process it (to make it clearer), and replace/suggest the improved text.

## Phase 1: Project Initialization & Configuration
- [ ] Initialize Android project structure (Gradle, Manifest).
- [ ] Configure `AndroidManifest.xml`:
    - [ ] Add `SYSTEM_ALERT_WINDOW` permission (for floating button).
    - [ ] Add `BIND_ACCESSIBILITY_SERVICE` permission (to read/write text).
    - [ ] Add Internet permission (for API calls).
- [ ] Create `res/xml/accessibility_service_config.xml` to define service capabilities (listen to text changes, retrieve window content).

## Phase 2: Accessibility Service (The Core)
- [ ] Create `TextEnhancerService` extending `AccessibilityService`.
- [ ] Implement `onServiceConnected()` to configure the service overlay.
- [ ] Implement `onAccessibilityEvent()` to detect input focus:
    - [ ] Listen for `TYPE_VIEW_FOCUSED` on nodes where `isEditable == true`.
    - [ ] Show the floating button when an editable field is focused.
    - [ ] Hide the floating button when focus is lost or screen is locked.
- [ ] (Optional) Refine keyboard detection using `WindowInsets` if possible, or stick to focus-based proxy for simplicity.

## Phase 3: Floating UI (The Button)
- [ ] Create a layout for the floating button (e.g., a small "Sparkle" or "Enhance" icon).
- [ ] Implement `WindowManager` logic to draw the button over other apps.
- [ ] Ensure the button position is adjustable or placed conveniently near the keyboard area (but not blocking keys).
- [ ] **New**: Implement a "Preview Popup" window that appears if the user has "Preview Mode" enabled.

## Phase 4: Text Processing Logic (Cloud API)
- [ ] Add networking library (Retrofit or OkHttp).
- [ ] Create an API Client to communicate with LLM (OpenAI/Gemini).
- [ ] Implement `TextProcessor` class:
    - [ ] Method `enhanceText(originalText: String): Future<String>`
    - [ ] Securely handle API Keys (user provided in settings).

## Phase 5: User Interface (Settings)
- [ ] Create a `MainActivity` with a Settings screen:
    - [ ] **Toggle**: "Auto-Replace" vs "Show Preview".
    - [ ] **Input**: API Key field.
    - [ ] **Action**: Enable Accessibility Service.
    - [ ] **Action**: Grant Overlay permissions.
    - [ ] **Slider**: Adjust button opacity/position.

## Phase 6: Testing & Refinement
- [ ] Test on Android Emulator.
- [ ] Verify interaction with common apps (WhatsApp, Messages, Chrome).
- [ ] Verify "Keyboard Open/Close" behavior (button visibility).
- [ ] Handle edge cases (password fields, empty text, network errors).
