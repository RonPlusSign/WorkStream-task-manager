package it.polito.workstream.ui.screens.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
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
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

@Composable
fun ChatList(
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    onChatClick: (route: Int, taskId: Int?, taskName: String?, userId: Long?) -> Unit,
) {
    val chats by vm.chats.collectAsState()
    val groupChat by vm.groupChat.collectAsState()

    WorkStreamTheme {
        Scaffold (
            floatingActionButton = {
               ExtendedFloatingActionButton(
                   onClick = { onChatClick(9, null, null, null) },
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
                                .clickable { onChatClick(8, null, null, -1) }
                        ) {
                            SmallChatBox(
                                userName = "Team chat",
                                lastMessage = groupChat.last().author.firstName + ": " + groupChat.last().text,
                                timestamp = groupChat.last().timestamp,
                                isGroup = true
                            )
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) }
                    // Private chats
                    item { Text(text = "Private chats", fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(bottom = 8.dp)) }
                    chats.forEach { chat ->
                        item {
                            Column(
                                modifier = Modifier
                                    .padding(top = 5.dp, bottom = 5.dp)
                                    .clickable { onChatClick(8, null, null, chat.key.id) }
                            ) {
                                SmallChatBox(
                                    userName = chat.key.firstName + " " + chat.key.lastName,
                                    lastMessage = chat.value.last().text,
                                    timestamp = chat.value.last().timestamp,
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