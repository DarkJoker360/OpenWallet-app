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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTags(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toTags(value: String): List<String> = runCatching {
        gson.fromJson<List<String>>(value, object : TypeToken<List<String>>() {}.type)
    }.getOrDefault(emptyList())
    
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
