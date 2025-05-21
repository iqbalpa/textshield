package org.textshield.project.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.textshield.project.domain.model.SmsMessage
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
        onRefresh = { viewModel.loadMessages() }
    )
}

@Composable
private fun InboxScreenContent(
    state: InboxState,
    onTabSelected: (InboxTab) -> Unit,
    onMarkAsSpam: (String) -> Unit,
    onMarkAsNotSpam: (String) -> Unit,
    onRefresh: () -> Unit
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
                    text = { Text("Spam") }
                )
            }
            
            // Content based on selected tab
            when (state.currentTab) {
                InboxTab.INBOX -> MessageList(
                    messages = state.inboxMessages,
                    isLoading = state.isLoading,
                    emptyMessage = "Your inbox is empty",
                    onMarkAsSpam = onMarkAsSpam,
                    onMarkAsNotSpam = onMarkAsNotSpam
                )
                InboxTab.SPAM -> MessageList(
                    messages = state.spamMessages,
                    isLoading = state.isLoading,
                    emptyMessage = "No spam messages",
                    onMarkAsSpam = onMarkAsSpam,
                    onMarkAsNotSpam = onMarkAsNotSpam
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
private fun MessageList(
    messages: List<SmsMessage>,
    isLoading: Boolean,
    emptyMessage: String,
    onMarkAsSpam: (String) -> Unit,
    onMarkAsNotSpam: (String) -> Unit
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
                    MessageItem(
                        message = message,
                        onMarkAsSpam = { onMarkAsSpam(message.id) },
                        onMarkAsNotSpam = { onMarkAsNotSpam(message.id) }
                    )
                }
            }
        }
    }
} 