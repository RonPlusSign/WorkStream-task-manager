package it.polito.workstream.ui.screens.chats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.theme.Purple80
import it.polito.workstream.ui.theme.PurpleGrey80
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun GroupChat(vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))) {
    val groupChat by vm.groupChat.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // The list of messages
        LazyColumn(
            reverseLayout = true,
            modifier = Modifier
                .padding(5.dp)
                .weight(1f)
        ) {
            groupChat.reversed().forEach { mex ->
                item {
                    GroupChatMessageBox(message = mex, vm)
                }
            }
        }
        // The input box to send a message
        GroupChatInputBox(vm)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupChatMessageBox(message: ChatMessage, vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))) {
    var messageToEdit by rememberSaveable { mutableStateOf<Long?>(null) }

    Row(
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            //.align(if (mex.isFromMe) Alignment.End else Alignment.Start)
            .padding(bottom = 3.dp, top = 3.dp, start = 16.dp, end = 16.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    if (message.isFromMe) {
                        messageToEdit = message.id
                        vm.toggleShowEditDialog()
                    }
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(
                    RoundedCornerShape(
                        topStart = 48f,
                        topEnd = 48f,
                        bottomStart = if (message.isFromMe) 48f else 0f,
                        bottomEnd = if (message.isFromMe) 0f else 48f
                    )
                )
                .widthIn(10.dp, 320.dp)
                .background(if (message.isFromMe) Purple80 else PurpleGrey80)
                .padding(16.dp)
        ) {
            Column {
                Text(text = message.author.firstName + " " + message.author.lastName, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                Text(text = message.text)
                Row(
                    modifier = Modifier
                        .align(if (message.isFromMe) Alignment.Start else Alignment.End)
                        .padding(top = 2.dp)
                ) {
                    message.timestamp?.let {
                        Text(
                            text = it.format(DateTimeFormatter.ofPattern("HH:mm")),
                            fontSize = 12.sp
                        )
                    }
                }

            }
        }
    }

    messageToEdit?.let { EditGroupMessageSheet(it, vm) }
}

@Composable
fun GroupChatInputBox(vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))) {
    var newMessage by remember { mutableStateOf("") }
    val users = vm.teamMembers.collectAsState().value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = newMessage,
            onValueChange = { newMessage = it },
            placeholder = { Text("Send a message") },
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "",
                    modifier = Modifier.clickable {
                        vm.sendGroupMessage(ChatMessage(newMessage, users[0], true, LocalDateTime.now()))
                        newMessage = ""
//                        sleep(5000);
//                        sendMessage(destUser, ChatMessage("Risposta di prova", "Autore", false))
                    }
                )
            }
        )
        //Icon(Icons.Default.Send, contentDescription = "", modifier = Modifier.size(40.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditGroupMessageSheet(messageToEdit: Long, vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))) {
    if (!vm.showEditDialog) return
    val sheetState = rememberModalBottomSheetState()

//    var editTextBoxState by remember { mutableStateOf(false) }
//    fun toggleEditTextBoxState() { editTextBoxState = !editTextBoxState }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { vm.toggleShowEditDialog() },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        ) {
            Button(
                modifier = Modifier.padding(5.dp),
                onClick = {
                    vm.deleteGroupMessage(messageToEdit)
                    vm.toggleShowEditDialog()
                }
            ) {
                Text(text = "Delete message")
            }
            //Spacer(modifier = Modifier.weight(0.00001f))
            Button(
                enabled = false,
                modifier = Modifier.padding(5.dp),
                onClick = {
                    // Todo
                    vm.toggleShowEditDialog()
                }
            ) {
                Text(text = "Edit message")
            }
        }

    }
}