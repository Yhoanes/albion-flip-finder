package com.shadr.miflipperapp

import android.content.Context
import android.util.Log
import java.io.InputStream

class ItemManager(private val context: Context) {

    private val itemMap = mutableMapOf<String, String>()

    init {
        val languageCode = LocaleHelper.getPersistedLocaleCode(context)

        val appContext = context.applicationContext
        val localizedContext = LocaleHelper.setLocale(appContext, languageCode)

        loadItems(languageCode, localizedContext)
    }

    fun loadItems(languageCode: String, localizedContext: Context) {
        itemMap.clear()

        val filePath = if (languageCode == Constants.DEFAULT_LANGUAGE) {
            "items.txt"
        } else {
            "$languageCode/items.txt"
        }

        var inputStream: InputStream? = null
        try {
            inputStream = localizedContext.assets.open(filePath)

            inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->

                    val parts = line.split(":")

                    if (parts.size >= 3) {
                        val rawItemId = parts[1].trim()
                        val itemName = parts.last().trim()

                        val fullIdKey = rawItemId.lowercase()
                        val baseIdKey = rawItemId.substringBefore('@').lowercase()
                        val baseName = itemName.substringAfter(" ").trim()

                        itemMap[fullIdKey] = baseName
                        itemMap[baseIdKey] = baseName
                    }
                }
            }
            Log.d("ItemManager", "Items loaded successfully from: $filePath")

        } catch (e: Exception) {
            if (languageCode != Constants.DEFAULT_LANGUAGE) {
                Log.e("ItemManager", "Error loading localized items for $languageCode. Falling back to default 'en'.", e)
                loadItems(Constants.DEFAULT_LANGUAGE, context.applicationContext)
            } else {
                Log.e("ItemManager", "Critical error: Could not load default items.txt.", e)
            }
        } finally {
            inputStream?.close()
        }
    }

    fun getDisplayName(itemId: String): String {
        return itemMap[itemId.lowercase()] ?: itemId
    }
}