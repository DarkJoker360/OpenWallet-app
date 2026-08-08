/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.esposito.openwallet.R
import com.esposito.openwallet.core.di.AppContainer
import java.util.Locale
import java.util.zip.CRC32

/**
 * Posts a "card expiring soon" reminder for a stored credit/debit card.
 * Scheduled by [CardExpiryNotificationScheduler] ahead of the card's expiry.
 */
class CardExpiryNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val INPUT_CARD_ID = "card_id"
        const val NOTIFICATION_CHANNEL_ID = "card_expiry_reminders"
        const val NOTIFICATION_ID_BASE = 2000
    }

    override suspend fun doWork(): Result {
        val cardId = inputData.getString(INPUT_CARD_ID) ?: return Result.failure()
        val repository = AppContainer.getRepository(applicationContext)
        val card = repository.getCreditCard(cardId) ?: return Result.failure()

        createNotificationChannel()

        val cardLabel = card.cardNickname?.takeIf { it.isNotBlank() } ?: card.issuerBank
        val expiry = String.format(
            Locale.US,
            "%02d/%02d",
            card.expiryMonth,
            card.expiryYear % 100
        )

        val title = applicationContext.getString(R.string.card_expiry_notification_title)
        val text = applicationContext.getString(
            R.string.card_expiry_notification_text,
            cardLabel,
            card.maskedCardNumber,
            expiry
        )

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_wallet)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId(card.id), notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        val name = applicationContext.getString(R.string.notification_channel_card_expiry)
        val descriptionText = applicationContext.getString(R.string.notification_channel_card_expiry_desc)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = descriptionText }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun notificationId(cardId: String): Int {
        val checksum = CRC32().apply { update(cardId.toByteArray(Charsets.UTF_8)) }.value
        return NOTIFICATION_ID_BASE + (checksum % (Int.MAX_VALUE - NOTIFICATION_ID_BASE)).toInt()
    }
}
