/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.parser

import com.esposito.openwallet.core.domain.model.WalletPass
import java.io.InputStream

/**
 * Interface for handling different pass formats in a modular way.
 * This allows the app to support multiple pass types (PKPass, Google Wallet JSON, etc.)
 * without breaking existing functionality.
 *
 * Each pass format should implement this interface to provide:
 * - Format detection
 * - Parsing logic
 * - Validation
 * - Error handling
 */
interface PassHandler {
    
    /**
     * The unique identifier for this pass format
     */
    val formatName: String
    
    /**
     * File extensions this handler supports (e.g., "pkpass", "json", "bin")
     */
    val supportedExtensions: List<String>
    
    /**
     * MIME types this handler supports (optional)
     */
    val supportedMimeTypes: List<String>
        get() = emptyList()
    
    /**
     * Determines if this handler can process the given file based on
     * extension, MIME type, or file content analysis.
     *
     * @param fileName The name of the file (with extension)
     * @param mimeType The MIME type of the file (optional)
     * @param inputStream The file content stream for content-based detection
     * @return true if this handler can process the file
     */
    fun canHandle(fileName: String?, mimeType: String?, inputStream: InputStream): Boolean
    
    /**
     * Parses the input stream and converts it to a WalletPass object.
     * The input stream will be reset to the beginning before calling this method.
     *
     * @param inputStream The file content to parse
     * @param fileName Original filename (optional, for context)
     * @param metadata Additional metadata about the file (optional)
     * @return WalletPass object or null if parsing fails
     * @throws PassParsingException if parsing fails with detailed error information
     */
    @Throws(PassParsingException::class)
    fun parsePass(
        inputStream: InputStream,
        fileName: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): WalletPass?
    
    /**
     * Validates that the parsed pass data is complete and valid.
     * This is called after successful parsing.
     *
     * @param pass The parsed WalletPass to validate
     * @return ValidationResult with success/failure and details
     */
    fun validatePass(pass: WalletPass): ValidationResult
}

/**
 * Result of pass validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(errors: List<String>) = ValidationResult(false, errors)
        fun withWarnings(warnings: List<String>) = ValidationResult(true, warnings = warnings)
    }
}

/**
 * Exception thrown when pass parsing fails
 */
class PassParsingException(
    message: String,
    cause: Throwable? = null,
    val formatName: String? = null,
    val fileName: String? = null
) : Exception("$fileName: $message", cause)
