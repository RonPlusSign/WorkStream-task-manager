package it.polito.workstream.ui.screens.chats

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.theme.Purple40
import it.polito.workstream.ui.theme.Purple80
import it.polito.workstream.ui.theme.PurpleGrey40
import it.polito.workstream.ui.theme.PurpleGrey80
import it.polito.workstream.ui.theme.isLight
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Chat(
    destUserId: String,
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
//    messages: List<ChatMessage>?,
//    sendMessage: (User, ChatMessage) -> Unit
) {
//    val chat = vm.chats.map { it.find { it.user1Id == destUserId || it.user2Id == destUserId } }.collectAsState(
//        initial = null
//    )

    val chat = vm.fetchChats(vm.activeTeam.collectAsState(initial = null).value?.id ?: "", destUserId).collectAsState(
        initial = listOf()
    ).value.find { it.user1Id == destUserId || it.user2Id == destUserId }

    val activeTeam = vm.activeTeam.collectAsState(initial = null).value
    val teamMembers = vm.teamMembers.collectAsState(initial = listOf()).value

//    if (chat!=null && chat!!.messages.size > 0) {
//        for (mex in chat!!.messages){
//            if (!mex.seenBy.contains(vm.user.email))
//                vm.setMessageAsSeen(destUserId, mex.id)
//        }
//    }


    Column {
        // The list of messages
        LazyColumn(
            reverseLayout = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .weight(1f)
        ) {
            chat?.messages?.sortedBy { it.timestamp }?.reversed()?.forEach { mex ->
                val sender = teamMembers.find { it.email == mex.authorId }
                val isFromMe = mex.authorId == vm.user.email
                item {
                    ChatMessageBox(mex, destUserId, vm, isFromMe);
                }
            }
        }
        // The input box to send a message
        ChatInputBox(vm, destUserId)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBox(
    message: ChatMessage,
    destUserId: String,
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    isFromMe: Boolean
) {
    var messageToEdit by rememberSaveable { mutableStateOf<String?>(null) }

    // Depending on the dark mode, the color of the message will be different
    val otherMsgColor = if (MaterialTheme.colorScheme.isLight()) PurpleGrey80 else PurpleGrey40
    val myMsgColor = if (MaterialTheme.colorScheme.isLight()) Purple80 else Purple40

    if (!message.seenBy.contains(vm.user.email))
        vm.setMessageAsSeen(destUserId, message.id)

    Row(
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            //.align(if (mex.isFromMe) Alignment.End else Alignment.Start)
            .padding(bottom = 3.dp, top = 3.dp, start = 16.dp, end = 16.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    if (isFromMe) {
                        messageToEdit = message.id;
                        vm.toggleShowEditDialog();
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
                        bottomStart = if (isFromMe) 48f else 0f,
                        bottomEnd = if (isFromMe) 0f else 48f
                    )
                )
                .widthIn(10.dp, 320.dp)
                .background(if (isFromMe) myMsgColor else otherMsgColor)
                .padding(14.dp)
        ) {
            Column {
                Text(text = message.text, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Row(modifier = Modifier.align(if (isFromMe) Alignment.Start else Alignment.End)) {
                    Text(
                        text = DateTimeFormatter.ofPattern("HH:mm").format(message.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

            }
        }
    }

    messageToEdit?.let { EditMessageSheet(destUserId, it, vm) }
}

@Composable
fun ChatInputBox(
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    destUserId: String
) {
    var newMessage by remember { mutableStateOf("") }

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
                Icon(Icons.AutoMirrored.Filled.Send,
                    contentDescription = "",
                    modifier = Modifier.clickable {
                        vm.sendMessage(destUserId, ChatMessage("", newMessage, destUserId, Timestamp.now()));
                        newMessage = "";
                    }
                )
            }
        )
        //Icon(Icons.Default.Send, contentDescription = "", modifier = Modifier.size(40.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditMessageSheet(
    destUserId: String,
    messageToEdit: String,
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {
    if (!vm.showEditDialog) return;
    val sheetState = rememberModalBottomSheetState();

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
                    vm.deleteMessage(destUserId, messageToEdit);
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