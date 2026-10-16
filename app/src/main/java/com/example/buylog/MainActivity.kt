package com.example.buylog

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.buylog.ui.BuyLogApp
import com.example.buylog.ui.theme.BuyLogTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var addPaymentRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            BuyLogTheme {
                BuyLogApp(viewModel, addPaymentRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != MissedPaymentNotificationWorker.ACTION_ADD_PAYMENT) return
        addPaymentRequest++
        getSystemService(NotificationManager::class.java).cancel(
            intent.getIntExtra(MissedPaymentNotificationWorker.EXTRA_NOTIFICATION_ID, 0),
        )
    }
}
