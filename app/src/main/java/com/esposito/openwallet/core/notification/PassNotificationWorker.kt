/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.esposito.openwallet.R
import com.esposito.openwallet.core.di.AppContainer
import com.esposito.openwallet.feature.passmanagement.ui.PassDetailActivity.Companion.createIntent

class PassNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val INPUT_PASS_ID = "pass_id"
        const val NOTIFICATION_CHANNEL_ID = "pass_reminders"
        const val NOTIFICATION_ID_BASE = 1000
    }

    override suspend fun doWork(): Result {
        val passId = inputData.getString(INPUT_PASS_ID) ?: return Result.failure()
        val repository = AppContainer.getRepository(applicationContext)
        val pass = repository.getPassById(passId) ?: return Result.failure()

        createNotificationChannel()

        val title = pass.title.ifBlank { applicationContext.getString(R.string.upcoming_pass) }
        val description = pass.organizationName

        val intent = createIntent(applicationContext, passId)

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(applicationContext).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                pass.hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val eventTime = pass.relevantDate?.time ?: System.currentTimeMillis()
        val expiryTime = eventTime + (60 * 60 * 1000)
        val timeout = expiryTime - System.currentTimeMillis()

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_ticket)
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .apply {
                if (timeout > 0) {
                    setTimeoutAfter(timeout)
                }
            }
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + pass.hashCode(), notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        val name = applicationContext.getString(R.string.notification_channel_pass_reminders)
        val descriptionText = applicationContext.getString(R.string.notification_channel_pass_reminders_desc)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
