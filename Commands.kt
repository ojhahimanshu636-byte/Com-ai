package com.com.ai

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Commands(private val ctx: Context, private val speaker: Speaker) {

    private val openWords = listOf("open", "kholo", "khol do", "khol", "chalao", "chalu karo",
        "start", "launch", "खोलो", "खोल", "चलाओ")
    private val filler = listOf("karo", "kardo", "do", "ko", "app", "please", "plz", "na", "kar do")
    private var torchOn = false

    /** Wake word "com" ke baad ka text yahan aata hai */
    fun handle(raw: String, done: () -> Unit) {
        val t = raw.lowercase(Locale.getDefault()).trim()
        when {
            t.isEmpty() -> speaker.say("Ji boliye", done)

            t.contains("torch") || t.contains("flashlight") || t.contains("flash") -> {
                val on = !(t.contains("off") || t.contains("band"))
                setTorch(on)
                speaker.say(if (on) "Torch chalu kiya ja raha hai" else "Torch band kiya ja raha hai", done)
            }

            t.contains("volume") && (t.contains("up") || t.contains("badha") || t.contains("zyada")) -> {
                adjustVolume(AudioManager.ADJUST_RAISE)
                speaker.say("Volume badhaya ja raha hai", done)
            }
            t.contains("volume") && (t.contains("down") || t.contains("kam") || t.contains("ghata")) -> {
                adjustVolume(AudioManager.ADJUST_LOWER)
                speaker.say("Volume kam kiya ja raha hai", done)
            }

            t.contains("time") || t.contains("samay") || t.contains("baje") -> {
                val now = SimpleDateFormat("h mm a", Locale.getDefault()).format(Date())
                speaker.say("Abhi samay hai $now", done)
            }

            t.startsWith("call") || t.contains("call karo") -> {
                val num = t.filter { it.isDigit() }
                if (num.length >= 5) {
                    speaker.say("$num par call kiya ja raha hai") {
                        val i = Intent(Intent.ACTION_CALL, Uri.parse("tel:$num"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(i); done()
                    }
                } else speaker.say("Number bataiye, jaise call 9 8 7 6 5 4 3 2 1 0", done)
            }

            t.contains("settings") -> launch(Intent(Settings.ACTION_SETTINGS), "Settings", done)

            t.contains("home") -> launch(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), "Home screen", done)

            openWords.any { t.contains(it) } -> openApp(t, done)

            else -> speaker.say("Maaf kijiye, samajh nahi aaya", done)
        }
    }

    private fun openApp(text: String, done: () -> Unit) {
        var name = text
        openWords.sortedByDescending { it.length }.forEach { name = name.replace(it, " ") }
        name = name.split(" ").filter { it.isNotBlank() && it !in filler }.joinToString(" ").trim()

        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        val match = apps.firstOrNull { pm.getApplicationLabel(it.activityInfo.applicationInfo).toString().lowercase() == name }
            ?: apps.firstOrNull { pm.getApplicationLabel(it.activityInfo.applicationInfo).toString().lowercase().contains(name) }

        if (name.isEmpty() || match == null) {
            speaker.say("$name naam ka app nahi mila", done); return
        }
        val label = pm.getApplicationLabel(match.activityInfo.applicationInfo).toString()
        val intent = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
        // Pehle bolo, phir kholo
        speaker.say("$label open kiya ja raha hai") {
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.let { ctx.startActivity(it) }
            done()
        }
    }

    private fun launch(i: Intent, label: String, done: () -> Unit) {
        speaker.say("$label open kiya ja raha hai") {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); done()
        }
    }

    private fun setTorch(on: Boolean) {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try { cm.setTorchMode(cm.cameraIdList[0], on); torchOn = on } catch (_: Exception) {}
    }

    private fun adjustVolume(dir: Int) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        repeat(3) { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI) }
    }
}
