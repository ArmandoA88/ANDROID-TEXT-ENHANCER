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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider

class TextEnhancerService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingButton: View? = null
    private var previewDialog: View? = null

    private var lastFocusedNode: AccessibilityNodeInfo? = null
    private var targetInputNode: AccessibilityNodeInfo? = null // Captured when button is clicked
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

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingButton?.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingButton, params)
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val xDiff = (event.rawX - initialTouchX).toInt()
                    val yDiff = (event.rawY - initialTouchY).toInt()
                    // Detect click
                    if (Math.abs(xDiff) < 10 && Math.abs(yDiff) < 10) {
                        v.performClick()
                    }
                    true
                }
                else -> false
            }
        }

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

        val eventType = event.eventType
        val source = event.source

        when (eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                if (source != null && source.isEditable) {
                    lastFocusedNode = source
                    // Cancel any pending hide
                    serviceScope.launch {
                        floatingButton?.visibility = View.VISIBLE
                    }
                } else {
                    // Don't hide immediately, might be transient focus change
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                 // Check if the new window has an input field focused?
                 // For now, let's just NOT hide immediately on window change, as keyboards are windows too.
            }
        }
    }

    private fun showPreviewDialog(initialEnhancedText: String) {
        if (previewDialog != null) {
            try { windowManager?.removeView(previewDialog) } catch (e: Exception) {}
        }

        // Use a ContextThemeWrapper to ensure Material attributes are available
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_TextEnhancer)
        previewDialog = LayoutInflater.from(themedContext).inflate(R.layout.layout_preview_dialog, null)
        val textView = previewDialog?.findViewById<android.widget.EditText>(R.id.previewText)
        val applyBtn = previewDialog?.findViewById<Button>(R.id.applyButton)
        val cancelBtn = previewDialog?.findViewById<Button>(R.id.cancelButton)
        val regenerateBtn = previewDialog?.findViewById<Button>(R.id.regenerateButton)
        val toneGroup = previewDialog?.findViewById<ChipGroup>(R.id.toneChipGroup)
        val lengthGroup = previewDialog?.findViewById<ChipGroup>(R.id.lengthChipGroup)
        val languageSpinner = previewDialog?.findViewById<android.widget.Spinner>(R.id.languageSpinner)

        textView?.setText(initialEnhancedText)

        // Load Preferences
        val prefs = getSharedPreferences("TextEnhancerPrefs", Context.MODE_PRIVATE)
        val savedTone = prefs.getString("PREF_TONE", "Professional")
        val savedLength = prefs.getInt("PREF_LENGTH", 50)
        val savedLanguage = prefs.getString("PREF_LANGUAGE", "Auto")

        // Set Tone Chip
        val toneCount = toneGroup?.childCount ?: 0
        for (i in 0 until toneCount) {
             val chip = toneGroup?.getChildAt(i) as? Chip
             if (chip?.text.toString() == savedTone) {
                 chip?.isChecked = true
                 break
             }
        }
        
        // Set Length Chip
        val lenCount = lengthGroup?.childCount ?: 0
        for (i in 0 until lenCount) {
             val chip = lengthGroup?.getChildAt(i) as? Chip
             if ((chip?.text.toString().toIntOrNull() ?: 0) == savedLength) {
                 chip?.isChecked = true
                 break
             }
        }

        // Set Language Spinner
        val languages = listOf("Auto (Same as Input)", "English", "Spanish", "French", "German")
        val adapter = android.widget.ArrayAdapter(themedContext, R.layout.spinner_item, languages)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        languageSpinner?.adapter = adapter
        
        val langIndex = languages.indexOfFirst { it == savedLanguage }
        if (langIndex >= 0) {
            languageSpinner?.setSelection(langIndex)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.horizontalMargin = 0f 
        params.verticalMargin = 0f
        params.dimAmount = 0.5f
        
        // Ensure the dialog allows touch outside to dismiss?
        // Actually, we want it to behave like a bottom sheet.

        applyBtn?.setOnClickListener {
            val validText = textView?.text.toString()
            removePreviewDialog()
            
            // Wait for focus to return to the app, then apply
            serviceScope.launch {
                kotlinx.coroutines.delay(300)
                applyText(validText)
            }
        }

        cancelBtn?.setOnClickListener {
            removePreviewDialog()
        }
        
        regenerateBtn?.setOnClickListener {
            val selectedChipId = toneGroup?.checkedChipId
            val selectedTone = if (selectedChipId != null && selectedChipId != View.NO_ID) {
                val chip = previewDialog?.findViewById<Chip>(selectedChipId)
                chip?.text.toString()
            } else {
                "Professional"
            }
            
            val selectedLengthId = lengthGroup?.checkedChipId
            val selectedLength = if (selectedLengthId != null && selectedLengthId != View.NO_ID) {
                 val chip = previewDialog?.findViewById<Chip>(selectedLengthId)
                 chip?.text.toString().toIntOrNull() ?: 50
            } else {
                50
            }

            val selectedLanguage = languageSpinner?.selectedItem?.toString() ?: "Auto"
            
            // Re-run enhancement
            performEnhancement(selectedTone, selectedLength, selectedLanguage, updatePreview = true)
        }

        windowManager?.addView(previewDialog, params)
    }

    private fun enhanceCurrentText() {
        // Capture the node at the moment of interaction to prevent focus loss issues
        targetInputNode = lastFocusedNode
        
        val prefs = getSharedPreferences("TextEnhancerPrefs", Context.MODE_PRIVATE)
        val savedTone = prefs.getString("PREF_TONE", "Professional") ?: "Professional"
        val savedLength = prefs.getInt("PREF_LENGTH", 50)
        val savedLanguage = prefs.getString("PREF_LANGUAGE", "Auto") ?: "Auto"
        
        performEnhancement(savedTone, savedLength, savedLanguage, updatePreview = false)
    }

    private fun performEnhancement(tone: String, length: Int, language: String, updatePreview: Boolean) {
        targetInputNode?.refresh()
        val text = targetInputNode?.text?.toString()

        if (text.isNullOrEmpty()) {
            Toast.makeText(this, "No text to enhance", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("TextEnhancerPrefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("API_KEY", "")
        
        // Save preferences
        prefs.edit()
            .putString("PREF_TONE", tone)
            .putInt("PREF_LENGTH", length)
            .putString("PREF_LANGUAGE", language)
            .apply()
        
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "Please set API Key in App", Toast.LENGTH_LONG).show()
            return
        }

        if (!updatePreview) {
             Toast.makeText(this, "Enhancing...", Toast.LENGTH_SHORT).show()
        } else {
             val textView = previewDialog?.findViewById<android.widget.EditText>(R.id.previewText)
             textView?.setText("Regenerating...")
        }

        serviceScope.launch {
            try {
                val processor = TextProcessor(apiKey)
                val result = processor.enhance(text, tone, length, language)

                if (updatePreview && previewDialog != null) {
                    val textView = previewDialog?.findViewById<android.widget.EditText>(R.id.previewText)
                    textView?.setText(result)
                } else {
                    // Initial show
                    showPreviewDialog(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@TextEnhancerService, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                if (updatePreview && previewDialog != null) {
                     val textView = previewDialog?.findViewById<android.widget.EditText>(R.id.previewText)
                     textView?.setText("Failed: ${e.message}")
                }
            }
        }
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
        
        // Attempt 1: Use captured node
        targetInputNode?.refresh()
        var success = targetInputNode?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments) ?: false

        // Attempt 2: If failed, try to find fresh focus (dialog is gone now)
        var currentNode = targetInputNode
        if (!success) {
             val root = rootInActiveWindow
             val freshFocus = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
             if (freshFocus != null) {
                 currentNode = freshFocus
                 success = currentNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
             }
        }

        if (!success) {
            // Fallback: Copy to clipboard and Paste
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Enhanced Text", text)
                clipboard.setPrimaryClip(clip)
                
                // Try paste action on best available node
                val pasteSuccess = currentNode?.performAction(AccessibilityNodeInfo.ACTION_PASTE) ?: false
                
                if (pasteSuccess) {
                    Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to apply. Text copied to clipboard.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                 Toast.makeText(this, "Failed to apply text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInterrupt() {
        if (floatingButton != null) {
            windowManager?.removeView(floatingButton)
            floatingButton = null
        }
    }
}
