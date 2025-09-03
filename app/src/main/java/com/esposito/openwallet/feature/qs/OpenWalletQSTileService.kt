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
import com.esposito.openwallet.app.MainActivity

/**
 * Quick Settings Tile Service for OpenWallet
 * Provides quick access to the app from the device's Quick Settings panel
 */
class OpenWalletQSTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val intent = Intent(this, MainActivity::class.java).apply {
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
            pendingIntent.send()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.app_name)
        tile.contentDescription = getString(R.string.qs_tile_description, getString(R.string.app_name))
        tile.subtitle = tile.contentDescription
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_wallet)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }
}