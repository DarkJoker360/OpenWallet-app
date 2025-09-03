/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.data.local.database

import androidx.room.TypeConverter
import com.esposito.openwallet.core.domain.model.BarcodeFormat
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.CreditCardType
import java.util.Date

class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromPassType(value: PassType): String {
        return value.name
    }

    @TypeConverter
    fun toPassType(value: String): PassType {
        return PassType.valueOf(value)
    }

    @TypeConverter
    fun fromBarcodeFormat(value: BarcodeFormat?): String? {
        return value?.name
    }

    @TypeConverter
    fun toBarcodeFormat(value: String?): BarcodeFormat? {
        return value?.let { BarcodeFormat.valueOf(it) }
    }
    
    @TypeConverter
    fun fromCreditCardType(value: CreditCardType): String {
        return value.name
    }

    @TypeConverter
    fun toCreditCardType(value: String): CreditCardType {
        return CreditCardType.valueOf(value)
    }
}
