/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.esposito.openwallet.feature.authentication.domain.AuthenticationSessionManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OpenWalletApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    AuthenticationSessionManager.clearSessionOnBackground()
                }
            }
        )
    }
}