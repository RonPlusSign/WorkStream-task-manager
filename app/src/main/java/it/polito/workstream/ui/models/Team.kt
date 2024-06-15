package it.polito.workstream.ui.models

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class Team(
    var id: String = getNewId(),
    var name: String,
    var members: MutableList<User> = mutableListOf(),
    var tasks: MutableList<Task> = mutableListOf(),
    var sections: MutableList<String> = mutableListOf("General"),
    var admin: User? = null,
    var profilePicture: MutableState<String> = mutableStateOf(""),
    var profileBitmap: MutableState<Bitmap?> = mutableStateOf(null),
    var membersFlow : Flow<List<User>> = flowOf(),
    var tasksFlow : Flow<List<Task>> = flowOf(),
    var adminFlow : Flow<User> = flowOf(),
    var membersRef: CollectionReference? = null,
    var adminEmail : String = ""
) {
    init {
        members.forEach { it.addTeam(this) }
        admin = members.firstOrNull()
        tasks.forEach { it.team = this }
    }

    constructor(
        name: String,
        members: MutableList<User> = mutableListOf(),
        tasks: MutableList<Task> = mutableListOf(),
        sections: MutableList<String> = mutableListOf()
    ) : this(getNewId(), name, members, tasks, sections)

    companion object {  // To generate unique identifiers for teams
        private var idCounter: Long = 0
        private fun getNewId() = "${idCounter++}"
    }

    /** Adds a task to the team
     * @param task The task to be added
     */
    fun addTask(task: Task) {
        tasks.add(task)
    }

    /** Removes a task from the team
     * @param task The task to be removed
     */
    fun removeTask(task: Task) {
        tasks.remove(task)
    }

    /** Adds a member to the team
     * @param member The member to be added
     */
    fun addMember(member: User) {
        members.add(member)
    }

    /** Removes a member from the team
     * @param member The member to be removed
     */
    fun removeMember(member: User) {
        members.remove(member)
    }

    /** Adds a section to the team
     * @param section The section to be added
     */
    fun addSection(section: String) {
        sections.add(section)
    }

    /** Removes a section from the team
     * @param section The section to be removed
     */
    fun removeSection(section: String) {
        sections.remove(section)
    }
}
