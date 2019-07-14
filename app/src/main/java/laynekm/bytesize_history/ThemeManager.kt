package laynekm.bytesize_history

import android.content.Context

class ThemeManager(val context: Context, val recreate: () -> Unit) {

    private val preferencesKey = context.getString(R.string.preferences_key)
    private val themePrefKey = context.getString(R.string.theme_pref_key)
    private val sharedPref = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)

    // Applies theme from shared preferences
    fun applyTheme() {
        when (sharedPref.getString(themePrefKey, "light")) {
            "dark" -> context.setTheme(R.style.AppTheme_Dark)
            "light" -> context.setTheme(R.style.AppTheme_Light)
        }
    }

    fun getTheme(): String {
        return sharedPref.getString(themePrefKey, "light")!!
    }

    // Sets theme from argument and updates shared preferences
    fun setTheme(theme: String) {
        when (theme) {
            "dark" -> context.setTheme(R.style.AppTheme_Dark)
            "light" -> context.setTheme(R.style.AppTheme_Light)
        }

        with (sharedPref.edit()) {
            putString(themePrefKey, theme)
            apply()
        }

        recreate()
    }
}