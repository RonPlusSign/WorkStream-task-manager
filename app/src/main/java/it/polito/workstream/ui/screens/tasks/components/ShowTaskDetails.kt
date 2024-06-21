package it.polito.workstream.ui.screens.tasks.components

import android.annotation.SuppressLint
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ui.models.Comment
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.TaskListViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("Range")
@Composable
fun ShowTaskDetails(_task: Task, actual_user: User, onComplete: (Task) -> Unit, vm: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))) {
    // Responsive layout: 1 column with 2 rows for vertical screens, 2 columns with 1 row for horizontal screens
    val activeTeamId by vm.activeTeamId.collectAsState()
    val tasks by vm.getTasks(activeTeamId).collectAsState(initial = emptyList())
    val users by vm.fetchUsers(activeTeamId).collectAsState(initial = emptyList())

    val task = tasks.find { it.id == _task.id } ?: Task()
    val comments by vm.fetchComments(task.id).collectAsState(initial = emptyList())
    val assignee = users.find { it.email == task.assignee }
    val context = LocalContext.current
    val takeDocument = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->

        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor = uri?.let { contentResolver.query(it, projection, null, null, null) }

        if (cursor != null && cursor.moveToFirst()) {
            val fileName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
            Log.d("MyApp", "Nome file PDF: $fileName")
            task.attachments.add(fileName)
        } else {
            Log.d("MyApp", "Impossibile recuperare il nome del file PDF dai metadati")
        }

        cursor?.close()
    }
    val (message, setMessage) = remember { mutableStateOf("") }

    val deleteDocument = { attachment: String ->
        task.attachments.remove(attachment)
    }
    val configuration = LocalConfiguration.current
    if (configuration.screenWidthDp > configuration.screenHeightDp) {
        //  horizontal layout
        Row(
            modifier = Modifier.fillMaxSize()

        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .weight(0.5f)
            ) {

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onComplete(task) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Default.Check, contentDescription = "mark as completed",
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(26.dp)
                    )
                    Text(text = "Mark as completed", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = task.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    label = { Text(text = "Assignee") }, value = if (!(assignee?.firstName.isNullOrEmpty() || assignee?.lastName.isNullOrEmpty())) {
                        assignee?.firstName + " " + assignee?.lastName
                    } else {
                        "nobody"
                    }, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(label = { Text("Due Date") }, value = task.dueDate.toDate() ?: "ASAP", onValueChange = {}, readOnly = true, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(label = { Text("Status") }, value = task.status ?: "To do", onValueChange = {}, readOnly = true, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = task.description.ifBlank { "No description" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .weight(0.5f)
            ) {
                Row {
                    Text(text = "Attachments", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .weight(1f)
                            .padding(start = 4.dp, end = 8.dp)
                    )
                    Icon(imageVector = Icons.Default.Add, contentDescription = "", modifier = Modifier.clickable { takeDocument.launch("application/pdf") })
                }
                for (file in task.attachments) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = "attachment", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                            Text(
                                file, modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .weight(1f)
                            )
                            IconButton(onClick = { /*TODO*/ }, modifier = Modifier.width(30.dp)) {
                                Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = "download file", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleteDocument(file) }, modifier = Modifier.width(30.dp)) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "delete file", tint = MaterialTheme.colorScheme.error)
                            }

                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(text = "Comments", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.align(Alignment.CenterVertically))
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    for (c in comments) {
                        Card(
                            modifier = if (c.author != actual_user.getFirstAndLastName() ) Modifier.padding(horizontal = 8.dp, vertical = 3.dp) else Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .align(Alignment.End)
                        ) {
                            Column(modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 8.dp)) {

                                Text(
                                    text = "${c.author}:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Text(text = c.text.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    TextField(
                        value = message,
                        onValueChange = { setMessage(it) },
                        placeholder = { Text("Add a comment...") },
                        label = { Text("Comment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val c = Comment()
                                    c.taskId = task.id
                                    c.text = message
                                    c.author = actual_user.getFirstAndLastName()
                                    task.comments.add(c)
                                    vm.uploadComment(c)
                                    setMessage("")
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send comment") }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(text = "History", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.align(Alignment.CenterVertically))
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    ) {
                        if (task.history.isEmpty())
                            Text("No updates yet", modifier = Modifier.align(Alignment.CenterHorizontally))

                        for ((timestamp, entry) in task.history) {
                            Text(text = "[${timestamp.toDate()?.format("dd/MM/yy HH:mm")}] $entry", style = MaterialTheme.typography.bodyLarge)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    } else {
        WorkStreamTheme {
            // vertical layout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onComplete(task) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Default.Check, contentDescription = "mark as completed",
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(26.dp)
                    )
                    Text(text = "Mark as completed", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = task.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    label = { Text(text = "Assignee") }, value = if (!(assignee?.firstName.isNullOrEmpty() || assignee?.lastName.isNullOrEmpty())) {
                        assignee?.firstName + " " + assignee?.lastName
                    } else {
                        "nobody"
                    }, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(label = { Text("Due Date") }, value = task.dueDate.toDate() ?: "ASAP", onValueChange = {}, readOnly = true, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(label = { Text("Status") }, value = task.status ?: "To do", onValueChange = {}, readOnly = true, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = task.description.ifBlank { "No description" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(text = "Attachments", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .weight(1f)
                            .padding(start = 4.dp, end = 8.dp)
                    )
                    Icon(imageVector = Icons.Default.Add, contentDescription = "", modifier = Modifier.clickable { takeDocument.launch("application/pdf") })
                }
                for (file in task.attachments) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = "attachment", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                            Text(
                                file, modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .weight(1f)
                            )
                            IconButton(onClick = { /*TODO*/ }, modifier = Modifier.width(30.dp)) {
                                Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = "download file", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleteDocument(file) }, modifier = Modifier.width(30.dp)) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "delete file", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(text = "Comments", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.align(Alignment.CenterVertically))
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    for (c in comments.sortedBy { it.timestamp }) {
                        Card(
                            modifier = if (c.author != actual_user.getFirstAndLastName() ) Modifier.padding(horizontal = 8.dp, vertical = 3.dp) else Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .align(Alignment.End)
                        ) {
                            Column(modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 8.dp)) {

                                Text(
                                    text = "${c.author}:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Text(text = c.text.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    TextField(
                        value = message,
                        onValueChange = { setMessage(it) },
                        placeholder = { Text("Add a comment...") },
                        label = { Text("Comment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val c = Comment()
                                    c.taskId = task.id
                                    c.text = message
                                    c.author = actual_user.getFirstAndLastName()
                                    task.comments.add(c)
                                    vm.uploadComment(c)
                                    setMessage("")
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send comment") }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(text = "History", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.align(Alignment.CenterVertically))
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    ) {
                        if (task.history.isEmpty())
                            Text("No updates yet", modifier = Modifier.align(Alignment.CenterHorizontally))

                        for ((timestamp, entry) in task.history) {
                            Text(text = "[${timestamp.toDate()?.format("dd/MM/yy HH:mm")}] $entry", style = MaterialTheme.typography.bodyLarge)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

fun Timestamp?.toDate(): String? {
    if (this == null)
        return null
    val date = Date(this.time)
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return sdf.format(date)
}