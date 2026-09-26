package me.zhanghai.android.files.settings

import android.content.Intent
import android.os.Bundle
import me.zhanghai.android.files.R

class EditorAppearanceSettingsActivity : me.zhanghai.android.files.app.AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editor_appearance_settings_fragment)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_editor_appearance_title)
    }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
    companion object {
        const val EXTRA_PLAIN_TEXT = "plain_text"
        fun createIntent(context: android.content.Context, plainText: Boolean): Intent = Intent(context, EditorAppearanceSettingsActivity::class.java).apply { putExtra(EXTRA_PLAIN_TEXT, plainText) }
    }
}
