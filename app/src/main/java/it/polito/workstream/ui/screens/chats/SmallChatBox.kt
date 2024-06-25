package it.polito.workstream.ui.screens.chats

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.Timestamp
import it.polito.workstream.Route
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun SmallChatBox(
    destUser: User?,
    userName: String,
    lastMessage: String,
    timestamp: Timestamp?,
    isGroup: Boolean,
    activeTeam: Team?,
    unseenMessages: Int
) {
    val today = LocalDate.now()

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(0.5.dp, Color.Black),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.elevatedCardElevation(8.dp),
    ) {
        Row (
            //horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(5.dp)
                .height(55.dp)
        ) {
            Column (
                modifier = Modifier.padding(start = 5.dp, end = 5.dp).align(Alignment.CenterVertically)
            ) {
                if (isGroup) {
                    if (activeTeam != null) {
                        AsyncImage(
                            model =
                            ImageRequest.Builder(LocalContext.current)
                                .data(LocalContext.current.getFileStreamPath(activeTeam.photo).absolutePath)
                                .crossfade(true)
                                .build(), //minchia ci siamo
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Icon(Icons.Default.Groups, contentDescription = "Group Chat", modifier = Modifier.size(40.dp))
                        //Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(40.dp))
                    }
                }
                else {
                    if (destUser != null && destUser.photo.isNotEmpty())
                        AsyncImage(
                            model =
                            ImageRequest.Builder(LocalContext.current)
                                .data(LocalContext.current.getFileStreamPath(destUser.photo).absolutePath)
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
                                text = "${destUser?.firstName} ${destUser?.lastName}".trim().split(" ").map { it.first().uppercaseChar() }.joinToString("").take(2),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                        }
                }
            }

            Column (
                modifier = Modifier.width(240.dp)
            ) {
                Text(text = userName, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = lastMessage, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(
                Modifier
                    .weight(1f)
                    .fillMaxHeight())

            Column (
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 5.dp)
            ) {
                if (timestamp != null) {
                    if (Instant.ofEpochSecond(timestamp.seconds).atZone(ZoneId.systemDefault()).toLocalDate() == today)
                        Text(text = DateTimeFormatter.ofPattern("HH:mm").format(timestamp.toDate().toInstant().atZone(
                            ZoneId.systemDefault()).toLocalDateTime()))
                    else
                        Text(text = DateTimeFormatter.ofPattern("dd/MM").format(timestamp.toDate().toInstant().atZone(
                            ZoneId.systemDefault()).toLocalDateTime()))
                }
                if (unseenMessages > 0)
                    Box (
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    ) {
                        Text(text = unseenMessages.toString(), fontSize = 20.sp, color = Color.White)
                    }
            }
        }
    }
}