/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WalletPrimaryDark,
    onPrimary = WalletOnPrimaryDark,
    primaryContainer = WalletPrimaryContainerDark,
    onPrimaryContainer = WalletOnPrimaryContainerDark,
    secondary = WalletSecondaryDark,
    onSecondary = WalletOnSecondaryDark,
    secondaryContainer = WalletSecondaryContainerDark,
    onSecondaryContainer = WalletOnSecondaryContainerDark,
    tertiary = WalletTertiaryDark,
    onTertiary = WalletOnTertiaryDark,
    tertiaryContainer = WalletTertiaryContainerDark,
    onTertiaryContainer = WalletOnTertiaryContainerDark,
    error = WalletErrorDark,
    errorContainer = WalletErrorContainerDark,
    onError = WalletOnErrorDark,
    onErrorContainer = WalletOnErrorContainerDark,
    background = WalletBackgroundDark,
    onBackground = WalletOnBackgroundDark,
    surface = WalletSurfaceDark,
    onSurface = WalletOnSurfaceDark,
    surfaceVariant = WalletSurfaceVariantDark,
    onSurfaceVariant = WalletOnSurfaceVariantDark,
    outline = WalletOutlineDark,
    inverseOnSurface = WalletInverseOnSurfaceDark,
    inverseSurface = WalletInverseSurfaceDark,
    inversePrimary = WalletInversePrimaryDark,
)

private val LightColorScheme = lightColorScheme(
    primary = WalletPrimary,
    onPrimary = WalletOnPrimary,
    primaryContainer = WalletPrimaryContainer,
    onPrimaryContainer = WalletOnPrimaryContainer,
    secondary = WalletSecondary,
    onSecondary = WalletOnSecondary,
    secondaryContainer = WalletSecondaryContainer,
    onSecondaryContainer = WalletOnSecondaryContainer,
    tertiary = WalletTertiary,
    onTertiary = WalletOnTertiary,
    tertiaryContainer = WalletTertiaryContainer,
    onTertiaryContainer = WalletOnTertiaryContainer,
    error = WalletError,
    errorContainer = WalletErrorContainer,
    onError = WalletOnError,
    onErrorContainer = WalletOnErrorContainer,
    background = WalletBackground,
    onBackground = WalletOnBackground,
    surface = WalletSurface,
    onSurface = WalletOnSurface,
    surfaceVariant = WalletSurfaceVariant,
    onSurfaceVariant = WalletOnSurfaceVariant,
    outline = WalletOutline,
    inverseOnSurface = WalletInverseOnSurface,
    inverseSurface = WalletInverseSurface,
    inversePrimary = WalletInversePrimary,
)

@Composable
fun OpenWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Set status bar color using the new approach
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}