package com.example.buylog

import android.app.Application
import com.example.buylog.data.BuyLogDatabase
import com.example.buylog.data.PaymentRepository
import com.example.buylog.data.BuyLogBackupManager

class BuyLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (MissedPaymentPreferences.isEnabled(this)) MissedPaymentScheduler.schedule(this)
    }

    val repository: PaymentRepository by lazy {
        val database = BuyLogDatabase.getInstance(this)
        PaymentRepository(
            paymentDao = database.paymentDao(),
            paymentMethodDao = database.paymentMethodDao(),
            notificationEventDao = database.notificationEventDao(),
            tagDao = database.tagDao(),
            autoTagRuleDao = database.autoTagRuleDao(),
        )
    }

    val backupManager: BuyLogBackupManager by lazy {
        BuyLogBackupManager(contentResolver, BuyLogDatabase.getInstance(this))
    }
}
