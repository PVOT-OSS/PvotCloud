// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sample.vault

import android.app.Activity
import android.os.Bundle
import java.util.UUID
import org.prauga.cloud.sdk.agent.Credential

class VaultActivity : Activity() {

    private lateinit var screen: SampleScreen
    private val store by lazy { VaultStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen = SampleScreen(this)
        screen.button(this, R.string.add_sign_in) {
            val number = store.all().size + 1
            store.add(
                Credential(
                    id = UUID.randomUUID().toString(),
                    label = getString(R.string.site, number),
                    username = getString(R.string.user, number),
                    secret = UUID.randomUUID().toString(),
                )
            )
            render()
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val credentials = store.all()
        screen.status.text =
            getString(
                R.string.status,
                credentials.size,
                credentials.joinToString(", ") { "${it.label} (${it.username})" },
                credentials.count { it.secret.length == SECRET_LENGTH },
            )
    }

    private companion object {
        const val SECRET_LENGTH = 36
    }
}
