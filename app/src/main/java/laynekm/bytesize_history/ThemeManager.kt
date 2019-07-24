package laynekm.bytesize_history

import android.content.Context

fun default() {
    return
}

class ThemeManager(val context: Context, val recreate: () -> Unit = ::default) {

    private val preferencesKey = context.getString(R.string.preferences_key)
    private val themePrefKey = context.getString(R.string.theme_pref_key)
    private val sharedPref = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)
    private var currentTheme = sharedPref.getString(themePrefKey, "light")

    // Applies theme from shared preferences
    fun applyTheme() {
        when (currentTheme) {
            "dark" -> context.setTheme(R.style.AppTheme_Dark)
            "light" -> context.setTheme(R.style.AppTheme_Light)
        }
    }

    fun getTheme(): String {
        return this.currentTheme
    }

    // Sets theme from argument and updates shared preferences
    fun toggleTheme() {
        when (currentTheme) {
            "dark" -> {
                currentTheme = "light"
                context.setTheme(R.style.AppTheme_Dark)
            }
            "light" -> {
                currentTheme = "dark"
                context.setTheme(R.style.AppTheme_Light)
            }
        }

        with (sharedPref.edit()) {
            putString(themePrefKey, currentTheme)
            apply()
        }

        recreate()
    }
}