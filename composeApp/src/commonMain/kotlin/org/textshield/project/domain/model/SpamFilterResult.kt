package org.textshield.project.domain.model

/**
 * The result of filtering a message for spam
 */
data class SpamFilterResult(
    val isSpam: Boolean, 
    val matchedKeywords: List<String> = emptyList()
) 