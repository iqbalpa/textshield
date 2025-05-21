package org.textshield.project.domain.detector

import org.textshield.project.domain.model.SpamFilterResult

/**
 * Interface for spam detection using ML or other techniques
 */
interface SpamDetector {
    /**
     * Analyzes a message and returns a spam detection result
     * @param messageContent The content of the SMS message to analyze
     * @return SpamFilterResult containing classification and confidence score
     */
    suspend fun detectSpam(messageContent: String): SpamFilterResult
    
    /**
     * Initializes the spam detector, loading any necessary models or resources
     */
    suspend fun initialize()
    
    /**
     * Returns true if the detector is ready for detection
     */
    fun isInitialized(): Boolean
} 