package org.textshield.project.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    isSelected: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSelectable = onToggleSelection != null
    
    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        message.isAutoProcessed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val cardBorder = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        Modifier
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(cardBorder)
            .clip(RoundedCornerShape(12.dp)),
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
                // Selection checkbox if in selection mode
                if (isSelectable) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection?.invoke() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                
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
                text = message.content.let { 
                    if (it.length > 50) it.take(50) + "..." else it 
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
 * Format timestamp according to the requirements:
 * - If the message was received today, show the time (e.g., "3:45 PM")
 * - If the message was received yesterday, show "Yesterday"
 * - If from current year but not today/yesterday, show "MMM dd" (e.g., "May 19")
 * - If from previous years, show "MMM dd, yyyy" (e.g., "May 19, 2023")
 */
private fun formatTimestamp(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val now = calendar.clone() as Calendar
    
    calendar.timeInMillis = timestamp
    
    // Check if the message is from today
    if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
        // Format as time: 3:45 PM 
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }
    
    // Check if the message is from yesterday
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday"
    }
    
    // Reset calendar to message date
    calendar.timeInMillis = timestamp
    
    // Check if the message is from current year
    return if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
        // Format as MMM dd (e.g., "May 19")
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    } else {
        // Format as MMM dd, yyyy (e.g., "May 19, 2023") for previous years
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    }
} 