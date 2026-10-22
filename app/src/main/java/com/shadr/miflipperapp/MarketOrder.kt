package com.shadr.miflipperapp

import com.squareup.moshi.Json

data class MarketOrder(
    @Json(name = "ItemTypeId") val itemTypeId: String?,
    @Json(name = "LocationId") val locationId: Int?,
    @Json(name = "QualityLevel") val qualityLevel: Int?,
    @Json(name = "UnitPriceSilver") val unitPriceSilver: Long?,
    @Json(name = "Amount") val amount: Int?,
    @Json(name = "AuctionType") val auctionType: String?
)