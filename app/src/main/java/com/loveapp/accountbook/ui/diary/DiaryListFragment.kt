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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_diary_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiaryAdapter(
            onMoodClick = {
                Toast.makeText(requireContext(), EasterEggManager.moodWords.random(), Toast.LENGTH_SHORT).show()
            },
            onItemClick = { entry, position ->
                if (adapter.isSwipeOpenAt(position)) {
                    closeSwipeAt(rvDiaries, position)
                } else {
                    showDetailDialog(entry)
                }
            },
            onEditClick = { entry ->
                val openPosition = adapter.getSwipeOpenPosition()
                closeSwipeAt(rvDiaries, openPosition)
                showEditDialog(entry)
            },
            onDeleteClick = { entry ->
                val openPosition = adapter.getSwipeOpenPosition()
                closeSwipeAt(rvDiaries, openPosition)
                showDeleteConfirmDialog(entry)
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
                return (actionWidth / itemWidth.toFloat()).coerceIn(0.12f, 0.4f)
            }

            override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 1.5f

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
        val closeTrigger = recyclerView.resources.displayMetrics.density * 10f
        var downX = 0f
        var downY = 0f
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val openPosition = adapter.getSwipeOpenPosition()
                if (openPosition == RecyclerView.NO_POSITION) return false
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.x
                        downY = e.y
                        val touchedChild = rv.findChildViewUnder(e.x, e.y)
                        val touchedPosition = touchedChild?.let { rv.getChildAdapterPosition(it) }
                        if (touchedPosition != openPosition) {
                            closeSwipeAt(rv, openPosition)
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.x - downX
                        val dy = e.y - downY
                        if (dx > closeTrigger && abs(dx) > abs(dy)) {
                            closeSwipeAt(rv, openPosition)
                            return true
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val dx = e.x - downX
                        val dy = e.y - downY
                        if (dx > closeTrigger && abs(dx) > abs(dy)) {
                            closeSwipeAt(rv, openPosition)
                            return true
                        }
                    }
                }
                return false
            }
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
                .setDuration(150L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            adapter.setSwipeOpenPosition(position, notify = false)
        } else {
            adapter.setSwipeOpenPosition(position)
        }
    }

    private fun closeSwipeAt(recyclerView: RecyclerView, position: Int) {
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? DiaryAdapter.ViewHolder
        if (holder != null) {
            holder.cardForeground.animate()
                .translationX(0f)
                .setDuration(120L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            adapter.clearSwipeOpenPosition(position, notify = false)
        } else {
            adapter.clearSwipeOpenPosition(position)
        }
    }

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
            .setPositiveButton("\u5173\u95ED", null)
            .setNeutralButton("\u7F16\u8F91") { _, _ -> showEditDialog(entry) }
            .setNegativeButton("\u5220\u9664") { _, _ -> showDeleteConfirmDialog(entry) }
            .show()
    }

    private fun showEditDialog(entry: DiaryEntry) {
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
            .setTitle("\u786E\u8BA4\u5220\u9664")
            .setMessage("\u5220\u9664\u65E5\u8BB0\u300A${entry.title}\u300B\uFF1F")
            .setPositiveButton("\u5220\u9664") { _, _ -> viewModel.deleteDiary(entry) }
            .setNegativeButton("\u53D6\u6D88", null)
            .show()
    }
}
