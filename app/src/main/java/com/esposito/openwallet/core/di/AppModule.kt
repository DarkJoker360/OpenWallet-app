/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.di

import android.content.Context
import com.esposito.openwallet.core.data.local.dao.CreditCardDao
import com.esposito.openwallet.core.data.local.database.SecureWalletDatabase
import com.esposito.openwallet.core.data.local.database.WalletPassDao
import com.esposito.openwallet.core.data.local.manager.AppPreferencesManager
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.parser.GoogleWalletHandler
import com.esposito.openwallet.core.domain.parser.PKPassHandler
import com.esposito.openwallet.core.domain.parser.PassManager
import com.esposito.openwallet.core.domain.repository.CreditCardRepository
import com.esposito.openwallet.core.domain.repository.CryptoWalletRepository
import com.esposito.openwallet.core.domain.repository.WalletPassRepository
import com.esposito.openwallet.core.security.EnhancedCreditCardEncryption
import com.esposito.openwallet.core.ui.navigation.MainNavigationHandler
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt modules providing application-level dependencies
 */

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSecureWalletDatabase(
        @ApplicationContext context: Context
    ): SecureWalletDatabase = SecureWalletDatabase.getDatabase(context)

    @Provides
    fun provideWalletPassDao(database: SecureWalletDatabase): WalletPassDao = 
        database.walletPassDao()

    @Provides
    fun provideCreditCardDao(database: SecureWalletDatabase): CreditCardDao = 
        database.creditCardDao()
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideAppPreferencesManager(
        @ApplicationContext context: Context
    ): AppPreferencesManager = AppPreferencesManager(context)
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        .create()
}

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun providePKPassHandler(gson: Gson): PKPassHandler = PKPassHandler(gson)

    @Provides
    @Singleton
    fun providePassManager(gson: Gson, @ApplicationContext context: Context): PassManager = PassManager().apply {
        // Register all pass handlers
        registerHandler(PKPassHandler(gson))
        registerHandler(GoogleWalletHandler(gson, context))
        // Add more handlers here as needed
    }
}

@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {

    @Provides
    @Singleton
    fun provideMainNavigationHandler(): MainNavigationHandler = MainNavigationHandler()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideWalletRepository(
        walletPassDao: WalletPassDao,
        creditCardDao: CreditCardDao,
        passManager: PassManager,
        scheduler: com.esposito.openwallet.core.notification.PassNotificationScheduler
    ): WalletRepository = WalletRepository(
        walletPassDao = walletPassDao,
        creditCardDao = creditCardDao,
        passManager = passManager,
        notificationScheduler = scheduler
    )

    @Provides
    @Singleton
    fun provideWalletPassRepository(impl: WalletRepository): WalletPassRepository = impl

    @Provides
    @Singleton
    fun provideCreditCardRepository(impl: WalletRepository): CreditCardRepository = impl

    @Provides
    @Singleton
    fun provideCryptoWalletRepository(impl: WalletRepository): CryptoWalletRepository = impl
}

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideEnhancedCreditCardEncryption(): EnhancedCreditCardEncryption {
        return EnhancedCreditCardEncryption
    }
}
