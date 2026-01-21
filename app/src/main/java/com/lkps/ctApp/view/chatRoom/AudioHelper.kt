package com.lkps.ctApp.view.chatRoom

import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.lkps.ct.R
import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.utils.FileHelper
import com.lkps.ctApp.utils.Utility
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class AudioHelper(
    private val fileHelper: FileHelper,
    private val activity: AppCompatActivity
) {

    private var playButton: View? = null
    private var seekBar: SeekBar? = null
    private var audioTimeTextView: TextView? = null

    private var playFileName: String? = null
    var recordFileName: String? = null
    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null

    private var timer: Timer? = null
    private var pauseTime: Int? = null
    var recorderDuration: Long? = null

    private val audioManager = activity.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
    private var isRecording = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Another app has requested audio focus, stop recording
                if (isRecording) {
                    stopRecording()
                    Utility.makeText(activity, "Recording stopped - another app is using audio")
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // We could lower volume here, but for recording it's better to stop
                if (isRecording) {
                    stopRecording()
                    Utility.makeText(activity, "Recording stopped - audio interrupted")
                }
            }
        }
    }

    fun setupAudioHelper(view: View, message: Message) {
        if (playButton != view) {
            pauseTime = player?.currentPosition
            stopPlaying()
            playButton?.isActivated = false
        }

        if (view.isActivated.not()) {
            playButton = view
            val parentView = view.parent as ConstraintLayout
            seekBar = parentView.findViewById(R.id.seekBar)
            audioTimeTextView = parentView.findViewById(R.id.audioTimeTextView)
            playFileName = message.audioFile
            stopPlaying()
            startPlaying()
            view.isActivated = true
        } else {
            pauseTime = player?.currentPosition
            stopPlaying()
            view.isActivated = false
        }
    }

    private fun setSeekBarTimer() {
        timer = Timer().apply { scheduleAtFixedRate(getTimerTask(), 0, 20) }
    }

    private fun getTimerTask() = object : TimerTask() {
        override fun run() {
            if (player == null) timer?.cancel() else activity.runOnUiThread { setSeekBarPosition() }
        }
    }

    private fun setSeekBarPosition() {
        val player = player ?: return
        val audioTimeTextView = audioTimeTextView ?: return
        seekBar?.progress = player.currentPosition * 100 / player.duration
        Utility.setAudioTimeMmSs(audioTimeTextView, player.currentPosition.toLong())
    }

    fun setAudioTime(seekBar: SeekBar, textView: TextView, duration: Long) {
        val time = duration * seekBar.progress / 100
        Utility.setAudioTimeMmSs(textView, time)
    }

    fun startPlaying() {
        player = MediaPlayer().apply {
            setOnCompletionListener { onCompleted() }
            try {
                setDataSource(playFileName); prepare(); start()
                seekTo(getTime(this)); setSeekBarTimer()
            } catch (e: IOException) {
                Log.e(ChatFragment.TAG, "prepare() failed")
            }
        }
    }

    private fun MediaPlayer.onCompleted() {
        audioTimeTextView?.let { Utility.setAudioTimeMmSs(it, duration.toLong()) }
        stopPlaying()
        playButton?.isActivated = false
        seekBar?.progress = 0
    }

    private fun getTime(mediaPlayer: MediaPlayer): Int {
        val seekBarProgress = seekBar?.progress ?: return 0
        return mediaPlayer.duration * seekBarProgress / 100
    }

    fun stopPlaying() {
        stopTimer()
        try {
            player?.apply { stop(); release() }
            player = null
        } catch (stopException: RuntimeException) {
            Log.e(ChatFragment.TAG, "stop() failed")
        }
    }

    fun stopTimer() {
        timer?.cancel(); timer = null
    }

    fun startRecording(): Boolean {
        val filePath = getRecordFilePath()
        if (filePath == null) {
            Log.e("AudioHelper", "Failed to get recording file path")
            Utility.makeText(activity, "Failed to create audio file")
            return false
        }

        Log.d("AudioHelper", "Recording file path: $filePath")

        // Check if we can write to the directory
        try {
            val testFile = java.io.File(filePath)
            val parentDir = testFile.parentFile

            if (parentDir == null || !parentDir.exists()) {
                Log.e("AudioHelper", "Parent directory doesn't exist: ${parentDir?.absolutePath}")
                Utility.makeText(activity, "Storage directory not available")
                return false
            }

            if (!parentDir.canWrite()) {
                Log.e("AudioHelper", "Cannot write to directory: ${parentDir.absolutePath}")
                Utility.makeText(activity, "Cannot write to storage")
                return false
            }

            // Try to create the file to ensure we can actually write to it
            if (!testFile.createNewFile() && !testFile.exists()) {
                Log.e("AudioHelper", "Cannot create audio file: $filePath")
                Utility.makeText(activity, "Cannot create audio file")
                return false
            }

        } catch (e: Exception) {
            Log.e("AudioHelper", "Error with file operations", e)
            Utility.makeText(activity, "File system error: ${e.message}")
            return false
        }

        // Request audio focus before recording
        val focusResult = audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )

        Log.d("AudioHelper", "Audio focus request result: $focusResult")

        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e("AudioHelper", "Audio focus not granted - microphone may be in use")
            Utility.makeText(activity, "Microphone is busy. Please close other audio apps and try again.")
            return false
        }

        return try {
            // Clean up any existing recorder first
            recorder?.apply {
                try { stop() } catch (e: Exception) { }
                try { release() } catch (e: Exception) { }
            }
            recorder = null

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(filePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }

            recorderDuration = System.currentTimeMillis()
            isRecording = true
            true
        } catch (e: IOException) {
            Log.e(ChatFragment.TAG, "MediaRecorder prepare failed", e)
            Utility.makeText(activity, "Failed to prepare recording: ${e.message}")
            abandonAudioFocus()
            false
        } catch (e: IllegalStateException) {
            Log.e(ChatFragment.TAG, "MediaRecorder start failed", e)
            Utility.makeText(activity, "Microphone unavailable. Please close other audio apps.")
            abandonAudioFocus()
            false
        } catch (e: RuntimeException) {
            Log.e(ChatFragment.TAG, "MediaRecorder initialization failed", e)
            Utility.makeText(activity, "Audio recording not supported: ${e.message}")
            abandonAudioFocus()
            false
        }
    }

    private fun getRecordFilePath(): String? {
        return fileHelper.createAudioMediaFile()?.absolutePath.apply { recordFileName = this }
    }

    fun stopRecording() {
        recorderDuration?.let { recorderDuration = System.currentTimeMillis().minus(it) }
        try {
            recorder?.apply { stop(); release() }
            recorder = null
        } catch (stopException: RuntimeException) {
            Log.e(ChatFragment.TAG, "stop() failed")
        }
        isRecording = false
        abandonAudioFocus()
    }

    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocus(audioFocusChangeListener)
    }

    fun onStop() {
        stopPlaying()
        stopRecording()
        abandonAudioFocus()
    }
}