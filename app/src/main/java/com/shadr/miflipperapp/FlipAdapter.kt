package com.shadr.miflipperapp

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FlipAdapter(
    private val flips: MutableList<FlipOpportunity>,
    private val itemManager: ItemManager
) : RecyclerView.Adapter<FlipAdapter.FlipViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateTimeRunnable: Runnable

    companion object {
        private const val UPDATE_INTERVAL_MS = 30000L
        private const val PAYLOAD_UPDATE_TIME = "PAYLOAD_UPDATE_TIME"

        private val QUALITY_RESOURCE_MAP = mapOf(
            "1" to R.string.quality_normal,
            "2" to R.string.quality_good,
            "3" to R.string.quality_outstanding,
            "4" to R.string.quality_excellent,
            "5" to R.string.quality_masterpiece
        )
    }

    init {
        updateTimeRunnable = Runnable {
            notifyItemRangeChanged(0, flips.size, PAYLOAD_UPDATE_TIME)
            handler.postDelayed(updateTimeRunnable, UPDATE_INTERVAL_MS)
        }
    }

    inner class FlipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameText: TextView = itemView.findViewById(R.id.item_name_text)
        val profitText: TextView = itemView.findViewById(R.id.profit_text)
        val buyCityText: TextView = itemView.findViewById(R.id.buy_city_text)
        val buyPriceText: TextView = itemView.findViewById(R.id.buy_price_text)
        val sellPriceText: TextView = itemView.findViewById(R.id.sell_price_text)
        val profitPercentageText: TextView = itemView.findViewById(R.id.profit_percentage_text)
        val timestampText: TextView = itemView.findViewById(R.id.timestamp_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_flip, parent, false)
        return FlipViewHolder(view)
    }

    override fun onBindViewHolder(holder: FlipViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_UPDATE_TIME)) {
            val currentFlip = flips[position]
            holder.timestampText.text = getRelativeTime(currentFlip.timestamp)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: FlipViewHolder, position: Int) {
        val currentFlip = flips[position]
        val context = holder.itemView.context

        val baseName = itemManager.getDisplayName(currentFlip.itemName)

        val enchantment = getEnchantmentFromId(currentFlip.itemName)

        val tierPrefix = "T${currentFlip.tier}.${enchantment}"
        val fullItemName = "$tierPrefix $baseName"

        val qualityResId = QUALITY_RESOURCE_MAP[currentFlip.quality] ?: 0
        val qualityName = if (qualityResId != 0) {
            context.getString(qualityResId)
        } else {
            ""
        }

        holder.itemNameText.text = "$fullItemName ($qualityName)"

        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())

        holder.buyCityText.text = currentFlip.buyCity
        holder.buyPriceText.text = formatter.format(currentFlip.buyPrice)
        holder.sellPriceText.text = formatter.format(currentFlip.sellPrice)
        holder.profitText.text = formatter.format(currentFlip.profit)
        holder.profitPercentageText.text = String.format(Locale.getDefault(), "(+%.2f%%)", currentFlip.profitPercentage)
        holder.timestampText.text = getRelativeTime(currentFlip.timestamp)
    }

    override fun getItemCount(): Int = flips.size

    fun updateFlips(newFlips: List<FlipOpportunity>) {
        flips.clear()
        flips.addAll(newFlips)
        notifyDataSetChanged()
    }

    fun getFlipAt(position: Int): FlipOpportunity {
        return flips[position]
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        handler.post(updateTimeRunnable)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun getEnchantmentFromId(itemId: String): Int {
        val match = Regex("@(\\d)").find(itemId)
        return match?.groups?.get(1)?.value?.toIntOrNull() ?: 0
    }

    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        if (seconds < 60) return "${seconds}s ago"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        if (minutes < 60) return "${minutes}m ago"
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        if (hours < 24) return "${hours}h ago"
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return "${days}d ago"
    }
}
