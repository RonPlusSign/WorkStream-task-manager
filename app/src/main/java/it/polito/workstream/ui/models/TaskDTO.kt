package it.polito.workstream.ui.models

import java.sql.Timestamp
import java.util.Date

data class TaskDTO(val id: String = "",
                   var title: String = "",
                   var description: String = "",
                   var completed: Boolean = false,
                   var dueDate: Date? = null,
                   var status: String? = "To Do",
                   var assignee: String? = null,
                   var section: String = "General",
                   var recurrent: Boolean = false,
                   var frequency: String? = null,
                   var attachments: MutableList<String> = mutableListOf(),
                   var teamId: String? = null)

fun Task.toDTO() = TaskDTO(id, title, description, completed, dueDate, status, assignee, section, recurrent, frequency, attachments, teamId)

fun TaskDTO.toTask() : Task = Task(id = id, title = title, description = description, completed= completed,
   dueDate = dueDate?.let { Timestamp(it.time) },status=  status, assignee=assignee, section = section, recurrent = recurrent, frequency =frequency, attachments = attachments, teamId = teamId)