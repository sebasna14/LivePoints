package com.example.livepoints.Data

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val imageUrl: String = "",
    val identificationNumber: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var isAvailable: Boolean = false
)
