package com.shadr.miflipperapp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import android.util.Log

class FlipDetectionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private val natsManager = NatsManager()
    private val flipDetector = FlipDetector()
    private lateinit var itemManager: ItemManager
    private lateinit var notificationHelper: NotificationHelper

    companion object {
        const val ACTION_UPDATE_PREMIUM_STATUS = "com.example.miflipperapp.UPDATE_PREMIUM"
        const val EXTRA_PREMIUM_STATUS = "NEW_PREMIUM_STATUS"
        private const val DEFAULT_MIN_PROFIT = 0
    }

    override fun onCreate() {
        super.onCreate()
        itemManager = ItemManager(this)
        notificationHelper = NotificationHelper(this)

        notificationHelper.createFlipNotificationChannel()
        notificationHelper.createForegroundNotificationChannel()

        updatePremiumStatus()
        setupFlipDetector()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_UPDATE_PREMIUM_STATUS) {
            val newStatus = intent.getBooleanExtra(EXTRA_PREMIUM_STATUS, false)

            serviceScope.launch {
                flipDetector.isPremiumActive = newStatus

                val recalculatedList = flipDetector.recalculateAllFlips()

                withContext(Dispatchers.Main) {
                    val app = applicationContext as MiFlipperApplication
                    app.mainViewModel.setFlipList(recalculatedList)
                }
            }
            return START_STICKY
        }

        val serverName = intent?.getStringExtra("SERVER_NAME") ?: "Europe"

        val notification = notificationHelper.createForegroundNotification()
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)

        serviceScope.launch {
            if (natsManager.connect(serverName)) {
                natsManager.subscribeToMarketOrders { marketOrder ->
                    flipDetector.processMarketOrder(marketOrder)
                }
            } else {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun setupFlipDetector() {
        flipDetector.setOnFlipsFoundListener { flips ->
            serviceScope.launch {
                val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

                val minNotificationProfit = prefs.getInt("min_notification_profit", DEFAULT_MIN_PROFIT)
                val isFilterActive = prefs.getBoolean("is_notification_filter_active", true)

                val app = applicationContext as MiFlipperApplication

                Log.d("FlipService", "Filtro activo: $isFilterActive, Mínimo de ganancia (guardado): $minNotificationProfit")

                flips.forEach { flip ->

                    val meetsProfitThreshold = flip.profit >= minNotificationProfit

                    val shouldNotify = !isFilterActive || (isFilterActive && meetsProfitThreshold)

                    Log.d("FlipService", "Flip ${flip.itemName} Profit: ${flip.profit}. ¿Notificar? $shouldNotify")

                    if (shouldNotify) {
                        notificationHelper.showFlipNotification(flip, itemManager)
                    }

                    withContext(Dispatchers.Main) {
                        app.mainViewModel.processFlipUpdate(flip)
                    }

                    delay(500)
                }
            }
        }
    }

    private fun updatePremiumStatus() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val isPremium = prefs.getBoolean("is_premium", false)
        flipDetector.isPremiumActive = isPremium
    }


    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}