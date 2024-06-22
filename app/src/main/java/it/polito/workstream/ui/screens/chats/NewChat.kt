package it.polito.workstream.ui.screens.chats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

@Composable
fun NewChat(
    vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    onChatClick: (route: Int, taskId: String?, taskName: String?, userId: Long?, userMail: String?) -> Unit
) {
    val chats = vm.chats.collectAsState(initial = listOf()).value
    val activeTeam = vm.activeTeam.collectAsState(initial = null).value
    val teamMembers = vm.teamMembers.collectAsState(initial = listOf()).value

    Column {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val (matching, notMatching) = teamMembers.filter { it.email != vm.user.email }
                .partition { user ->
                    val chat = chats.find { it.user1Id == user.email || it.user2Id == user.email }
                    if (chat != null)
                        chat.messages.size > 0
                    else false
                }

            item { Text(text = "New chats", fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(bottom = 8.dp)) }

            notMatching.forEach { user ->
                item {
                    Card(
                        modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth()
                            .padding(top = 5.dp, bottom = 5.dp)
                            .clickable {
                                if (chats.find { it.user1Id == user.email || it.user2Id == user.email } != null)
                                    onChatClick(8, null, null, null, user.email)
                                else {
                                    vm.newChat(user.email)
                                    onChatClick(8, null, null, null, user.email)
                                }
                            },
                        border = BorderStroke(0.5.dp, Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        elevation = CardDefaults.elevatedCardElevation(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentHeight(Alignment.CenterVertically)
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            //Icon(Icons.Default.Person, contentDescription = "Chat", modifier = Modifier.size(40.dp))
                            if (user.photo.isNotEmpty())
                                AsyncImage(
                                    model =
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(LocalContext.current.getFileStreamPath(user.photo).absolutePath)
                                        .crossfade(true)
                                        .build(), //minchia ci siamo
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            else
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${user?.firstName} ${user?.lastName}".trim().split(" ").map { it.first().uppercaseChar() }.joinToString("").take(2),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp
                                    )
                                }
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

            item { HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) }
            // Private chats
            item { Text(text = "Existing chats", fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(bottom = 8.dp)) }

            matching.forEach { user ->
                item {
                    Card(
                        modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth()
                            .padding(top = 5.dp, bottom = 5.dp)
                            .clickable {
                                if (chats.find { it.user1Id == user.email || it.user2Id == user.email } != null)
                                    onChatClick(8, null, null, null, user.email)
                                else {
                                    vm.newChat(user.email)
                                    onChatClick(8, null, null, null, user.email)
                                }
                            },
                        border = BorderStroke(0.5.dp, Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        elevation = CardDefaults.elevatedCardElevation(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentHeight(Alignment.CenterVertically)
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            //Icon(Icons.Default.Person, contentDescription = "Chat", modifier = Modifier.size(40.dp))
                            if (user.photo.isNotEmpty())
                                AsyncImage(
                                    model =
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(LocalContext.current.getFileStreamPath(user.photo).absolutePath)
                                        .crossfade(true)
                                        .build(), //minchia ci siamo
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            else
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${user?.firstName} ${user?.lastName}".trim().split(" ").map { it.first().uppercaseChar() }.joinToString("").take(2),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp
                                    )
                                }
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