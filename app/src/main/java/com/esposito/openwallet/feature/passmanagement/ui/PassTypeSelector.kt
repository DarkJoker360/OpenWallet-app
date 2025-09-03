/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.PassCategory
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.util.PassTypeUtils

@Composable
fun PassTypeSelector(
    selectedPassType: PassType,
    onPassTypeSelected: (PassType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_pass_type_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PassCategory.entries.forEach { category ->
                        val passTypesInCategory = PassType.entries.filter { it.category == category }
                        
                        item(key = "header_${category.name}") {
                            CategoryHeaderItem(
                                category = category,
                                context = context,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(
                            items = passTypesInCategory,
                            key = { it.name }
                        ) { passType ->
                            PassTypeItem(
                                passType = passType,
                                isSelected = passType == selectedPassType,
                                context = context,
                                onClick = {
                                    onPassTypeSelected(passType)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeaderItem(
    category: PassCategory,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(category.color.toColorInt()).copy(alpha = 0.1f)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        Color(category.color.toColorInt()).copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = Color(category.color.toColorInt()),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        Text(
            text = PassTypeUtils.getPassCategoryDisplayName(context, category),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PassTypeItem(
    passType: PassType,
    isSelected: Boolean,
    onClick: () -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = passType.icon,
                style = MaterialTheme.typography.titleMedium
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = PassTypeUtils.getPassTypeDisplayName(context, passType),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (passType.supportsBarcodes) {
                        FeatureChip(stringResource(R.string.feature_barcode))
                    }
                    if (passType.supportsNFC) {
                        FeatureChip(stringResource(R.string.feature_nfc))
                    }
                    if (passType.supportsLocationNotifications) {
                        FeatureChip(stringResource(R.string.feature_location))
                    }
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selection),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getCategoryIcon(category: PassCategory): ImageVector {
    return when (category) {
        PassCategory.PAYMENT -> Icons.Default.CreditCard
        PassCategory.RETAIL -> Icons.Default.ShoppingBag
        PassCategory.TRAVEL -> Icons.Default.Flight
        PassCategory.ENTERTAINMENT -> Icons.Default.LocalActivity
        PassCategory.HEALTH -> Icons.Default.LocalHospital
        PassCategory.ACCESS -> Icons.Default.Key
        PassCategory.GOVERNMENT -> Icons.Default.AccountBalance
        PassCategory.GENERIC -> Icons.Default.Description
    }
}
