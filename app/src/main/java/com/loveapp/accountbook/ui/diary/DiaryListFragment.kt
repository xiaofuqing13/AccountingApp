package com.loveapp.accountbook.ui.diary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
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
    private var lastSwipeDx = 0f
    private val swipeOpenScale = 0.96f
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
        attachSwipeActions(rvDiaries)
        attachSwipeCloseFallback(rvDiaries)

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

        viewModel.loadDiaries()
    }

    // ========== 与会议模块完全一致的滑动实现 ==========

    private fun attachSwipeActions(recyclerView: RecyclerView) {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                val itemWidth = viewHolder.itemView.width.takeIf { it > 0 } ?: return 0.2f
                val actionWidth = adapter.getSwipeActionTotalWidthPx().toFloat()
                return ((actionWidth / itemWidth.toFloat()) + 0.03f).coerceIn(0.22f, 0.45f)
            }

            override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 1.6f

            override fun getSwipeVelocityThreshold(defaultValue: Float): Float = defaultValue * 1.4f

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (viewHolder is DiaryAdapter.ViewHolder) {
                    getDefaultUIUtil().onSelected(viewHolder.cardForeground)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                if (viewHolder is DiaryAdapter.ViewHolder) {
                    getDefaultUIUtil().clearView(viewHolder.cardForeground)
                    val position = viewHolder.bindingAdapterPosition
                    val closeThreshold = recyclerView.resources.displayMetrics.density * 12f
                    if (
                        position != RecyclerView.NO_POSITION &&
                        adapter.isSwipeOpenAt(position) &&
                        lastSwipeDx > closeThreshold
                    ) {
                        closeSwipeAt(recyclerView, position)
                    }
                } else {
                    super.clearView(recyclerView, viewHolder)
                }
                lastSwipeDx = 0f
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (viewHolder !is DiaryAdapter.ViewHolder) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    return
                }
                lastSwipeDx = dX
                val maxSwipe = adapter.getSwipeActionTotalWidthPx().toFloat()
                val position = viewHolder.bindingAdapterPosition
                val isOpen = position != RecyclerView.NO_POSITION && adapter.isSwipeOpenAt(position)
                val clampedDx = when {
                    dX < 0f -> dX.coerceAtLeast(-maxSwipe)
                    isOpen -> (-maxSwipe + dX).coerceIn(-maxSwipe, 0f)
                    else -> 0f
                }
                val progress = (abs(clampedDx) / maxSwipe).coerceIn(0f, 1f)
                val scale = 1f - (1f - swipeOpenScale) * progress
                viewHolder.cardForeground.scaleX = scale
                viewHolder.cardForeground.scaleY = scale
                getDefaultUIUtil().onDraw(
                    c,
                    recyclerView,
                    viewHolder.cardForeground,
                    clampedDx,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                if (direction == ItemTouchHelper.RIGHT) {
                    closeSwipeAt(recyclerView, position)
                } else {
                    openSwipeAt(recyclerView, position)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private fun attachSwipeCloseFallback(recyclerView: RecyclerView) {
        val tapSlop = recyclerView.resources.displayMetrics.density * 24f
        var downX = 0f
        var downY = 0f
        var downInActionArea = false
        var downActionZoneStartX = 0f
        var downOpenPosition = RecyclerView.NO_POSITION

        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val openPosition = adapter.getSwipeOpenPosition()

                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.x
                        downY = e.y
                        downInActionArea = false
                        downActionZoneStartX = 0f
                        downOpenPosition = openPosition

                        if (openPosition == RecyclerView.NO_POSITION) return false

                        val touchedChild = rv.findChildViewUnder(e.x, e.y) ?: return false
                        val touchedPosition = rv.getChildAdapterPosition(touchedChild)

                        if (touchedPosition != openPosition) {
                            closeSwipeAt(rv, openPosition)
                            return false
                        }

                        // 计算操作区域（卡片右侧露出的按钮区域）
                        val actionWidth = adapter.getSwipeActionTotalWidthPx().toFloat()
                        val localX = e.x - touchedChild.left
                        downActionZoneStartX = touchedChild.width - actionWidth
                        downInActionArea = localX >= downActionZoneStartX

                        // 无论点击哪里都拦截，在 onTouchEvent 中处理
                        return true
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                when (e.actionMasked) {
                    MotionEvent.ACTION_UP -> {
                        val openPosition = downOpenPosition
                        if (openPosition == RecyclerView.NO_POSITION) return

                        val moveX = abs(e.x - downX)
                        val moveY = abs(e.y - downY)
                        val isTap = moveX <= tapSlop && moveY <= tapSlop

                        if (isTap && downInActionArea) {
                            // 点击了操作区域
                            val touchedChild = rv.findChildViewUnder(downX, downY)
                            if (touchedChild != null) {
                                val localX = downX - touchedChild.left
                                val actionWidth = adapter.getSwipeActionTotalWidthPx().toFloat()
                                val actionOffset = localX - downActionZoneStartX

                                val target = adapter.getItem(openPosition)
                                closeSwipeAt(rv, openPosition)

                                // 左半边是编辑，右半边是删除
                                if (actionOffset < actionWidth / 2f) {
                                    showEditDialog(target)
                                } else {
                                    showDeleteConfirmDialog(target)
                                }
                            }
                        } else if (isTap) {
                            // 点击了卡片主体，关闭
                            closeSwipeAt(rv, openPosition)
                        } else if (e.x - downX > tapSlop) {
                            // 右滑关闭
                            closeSwipeAt(rv, openPosition)
                        }

                        downOpenPosition = RecyclerView.NO_POSITION
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        downOpenPosition = RecyclerView.NO_POSITION
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun openSwipeAt(recyclerView: RecyclerView, position: Int) {
        val maxSwipe = adapter.getSwipeActionTotalWidthPx().toFloat()
        val previous = adapter.getSwipeOpenPosition()
        if (previous != RecyclerView.NO_POSITION && previous != position) {
            val previousHolder =
                recyclerView.findViewHolderForAdapterPosition(previous) as? DiaryAdapter.ViewHolder
            if (previousHolder != null) {
                previousHolder.cardForeground.animate()
                    .translationX(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                adapter.clearSwipeOpenPosition(previous, notify = false)
            } else {
                adapter.clearSwipeOpenPosition(previous)
            }
        }
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? DiaryAdapter.ViewHolder
        if (holder != null) {
            holder.cardForeground.animate()
                .translationX(-maxSwipe)
                .scaleX(swipeOpenScale)
                .scaleY(swipeOpenScale)
                .setDuration(150L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            adapter.setSwipeOpenPosition(position, notify = false)
        } else {
            adapter.setSwipeOpenPosition(position)
        }
    }

    private fun closeSwipeAt(recyclerView: RecyclerView, position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? DiaryAdapter.ViewHolder
        if (holder != null) {
            holder.cardForeground.animate()
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120L)
                .setInterpolator(DecelerateInterpolator())
                .start()
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
