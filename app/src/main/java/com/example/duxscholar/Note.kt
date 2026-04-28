package com.example.duxscholar

import java.util.UUID

data class Note(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val dateKey: String = "" // Ex: "2024-05-20"
)
