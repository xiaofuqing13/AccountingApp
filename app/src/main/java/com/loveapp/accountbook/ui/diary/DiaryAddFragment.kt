package com.loveapp.accountbook.ui.diary

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.provider.Settings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.load
import java.util.*

class DiaryAddFragment : Fragment() {

    private val viewModel: DiaryViewModel by activityViewModels()
    private var titleClickCount = 0
    private var currentLocation = ""
    private var editRowIndex = -1
    private var isEditMode = false

    // 录音相关
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentAudioFileName: String? = null
    private var recordingStartTime = 0L
    private var lastRecordingDurationMs = 0L
    private var hasValidRecording = false
    private val timerHandler = Handler(Looper.getMainLooper())

    // 拍照相关
    private var cameraImageFileName: String? = null

    private lateinit var etContent: EditText
    private lateinit var contentPreview: LinearLayout
    private val existingMediaMarkers = mutableListOf<String>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationTimeoutHandler = Handler(Looper.getMainLooper())

    // ===== ActivityResult Launchers =====

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val fileName = DiaryMediaManager.copyImageToPrivate(requireContext(), uri)
        if (fileName != null) {
            insertImageMarker(fileName)
        } else {
            Toast.makeText(requireContext(), "图片添加失败", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageFileName != null) {
            insertImageMarker(cameraImageFileName!!)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showRecordingDialog()
        else Toast.makeText(requireContext(), "需要麦克风权限才能录音", Toast.LENGTH_SHORT).show()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocation()
        else showManualLocationDialog("权限被拒绝，请手动输入位置")
    }

    // ===== Lifecycle =====

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<EditText>(R.id.et_title)
        etContent = view.findViewById(R.id.et_content)
        contentPreview = view.findViewById(R.id.content_preview)
        val tagDate = view.findViewById<TextView>(R.id.tag_date)
        val tagWeather = view.findViewById<TextView>(R.id.tag_weather)
        val tagMood = view.findViewById<TextView>(R.id.tag_mood)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        tagDate.text = "${DateUtils.today()} ${DateUtils.currentTime()}"
        tagWeather.text = "晴"
        tagMood.text = "开心"

        // 天气/心情选项（编辑模式需要在检测之前声明）
        val weatherOptions = arrayOf("晴", "多云", "雨", "雪", "雾", "彩虹")
        val weatherIcons = intArrayOf(R.drawable.ic_weather_sunny, R.drawable.ic_weather_cloudy, R.drawable.ic_weather_rainy, R.drawable.ic_weather_snowy, R.drawable.ic_weather_foggy, R.drawable.ic_weather_rainbow)
        var selectedWeather = 0
        val moodOptions = arrayOf("开心", "愉快", "平静", "难过", "生气", "疲惫", "思考", "充实")
        val moodIcons = intArrayOf(R.drawable.ic_mood_happy, R.drawable.ic_mood_pleasant, R.drawable.ic_mood_calm, R.drawable.ic_mood_sad, R.drawable.ic_mood_angry, R.drawable.ic_mood_tired, R.drawable.ic_mood_thinking, R.drawable.ic_mood_fulfilled)
        var selectedMood = 0

        // 检测编辑模式
        editRowIndex = arguments?.getInt("entryRowIndex", -1) ?: -1
        isEditMode = editRowIndex >= 0

        if (isEditMode) {
            // 编辑模式：修改标题栏文字
            val headerTitle = (view.findViewById<View>(R.id.btn_back).parent as ViewGroup).getChildAt(1) as TextView
            headerTitle.text = getString(R.string.diary_edit)

            val editTitle = arguments?.getString("entryTitle", "") ?: ""
            val editContent = arguments?.getString("entryContent", "") ?: ""
            val editWeather = arguments?.getString("entryWeather", "") ?: ""
            val editMood = arguments?.getString("entryMood", "") ?: ""
            val editLocation = arguments?.getString("entryLocation", "") ?: ""
            val editDate = arguments?.getString("entryDate", "") ?: ""

            etTitle.setText(editTitle)

            // 分离媒体标记和纯文本
            val mediaPattern = Regex("\\[(IMG|AUDIO):(.+?)]")
            existingMediaMarkers.clear()
            mediaPattern.findAll(editContent).forEach { match ->
                existingMediaMarkers.add(match.value)
            }

            // EditText 只显示纯文本（去掉媒体标记，清理多余空行）
            val pureText = editContent
                .replace(mediaPattern, "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
            etContent.setText(pureText)

            // 渲染媒体内容到预览区
            if (existingMediaMarkers.isNotEmpty()) {
                contentPreview.visibility = View.VISIBLE
                refreshMediaPreview()
            }

            currentLocation = editLocation

            // 设置天气标签（找到匹配的选项）
            if (editWeather.isNotEmpty()) {
                val weatherIdx = weatherOptions.indexOfFirst { it.contains(editWeather) }
                if (weatherIdx >= 0) {
                    selectedWeather = weatherIdx
                    tagWeather.text = weatherOptions[weatherIdx]
                } else {
                    tagWeather.text = editWeather
                }
            }

            // 设置心情标签
            if (editMood.isNotEmpty()) {
                val moodIdx = moodOptions.indexOfFirst { it.startsWith(editMood) }
                if (moodIdx >= 0) {
                    selectedMood = moodIdx
                    tagMood.text = moodOptions[moodIdx]
                } else {
                    tagMood.text = editMood
                }
            }

            // 设置日期（编辑模式保留原日期）
            if (editDate.isNotEmpty()) {
                tagDate.text = editDate
            }
        }

        // 天气选择
        tagWeather.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("选择天气")
                .setSingleChoiceItems(weatherOptions, selectedWeather) { dialog, which ->
                    selectedWeather = which
                    tagWeather.text = weatherOptions[which]
                    dialog.dismiss()
                }
                .show()
        }

        // 心情选择器（内联横向滚动 Chip）
        val moodSelector = view.findViewById<HorizontalScrollView>(R.id.mood_selector)
        val moodChipGroup = view.findViewById<ChipGroup>(R.id.mood_chip_group)
        setupMoodSelector(moodChipGroup, moodOptions, moodIcons, selectedMood, tagMood) { newIndex ->
            selectedMood = newIndex
        }

        tagMood.setOnClickListener {
            if (moodSelector.visibility == View.GONE) {
                moodSelector.visibility = View.VISIBLE
                moodSelector.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 200 })
            } else {
                val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 150 }
                fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(a: android.view.animation.Animation?) {}
                    override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(a: android.view.animation.Animation?) {
                        moodSelector.visibility = View.GONE
                    }
                })
                moodSelector.startAnimation(fadeOut)
            }
        }

        // 自动保存：恢复草稿（编辑模式不恢复）
        if (!isEditMode) {
            val hasDraft = DraftManager.restoreDraft(requireContext(), etTitle, DraftManager.KEY_DIARY_TITLE)
            DraftManager.restoreDraft(requireContext(), etContent, DraftManager.KEY_DIARY_CONTENT)
            if (hasDraft) Toast.makeText(requireContext(), "已恢复上次编辑的草稿", Toast.LENGTH_SHORT).show()
        }

        // 自动保存：绑定输入监听
        DraftManager.bindAutoSave(requireContext(), etTitle, DraftManager.KEY_DIARY_TITLE)
        DraftManager.bindAutoSave(requireContext(), etContent, DraftManager.KEY_DIARY_CONTENT)

        // 彩蛋: 标题连点3次
        etTitle.setOnClickListener {
            titleClickCount++
            if (titleClickCount >= 3) {
                titleClickCount = 0
                EasterEggManager.showLovePopup(requireContext(),
                    LoveWord("📝", "写给你的日记", "以后我们的每一篇日记，\n都有两个人的温度。\n\n你写你的心情，\n我写我有多喜欢你。"))
            }
        }

        // ===== 底部工具栏 =====

        // 📷 拍照/选图
        view.findViewById<View>(R.id.btn_photo).setOnClickListener {
            val options = arrayOf("拍照", "从相册选择")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("添加图片")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            if (ContextCompat.checkSelfPermission(requireContext(),
                                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchCamera()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        1 -> pickImageLauncher.launch("image/*")
                    }
                }
                .show()
        }

        // 🎙️ 录音
        view.findViewById<View>(R.id.btn_voice).setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }
            showRecordingDialog()
        }

        // 📍 定位
        view.findViewById<View>(R.id.btn_location).setOnClickListener {
            val hasFine = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                fetchLocation()
            } else {
                locationPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        // 🏷️ 标签
        view.findViewById<View>(R.id.btn_tag).setOnClickListener {
            showTagDialog()
        }

        // 🗑️ 一键清空
        view.findViewById<View>(R.id.btn_clear).setOnClickListener {
            if (etTitle.text.isNullOrEmpty() && etContent.text.isNullOrEmpty() && existingMediaMarkers.isEmpty()) {
                Toast.makeText(requireContext(), "已经是空的啦~", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("确认清空")
                .setMessage("清空所有内容？（标题、正文、图片、语音、位置）")
                .setPositiveButton("清空") { _, _ ->
                    etTitle.setText("")
                    etContent.setText("")
                    existingMediaMarkers.clear()
                    contentPreview.removeAllViews()
                    contentPreview.visibility = View.GONE
                    currentLocation = ""
                    DraftManager.clearDrafts(requireContext(), "draft_diary_")
                    Toast.makeText(requireContext(), "已清空", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 返回 & 保存
        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 将媒体标记拼接到内容中
            val finalContent = if (existingMediaMarkers.isNotEmpty()) {
                val mediaBlock = existingMediaMarkers.joinToString("\n")
                if (content.isNotEmpty()) "$content\n\n$mediaBlock" else mediaBlock
            } else {
                content
            }
            val weatherText = tagWeather.text.toString()
            val moodText = tagMood.text.toString()
            if (isEditMode) {
                viewModel.updateDiary(DiaryEntry(
                    date = arguments?.getString("entryDate", "") ?: DateUtils.todayWithTime(),
                    title = title,
                    content = finalContent,
                    weather = weatherText,
                    mood = moodText,
                    location = currentLocation,
                    rowIndex = editRowIndex
                ))
            } else {
                viewModel.addDiary(DiaryEntry(
                    date = DateUtils.todayWithTime(),
                    title = title,
                    content = finalContent,
                    weather = weatherText,
                    mood = moodText,
                    location = currentLocation
                ))
            }
            DraftManager.clearDrafts(requireContext(), "draft_diary_")
            Toast.makeText(requireContext(), "日记保存成功", Toast.LENGTH_SHORT).show()
            if ((0..2).random() == 0) {
                EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggDiarySave)
            }
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording(discard = true)
        timerHandler.removeCallbacksAndMessages(null)
        locationTimeoutHandler.removeCallbacksAndMessages(null)
    }

    // ===== 心情选择器 =====

    private fun setupMoodSelector(
        chipGroup: ChipGroup,
        moodOptions: Array<String>,
        moodIcons: IntArray,
        initialSelected: Int,
        tagMood: TextView,
        onSelected: (Int) -> Unit
    ) {
        chipGroup.removeAllViews()
        val ctx = requireContext()
        val pinkColor = ContextCompat.getColor(ctx, R.color.pink_primary)
        val chipBgColor = ContextCompat.getColor(ctx, R.color.tag_chip_bg)
        val whiteColor = ContextCompat.getColor(ctx, R.color.pink_card)

        moodOptions.forEachIndexed { index, mood ->
            val chip = Chip(ctx).apply {
                text = mood
                isCheckable = true
                isCheckedIconVisible = false
                textSize = 13f
                chipIconSize = resources.getDimension(R.dimen.icon_size_small)
                if (index < moodIcons.size) {
                    chipIcon = ContextCompat.getDrawable(ctx, moodIcons[index])
                    isChipIconVisible = true
                    chipIconTint = android.content.res.ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf()
                        ),
                        intArrayOf(whiteColor, pinkColor)
                    )
                }
                @Suppress("DEPRECATION")
                chipCornerRadius = resources.getDimension(R.dimen.radius_lg)
                chipStrokeWidth = 0f
                setEnsureMinTouchTargetSize(false)
                chipBackgroundColor = android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(pinkColor, chipBgColor)
                )
                setTextColor(android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(whiteColor, pinkColor)
                ))
                isChecked = index == initialSelected
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    tagMood.text = mood
                    onSelected(index)
                }
            }
            chipGroup.addView(chip)
        }
    }

    // ===== 编辑模式媒体预览 =====

    private fun renderMediaPreview(content: String) {
        contentPreview.removeAllViews()
        val ctx = requireContext()
        val pattern = Regex("\\[(IMG|AUDIO)\\s*:(.+?)]", RegexOption.IGNORE_CASE)

        pattern.findAll(content).forEach { match ->
            when (match.groupValues[1].uppercase(Locale.ROOT)) {
                "IMG" -> {
                    val fileName = DiaryMediaManager.normalizeStoredFileName(match.groupValues[2])
                    val file = DiaryMediaManager.resolveImageFile(ctx, match.groupValues[2])
                    if (file.exists()) {
                        val maxH = (280 * ctx.resources.displayMetrics.density).toInt()
                        contentPreview.addView(ImageView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 8, 0, 8) }
                            adjustViewBounds = true
                            maxHeight = maxH
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            clipToOutline = true
                            outlineProvider = object : android.view.ViewOutlineProvider() {
                                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                                    outline.setRoundRect(0, 0, view.width, view.height, 12f * ctx.resources.displayMetrics.density)
                                }
                            }
                            load(file) {
                                crossfade(true)
                                size(1080, 810)
                            }
                        })
                    } else {
                        contentPreview.addView(LinearLayout(ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER_VERTICAL
                            setPadding(24, 32, 24, 32)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 8, 0, 8) }
                            background = ContextCompat.getDrawable(ctx, R.drawable.bg_tag_chip)
                            alpha = 0.5f
                            addView(ImageView(ctx).apply {
                                setImageResource(R.drawable.ic_camera)
                                imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.text_secondary))
                                layoutParams = LinearLayout.LayoutParams(48, 48).apply { setMargins(8, 8, 16, 8) }
                            })
                            addView(TextView(ctx).apply {
                                text = "\u56FE\u7247\u6587\u4EF6"
                                textSize = 14f
                                setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                            })
                        })
                    }
                }
                "AUDIO" -> {
                    val fileName = DiaryMediaManager.normalizeStoredFileName(match.groupValues[2])
                    val file = DiaryMediaManager.resolveAudioFile(ctx, match.groupValues[2])
                    if (file.exists()) {
                        contentPreview.addView(createAudioPlayerView(file))
                    } else {
                        contentPreview.addView(createMissingAudioView(fileName))
                    }
                }
            }
        }
    }

    private fun refreshMediaPreview() {
        contentPreview.removeAllViews()
        val ctx = requireContext()
        val pattern = Regex("\\[(IMG|AUDIO)\\s*:(.+?)]", RegexOption.IGNORE_CASE)

        existingMediaMarkers.forEach { marker ->
            val match = pattern.find(marker) ?: return@forEach
            when (match.groupValues[1].uppercase(Locale.ROOT)) {
                "IMG" -> {
                    val fileName = DiaryMediaManager.normalizeStoredFileName(match.groupValues[2])
                    val file = DiaryMediaManager.resolveImageFile(ctx, match.groupValues[2])
                    if (file.exists()) {
                        val maxH = (280 * ctx.resources.displayMetrics.density).toInt()
                        contentPreview.addView(ImageView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 8, 0, 8) }
                            adjustViewBounds = true
                            maxHeight = maxH
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            clipToOutline = true
                            outlineProvider = object : android.view.ViewOutlineProvider() {
                                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                                    outline.setRoundRect(0, 0, view.width, view.height, 12f * ctx.resources.displayMetrics.density)
                                }
                            }
                            load(file) { crossfade(true); size(1080, 810) }
                        })
                    }
                }
                "AUDIO" -> {
                    val fileName = DiaryMediaManager.normalizeStoredFileName(match.groupValues[2])
                    val file = DiaryMediaManager.resolveAudioFile(ctx, match.groupValues[2])
                    if (file.exists()) {
                        contentPreview.addView(createAudioPlayerView(file))
                    } else {
                        contentPreview.addView(createMissingAudioView(fileName))
                    }
                }
            }
        }
    }

    private fun createMissingAudioView(fileName: String): View {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
            background = ContextCompat.getDrawable(ctx, R.drawable.bg_tag_chip)
            alpha = 0.75f

            addView(ImageView(ctx).apply {
                setImageResource(R.drawable.ic_love_shine)
                imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(44, 44).apply { setMargins(4, 4, 12, 4) }
            })
            addView(TextView(ctx).apply {
                text = "语音文件不存在（$fileName）\n请重新录音后保存日记"
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            })
        }
    }

    private fun createAudioPlayerView(file: java.io.File): View {
        val ctx = requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.view_audio_player, null)
        view.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val btnPlayPause = view.findViewById<ImageView>(R.id.btn_play_pause)
        val seekBar = view.findViewById<android.widget.SeekBar>(R.id.seek_bar)
        val tvDuration = view.findViewById<TextView>(R.id.tv_duration)

        var mediaPlayer: android.media.MediaPlayer? = null
        var playing = false
        val handler = Handler(Looper.getMainLooper())

        // 获取音频时长
        try {
            val mp = android.media.MediaPlayer()
            if (!DiaryMediaManager.configurePlayerDataSource(mp, file)) {
                mp.release()
                tvDuration.text = "不可播放"
                return view
            }
            mp.prepare()
            tvDuration.text = formatAudioDuration(mp.duration)
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
                        tvDuration.text = "${formatAudioDuration(current)} / ${formatAudioDuration(total)}"
                        handler.postDelayed(this, 200)
                    }
                }
            }
        }

        val togglePlayPause = {
            if (!playing) {
                try {
                    mediaPlayer = android.media.MediaPlayer().apply {
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
                            seekBar.progress = 0
                            tvDuration.text = formatAudioDuration(it.duration)
                            handler.removeCallbacks(updateRunnable)
                            it.release()
                            mediaPlayer = null
                        }
                    }
                    playing = true
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                    handler.post(updateRunnable)
                } catch (_: Exception) {
                    Toast.makeText(ctx, "语音播放失败，请重新录音", Toast.LENGTH_SHORT).show()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    playing = false
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                }
            } else {
                handler.removeCallbacks(updateRunnable)
                mediaPlayer?.release()
                mediaPlayer = null
                playing = false
                btnPlayPause.setImageResource(R.drawable.ic_play)
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
                mediaPlayer?.release()
                mediaPlayer = null
                playing = false
            }
        })

        return view
    }

    private fun formatAudioDuration(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "$min:${String.format("%02d", sec)}"
    }

    // ===== 📷 拍照/图片 =====

    private fun launchCamera() {
        cameraImageFileName = DiaryMediaManager.generateImageFileName()
        val file = DiaryMediaManager.getImageFile(requireContext(), cameraImageFileName!!)
        val uri = FileProvider.getUriForFile(requireContext(),
            "${requireContext().packageName}.fileprovider", file)
        takePictureLauncher.launch(uri)
    }

    private fun insertImageMarker(fileName: String) {
        val marker = "\n[IMG:$fileName]\n"
        val start = etContent.selectionStart.coerceAtLeast(0)
        etContent.text.insert(start, marker)
        // 在 EditText 中显示缩略图
        renderImageSpan(start + 1, start + marker.length - 1, fileName)
        Toast.makeText(requireContext(), "图片已添加", Toast.LENGTH_SHORT).show()
    }

    private fun renderImageSpan(start: Int, end: Int, fileName: String) {
        val file = DiaryMediaManager.getImageFile(requireContext(), fileName)
        if (!file.exists()) return
        try {
            val maxWidth = (etContent.width - etContent.paddingLeft - etContent.paddingRight)
                .coerceAtLeast(300)
            val bitmap = decodeSampledBitmap(file.absolutePath, maxWidth, 600)
            val drawable = BitmapDrawable(resources, bitmap).apply {
                setBounds(0, 0, bitmap.width, bitmap.height)
            }
            val spannable = etContent.text as SpannableStringBuilder
            if (end <= spannable.length) {
                spannable.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } catch (_: Exception) {}
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val h = options.outHeight
        val w = options.outWidth
        var inSampleSize = 1
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // ===== 🎙️ 录音 =====

    private fun showRecordingDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_voice_record, null)
        val tvTimer = dialogView.findViewById<TextView>(R.id.tv_timer)
        val tvHint = dialogView.findViewById<TextView>(R.id.tv_record_hint)
        val btnToggle = dialogView.findViewById<TextView>(R.id.btn_record_toggle)
        tvTimer.text = "00:00"
        tvHint.text = "点击开始录音，录完后再点完成并插入"

        val timerRunnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                    val min = elapsed / 60
                    val sec = elapsed % 60
                    tvTimer.text = String.format("%02d:%02d", min, sec)
                    timerHandler.postDelayed(this, 500)
                }
            }
        }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("录音")
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("取消", null)
            .setPositiveButton("完成并插入", null)
            .create()

        dialog.setOnShowListener {
            val btnCancel = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
            val btnDone = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            btnDone.isEnabled = false

            btnCancel.setOnClickListener {
                stopRecording(discard = true)
                timerHandler.removeCallbacksAndMessages(null)
                dialog.dismiss()
            }

            btnDone.setOnClickListener {
                if (isRecording) {
                    val ok = stopRecording(discard = false)
                    timerHandler.removeCallbacksAndMessages(null)
                    btnToggle.text = "\uD83C\uDFA4 开始录音"
                    if (!ok) {
                        btnDone.isEnabled = false
                        tvHint.text = "录音无效，请重新录制"
                        return@setOnClickListener
                    }
                }

                val fileName = currentAudioFileName
                if (!hasValidRecording || fileName.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "请先完成一段有效录音", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val marker = "[AUDIO:$fileName]"
                if (marker !in existingMediaMarkers) {
                    existingMediaMarkers.add(marker)
                }
                contentPreview.visibility = View.VISIBLE
                refreshMediaPreview()
                Toast.makeText(requireContext(), "语音已添加（${formatAudioDuration(lastRecordingDurationMs.toInt())}）", Toast.LENGTH_SHORT).show()

                currentAudioFileName = null
                hasValidRecording = false
                lastRecordingDurationMs = 0L
                dialog.dismiss()
            }

            btnToggle.setOnClickListener {
                if (!isRecording) {
                    // 开始新录音前，清理上一段未插入的临时音频
                    if (hasValidRecording && !currentAudioFileName.isNullOrBlank()) {
                        stopRecording(discard = true)
                    }
                    if (!startRecording()) return@setOnClickListener
                    btnDone.isEnabled = false
                    tvHint.text = "录音中，点击“停止录音”结束"
                    btnToggle.text = "\u23F9\uFE0F 停止录音"
                    timerHandler.post(timerRunnable)
                } else {
                    val ok = stopRecording(discard = false)
                    timerHandler.removeCallbacksAndMessages(null)
                    btnToggle.text = "\uD83C\uDFA4 开始录音"
                    btnDone.isEnabled = ok
                    tvHint.text = if (ok) "录音完成，点击“完成并插入”" else "录音无效，请重新录制"
                }
            }
        }

        dialog.setOnDismissListener {
            timerHandler.removeCallbacksAndMessages(null)
            if (isRecording) stopRecording(discard = true)
        }

        dialog.show()
    }

    @Suppress("DEPRECATION")
    private fun startRecording(): Boolean {
        currentAudioFileName = DiaryMediaManager.generateAudioFileName()
        hasValidRecording = false
        lastRecordingDurationMs = 0L
        val file = DiaryMediaManager.getAudioFile(requireContext(), currentAudioFileName!!)
        return try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            currentAudioFileName = null
            file.delete()
            Toast.makeText(requireContext(), "录音启动失败，请重试", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun stopRecording(discard: Boolean): Boolean {
        var stopSucceeded = true
        if (isRecording || mediaRecorder != null) {
            try {
                mediaRecorder?.stop()
            } catch (_: Exception) {
                stopSucceeded = false
            } finally {
                try {
                    mediaRecorder?.release()
                } catch (_: Exception) { }
            }
            mediaRecorder = null
            isRecording = false
            if (stopSucceeded) {
                lastRecordingDurationMs = System.currentTimeMillis() - recordingStartTime
            }
        }

        val ctx = context ?: return false
        val fileName = currentAudioFileName
        if (fileName.isNullOrBlank()) return false
        val file = DiaryMediaManager.getAudioFile(ctx, fileName)

        val tooShort = lastRecordingDurationMs in 1 until 1000
        val invalidFile = !file.exists() || file.length() < 1024L
        if (discard || !stopSucceeded || tooShort || invalidFile) {
            file.delete()
            if (!discard) {
                val msg = when {
                    !stopSucceeded -> "录音保存失败，请重试"
                    tooShort -> "录音时间太短，请至少录 1 秒"
                    else -> "录音文件异常，请重试"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
            hasValidRecording = false
            lastRecordingDurationMs = 0L
            currentAudioFileName = null
            return false
        }

        hasValidRecording = true
        return true
    }

    // ===== 📍 定位 =====

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        // 检查定位服务是否开启（小米手机用户经常关闭定位）
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsEnabled && !networkEnabled) {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("定位服务未开启")
                .setMessage("请在系统设置中开启定位服务，以便获取当前位置。")
                .setPositiveButton("去设置") { _, _ ->
                    try {
                        startActivity(android.content.Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    } catch (_: Exception) {}
                }
                .setNegativeButton("手动输入") { _, _ ->
                    showManualLocationDialog("请手动输入位置")
                }
                .show()
            return
        }

        Toast.makeText(requireContext(), "正在获取位置...", Toast.LENGTH_SHORT).show()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (!isAdded) return@addOnSuccessListener
            if (location != null) {
                reverseGeocode(location.latitude, location.longitude)
            } else {
                // lastLocation 为 null，请求一次新定位（设置15秒超时）
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMaxUpdates(1)
                    .setDurationMillis(15000)
                    .build()
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        locationTimeoutHandler.removeCallbacksAndMessages(null)
                        if (!isAdded) return
                        val loc = result.lastLocation
                        if (loc != null) {
                            reverseGeocode(loc.latitude, loc.longitude)
                        } else {
                            showManualLocationDialog("无法获取位置，请手动输入")
                        }
                    }
                }
                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                // 15秒超时兜底，防止回调永远不触发
                locationTimeoutHandler.postDelayed({
                    fusedLocationClient.removeLocationUpdates(callback)
                    if (isAdded) {
                        showManualLocationDialog("定位超时，请检查定位设置或手动输入")
                    }
                }, 15000)
            }
        }.addOnFailureListener {
            if (!isAdded) return@addOnFailureListener
            showManualLocationDialog("定位失败，请手动输入")
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        if (!isAdded) return
        val ctx = requireContext().applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val addressList = mutableListOf<String>()
            try {
                @Suppress("DEPRECATION")
                val results = Geocoder(ctx, Locale.CHINA).getFromLocation(lat, lng, 10)
                results?.forEach { addr ->
                    val name = buildString {
                        addr.subLocality?.let { append(it) }
                        addr.thoroughfare?.let { append(it) }
                        addr.featureName?.let { f ->
                            if (f != addr.thoroughfare) append(f)
                        }
                    }.ifEmpty { addr.getAddressLine(0) ?: "" }
                    if (name.isNotEmpty() && name !in addressList) {
                        addressList.add(name)
                    }
                }
            } catch (_: Exception) {}

            if (addressList.isEmpty()) {
                addressList.add("($lat, $lng)")
            }

            withContext(Dispatchers.Main) {
                if (isAdded) showLocationPickerDialog(addressList)
            }
        }
    }

    private fun showLocationPickerDialog(addresses: List<String>) {
        if (!isAdded) return
        val items = addresses.toTypedArray()
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("\uD83D\uDCCD 选择附近位置")
            .setItems(items) { _, which ->
                currentLocation = items[which]
                Toast.makeText(requireContext(), "位置已设置：$currentLocation", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("手动输入") { _, _ ->
                showManualLocationDialog("手动输入位置")
            }
            .setNegativeButton("清除位置") { _, _ ->
                currentLocation = ""
                Toast.makeText(requireContext(), "位置已清除", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showManualLocationDialog(hint: String) {
        if (!isAdded) return
        val input = EditText(requireContext()).apply {
            this.hint = "输入当前位置，如：星巴克咖啡厅"
            setText(currentLocation)
            setPadding(60, 40, 60, 40)
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("\uD83D\uDCCD $hint")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                currentLocation = input.text.toString().trim()
                if (currentLocation.isNotEmpty()) {
                    Toast.makeText(requireContext(), "位置已设置：$currentLocation", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ===== 🏷️ 标签 =====

    private fun showTagDialog() {
        val prefs = requireContext().getSharedPreferences("diary_tags", Context.MODE_PRIVATE)
        val customTags = prefs.getStringSet("custom_tags", emptySet())!!.toList().sorted()
        val defaultTags = listOf("#日常", "#美食", "#旅行", "#学习", "#运动", "#约会", "#工作", "#心情", "#纪念日", "#灵感")
        val allTags = (defaultTags + customTags.map { "#$it" }).distinct().toTypedArray()
        val checked = BooleanArray(allTags.size)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("\uD83C\uDFF7\uFE0F 选择标签")
            .setMultiChoiceItems(allTags, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("插入") { _, _ ->
                val selected = allTags.filterIndexed { i, _ -> checked[i] }
                if (selected.isNotEmpty()) {
                    val tagText = "\n${selected.joinToString(" ")}\n"
                    val start = etContent.selectionStart.coerceAtLeast(0)
                    etContent.text.insert(start, tagText)
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("+ 新标签", null)
            .create()

        dialog.setOnShowListener {
            // 重写中性按钮点击，弹出添加对话框后自动重新打开标签选择
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                dialog.dismiss()
                showAddCustomTagDialog { showTagDialog() }
            }
        }
        dialog.show()

        // 在按钮栏上方添加"管理标签"入口
        if (customTags.isNotEmpty()) {
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).text = "管理标签"
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
                showManageTagsDialog()
            }
        }
    }

    private fun showAddCustomTagDialog(onAdded: (() -> Unit)? = null) {
        val input = EditText(requireContext()).apply {
            hint = "输入新标签名（不需要#号）"
            setPadding(60, 40, 60, 40)
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("添加自定义标签")
            .setView(input)
            .setPositiveButton("添加") { _, _ ->
                val tag = input.text.toString().trim()
                if (tag.isNotEmpty()) {
                    val prefs = requireContext().getSharedPreferences("diary_tags", Context.MODE_PRIVATE)
                    val existing = prefs.getStringSet("custom_tags", emptySet())!!.toMutableSet()
                    existing.add(tag)
                    prefs.edit().putStringSet("custom_tags", existing).apply()
                    Toast.makeText(requireContext(), "标签 #$tag 已添加", Toast.LENGTH_SHORT).show()
                    onAdded?.invoke()
                }
            }
            .setNegativeButton("取消") { _, _ ->
                // 取消时也回到标签选择对话框
                onAdded?.invoke()
            }
            .show()
    }

    // 管理自定义标签：查看和删除
    private fun showManageTagsDialog() {
        val prefs = requireContext().getSharedPreferences("diary_tags", Context.MODE_PRIVATE)
        val customTags = prefs.getStringSet("custom_tags", emptySet())!!.toList().sorted()

        if (customTags.isEmpty()) {
            Toast.makeText(requireContext(), "暂无自定义标签", Toast.LENGTH_SHORT).show()
            showTagDialog()
            return
        }

        val tagArray = customTags.map { "#$it" }.toTypedArray()
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("\uD83D\uDDD1\uFE0F 管理自定义标签（点击删除）")
            .setItems(tagArray) { _, which ->
                val tagToDelete = customTags[which]
                // 确认删除
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("删除标签")
                    .setMessage("确定删除标签 #$tagToDelete 吗？")
                    .setPositiveButton("删除") { _, _ ->
                        val existing = prefs.getStringSet("custom_tags", emptySet())!!.toMutableSet()
                        existing.remove(tagToDelete)
                        prefs.edit().putStringSet("custom_tags", existing).apply()
                        Toast.makeText(requireContext(), "标签 #$tagToDelete 已删除", Toast.LENGTH_SHORT).show()
                        // 删除后重新打开管理对话框
                        showManageTagsDialog()
                    }
                    .setNegativeButton("取消") { _, _ ->
                        showManageTagsDialog()
                    }
                    .show()
            }
            .setPositiveButton("返回选择标签") { _, _ ->
                showTagDialog()
            }
            .setNegativeButton("关闭", null)
            .show()
    }
}
