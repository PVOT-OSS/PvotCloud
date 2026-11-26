// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sample.vault

import android.app.Activity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

internal class SampleScreen(activity: Activity) {
    private val padding = (PADDING_DP * activity.resources.displayMetrics.density).toInt()
    private val column =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding * TOP_PADDING_FACTOR, padding, padding)
        }
    val status = TextView(activity).apply { textSize = TEXT_SP }

    init {
        column.addView(status)
        activity.setContentView(
            ScrollView(activity).apply {
                addView(
                    column,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
        )
    }

    fun button(activity: Activity, label: Int, onClick: () -> Unit) {
        column.addView(
            Button(activity).apply {
                setText(label)
                setOnClickListener { onClick() }
            }
        )
    }

    private companion object {
        const val PADDING_DP = 16
        const val TOP_PADDING_FACTOR = 3
        const val TEXT_SP = 18f
    }
}
