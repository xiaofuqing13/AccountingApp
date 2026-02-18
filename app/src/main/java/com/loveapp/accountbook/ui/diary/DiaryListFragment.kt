package com.loveapp.accountbook.ui.diary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.ui.adapter.DiaryAdapter
import com.loveapp.accountbook.util.EasterEggManager

class DiaryListFragment : Fragment() {

    private val viewModel: DiaryViewModel by activityViewModels()
    private lateinit var adapter: DiaryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiaryAdapter(
            onMoodClick = { _ ->
                Toast.makeText(requireContext(), EasterEggManager.moodWords.random(), Toast.LENGTH_SHORT).show()
            },
          onItemClick = { entry -> showDetailDialog(entry) },
            onLongClick = { entry -> showEditDeleteDialog(entry) }
        )

        view.findViewById<RecyclerView>(R.id.rv_diaries).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@DiaryListFragment.adapter
        }

        viewModel.diaries.observe(viewLifecycleOwner) {
            adapter.updateData(it)
            view.findViewById<View>(R.id.empty_view)?.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.rv_diaries)?.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }

        val etSearch = view.findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                when {
                    text.contains("爱") || text.contains("love") || text.contains("喜欢") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggSearch)
                        etSearch.setText("")
                    }
                    text.contains("想你") || text.contains("miss") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggMiss)
                        etSearch.setText("")
                    }
                    text.contains("永远") || text.contains("forever") -> {
                        EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggForever)
                        etSearch.setText("")
                    }
                    text.contains("开心") || text.contains("快乐") || text.contains("happy") -> {
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
        AlertDialog.Builder(requireContext())
            .setTitle("${entry.mood} ${entry.title}")
            .setMessage("📅 ${entry.date}  ${entry.weather}\n\n${entry.content}")
            .setPositiveButton("关闭", null)
            .setNeutralButton("✏️ 编辑") { _, _ -> showEditDialog(entry) }
            .show()
    }

    private fun showEditDeleteDialog(entry: DiaryEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle(entry.title)
            .setItems(arrayOf("✏️ 编辑", "🗑️ 删除")) { _, which ->
                when (which) {
                    0 -> showEditDialog(entry)
                    1 -> showDeleteConfirmDialog(entry)
                }
            }
            .show()
    }

    private fun showEditDialog(entry: DiaryEntry) {
        val etTitle = EditText(requireContext()).apply { setText(entry.title); hint = "标题" }
        val etContent = EditText(requireContext()).apply {
            setText(entry.content); hint = "内容"
            minLines = 4; gravity = android.view.Gravity.TOP
        }
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(etTitle)
            addView(etContent)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("编辑日记")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                viewModel.updateDiary(entry.copy(
                    title = etTitle.text.toString(),
                    content = etContent.text.toString()
                ))
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(entry: DiaryEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("删除日记「${entry.title}」？")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteDiary(entry) }
            .setNegativeButton("取消", null)
            .show()
    }
}
