package org.textshield.project.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.textshield.project.domain.model.SmsMessage
import org.textshield.project.domain.model.SpamAction
import org.textshield.project.presentation.ui.components.MessageItem
import org.textshield.project.presentation.viewmodel.InboxState
import org.textshield.project.presentation.viewmodel.InboxTab
import org.textshield.project.presentation.viewmodel.InboxViewModel

/**
 * Main inbox screen with tabs for inbox and spam messages
 */
@Composable
fun InboxScreen(viewModel: InboxViewModel) {
    val state by viewModel.state.collectAsState()
    
    InboxScreenContent(
        state = state,
        onTabSelected = { viewModel.setCurrentTab(it) },
        onMarkAsSpam = { viewModel.markMessageSpamStatus(it, true) },
        onMarkAsNotSpam = { viewModel.markMessageSpamStatus(it, false) },
        onRemoveMessage = { viewModel.performSpamAction(it, SpamAction.REMOVED) },
        onSetDefaultSpamAction = { viewModel.setDefaultSpamAction(it) },
        onRefresh = { viewModel.loadMessages() },
        onToggleMessageSelection = { viewModel.toggleMessageSelection(it) },
        onSelectAllSpam = { viewModel.selectAllSpamMessages() },
        onClearSelections = { viewModel.clearAllSelections() },
        onDeleteSelected = { viewModel.deleteSelectedMessages() }
    )
}

@Composable
private fun InboxScreenContent(
    state: InboxState,
    onTabSelected: (InboxTab) -> Unit,
    onMarkAsSpam: (String) -> Unit,
    onMarkAsNotSpam: (String) -> Unit,
    onRemoveMessage: (String) -> Unit,
    onSetDefaultSpamAction: (SpamAction) -> Unit,
    onRefresh: () -> Unit,
    onToggleMessageSelection: (String) -> Unit,
    onSelectAllSpam: () -> Unit,
    onClearSelections: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TextShield",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onRefresh) {
                        // Refresh icon would go here
                        Text("⟳")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Default SMS app warning if needed
            if (!state.isDefaultSmsApp) {
                DefaultSmsAppWarning()
            }
            
            // Tab row
            TabRow(
                selectedTabIndex = if (state.currentTab == InboxTab.INBOX) 0 else 1
            ) {
                Tab(
                    selected = state.currentTab == InboxTab.INBOX,
                    onClick = { onTabSelected(InboxTab.INBOX) },
                    text = { Text("Inbox") }
                )
                Tab(
                    selected = state.currentTab == InboxTab.SPAM,
                    onClick = { onTabSelected(InboxTab.SPAM) },
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Spam")
                            if (state.spamMessages.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge { 
                                    Text(state.spamMessages.size.toString()) 
                                }
                            }
                        }
                    }
                )
            }
            
            // Settings action selector
            if (state.currentTab == InboxTab.SPAM) {
                SpamActionSelector(
                    currentAction = state.defaultSpamAction,
                    onActionSelected = onSetDefaultSpamAction
                )
                
                // Show bulk actions toolbar only in spam tab
                if (state.spamMessages.isNotEmpty()) {
                    BulkActionsToolbar(
                        selectedCount = state.selectedMessageIds.size,
                        totalCount = state.spamMessages.size,
                        onSelectAll = onSelectAllSpam, 
                        onClearSelections = onClearSelections,
                        onDeleteSelected = onDeleteSelected,
                        isDeleteInProgress = state.isBulkDeleteInProgress,
                        isDefaultSmsApp = state.isDefaultSmsApp
                    )
                }
            }
            
            // Content based on selected tab
            when (state.currentTab) {
                InboxTab.INBOX -> MessageList(
                    messages = state.inboxMessages,
                    isLoading = state.isLoading,
                    emptyMessage = "Your inbox is empty",
                    onMarkAsSpam = onMarkAsSpam,
                    onMarkAsNotSpam = onMarkAsNotSpam,
                    onRemoveMessage = onRemoveMessage,
                    selectedMessageIds = state.selectedMessageIds,
                    onToggleSelection = null // Not used in inbox tab
                )
                InboxTab.SPAM -> MessageList(
                    messages = state.spamMessages,
                    isLoading = state.isLoading || state.isBulkDeleteInProgress,
                    emptyMessage = "No spam messages",
                    onMarkAsSpam = onMarkAsSpam,
                    onMarkAsNotSpam = onMarkAsNotSpam,
                    onRemoveMessage = onRemoveMessage,
                    selectedMessageIds = state.selectedMessageIds,
                    onToggleSelection = onToggleMessageSelection
                )
            }
            
            // Error message if any
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(error)
                }
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
private fun DefaultSmsAppWarning() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚠️ Message Deletion Restricted",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "TextShield needs to be set as your default SMS app to delete messages. This is an Android requirement for SMS management.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun SpamActionSelector(
    currentAction: SpamAction,
    onActionSelected: (SpamAction) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Default action for detected spam:",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = currentAction == SpamAction.MARKED,
                onClick = { onActionSelected(SpamAction.MARKED) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Mark as spam",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onActionSelected(SpamAction.MARKED) }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            RadioButton(
                selected = currentAction == SpamAction.REMOVED,
                onClick = { onActionSelected(SpamAction.REMOVED) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Remove message",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onActionSelected(SpamAction.REMOVED) }
            )
        }
        
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun MessageList(
    messages: List<SmsMessage>,
    isLoading: Boolean,
    emptyMessage: String,
    onMarkAsSpam: (String) -> Unit,
    onMarkAsNotSpam: (String) -> Unit,
    onRemoveMessage: (String) -> Unit,
    selectedMessageIds: Set<String> = emptySet(),
    onToggleSelection: ((String) -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (messages.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    val isSelected = selectedMessageIds.contains(message.id)
                    
                    MessageItem(
                        message = message,
                        onMarkAsSpam = { onMarkAsSpam(message.id) },
                        onMarkAsNotSpam = { onMarkAsNotSpam(message.id) },
                        onRemoveMessage = { onRemoveMessage(message.id) },
                        isSelected = isSelected,
                        onToggleSelection = if (onToggleSelection != null) {
                            { onToggleSelection(message.id) }
                        } else null
                    )
                }
            }
        }
    }
} 