package it.polito.workstream.ui.screens.chats

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

@Composable
fun ChatList(
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    onChatClick: (route: Int, taskId: String?, taskName: String?, userId: Long?, userMail: String?) -> Unit,
) {
    val chats = vm.chats.collectAsState(initial = listOf()).value
    val activeTeamId = vm.activeTeamId.collectAsState().value
    val activeTeam = vm.fetchActiveTeam(activeTeamId).collectAsState(initial = null).value
    val teamMembers = vm.teamMembers.collectAsState(initial = listOf()).value
    val groupChat = vm.fetchGroupChat().collectAsState(initial = null).value

    val lastMessageAuthor = teamMembers.find { it.email == groupChat?.messages?.lastOrNull()?.authorId }

    WorkStreamTheme {
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { onChatClick(9, null, null, null, null) },
                    text = { Text("New chat") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Task") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .height(40.dp)
                )
            },
            content = { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .padding(padding)
                ) {
                    // Group chat
                    item {
                        Text(text = "Team chat", fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(bottom = 8.dp))
                        Column(
                            modifier = Modifier
                                .padding(top = 5.dp, bottom = 5.dp)
                                .clickable { onChatClick(8, null, null, -1, null) }
                        ) {
                            SmallChatBox(
                                destUser = null,
                                userName = "Team chat",
                                lastMessage = if (groupChat?.messages?.lastOrNull() != null) (lastMessageAuthor?.firstName ?: "") + " : " + groupChat.messages.lastOrNull()?.text else "No messages yet",
                                timestamp = groupChat?.messages?.lastOrNull()?.timestamp,
                                isGroup = true,
                                activeTeam = activeTeam,
                                unseenMessages = vm.unseenGroupMessages.collectAsState(initial = 0).value ?: 0
                            )
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) }
                    // Private chats
                    item { Text(text = "Private chats", fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(bottom = 8.dp)) }
                    chats.sortedBy { it.messages.lastOrNull()?.timestamp }.filter { it.messages.size > 0 }.forEach { chat ->
                        item {
                            val destUserId = if (chat.user1Id == vm.user.email) chat.user2Id else chat.user1Id
                            val destUser = teamMembers.find {
                                it.email == destUserId
                            }
                            val unseenMessages = vm.countUnseenChatMessages(destUserId).collectAsState(initial = 0).value
                            Column(
                                modifier = Modifier
                                    .padding(top = 5.dp, bottom = 5.dp)
                                    .clickable {
                                        onChatClick(8, null, null, null, destUserId)
                                    }
                            ) {
                                SmallChatBox(
                                    destUser = destUser,
                                    userName = destUser?.firstName + " " + destUser?.lastName,
                                    lastMessage = chat.messages.sortedBy { it.timestamp }.lastOrNull()?.text ?: "No message",
                                    timestamp = chat.messages.sortedBy { it.timestamp }.lastOrNull()?.timestamp,
                                    isGroup = false,
                                    activeTeam = activeTeam,
                                    unseenMessages = unseenMessages
                                )

                            }
                        }
                    }
                }
            }
        )
    }
}