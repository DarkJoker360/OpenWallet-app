/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.data.local.database

import androidx.room.migration.Migration

/**
 * Database migration utility for OpenWallet
 *
 * Current database version: 2
 */
object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            listOf("wallet_passes", "credit_cards", "crypto_wallets").forEach { table ->
                addColumnIfMissing(database, table, "isFavorite", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, table, "isArchived", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, table, "tags", "TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private fun addColumnIfMissing(
            database: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String,
            column: String,
            definition: String
        ) {
            val exists = database.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                generateSequence {
                    if (cursor.moveToNext()) cursor.getString(nameIndex) else null
                }.any { it == column }
            }
            if (!exists) {
                database.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
            }
        }
    }
    
    /**
     * Add new migrations here as needed.
     * Each migration should handle a specific version upgrade.
     */
    // Example: Migration from version 1 to 2
    // val MIGRATION_1_2 = object : Migration(1, 2) {
    //     override fun migrate(database: SupportSQLiteDatabase) {
    //         
    //         // === ADD NEW TABLE ===
    //         database.execSQL("""
    //             CREATE TABLE IF NOT EXISTS new_table (
    //                 id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    //                 name TEXT NOT NULL,
    //                 created_at INTEGER NOT NULL,
    //                 is_active INTEGER NOT NULL DEFAULT 1
    //             )
    //         """.trimIndent())
    //         
    //         // === ADD INDEX ===
    //         database.execSQL("CREATE INDEX IF NOT EXISTS idx_new_table_name ON new_table(name)")
    //         database.execSQL("CREATE INDEX IF NOT EXISTS idx_new_table_active ON new_table(is_active)")
    //     }
    // }

    /**
     * Get all migrations for the database.
     * Add your migrations to this array when you create them.
     * 
     * IMPORTANT: Always add migrations in order!
     * Example: [MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4]
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
        )
    }
}
