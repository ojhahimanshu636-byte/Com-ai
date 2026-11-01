package com.com.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class Speaker(ctx: Context) {
    private var ready = false
    private val callbacks = HashMap<String, () -> Unit>()
    private val tts: TextToSpeech = TextToSpeech(ctx) {
        if (it == TextToSpeech.SUCCESS) {
            tts2().language = Locale("en", "IN")
            tts2().setPitch(0.9f)
            ready = true
        }
    }
    private fun tts2() = tts

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onError(id: String?) { callbacks.remove(id)?.invoke() }
            override fun onDone(id: String?) { callbacks.remove(id)?.invoke() }
        })
    }

    /** Bolna khatam hone ke baad onDone chalega */
    fun say(text: String, onDone: () -> Unit = {}) {
        if (!ready) { onDone(); return }
        val id = System.nanoTime().toString()
        callbacks[id] = onDone
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun shutdown() = tts.shutdown()
}
