package org.textshield.project.data.datasource

import android.content.Context

/**
 * Android implementation of SmsDataSourceProvider
 */
actual object SmsDataSourceProvider {
    // Android application context for services requiring context
    private lateinit var appContext: Context

    /**
     * Initialize the provider with Android application context
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    actual fun createSmsDataSource(): SmsDataSource {
        if (!::appContext.isInitialized) {
            throw IllegalStateException("SmsDataSourceProvider not initialized. Call init(context) first.")
        }
        
        return SmsDataSource(appContext)
    }
} 