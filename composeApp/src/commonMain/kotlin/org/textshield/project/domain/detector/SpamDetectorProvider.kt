package org.textshield.project.domain.detector

/**
 * Provider for platform-specific SpamDetector implementation
 */
expect object SpamDetectorProvider {
    /**
     * Get the platform-specific SpamDetector implementation
     */
    fun getSpamDetector(): SpamDetector
} 