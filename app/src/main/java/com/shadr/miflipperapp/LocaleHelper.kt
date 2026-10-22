@file:Suppress("DEPRECATION")

package com.shadr.miflipperapp

import android.content.Context
import android.content.ContextWrapper
import java.util.Locale

object LocaleHelper {

    fun getPersistedLocaleCode(context: Context): String {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        val persistedCode = prefs.getString(Constants.KEY_LANGUAGE, null)

        if (persistedCode != null) {
            return persistedCode
        }

        val systemLocaleCode = context.resources.configuration.locales.get(0).language

        return when (systemLocaleCode) {
            "es", "pt" -> systemLocaleCode
            else -> Constants.DEFAULT_LANGUAGE
        }
    }

    fun setLocale(context: Context, localeCode: String): ContextWrapper {

        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.KEY_LANGUAGE, localeCode).apply()

        val locale = Locale(localeCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        return ContextWrapper(context.createConfigurationContext(config))
    }
}
