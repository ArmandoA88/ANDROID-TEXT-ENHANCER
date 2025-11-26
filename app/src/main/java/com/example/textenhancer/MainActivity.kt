package com.example.textenhancer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val apiKeyInput = findViewById<EditText>(R.id.apiKeyInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val enableServiceButton = findViewById<Button>(R.id.enableServiceButton)
        val overlayPermissionButton = findViewById<Button>(R.id.overlayPermissionButton)
        val modeSwitch = findViewById<Switch>(R.id.modeSwitch)

        val prefs = getSharedPreferences("TextEnhancerPrefs", Context.MODE_PRIVATE)
        apiKeyInput.setText(prefs.getString("API_KEY", ""))
        modeSwitch.isChecked = prefs.getBoolean("PREVIEW_MODE", true)
        modeSwitch.text = if (modeSwitch.isChecked) getString(R.string.preview_mode) else getString(R.string.auto_replace_mode)

        saveButton.setOnClickListener {
            prefs.edit().putString("API_KEY", apiKeyInput.text.toString()).apply()
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        }

        modeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("PREVIEW_MODE", isChecked).apply()
            modeSwitch.text = if (isChecked) getString(R.string.preview_mode) else getString(R.string.auto_replace_mode)
        }

        enableServiceButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        overlayPermissionButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
