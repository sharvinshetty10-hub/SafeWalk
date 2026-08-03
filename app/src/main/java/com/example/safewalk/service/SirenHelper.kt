package com.example.safewalk.service

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

class SirenHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun startSiren() {
        if (mediaPlayer == null) {
            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer.create(context, alertUri).apply {
                isLooping = true
                start()
            }
        }
    }

    fun stopSiren() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
            } catch (e: Exception) {
                // Ignore state exception
            }
            it.release()
        }
        mediaPlayer = null
    }
}
