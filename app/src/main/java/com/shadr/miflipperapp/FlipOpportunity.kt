package com.shadr.miflipperapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FlipOpportunity(
    val itemName: String,
    val quality: String,
    val tier: Int,
    val buyCity: String,
    val buyPrice: Int,
    val sellPrice: Int,
    val profit: Int,
    val profitPercentage: Double,
    val timestamp: Long
) : Parcelable {

    val uniqueId: String
        get() = "$itemName-$quality-$buyCity"
}