/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.qs

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.esposito.openwallet.R
import com.esposito.openwallet.feature.quickpass.ui.QuickPassActivity

/**
 * Quick Settings Tile Service for OpenWallet Passes
 * Provides quick access to tickets and passes
 */
class OpenWalletPassesQSTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val intent = Intent(this, QuickPassActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                   Intent.FLAG_ACTIVITY_CLEAR_TOP or
                   Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.quick_pass_title)
        tile.contentDescription = getString(R.string.quick_pass_tile_description)
        tile.subtitle = tile.contentDescription
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_ticket)
        tile.updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }
}
