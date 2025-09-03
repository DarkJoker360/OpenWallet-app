/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import java.util.Date

/**
 * Simplified Pass model for UI state
 * This represents a unified interface for all types of passes
 */
data class Pass(
    val id: String,
    val organizationName: String,
    val description: String,
    val serialNumber: String,
    val passType: String,
    val barcodeMessage: String? = null,
    val barcodeFormat: String? = null,
    val barcodeAltText: String? = null,
    val headerFields: List<PassField> = emptyList(),
    val primaryFields: List<PassField> = emptyList(),
    val secondaryFields: List<PassField> = emptyList(),
    val auxiliaryFields: List<PassField> = emptyList(),
    val backFields: List<PassField> = emptyList(),
    val locations: List<Location> = emptyList(),
    val relevantDate: Date? = null,
    val expirationDate: Date? = null,
    val backgroundColor: String? = null,
    val foregroundColor: String? = null,
    val labelColor: String? = null,
    val logoText: String? = null,
    val suppressStripShine: Boolean = false,
    val webServiceURL: String? = null,
    val authenticationToken: String? = null,
    val isVoided: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val type: String // String representation of PassType
)

/**
 * Location data for pass relevance
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val relevantText: String? = null
)
