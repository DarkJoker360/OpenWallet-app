/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.esposito.openwallet.core.domain.model.WalletPass
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext

class PassNotificationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val NOTIFICATION_LEAD_TIME_HOURS = 2L
    }

    fun schedulePassNotification(pass: WalletPass) {
        val relevantDate = pass.relevantDate ?: return
        val now = System.currentTimeMillis()
        val eventTime = relevantDate.time
        val leadTimeMillis = TimeUnit.HOURS.toMillis(NOTIFICATION_LEAD_TIME_HOURS)
        val triggerTime = eventTime - leadTimeMillis
        val delay = triggerTime - now

        if (eventTime < now) {
            cancelPassNotification(pass.id)
            return
        }

        val finalDelay = if (delay < 0) 0L else delay

        val data = Data.Builder()
            .putString(PassNotificationWorker.INPUT_PASS_ID, pass.id)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PassNotificationWorker>()
            .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getWorkName(pass.id),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelPassNotification(passId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(getWorkName(passId))
    }

    private fun getWorkName(passId: String) = "pass_notification_$passId"
}
