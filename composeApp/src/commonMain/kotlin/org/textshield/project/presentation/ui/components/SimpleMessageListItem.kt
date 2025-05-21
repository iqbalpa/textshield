package org.textshield.project.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.textshield.project.domain.model.SmsMessage

/**
 * iOS-style message list item component for the message list
 */
@Composable
fun SimpleMessageListItem(
    message: SmsMessage,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    showCheckbox: Boolean = false
) {
    val isRead = true // In a real app, you'd track read status
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for selection
            if (showCheckbox) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { if (onToggleSelection != null) onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // Contact Initial or Profile Picture
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (message.isSpam) 
                            MaterialTheme.colorScheme.errorContainer
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.sender.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (message.isSpam) 
                               MaterialTheme.colorScheme.error 
                           else 
                               MaterialTheme.colorScheme.primary
                )
                
                // Spam indicator badge
                if (message.isSpam) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "!",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Message content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (!isRead) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Timestamp in iOS style
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Preview of message
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isSpam) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Spam Message",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (!isRead) FontWeight.Medium else FontWeight.Normal,
                            color = if (!isRead) 
                                      MaterialTheme.colorScheme.onSurface 
                                   else 
                                      MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // iOS-style chevron indicator (using proper icon instead of text)
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "View conversation",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
    
    // iOS-style separator line
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 80.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}

// Simple timestamp formatter in iOS style
private fun formatTimestamp(timestamp: Long): String {
    // In a real app, this would be properly formatted based on time difference
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 24 * 60 * 60 * 1000 -> "Today" // Today
        diff < 48 * 60 * 60 * 1000 -> "Yesterday" // Yesterday
        diff < 7 * 24 * 60 * 60 * 1000 -> "This Week" // This week
        else -> "Older" // Older
    }
} 