package it.polito.workstream.ui.screens.tasks.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.polito.workstream.ui.models.Task
import java.sql.Timestamp

@Composable
fun SmallTaskBox(
    title: String,
    assignee: String?,
    section: String?,
    dueDate: Timestamp?,
    task: Task,
    onEditClick: (Task) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(0.5.dp, Color.Black),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.elevatedCardElevation(8.dp),
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(5.dp)
                    .weight(0.2f)
            ) {
                Icon(
                    if (task.completed) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )
            }
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(5.dp)
                    .weight(1f)
            ) {
                Row {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (assignee != null) {
                        Text("$assignee ")
                    } else if (section != null) {
                        Text("$section ")
                    }
                    Spacer(Modifier.weight(1f))
                    if (dueDate != null) {
                        Text(dueDate.toDate().toString(), color = if (dueDate.isExpired()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Column(modifier = Modifier.weight(0.2f)) {
                IconButton(
                    onClick = { onEditClick(task) },
                    modifier = Modifier
                        .padding(5.dp)
                        .size(50.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    }
}