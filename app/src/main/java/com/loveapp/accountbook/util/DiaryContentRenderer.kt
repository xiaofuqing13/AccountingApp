package com.loveapp.accountbook.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import coil.load
import com.loveapp.accountbook.R
import com.loveapp.accountbook.ui.widget.VoiceWaveView
import java.util.Locale

object DiaryContentRenderer {

    fun renderToContainer(context: Context, container: LinearLayout, content: String, clearFirst: Boolean = true) {
        if (clearFirst) container.removeAllViews()
        val pattern = Regex("\\[(IMG|AUDIO)\\s*:(.+?)]", RegexOption.IGNORE_CASE)
        var lastEnd = 0

        pattern.findAll(content).forEach { match ->
            if (match.range.first > lastEnd) {
                val text = content.substring(lastEnd, match.range.first).trim()
                if (text.isNotEmpty()) {
                    container.addView(TextView(context).apply {
                        this.text = text
                        textSize = 15f
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        setLineSpacing(0f, 1.5f)
                        setPadding(0, 8, 0, 8)
                    })
                }
            }

            when (match.groupValues[1].uppercase(Locale.ROOT)) {
                "IMG" -> {
                    val fileName = DiaryMediaManager.normalizeStoredFileName(match.groupValues[2])
                    val file = DiaryMediaManager.resolveImageFile(context, match.groupValues[2])
                    if (file.exists()) {
                        val maxH = (300 * context.resources.displayMetrics.density).toInt()
                        container.addView(ImageView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 16, 0, 16) }
                            adjustViewBounds = true
                            maxHeight = maxH
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            clipToOutline = true
                            outlineProvider = object : android.view.ViewOutlineProvider() {
                                override fun getOutline(view: View, outline: android.graphics.Outline) {
                                    outline.setRoundRect(0, 0, view.width, view.height, 12f * context.resources.displayMetrics.density)
                                }
                            }
                            load(file) {
                                crossfade(true)
                                size(1080, 810)
                            }
                        })
                    } else {
                        container.addView(LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(24, 32, 24, 32)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 12, 0, 12) }
                            background = ContextCompat.getDrawable(context, R.drawable.bg_tag_chip)
                            alpha = 0.5f
                            addView(ImageView(context).apply {
                                setImageResource(R.drawable.ic_camera)
                                layoutParams = LinearLayout.LayoutParams(48, 48).apply { setMargins(8, 8, 16, 8) }
                                imageTintList = android.content.res.ColorStateList.valueOf(
                                    ContextCompat.getColor(context, R.color.text_secondary))
                            })
                            addView(TextView(context).apply {
                                text = "\u56FE\u7247\u6587\u4EF6"
                                textSize = 14f
                                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                            })
                        })
                    }
                }
                "AUDIO" -> {
                    val fileName = DiaryMediaManager.normalizeStoredFileName(match.groupValues[2])
                    val file = DiaryMediaManager.resolveAudioFile(context, match.groupValues[2])
                    if (file.exists()) {
                        container.addView(createAudioPlayerView(context, file))
                    } else {
                        container.addView(LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(24, 16, 24, 16)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 12, 0, 12) }
                            background = ContextCompat.getDrawable(context, R.drawable.bg_tag_chip)
                            alpha = 0.5f
                            addView(ImageView(context).apply {
                                setImageResource(R.drawable.ic_play)
                                layoutParams = LinearLayout.LayoutParams(48, 48).apply { setMargins(8, 8, 16, 8) }
                                imageTintList = android.content.res.ColorStateList.valueOf(
                                    ContextCompat.getColor(context, R.color.text_secondary))
                            })
                            addView(TextView(context).apply {
                                text = "\u8BED\u97F3\u6587\u4EF6\u4E0D\u5B58\u5728\uFF08$fileName\uFF09"
                                textSize = 14f
                                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                            })
                        })
                    }
                }
            }
            lastEnd = match.range.last + 1
        }

        if (lastEnd < content.length) {
            val text = content.substring(lastEnd).trim()
            if (text.isNotEmpty()) {
                container.addView(TextView(context).apply {
                    this.text = text
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setLineSpacing(0f, 1.5f)
                })
            }
        }
    }

    private fun createAudioPlayerView(context: Context, file: java.io.File): View {
        val view = android.view.LayoutInflater.from(context).inflate(R.layout.view_audio_player, null)
        view.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val btnPlayPause = view.findViewById<ImageView>(R.id.btn_play_pause)
        val seekBar = view.findViewById<android.widget.SeekBar>(R.id.seek_bar)
        val tvDuration = view.findViewById<TextView>(R.id.tv_duration)
        val voiceWave = view.findViewById<VoiceWaveView>(R.id.voice_wave)

        var mediaPlayer: MediaPlayer? = null
        var visualizer: Visualizer? = null
        var playing = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        // 获取音频时长
        try {
            val mp = MediaPlayer()
            if (!DiaryMediaManager.configurePlayerDataSource(mp, file)) {
                mp.release()
                tvDuration.text = "不可播放"
                return view
            }
            mp.prepare()
            val totalMs = mp.duration
            tvDuration.text = formatDuration(totalMs)
            mp.release()
        } catch (_: Exception) {
            tvDuration.text = "0:00"
        }

        val updateRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (playing) {
                        val current = mp.currentPosition
                        val total = mp.duration
                        seekBar.max = total
                        seekBar.progress = current
                        tvDuration.text = "${formatDuration(current)} / ${formatDuration(total)}"
                        handler.postDelayed(this, 200)
                    }
                }
            }
        }

        val togglePlayPause = {
            if (!playing) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        if (!DiaryMediaManager.configurePlayerDataSource(this, file)) {
                            throw IllegalStateException("setDataSource failed")
                        }
                        prepare()
                        start()
                        setOnCompletionListener {
                            playing = false
                            btnPlayPause.setImageResource(R.drawable.ic_play)
                            releaseVisualizer(visualizer); visualizer = null
                            voiceWave.reset()
                            seekBar.progress = 0
                            tvDuration.text = formatDuration(it.duration)
                            handler.removeCallbacks(updateRunnable)
                            it.release()
                            mediaPlayer = null
                        }
                    }
                    playing = true
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                    // 真实音频波形
                    visualizer = attachVisualizer(mediaPlayer!!, voiceWave)
                    handler.post(updateRunnable)
                } catch (_: Exception) {
                    Toast.makeText(context, "语音播放失败，请重新录音", Toast.LENGTH_SHORT).show()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    playing = false
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                }
            } else {
                handler.removeCallbacks(updateRunnable)
                releaseVisualizer(visualizer); visualizer = null
                mediaPlayer?.release()
                mediaPlayer = null
                playing = false
                btnPlayPause.setImageResource(R.drawable.ic_play)
                voiceWave.reset()
            }
        }
        btnPlayPause.setOnClickListener { togglePlayPause() }
        view.setOnClickListener { togglePlayPause() }

        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                handler.removeCallbacks(updateRunnable)
                releaseVisualizer(visualizer); visualizer = null
                mediaPlayer?.release()
                mediaPlayer = null
                playing = false
            }
        })

        return view
    }

    private fun attachVisualizer(mp: MediaPlayer, waveView: VoiceWaveView): Visualizer? {
        return try {
            Visualizer(mp.audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray, samplingRate: Int) {
                        var sum = 0.0
                        for (b in waveform) {
                            val sample = ((b.toInt() and 0xFF) - 128).toDouble()
                            sum += sample * sample
                        }
                        val rms = (kotlin.math.sqrt(sum / waveform.size) / 128.0).toFloat()
                        waveView.post { waveView.setAmplitude(rms * 3f) }
                    }
                    override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray, samplingRate: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
        } catch (_: Exception) {
            waveView.startPlayingAnimation()
            null
        }
    }

    private fun releaseVisualizer(vis: Visualizer?) {
        try {
            vis?.enabled = false
            vis?.release()
        } catch (_: Exception) {}
    }

    private fun formatDuration(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "$min:${String.format("%02d", sec)}"
    }

    fun getPlainPreview(content: String): String {
        return content
            .replace(DiaryMediaManager.IMG_PATTERN, "[图片]")
            .replace(DiaryMediaManager.AUDIO_PATTERN, "[语音]")
            .replace(Regex("\\s*\\n\\s*"), " ")
            .replace(Regex("[ \\t]{2,}"), " ")
            .trim()
    }
}
