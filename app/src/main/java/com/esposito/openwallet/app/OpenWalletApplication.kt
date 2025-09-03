/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.esposito.openwallet.feature.authentication.domain.AuthenticationSessionManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OpenWalletApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var activityCount = 0
    private var isAppInForeground = false

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        AuthenticationSessionManager.clearSession()
    }
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }
    
    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (!isAppInForeground) {
            isAppInForeground = true
        }
    }
    
    override fun onActivityResumed(activity: Activity) { }
    
    override fun onActivityPaused(activity: Activity) { }
    
    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            isAppInForeground = false
            AuthenticationSessionManager.clearSessionOnBackground()
        }
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }

    override fun onActivityDestroyed(activity: Activity) { }
}