package com.loveapp.accountbook.data.model

data class DiaryEntry(
    val date: String,         // yyyy-MM-dd
    val title: String,
    val content: String,
    val weather: String = "",
    val mood: String = "",
    val location: String = ""
)
