/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.parser

import com.esposito.openwallet.core.domain.model.WalletPass
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * Central manager for handling multiple pass formats.
 * This class routes pass files to the appropriate handler based on format detection.
 * 
 * It maintains a registry of pass handlers and tries each one until it finds
 * a compatible handler for the given file format.
 */
class PassManager {
    
    private val handlers = mutableListOf<PassHandler>()
    
    /**
     * Registers a new pass handler
     */
    fun registerHandler(handler: PassHandler) {
        // Insert PKPass handler first to maintain compatibility
        if (handler is PKPassHandler) {
            handlers.add(0, handler)
        } else {
            handlers.add(handler)
        }
    }
    
    /**
     * Finds a compatible handler for the given file
     */
    fun findHandler(fileName: String?, mimeType: String?, inputStream: InputStream): PassHandler? {
        // Use BufferedInputStream to allow multiple reads
        val bufferedStream = inputStream as? BufferedInputStream ?: BufferedInputStream(inputStream)
        
        // Use larger mark buffer to handle bigger files (1MB)
        bufferedStream.mark(1024 * 1024)
        
        try {
            for (handler in handlers) {
                bufferedStream.reset() // Reset stream for each handler
                bufferedStream.mark(1024 * 1024) // Re-mark after reset with larger buffer
                
                if (handler.canHandle(fileName, mimeType, bufferedStream)) {
                    return handler
                }
            }
        } catch (_: Exception) {
            // If reset fails, fall back to extension-based detection
            return findHandlerByExtension(fileName)
        }
        
        return null
    }
    
    /**
     * Fallback method to find handler by file extension only
     */
    private fun findHandlerByExtension(fileName: String?): PassHandler? {
        if (fileName == null) return null
        
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return handlers.find { handler ->
            handler.supportedExtensions.any { it.equals(extension, ignoreCase = true) }
        }
    }
    
    /**
     * Parses a pass file using the appropriate handler
     * 
     * @param inputStream The file content to parse
     * @param fileName Original filename (optional)
     * @param mimeType MIME type (optional)
     * @param metadata Additional metadata (optional)
     * @return ParseResult containing the result or error information
     */
    fun parsePass(
        inputStream: InputStream,
        fileName: String? = null,
        mimeType: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): ParseResult {
        val bufferedStream = inputStream as? BufferedInputStream ?: BufferedInputStream(inputStream)
        
        try {
            // Use larger mark buffer for bigger files (1MB)
            bufferedStream.mark(1024 * 1024)
            
            val handler = findHandler(fileName, mimeType, bufferedStream)
                ?: return ParseResult.failure(
                    "No compatible handler found for file: $fileName",
                    "UNSUPPORTED_FORMAT"
                )
            
            bufferedStream.reset()
            
            val pass = handler.parsePass(bufferedStream, fileName, metadata)
                ?: return ParseResult.failure(
                    "Handler ${handler.formatName} failed to parse file: $fileName",
                    "PARSING_FAILED"
                )
            
            val validation = handler.validatePass(pass)
            
            return if (validation.isValid) {
                ParseResult.success(pass, handler.formatName, validation.warnings)
            } else {
                ParseResult.failure(
                    "Pass validation failed: ${validation.errors.joinToString(", ")}",
                    "VALIDATION_FAILED",
                    handler.formatName
                )
            }
            
        } catch (e: PassParsingException) {
            return ParseResult.failure(
                e.message ?: "Unknown parsing error",
                "PARSING_EXCEPTION",
                e.formatName
            )
        } catch (e: Exception) {
            return ParseResult.failure(
                "Unexpected error: ${e.message}",
                "UNEXPECTED_ERROR"
            )
        }
    }
}

/**
 * Result of pass parsing operation
 */
sealed class ParseResult {
    data class Success(
        val pass: WalletPass,
        val formatName: String,
        val warnings: List<String> = emptyList()
    ) : ParseResult()
    
    data class Failure(
        val error: String,
        val errorCode: String,
        val formatName: String? = null
    ) : ParseResult()
    
    companion object {
        fun success(pass: WalletPass, formatName: String, warnings: List<String> = emptyList()) =
            Success(pass, formatName, warnings)
        
        fun failure(error: String, errorCode: String, formatName: String? = null) =
            Failure(error, errorCode, formatName)
    }
}
