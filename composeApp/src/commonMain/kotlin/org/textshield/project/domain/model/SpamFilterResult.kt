package org.textshield.project.domain.model

/**
 * The result of filtering a message for spam
 */
data class SpamFilterResult(
    val isSpam: Boolean, 
    val matchedKeywords: List<String> = emptyList(),
    val confidenceScore: Float = 0.0f, // ML detection confidence (0.0-1.0)
    val detectionMethod: DetectionMethod = DetectionMethod.KEYWORD_BASED
)

/**
 * Method used for spam detection
 */
enum class DetectionMethod {
    KEYWORD_BASED, // Simple rule-based detection
    MACHINE_LEARNING // ML-based detection
} 