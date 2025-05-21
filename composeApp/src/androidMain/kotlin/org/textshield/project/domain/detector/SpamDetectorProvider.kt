package org.textshield.project.domain.detector

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android implementation of SpamDetectorProvider
 */
actual object SpamDetectorProvider {
    private lateinit var appContext: Context
    private lateinit var spamDetector: SpamDetector
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Initialize the provider with Android application context
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        
        // Create TFLite-based detector for Android
        spamDetector = TFLiteSpamDetector(appContext)
        
        // Start initializing the detector in the background
        scope.launch {
            spamDetector.initialize()
        }
    }
    
    /**
     * Get spam detector for Android implementation
     */
    actual fun getSpamDetector(): SpamDetector {
        if (!::spamDetector.isInitialized) {
            throw IllegalStateException("SpamDetectorProvider not initialized. Call init(context) first.")
        }
        
        return spamDetector
    }
} 