package it.polito.workstream.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.tasks.components.toDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class TaskViewModel(
    activeTeamFlow: Flow<Team?>,
    val activeUser: User,
    val activeTeamId: MutableStateFlow<String>,
    val onTaskUpdated: (updatedTask: Task) -> Unit,
) : ViewModel() {
    // List of possible values for the frequency of a recurrent task
    val frequencies = listOf("None", "Daily", "Weekly", "Monthly")
    val statuses = listOf("To Do", "In progress", "Paused", "On review", "Completed")

    val activeTeam = activeTeamFlow.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    var task = mutableStateOf(Task(title = "New Task", section = activeTeam.value?.sections?.get(0) ?: "General"))
        private set

    fun setTask(value: Task) {
        task.value = value
        taskBeforeEditing = value.copy()
        titleValue = value.title
        descriptionValue = value.description
        assigneeValue = value.assignee
        sectionValue = value.section
        isRecurrentValue = value.recurrent
        frequencyValue = value.frequency
        statusValue.value = value.status
        dueDateValue = value.dueDate

        expandedSection = false
        expandedStatus = false
        expandedFrequency = false
        expandedUser = false
        isDatePickerOpen = false

        titleError = ""
        sectionError = ""
        dueDateError = ""
    }

    // ------- Mutable state variables to store the values of the task -------
    var dueDateValue by mutableStateOf(task.value.dueDate)
        private set
    var dueDateError by mutableStateOf("")
        private set

    fun setDueDate(value: Timestamp?) {
        dueDateValue = value
    }

    private fun checkDueDate() {
        val isPast = dueDateValue?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(it) < SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(Timestamp(System.currentTimeMillis()))
        } ?: false

        dueDateError = if (dueDateValue != null && isPast) "Due date cannot be in the past" else ""
    }

    var titleValue by mutableStateOf(task.value.title)
        private set
    var titleError by mutableStateOf("")
        private set

    fun setTitle(value: String) {
        titleValue = value
    }

    private fun checkTitle() {
        titleError = if (titleValue.isEmpty()) "Title must not be empty" else ""
    }

    var descriptionValue by mutableStateOf(task.value.description)
        private set

    fun setDescription(value: String) {
        descriptionValue = value
    }

    var assigneeValue by mutableStateOf(task.value.assignee)

    var sectionValue by mutableStateOf(task.value.section)
        private set
    var sectionError by mutableStateOf("")
        private set

    fun setSection(value: String) {
        sectionValue = value
    }

    private fun checkSection() {
        sectionError = if (sectionValue.isEmpty()) "Section must not be empty" else ""
    }

    var isRecurrentValue by mutableStateOf(task.value.recurrent)
        private set

    fun setRecurrent(value: Boolean) {
        isRecurrentValue = value
    }

    var frequencyValue by mutableStateOf(task.value.frequency)
        private set

    fun setFrequency(value: String?) {
        frequencyValue = value
    }

    var statusValue = mutableStateOf(task.value.status)
        private set

    fun setStatus(value: String) {
        statusValue.value = value
    }

    var expandedSection by mutableStateOf(false)
    fun toggleSectionExpanded() {
        expandedSection = !expandedSection
    }

    var expandedStatus by mutableStateOf(false)
    fun toggleStatusExpanded() {
        expandedStatus = !expandedStatus
    }

    var expandedFrequency by mutableStateOf(false)
    fun toggleFrequencyExpanded() {
        expandedFrequency = !expandedFrequency
    }

    var expandedUser by mutableStateOf(false)
    fun toggleUserExpanded() {
        expandedUser = !expandedUser
    }

    var isDatePickerOpen by mutableStateOf(false)
    fun toggleDatePickerOpen() {
        isDatePickerOpen = !isDatePickerOpen
    }

    // ------- Functions to validate the values of the task.value -------
    var taskBeforeEditing by mutableStateOf(task.value.copy())

    /**
     * Checks if the provided task parameters are valid.
     *
     * A valid task title is a non-blank string after trimming any leading or trailing whitespace.
     * A valid section is a non-blank string.
     * A valid due date is a date in the future.
     *
     * @return true if all parameters are valid, false otherwise
     */
    fun isTaskValid(): Boolean {
        titleValue = titleValue.trim()

        checkTitle()
        checkDueDate()
        checkSection()

        return titleError.isBlank() && dueDateError.isBlank() && sectionError.isBlank()
    }

    fun discard() {
        dueDateValue = taskBeforeEditing.dueDate
        titleValue = taskBeforeEditing.title
        descriptionValue = taskBeforeEditing.description
        assigneeValue = taskBeforeEditing.assignee
        sectionValue = taskBeforeEditing.section
        isRecurrentValue = taskBeforeEditing.recurrent
        frequencyValue = taskBeforeEditing.frequency
        statusValue.value = taskBeforeEditing.status
    }

    fun save(): Task {
        task.value.dueDate = dueDateValue
        task.value.title = titleValue
        task.value.description = descriptionValue
        task.value.assignee = assigneeValue
        task.value.section = sectionValue
        task.value.recurrent = isRecurrentValue
        task.value.frequency = frequencyValue
        task.value.status = statusValue.value

        updateTaskHistory(taskBeforeEditing, task.value)
        return task.value
    }

    fun updateTaskHistory(taskBeforeEditing: Task, updatedTask: Task) {
        if (updatedTask.history.isEmpty()) {
            updatedTask.addHistoryEntry("Task created")
            return
        }

        val author = activeUser.getFirstAndLastName()

        // Get the differences in order to update the history. Every change in the task should be recorded (e.g. "Title changed from 'oldTitle' to 'newTitle'")
        if (taskBeforeEditing.title != updatedTask.title)
            updatedTask.addHistoryEntry(author + ": changed title from '${taskBeforeEditing.title}' to '${updatedTask.title}'")
        if (taskBeforeEditing.description != updatedTask.description)
            updatedTask.addHistoryEntry("$author changed the description")
        if (taskBeforeEditing.completed != updatedTask.completed)
            updatedTask.addHistoryEntry("$author marked the task as completed")
        if (taskBeforeEditing.assignee != updatedTask.assignee)
            updatedTask.addHistoryEntry("$author changed assignee from '${taskBeforeEditing.assignee ?: "anyone"}' to '${updatedTask.assignee ?: "anyone"}'")
        if (taskBeforeEditing.section != updatedTask.section)
            updatedTask.addHistoryEntry("$author changed the section from '${taskBeforeEditing.section}' to '${updatedTask.section}'")
        if (taskBeforeEditing.dueDate != updatedTask.dueDate)
            updatedTask.addHistoryEntry("$author changed the due date from '${taskBeforeEditing.dueDate.toDate() ?: "no deadline"}' to '${updatedTask.dueDate.toDate() ?: "no deadline"}'")
        if (taskBeforeEditing.status != updatedTask.status)
            updatedTask.addHistoryEntry("$author changed the status from '${taskBeforeEditing.status ?: "to do"}' to '${updatedTask.status ?: "to do"}'")
        if (taskBeforeEditing.frequency != updatedTask.frequency)
            updatedTask.addHistoryEntry("$author changed the frequency from '${taskBeforeEditing.frequency ?: "no frequency"}' to '${updatedTask.frequency ?: "no frequency"}'")
        if (taskBeforeEditing.attachments.count() > updatedTask.attachments.count())
            updatedTask.addHistoryEntry("$author removed an attachment")
        else if (taskBeforeEditing.attachments.count() < updatedTask.attachments.count())
            updatedTask.addHistoryEntry("$author added an attachment")
        if (taskBeforeEditing.comments.count() > updatedTask.comments.count())
            updatedTask.addHistoryEntry("$author removed a comment")
        else if (taskBeforeEditing.comments.count() < updatedTask.comments.count())
            updatedTask.addHistoryEntry("$author added a comment")
    }

    fun setAssignee(m: User) {
        assigneeValue = m.email
    }
}
