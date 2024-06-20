package it.polito.workstream.ui.screens.tasks

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.tasks.components.SmallTaskBox
import it.polito.workstream.ui.viewmodels.TaskListViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TeamTasksScreen(

    _users: StateFlow<List<User>>,
    getOfSection: (String, String) -> List<Task>,

    newSectionValue: String,
    toggleSectionExpansion: (String) -> Unit,
    isAddingSection: Boolean,
    deleteSection: (String) -> Unit,
    setNewSection: (String) -> Unit,
    newSectionError: String,
    onTaskClick: (route: Int, taskId: String?, taskName: String?, userId: Long?, userMail: String?) -> Unit,
    toggleAddSection: () -> Unit,
    validateSection: () -> Unit,
    currentSortOrder: MutableStateFlow<String>,
    vm: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {

    val activeteam = vm.activeTeam.collectAsState()
    val activeTeamId by vm.activeTeamId.collectAsState()
    Log.d("TeamTasksScreen", "${ activeteam.value}")
    //var sectionExpanded = mutableMapOf(*activeteam.value?.sections?.map { it to true }?.toTypedArray() ?: arrayOf())
    vm.initSectionExpanded(mutableMapOf(*activeteam.value?.sections?.map { it to true }?.toTypedArray() ?: arrayOf()))
    //val sectionExpanded = vm.sectionExpanded
    val users by vm.fetchUsers(activeTeamId).collectAsState(initial = emptyList())
    val taskList = vm.tasks.collectAsState(initial = emptyList()).value
    val sections by vm.sections.collectAsState(listOf())

    val isDeletingSection: MutableMap<String, Boolean> = remember { mutableStateMapOf( *sections.map { it to false }.toTypedArray() ) }


    val sortOrder by currentSortOrder.collectAsState()
    val sectionToCancel by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        // To calculate the bottom padding
        val fabHeight by remember { mutableIntStateOf(0) }
        val heightInDp = with(LocalDensity.current) { fabHeight.toDp() }

        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Add new task") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Task") },
                    onClick = {
                        //onAddTask("General")
                        onTaskClick(3, null, null, null, null)
                    },

                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(40.dp)
                )
            },
            floatingActionButtonPosition = FabPosition.Center,
            modifier = Modifier.fillMaxSize(),
            content = { padding ->
                LazyColumn(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    contentPadding = PaddingValues(bottom = heightInDp + 55.dp)
                ) {
                    for (section in sections ) {
                        Log.d("Section" , section )
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.background),
                                elevation = CardDefaults.elevatedCardElevation(0.dp),
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 5.dp)
                                            .clickable { vm.toggleSectionExpansion(section) }) {
                                        Text(
                                            text = section,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                        )

                                        HorizontalDivider(
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .padding(horizontal = 8.dp)
                                                .weight(1f)
                                        )

                                        Icon(
                                            if (vm.sectionExpanded[section] == true) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand/Collapse",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .padding(end = 0.dp)
                                                .size(24.dp)
                                        )


                                        // Button to delete a section
                                        if (vm.getOfSectionByList(section, sortOrder, taskList ).isEmpty() && section != "General") {
                                            IconButton(onClick = { isDeletingSection[section] = true }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Section")
                                            }
                                        }

                                        // If deleting a section, show a Dialog to confirm the deletion
                                        if (isDeletingSection[section] == true) {
                                            Dialog(onDismissRequest = { isDeletingSection[section] = false }) {
                                                Card(modifier = Modifier.height(220.dp), shape = RoundedCornerShape(16.dp)) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(20.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                    ) {

                                                        Text("Delete Section", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                                        // Confirmation message
                                                        Text("Are you sure you want to delete this section?")

                                                        Spacer(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .weight(1f)
                                                        )

                                                        // Buttons to cancel/confirm
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            OutlinedButton(onClick = { isDeletingSection[section] = false }) { Text("Cancel") }
                                                            Button(onClick = {
                                                                deleteSection(section);
                                                                isDeletingSection[section] = false }) { Text("Delete") }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Tasks of the section
                                    if ( vm.sectionExpanded[section] == true) {//sectionExpanded[section] == true
                                        vm.getOfSectionByList(section, sortOrder, taskList ).forEach { task ->
                                            val assignee = users.find { it.email == task.assignee }
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier.clickable { onTaskClick(1, task.id, task.title, null, null) } // navigation
                                            ) {
                                                SmallTaskBox(title = task.title,
                                                    task = task,
                                                    assignee = (assignee?.getFirstAndLastName() ?: ""),
                                                    section = null,
                                                    dueDate = task.dueDate,
                                                    onEditClick = { onTaskClick(4, task.id, task.title, null, null) }
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Button to add a new Section
                    item {
                        OutlinedButton(onClick = toggleAddSection, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = "Add Section")
                            Text(text = "New Section")
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // If adding a new section, show a Dialog to input the new section name
                        if (isAddingSection) {
                            Dialog(onDismissRequest = toggleAddSection) {
                                Card(
                                    modifier = Modifier.height(220.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {

                                        Text("New Section", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                        // Input field for the new section name
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            OutlinedTextField(
                                                value = newSectionValue,
                                                label = { Text("Section name") },
                                                shape = RoundedCornerShape(8.dp),
                                                onValueChange = setNewSection,
                                                isError = newSectionError.isNotEmpty(),
                                            )
                                            if (newSectionError.isNotEmpty()) {
                                                Text(newSectionError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }

                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .weight(1f)
                                        )

                                        // Buttons to cancel/confirm
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            OutlinedButton(onClick = toggleAddSection) { Text("Cancel") }
                                            Button(onClick = validateSection) { Text("Add") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun TeamTaskScreenWrapper(vm: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)), onItemSelect: (route: Int, taskId: String?, taskName: String?, userId: Long?, userMail: String?) -> Unit) {
    TeamTasksScreen(
        //_sections = vm.sections ,
        _users = vm.teamMembers,
        getOfSection = vm::getOfSection,

        newSectionValue = vm.newSectionValue,
        toggleSectionExpansion = vm::toggleSectionExpansion,
        isAddingSection = vm.isAddingSection,
        setNewSection = vm::setNewSection,
        newSectionError = vm.newSectionError,
        onTaskClick = onItemSelect,
        toggleAddSection = vm::toggleAddSection,
        validateSection = vm::validateSection,
        deleteSection = vm::removeSection,
        currentSortOrder = vm.currentSortOrder
    )
}