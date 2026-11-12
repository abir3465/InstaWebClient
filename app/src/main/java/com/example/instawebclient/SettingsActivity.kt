package com.example.instawebclient

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

            val desktopPref = androidx.preference.SwitchPreferenceCompat(context).apply {
                key = "desktop_mode"
                title = "Desktop Mode"
                summary = "Use desktop user-agent (sometimes helps with media detection)"
                setDefaultValue(false)
            }

            val darkPref = androidx.preference.SwitchPreferenceCompat(context).apply {
                key = "force_dark"
                title = "Force Dark Background"
                summary = "Experimental – may not work on all pages"
                setDefaultValue(true)
            }

            screen.addPreference(desktopPref)
            screen.addPreference(darkPref)

            preferenceScreen = screen
        }
    }
}
