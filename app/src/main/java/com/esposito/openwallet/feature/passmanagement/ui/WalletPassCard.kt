/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.ui.components.BaseCard
import com.esposito.openwallet.core.ui.components.CategoryChip
import com.esposito.openwallet.core.util.PassTypeUtils
import com.esposito.openwallet.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPassCard(
    pass: WalletPass,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Use PKPassCard for imported passes, regular card for manual passes
    if (pass.isImported) {
        PKPassCard(
            pass = pass,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    } else {
        ManualPassCard(
            pass = pass,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ManualPassCard(
    pass: WalletPass,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val passColor = PassTypeUtils.getPassTypeColor(pass.type.name)

    BaseCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        height = 184.dp,
        backgroundBrush = if (pass.imageData != null) {
            // Will be overridden by AsyncImage
            Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
        } else {
            Brush.linearGradient(
                colors = listOf(
                    passColor.copy(alpha = 0.96f),
                    passColor.copy(alpha = 0.72f)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(900f, 900f)
            )
        },
        overlayBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.58f)
            )
        ),
        elevation = 4.dp,
        cornerRadius = 20.dp,
        contentPadding = 0.dp
    ) {
        // Background image if available
        if (pass.imageData != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(pass.imageData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Content with proper padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!pass.organizationName.equals(pass.title, ignoreCase = true)) {
                            Text(
                                text = pass.organizationName,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    CategoryChip(
                        text = PassTypeUtils.getPassTypeDisplayName(context, pass.type),
                        icon = PassTypeUtils.getPassTypeIcon(pass.type.name),
                        backgroundColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                )

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = pass.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                pass.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            // Logo in top right if available
            if (pass.logoData != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(pass.logoData)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.logo),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(4.dp)
                )
            }
        }
    }
}
