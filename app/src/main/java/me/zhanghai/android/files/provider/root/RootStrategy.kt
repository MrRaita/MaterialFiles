/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.provider.root

enum class RootStrategy {
    NEVER,
    AUTOMATIC,
    ALWAYS,
    // Same file-access behavior as ALWAYS (always use the root code path), but RootFileService
    // reads this value to force the Shizuku backend specifically instead of trying su first.
    // Kept as the last entry so its ordinal (3) never collides with existing saved preferences.
    SHIZUKU
}
