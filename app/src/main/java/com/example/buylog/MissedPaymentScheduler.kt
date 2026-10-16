package com.example.buylog

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MissedPaymentScheduler {
    private const val PERIODIC_WORK = "missed-payment-periodic-check"
    private const val IMMEDIATE_WORK = "missed-payment-immediate-check"

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MissedPaymentNotificationWorker>(1, TimeUnit.HOURS).build(),
        )
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MissedPaymentNotificationWorker>().build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).run {
            cancelUniqueWork(PERIODIC_WORK)
            cancelUniqueWork(IMMEDIATE_WORK)
        }
    }
}
