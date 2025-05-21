package org.textshield.project.domain.detector

/**
 * Desktop implementation of SpamDetectorProvider
 */
actual object SpamDetectorProvider {
    // For Desktop, we use the rule-based detector since we can't use TF Lite
    private val detector: SpamDetector = RuleBasedSpamDetector()
    
    init {
        // Initialize the detector
        kotlinx.coroutines.runBlocking {
            detector.initialize()
        }
    }
    
    /**
     * Get spam detector for Desktop - always returns rule-based implementation
     */
    actual fun getSpamDetector(): SpamDetector {
        return detector
    }
} 