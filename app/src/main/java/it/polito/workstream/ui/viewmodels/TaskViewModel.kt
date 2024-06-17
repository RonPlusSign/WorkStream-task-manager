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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class TaskViewModel(val activeTeamFlow: Flow<Team?>,val  activeTeamId: MutableStateFlow<String>) : ViewModel() {
    // List of possible values for the frequency of a recurrent task
    val frequencies = listOf("None", "Daily", "Weekly", "Monthly")
    val statuses = listOf("To do", "In progress", "Paused", "On review", "Completed")

    val activeTeam = activeTeamFlow.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    var task = mutableStateOf( Task(title = "New Task", section = activeTeam.value?.sections?.get(0) ?: "General"))
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
        statusValue = value.status
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
        dueDateError = if (dueDateValue != null && dueDateValue!!.before(Timestamp(System.currentTimeMillis())))
            "Due date cannot be in the past"
        else ""
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

    var statusValue by mutableStateOf(task.value.status)
        private set

    fun setStatus(value: String) {
        statusValue = value
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
    private var taskBeforeEditing = task.value.copy()

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
        statusValue = taskBeforeEditing.status
    }

    fun save(): Task {
        updateTaskHistory(taskBeforeEditing, task.value)

        task.value.dueDate = dueDateValue
        task.value.title = titleValue
        task.value.description = descriptionValue
        task.value.assignee = assigneeValue
        task.value.section = sectionValue
        task.value.recurrent = isRecurrentValue
        task.value.frequency = frequencyValue
        task.value.status = statusValue
        task.value.teamId = activeTeamId.value //PROBLEMA TODO activeTeam.value è sempre null

        return task.value
    }

    private fun updateTaskHistory(taskBeforeEditing: Task, updatedTask: Task) {

        if (taskBeforeEditing.title == "New Task") {
            updatedTask.addHistoryEntry("Task created")
            return
        }

        // Get the differences in order to update the history. Every change in the task should be recorded (e.g. "Title changed from 'oldTitle' to 'newTitle'")
        if (taskBeforeEditing.title != updatedTask.title)
            updatedTask.addHistoryEntry("Title changed from '${taskBeforeEditing.title}' to '${updatedTask.title}'")
        if (taskBeforeEditing.assignee != updatedTask.assignee)
            updatedTask.addHistoryEntry("Assignee changed from '${taskBeforeEditing.assignee ?: "anyone"}' to '${updatedTask.assignee ?: "anyone"}'")
        if (taskBeforeEditing.section != updatedTask.section)
            updatedTask.addHistoryEntry("Section changed from '${taskBeforeEditing.section}' to '${updatedTask.section}'")
        if (taskBeforeEditing.dueDate != updatedTask.dueDate)
            updatedTask.addHistoryEntry("Due date changed from '${taskBeforeEditing.dueDate ?: "no deadline"}' to '${updatedTask.dueDate ?: "no deadline"}'")
        if (taskBeforeEditing.status != updatedTask.status)
            updatedTask.addHistoryEntry("Status changed from '${taskBeforeEditing.status ?: "to do"}' to '${updatedTask.status ?: "to do"}'")
        if (taskBeforeEditing.frequency != updatedTask.frequency)
            updatedTask.addHistoryEntry("Frequency changed from '${taskBeforeEditing.frequency ?: "no frequency"}' to '${updatedTask.frequency ?: "no frequency"}'")
        if (taskBeforeEditing.attachments.count() > updatedTask.attachments.count())
            updatedTask.addHistoryEntry("Attachment removed")
        else if (taskBeforeEditing.attachments.count() < updatedTask.attachments.count())
            updatedTask.addHistoryEntry("Attachment added")
        if (taskBeforeEditing.comments.count() > updatedTask.comments.count())
            updatedTask.addHistoryEntry("Comment removed")
        else if (taskBeforeEditing.comments.count() < updatedTask.comments.count())
            updatedTask.addHistoryEntry("Comment added")

        Log.i("TASK_HISTORY", "Updated task history: ${updatedTask.history.toList()}") // If you remove this, the history will not be updated
    }

    /** Returns true if the task is expired (i.e. true date is in the past), false otherwise */
    fun isExpired(): Boolean {
        return dueDateValue?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(it) < SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(Timestamp(System.currentTimeMillis()))
        } ?: false
    }

    fun setAssignee(m: User) {
        assigneeValue = m.email
    }
}
