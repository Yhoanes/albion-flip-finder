package com.shadr.miflipperapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun flipDetector_calculatesArbitrageProfit_withoutPremiumTax() {
        val detector = FlipDetector()
        detector.isPremiumActive = false // 10.5% tax rate

        val detected = mutableListOf<FlipOpportunity>()
        detector.setOnFlipsFoundListener { flips ->
            detected.addAll(flips)
        }

        // 1. Sell offer in Lymhurst (locationId = 1002) for 100,000 silver
        detector.processMarketOrder(
            MarketOrder(
                itemTypeId = "T6_MAIN_SWORD@1",
                locationId = 1002,
                qualityLevel = 2,
                unitPriceSilver = 100_000L,
                amount = 1,
                auctionType = "offer"
            )
        )

        // 2. Buy order in Black Market (locationId = 3003) for 200,000 silver
        // Net after 10.5% tax = 200,000 * 0.895 = 179,000 -> Profit = 79,000 silver
        detector.processMarketOrder(
            MarketOrder(
                itemTypeId = "T6_MAIN_SWORD@1",
                locationId = 3003,
                qualityLevel = 2,
                unitPriceSilver = 200_000L,
                amount = 1,
                auctionType = "request"
            )
        )

        assertEquals(1, detected.size)
        val flip = detected.first()
        assertEquals("T6_MAIN_SWORD@1", flip.itemName)
        assertEquals(6, flip.tier)
        assertEquals("Lymhurst", flip.buyCity)
        assertEquals(100_000, flip.buyPrice)
        assertEquals(200_000, flip.sellPrice)
        assertEquals(79_000, flip.profit)
    }

    @Test
    fun flipDetector_recalculatesProfit_whenPremiumToggled() {
        val detector = FlipDetector()
        detector.isPremiumActive = false

        detector.processMarketOrder(
            MarketOrder("T8_BAG", 4002, 1, 100_000L, 1, "offer")
        )
        detector.processMarketOrder(
            MarketOrder("T8_BAG", 3003, 1, 200_000L, 1, "request")
        )

        // Switch to Premium (6.5% tax -> 200,000 * 0.935 - 100,000 = 87,000 profit)
        detector.isPremiumActive = true
        val recalculated = detector.recalculateAllFlips()
        assertEquals(1, recalculated.size)
        assertEquals("Fort Sterling", recalculated.first().buyCity)
        assertEquals(87_000, recalculated.first().profit)
        assertTrue(recalculated.first().profitPercentage > 80.0)
    }
}
