# Project Explanation: Android Text Enhancer

## The Challenge: "Addons" in Android
You requested an "addon" that works as a button *in* the keyboard. 

**Technical Reality:** Android security architecture isolates apps. One app cannot simply inject a button inside another app's keyboard (e.g., you cannot add a button *inside* Gboard's layout).

## The Solution: Accessibility Service Overlay
To achieve the desired effect without forcing you to switch to a completely new (and likely inferior) custom keyboard, we will build an **Accessibility Service Overlay**.

### How it works:
1.  **Monitoring**: The app runs in the background and monitors when you click on a text box (like in WhatsApp or Notes).
2.  **Overlay**: When a text box is active, our app draws a small floating button on top of the screen (positioned just above or to the side of your keyboard).
3.  **Action**:
    *   You type your text normally.
    *   You tap the "Enhance" button.
    *   The app reads the text from the box using Accessibility APIs.
    *   The app processes the text (using AI) to make it clearer.
    *   The app replaces the text in the box with the improved version.

### Why this approach?
*   **Keep your Keyboard**: You can keep using Gboard, SwiftKey, or Samsung Keyboard.
*   **Universal**: Works in almost any app (Email, SMS, Social Media).
*   **"Addon" Feel**: It feels like an extension to your typing experience.

## Alternative: Custom Input Method Service
The other option is to build a full keyboard from scratch. 
*   *Pros*: The button is truly "in" the keyboard.
*   *Cons*: We have to rebuild *everything* (autocorrect, swipe typing, emoji, multi-language support). This is usually too much work for just one feature and results in a worse typing experience.

**Recommendation**: We proceed with the **Overlay/Accessibility Service** approach.
