// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.Context
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal object AgentRuntime {
    val executor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "pvot-cloud-sdk").apply { isDaemon = true }
    }

    @Volatile private var work: AgentWork? = null

    fun work(context: Context): AgentWork =
        work
            ?: synchronized(this) {
                work
                    ?: AgentWork(
                            context.applicationContext,
                            AgentConfigLoader.load(context.applicationContext),
                        )
                        .also { work = it }
            }
}
