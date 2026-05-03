package com.example.duxscholar

data class Aluno(
    val name: String,
    val pfp: String,
    val email: String,
    val ra: String,
    val telephone: String,
    val temppass: String,
    val curso: String, // UID
    val semester: Int,
    val carteirinha: String // URI
)
