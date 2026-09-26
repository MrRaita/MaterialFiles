package me.zhanghai.android.files.settings

import android.os.Bundle
import me.zhanghai.android.files.R
import me.zhanghai.android.files.ui.PreferenceFragmentCompat

class CodeEditorSettingsPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_code_editor)
        findPreference<androidx.preference.Preference>(getString(R.string.pref_key_editor_toolbar_settings))?.setOnPreferenceClickListener {
            startActivity(EditorToolbarSettingsActivity.createIntent(false))
            true
        }
        findPreference<androidx.preference.Preference>(getString(R.string.pref_key_editor_appearance_settings))?.setOnPreferenceClickListener {
            startActivity(EditorAppearanceSettingsActivity.createIntent(requireContext(), false))
            true
        }
    }
}
