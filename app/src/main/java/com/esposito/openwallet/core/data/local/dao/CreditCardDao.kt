/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.data.local.dao

import androidx.room.*
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CreditCardType
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Credit Card operations
 */
@Dao
interface CreditCardDao {
    
    @Query("SELECT * FROM credit_cards ORDER BY isPrimary DESC, cardHolderName ASC")
    fun getAllCreditCards(): Flow<List<CreditCard>>
    
    @Query("SELECT * FROM credit_cards ORDER BY isPrimary DESC, cardHolderName ASC")
    suspend fun getAllCreditCardsSync(): List<CreditCard>
    
    @Query("SELECT * FROM credit_cards WHERE isActive = 1 ORDER BY isPrimary DESC, cardHolderName ASC")
    fun getActiveCreditCards(): Flow<List<CreditCard>>
    
    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCreditCardById(id: String): CreditCard?
    
    @Query("SELECT * FROM credit_cards WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryCard(): CreditCard?
    
    @Query("SELECT * FROM credit_cards WHERE cardType = :cardType")
    fun getCreditCardsByType(cardType: CreditCardType): Flow<List<CreditCard>>
    
    @Query("SELECT COUNT(*) FROM credit_cards")
    suspend fun getCreditCardCount(): Int
    
    @Query("SELECT COUNT(*) FROM credit_cards WHERE isActive = 1")
    suspend fun getActiveCreditCardCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(creditCard: CreditCard): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCards(creditCards: List<CreditCard>)
    
    @Update
    suspend fun updateCreditCard(creditCard: CreditCard)
    
    @Delete
    suspend fun deleteCreditCard(creditCard: CreditCard)
    
    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCreditCardById(id: String)
    
    @Query("UPDATE credit_cards SET isActive = 0 WHERE id = :id")
    suspend fun deactivateCreditCard(id: String)
    
    @Query("UPDATE credit_cards SET isPrimary = 0")
    suspend fun clearAllPrimaryFlags()
    
    @Query("UPDATE credit_cards SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimaryCard(id: String)
    
    @Query("DELETE FROM credit_cards")
    suspend fun deleteAllCreditCards()
}
