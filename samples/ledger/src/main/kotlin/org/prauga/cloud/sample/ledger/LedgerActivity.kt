// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sample.ledger

import android.app.Activity
import android.os.Bundle

class LedgerActivity : Activity() {

    private lateinit var screen: SampleScreen
    private val ledger by lazy { Ledger(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen = SampleScreen(this)
        screen.button(this, R.string.add_expense) {
            ledger.addExpense(EXPENSE)
            render()
        }
        screen.button(this, R.string.set_budget) {
            ledger.budget.writeText(getString(R.string.budget_value))
            ledger.settings.edit().putString(Ledger.CURRENCY, "EUR").apply()
            render()
        }
        screen.button(this, R.string.write_draft) {
            ledger.draft.writeText(getString(R.string.draft_value))
            render()
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val (count, total) = ledger.expenses()
        screen.status.text =
            getString(
                R.string.status,
                count,
                total,
                ledger.settings.getString(Ledger.CURRENCY, null) ?: getString(R.string.none),
                if (ledger.budget.isFile) ledger.budget.readText() else getString(R.string.none),
                getString(if (ledger.draft.isFile) R.string.present else R.string.none),
            )
    }

    private companion object {
        const val EXPENSE = 12
    }
}
