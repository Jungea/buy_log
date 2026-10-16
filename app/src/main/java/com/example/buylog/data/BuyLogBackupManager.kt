package com.example.buylog.data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class BuyLogBackupManager(
    private val contentResolver: ContentResolver,
    private val database: BuyLogDatabase,
) {
    suspend fun exportTo(uri: Uri) {
        val backupDao = database.backupDao()
        val snapshot = database.withTransaction {
            BuyLogBackupSnapshot(
                paymentMethods = backupDao.getPaymentMethods(),
                payments = backupDao.getPayments(),
                notificationEvents = backupDao.getNotificationEvents(),
                tags = backupDao.getTags(),
                paymentTags = backupDao.getPaymentTags(),
                autoTagRules = backupDao.getAutoTagRules(),
                missedPaymentChecks = backupDao.getMissedPaymentChecks(),
            )
        }
        val content = BuyLogBackupCodec.encode(snapshot)
        require(content.length <= MAX_BACKUP_CHARACTERS) { "백업 데이터가 허용 크기를 초과했습니다." }
        val stream = requireNotNull(contentResolver.openOutputStream(uri, "wt")) { "백업 파일을 열 수 없습니다." }
        OutputStreamWriter(stream, StandardCharsets.UTF_8).use { it.write(content) }
    }

    suspend fun restoreFrom(uri: Uri) {
        val snapshot = BuyLogBackupCodec.decode(readLimitedText(uri))
        val backupDao = database.backupDao()
        database.withTransaction {
            backupDao.deletePaymentTags()
            backupDao.deleteAutoTagRules()
            backupDao.deleteMissedPaymentChecks()
            backupDao.deleteNotificationEvents()
            backupDao.deletePayments()
            backupDao.deleteTags()
            backupDao.deletePaymentMethods()

            if (snapshot.paymentMethods.isNotEmpty()) backupDao.insertPaymentMethods(snapshot.paymentMethods)
            if (snapshot.tags.isNotEmpty()) backupDao.insertTags(snapshot.tags)
            if (snapshot.payments.isNotEmpty()) backupDao.insertPayments(snapshot.payments)
            if (snapshot.paymentTags.isNotEmpty()) backupDao.insertPaymentTags(snapshot.paymentTags)
            if (snapshot.autoTagRules.isNotEmpty()) backupDao.insertAutoTagRules(snapshot.autoTagRules)
            if (snapshot.notificationEvents.isNotEmpty()) backupDao.insertNotificationEvents(snapshot.notificationEvents)
            if (snapshot.missedPaymentChecks.isNotEmpty()) backupDao.insertMissedPaymentChecks(snapshot.missedPaymentChecks)
        }
    }

    private fun readLimitedText(uri: Uri): String {
        val stream = requireNotNull(contentResolver.openInputStream(uri)) { "백업 파일을 열 수 없습니다." }
        return InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(8_192)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                require(result.length + count <= MAX_BACKUP_CHARACTERS) { "백업 파일이 허용 크기를 초과했습니다." }
                result.append(buffer, 0, count)
            }
            result.toString()
        }
    }

    private companion object {
        const val MAX_BACKUP_CHARACTERS = 50 * 1024 * 1024
    }
}
