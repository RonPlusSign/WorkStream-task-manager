package it.polito.workstream.ui.screens.chats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SmallChatBox(
    userName: String,
    lastMessage: String,
    timestamp: LocalDateTime?,
    isGroup: Boolean
) {
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
                modifier = Modifier.padding(start = 5.dp, end = 5.dp)
            ) {
                if (!isGroup)
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(40.dp))
                else
                    Icon(Icons.Default.Groups, contentDescription = "Group Chat", modifier = Modifier.size(40.dp))
            }

            Column (
                modifier = Modifier.width(250.dp)
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
                timestamp?.format(DateTimeFormatter.ofPattern("HH:mm"))?.let { Text(text = it) }
            }
        }
    }
}