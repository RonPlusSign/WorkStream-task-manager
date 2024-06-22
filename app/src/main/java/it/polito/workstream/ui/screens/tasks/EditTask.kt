package it.polito.workstream.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.tasks.components.isExpired
import it.polito.workstream.ui.viewmodels.TaskListViewModel
import it.polito.workstream.ui.viewmodels.TaskViewModel
import it.polito.workstream.ui.viewmodels.TeamViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Composable function to edit a task.
 *
 * @param saveTask callback function when a task is saved
 * @param changeRoute callback function to change the route
 * @param taskVM view model for the task
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    changeRoute: (route: Int, taskId: String?, taskName: String?, userId: Long?, userMail: String?) -> Unit,
    taskVM: TaskViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    taskListVM: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    teamVM: TeamViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    saveTask: (Task) -> Unit = taskListVM::onTaskUpdated.get(),
) {
    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)

    val assignee: User? = teamVM.teamMembers.collectAsState(initial = emptyList()).value.find { it.email == taskVM.assigneeValue }
    val sections by taskListVM.sections.collectAsState(listOf())
    val activeTeamId by taskListVM.activeTeamId.collectAsState()
    val tasks by taskListVM.getTasks(activeTeamId).collectAsState(initial = emptyList())
    val task = tasks.firstOrNull { it.id == taskVM.task.value.id } ?: taskVM.task.value


    taskVM.taskBeforeEditing = task.copy()

    @Composable
    fun EditTaskInfo() {
        Column {
            // Title field
            OutlinedTextField(
                value = taskVM.titleValue,
                onValueChange = taskVM::setTitle,
                label = { Text("Title") },
                isError = taskVM.titleError.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (taskVM.titleError.isNotBlank())
                        Icon(Icons.Outlined.Cancel, contentDescription = "clear", modifier = Modifier
                            .size(20.dp)
                            .clickable { taskVM.setTitle("") })
                }
            )
            // Error message for title field
            if (taskVM.titleError.isNotBlank())
                Text(text = taskVM.titleError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Assignee
            ExposedDropdownMenuBox(
                expanded = taskVM.expandedUser,
                onExpandedChange = { taskVM.toggleUserExpanded() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = assignee?.getFirstAndLastName() ?: "Anyone",
                    onValueChange = {},
                    label = { Text("Assignee") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "choose status") }
                )
                ExposedDropdownMenu( // Dropdown menu for User selection
                    expanded = taskVM.expandedUser,
                    onDismissRequest = taskVM::toggleUserExpanded,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                ) {
                    teamVM.teamMembers.collectAsState(initial = emptyList()).value.forEach() { m ->
                        DropdownMenuItem(text = { Text(text = m.firstName + " " + m.lastName) }, onClick = { taskVM.setAssignee(m); taskVM.toggleUserExpanded() })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Due Date
            Column(modifier = Modifier.clickable { taskVM.toggleDatePickerOpen() }) {
                OutlinedTextField(
                    value = formatTimestamp(taskVM.dueDateValue) ?: "",
                    onValueChange = {},
                    label = {
                        Text("Due Date")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (taskVM.dueDateValue.isExpired()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface
                    ),
                    trailingIcon = {
                        if (taskVM.dueDateValue != null)
                            Icon(Icons.Outlined.Cancel, contentDescription = "clear", modifier = Modifier
                                .size(20.dp)
                                .clickable { taskVM.setDueDate(null) })
                    },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "assignee") },
                    isError = taskVM.dueDateError.isNotBlank()
                )
                // Error message for due date field
                if (taskVM.dueDateError.isNotBlank())
                    Text(text = taskVM.dueDateError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Status
                Column(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = taskVM.expandedStatus,
                        onExpandedChange = { taskVM.toggleStatusExpanded() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = taskVM.statusValue.value ?: "",
                            onValueChange = {},
                            label = { Text("Status") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "choose status") }
                        )
                        ExposedDropdownMenu( // Dropdown menu for status selection
                            expanded = taskVM.expandedStatus,
                            onDismissRequest = taskVM::toggleStatusExpanded,
                            modifier = Modifier.wrapContentSize(Alignment.Center)
                        ) {
                            taskVM.statuses.forEach { s ->
                                DropdownMenuItem(text = { Text(text = s) }, onClick = { taskVM.setStatus(s); taskVM.toggleStatusExpanded() })
                            }
                        }
                    }
                }

                // Section
                Column(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = taskVM.expandedSection,
                        onExpandedChange = { taskVM.toggleSectionExpanded() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = taskVM.sectionValue,
                            onValueChange = {},
                            label = { Text("Section") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "choose section") }
                        )

                        ExposedDropdownMenu( // Dropdown menu for section selection
                            expanded = taskVM.expandedSection,
                            onDismissRequest = taskVM::toggleSectionExpanded,
                            modifier = Modifier.wrapContentSize(Alignment.Center)
                        ) {

                            sections.forEach { sectionItem ->//QUI
                                DropdownMenuItem(text = { Text(text = sectionItem) }, onClick = { taskVM.setSection(sectionItem); taskVM.toggleSectionExpanded() })
                            }
                        }
                        // Error message for section field
                        if (taskVM.sectionError.isNotBlank())
                            Text(text = taskVM.sectionError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Description
            OutlinedTextField(
                value = taskVM.descriptionValue,
                onValueChange = taskVM::setDescription,
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth(),
                trailingIcon = {
                    if (taskVM.descriptionValue.isNotBlank())
                        Icon(Icons.Outlined.Cancel, contentDescription = "clear", modifier = Modifier
                            .size(20.dp)
                            .clickable { taskVM.setDescription("") })
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Recurrent toggle and Frequency selection button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    // Recurrent toggle
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text(text = "Recurrent: ", modifier = Modifier.padding(end = 1.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Checkbox(checked = taskVM.isRecurrentValue, onCheckedChange = { taskVM.setRecurrent(it); if (!it) taskVM.setFrequency(null) })
                    }
                }

                // Frequency selection
                Column(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = taskVM.expandedFrequency,
                        onExpandedChange = { if (taskVM.isRecurrentValue) taskVM.toggleFrequencyExpanded() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = taskVM.frequencyValue ?: "Don't repeat",
                            onValueChange = {},
                            label = { Text("Frequency") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            enabled = taskVM.isRecurrentValue,
                            trailingIcon = { Icon(Icons.Default.AvTimer, contentDescription = "choose frequency") }
                        )
                        ExposedDropdownMenu(    // Dropdown menus for frequency selection
                            expanded = taskVM.expandedFrequency,
                            onDismissRequest = taskVM::toggleFrequencyExpanded,
                            modifier = Modifier.wrapContentSize(Alignment.Center)
                        ) {
                            taskVM.frequencies.forEach { frequencyItem ->
                                DropdownMenuItem(
                                    text = { Text(text = frequencyItem) },
                                    onClick = {
                                        if (frequencyItem != "None") taskVM.setFrequency(frequencyItem)
                                        else taskVM.setFrequency(null)
                                        taskVM.toggleFrequencyExpanded()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DeleteButton() {
        OutlinedButton(
            onClick = {
                taskListVM.deleteTask(taskVM.task.value)
                changeRoute(1, null, null, null, null)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors().copy(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Delete Task")
            Icon(
                Icons.Default.DeleteOutline, contentDescription = "delete task", modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(20.dp)
            )
        }
    }

    @Composable
    fun SaveButton() {
        Button(
            onClick = {
                if (taskVM.isTaskValid()) {
                    saveTask(taskVM.save())
                    changeRoute(1, null, null, null, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors()
        ) {
            Text("Save")
            Icon(
                Icons.Default.Save,
                contentDescription = "save updates",
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(20.dp)
            )
        }
    }

    val configuration = LocalConfiguration.current
    if (configuration.screenWidthDp > configuration.screenHeightDp) { // Horizontal layout
        Row(modifier = Modifier.padding(16.dp)) {
            // First column, occupies 75% of the space
            LazyColumn(Modifier.weight(3f)) {
                item { EditTaskInfo() }
            }
            // Spacer between columns
            Spacer(modifier = Modifier.width(16.dp))

            // Second column, occupies 25% of the space
            Column(Modifier.weight(1f)) {
                // Vertical scrolling for second column content
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DeleteButton()
                    Spacer(modifier = Modifier.height(8.dp))
                    SaveButton()
                }
            }
        }
    } else {    // Vertical layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            EditTaskInfo()
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) { DeleteButton() }
                Column(modifier = Modifier.weight(1f)) { SaveButton() }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Date picker dialog
    if (taskVM.isDatePickerOpen) {
        DatePickerDialog(
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            onDismissRequest = taskVM::toggleDatePickerOpen,
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.convertMillisToDate()
                    selectedDate?.let { convertDateToTimestamp(it) }?.let { taskVM.setDueDate(it) }
                    taskVM.toggleDatePickerOpen()
                }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = { TextButton(onClick = taskVM::toggleDatePickerOpen) { Text("CANCEL", color = MaterialTheme.colorScheme.primary) } }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    }
}


/**
 * Function to convert milliseconds to formatted date string.
 * @return formatted date string (dd/MM/yyyy)
 */
fun Long.convertMillisToDate(): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = this@convertMillisToDate
        val zoneOffset = get(Calendar.ZONE_OFFSET)
        val dstOffset = get(Calendar.DST_OFFSET)
        add(Calendar.MILLISECOND, -(zoneOffset + dstOffset))
    }
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.ITALIAN)
    return sdf.format(calendar.time)
}

/**
 * Function to convert date string (MMM dd, yyyy) to Timestamp.
 * @param dateString date string in MMM dd, yyyy format
 * @return Timestamp object
 */
fun convertDateToTimestamp(dateString: String): Timestamp {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.ITALIAN)
    val date = sdf.parse(dateString)
    return Timestamp(requireNotNull(date).time)
}

/**
 * Function to format Timestamp to formatted date string (dd/MM/yyyy).
 * @param timestamp Timestamp object
 * @return formatted date string (dd/MM/yyyy)
 */
fun formatTimestamp(timestamp: Timestamp?): String? {
    if (timestamp == null) {
        return null
    }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
    return sdf.format(timestamp)
}