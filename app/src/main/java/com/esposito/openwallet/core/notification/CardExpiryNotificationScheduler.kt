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
import com.esposito.openwallet.core.domain.model.CreditCard
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Schedules a one-off reminder before a stored card's expiry date so users can
 * renew or replace the card before it stops working.
 *
 * Mirrors [PassNotificationScheduler]: each card gets a unique WorkManager job
 * keyed by its id, and rescheduling simply replaces the existing job.
 */
class CardExpiryNotificationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val NOTIFICATION_LEAD_TIME_DAYS = 7L
        private const val MIN_MONTH = 1
        private const val MAX_MONTH = 12
    }

    fun scheduleCardExpiryNotification(card: CreditCard) {
        val expiryTime = computeExpiryEndMillis(card) ?: return
        val now = System.currentTimeMillis()

        // Card already expired – nothing useful to remind about.
        if (expiryTime < now) {
            cancelCardExpiryNotification(card.id)
            return
        }

        val leadTimeMillis = TimeUnit.DAYS.toMillis(NOTIFICATION_LEAD_TIME_DAYS)
        val triggerTime = expiryTime - leadTimeMillis
        val finalDelay = (triggerTime - now).coerceAtLeast(0L)

        val data = Data.Builder()
            .putString(CardExpiryNotificationWorker.INPUT_CARD_ID, card.id)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<CardExpiryNotificationWorker>()
            .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getWorkName(card.id),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelCardExpiryNotification(cardId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(getWorkName(cardId))
    }

    /**
     * A card is valid through the final moment of its expiry month. Returns null
     * when the stored month/year are out of range (e.g. a card with no expiry set).
     */
    private fun computeExpiryEndMillis(card: CreditCard): Long? {
        if (card.expiryMonth !in MIN_MONTH..MAX_MONTH || card.expiryYear <= 0) return null

        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, card.expiryYear)
            set(Calendar.MONTH, card.expiryMonth - 1) // Calendar months are 0-based
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
    }

    private fun getWorkName(cardId: String) = "card_expiry_notification_$cardId"
}
