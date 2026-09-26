/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filelist

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.CreateFileDialogBinding
import me.zhanghai.android.files.databinding.NameDialogNameIncludeBinding
import me.zhanghai.android.files.util.show

class CreateFileDialogFragment : FileNameDialogFragment() {
    override val listener: Listener
        get() = super.listener as Listener

    @StringRes
    override val titleRes: Int = R.string.file_create_file_title

    private val extensions = listOf(
        "txt", "xml", "json", "md", "java", "kt", "kts", "py", "js", "ts", "tsx",
        "html", "css", "sh", "bash", "zsh", "fish", "gradle", "toml", "yaml", "yml",
        "properties", "ini", "conf", "sql", "smali"
    )

    override fun onInflateBinding(inflater: LayoutInflater): NameDialogFragment.Binding {
        val b = CreateFileDialogBinding.inflate(inflater)
        val name = NameDialogNameIncludeBinding.bind(b.root)
        extensions.forEach { extension ->
            val chip = Chip(requireContext()).apply {
                text = ".${extension}"
                isCheckable = true
                setOnClickListener { applyExtension(extension) }
            }
            b.extensionGroup.addView(chip)
        }
        return NameDialogFragment.Binding(b.root, name.nameLayout, name.nameEdit)
    }

    private fun applyExtension(extension: String) {
        val current = binding.nameEdit.text?.toString().orEmpty()
        val base = if (current.contains('.') && !current.endsWith('.')) current.substringBeforeLast('.') else current
        binding.nameEdit.setText(if (base.isBlank()) "dosya.$extension" else "$base.$extension")
        binding.nameEdit.setSelection(0, binding.nameEdit.length())
    }

    override fun onOk(name: String) {
        listener.createFile(name)
    }

    companion object {
        fun show(fragment: Fragment) {
            CreateFileDialogFragment().show(fragment)
        }
    }



    interface Listener : FileNameDialogFragment.Listener {
        fun createFile(name: String)
    }
}
