package com.example.buylog

import android.app.Application
import com.example.buylog.data.BuyLogDatabase
import com.example.buylog.data.PaymentRepository

class BuyLogApplication : Application() {
    val repository: PaymentRepository by lazy {
        val database = BuyLogDatabase.getInstance(this)
        PaymentRepository(database.paymentDao(), database.paymentMethodDao())
    }
}
