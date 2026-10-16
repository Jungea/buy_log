package com.example.buylog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PaymentEntity::class, PaymentMethodEntity::class, NotificationEventEntity::class, TagEntity::class, PaymentTagCrossRef::class, AutoTagRuleEntity::class, MissedPaymentCheckEntity::class],
    version = 9,
    exportSchema = true,
)
abstract class BuyLogDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun tagDao(): TagDao
    abstract fun autoTagRuleDao(): AutoTagRuleDao
    abstract fun missedPaymentCheckDao(): MissedPaymentCheckDao

    companion object {
        @Volatile
        private var instance: BuyLogDatabase? = null

        fun getInstance(context: Context): BuyLogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BuyLogDatabase::class.java,
                    "buy-log.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `parseStatus` TEXT NOT NULL DEFAULT 'UNPROCESSED'")
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `parsedAmount` INTEGER")
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `parsedMerchant` TEXT")
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `parsedPaidAt` INTEGER")
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `parsedPaymentMethod` TEXT")
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `parseMessage` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `registrationStatus` TEXT NOT NULL DEFAULT 'NOT_ELIGIBLE'")
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `duplicateConfidence` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `paymentId` INTEGER")
                db.execSQL("ALTER TABLE `notification_events` ADD COLUMN `suggestedPaymentId` INTEGER")
                db.execSQL("UPDATE `notification_events` SET `registrationStatus` = 'PENDING' WHERE `parseStatus` = 'CANDIDATE'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `excludeFromSpending` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `payment_tags` (`paymentId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`paymentId`, `tagId`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_tags_tagId` ON `payment_tags` (`tagId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `payment_methods` ADD COLUMN `isCard` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `payment_methods` ADD COLUMN `cardCompany` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `payment_methods` ADD COLUMN `periodStartDay` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `payment_methods` ADD COLUMN `performanceTarget` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `payments` ADD COLUMN `performanceIncluded` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `auto_tag_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `phrase` TEXT NOT NULL, `tagId` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_auto_tag_rules_phrase_tagId` ON `auto_tag_rules` (`phrase`, `tagId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_auto_tag_rules_tagId` ON `auto_tag_rules` (`tagId`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `missed_payment_checks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `startHour` INTEGER NOT NULL, `endHour` INTEGER NOT NULL, `probabilityPercent` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `respondedAt` INTEGER)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_missed_payment_checks_dateEpochDay` ON `missed_payment_checks` (`dateEpochDay`)")
            }
        }
    }
}
