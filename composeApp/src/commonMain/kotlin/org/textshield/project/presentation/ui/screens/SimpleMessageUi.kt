package org.textshield.project.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction
import org.textshield.project.presentation.di.Dependencies
import org.textshield.project.presentation.viewmodel.InboxTab
import org.textshield.project.presentation.viewmodel.InboxViewModel

/**
 * Simple message screen for inbox with modern UI
 */
@Composable
fun SimpleMessageScreen() {
    val viewModel = Dependencies.inboxViewModel
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Automatically show spam tab if there are spam messages
    LaunchedEffect(state.spamMessages) {
        if (state.spamMessages.isNotEmpty()) {
            viewModel.setCurrentTab(InboxTab.SPAM)
        }
    }
    
    // Set the current screen based on the selected tab
    var currentScreen by remember { 
        mutableStateOf<Screen>(
            if (state.currentTab == InboxTab.SPAM) Screen.Spam else Screen.Inbox
        ) 
    }
    
    // Update the current screen when tab changes
    LaunchedEffect(state.currentTab) {
        currentScreen = when (state.currentTab) {
            InboxTab.INBOX -> Screen.Inbox
            InboxTab.SPAM -> Screen.Spam
        }
    }
    
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var selectedConversationMessages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    
    // Group messages by conversation (sender)
    val messagesBySender = remember(state.inboxMessages) {
        state.inboxMessages.groupBy { it.sender }
    }
    
    val spamMessagesBySender = remember(state.spamMessages) {
        state.spamMessages.groupBy { it.sender }
    }
    
    // Show error message if any
    LaunchedEffect(state.error) {
        state.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            when (currentScreen) {
                is Screen.Inbox -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // App bar
                        AppBar(
                            title = "Messages",
                            isInbox = true,
                            onToggle = { 
                                viewModel.setCurrentTab(InboxTab.SPAM)
                                currentScreen = Screen.Spam 
                            },
                            onRefresh = { viewModel.loadMessages() }
                        )
                        
                        // Default SMS app warning
                        if (!state.isDefaultSmsApp) {
                            DefaultSmsAppWarning(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // Content
                        if (state.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (messagesBySender.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Your inbox is empty",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(
                                    messagesBySender.map { (sender, messages) -> 
                                        messages.maxByOrNull { it.timestamp } ?: messages.first()
                                    }.sortedByDescending { it.timestamp }
                                ) { message ->
                                    SimpleMessageItem(
                                        message = message,
                                        onClick = {
                                            selectedConversationId = message.sender
                                            selectedConversationMessages = messagesBySender[message.sender] ?: emptyList()
                                            currentScreen = Screen.Conversation
                                        },
                                        onMarkAsSpam = { viewModel.markMessageSpamStatus(message.id, true) },
                                        onDelete = { viewModel.performSpamAction(message.id, SpamAction.REMOVED) },
                                        isDefault = state.isDefaultSmsApp
                                    )
                                }
                            }
                        }
                    }
                }
                
                is Screen.Spam -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // App bar
                        AppBar(
                            title = "Spam" + if (state.spamMessages.isNotEmpty()) " (${state.spamMessages.size})" else "",
                            isInbox = false,
                            onToggle = { 
                                viewModel.setCurrentTab(InboxTab.INBOX)
                                currentScreen = Screen.Inbox 
                            },
                            onRefresh = { viewModel.loadMessages() }
                        )
                        
                        // Default SMS app warning
                        if (!state.isDefaultSmsApp) {
                            DefaultSmsAppWarning(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // Settings action selector
                        SpamActionSelector(
                            currentAction = state.defaultSpamAction,
                            onActionSelected = { viewModel.setDefaultSpamAction(it) }
                        )
                        
                        // Show bulk actions toolbar if there are spam messages
                        if (state.spamMessages.isNotEmpty()) {
                            BulkActionsToolbar(
                                selectedCount = state.selectedMessageIds.size,
                                totalCount = state.spamMessages.size,
                                onSelectAll = { viewModel.selectAllSpamMessages() },
                                onClearSelections = { viewModel.clearAllSelections() },
                                onDeleteSelected = { viewModel.deleteSelectedMessages() },
                                isDeleteInProgress = state.isBulkDeleteInProgress,
                                isDefaultSmsApp = state.isDefaultSmsApp
                            )
                        }
                        
                        // Content
                        if (state.isLoading || state.isBulkDeleteInProgress) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (spamMessagesBySender.isEmpty()) {
                            SpamEmptyState(
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(
                                    spamMessagesBySender.map { (sender, messages) -> 
                                        messages.maxByOrNull { it.timestamp } ?: messages.first()
                                    }.sortedByDescending { it.timestamp }
                                ) { message ->
                                    val isSelected = state.selectedMessageIds.contains(message.id)
                                    
                                    SimpleMessageItem(
                                        message = message,
                                        onClick = {
                                            if (state.selectedMessageIds.isNotEmpty()) {
                                                viewModel.toggleMessageSelection(message.id)
                                            } else {
                                                selectedConversationId = message.sender
                                                selectedConversationMessages = spamMessagesBySender[message.sender] ?: emptyList()
                                                currentScreen = Screen.Conversation
                                            }
                                        },
                                        onMarkAsNotSpam = { viewModel.markMessageSpamStatus(message.id, false) },
                                        onDelete = { viewModel.performSpamAction(message.id, SpamAction.REMOVED) },
                                        isDefault = state.isDefaultSmsApp,
                                        isSelected = isSelected,
                                        onToggleSelection = { viewModel.toggleMessageSelection(message.id) },
                                        showCheckbox = true
                                    )
                                }
                            }
                        }
                    }
                }
                
                is Screen.Conversation -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Conversation app bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "←",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clickable { currentScreen = if (selectedConversationMessages.any { it.isSpam }) Screen.Spam else Screen.Inbox },
                                style = MaterialTheme.typography.headlineSmall
                            )
                            
                            Text(
                                text = selectedConversationId ?: "Conversation",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        // Messages
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = false,
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(selectedConversationMessages.sortedBy { it.timestamp }) { message ->
                                SimpleChatBubble(
                                    message = message,
                                    onDelete = { 
                                        viewModel.performSpamAction(message.id, SpamAction.REMOVED)
                                        // If this was the last message, go back to the previous screen
                                        if (selectedConversationMessages.size <= 1) {
                                            currentScreen = if (message.isSpam) Screen.Spam else Screen.Inbox
                                        }
                                    },
                                    onMarkAsSpam = { viewModel.markMessageSpamStatus(message.id, !message.isSpam) },
                                    isDefault = state.isDefaultSmsApp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBar(
    title: String,
    isInbox: Boolean,
    onToggle: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            
            Row(
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onRefresh) {
                    Text("⟳", style = MaterialTheme.typography.titleLarge)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(onClick = onToggle) {
                    Text(text = if (isInbox) "Spam" else "Inbox")
                }
            }
        }
    }
}

@Composable
private fun SpamEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No spam messages",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your inbox is clean!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DefaultSmsAppWarning(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Set TextShield as Default SMS App",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Message deletion requires TextShield to be your default SMS app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun SpamActionSelector(
    currentAction: SpamAction,
    onActionSelected: (SpamAction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Default action for detected spam:",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentAction == SpamAction.MARKED,
                    onClick = { onActionSelected(SpamAction.MARKED) }
                )
                Text(
                    text = "Mark as spam",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onActionSelected(SpamAction.MARKED) }
                )
                
                Spacer(modifier = Modifier.width(24.dp))
                
                RadioButton(
                    selected = currentAction == SpamAction.REMOVED,
                    onClick = { onActionSelected(SpamAction.REMOVED) }
                )
                Text(
                    text = "Remove message",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onActionSelected(SpamAction.REMOVED) }
                )
            }
        }
    }
}

