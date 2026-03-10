package com.example.githubuserrview.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Package(

    val username: String,
    val surename: String,
    val photo: Int,
    val location: String,
    val repository: Int,
    val company: String,
    val followers: Int,
    val following: Int,
    val description: String

) : Parcelable
