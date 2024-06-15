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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

@Composable
fun ChatList(
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    onChatClick: (route: Int, taskId: String?, taskName: String?, userId: Long?, userMail: String?) -> Unit,
) {
    val chats by vm.chats.collectAsState(initial = listOf())
    val groupChat = vm.getGroupChatsOfTeam()

    WorkStreamTheme {
        Scaffold (
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
                LazyColumn (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .padding(padding)
                ) {
                    // Gorup chat
                    item {
                        Text(text = "Team chat", fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(bottom = 8.dp))
                        Column(
                            modifier = Modifier
                                .padding(top = 5.dp, bottom = 5.dp)
                                .clickable { onChatClick(8, null, null, -1, null) }
                        ) {
                            SmallChatBox(
                                userName = "Team chat",
                                lastMessage = groupChat?.last()?.author?.firstName + ": " + groupChat?.last()?.text,
                                timestamp = groupChat?.last()?.timestamp,
                                isGroup = true
                            )
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) }
                    // Private chats
                    item { Text(text = "Private chats", fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(bottom = 8.dp)) }
                    chats.forEach { chat ->
                        Log.d("Chat", "Chat with id " + chat.user1Id + " " + chat.user2Id)
                        item {
                            val destUserId = if (chat.user1Id == vm.user.email) chat.user2Id else chat.user1Id
                            val destUser = vm.usersList.find {
                                it.email == destUserId
                            }
                            Column(
                                modifier = Modifier
                                    .padding(top = 5.dp, bottom = 5.dp)
                                    .clickable {
                                        onChatClick(8, null, null, null, destUserId)
                                    }
                            ) {
                                SmallChatBox(
                                    userName = destUser?.firstName + " " + destUser?.lastName,
                                    lastMessage = chat.messages.lastOrNull()?.text?:"No message",
                                    timestamp = chat.messages.lastOrNull()?.timestamp?: Timestamp.now(),
                                    isGroup = false
                                )

                            }
                        }
                    }
                }
            }
        )
    }
}