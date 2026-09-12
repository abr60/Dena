package com.dena.core

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun wrap(context: Context): Context {
        val lang = DenaPreferences(context).getLanguage()
        return updateResources(context, lang)
    }

    fun updateResources(context: Context, lang: String): Context {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList.forLanguageTags(lang))
        return context.createConfigurationContext(config)
    }

    fun persistAndApply(context: Context, lang: String) {
        DenaPreferences(context).setLanguage(lang)
        updateResources(context, lang)
    }
}
