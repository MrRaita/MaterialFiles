/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.viewer.text

import android.content.Intent
import android.os.Bundle
import android.view.View
import java8.nio.file.Path
import me.zhanghai.android.files.file.MimeType
import me.zhanghai.android.files.file.fileProviderUri
import me.zhanghai.android.files.file.intentType
import me.zhanghai.android.files.util.extraPath
import androidx.fragment.app.commit
import me.zhanghai.android.files.app.AppActivity
import me.zhanghai.android.files.app.application
import me.zhanghai.android.files.util.putArgs

open class TextEditorActivity : AppActivity() {
    private lateinit var fragment: TextEditorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            fragment = TextEditorFragment().putArgs(TextEditorFragment.Args(intent))
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        } else {
            fragment = supportFragmentManager.findFragmentById(android.R.id.content)
                as TextEditorFragment
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val path = intent.extraPath ?: return
        if (fragment.switchToFile(path)) {
            setIntent(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (fragment.onSupportNavigateUp()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    companion object {
        private val SPECIAL_EDITOR_EXTENSIONS = setOf(
            "txt", "log", "xml", "axml", "json", "yaml", "yml", "md", "markdown",
            "java", "kt", "kts", "gradle", "py", "js", "mjs", "cjs", "ts", "tsx",
            "html", "htm", "css", "scss", "less", "sql", "sh", "bash", "zsh", "fish",
            "ps1", "bat", "cmd", "smali", "dex", "arsc", "toml", "properties", "ini", "conf",
            "csv", "tsv", "c", "cc", "cpp", "h", "hpp", "cs", "go", "rs", "php", "rb", "swift",
            "dart", "lua", "r", "vue"
        )

        fun shouldHandle(path: Path, mimeType: MimeType): Boolean {
            if (mimeType.type == "text") return true
            if (mimeType.subtype in setOf("json", "javascript", "ecmascript", "typescript", "yaml", "x-sh", "x-shellscript", "xml")) return true
            val name = path.fileName.toString().lowercase()
            return SPECIAL_EDITOR_EXTENSIONS.any { name.endsWith(".$it") }
        }

        const val EXTRA_PLAIN_TEXT_MODE = "plain_text_mode"

        private fun isPlainText(path: Path, mimeType: MimeType): Boolean {
            val name = path.fileName.toString().lowercase()
            if (name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".csv") || name.endsWith(".tsv")) return true
            return mimeType.type == "text" && name.substringAfterLast('.', "") !in setOf(
                "java", "kt", "kts", "gradle", "py", "js", "mjs", "cjs", "ts", "tsx",
                "html", "htm", "css", "scss", "less", "sql", "sh", "bash", "zsh", "fish",
                "ps1", "bat", "cmd", "smali", "toml", "properties", "ini", "conf", "xml",
                "json", "yaml", "yml", "md", "markdown", "c", "cc", "cpp", "h", "hpp",
                "cs", "go", "rs", "php", "rb", "swift", "dart", "lua", "r", "vue"
            )
        }

        fun createIntent(path: Path, mimeType: MimeType): Intent =
            Intent(application, if (isPlainText(path, mimeType)) PlainTextEditorActivity::class.java else TextEditorActivity::class.java)
                .putExtra(EXTRA_PLAIN_TEXT_MODE, isPlainText(path, mimeType))
                .setAction(Intent.ACTION_EDIT)
                .setDataAndType(path.fileProviderUri, mimeType.intentType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .apply { extraPath = path }
    }
}
