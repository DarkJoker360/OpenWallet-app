/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.di

import android.content.Context
import com.esposito.openwallet.R
import com.esposito.openwallet.core.data.local.database.SecureWalletDatabase
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.parser.GoogleWalletHandler
import com.esposito.openwallet.core.domain.parser.PKPassHandler
import com.esposito.openwallet.core.domain.parser.PassManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Simple dependency container
 */
object AppContainer {
    
    private var _database: SecureWalletDatabase? = null
    private var _repository: WalletRepository? = null
    private var _gson: Gson? = null
    private var _passManager: PassManager? = null
    
    fun getDatabase(context: Context): SecureWalletDatabase {
        return _database ?: synchronized(this) {
            _database ?: SecureWalletDatabase.getDatabase(context).also { _database = it }
        }
    }
    
    fun getGson(): Gson {
        return _gson ?: synchronized(this) {
            _gson ?: GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create().also { _gson = it }
        }
    }
    
    /**
     * Get the PassManager with all supported pass handlers registered
     */
    fun getPassManager(context: Context): PassManager {
        return _passManager ?: PassManager().apply {
            val gson = getGson()
            
            // Register PKPass handler first (for priority)
            registerHandler(PKPassHandler(gson))
            
            // Register additional format handlers
            registerHandler(GoogleWalletHandler(gson, context))
            
            // Add more handlers here as needed...
            // registerHandler(MyCustomHandler(gson))
            
        }.also { _passManager = it }
    }
    
    fun getRepository(context: Context): WalletRepository {
        return _repository ?: synchronized(this) {
            _repository ?: WalletRepository(
                walletPassDao = getDatabase(context).walletPassDao(),
                creditCardDao = getDatabase(context).creditCardDao(),
                passManager = getPassManager(context),
                notificationScheduler = com.esposito.openwallet.core.notification.PassNotificationScheduler(context)
            ).also { _repository = it }
        }
    }
}
