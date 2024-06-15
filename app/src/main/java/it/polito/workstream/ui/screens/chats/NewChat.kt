package it.polito.workstream.ui.screens.chats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

@Composable
fun NewChat(
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    onChatClick: (route: Int, taskId: String?, taskName: String?, userId: Long?) -> Unit
) {
    val users = vm.getUsers()

    Column {
        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            users.forEach { user ->
                item {
                    Card (
                        modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth()
                            .padding(top = 5.dp, bottom = 5.dp)
                            .clickable {
                               if(vm.chats.value[user] != null)
                                   onChatClick(8, null, null, 0, /*user.id*/)   // TODO: Uccidimi
                               else {
                                   vm.newChat(user)
                                   onChatClick(8, null, null, 0 /* user.id */)  // TODO: UCCIDIMI
                               }
                            },
                        border = BorderStroke(0.5.dp, Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        elevation = CardDefaults.elevatedCardElevation(8.dp),
                    ) {
                        Row (
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentHeight(Alignment.CenterVertically)
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Chat", modifier = Modifier.size(40.dp))
                            Text(
                                text = user.firstName + " " + user.lastName,
                                fontSize = 25.sp, modifier =
                                Modifier.padding(start = 8.dp, bottom = 5.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}