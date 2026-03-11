package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("total_count")
    val totalCount: Int,
    val items: ArrayList<User>
)
