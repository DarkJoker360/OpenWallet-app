/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.util

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.toColorInt
import com.esposito.openwallet.core.domain.model.PassCategory
import com.esposito.openwallet.core.domain.model.PassType

/**
 * Utility functions for working with PassType enum
 */
object PassTypeUtils {
    
    /**
     * Get the display color for a pass type
     */
    fun getPassTypeColor(passType: String): Color {
        val type = PassType.valueOf(passType)
        return Color(type.category.color.toColorInt())
    }
    
    /**
     * Get the display icon for a pass type
     */
    fun getPassTypeIcon(passType: String): ImageVector {
        return when (PassType.valueOf(passType).category) {
            PassCategory.PAYMENT -> Icons.Default.CreditCard
            PassCategory.RETAIL -> Icons.Default.LocalGroceryStore
            PassCategory.TRAVEL -> Icons.Default.Flight
            PassCategory.ENTERTAINMENT -> Icons.Default.Event
            PassCategory.HEALTH -> Icons.Default.MedicalServices
            PassCategory.ACCESS -> Icons.Default.VpnKey
            PassCategory.GOVERNMENT -> Icons.Default.AccountBox
            PassCategory.GENERIC -> Icons.Default.Description
        }
    }
    
    /**
     * Get the localized display name for a pass type
     */
    fun getPassTypeDisplayName(context: Context, passType: PassType): String {
        return context.getString(passType.displayName)
    }

    /**
     * Get the localized display name for a pass category
     */
    fun getPassCategoryDisplayName(context: Context, category: PassCategory): String {
        return context.getString(category.displayName)
    }
}