package com.loveapp.accountbook.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.Toast

object DraftManager {

    private const val PREF_NAME = "app_drafts"
    private const val DEBOUNCE_MS = 2000L

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveDraft(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun getDraft(context: Context, key: String): String? =
        prefs(context).getString(key, null)

    fun clearDraft(context: Context, key: String) {
        prefs(context).edit().remove(key).apply()
    }

    fun clearDrafts(context: Context, prefix: String) {
        val editor = prefs(context).edit()
        prefs(context).all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        editor.apply()
    }

    fun hasDraft(context: Context, prefix: String): Boolean =
        prefs(context).all.keys.any { it.startsWith(prefix) }

    /**
     * 为EditText绑定自动保存，输入停止2秒后自动保存草稿
     */
    fun bindAutoSave(context: Context, editText: EditText, draftKey: String) {
        val handler = Handler(Looper.getMainLooper())
        var saveRunnable: Runnable? = null

        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                saveRunnable?.let { handler.removeCallbacks(it) }
                saveRunnable = Runnable {
                    saveDraft(context, draftKey, s.toString())
                }
                handler.postDelayed(saveRunnable!!, DEBOUNCE_MS)
            }
        })
    }

    /**
     * 恢复草稿到EditText，如果有草稿则提示用户
     */
    fun restoreDraft(context: Context, editText: EditText, draftKey: String): Boolean {
        val draft = getDraft(context, draftKey)
        if (!draft.isNullOrBlank()) {
            editText.setText(draft)
            return true
        }
        return false
    }

    // ===== 记账草稿 =====
    const val KEY_ACCOUNT_AMOUNT = "draft_account_amount"
    const val KEY_ACCOUNT_NOTE = "draft_account_note"
    const val KEY_ACCOUNT_CATEGORY = "draft_account_category"
    const val KEY_ACCOUNT_TYPE = "draft_account_type"

    // ===== 日记草稿 =====
    const val KEY_DIARY_TITLE = "draft_diary_title"
    const val KEY_DIARY_CONTENT = "draft_diary_content"
    const val KEY_DIARY_WEATHER = "draft_diary_weather"
    const val KEY_DIARY_MOOD = "draft_diary_mood"

    // ===== 会议草稿 =====
    const val KEY_MEETING_TOPIC = "draft_meeting_topic"
    const val KEY_MEETING_LOCATION = "draft_meeting_location"
    const val KEY_MEETING_ATTENDEES = "draft_meeting_attendees"
    const val KEY_MEETING_CONTENT = "draft_meeting_content"
    const val KEY_MEETING_TODO = "draft_meeting_todo"
}
