package com.loveapp.accountbook.ui.diary

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.ui.adapter.DiaryAdapter
import com.loveapp.accountbook.util.DiaryContentRenderer
import com.loveapp.accountbook.util.EasterEggManager
import kotlin.math.abs

class DiaryListFragment : Fragment() {

    private val viewModel: DiaryViewModel by activityViewModels()
    private lateinit var adapter: DiaryAdapter
    private lateinit var rvDiaries: RecyclerView
    private var lastErrorMessage: String? = null
    private var isNavigating = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_diary_list, container, false)

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiaryAdapter(
            onItemClick = { diary, position ->
                if (adapter.isSwipeOpenAt(position)) {
                    closeSwipeAt(rvDiaries, position)
                } else {
                    showDetailDialog(diary)
                }
            },
            onMoodClick = { diary ->
                Toast.makeText(requireContext(), EasterEggManager.moodWords.random(), Toast.LENGTH_SHORT).show()
            },
            onEditClick = { diary ->
                val openPosition = adapter.getSwipeOpenPosition()
                closeSwipeAt(rvDiaries, openPosition)
                showEditDialog(diary)
            },
            onDeleteClick = { diary ->
                val openPosition = adapter.getSwipeOpenPosition()
                closeSwipeAt(rvDiaries, openPosition)
                showDeleteConfirmDialog(diary)
            }
        )

        rvDiaries = view.findViewById(R.id.rv_diaries)
        rvDiaries.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@DiaryListFragment.adapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        attachSwipeHandler(rvDiaries)

        viewModel.diaries.observe(viewLifecycleOwner) { diaries ->
            adapter.updateData(diaries)
            view.findViewById<View>(R.id.empty_view)?.visibility =
                if (diaries.isEmpty()) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.rv_diaries)?.visibility =
                if (diaries.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank() && message != lastErrorMessage) {
                lastErrorMessage = message
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        val etSearch = view.findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                when {
                    text.contains("love", ignoreCase = true) -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggSearch)
                        etSearch.setText("")
                    }
                    text.contains("miss", ignoreCase = true) -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggMiss)
                        etSearch.setText("")
                    }
                    text.contains("forever", ignoreCase = true) -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggForever)
                        etSearch.setText("")
                    }
                    text.contains("happy", ignoreCase = true) -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggHappy)
                        etSearch.setText("")
                    }
                    else -> viewModel.searchDiaries(text)
                }
            }
        })

        view.findViewById<View>(R.id.fab_add).setOnClickListener {
            findNavController().navigate(R.id.action_diary_to_add)
        }

        setupTagFilter(view)
        viewModel.loadDiaries()
    }

    private fun setupTagFilter(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filter)
        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("diary_tags", Context.MODE_PRIVATE)
        val deletedBuiltin = prefs.getStringSet("deleted_builtin_tags", emptySet())!!
        val customTags = prefs.getStringSet("custom_tags", emptySet())!!.map { raw ->
            val idx = raw.indexOf(':')
            if (idx > 0) raw.substring(idx + 1) else raw
        }.sorted()

        val builtinTags = listOf(
            "日常", "美食", "运动", "购物",
            "约会", "心情", "纪念日", "感恩",
            "学习", "工作", "灵感", "阅读",
            "旅行", "探店", "户外"
        ).filter { it !in deletedBuiltin }
        val allTags = (builtinTags + customTags).distinct()
        if (allTags.isEmpty()) return

        val pinkColor = ContextCompat.getColor(ctx, R.color.pink_primary)
        val chipBg = ContextCompat.getColor(ctx, R.color.tag_chip_bg)
        val strokeColor = ContextCompat.getColor(ctx, R.color.tag_chip_stroke)
        val whiteColor = ContextCompat.getColor(ctx, R.color.text_white)
        val textColor = ContextCompat.getColor(ctx, R.color.text_primary)
        val dp = ctx.resources.displayMetrics.density

        fun styleChip(chip: Chip, selected: Boolean) {
            if (selected) {
                chip.chipBackgroundColor = ColorStateList.valueOf(pinkColor)
                chip.setTextColor(whiteColor)
                chip.chipStrokeWidth = 0f
            } else {
                chip.chipBackgroundColor = ColorStateList.valueOf(chipBg)
                chip.setTextColor(textColor)
                chip.chipStrokeWidth = 1f * dp
                chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
            }
        }

        // "全部" chip
        val allChip = Chip(ctx).apply {
            text = "全部"
            isCheckable = false
            textSize = 12f
            chipMinHeight = 30f * dp
            tag = null
        }
        styleChip(allChip, true)
        chipGroup.addView(allChip)

        val chips = mutableListOf(allChip)
        for (tagName in allTags) {
            val chip = Chip(ctx).apply {
                text = "#$tagName"
                isCheckable = false
                textSize = 12f
                chipMinHeight = 30f * dp
                this.tag = tagName
            }
            styleChip(chip, false)
            chipGroup.addView(chip)
            chips.add(chip)
        }

        for (chip in chips) {
            chip.setOnClickListener {
                val selectedTag = chip.tag as? String
                chips.forEach { c -> styleChip(c, c == chip) }
                viewModel.filterByTag(selectedTag)
            }
        }
    }

    // ========== Apple 备忘录风格滑动 ==========

    private fun attachSwipeHandler(recyclerView: RecyclerView) {
        val viewConfig = ViewConfiguration.get(requireContext())
        val touchSlop = viewConfig.scaledTouchSlop
        val minFlingVelocity = viewConfig.scaledMinimumFlingVelocity.toFloat()

        var downX = 0f
        var downY = 0f
        var dragging = false
        var dragPosition = RecyclerView.NO_POSITION
        var dragHolder: DiaryAdapter.ViewHolder? = null
        var dragStartTx = 0f
        var velocityTracker: VelocityTracker? = null

        fun maxSwipe() = adapter.getSwipeActionTotalWidthPx().toFloat()

        fun resetState() {
            dragging = false
            dragPosition = RecyclerView.NO_POSITION
            dragHolder = null
            velocityTracker?.recycle()
            velocityTracker = null
        }

        // 滚动时自动关闭
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    val p = adapter.getSwipeOpenPosition()
                    if (p != RecyclerView.NO_POSITION) closeSwipeAt(rv, p)
                }
            }
        })

        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {

            /** 是否已拦截当前手势 */
            private var intercepted = false

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.x; downY = e.y
                        dragging = false; intercepted = false
                        dragPosition = RecyclerView.NO_POSITION; dragHolder = null
                        velocityTracker?.recycle()
                        velocityTracker = VelocityTracker.obtain()
                        velocityTracker?.addMovement(e)

                        val openPos = adapter.getSwipeOpenPosition()
                        if (openPos != RecyclerView.NO_POSITION) {
                            // —— 有打开项 ——
                            val child = rv.findChildViewUnder(e.x, e.y)
                            val touchedPos = child?.let { rv.getChildAdapterPosition(it) }
                                ?: RecyclerView.NO_POSITION
                            if (touchedPos != openPos) {
                                // 触摸别处 → 关闭
                                closeSwipeAt(rv, openPos)
                                intercepted = true
                                return true
                            }
                            // 触摸在打开条目上
                            dragPosition = openPos
                            dragHolder = rv.findViewHolderForAdapterPosition(openPos)
                                    as? DiaryAdapter.ViewHolder
                            dragHolder?.cardForeground?.animate()?.cancel()
                            dragStartTx = dragHolder?.cardForeground?.translationX ?: 0f
                            intercepted = true
                            return true
                        }

                        // —— 无打开项 → 记录位置，不拦截 ——
                        val child = rv.findChildViewUnder(e.x, e.y)
                        if (child != null) {
                            dragPosition = rv.getChildAdapterPosition(child)
                            dragHolder = rv.findViewHolderForAdapterPosition(dragPosition)
                                    as? DiaryAdapter.ViewHolder
                        }
                        dragStartTx = 0f
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (intercepted) return true
                        if (dragHolder == null) return false
                        velocityTracker?.addMovement(e)
                        val dx = e.x - downX
                        val dy = e.y - downY
                        // 仅在明确水平左滑时拦截
                        if (!dragging && abs(dx) > touchSlop
                            && abs(dx) > abs(dy) * 1.5f && dx < 0
                        ) {
                            dragging = true
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            intercepted = true
                            return true
                        }
                        return false
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (!intercepted) resetState()
                        return false
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                velocityTracker?.addMovement(e)
                when (e.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        val holder = dragHolder ?: return
                        val dx = e.x - downX
                        val dy = e.y - downY
                        if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                            if (abs(dx) > abs(dy)) {
                                dragging = true
                                holder.cardForeground.animate().cancel()
                                rv.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                        if (dragging) {
                            val newTx = (dragStartTx + dx).coerceIn(-maxSwipe(), 0f)
                            holder.cardForeground.translationX = newTx
                            // 拖动时动态改变透明度
                            holder.diaryActions.visibility = View.VISIBLE
                            holder.diaryActions.alpha = abs(newTx) / maxSwipe()
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        val holder = dragHolder
                        val pos = dragPosition
                        if (holder != null && pos != RecyclerView.NO_POSITION
                            && pos in 0 until adapter.itemCount
                        ) {
                            if (!dragging) {
                                handleTap(rv, holder, pos, maxSwipe(), downX)
                            } else {
                                handleSwipeEnd(rv, holder, pos, maxSwipe(), minFlingVelocity, velocityTracker)
                            }
                        }
                        intercepted = false
                        resetState()
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        val holder = dragHolder
                        val pos = dragPosition
                        if (holder != null && pos != RecyclerView.NO_POSITION) {
                            if (adapter.isSwipeOpenAt(pos)) {
                                animateTo(holder, -maxSwipe())
                            } else {
                                animateTo(holder, 0f)
                            }
                        }
                        intercepted = false
                        resetState()
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    /** 点击处理：按钮 / 卡片体 */
    private fun handleTap(
        rv: RecyclerView, holder: DiaryAdapter.ViewHolder,
        position: Int, maxSwipe: Float, downX: Float
    ) {
        if (!adapter.isSwipeOpenAt(position)) return
        val touchLocalX = downX - holder.itemView.left
        val cardEndX = holder.itemView.width + holder.cardForeground.translationX

        if (touchLocalX > cardEndX) {
            // 按钮区域
            val offset = touchLocalX - cardEndX
            val target = adapter.getItem(position)
            closeSwipeAt(rv, position)
            if (offset < maxSwipe / 2f) {
                showEditDialog(target)
            } else {
                showDeleteConfirmDialog(target)
            }
        } else {
            // 卡片体 → 关闭
            closeSwipeAt(rv, position)
        }
    }

    /** 滑动结束：根据位置和速度决定打开/关闭 */
    private fun handleSwipeEnd(
        rv: RecyclerView, holder: DiaryAdapter.ViewHolder,
        position: Int, maxSwipe: Float, minFlingVelocity: Float,
        velocityTracker: VelocityTracker?
    ) {
        velocityTracker?.computeCurrentVelocity(1000)
        val vx = velocityTracker?.xVelocity ?: 0f
        val tx = holder.cardForeground.translationX
        val isOpen = adapter.isSwipeOpenAt(position)

        if (isOpen) {
            // 已打开 → 判断是否关闭
            if (tx > -maxSwipe * 0.5f || vx > minFlingVelocity) {
                closeSwipeAt(rv, position)
            } else {
                animateTo(holder, -maxSwipe)
            }
        } else {
            // 未打开 → 判断是否打开
            if (tx < -maxSwipe * 0.4f || vx < -minFlingVelocity) {
                openSwipeAt(rv, position)
            } else {
                animateTo(holder, 0f)
            }
        }
    }

    /** 平滑动画到目标 translationX */
    private fun animateTo(holder: DiaryAdapter.ViewHolder, targetTx: Float) {
        val maxPx = adapter.getSwipeActionTotalWidthPx().toFloat()
        val isOpening = targetTx < 0f
        
        // 确保动作菜单的显示层级
        if (isOpening) {
            holder.diaryActions.visibility = View.VISIBLE
        }
        
        holder.cardForeground.animate()
            .translationX(targetTx)
            .setDuration(250L)
            .setInterpolator(DecelerateInterpolator())
            .start()
            
        holder.diaryActions.animate()
            .alpha(if (isOpening) 1f else 0f)
            .setDuration(250L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (!isOpening) {
                    holder.diaryActions.visibility = View.INVISIBLE
                }
            }
            .start()
    }

    private fun openSwipeAt(recyclerView: RecyclerView, position: Int) {
        val maxSwipe = adapter.getSwipeActionTotalWidthPx().toFloat()
        val previous = adapter.getSwipeOpenPosition()
        if (previous != RecyclerView.NO_POSITION && previous != position) {
            closeSwipeAt(recyclerView, previous)
        }
        adapter.setSwipeOpenPosition(position, notify = false)
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? DiaryAdapter.ViewHolder
        if (holder != null) animateTo(holder, -maxSwipe)
    }

    private fun closeSwipeAt(recyclerView: RecyclerView, position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? DiaryAdapter.ViewHolder
        if (holder != null) {
            animateTo(holder, 0f)
            adapter.clearSwipeOpenPosition(position, notify = false)
        } else {
            adapter.clearSwipeOpenPosition(position)
        }
    }

    // ========== 对话框 ==========

    private fun showDetailDialog(entry: DiaryEntry) {
        val scrollView = ScrollView(requireContext())
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        scrollView.addView(container)

        container.addView(TextView(requireContext()).apply {
            text = "${entry.date}  ${entry.weather}"
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
            setPadding(0, 0, 0, 16)
        })

        if (entry.location.isNotEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = "位置：${entry.location}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
                setPadding(0, 0, 0, 16)
            })
        }

        DiaryContentRenderer.renderToContainer(requireContext(), container, entry.content, clearFirst = false)

        AlertDialog.Builder(requireContext())
            .setTitle(entry.title)
            .setView(scrollView)
            .setPositiveButton("关闭", null)
            .setNeutralButton("编辑") { _, _ -> showEditDialog(entry) }
            .setNegativeButton("删除") { _, _ -> showDeleteConfirmDialog(entry) }
            .show()
    }

    private fun showEditDialog(entry: DiaryEntry) {
        if (isNavigating) return
        isNavigating = true
        val bundle = Bundle().apply {
            putInt("entryRowIndex", entry.rowIndex)
            putString("entryTitle", entry.title)
            putString("entryContent", entry.content)
            putString("entryWeather", entry.weather)
            putString("entryMood", entry.mood)
            putString("entryLocation", entry.location)
            putString("entryTags", entry.tags)
            putString("entryDate", entry.date)
        }
        findNavController().navigate(R.id.action_diary_to_add, bundle)
    }

    private fun showDeleteConfirmDialog(entry: DiaryEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除日记《${entry.title}》？")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteDiary(entry) }
            .setNegativeButton("取消", null)
            .show()
    }
}