@Composable
private fun BulkActionsToolbar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelections: () -> Unit,
    onDeleteSelected: () -> Unit,
    isDeleteInProgress: Boolean,
    isDefaultSmsApp: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Selection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount of $totalCount selected",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row {
                    // Select All button
                    if (selectedCount < totalCount) {
                        TextButton(onClick = onSelectAll) {
                            Text("Select All")
                        }
                    } else if (selectedCount > 0) {
                        // Clear selection button
                        TextButton(onClick = onClearSelections) {
                            Text("Clear Selection")
                        }
                    }
                }
            }
            
            // Delete button - only show if messages are selected
            if (selectedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeleteInProgress && isDefaultSmsApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isDeleteInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isDeleteInProgress) "Deleting..." else "Delete Selected ($selectedCount)"
                    )
                }
                
                if (!isDefaultSmsApp) {
                    Text(
                        text = "Set TextShield as your default SMS app to delete messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleMessageItem(
    message: SmsMessage,
    onClick: () -> Unit,
    onMarkAsSpam: (() -> Unit)? = null,
    onMarkAsNotSpam: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isDefault: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    showCheckbox: Boolean = false
) {
    var showActions by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for selection in spam tab
            if (showCheckbox) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { if (onToggleSelection != null) onToggleSelection() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // Contact initial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (message.isSpam) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.sender.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Message content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Timestamp
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Message preview
                Text(
                    text = if (message.isSpam) "🚫 ${message.content}" else message.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Action buttons
            IconButton(
                onClick = { showActions = true }
            ) {
                Text(
                    text = "⋮",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        
        // Message actions dialog
        if (showActions) {
            AlertDialog(
                onDismissRequest = { showActions = false },
                title = { Text("Message Actions") },
                text = { Text("What would you like to do with this message?") },
                confirmButton = {
                    if (isDefault && onDelete != null) {
                        TextButton(
                            onClick = {
                                onDelete()
                                showActions = false
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (message.isSpam && onMarkAsNotSpam != null) {
                                onMarkAsNotSpam()
                            } else if (!message.isSpam && onMarkAsSpam != null) {
                                onMarkAsSpam()
                            }
                            showActions = false
                        }
                    ) {
                        Text(if (message.isSpam) "Not Spam" else "Mark as Spam")
                    }
                }
            )
        }
    }
    
    Divider(modifier = Modifier.padding(start = 80.dp))
}

@Composable
private fun SimpleChatBubble(
    message: SmsMessage,
    onDelete: () -> Unit,
    onMarkAsSpam: () -> Unit,
    isDefault: Boolean
) {
    var showActions by remember { mutableStateOf(false) }
    val isUserMessage = false // For illustrative purposes, all messages are from the contact
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUserMessage) 16.dp else 0.dp,
                        topEnd = if (isUserMessage) 0.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(
                    when {
                        message.isSpam -> MaterialTheme.colorScheme.errorContainer
                        isUserMessage -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .clickable { showActions = true }
                .padding(12.dp)
        ) {
            Column {
                if (message.isSpam) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ Potential spam",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "(${(message.confidenceScore * 100).toInt()}% confidence)",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUserMessage) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else if (message.isSpam) 
                        MaterialTheme.colorScheme.onErrorContainer
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End),
                    color = if (isUserMessage) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                    else if (message.isSpam) 
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        if (showActions) {
            Card(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(if (isUserMessage) Alignment.End else Alignment.Start),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            onMarkAsSpam()
                            showActions = false
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (message.isSpam) "Not Spam" else "Mark as Spam")
                    }
                    
                    if (isDefault) {
                        TextButton(
                            onClick = {
                                onDelete()
                                showActions = false
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// Simple timestamp formatter, in real app this would be formatted properly
private fun formatTimestamp(timestamp: Long): String {
    // Convert timestamp to relative time like "Today", "Yesterday", or "Aug 15"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 24 * 60 * 60 * 1000 -> "Today"
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        else -> "Older"
    }
}

// Screens for navigation
sealed class Screen {
    object Inbox : Screen()
    object Spam : Screen()
    object Conversation : Screen()
} 