package org.textshield.project.domain.detector

import org.textshield.project.domain.model.DetectionMethod
import org.textshield.project.domain.model.SpamFilterResult

/**
 * A simple rule-based spam detector that uses keyword matching
 */
class RuleBasedSpamDetector : SpamDetector {
    // Common spam keywords
    private val spamKeywords = listOf(
        "win", "winner", "free", "prize", "urgent", "congrats", "congratulations", 
        "claim", "offer", "lottery", "limited time", "cash", "click", "subscribe",
        "investment", "crypto", "bitcoin", "bank account", "verify", "password",
        "suspicious", "activate", "deactivate", "account locked", "security alert",
        // Additional spam keywords
        "act now", "special offer", "cash prize", "instant approval", "no credit check",
        "you've been selected", "guaranteed", "risk-free", "double your money",
        "earn money fast", "discount", "best price", "call now", "get rich", "money back",

        "cair", "dana darurat", "paylater", "bayar nanti", "bayar kemudian", "bayar kembali",
    )
    
    // Initialize to true by default for immediate use
    private var initialized = true
    
    override suspend fun initialize() {
        // Nothing to initialize for rule-based detector
        initialized = true
    }
    
    override fun isInitialized(): Boolean {
        return initialized
    }
    
    override suspend fun detectSpam(messageContent: String): SpamFilterResult {
        if (!isInitialized()) {
            initialize()
        }
        
        // Convert to lowercase for case-insensitive matching
        val lowerContent = messageContent.lowercase()
        
        // Find matching keywords
        val matchedKeywords = spamKeywords.filter { keyword ->
            lowerContent.contains(keyword.lowercase())
        }
        
        // Calculate a simple confidence score based on number of spam keywords found
        val confidenceScore = when {
            matchedKeywords.isEmpty() -> 0.0f
            matchedKeywords.size == 1 -> 0.6f
            matchedKeywords.size == 2 -> 0.8f
            else -> 0.95f
        }
        
        // Message is considered spam if it contains any spam keywords
        val isSpam = matchedKeywords.isNotEmpty()
        
        return SpamFilterResult(
            isSpam = isSpam,
            matchedKeywords = matchedKeywords,
            confidenceScore = confidenceScore,
            detectionMethod = DetectionMethod.KEYWORD_BASED
        )
    }
} 