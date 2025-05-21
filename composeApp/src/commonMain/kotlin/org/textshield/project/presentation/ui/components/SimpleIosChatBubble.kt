package org.textshield.project.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.textshield.project.domain.model.SmsMessage

/**
 * iOS-style chat bubble component for the conversation view
 */
@Composable
fun SimpleIosChatBubble(
    message: SmsMessage,
    onLongClick: (() -> Unit)? = null,
    isUserMessage: Boolean = false
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUserMessage) 18.dp else 5.dp,
        bottomEnd = if (isUserMessage) 5.dp else 18.dp
    )
    
    // iOS-style colors
    val bubbleColor = animateColorAsState(
        targetValue = when {
            message.isSpam -> MaterialTheme.colorScheme.errorContainer 
            isUserMessage -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)  // iOS blue bubble
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) // iOS gray bubble
        },
        animationSpec = tween(durationMillis = 300)
    )
    
    val textColor = animateColorAsState(
        targetValue = when {
            message.isSpam -> MaterialTheme.colorScheme.error
            isUserMessage -> MaterialTheme.colorScheme.onPrimary  // White text for blue bubbles
            else -> MaterialTheme.colorScheme.onSurface  // Black text for gray bubbles
        },
        animationSpec = tween(durationMillis = 300)
    )
    
    // Long press handling for contextual menu
    var showActions by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp, horizontal = 16.dp),
        horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start
    ) {
        // Timestamp for the first message of a group
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // iOS-style timestamp pill
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor.value)
                .clickable { if (onLongClick != null) showActions = true }
                .padding(12.dp)
        ) {
            Column {
                // Spam warning if needed
                if (message.isSpam) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Spam Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Potential spam (${(message.confidenceScore * 100).toInt()}%)",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Message content
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.value
                )
                
                // Delivery status and timestamp (iOS style)
                if (isUserMessage) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Delivered",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.value.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Show actions menu when long pressed
        if (showActions) {
            // iOS-style action menu
            IosChatBubbleActions(
                message = message,
                onDismiss = { showActions = false },
                onDelete = {
                    // Handle delete (would call onDelete passed from parent)
                    showActions = false
                },
                onMarkAsSpam = {
                    // Handle spam marking (would call onMarkAsSpam from parent)
                    showActions = false
                }
            )
        }
    }
}

@Composable
private fun IosChatBubbleActions(
    message: SmsMessage,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsSpam: () -> Unit
) {
    // iOS-style action sheet
    Surface(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            TextButton(
                onClick = onMarkAsSpam,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (message.isSpam) "Not Spam" else "Mark as Spam",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Delete Message",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// iOS-style date formatter
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 24 * 60 * 60 * 1000 -> "Today" 
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        else -> "Older"
    }
} 