package com.com.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0A0F1E"))
            setPadding(60, 60, 60, 60)
        }
        val title = TextView(this).apply {
            text = "COM"
            textSize = 56f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "Bolo: \"Com, YouTube kholo\""
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val start = Button(this).apply {
            text = "Com ko ON karo"
            setOnClickListener { startCom() }
        }
        val stop = Button(this).apply {
            text = "Com ko OFF karo"
            setOnClickListener { stopService(Intent(this@MainActivity, ComService::class.java)) }
        }
        root.addView(title)
        root.addView(sub)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }

    private fun startCom() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 1)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            return
        }
        startForegroundService(Intent(this, ComService::class.java))
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(c, p, r)
        startCom()
    }
}
