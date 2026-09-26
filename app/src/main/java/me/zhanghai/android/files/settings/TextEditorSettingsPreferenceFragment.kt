/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.settings

import android.os.Bundle
import me.zhanghai.android.files.R
import me.zhanghai.android.files.ui.PreferenceFragmentCompat

class TextEditorSettingsPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_text_editor)
        findPreference<androidx.preference.Preference>(getString(R.string.pref_key_editor_toolbar_settings))?.setOnPreferenceClickListener {
            startActivity(EditorToolbarSettingsActivity.createIntent(true))
            true
        }
        findPreference<androidx.preference.Preference>(getString(R.string.pref_key_editor_appearance_settings))?.setOnPreferenceClickListener {
            startActivity(EditorAppearanceSettingsActivity.createIntent(requireContext(), true))
            true
        }
    }
}
