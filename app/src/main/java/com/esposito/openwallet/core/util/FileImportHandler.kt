/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.esposito.openwallet.R
import com.esposito.openwallet.core.data.repository.ImportResult
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.di.IoDispatcher
import com.esposito.openwallet.core.di.MainDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.inject.Inject

class FileImportHandler @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {

        private val TAG = "FileImportHandler"
        private  val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        private  val MIN_FILE_SIZE = 10 // 10 bytes
        private val ALLOWED_EXTENSIONS = setOf("pkpass", "json", "gwpass", "walletpass")
        private val SUSPICIOUS_PATTERNS = listOf(
            "<script", "javascript:", "vbscript:", "data:text/html",
            "eval(", "Function(", "setTimeout(", "setInterval("
        )

    
    fun handleFileImport(
        context: Context,
        uri: Uri,
        repository: WalletRepository,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(ioDispatcher) {
            try {
                val result = processFile(context, uri, repository)
                withContext(mainDispatcher) {
                    showResult(context, result)
                }
            } catch (e: Exception) {
                withContext(mainDispatcher) {
                    showError(context, e)
                }
            }
        }
    }
    
    private suspend fun processFile(
        context: Context,
        uri: Uri,
        repository: WalletRepository
    ): ImportResult = withContext(ioDispatcher) {
        
        val (fileName, fileSize, fileBytes) = extractFileData(context, uri)
        
        validateFile(fileName, fileSize, fileBytes)
        
        SecureLogger.d(TAG, "Processing file import")
        
        if (fileName.endsWith(".pkpass", ignoreCase = true)) {
            importPKPass(repository, fileBytes)
        } else {
            importGenericFile(repository, fileBytes, fileName)
        }
    }
    
    private fun extractFileData(context: Context, uri: Uri): Triple<String, Long, ByteArray> {
        val fileName = getFileName(context, uri)
        val fileSize = getFileSize(context, uri)
        
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw SecurityException("Unable to access file")
        
        val fileBytes = inputStream.use { it.readBytes() }
        
        return Triple(fileName, fileSize, fileBytes)
    }
    
    private fun validateFile(fileName: String, fileSize: Long, fileBytes: ByteArray) {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        when {
            !ALLOWED_EXTENSIONS.contains(extension) -> 
                throw SecurityException("Invalid file type")
            fileSize !in MIN_FILE_SIZE..MAX_FILE_SIZE -> 
                throw SecurityException("File size validation failed")
            fileBytes.isEmpty() -> 
                throw SecurityException("Empty file not allowed")
            containsSuspiciousContent(fileBytes) -> 
                throw SecurityException("File contains suspicious content")
        }
    }
    
    private fun containsSuspiciousContent(bytes: ByteArray): Boolean {
        val content = String(bytes.take(1024).toByteArray(), Charsets.UTF_8)
        return SUSPICIOUS_PATTERNS.any { pattern ->
            content.contains(pattern, ignoreCase = true)
        }
    }
    
    private suspend fun importPKPass(repository: WalletRepository, fileBytes: ByteArray): ImportResult {
        val result = repository.importPass(
            inputStream = ByteArrayInputStream(fileBytes), 
            fileName = "import.pkpass"
        )
        
        return result
    }
    
    private suspend fun importGenericFile(
        repository: WalletRepository,
        fileBytes: ByteArray,
        fileName: String
    ): ImportResult {
        return repository.importPass(ByteArrayInputStream(fileBytes).buffered(), fileName)
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
            } else 0L
        } ?: 0L
    }
    private fun showResult(context: Context, result: ImportResult) {
        val message = when (result) {
            is ImportResult.Success ->
                context.getString(
                    R.string.import_success_format,
                    result.formatName,
                    result.pass.title
                )
            is ImportResult.Failure -> {
                SecureLogger.e(TAG, "Import failed", null)
                context.getString(
                    R.string.import_failed_format,
                    result.error
                )
            }
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showError(context: Context, error: Exception) {
        SecureLogger.e(TAG, "Error processing file import", error)
        Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
    }

    companion object {
         fun getFileName(context: Context, uri: Uri): String {
            return if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst()) cursor.getString(index) else null
                }
            } else {
                uri.lastPathSegment
            } ?: "unknown_file"
        }
    }
}