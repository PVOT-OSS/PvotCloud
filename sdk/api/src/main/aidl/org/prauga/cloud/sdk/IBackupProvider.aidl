// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk;

import android.os.Bundle;

interface IBackupProvider {
    int contractVersion() = 0;
    Bundle describe() = 1;
}
