package com.example.textenhancer

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TextEnhancerService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingButton: View? = null
    private var previewDialog: View? = null
    private var lastFocusedNode: AccessibilityNodeInfo? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingButton()
    }

    private fun createFloatingButton() {
        floatingButton = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 50
        params.y = 200 // Offset from bottom to avoid covering keyboard keys too much

        floatingButton?.setOnClickListener {
            enhanceCurrentText()
        }

        try {
            windowManager?.addView(floatingButton, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Initially hide until we detect an input field
        floatingButton?.visibility = View.GONE
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val source = event.source
            if (source != null && source.isEditable) {
                lastFocusedNode = source
                floatingButton?.visibility = View.VISIBLE
            } else {
                // If focus moves to something not editable, hide it? 
                // We might want to keep it if the keyboard is still up, but that's hard to know for sure.
                // For now, let's be strict: only show if editable is focused.
                // floatingButton?.visibility = View.GONE 
            }
        }
    }

    private fun enhanceCurrentText() {
        // Refresh the node to get latest text
        lastFocusedNode?.refresh()
        val text = lastFocusedNode?.text?.toString()

        if (text.isNullOrEmpty()) {
            Toast.makeText(this, "No text to enhance", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("TextEnhancerPrefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("API_KEY", "")
        val isPreviewMode = prefs.getBoolean("PREVIEW_MODE", true)

        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "Please set API Key in App", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Enhancing...", Toast.LENGTH_SHORT).show()

        serviceScope.launch {
            val processor = TextProcessor(apiKey)
            val enhancedText = processor.enhance(text)

            if (isPreviewMode) {
                showPreviewDialog(enhancedText)
            } else {
                applyText(enhancedText)
            }
        }
    }

    private fun showPreviewDialog(newText: String) {
        if (previewDialog != null) {
            try { windowManager?.removeView(previewDialog) } catch (e: Exception) {}
        }

        previewDialog = LayoutInflater.from(this).inflate(R.layout.layout_preview_dialog, null)
        val textView = previewDialog?.findViewById<TextView>(R.id.previewText)
        val applyBtn = previewDialog?.findViewById<Button>(R.id.applyButton)
        val cancelBtn = previewDialog?.findViewById<Button>(R.id.cancelButton)

        textView?.text = newText

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        params.dimAmount = 0.5f

        applyBtn?.setOnClickListener {
            applyText(newText)
            removePreviewDialog()
        }

        cancelBtn?.setOnClickListener {
            removePreviewDialog()
        }

        windowManager?.addView(previewDialog, params)
    }

    private fun removePreviewDialog() {
        if (previewDialog != null) {
            windowManager?.removeView(previewDialog)
            previewDialog = null
        }
    }

    private fun applyText(text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        lastFocusedNode?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    override fun onInterrupt() {
        if (floatingButton != null) {
            windowManager?.removeView(floatingButton)
            floatingButton = null
        }
    }
}
