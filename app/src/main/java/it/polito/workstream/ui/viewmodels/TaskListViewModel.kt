package it.polito.workstream.ui.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import it.polito.workstream.FilterParams
import it.polito.workstream.ui.models.Comment
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class TaskListViewModel(
    activeTeamFlow: Flow<Team?>,
    activeTeamMembers: Flow<List<User>>,
    val activePageValue: MutableStateFlow<String>,
    val setActivePage: (page: String) -> Unit,
    val deleteTask: (task: Task) -> Unit,
    val onTaskCreated: (task: Task) -> Unit,
    val onTaskUpdated: (updatedTask: Task) -> Unit,
    val onAddSection: (section: String) -> Unit,
    val onDeleteSection: (section: String) -> Unit,
    val tasksFlow: Flow<List<Task>>,
    val currentSortOrder: MutableStateFlow<String>,
    val setSortOrder: (newSortOrder: String) -> Unit,
    filterParamsState: MutableState<FilterParams>,
    val searchQuery: MutableState<String>,
    val setSearchQuery: (newQuery: String) -> Unit,
    val activeTeamId: MutableStateFlow<String>,
    val getTasks: (String) -> Flow<List<Task>>,
    val fetchSections: (String) -> Flow<List<String>>,
    val fetchActiveTeam: (String) -> Flow<Team?>,
    val fetchUsers: (String) -> Flow<List<User>>,
    val fetchComments: (String) -> Flow<List<Comment>>,
    val uploadComment: (Comment) -> Unit,
    val uploadDocument: (documentPath: String, taskId: String) -> Unit,
    val deleteDocument: (String, String) -> Unit,
) : ViewModel() {
    val filterParams = filterParamsState.value

    val activeTeam = fetchActiveTeam(activeTeamId.value).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val teamMembers = activeTeamMembers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val tasks = getTasks(activeTeamId.value).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList()) //tasksFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val sections = fetchSections(activeTeamId.value).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())


    fun getOfUser(userId: String, tasksList: List<Task>): List<Task> {

        var tempTaskList = when (currentSortOrder.value) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy {
                val assignee = teamMembers.value?.find { userId == it.email }
                assignee?.getFirstAndLastName()
            }

            "Section" -> tasksList.sortedBy { it.section }
            else -> tasksList
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter { it.assignee == userId && it.title.contains(searchQuery.value, ignoreCase = true) }
    }


    private fun customFilter(inputList: List<Task>): List<Task> {
        return inputList.filter {
            (filterParams.section == "" || it.section.contains(filterParams.section, ignoreCase = true))
                    && (filterParams.assignee == "" || it.assignee == filterParams.assignee)
                    && (filterParams.recurrent == "" || (filterParams.recurrent.isNotEmpty() == it.recurrent))
                    && (filterParams.status == "" || it.status == filterParams.status) && ((filterParams.completed && it.completed) || (!filterParams.completed && !it.completed))
        }
    }


    var sectionExpanded: SnapshotStateMap<String, Boolean> = mutableStateMapOf()//mutableStateMapOf(*activeTeam.value?.sections?.map { it to true }?.toTypedArray() ?: arrayOf())


    var statusList = mutableListOf("To Do", "In progress", "Paused", "On review", "Completed")

    fun toggleSectionExpansion(section: String) {
        //if (activeTeam.value == null || !activeTeam.value!!.sections.contains(section)) return
        sectionExpanded[section] = !sectionExpanded[section]!!

    }

    fun getAssignees(): List<String> {
        val tasksList = activeTeam.value?.tasks ?: return emptyList()
        return tasksList.map {
            val assignee = teamMembers.value?.find { user -> user.email == it.assignee }
            assignee?.getFirstAndLastName() ?: ""
        }.distinct()
    }

    private fun addSection(section: String) {
        sectionExpanded[section] = true
        onAddSection(section)
    }

    fun removeSection(section: String) {
        onDeleteSection(section)
        sectionExpanded.remove(section)
    }

    fun initSectionExpanded(m: Map<String, Boolean>) {
        for (k in m.entries) {
            sectionExpanded[k.key] = k.value
        }
    }


    fun getOfSection(section: String, sortOrder: String): List<Task> {
        val tasksList = tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList()).value //Questo non funziona

        var tempTaskList = when (sortOrder) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy {
                val assignee = teamMembers.value?.find { user -> user.email == it.assignee }
                assignee?.getFirstAndLastName()
            }

            "Section" -> tasksList.sortedBy { it.section }
            else -> tasksList
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter { it.section == section && it.title.contains(searchQuery.value, ignoreCase = true) }
    }

    fun getOfSectionByList(section: String, sortOrder: String, tasksList: List<Task>): List<Task> {


        var tempTaskList = when (sortOrder) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy {
                val assignee = teamMembers.value?.find { user -> user.email == it.assignee }
                assignee?.getFirstAndLastName()
            }

            "Section" -> tasksList.sortedBy { it.section }
            else -> tasksList
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter { it.section == section && it.title.contains(searchQuery.value, ignoreCase = true) }
    }


    fun getOfUser(userId: String): List<Task> {
        val tasksList = activeTeam.value?.tasks ?: return emptyList()

        var tempTaskList = when (currentSortOrder.value) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy {
                val assignee = teamMembers.value?.find { userId == it.email }
                assignee?.getFirstAndLastName()
            }

            "Section" -> tasksList.sortedBy { it.section }
            else -> {
                tasksList
            }
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter { it.assignee == userId && it.title.contains(searchQuery.value, ignoreCase = true) }
    }

    fun areThereActiveFilters(): Boolean {

        return if (filterParams.assignee != "") true
        else if (filterParams.section != "") true
        else if (filterParams.status != "") true
        else if (filterParams.completed) true
        else false
    }

    val recurrentList = listOf("Daily", "Weekly", "Monthly")

    // SORT VARIABLES
    val allSortOrders = listOf("A-Z order", "Z-A order", "Due date", "Assignee", "Section")

    var isAddingSection by mutableStateOf(false)
        private set

    var newSectionValue by mutableStateOf("")
        private set
    var newSectionError by mutableStateOf("")
        private set

    fun setNewSection(value: String) {
        newSectionValue = value

    }

    fun toggleAddSection() {
        isAddingSection = !isAddingSection
        newSectionValue = ""
        newSectionError = ""
    }

    fun validateSection() {
        val sections = activeTeam.value?.sections ?: emptyList()

        newSectionValue = newSectionValue.trim()

        // The section name must be unique and not blank
        newSectionError = if (newSectionValue.isBlank()) "Section name cannot be blank"
        else ""

        if (newSectionError.isBlank()) {
            addSection(newSectionValue)
            newSectionValue = ""
            newSectionError = ""
            isAddingSection = false
        }
    }

    var showFilterDialogValue by mutableStateOf(false)
    var showSortDialogValue by mutableStateOf(false)

    fun toggleShowFilterDialog() {
        showFilterDialogValue = !showFilterDialogValue
    }

    fun toggleShowSortDialog() {
        showSortDialogValue = !showSortDialogValue
    }


}