package com.shadr.miflipperapp

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import android.content.Context

class MiFlipperApplication : Application(), ViewModelStoreOwner {

    override val viewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    val mainViewModel: FlipViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        ).get(FlipViewModel::class.java)
    }

    override fun attachBaseContext(base: Context) {
        val localeCode = LocaleHelper.getPersistedLocaleCode(base)

        val contextWithLocale = LocaleHelper.setLocale(base, localeCode)

        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate() {
        super.onCreate()

    }
}