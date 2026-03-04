package com.loveapp.accountbook.data.sync

import android.content.Context
import com.loveapp.accountbook.data.repository.ExcelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncResult(
    val success: Boolean,
    val accounts: Int = 0,
    val diaries: Int = 0,
    val meetings: Int = 0,
    val message: String = ""
)

class SyncManager(private val context: Context) {

    companion object {
        const val BASE_URL = "https://resistive-diotic-jolie.ngrok-free.dev"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val repo = ExcelRepository(context)

    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val accounts = repo.getAccounts()
            val diaries = repo.getDiaries()
            val meetings = repo.getMeetings()

            val json = JSONObject().apply {
                put("accounts", JSONArray().apply {
                    accounts.forEach { a ->
                        put(JSONObject().apply {
                            put("date", a.date)
                            put("type", a.type)
                            put("category", a.category)
                            put("amount", a.amount)
                            put("note", a.note)
                            put("location", a.location)
                        })
                    }
                })
                put("diaries", JSONArray().apply {
                    diaries.forEach { d ->
                        put(JSONObject().apply {
                            put("date", d.date)
                            put("title", d.title)
                            put("content", d.content)
                            put("weather", d.weather)
                            put("mood", d.mood)
                            put("location", d.location)
                            put("tags", d.tags)
                        })
                    }
                })
                put("meetings", JSONArray().apply {
                    meetings.forEach { m ->
                        put(JSONObject().apply {
                            put("date", m.date)
                            put("topic", m.topic)
                            put("start_time", m.startTime)
                            put("end_time", m.endTime)
                            put("location", m.location)
                            put("attendees", m.attendees)
                            put("content", m.content)
                            put("todo_items", m.todoItems)
                        })
                    }
                })
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/sync/upload")
                .addHeader("ngrok-skip-browser-warning", "true")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val result = JSONObject(responseBody)
                if (result.optBoolean("success", false)) {
                    val synced = result.getJSONObject("synced")
                    SyncResult(
                        success = true,
                        accounts = synced.optInt("accounts", 0),
                        diaries = synced.optInt("diaries", 0),
                        meetings = synced.optInt("meetings", 0),
                        message = "同步成功"
                    )
                } else {
                    SyncResult(false, message = result.optString("message", "服务器返回错误"))
                }
            } else {
                SyncResult(false, message = "HTTP ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            SyncResult(false, message = "同步失败: ${e.message}")
        }
    }
}
