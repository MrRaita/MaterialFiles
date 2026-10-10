/*
 * Copyright (c) 2026 MrRaita
 * All Rights Reserved.
 */

package me.zhanghai.android.files.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shared background executor, replacing the deprecated `AppExecutors.io`.
 *
 * The sizing mirrors what AsyncTask used, so behavior is unchanged.
 */
object AppExecutors {
    private val cpuCount = Runtime.getRuntime().availableProcessors()

    val io: ExecutorService = ThreadPoolExecutor(
        (cpuCount - 1).coerceIn(2, 4), cpuCount * 2 + 1, 30L, TimeUnit.SECONDS,
        LinkedBlockingQueue(128),
        object : ThreadFactory {
            private val count = AtomicInteger(1)

            override fun newThread(runnable: Runnable): Thread =
                Thread(runnable, "AppExecutors #${count.getAndIncrement()}")
        }
    ).apply { allowCoreThreadTimeOut(true) }
}
