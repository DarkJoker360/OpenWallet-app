/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * Clipboard helper for sensitive values (card numbers, IBANs, wallet addresses).
 *
 * It flags the copied data as sensitive so the system suppresses it from
 * clipboard previews (Android 13+), and automatically clears it a short while
 * later so secrets don't linger on the clipboard indefinitely.
 */
object SecureClipboard {

    private const val AUTO_CLEAR_DELAY_MS = 60_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    fun copySensitive(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)

        // Mark as sensitive so it is hidden from clipboard previews (API 33+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboard.setPrimaryClip(clip)
        scheduleAutoClear(clipboard, text)
    }

    private fun scheduleAutoClear(clipboard: ClipboardManager, copiedText: String) {
        mainHandler.postDelayed({
            // Only clear if our value is still the current clip, so we never wipe
            // something the user copied afterwards.
            val current = clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.text
                ?.toString()
            if (current == copiedText) {
                clipboard.clearPrimaryClip()
            }
        }, AUTO_CLEAR_DELAY_MS)
    }
}
