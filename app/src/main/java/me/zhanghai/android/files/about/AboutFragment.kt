/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import me.zhanghai.android.files.databinding.AboutFragmentBinding
import me.zhanghai.android.files.ui.LicensesDialogFragment
import me.zhanghai.android.files.util.createViewIntent
import me.zhanghai.android.files.util.startActivitySafe

class AboutFragment : Fragment() {
    private lateinit var binding: AboutFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        AboutFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.gitHubLayout.setOnClickListener { startActivitySafe(GITHUB_URI.createViewIntent()) }
        binding.licensesLayout.setOnClickListener { LicensesDialogFragment.show(this) }
        binding.authorNameLayout.setOnClickListener {
            startActivitySafe(Intent(Intent.ACTION_SENDTO, AUTHOR_EMAIL_URI))
        }
        binding.authorGitHubLayout.setOnClickListener {
            startActivitySafe(AUTHOR_GITHUB_URI.createViewIntent())
        }
        binding.authorRedditLayout.setOnClickListener {
            startActivitySafe(AUTHOR_REDDIT_URI.createViewIntent())
        }
        // Hidden until a Discord invite link is set in AUTHOR_DISCORD_URI.
        val discordUri = AUTHOR_DISCORD_URI
        binding.authorDiscordLayout.isVisible = discordUri != null
        binding.authorDiscordLayout.setOnClickListener {
            discordUri?.let { startActivitySafe(it.createViewIntent()) }
        }
    }

    companion object {
        private val GITHUB_URI = Uri.parse("https://github.com/MrRaita/MaterialFiles")
        private val AUTHOR_EMAIL_URI = Uri.parse("mailto:seyfettin.ozviran@protonmail.com")
        private val AUTHOR_GITHUB_URI = Uri.parse("https://github.com/MrRaita")
        private val AUTHOR_REDDIT_URI =
            Uri.parse("https://www.reddit.com/u/_toutseul_/s/LAiNtO2jwS")
        // TODO: Set a Discord invite link here to show the Discord row in the About screen.
        private val AUTHOR_DISCORD_URI: Uri? = null
    }
}
