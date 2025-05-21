package org.textshield.project.data.datasource

/**
 * Provider for platform-specific SmsDataSource implementation
 */
expect object SmsDataSourceProvider {
    fun createSmsDataSource(): SmsDataSource
} 