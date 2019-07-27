package laynekm.bytesize_history

import android.content.Context
import android.content.SharedPreferences

fun noop() { return }

class ThemeManager(val context: Context, val recreate: () -> Unit = ::noop) {

    private val preferencesKey: String = context.getString(R.string.preferences_key)
    private val themePrefKey: String = context.getString(R.string.theme_pref_key)
    private val sharedPref: SharedPreferences = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)
    private var currentTheme: Theme = stringToTheme(sharedPref.getString(themePrefKey, "light"))

    // Applies theme from shared preferences
    fun applyTheme() {
        when (currentTheme) {
            Theme.DARK -> context.setTheme(R.style.AppTheme_Dark)
            Theme.LIGHT -> context.setTheme(R.style.AppTheme_Light)
        }
    }

    fun getTheme(): Theme {
        return this.currentTheme
    }

    // Sets theme from argument and updates shared preferences
    fun toggleTheme() {
        when (currentTheme) {
            Theme.DARK -> {
                currentTheme = Theme.LIGHT
                context.setTheme(R.style.AppTheme_Dark)
            }
            Theme.LIGHT -> {
                currentTheme = Theme.DARK
                context.setTheme(R.style.AppTheme_Light)
            }
        }

        with (sharedPref.edit()) {
            putString(themePrefKey, "$currentTheme")
            apply()
        }

        recreate()
    }
}