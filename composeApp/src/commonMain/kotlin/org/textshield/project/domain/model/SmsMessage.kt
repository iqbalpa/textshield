package org.textshield.project.domain.model

/**
 * Domain model representing an SMS message
 */
data class SmsMessage(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isSpam: Boolean = false
) 