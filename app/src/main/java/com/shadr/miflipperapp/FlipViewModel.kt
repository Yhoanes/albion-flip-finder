package com.shadr.miflipperapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FlipViewModel(application: Application) : AndroidViewModel(application) {

    private val _isPremiumActive = MutableLiveData<Boolean>(false)
    val isPremiumActive: LiveData<Boolean> = _isPremiumActive

    fun setPremiumStatus(isActive: Boolean) {
        if (_isPremiumActive.value != isActive) {
            _isPremiumActive.value = isActive
            recalculateWatchlistFlips()
        }
    }

    private val flipMap = mutableMapOf<String, FlipOpportunity>()
    private val _flips = MutableLiveData<List<FlipOpportunity>>(emptyList())
    val flips: LiveData<List<FlipOpportunity>> = _flips

    private val watchlistMap = mutableMapOf<String, FlipOpportunity>()
    private val _watchlistFlips = MutableLiveData<List<FlipOpportunity>>(emptyList())
    val watchlistFlips: LiveData<List<FlipOpportunity>> = _watchlistFlips

    val statusText = MutableLiveData(application.getString(R.string.service_stopped))

    val isServiceRunning = MutableLiveData(false)
    var selectedSpinnerPosition = 0

    fun addToWatchlist(flip: FlipOpportunity) {
        synchronized(watchlistMap) {
            watchlistMap[flip.uniqueId] = flip
            recalculateWatchlistFlips()
        }
    }

    fun removeFromWatchlist(flip: FlipOpportunity) {
        synchronized(watchlistMap) {
            watchlistMap.remove(flip.uniqueId)
            recalculateWatchlistFlips()
        }
    }

    private fun recalculateWatchlistFlips() {
        val currentPremiumStatus = _isPremiumActive.value ?: false
        val taxRate = if (currentPremiumStatus) 0.065 else 0.105
        val recalculatedList = synchronized(watchlistMap) {
            watchlistMap.values.map { originalFlip ->
                val newProfit = (originalFlip.sellPrice * (1 - taxRate)) - originalFlip.buyPrice
                originalFlip.copy(
                    profit = newProfit.toInt(),
                    profitPercentage = if (originalFlip.buyPrice > 0) (newProfit / originalFlip.buyPrice.toDouble()) * 100 else 0.0
                )
            }.filter { it.profit > 0 }
                .sortedByDescending { it.timestamp }
        }
        _watchlistFlips.value = recalculatedList
    }

    fun clearWatchlist() {
        synchronized(watchlistMap) {
            watchlistMap.clear()
            _watchlistFlips.value = emptyList()
        }
    }

    fun processFlipUpdate(flip: FlipOpportunity) {
        synchronized(flipMap) {
            flipMap[flip.uniqueId] = flip
            val currentList = flipMap.values.toList()
            _flips.value = currentList
        }
    }

    fun setFlipList(flipList: List<FlipOpportunity>) {
        synchronized(flipMap) {
            flipMap.clear()
            flipList.forEach { flip ->
                flipMap[flip.uniqueId] = flip
            }
            val currentList = flipMap.values.toList()
            _flips.value = currentList
        }
    }

    fun removeFlip(flip: FlipOpportunity) {
        synchronized(flipMap) {
            flipMap.remove(flip.uniqueId)
            val currentList = flipMap.values.toList()
            _flips.value = currentList
        }
    }

    fun addFlip(flip: FlipOpportunity, position: Int? = null) {
        processFlipUpdate(flip)
    }

    fun clearFlipList() {
        synchronized(flipMap) {
            flipMap.clear()
            _flips.value = emptyList()
        }
    }
}