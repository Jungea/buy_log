package com.example.buylog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PaymentEntity::class, PaymentMethodEntity::class, NotificationEventEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class BuyLogDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun notificationEventDao(): NotificationEventDao

    companion object {
        @Volatile
        private var instance: BuyLogDatabase? = null

        fun getInstance(context: Context): BuyLogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BuyLogDatabase::class.java,
                    "buy-log.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE `payments_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `merchant` TEXT NOT NULL,
                        `paidAt` INTEGER NOT NULL,
                        `paymentMethodId` INTEGER NOT NULL,
                        `memo` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `payments_new` (`id`, `amount`, `merchant`, `paidAt`, `paymentMethodId`, `memo`, `createdAt`, `updatedAt`)
                    SELECT `id`, `amount`, `merchant`, `paidAt`, `paymentMethodId`, `memo`, `createdAt`, `updatedAt`
                    FROM `payments`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `payments`")
                db.execSQL("ALTER TABLE `payments_new` RENAME TO `payments`")
                db.execSQL("CREATE INDEX `index_payments_paidAt` ON `payments` (`paidAt`)")
                db.execSQL("CREATE INDEX `index_payments_paymentMethodId` ON `payments` (`paymentMethodId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notification_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `rawText` TEXT NOT NULL,
                        `receivedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_events_receivedAt` ON `notification_events` (`receivedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_events_packageName` ON `notification_events` (`packageName`)")
            }
        }
    }
}
