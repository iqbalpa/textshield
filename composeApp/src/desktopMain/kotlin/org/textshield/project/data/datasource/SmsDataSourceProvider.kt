package org.textshield.project.data.datasource

/**
 * Desktop implementation of SmsDataSourceProvider
 */
actual object SmsDataSourceProvider {
    actual fun createSmsDataSource(): SmsDataSource {
        // Desktop version doesn't need any context
        return SmsDataSource()
    }
} 