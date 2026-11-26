// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk;

oneway interface IResultCallback {
    void onProgress(long bytes) = 0;
    void onResult(int code, String reason) = 1;
}
