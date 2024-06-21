package it.polito.workstream.ui.models

import java.sql.Timestamp
import java.util.Date


/**
 * Class that represents a task to be done.
 *
 * @property id The unique identifier of the task
 * @property title The title of the task
 * @property description The description of the task
 * @property completed Whether the task has been completed or not
 * @property dueDate The due date of the task
 * @property status The status of the task (e.g. "To Do", "In Progress", "Done")
 * @property assignee The user to whom the task is assigned
 * @property section The section to which the task belongs
 * @property recurrent Whether the task is recurrent or not
 * @property frequency The frequency of the task, if recurrent (e.g. "Daily", "Weekly", "Monthly")
 * @property attachments The attachments related to the task (list of file paths)
 * @property comments The comments related to the task
 */
data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var completed: Boolean = false,
    var dueDate: Timestamp? = null,
    var status: String? = "To Do",
    var assignee: String? = null,
    var section: String = "General",
    var recurrent: Boolean = false,
    var frequency: String? = null,
    var attachments: MutableList<String> = mutableListOf(),
    var comments: MutableList<Comment> = mutableListOf(),
    var history: MutableMap<Timestamp, String> = mutableMapOf(), // list of status changes, can be represented as a list of pairs of "change description" and timestamp
    var teamId: String? = null
) {


    /** Completes the task */
    fun complete() {
        completed = true
    }

    fun addHistoryEntry(entry: String) {
        history[Timestamp(System.currentTimeMillis())] = entry
    }

    fun copy(
        title: String = this.title,
        description: String = this.description,
        completed: Boolean = this.completed,
        dueDate: Timestamp? = this.dueDate,
        status: String? = this.status,
        assignee: String? = this.assignee,
        section: String = this.section,
        recurrent: Boolean = this.recurrent,
        frequency: String? = this.frequency,
        attachments: MutableList<String> = this.attachments,
        comments: MutableList<Comment> = this.comments,
    ) = Task(
        title = title,
        description = description,
        completed = completed,
        dueDate = dueDate,
        status = status,
        assignee = assignee,
        section = section,
        recurrent = recurrent,
        frequency = frequency,
        attachments = attachments,
        comments = comments,
        history = this.history,
        teamId = this.teamId,
    )
}

class Comment {
    var id: String = ""
    var text: String = ""
    var author: String = ""
    var timestamp: Date = Date()
    var taskId: String = ""



}

