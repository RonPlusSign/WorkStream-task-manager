package it.polito.workstream.ui.viewmodels

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import it.polito.workstream.FilterParams
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TaskListViewModel(
    val activeTeam: Flow<Team?>,
    val onTaskUpdated: (updatedTask: Task) -> Unit,
    val deleteTask: (task: Task) -> Unit,
    val onTaskCreated: (task1: Task) -> Unit,
    val getTasks: (teamId: String) -> Flow<List<Task>>,
    val currentSortOrder: MutableStateFlow<String>,
    val setSortOrder: (newSortOrder: String) -> Unit,
    filterParams: MutableState<FilterParams>,
    val searchQuery: MutableState<String>,
    setSearchQuery: (newQuery: String) -> Unit
)
    : ViewModel() {
    val filterParams = filterParams.value

    fun getOfUser(user: String, tasksList: List<Task> ): List<Task> {
        var tempTaskList = when (currentSortOrder.value) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy { it.assignee?.firstName +" "+ it.assignee?.lastName }
            "Section" -> tasksList.sortedBy { it.section }
            else -> {
                tasksList
            }
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter {  it.assignee?.firstName +" "+ it.assignee?.lastName == user && it.title.contains(searchQuery.value, ignoreCase = true) }
    }
    private fun customFilter(inputList: List<Task>): List<Task> {
        return inputList.filter {
            (filterParams.section == "" || it.section.contains(filterParams.section, ignoreCase = true))
                    && (filterParams.assignee == "" || (it.assignee?.firstName +" "+ it.assignee?.lastName).contains(filterParams.assignee, ignoreCase = true))
                    && (filterParams.status == "" || it.status == filterParams.status) && ((filterParams.completed && it.completed) || (!filterParams.completed && !it.completed))
        }


}
/*
class TaskListViewModel(
    _tasksList: MutableStateFlow<MutableList<Task>>,
    val sections: SnapshotStateList<String>,
    val activePageValue: MutableStateFlow<String>,
    val setActivePage: (page: String) -> Unit,
    val currentSortOrder: MutableStateFlow<String>,
    val setSortOrder: (newSortOrder: String) -> Unit,
    filterParams: MutableState<FilterParams>,
    val searchQuery: MutableState<String>,
    val setSearchQuery: (newQuery: String) -> Unit,
) : ViewModel() {
    val tasksList = _tasksList.value
    val filterParams = filterParams.value

    var sectionExpanded = mutableStateMapOf(*sections.map { it to true }.toTypedArray())
        private set

    var statusList = mutableListOf("To do", "In progress", "Paused", "On review", "Completed")

    fun toggleSectionExpansion(section: String) {
        if (!sections.contains(section)) return
        sectionExpanded[section] = !sectionExpanded[section]!!
    }

    fun getAssignees(): List<String> {
        return tasksList.map { it.assignee?.firstName +" "+ it.assignee?.lastName }.distinct()
    }

    /*var activePageValue by mutableStateOf(Route.TeamTasks.name)
        private set

    fun setActivePage(page: String) {
        activePageValue = page
    }*/

    fun addTask(task: Task) = tasksList.add(task)

    fun addSection(section: String) {
        sectionExpanded.put(section, true)
        sections.add(section)
    }

    fun removeSection(section: String) {
        if (!sections.contains(section)) return // Section does not exist
        if (tasksList.any { it.section == section }) return // If the section is not empty, do not remove it

        sections.remove(section)
        sectionExpanded.remove(section)
    }

    fun onTaskCreated(task1: Task) {
        val task= task1.copy()
        //task.addHistoryEntry("Task created")
        task.team?.tasks?.add(task)
        task.assignee?.tasks?.add(task)
        addTask(task)
    }

    fun deleteTask(task: Task) {
        tasksList.remove(task)

//        val sectionToRemove = task.section
//        if (tasksList.count { it.section == sectionToRemove } == 0 && sectionToRemove != "General")
//            removeSection(sectionToRemove)
    }

    fun onTaskUpdated(updatedTask: Task) {
        val index = tasksList.indexOfFirst { it.id == updatedTask.id }
        tasksList[index] = updatedTask
    }

    fun getOfSection(section: String, sortOrder: String): List<Task> {
        var tempTaskList = when (sortOrder) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy {  it.assignee?.firstName +" "+ it.assignee?.lastName }
            "Section" -> tasksList.sortedBy { it.section }
            else -> tasksList
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter { it.section == section && it.title.contains(searchQuery.value, ignoreCase = true) }
    }



    fun getOfUser(user: String): List<Task> {
        var tempTaskList = when (currentSortOrder.value) {
            "Due date" -> tasksList.sortedBy { it.dueDate }
            "A-Z order" -> tasksList.sortedBy { it.title }
            "Z-A order" -> tasksList.sortedBy { it.title }.reversed()
            "Assignee" -> tasksList.sortedBy { it.assignee?.firstName +" "+ it.assignee?.lastName }
            "Section" -> tasksList.sortedBy { it.section }
            else -> {
                tasksList
            }
        }
        tempTaskList = customFilter(tempTaskList)
        return tempTaskList.filter {  it.assignee?.firstName +" "+ it.assignee?.lastName == user && it.title.contains(searchQuery.value, ignoreCase = true) }
    }

    /*var searchQuery by mutableStateOf("")

    fun changeSearchQuery(newQuery: String) {
        searchQuery = newQuery
    }*/

    // FILTERS VARIABLES
    /*class FilterParams {
        var assignee by mutableStateOf("")
        var section by mutableStateOf("")
        var status by mutableStateOf("")
        var recurrent by mutableStateOf("")
        var completed by mutableStateOf(false)

        fun clear() {
            assignee = ""
            section = ""
            status = ""
            recurrent = ""
            completed = false
        }
    }*/



    fun areThereActiveFilters(): Boolean {

        return if (filterParams.assignee != "") true
        else if (filterParams.section != "") true
        else if (filterParams.status != "") true
        else if (filterParams.completed) true
        else false
    }

    private fun customFilter(inputList: List<Task>): List<Task> {
        return inputList.filter {
            (filterParams.section == "" || it.section.contains(filterParams.section, ignoreCase = true))
                    && (filterParams.assignee == "" || (it.assignee?.firstName +" "+ it.assignee?.lastName).contains(filterParams.assignee, ignoreCase = true))
                    && (filterParams.status == "" || it.status == filterParams.status) && ((filterParams.completed && it.completed) || (!filterParams.completed && !it.completed))
        }
    }

    val recurrentList = listOf("None", "Daily", "Weekly", "Monthly")


    // SORT VARIABLES
    val allSortOrders = listOf("A-Z order", "Z-A order", "Due date", "Assignee", "Section")
    /*var currentSortOrder by mutableStateOf("Due date")
        private set
        */

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
}*/