package com.shadr.miflipperapp

typealias PriceInfo = MutableMap<Int, Pair<Long, Long>>
typealias CityPrices = MutableMap<Int, PriceInfo>

class FlipDetector {

    private val citySellPrices = mutableMapOf<String, CityPrices>()

    private val blackMarketBuyPrices = mutableMapOf<String, MutableMap<Int, Pair<Long, Long>>>()

    private val locationMap = mapOf(
        7 to "Thetford",
        1002 to "Lymhurst",
        2004 to "Bridgewatch",
        3005 to "Caerleon",
        3008 to "Martlock",
        4002 to "Fort Sterling",
        5003 to "Brecilien"
    )

    @Volatile var isPremiumActive: Boolean = false
    private var onFlipsFound: ((List<FlipOpportunity>) -> Unit)? = null

    fun setOnFlipsFoundListener(listener: (List<FlipOpportunity>) -> Unit) {
        onFlipsFound = listener
    }

    private fun parseTierFromItemId(itemId: String): Int {
        val match = Regex("^T(\\d+)_").find(itemId.uppercase())
        return match?.groups?.get(1)?.value?.toIntOrNull() ?: 0
    }

    fun processMarketOrder(order: MarketOrder) {
        val itemId = order.itemTypeId ?: return
        val locationId = order.locationId ?: return
        val quality = order.qualityLevel ?: return
        val price = order.unitPriceSilver ?: return
        if (price <= 0) return
        val auctionType = order.auctionType ?: return
        val currentTime = System.currentTimeMillis()

        var priceWasUpdated = false

        if (locationId == 3003 && auctionType == "request") {
            val pricesForQuality = blackMarketBuyPrices.getOrPut(itemId) { mutableMapOf() }
            val currentBestPair = pricesForQuality[quality]
            val currentBestPrice = currentBestPair?.first ?: 0L

            if (price > currentBestPrice) {
                pricesForQuality[quality] = Pair(price, currentTime)
                priceWasUpdated = true
            }

        } else if (locationId in locationMap.keys && auctionType == "offer") {
            val pricesForCity = citySellPrices.getOrPut(itemId) { mutableMapOf() }
            val pricesForQuality = pricesForCity.getOrPut(locationId) { mutableMapOf() }
            val currentBestPair = pricesForQuality[quality]
            val currentBestPrice = currentBestPair?.first ?: Long.MAX_VALUE

            if (price < currentBestPrice) {
                pricesForQuality[quality] = Pair(price, currentTime)
                priceWasUpdated = true
            }
        }

        if (priceWasUpdated) {
            checkForSingleFlip(itemId, quality)
        }
    }

    private fun checkForSingleFlip(itemId: String, quality: Int) {
        val bmBuyPair = blackMarketBuyPrices[itemId]?.get(quality) ?: return
        val bmBuyPrice = bmBuyPair.first

        val cityPricesForItem = citySellPrices[itemId] ?: return

        cityPricesForItem.forEach { (cityId, pricesForQuality) ->
            val citySellPair = pricesForQuality[quality] ?: return@forEach
            val citySellPrice = citySellPair.first
            val cityTimestamp = citySellPair.second

            if (citySellPrice <= 0) return@forEach

            val taxRate = if (isPremiumActive) 0.065 else 0.105
            val profit = (bmBuyPrice * (1 - taxRate)) - citySellPrice

            if (profit > 0) {
                val percentage = (profit / citySellPrice.toDouble()) * 100

                val flip = FlipOpportunity(
                    itemName = itemId,
                    quality = quality.toString(),
                    tier = parseTierFromItemId(itemId),
                    buyCity = locationMap[cityId] ?: "Unknown",
                    buyPrice = citySellPrice.toInt(),
                    sellPrice = bmBuyPrice.toInt(),
                    profit = profit.toInt(),
                    profitPercentage = percentage,
                    timestamp = cityTimestamp
                )
                onFlipsFound?.invoke(listOf(flip))
            }
        }
    }

    fun recalculateAllFlips(): List<FlipOpportunity> {
        val recalculatedOpportunities = mutableListOf<FlipOpportunity>()
        val taxRate = if (isPremiumActive) 0.065 else 0.105

        blackMarketBuyPrices.forEach { (itemId, bmPricesForQuality) ->
            bmPricesForQuality.forEach { (quality, bmBuyPair) ->
                val bmBuyPrice = bmBuyPair.first

                citySellPrices[itemId]?.forEach { (cityId, cityPricesForQuality) ->
                    val citySellPair = cityPricesForQuality[quality]
                    if (citySellPair != null) {
                        val citySellPrice = citySellPair.first
                        val cityTimestamp = citySellPair.second

                        if (citySellPrice > 0) {
                            val profit = (bmBuyPrice * (1 - taxRate)) - citySellPrice
                            if (profit > 0) {
                                val percentage = (profit / citySellPrice.toDouble()) * 100

                                recalculatedOpportunities.add(
                                    FlipOpportunity(
                                        itemName = itemId,
                                        quality = quality.toString(),
                                        tier = parseTierFromItemId(itemId),
                                        buyCity = locationMap[cityId] ?: "Unknown",
                                        buyPrice = citySellPrice.toInt(),
                                        sellPrice = bmBuyPrice.toInt(),
                                        profit = profit.toInt(),
                                        profitPercentage = percentage,
                                        timestamp = cityTimestamp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        return recalculatedOpportunities.sortedByDescending { it.timestamp }
    }
}