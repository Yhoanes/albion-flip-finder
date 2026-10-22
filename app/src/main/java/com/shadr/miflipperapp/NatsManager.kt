package com.shadr.miflipperapp

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.nats.client.Connection
import io.nats.client.Nats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.StandardCharsets

class NatsManager {

    private var natsConnection: Connection? = null
    private val serverUrls = mapOf(
        "Europe" to "nats://public:thenewalbiondata@nats.albion-online-data.com:34222",
        "America" to "nats://public:thenewalbiondata@nats.albion-online-data.com:4222",
        "Asia" to "nats://public:thenewalbiondata@nats.albion-online-data.com:24222"
    )

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val marketOrderAdapter: JsonAdapter<MarketOrder> = moshi.adapter(MarketOrder::class.java)

    suspend fun connect(serverName: String): Boolean {
        val url = serverUrls[serverName] ?: return false
        return withContext(Dispatchers.IO) {
            try {
                natsConnection?.close()
                natsConnection = Nats.connect(url)
                natsConnection?.status == Connection.Status.CONNECTED
            } catch (e: IOException) {
                e.printStackTrace()
                false
            } catch (e: InterruptedException) {
                e.printStackTrace()
                false
            }
        }
    }

    fun subscribeToMarketOrders(onOrderReceived: (MarketOrder) -> Unit) {
        val dispatcher = natsConnection?.createDispatcher { msg ->
            val jsonString = String(msg.data, StandardCharsets.UTF_8)

            val marketOrder = marketOrderAdapter.fromJson(jsonString)

            if (marketOrder != null) {
                onOrderReceived(marketOrder)
            }
        }

        dispatcher?.subscribe("marketorders.deduped")
    }
}