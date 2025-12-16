package com.lkps.ctApp.controllers.locale

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.preference.PreferenceManager
import com.lkps.ct.BuildConfig
import java.util.*

object LocaleController {

    /*Constants ------------------------------------------------------------------------*/
    const val COUNTR_LANG = "en"
    const val SELECTED_LANGUAGE = "LocaleController." + BuildConfig.APPLICATION_ID

    fun onAttach(context: Context, countryCode: String): Context? =
        setLanguage(context, getPersistedData(context, Locale.getDefault().language), countryCode)

    fun onAttach(context: Context?, defaultLanguage: String, countryCode: String): Context? =
        setLanguage(context, getPersistedData(context, defaultLanguage), countryCode)

    fun getLanguage(context: Context): String? =
        getPersistedData(context, Locale.getDefault().language)

    fun setLanguage(context: Context?, language: String?, countryCode: String): Context? {
        persist(context, language)
        return updateResources(context, language, countryCode)
    }

    fun setLocale(resources: Resources) {
        val config = Configuration(resources.configuration)
        config.locale = Locale.ENGLISH
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun getPersistedData(context: Context?, defaultLanguage: String): String? =
        context?.let {
            PreferenceManager.getDefaultSharedPreferences(it)
                .getString(SELECTED_LANGUAGE, defaultLanguage)
        }

    private fun persist(context: Context?, language: String?) {
        val preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val editor = preferences?.edit()
        editor?.putString(SELECTED_LANGUAGE, language)
        editor?.apply()
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(context: Context?, language: String?, countryCode: String): Context? {
        val locale = Locale(language, countryCode)
        Locale.setDefault(locale)

        val configuration = context?.resources?.configuration
        var newContext: Context? = null

        configuration?.apply {
            setLocale(locale)
            setLayoutDirection(locale)
            newContext = context.createConfigurationContext(configuration)
        }
        updateResourcesLegacy(newContext, language)
        return newContext
    }

    private fun updateResourcesLegacy(context: Context?, language: String?): Context? {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context?.resources
        val configuration = resources?.configuration

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ->
                configuration?.setLocales(LocaleList(locale))
            else -> configuration?.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            configuration?.setLayoutDirection(locale)

        resources?.updateConfiguration(configuration, resources.displayMetrics)

        return context
    }
}