package org.textshield.project.domain.usecase

import org.textshield.project.domain.model.SpamFilterResult

class FilterSpamUseCase {
    // Common spam keywords
    private val spamKeywords = listOf(
        "win", "winner", "free", "prize", "urgent", "congrats", "congratulations", 
        "claim", "offer", "lottery", "limited time", "cash", "click", "subscribe"
    )
    
    /**
     * Check if a message is spam based on keyword matching
     */
    fun execute(messageContent: String): SpamFilterResult {
        // Convert to lowercase for case-insensitive matching
        val lowerContent = messageContent.lowercase()
        
        // Find matching keywords
        val matchedKeywords = spamKeywords.filter { keyword ->
            lowerContent.contains(keyword.lowercase())
        }
        
        // Message is considered spam if it contains any spam keywords
        val isSpam = matchedKeywords.isNotEmpty()
        
        return SpamFilterResult(
            isSpam = isSpam,
            matchedKeywords = matchedKeywords
        )
    }
} 