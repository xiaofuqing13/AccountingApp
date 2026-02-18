package com.loveapp.accountbook.ui.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.loveapp.accountbook.R
import com.loveapp.accountbook.data.model.DiaryEntry
import com.loveapp.accountbook.util.DateUtils
import com.loveapp.accountbook.util.DraftManager
import com.loveapp.accountbook.util.EasterEggManager
import com.loveapp.accountbook.util.LoveWord

class DiaryAddFragment : Fragment() {

    private val viewModel: DiaryViewModel by activityViewModels()
    private var titleClickCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<EditText>(R.id.et_title)
        val etContent = view.findViewById<EditText>(R.id.et_content)
        val tagDate = view.findViewById<TextView>(R.id.tag_date)
        val tagWeather = view.findViewById<TextView>(R.id.tag_weather)
        val tagMood = view.findViewById<TextView>(R.id.tag_mood)

        tagDate.text = "📅 ${DateUtils.today()}"
        tagWeather.text = "☀️ 晴"
        tagMood.text = "🥰 开心"

        // 自动保存：恢复草稿
        val hasDraft = DraftManager.restoreDraft(requireContext(), etTitle, DraftManager.KEY_DIARY_TITLE)
        DraftManager.restoreDraft(requireContext(), etContent, DraftManager.KEY_DIARY_CONTENT)
        if (hasDraft) Toast.makeText(requireContext(), "已恢复上次编辑的草稿", Toast.LENGTH_SHORT).show()

        // 自动保存：绑定输入监听
        DraftManager.bindAutoSave(requireContext(), etTitle, DraftManager.KEY_DIARY_TITLE)
        DraftManager.bindAutoSave(requireContext(), etContent, DraftManager.KEY_DIARY_CONTENT)

        // 彩蛋: 标题连点3次
        etTitle.setOnClickListener {
            titleClickCount++
            if (titleClickCount >= 3) {
                titleClickCount = 0
                EasterEggManager.showLovePopup(requireContext(),
                    LoveWord("✏️", "写给你的日记", "以后我们的每一篇日记，\n都有两个人的温度。\n\n你写你的心情，\n我写我有多喜欢你。"))
            }
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addDiary(DiaryEntry(
                date = DateUtils.today(),
                title = title,
                content = content,
                weather = "晴",
                mood = "🥰"
            ))
            DraftManager.clearDrafts(requireContext(), "draft_diary_")
            Toast.makeText(requireContext(), "日记保存成功", Toast.LENGTH_SHORT).show()
            // 随机概率弹出保存惊喜
            if ((0..2).random() == 0) {
                EasterEggManager.showLovePopup(requireContext(), EasterEggManager.eggDiarySave)
            }
            findNavController().popBackStack()
        }
    }
}
