package pl.edu.finanseosobiste.ui

import android.content.Context
import android.content.res.Configuration
import pl.edu.finanseosobiste.R
import java.util.Locale

object AppSettings {
    val currencies = arrayOf("PLN", "UAH", "EUR", "USD", "GBP", "CZK", "CHF")
    fun configureMoney(context: Context) {
        val prefs=preferences(context)
        pl.edu.finanseosobiste.util.BudgetLimitCalculator.currencyCode=prefs.getString("currency","PLN") ?: "PLN"
        pl.edu.finanseosobiste.util.BudgetLimitCalculator.displayLocale=Locale.forLanguageTag(prefs.getString("language","en") ?: "en")
    }
    fun preferences(context: Context) = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun localized(context: Context): Context {
        val code = preferences(context).getString("language", "en") ?: "en"
        val locale = Locale.forLanguageTag(code)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
    fun theme(context: Context) = when (preferences(context).getInt("theme", 0)) {
        1 -> R.style.Theme_FinanseOsobiste_Lavender
        2 -> R.style.Theme_FinanseOsobiste_Coral
        3 -> R.style.Theme_FinanseOsobiste_Amber
        4 -> R.style.Theme_FinanseOsobiste_Rose
        5 -> R.style.Theme_FinanseOsobiste_Indigo
        6 -> R.style.Theme_FinanseOsobiste_Sky
        7 -> R.style.Theme_FinanseOsobiste_Teal
        8 -> R.style.Theme_FinanseOsobiste_Emerald
        9 -> R.style.Theme_FinanseOsobiste_Plum
        10 -> R.style.Theme_FinanseOsobiste_Cocoa
        11 -> R.style.Theme_FinanseOsobiste_Slate
        12 -> R.style.Theme_FinanseOsobiste_Crimson
        else -> R.style.Theme_FinanseOsobiste
    }
    fun category(context: Context, name: String): String {
        val id = when (name) {
            "Jedzenie i zakupy" -> R.string.cat_food
            "Mieszkanie i rachunki" -> R.string.cat_home
            "Transport i paliwo" -> R.string.cat_transport
            "Rozrywka i kultura" -> R.string.cat_fun
            "Zdrowie i sport" -> R.string.cat_health
            "Inne wydatki" -> R.string.cat_other
            else -> return name
        }
        return context.getString(id)
    }
}
