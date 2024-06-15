package it.polito.workstream.ui.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.polito.workstream.FilterParams
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class TaskListViewModel(
    activeTeamFlow: Flow<Team?>,
    val activePageValue: MutableStateFlow<String>,
    val setActivePage: (page: String) -> Unit,
    val deleteTask: (task: Task) -> Unit,
    val onTaskCreated: (task: Task) -> Unit,
    val onTaskUpdated: (updatedTask: Task) -> Unit,
    val onAddSection: (section: String) -> Unit,
    val onDeleteSection: (section: String) -> Unit,
    val getTasks: (teamId: String) -> Flow<List<Task>>,
    val currentSortOrder: MutableStateFlow<String>,
    val setSortOrder: (newSortOrder: String) -> Unit,
    filterParamsState: MutableState<FilterParams>,
    val searchQuery: MutableState<String>,
    val setSearchQuery: (newQuery: String) -> Unit
) : ViewModel() {
    val filterParams = filterParamsState.value
    val activeTeam = activeTeamFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun getOfUser(user: String, tasksList: List<Task>): List<Task> {
        var tempTaskList = when (currentSortOrder.value) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy { it.assignee?.firstName + " " + it.assignee?.lastName }
            "Section" -> tasksList.sortedBy { it.section }
            else -> tasksList
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter { it.assignee?.firstName + " " + it.assignee?.lastName == user && it.title.contains(searchQuery.value, ignoreCase = true) }
    }

    private fun customFilter(inputList: List<Task>): List<Task> {
        return inputList.filter {
            (filterParams.section == "" || it.section.contains(filterParams.section, ignoreCase = true))
                    && (filterParams.assignee == "" || (it.assignee?.firstName + " " + it.assignee?.lastName).contains(filterParams.assignee, ignoreCase = true))
                    && (filterParams.status == "" || it.status == filterParams.status) && ((filterParams.completed && it.completed) || (!filterParams.completed && !it.completed))
        }
    }


    var sectionExpanded = mutableStateMapOf(*activeTeam.value?.sections?.map { it to true }?.toTypedArray() ?: arrayOf())
        private set

    var statusList = mutableListOf("To do", "In progress", "Paused", "On review", "Completed")

    fun toggleSectionExpansion(section: String) {
        if (activeTeam.value == null || !activeTeam.value!!.sections.contains(section)) return
        sectionExpanded[section] = !sectionExpanded[section]!!
    }

    fun getAssignees(): List<String> {
        val tasksList = activeTeam.value?.tasks ?: return emptyList()
        return tasksList.map { it.assignee?.firstName + " " + it.assignee?.lastName }.distinct()
    }

    fun addSection(section: String) {
        sectionExpanded[section] = true
        onAddSection(section)
    }

    fun removeSection(section: String) {
        // Check if the section exists and if it is empty
        val tasksList = activeTeam.value?.tasks ?: return
        val sections = activeTeam.value?.sections ?: return

        if (!sections.contains(section)) return // Section does not exist
        if (tasksList.any { it.section == section }) return // If the section is not empty, do not remove it

        onDeleteSection(section)
        sectionExpanded.remove(section)
    }


    fun getOfSection(section: String, sortOrder: String): List<Task> {
        val tasksList = activeTeam.value?.tasks ?: return emptyList()

        var tempTaskList = when (sortOrder) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy { it.assignee?.firstName + " " + it.assignee?.lastName }
            "Section" -> tasksList.sortedBy { it.section }
            else -> tasksList
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter { it.section == section && it.title.contains(searchQuery.value, ignoreCase = true) }
    }


    fun getOfUser(user: String): List<Task> {
        val tasksList = activeTeam.value?.tasks ?: return emptyList()

        var tempTaskList = when (currentSortOrder.value) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy { it.assignee?.firstName + " " + it.assignee?.lastName }
            "Section" -> tasksList.sortedBy { it.section }
            else -> {
                tasksList
            }
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter { it.assignee?.firstName + " " + it.assignee?.lastName == user && it.title.contains(searchQuery.value, ignoreCase = true) }
    }

    fun areThereActiveFilters(): Boolean {

        return if (filterParams.assignee != "") true
        else if (filterParams.section != "") true
        else if (filterParams.status != "") true
        else if (filterParams.completed) true
        else false
    }

    val recurrentList = listOf("None", "Daily", "Weekly", "Monthly")

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
        else if (sections.contains(newSectionValue)) "Section name already exists"
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