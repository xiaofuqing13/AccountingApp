package com.loveapp.accountbook.ui.diary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.ui.adapter.DiaryAdapter
import com.loveapp.accountbook.util.DiaryContentRenderer
import com.loveapp.accountbook.util.EasterEggManager

class DiaryListFragment : Fragment() {

    private val viewModel: DiaryViewModel by activityViewModels()
    private lateinit var adapter: DiaryAdapter
    private var lastErrorMessage: String? = null

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
            onItemClick = { entry -> showDetailDialog(entry) },
            onLongClick = { entry -> showEditDeleteDialog(entry) },
            onEditClick = { entry -> showEditDialog(entry) },
            onDeleteClick = { entry -> showDeleteConfirmDialog(entry) }
        )

        view.findViewById<RecyclerView>(R.id.rv_diaries).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@DiaryListFragment.adapter
        }

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

    private fun showEditDeleteDialog(entry: DiaryEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle(entry.title)
            .setItems(arrayOf("\u7F16\u8F91", "\u5220\u9664")) { _, which ->
                when (which) {
                    0 -> showEditDialog(entry)
                    1 -> showDeleteConfirmDialog(entry)
                }
            }
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
