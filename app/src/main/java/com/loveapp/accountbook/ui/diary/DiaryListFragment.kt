package com.loveapp.accountbook.ui.diary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveapp.accountbook.R
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

        adapter = DiaryAdapter { position ->
            // 彩蛋: mood emoji点击
            Toast.makeText(requireContext(), EasterEggManager.moodWords.random(), Toast.LENGTH_SHORT).show()
        }

        view.findViewById<RecyclerView>(R.id.rv_diaries).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@DiaryListFragment.adapter
        }

        viewModel.diaries.observe(viewLifecycleOwner) { adapter.updateData(it) }

        // 搜索框 + 彩蛋
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().lowercase()
                if (text.contains("爱") || text.contains("love") || text.contains("喜欢")) {
                    EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggSearch)
                    etSearch.setText("")
                } else if (text.contains("想你") || text.contains("miss")) {
                    EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggMiss)
                    etSearch.setText("")
                } else {
                    viewModel.searchDiaries(text)
                }
            }
        })

        view.findViewById<View>(R.id.fab_add).setOnClickListener {
            findNavController().navigate(R.id.action_diary_to_add)
        }

        viewModel.loadDiaries()
    }
}
