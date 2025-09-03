/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.esposito.openwallet.R

@Composable
fun FabMenu(
    showFabMenu: Boolean,
    onToggleFabMenu: () -> Unit,
    onNavigateToAddPass: () -> Unit,
    onNavigateToAddCreditCard: () -> Unit,
    onNavigateToAddCryptoWallet: () -> Unit,
    onDismissFabMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (showFabMenu) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fab_menu_alpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Menu options
        FabMenuItem(
            text = stringResource(R.string.add_pass_card),
            icon = Icons.Default.Badge,
            onClick = {
                onNavigateToAddPass()
                onDismissFabMenu()
            },
            visible = showFabMenu,
            alpha = alpha,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        FabMenuItem(
            text = stringResource(R.string.add_credit_card),
            icon = Icons.Default.CreditCard,
            onClick = {
                onNavigateToAddCreditCard()
                onDismissFabMenu()
            },
            visible = showFabMenu,
            alpha = alpha,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )

        FabMenuItem(
            text = stringResource(R.string.add_crypto_wallet),
            icon = Icons.Default.AccountBalanceWallet,
            onClick = {
                onNavigateToAddCryptoWallet()
                onDismissFabMenu()
            },
            visible = showFabMenu,
            alpha = alpha,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        // Main FAB
        FloatingActionButton(
            onClick = onToggleFabMenu,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (showFabMenu) stringResource(R.string.close_menu) else stringResource(R.string.add_item)
            )
        }
    }
}

@Composable
private fun FabMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    visible: Boolean,
    alpha: Float,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    if (visible) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            text = { Text(text) },
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = text
                )
            },
            modifier = modifier
                .alpha(alpha)
                .padding(bottom = 0.dp),
            containerColor = containerColor,
            contentColor = contentColor
        )
    }
}
