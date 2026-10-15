package com.example.buylog

import android.app.Application
import com.example.buylog.data.BuyLogDatabase
import com.example.buylog.data.PaymentRepository

class BuyLogApplication : Application() {
    val repository: PaymentRepository by lazy {
        val database = BuyLogDatabase.getInstance(this)
        PaymentRepository(
            paymentDao = database.paymentDao(),
            paymentMethodDao = database.paymentMethodDao(),
            notificationEventDao = database.notificationEventDao(),
        )
    }
}
