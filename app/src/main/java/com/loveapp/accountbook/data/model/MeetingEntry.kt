package com.loveapp.accountbook.data.model

data class MeetingEntry(
    val date: String,         // yyyy-MM-dd
    val topic: String,
    val startTime: String,    // HH:mm
    val endTime: String,      // HH:mm
    val location: String = "",
    val attendees: String = "",
    val content: String = "",
    val todoItems: String = "",
    val tags: String = ""
)
