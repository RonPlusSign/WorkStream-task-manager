package it.polito.workstream.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalContext
import it.polito.workstream.MainApplication
import java.sql.Timestamp


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
class Task(
    val id: String = getNewId(),
    var title: String,
    var description: String = "",
    var completed: Boolean = false,
    var dueDate: Timestamp? = null,
    var status: String? = "To Do",
    //var userAssigneName: String? = null,
    var assignee: User? ,
    var section: String = "General",
    var recurrent: Boolean = false,
    var frequency: String? = null,
    var attachments: MutableList<String> = mutableListOf(),
    var comments: MutableList<Comment> = mutableListOf(),
    var history: MutableMap<Timestamp, String> = mutableMapOf(), // list of status changes, can be represented as a list of pairs of "change description" and timestamp
    var team: Team? = null,
    val taskId : String = ""
) {
    init {
        //if (assignee==null) this.assignee=userAssigneName?.let { (context?.applicationContext as? MainApplication)?._userList?.value?.find { (it.firstName+" "+it.lastName) == userAssigneName } }
        addHistoryEntry("Task created")
        assignee?.addTask(this)
    }

    // Secondary constructor, which allows to create a task without specifying the id
    constructor(
        title: String,
        description: String = "",
        completed: Boolean = false,
        dueDate: Timestamp? = null,
        status: String? = "To Do",
        assignee: User? = null,
        section: String = "General",
        recurrent: Boolean = false,
        frequency: String? = null,
        attachments: MutableList<String> = mutableListOf(),
        comments: MutableList<Comment> = mutableListOf(),
        history: MutableMap<Timestamp, String> = mutableMapOf(),
        team: Team? = null,
    ) : this(getNewId(), title, description, completed, dueDate, status, assignee, section, recurrent, frequency, attachments, comments, history, team)

    companion object {  // To generate unique identifiers for tasks
        private var idCounter: Long = 0
        private fun getNewId() = "${idCounter++}"
    }

    /** Adds a comment to the task
     * @param text The text of the comment
     * @param author The author of the comment
     */
    fun addComment(text: String, author: String) {
        comments.add(Comment(text = text, author = author))
    }

    /** Removes a comment from the task
     * @param comment The comment to be removed
     */
    fun removeComment(comment: Comment) {
        comments.remove(comment)
    }

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
        assignee: User? = this.assignee,
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
        team = this.team,
    )
}

class Comment(
    val id: Long = getNewId(),
    val text: String,
    val author: String,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
) {
    // Secondary constructor, which allows to create a comment without specifying the id
    constructor(text: String, author: String) : this(getNewId(), text, author)

    companion object {  // To generate unique identifiers for comments
        private var idCounter: Long = 0
        private fun getNewId() = idCounter++
    }
}

