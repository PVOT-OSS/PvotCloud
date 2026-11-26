// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import org.prauga.cloud.sdk.IResultCallback;

interface IBackupAgent {
    int contractVersion() = 0;
    Bundle describe() = 1;
    oneway void exportData(String type, in ParcelFileDescriptor sink, IResultCallback callback) = 2;
    oneway void importData(String type, int fromSchema, in ParcelFileDescriptor source, IResultCallback callback) = 3;
    oneway void restoreFinished() = 4;
}
