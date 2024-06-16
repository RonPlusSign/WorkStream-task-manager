package it.polito.workstream.ui.models

import java.sql.Timestamp

data class TaskDTO(val id: String = "",
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
                   var teamId: String? = null)

fun Task.toDTO() = TaskDTO(id, title, description, completed, dueDate, status, assignee, section, recurrent, frequency, attachments, teamId)
