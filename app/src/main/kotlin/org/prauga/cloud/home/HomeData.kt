// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.home

import org.prauga.cloud.core.model.BackupOverview
import org.prauga.cloud.core.model.RunProgress

data class HomeData(val overview: BackupOverview, val running: RunProgress?)
