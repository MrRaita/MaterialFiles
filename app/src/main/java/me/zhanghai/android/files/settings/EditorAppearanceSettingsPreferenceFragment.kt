package me.zhanghai.android.files.settings

import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.widget.EditText
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.zhanghai.android.files.R
import me.zhanghai.android.files.ui.PreferenceFragmentCompat

class EditorAppearanceSettingsPreferenceFragment : PreferenceFragmentCompat() {
    private val plainText by lazy { requireActivity().intent.getBooleanExtra(EditorAppearanceSettingsActivity.EXTRA_PLAIN_TEXT, false) }
    private fun key(base: String): String = if (plainText) "key_text_editor_${base.removePrefix("key_editor_")}" else "key_code_editor_${base.removePrefix("key_editor_")}"
    private val prefs by lazy { androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()) }
    private val colorKeys = listOf(
        "key_editor_color_background", "key_editor_color_text", "key_editor_color_comment",
        "key_editor_color_keyword", "key_editor_color_string", "key_editor_color_number",
        "key_editor_color_type", "key_editor_color_function", "key_editor_color_variable",
        "key_editor_color_constant", "key_editor_color_operator", "key_editor_color_tag",
        "key_editor_color_attribute", "key_editor_color_punctuation", "key_editor_color_current_line",
        "key_editor_color_selection", "key_editor_color_gutter_background", "key_editor_color_gutter_text",
        "key_editor_color_cursor", "key_editor_toolbar_background", "key_editor_toolbar_icon"
    )

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_editor_appearance)
        val baseKeys = colorKeys + listOf("key_editor_theme", "key_editor_font_family", "key_editor_font_weight", "key_editor_font_ligatures", "key_editor_appearance_reset")
        baseKeys.forEach { base -> findPreference<Preference>(base)?.key = key(base) }

        (findPreference<Preference>(key("key_editor_theme")) as? ListPreference)?.let {
            it.value = prefs.getString(it.key, "github_dark") ?: "github_dark"
        }
        (findPreference<Preference>(key("key_editor_font_family")) as? ListPreference)?.let {
            it.value = prefs.getString(it.key, "monospace") ?: "monospace"
        }
        (findPreference<Preference>(key("key_editor_font_weight")) as? ListPreference)?.let {
            it.value = prefs.getString(it.key, "normal") ?: "normal"
        }
        (findPreference<Preference>(key("key_editor_font_ligatures")) as? SwitchPreferenceCompat)?.let {
            it.isChecked = prefs.getBoolean(it.key, true)
        }

        findPreference<Preference>("key_editor_syntax_colors_category")?.isVisible = !plainText
        findPreference<Preference>(key("key_editor_appearance_reset"))?.setOnPreferenceClickListener {
            prefs.edit().apply {
                colorKeys.forEach { remove(key(it)) }
                remove(key("key_editor_theme"))
                remove(key("key_editor_font_family"))
                remove(key("key_editor_font_weight"))
                remove(key("key_editor_font_ligatures"))
            }.apply()
            (findPreference<Preference>(key("key_editor_theme")) as? ListPreference)?.value = "github_dark"
            (findPreference<Preference>(key("key_editor_font_family")) as? ListPreference)?.value = "monospace"
            (findPreference<Preference>(key("key_editor_font_weight")) as? ListPreference)?.value = "normal"
            (findPreference<Preference>(key("key_editor_font_ligatures")) as? SwitchPreferenceCompat)?.isChecked = true
            refreshSummaries()
            true
        }
        colorKeys.forEach { base ->
            findPreference<Preference>(key(base))?.setOnPreferenceClickListener {
                showColorDialog(base)
                true
            }
        }
        refreshSummaries()
    }

    override fun onResume() {
        super.onResume()
        refreshSummaries()
    }

    private fun refreshSummaries() {
        colorKeys.forEach { base ->
            findPreference<Preference>(key(base))?.summary = prefs.getString(key(base), null) ?: getString(R.string.settings_editor_color_theme_default)
        }
    }

    private fun defaultColor(key: String): String = when (key) {
        "key_editor_color_background" -> "#1E1E1E"
        "key_editor_color_text" -> "#D4D4D4"
        "key_editor_color_comment" -> "#6A9955"
        "key_editor_color_keyword" -> "#569CD6"
        "key_editor_color_string" -> "#CE9178"
        "key_editor_color_number" -> "#B5CEA8"
        "key_editor_color_type" -> "#4EC9B0"
        "key_editor_color_function" -> "#DCDCAA"
        "key_editor_color_variable" -> "#9CDCFE"
        "key_editor_color_constant" -> "#4FC1FF"
        "key_editor_color_operator" -> "#D4D4D4"
        "key_editor_color_tag" -> "#569CD6"
        "key_editor_color_attribute" -> "#9CDCFE"
        "key_editor_color_punctuation" -> "#D4D4D4"
        "key_editor_color_current_line" -> "#252526"
        "key_editor_color_selection" -> "#264F78"
        "key_editor_color_gutter_background" -> "#1E1E1E"
        "key_editor_color_gutter_text" -> "#858585"
        "key_editor_color_cursor" -> "#FFFFFF"
        "key_editor_toolbar_background" -> "#F5F5F5"
        "key_editor_toolbar_icon" -> "#424242"
        else -> "#000000"
    }

    private fun showColorDialog(base: String) {
        val input = EditText(requireContext()).apply {
            setText(prefs.getString(key(base), null) ?: "#RRGGBB")
            selectAll()
            hint = "#RRGGBB"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(findPreference<Preference>(key(base))?.title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = input.text.toString().trim()
                if (isValidColor(value)) {
                    prefs.edit().putString(key(base), value.uppercase()).apply()
                    refreshSummaries()
                }
            }
            .show()
    }

    private fun isValidColor(value: String): Boolean = try {
        Color.parseColor(value)
        true
    } catch (_: IllegalArgumentException) {
        false
    }
}
