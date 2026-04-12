package com.example.hostelfood

import com.google.firebase.Timestamp

data class FeedbackItem(
    val rollNumber: String = "",
    val mealType: String = "",
    val day: String = "",           // Added
    val ratings: Map<String, String> = emptyMap(),
    val comment: String = "",
    val timestamp: Timestamp? = null
    //val exported: Boolean = false
)