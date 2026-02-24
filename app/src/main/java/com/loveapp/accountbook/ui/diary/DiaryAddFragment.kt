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
import android.content.res.ColorStateList
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.ShapeAppearanceModel
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.ui.widget.VoiceWaveView
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
    private val selectedTags = mutableListOf<String>()

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
        if (granted) silentFetchLocation()
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

        // 初始化标签图标
        updateTagIcon(tagWeather, weatherIcons[selectedWeather])
        updateTagIcon(tagMood, moodIcons[selectedMood])

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

            // 恢复标签
            val editTags = arguments?.getString("entryTags", "") ?: ""
            if (editTags.isNotEmpty()) {
                selectedTags.clear()
                selectedTags.addAll(editTags.split(",").filter { it.isNotBlank() })
            }

            // 设置天气标签（找到匹配的选项）
            if (editWeather.isNotEmpty()) {
                val weatherIdx = weatherOptions.indexOfFirst { it.contains(editWeather) }
                if (weatherIdx >= 0) {
                    selectedWeather = weatherIdx
                    tagWeather.text = weatherOptions[weatherIdx]
                    updateTagIcon(tagWeather, weatherIcons[weatherIdx])
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
                    updateTagIcon(tagMood, moodIcons[moodIdx])
                } else {
                    tagMood.text = editMood
                }
            }

            // 设置日期（编辑模式保留原日期）
            if (editDate.isNotEmpty()) {
                tagDate.text = editDate
            }
        }

        // 天气选择器（内联横向滚动 Chip）
        val weatherSelector = view.findViewById<HorizontalScrollView>(R.id.weather_selector)
        val weatherChipGroup = view.findViewById<ChipGroup>(R.id.weather_chip_group)
        setupWeatherSelector(weatherChipGroup, weatherOptions, weatherIcons, selectedWeather, tagWeather) { newIndex ->
            selectedWeather = newIndex
            updateTagIcon(tagWeather, weatherIcons[newIndex])
        }

        // 心情选择器（内联横向滚动 Chip）
        val moodSelector = view.findViewById<HorizontalScrollView>(R.id.mood_selector)
        val moodChipGroup = view.findViewById<ChipGroup>(R.id.mood_chip_group)
        setupMoodSelector(moodChipGroup, moodOptions, moodIcons, selectedMood, tagMood) { newIndex ->
            selectedMood = newIndex
            updateTagIcon(tagMood, moodIcons[newIndex])
        }

        tagWeather.setOnClickListener {
            moodSelector.visibility = View.GONE
            toggleSelector(weatherSelector)
        }

        tagMood.setOnClickListener {
            weatherSelector.visibility = View.GONE
            toggleSelector(moodSelector)
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

        // 自动定位（非编辑模式）
        if (!isEditMode) {
            autoFetchLocation()
        }

        // 彩蛋: 标题连点3次
        etTitle.setOnClickListener {
            titleClickCount++
            if (titleClickCount >= 3) {
                titleClickCount = 0
                EasterEggManager.showLovePopup(requireContext(),
                    LoveWord("diary", "写给你的日记", "以后我们的每一篇日记，\n都有两个人的温度。\n\n你写你的心情，\n我写我有多喜欢你。"))
            }
        }

        // ===== 底部工具栏 =====

        // 拍照/选图
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

        // 录音
        view.findViewById<View>(R.id.btn_voice).setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }
            showRecordingDialog()
        }

        // 标签
        view.findViewById<View>(R.id.btn_tag).setOnClickListener {
            showTagDialog()
        }

        // 一键清空
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
            val tagsText = selectedTags.joinToString(",")
            if (isEditMode) {
                viewModel.updateDiary(DiaryEntry(
                    date = arguments?.getString("entryDate", "") ?: DateUtils.todayWithTime(),
                    title = title,
                    content = finalContent,
                    weather = weatherText,
                    mood = moodText,
                    location = currentLocation,
                    tags = tagsText,
                    rowIndex = editRowIndex
                ))
            } else {
                viewModel.addDiary(DiaryEntry(
                    date = DateUtils.todayWithTime(),
                    title = title,
                    content = finalContent,
                    weather = weatherText,
                    mood = moodText,
                    location = currentLocation,
                    tags = tagsText
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

    // ===== 标签图标 + 选择器动画 =====

    private fun updateTagIcon(tag: TextView, iconRes: Int) {
        val drawable = ContextCompat.getDrawable(requireContext(), iconRes)?.mutate()?.apply {
            val size = (14 * resources.displayMetrics.density).toInt()
            setBounds(0, 0, size, size)
        }
        tag.setCompoundDrawables(drawable, null, null, null)
        tag.compoundDrawablePadding = (4 * resources.displayMetrics.density).toInt()
    }

    private fun toggleSelector(selector: HorizontalScrollView) {
        if (selector.visibility == View.GONE) {
            selector.visibility = View.VISIBLE
            selector.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 200 })
        } else {
            val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 150 }
            fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(a: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                override fun onAnimationEnd(a: android.view.animation.Animation?) {
                    selector.visibility = View.GONE
                }
            })
            selector.startAnimation(fadeOut)
        }
    }

    // ===== 天气选择器 =====

    private fun setupWeatherSelector(
        chipGroup: ChipGroup,
        options: Array<String>,
        icons: IntArray,
        initialSelected: Int,
        tag: TextView,
        onSelected: (Int) -> Unit
    ) {
        chipGroup.removeAllViews()
        val ctx = requireContext()
        val pinkColor = ContextCompat.getColor(ctx, R.color.pink_primary)
        val chipBgColor = ContextCompat.getColor(ctx, R.color.tag_chip_bg)
        val whiteColor = ContextCompat.getColor(ctx, R.color.pink_card)

        options.forEachIndexed { index, label ->
            val chip = Chip(ctx).apply {
                text = label
                isCheckable = true
                isCheckedIconVisible = false
                textSize = 13f
                chipIconSize = resources.getDimension(R.dimen.icon_size_small)
                if (index < icons.size) {
                    chipIcon = ContextCompat.getDrawable(ctx, icons[index])
                    isChipIconVisible = true
                    chipIconTint = if (index == initialSelected) {
                        android.content.res.ColorStateList.valueOf(whiteColor)
                    } else {
                        null // 保留原始颜色
                    }
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
            chip.setOnCheckedChangeListener { chipView, isChecked ->
                (chipView as Chip).chipIconTint = if (isChecked) {
                    android.content.res.ColorStateList.valueOf(whiteColor)
                } else {
                    null
                }
                if (isChecked) {
                    tag.text = label
                    onSelected(index)
                }
            }
            chipGroup.addView(chip)
        }
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
                    chipIconTint = if (index == initialSelected) {
                        android.content.res.ColorStateList.valueOf(whiteColor)
                    } else {
                        null // 保留原始颜色
                    }
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
            chip.setOnCheckedChangeListener { chipView, isChecked ->
                (chipView as Chip).chipIconTint = if (isChecked) {
                    android.content.res.ColorStateList.valueOf(whiteColor)
                } else {
                    null
                }
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
        val voiceWave = view.findViewById<VoiceWaveView>(R.id.voice_wave)

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
                            voiceWave.stopPlayingAnimation()
                            seekBar.progress = 0
                            tvDuration.text = formatAudioDuration(it.duration)
                            handler.removeCallbacks(updateRunnable)
                            it.release()
                            mediaPlayer = null
                        }
                    }
                    playing = true
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                    voiceWave.startPlayingAnimation()
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
                voiceWave.stopPlayingAnimation()
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

    // ===== 拍照/图片 =====

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

    // ===== 录音 =====

    private fun showRecordingDialog() {
        val ctx = requireContext()
        val sheet = BottomSheetDialog(ctx, R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.dialog_voice_record, null)
        sheet.setContentView(sheetView)
        sheet.setCancelable(false)

        val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.background = ContextCompat.getDrawable(ctx, R.drawable.bg_bottom_sheet_tag)

        val tvTimer = sheetView.findViewById<TextView>(R.id.tv_timer)
        val tvHint = sheetView.findViewById<TextView>(R.id.tv_record_hint)
        val btnToggle = sheetView.findViewById<TextView>(R.id.btn_record_toggle)
        val btnCancel = sheetView.findViewById<View>(R.id.btn_cancel)
        val btnDone = sheetView.findViewById<TextView>(R.id.btn_done)
        val voiceWave = sheetView.findViewById<VoiceWaveView>(R.id.voice_wave)

        tvTimer.text = "00:00"
        btnDone.alpha = 0.4f
        btnDone.isEnabled = false
        voiceWave.startIdleAnimation()

        // 定时读取振幅并更新波形
        val amplitudeRunnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    val amp = try {
                        mediaRecorder?.maxAmplitude?.toFloat() ?: 0f
                    } catch (_: Exception) { 0f }
                    // maxAmplitude 范围 0~32767，归一化到 0~1
                    voiceWave.setAmplitude((amp / 32767f).coerceIn(0f, 1f))
                    timerHandler.postDelayed(this, 80)
                }
            }
        }

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

        btnCancel.setOnClickListener {
            stopRecording(discard = true)
            timerHandler.removeCallbacksAndMessages(null)
            voiceWave.reset()
            sheet.dismiss()
        }

        btnDone.setOnClickListener {
            if (isRecording) {
                val ok = stopRecording(discard = false)
                timerHandler.removeCallbacksAndMessages(null)
                voiceWave.reset()
                btnToggle.text = "开始录音"
                if (!ok) {
                    btnDone.isEnabled = false
                    btnDone.alpha = 0.4f
                    tvHint.text = "录音无效，请重新录制"
                    voiceWave.startIdleAnimation()
                    return@setOnClickListener
                }
            }

            val fileName = currentAudioFileName
            if (!hasValidRecording || fileName.isNullOrBlank()) {
                Toast.makeText(ctx, "请先完成一段有效录音", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val marker = "[AUDIO:$fileName]"
            if (marker !in existingMediaMarkers) {
                existingMediaMarkers.add(marker)
            }
            contentPreview.visibility = View.VISIBLE
            refreshMediaPreview()
            Toast.makeText(ctx, "语音已添加（${formatAudioDuration(lastRecordingDurationMs.toInt())}）", Toast.LENGTH_SHORT).show()

            currentAudioFileName = null
            hasValidRecording = false
            lastRecordingDurationMs = 0L
            sheet.dismiss()
        }

        btnToggle.setOnClickListener {
            if (!isRecording) {
                if (hasValidRecording && !currentAudioFileName.isNullOrBlank()) {
                    stopRecording(discard = true)
                }
                if (!startRecording()) return@setOnClickListener
                btnDone.isEnabled = false
                btnDone.alpha = 0.4f
                tvHint.text = "录音中..."
                btnToggle.text = "停止录音"
                voiceWave.stopIdleAnimation()
                timerHandler.post(timerRunnable)
                timerHandler.post(amplitudeRunnable)
            } else {
                val ok = stopRecording(discard = false)
                timerHandler.removeCallbacksAndMessages(null)
                voiceWave.reset()
                voiceWave.startIdleAnimation()
                btnToggle.text = "开始录音"
                btnDone.isEnabled = ok
                btnDone.alpha = if (ok) 1f else 0.4f
                tvHint.text = if (ok) "录音完成，点击“完成并插入”" else "录音无效，请重新录制"
            }
        }

        sheet.setOnDismissListener {
            timerHandler.removeCallbacksAndMessages(null)
            voiceWave.reset()
            if (isRecording) stopRecording(discard = true)
        }

        sheet.show()
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

    // ===== 自动定位 =====

    private fun autoFetchLocation() {
        val hasFine = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            return
        }
        silentFetchLocation()
    }

    @SuppressLint("MissingPermission")
    private fun silentFetchLocation() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsEnabled && !networkEnabled) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (!isAdded) return@addOnSuccessListener
            if (location != null) {
                silentReverseGeocode(location.latitude, location.longitude)
            } else {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMaxUpdates(1)
                    .setDurationMillis(10000)
                    .build()
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        locationTimeoutHandler.removeCallbacksAndMessages(null)
                        if (!isAdded) return
                        result.lastLocation?.let { silentReverseGeocode(it.latitude, it.longitude) }
                    }
                }
                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                locationTimeoutHandler.postDelayed({
                    fusedLocationClient.removeLocationUpdates(callback)
                }, 10000)
            }
        }
    }

    private fun silentReverseGeocode(lat: Double, lng: Double) {
        if (!isAdded) return
        val ctx = requireContext().applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            var address = ""
            try {
                @Suppress("DEPRECATION")
                val results = Geocoder(ctx, Locale.CHINA).getFromLocation(lat, lng, 1)
                val addr = results?.firstOrNull()
                if (addr != null) {
                    address = addr.getAddressLine(0) ?: buildString {
                        addr.adminArea?.let { append(it) }
                        addr.locality?.let { append(it) }
                        addr.subLocality?.let { append(it) }
                        addr.thoroughfare?.let { append(it) }
                        addr.featureName?.let { f ->
                            if (f != addr.thoroughfare) append(f)
                        }
                    }
                }
            } catch (_: Exception) {}
            if (address.isEmpty()) {
                address = "($lat, $lng)"
            }
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    currentLocation = address
                }
            }
        }
    }

    // ===== 标签 =====

    // 标签分类定义
    private val tagCategories = linkedMapOf(
        "生活" to listOf("日常", "美食", "运动", "购物"),
        "情感" to listOf("约会", "心情", "纪念日", "感恩"),
        "成长" to listOf("学习", "工作", "灵感", "阅读"),
        "出行" to listOf("旅行", "探店", "户外")
    )

    private fun showTagDialog() {
        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("diary_tags", Context.MODE_PRIVATE)
        val customTags = prefs.getStringSet("custom_tags", emptySet())!!.toList().sorted()
        val hiddenTags = prefs.getStringSet("hidden_builtin_tags", emptySet())!!
        val tempSelected = selectedTags.toMutableSet()

        // 收集所有可见标签名
        val allTagNames = mutableListOf<String>()
        tagCategories.values.forEach { tags -> allTagNames.addAll(tags.filter { it !in hiddenTags }) }
        if (customTags.isNotEmpty()) allTagNames.addAll(customTags)
        val distinctNames = allTagNames.distinct()

        val sheet = BottomSheetDialog(ctx, R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.dialog_tag_selector, null)
        sheet.setContentView(sheetView)

        val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.background = ContextCompat.getDrawable(ctx, R.drawable.bg_bottom_sheet_tag)

        val container = sheetView.findViewById<LinearLayout>(R.id.tag_categories_container)
        val dp = ctx.resources.displayMetrics.density
        val pinkColor = ContextCompat.getColor(ctx, R.color.pink_primary)
        val chipBgColor = ContextCompat.getColor(ctx, R.color.tag_chip_bg)
        val strokeColor = ContextCompat.getColor(ctx, R.color.tag_chip_stroke)
        val whiteColor = ContextCompat.getColor(ctx, R.color.text_white)
        val textPrimary = ContextCompat.getColor(ctx, R.color.text_primary)
        val hintColor = ContextCompat.getColor(ctx, R.color.text_hint)

        fun styleChip(chip: Chip, isSelected: Boolean) {
            if (isSelected) {
                chip.chipBackgroundColor = ColorStateList.valueOf(pinkColor)
                chip.setTextColor(whiteColor)
                chip.chipStrokeWidth = 0f
            } else {
                chip.chipBackgroundColor = ColorStateList.valueOf(chipBgColor)
                chip.setTextColor(textPrimary)
                chip.chipStrokeWidth = 1f * dp
                chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
            }
        }

        fun addCategorySection(title: String, tags: List<String>) {
            // 分类标题
            val header = TextView(ctx).apply {
                text = title
                textSize = 13f
                setTextColor(hintColor)
                setPadding(0, (12 * dp).toInt(), 0, (4 * dp).toInt())
            }
            container.addView(header)

            // 标签Chip组
            val chipGroup = ChipGroup(ctx).apply {
                chipSpacingHorizontal = (8 * dp).toInt()
                chipSpacingVertical = (8 * dp).toInt()
            }
            for (tagName in tags) {
                val chip = Chip(ctx).apply {
                    text = "#$tagName"
                    isCheckable = false
                    shapeAppearanceModel = com.google.android.material.shape.ShapeAppearanceModel.builder()
                        .setAllCornerSizes(18f * dp).build()
                    chipMinHeight = 36f * dp
                    this.textSize = 14f
                    tag = tagName
                }
                styleChip(chip, tempSelected.contains(tagName))
                chip.setOnClickListener {
                    val name = chip.tag as String
                    if (tempSelected.contains(name)) {
                        tempSelected.remove(name)
                        styleChip(chip, false)
                        chip.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction {
                            chip.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }.start()
                    } else {
                        tempSelected.add(name)
                        styleChip(chip, true)
                        chip.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                            chip.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }.start()
                    }
                }
                chipGroup.addView(chip)
            }
            container.addView(chipGroup)
        }

        // 按分类添加内置标签（过滤已隐藏的）
        for ((category, tags) in tagCategories) {
            val visibleTags = tags.filter { it !in hiddenTags }
            if (visibleTags.isNotEmpty()) addCategorySection(category, visibleTags)
        }

        // 自定义标签分类
        if (customTags.isNotEmpty()) {
            addCategorySection("自定义", customTags)
        }

        sheetView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            selectedTags.clear()
            selectedTags.addAll(distinctNames.filter { tempSelected.contains(it) })
            sheet.dismiss()
        }

        sheetView.findViewById<View>(R.id.btn_add_tag).setOnClickListener {
            sheet.dismiss()
            showAddCustomTagDialog { showTagDialog() }
        }

        val btnManage = sheetView.findViewById<TextView>(R.id.btn_manage)
        btnManage.visibility = View.VISIBLE
        btnManage.setOnClickListener {
            sheet.dismiss()
            showManageTagsDialog()
        }

        sheet.show()
    }

    private fun showAddCustomTagDialog(onAdded: (() -> Unit)? = null) {
        val ctx = requireContext()
        val sheet = BottomSheetDialog(ctx, R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.dialog_tag_selector, null)
        sheet.setContentView(sheetView)

        val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.background = ContextCompat.getDrawable(ctx, R.drawable.bg_bottom_sheet_tag)

        sheetView.findViewById<TextView>(R.id.tv_sheet_title).text = "添加新标签"
        sheetView.findViewById<View>(R.id.btn_manage).visibility = View.GONE
        sheetView.findViewById<View>(R.id.btn_add_tag).visibility = View.GONE

        // 替换 chipGroup 区域为输入框
        val chipGroup = sheetView.findViewById<ChipGroup>(R.id.chip_group_tags)
        chipGroup.removeAllViews()

        val input = EditText(ctx).apply {
            hint = "输入标签名（不需要#号）"
            textSize = 15f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            setHintTextColor(ContextCompat.getColor(ctx, R.color.text_hint))
            setPadding(0, 24, 0, 24)
            background = null
            requestFocus()
        }
        // 将 EditText 加入 chipGroup 的父容器
        val scrollView = chipGroup.parent as ViewGroup
        scrollView.addView(input, 0)
        chipGroup.visibility = View.GONE

        sheetView.findViewById<TextView>(R.id.btn_confirm).text = "添加"
        sheetView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            val tag = input.text.toString().trim()
            if (tag.isNotEmpty()) {
                val prefs = ctx.getSharedPreferences("diary_tags", Context.MODE_PRIVATE)
                val existing = prefs.getStringSet("custom_tags", emptySet())!!.toMutableSet()
                existing.add(tag)
                prefs.edit().putStringSet("custom_tags", existing).apply()
                Toast.makeText(ctx, "标签 #$tag 已添加", Toast.LENGTH_SHORT).show()
            }
            sheet.dismiss()
            onAdded?.invoke()
        }

        sheet.setOnShowListener {
            input.postDelayed({
                val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }, 200)
        }

        sheet.show()
    }

    private fun showManageTagsDialog() {
        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("diary_tags", Context.MODE_PRIVATE)
        val customTags = prefs.getStringSet("custom_tags", emptySet())!!.toList().sorted()
        val hiddenTags = prefs.getStringSet("hidden_builtin_tags", emptySet())!!.toMutableSet()
        val allBuiltinTags = tagCategories.values.flatten()
        val visibleBuiltin = allBuiltinTags.filter { it !in hiddenTags }
        val hiddenBuiltin = allBuiltinTags.filter { it in hiddenTags }

        val sheet = BottomSheetDialog(ctx, R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.dialog_tag_manage, null)
        sheet.setContentView(sheetView)

        val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.background = ContextCompat.getDrawable(ctx, R.drawable.bg_bottom_sheet_tag)

        val container = sheetView.findViewById<LinearLayout>(R.id.tag_list_container)
        val dp = ctx.resources.displayMetrics.density
        val pinkSoft = ContextCompat.getColor(ctx, R.color.pink_soft)
        val pinkPrimary = ContextCompat.getColor(ctx, R.color.pink_primary)
        val textPrimary = ContextCompat.getColor(ctx, R.color.text_primary)
        val hintColor = ContextCompat.getColor(ctx, R.color.text_hint)

        fun addSectionHeader(title: String) {
            val header = TextView(ctx).apply {
                text = title
                textSize = 13f
                setTextColor(hintColor)
                setPadding(0, (14 * dp).toInt(), 0, (6 * dp).toInt())
            }
            container.addView(header)
        }

        fun addDivider() {
            val divider = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
                ).also { it.marginStart = (4 * dp).toInt(); it.marginEnd = (4 * dp).toInt() }
                setBackgroundColor(ContextCompat.getColor(ctx, R.color.divider))
            }
            container.addView(divider)
        }

        fun addTagRow(tagName: String, isBuiltin: Boolean) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt())
            }

            val tvTag = TextView(ctx).apply {
                text = "#$tagName"
                textSize = 15f
                setTextColor(textPrimary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnAction = TextView(ctx).apply {
                text = "隐藏"
                textSize = 13f
                setTextColor(pinkSoft)
                setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
            }

            btnAction.setOnClickListener {
                if (isBuiltin) {
                    hiddenTags.add(tagName)
                    prefs.edit().putStringSet("hidden_builtin_tags", hiddenTags).apply()
                } else {
                    val existing = prefs.getStringSet("custom_tags", emptySet())!!.toMutableSet()
                    existing.remove(tagName)
                    prefs.edit().putStringSet("custom_tags", existing).apply()
                }
                selectedTags.remove(tagName)
                row.animate().alpha(0f).translationX(row.width.toFloat()).setDuration(250).withEndAction {
                    container.removeView(row)
                }.start()
                Toast.makeText(ctx, "#$tagName 已${if (isBuiltin) "隐藏" else "删除"}", Toast.LENGTH_SHORT).show()
            }

            row.addView(tvTag)
            row.addView(btnAction)
            container.addView(row)
        }

        // 内置标签（可隐藏）
        if (visibleBuiltin.isNotEmpty()) {
            addSectionHeader("内置标签")
            visibleBuiltin.forEachIndexed { i, tag ->
                addTagRow(tag, isBuiltin = true)
                if (i < visibleBuiltin.size - 1) addDivider()
            }
        }

        // 自定义标签（可删除）
        if (customTags.isNotEmpty()) {
            addSectionHeader("自定义标签")
            customTags.forEachIndexed { i, tag ->
                addTagRow(tag, isBuiltin = false)
                if (i < customTags.size - 1) addDivider()
            }
        }

        // 已隐藏的内置标签（可恢复）
        if (hiddenBuiltin.isNotEmpty()) {
            addSectionHeader("已隐藏")
            for (tagName in hiddenBuiltin) {
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding((4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt())
                }
                val tvTag = TextView(ctx).apply {
                    text = "#$tagName"
                    textSize = 15f
                    setTextColor(hintColor)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val btnRestore = TextView(ctx).apply {
                    text = "恢复"
                    textSize = 13f
                    setTextColor(pinkPrimary)
                    setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
                }
                btnRestore.setOnClickListener {
                    hiddenTags.remove(tagName)
                    prefs.edit().putStringSet("hidden_builtin_tags", hiddenTags).apply()
                    row.animate().alpha(0f).setDuration(200).withEndAction {
                        container.removeView(row)
                    }.start()
                    Toast.makeText(ctx, "#$tagName 已恢复", Toast.LENGTH_SHORT).show()
                }
                row.addView(tvTag)
                row.addView(btnRestore)
                container.addView(row)
                if (tagName != hiddenBuiltin.last()) addDivider()
            }
        }

        sheetView.findViewById<View>(R.id.btn_back_to_select).setOnClickListener {
            sheet.dismiss()
            showTagDialog()
        }

        sheet.show()
    }
}
