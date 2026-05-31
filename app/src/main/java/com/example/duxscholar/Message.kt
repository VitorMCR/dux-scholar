package com.example.duxscholar

data class Message(
    val text: String = "",
    val isUser: Boolean = false,
    val isLoading: Boolean = false,
    val timestamp: String = ""
)