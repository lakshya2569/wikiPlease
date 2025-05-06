package com.example.wikinow.data.model

data class featured(
    val image: Image,
    val mostread: Mostread,
    val news: List<New>,
    val onthisday: List<Onthisday>,
    val tfa: Tfa
)