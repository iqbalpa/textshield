package org.textshield.project.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.textshield.project.domain.model.DetectionMethod
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction
import java.text.SimpleDateFormat
import java.util.*

/**
 * Displays a single SMS message item in the list
 */
@Composable
fun MessageItem(
    message: SmsMessage,
    onMarkAsSpam: () -> Unit,
    onMarkAsNotSpam: () -> Unit,
    onRemoveMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardColor = if (message.isAutoProcessed) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Sender and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message content
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            
            // If spam, show confidence score and detection method
            if (message.isSpam) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Linear progress indicator for confidence
                SpamConfidenceIndicator(
                    confidence = message.confidenceScore,
                    detectionMethod = message.detectionMethod
                )
                
                if (message.isAutoProcessed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatically processed: ${message.actionTaken.name.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (message.isSpam) {
                    // Not spam button
                    Button(
                        onClick = onMarkAsNotSpam,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Not Spam")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Remove button
                    OutlinedButton(
                        onClick = onRemoveMessage,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Remove")
                    }
                } else {
                    OutlinedButton(
                        onClick = onMarkAsSpam
                    ) {
                        Text("Mark as Spam")
                    }
                }
            }
        }
    }
}

@Composable
fun SpamConfidenceIndicator(
    confidence: Float,
    detectionMethod: DetectionMethod
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spam Score: ${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
            )
            
            Text(
                text = when(detectionMethod) {
                    DetectionMethod.MACHINE_LEARNING -> "ML Detection"
                    DetectionMethod.KEYWORD_BASED -> "Rule-based"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress indicator showing confidence level
        LinearProgressIndicator(
            progress = { confidence },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = when {
                confidence < 0.6f -> MaterialTheme.colorScheme.primary
                confidence < 0.8f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Format timestamp to a readable date/time
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return format.format(date)
} 