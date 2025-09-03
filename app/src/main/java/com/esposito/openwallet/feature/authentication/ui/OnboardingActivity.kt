/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.authentication.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.esposito.openwallet.R
import com.esposito.openwallet.app.MainActivity
import com.esposito.openwallet.core.data.local.manager.AppPreferencesManager
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import kotlinx.coroutines.launch

/**
 * Onboarding Activity that introduces users to OpenWallet features
 */
class OnboardingActivity : ComponentActivity() {
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }
    
    private lateinit var appPrefs: AppPreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        appPrefs = AppPreferencesManager(this)
        
        setContent {
            OpenWalletTheme {
                OnboardingScreen(
                    onOnboardingComplete = {
                        completeOnboarding()
                    },
                    onSkip = {
                        completeOnboarding()
                    }
                )
            }
        }
    }
    
    private fun completeOnboarding() {
        lifecycleScope.launch {
            appPrefs.isOnboardingCompleted = true
            startActivity(MainActivity.createIntent(this@OnboardingActivity))
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val onboardingPages = listOf(
        OnboardingPageData(
            title = stringResource(R.string.welcome_to_openwallet),
            description = stringResource(R.string.onboarding_welcome_description),
            icon = Icons.Default.AccountBalanceWallet,
            features = listOf(
                stringResource(R.string.onboarding_security_feature_1),
                stringResource(R.string.onboarding_security_feature_2),
                stringResource(R.string.onboarding_security_feature_3)
            )
        ),
        OnboardingPageData(
            title = stringResource(R.string.manage_your_cards),
            description = stringResource(R.string.onboarding_cards_description),
            icon = Icons.Default.CreditCard,
            features = listOf(
                stringResource(R.string.onboarding_cards_feature_1),
                stringResource(R.string.onboarding_cards_feature_2),
                stringResource(R.string.onboarding_cards_feature_3)
            )
        ),
        OnboardingPageData(
            title = stringResource(R.string.crypto_digital_assets),
            description = stringResource(R.string.onboarding_crypto_description),
            icon = Icons.Default.CurrencyBitcoin,
            features = listOf(
                stringResource(R.string.onboarding_crypto_feature_1),
                stringResource(R.string.onboarding_crypto_feature_2),
                stringResource(R.string.onboarding_crypto_feature_3)
            )
        ),
        OnboardingPageData(
            title = stringResource(R.string.enhanced_security),
            description = stringResource(R.string.onboarding_enhanced_security_description),
            icon = Icons.Default.Security,
            features = listOf(
                stringResource(R.string.onboarding_enhanced_security_feature_1),
                stringResource(R.string.onboarding_enhanced_security_feature_2),
                stringResource(R.string.onboarding_enhanced_security_feature_3)
            )
        ),
        OnboardingPageData(
            title = stringResource(R.string.you_are_all_set),
            description = stringResource(R.string.onboarding_complete_description),
            icon = Icons.Default.CheckCircle,
            features = emptyList()
        )
    )
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 16.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (!isLastPage) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .height(44.dp)
                        .widthIn(min = 72.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.skip),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPage(
                page = onboardingPages[page],
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Bottom Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicators
            OnboardingIndicators(
                pageCount = onboardingPages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Next/Get Started Button
            Button(
                onClick = {
                    if (isLastPage) {
                        onOnboardingComplete()
                    } else {
                        // Navigate to next page
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (isLastPage) stringResource(R.string.get_started) else stringResource(R.string.next),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun OnboardingPage(
    page: OnboardingPageData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon/Image
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            lineHeight = 24.sp
        )
        
        // Features list (if available)
        if (page.features.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                page.features.forEach { feature ->
                    OnboardingFeatureItem(feature = feature)
                }
            }
        }
    }
}

@Composable
fun OnboardingFeatureItem(
    feature: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun OnboardingIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .size(
                        width = if (isSelected) 24.dp else 8.dp,
                        height = 8.dp
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        }
                    )
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearEasing
                        )
                    )
            )
        }
    }
}

/**
 * Data class for onboarding page content
 */
data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val features: List<String> = emptyList()
)
