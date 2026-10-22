package com.shadr.miflipperapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.NumberFormat
import java.util.Locale

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val FLIP_CHANNEL_ID = "flip_channel"
        private const val FLIP_NOTIFICATION_ID = 1

        private const val FOREGROUND_CHANNEL_ID = "foreground_service_channel"
        const val FOREGROUND_NOTIFICATION_ID = 2

        // Mapeo de Quality ID a Resource ID (para localización de calidad)
        private val QUALITY_RESOURCE_MAP = mapOf(
            "1" to R.string.quality_normal,
            "2" to R.string.quality_good,
            "3" to R.string.quality_outstanding,
            "4" to R.string.quality_excellent,
            "5" to R.string.quality_masterpiece
        )
    }

    fun createFlipNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FLIP_CHANNEL_ID,
                context.getString(R.string.flip_channel_name), // Usar solo R.string
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = context.getString(R.string.flip_channel_description) // Usar solo R.string
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                context.getString(R.string.foreground_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createForegroundNotification(): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_service_status))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showFlipNotification(opportunity: FlipOpportunity, itemManager: ItemManager) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())

        val baseName = itemManager.getDisplayName(opportunity.itemName)
        val enchantment = getEnchantmentFromId(opportunity.itemName)
        val tierPrefix = "T${opportunity.tier}.${enchantment}"
        val fullItemName = "$tierPrefix $baseName"

        val qualityResId = QUALITY_RESOURCE_MAP[opportunity.quality] ?: 0
        val qualityName = if (qualityResId != 0) {
            context.getString(qualityResId)
        } else {
            ""
        }

        val profitText = formatter.format(opportunity.profit)
        val buyPriceFormatted = formatter.format(opportunity.buyPrice)
        val sellPriceFormatted = formatter.format(opportunity.sellPrice)


        val expandedText = context.getString(R.string.notification_flip_expanded,
            fullItemName, qualityName,
            opportunity.buyCity, buyPriceFormatted,
            sellPriceFormatted, profitText
        )

        val builder = NotificationCompat.Builder(context, FLIP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)

            .setContentTitle(context.getString(R.string.notification_flip_title))

            .setContentText(context.getString(R.string.notification_flip_summary, fullItemName, qualityName, profitText))
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(FLIP_NOTIFICATION_ID, builder.build())
    }

    private fun getEnchantmentFromId(itemId: String): Int {
        val match = Regex("@(\\d)").find(itemId)
        return match?.groups?.get(1)?.value?.toIntOrNull() ?: 0
    }
}