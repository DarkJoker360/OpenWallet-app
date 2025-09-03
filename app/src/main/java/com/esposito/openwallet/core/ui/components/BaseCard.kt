/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Base card component, consolidates common card styling
 * across credit cards, crypto wallets, and passes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    backgroundBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6B7280),
            Color(0xFF9CA3AF)
        )
    ),
    overlayBrush: Brush? = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.2f)
        )
    ),
    elevation: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = backgroundBrush,
                        shape = RoundedCornerShape(cornerRadius)
                    )
            )

            // Overlay if provided
            overlayBrush?.let { overlay ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = overlay,
                            shape = RoundedCornerShape(cornerRadius)
                        )
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

/**
 * Standard card layout with header and footer sections
 */
@Composable
fun StandardCardLayout(
    header: @Composable RowScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            header()
        }

        Column {
            footer()
        }
    }
}