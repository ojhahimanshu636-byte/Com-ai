package com.com.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class ComService : Service() {

    private lateinit var speaker: Speaker
    private lateinit var commands: Commands
    private var recognizer: SpeechRecognizer? = null
    private val h = Handler(Looper.getMainLooper())
    private var running = false
    private var busy = false

    // "com" / "hey com" / "ok com" detect karne ke liye
    private val wake = Regex("\\b(hey |hi |ok |okay )?(com|kom|kaam|calm|कॉम|कोम)\\b[,. ]*")

    override fun onCreate() {
        super.onCreate()
        val ch = NotificationChannel("com", "Com", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        val n = Notification.Builder(this, "com")
            .setContentTitle("Com sun raha hai")
            .setContentText("\"Com\" bolke command do")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)

        speaker = Speaker(this)
        commands = Commands(this, speaker)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (!running) {
            running = true
            h.postDelayed({ speaker.say("Com online hai. Boliye.") { listen() } }, 1200)
        }
        return START_STICKY
    }

    private fun listen() {
        if (!running || busy) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(r: Bundle?) {
                    val text = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                    val m = wake.find(text)
                    if (m != null) {
                        busy = true
                        val cmd = text.substring(m.range.last + 1).trim()
                        commands.handle(cmd) { busy = false; h.postDelayed({ listen() }, 400) }
                    } else listen()
                }
                override fun onError(e: Int) { h.postDelayed({ listen() }, 600) }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            })
        }
    }

    override fun onDestroy() {
        running = false
        recognizer?.destroy()
        speaker.shutdown()
        super.onDestroy()
    }

    override fun onBind(i: Intent?): IBinder? = null
}
