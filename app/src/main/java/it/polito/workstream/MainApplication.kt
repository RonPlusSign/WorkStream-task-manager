package it.polito.workstream

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update


class MainApplication : Application() {
    lateinit var db: FirebaseFirestore

    lateinit var context: Context
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        db = Firebase.firestore
    }

    val _user = MutableStateFlow(User())
    val user: StateFlow<User> = _user

    private var activeTeamId = MutableStateFlow("")
    private fun fetchActiveTeam(): Flow<Team?> = if(activeTeamId.value.isEmpty()) emptyFlow() else callbackFlow {
        //val listener = db.collection("Teams").whereEqualTo("id", activeTeamId.value).limit(1)

            Log.d("Firestore", "Active team ID: ${activeTeamId.value}")
            val listener = db.collection("Teams").document(activeTeamId.value)
                .addSnapshotListener { value, error ->
                    if (value != null) {
                        val team = value.toObject(Team::class.java)
                        activeTeamId.value = team?.id!!
                        Log.d("Firestore", "Active team ID: ${team.id}")
                        trySend(team)
                    } else {
                        trySend(null)
                    }
                }
            awaitClose { listener.remove()
            }



    }

    val activeTeam = fetchActiveTeam()

    private fun getTeams(): Flow<List<Team>> = callbackFlow {
        val userId: String = user.value.email
        val listener = db.collection("Teams").whereArrayContains("members", listOf(userId)).addSnapshotListener { r, e ->
            if (r != null) {
                val teams = r.toObjects(Team::class.java)
                trySend(teams)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    val userTeams = getTeams()  // Teams of the current user

    private fun getTasks(teamId: String): Flow<List<Task>> = callbackFlow {
        val listener = db.collection("tasks").whereEqualTo("teamId", teamId).addSnapshotListener { r, e ->
            if (r != null) {
                val tasks = r.toObjects(Task::class.java)
                trySend(tasks)
            }
        }
        awaitClose { listener.remove() }
    }

    val teamTasks = getTasks(activeTeamId.value)


    private fun fetchUsers(teamId: String): Flow<List<User>> = callbackFlow {
        val listener = db.collection("Teams").whereEqualTo("teamId", teamId)
            .addSnapshotListener { r, e ->
                if (r != null) {
                    val users = r.toObjects(User::class.java)
                    trySend(users)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    val activeTeamMembers = fetchUsers(activeTeamId.value)

    fun createTask(task: Task) {
        db.collection("tasks").add(task)
            .addOnSuccessListener { documentReference -> Log.d("Firestore", "Task created with ID: ${documentReference.id}") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error creating a task", e) }
    }

    fun createTeam(team: Team) {
        db.collection("Teams").add(team)
            .addOnSuccessListener { documentReference -> Log.d("Firestore", "Team created with ID: ${documentReference.id}") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error creating a team", e) }
    }

    fun updateTeam(team: Team) {
        db.collection("Teams").document(activeTeamId.value).set(team)
            .addOnSuccessListener { Log.d("Firestore", "Team successfully updated!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error updating team", e) }
    }

    var activePageValue = MutableStateFlow(Route.TeamTasks.name)
        private set

    fun setActivePage(page: String) {
        activePageValue.value = page
    }

    fun changeActiveTeamId(teamId: String) {
        activeTeamId.value = teamId
    }

    fun leaveTeam(teamId: String, userId: String) {
        // Remove the user from the team's members list
        /*db.collection("Teams").document(teamId).update("members", FieldValue.arrayRemove(userId))
            .addOnSuccessListener { Log.d("Firestore", "User removed from team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error removing user from team", e) }

        // Remove the team from the user's teams list
        db.collection("users").document(userId).update("teams", FieldValue.arrayRemove(teamId))
            .addOnSuccessListener { Log.d("Firestore", "Team removed from user") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error removing team from user", e) }*/

        val teamRef = db.collection("Teams").document(teamId)
        val userRef = db.collection("users").document(userId)
        db.runTransaction {
            teamRef.update("members", FieldValue.arrayRemove(userId))
            userRef.update("teams", FieldValue.arrayRemove(teamId))
        }
        .addOnSuccessListener { Log.d("Firestore", "Team removed from user") }
        .addOnFailureListener { e -> Log.w("Firestore", "Error removing team from user", e) }



    }

    fun removeTeam(teamId: String) {
        db.collection("Teams").document(teamId.toString()).delete()
            .addOnSuccessListener { Log.d("Firestore", "Team $teamId deleted") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error deleting team $teamId", e) }
    }

    fun joinTeam(teamId: String, userId: String) {
        // Add the user to the team's members list
        db.collection("Teams").document(teamId).update("members", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { Log.d("Firestore", "User added to team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding user to team", e) }
    }

    fun createEmptyTeam(nameTeam: String): Result<String> {
        val newTeam = Team(name = nameTeam, admin = user.value.email, members = mutableListOf(user.value.email))
        var newTeamId = newTeam.id

        // Create the team in Firestore
        val newTeamRef = db.collection("Teams").document(newTeam.id)
        val userRef = db.collection("users").document(user.value.email)
        db.runTransaction {
            it.set(newTeamRef, newTeam)
            it.update(userRef, "teams", FieldValue.arrayUnion(newTeam.id))
        }
        .addOnSuccessListener { Log.d("Firestore", "User added to team") }
        .addOnFailureListener { e -> Log.w("Firestore", "Error adding user to team", e) }


        /*db.collection("Teams").add(newTeam)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Team created with ID: ${documentReference.id}")
                newTeam.id = documentReference.id

                // Add the team to the user's teams list
                db.collection("users").document(user.value.email).update("teams", FieldValue.arrayUnion(documentReference.id))
                    .addOnSuccessListener {
                        Log.d("Firestore", "Team added to user")
                        newTeamId = documentReference.id
                    }
                    .addOnFailureListener { e -> Log.w("Firestore", "Error adding team to user", e) }
            }
            .addOnFailureListener { e -> Log.w("Firestore", "Error creating a team", e) }*/

        if (newTeamId == "") return Result.failure(Exception("Error creating team"))
        return Result.success(newTeamId)
    }

    //TaskListViewModel

    //update task
    fun onTaskUpdated(updatedTask: Task) {
        db.collection("task").document(updatedTask.id).set(updatedTask)
    }

    fun deleteTask(task: Task) {
        // Remove the task from the user's tasks list
        val taskRef = db.collection("task").document(task.id)
        val userRef = task.assignee?.let { db.collection("users").document(it) }
        val teamRef = db.collection("Teams").document(activeTeamId.value)
        db.runTransaction { t ->
            t.delete(taskRef)
            task.assignee?.let {
                if (userRef != null) {
                    t.update(userRef, "tasks", FieldValue.arrayRemove(task.id))
                    t.update(teamRef,"tasks", FieldValue.arrayRemove(task.id))
                }
            }
        }
            .addOnSuccessListener { Log.d("Firestore", "Transaction success!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Transaction failure.", e) }

        // Remove the task from the team's tasks list
        /*
        db.collection("Teams").document(activeTeamId.value.toString()).update("tasks", FieldValue.arrayRemove(task.id))
            .addOnSuccessListener { Log.d("Firestore", "Task removed from team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error removing task from team", e) }*/
    }

    fun onTaskCreated(t: Task) {
        // Add the task to the user's tasks list
        val task = t.copy()
        val userRef = db.collection("users").document(t.assignee!!)
        val taskRef = db.collection("task").document()
        db.runTransaction {
            it.update(userRef, "tasks", FieldValue.arrayUnion(taskRef.id))
            it.set(taskRef, task)
        }
            .addOnSuccessListener { Log.d("Firestore", "Transaction success!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Transaction failure.", e) }

        // Add the task to the team's tasks list
        db.collection("Teams").document(activeTeamId.value).update("tasks", FieldValue.arrayUnion(taskRef.id))
            .addOnSuccessListener { Log.d("Firestore", "Task added to team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding task to team", e) }
    }

    fun onTaskDeleted(t: Task) {
        // Remove the task from the user's tasks list
        val taskRef = db.collection("task").document(t.id)
        val userRef = t.assignee?.let { db.collection("users").document(it) }
        val teamRef = db.collection("Teams").document(activeTeamId.value)
        db.runTransaction { tr ->
            tr.delete(taskRef)
            tr.update(userRef!!, "tasks", FieldValue.arrayRemove(t.id))
            tr.update(teamRef,"tasks", FieldValue.arrayRemove(t.id))
        }
            .addOnSuccessListener { Log.d("Firestore", "Transaction success!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Transaction failure.", e) }

        // Remove the task from the team's tasks list
        /*db.collection("Teams").document(activeTeamId.value.toString()).update("tasks", FieldValue.arrayRemove(t.id))
            .addOnSuccessListener { Log.d("Firestore", "Task removed from team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error removing task from team", e) }*/
    }

    val currentSortOrder: MutableStateFlow<String> = MutableStateFlow("Due date")
    fun setSortOrder(newSortOrder: String) {
        currentSortOrder.update { newSortOrder }
    }

    val filterParams = mutableStateOf(FilterParams())
    val searchQuery = mutableStateOf("")

    fun setSearchQuery(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun setTeamProfileBitmap(s: String, bitmap: Bitmap?) {
        TODO("Not yet implemented")
    }

    fun setTeamProfilePicture(s: String, s1: String) {
        TODO("Not yet implemented")
    }

    fun updateUser(firstName: String, lastName: String, email: String, location: String) {
        _user.value.firstName = firstName
        _user.value.lastName = lastName
        _user.value.email = email
        _user.value.location = location

        db.collection("users").document(email).set(_user.value)
            .addOnSuccessListener { Log.d("UserProfile", "User profile updated successfully") }
            .addOnFailureListener { e -> Log.e("UserProfile", "Error updating user profile", e) }
    }

    fun addSectionToTeam(section: String) {
        if (activeTeamId.value.isBlank()) {
            Log.e("AddSection", "Active team ID is blank!")
            return
        }

        db.collection("Teams").document(activeTeamId.value).update("sections", FieldValue.arrayUnion(section))
            .addOnSuccessListener { Log.d("Firestore", "Section added to team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding section to team", e) }
    }

    fun removeSectionFromTeam(section: String) {
        db.collection("Teams").document(activeTeamId.value).update("sections", FieldValue.arrayRemove(section))
            .addOnSuccessListener { Log.d("Firestore", "Section removed from team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error removing section from team", e) }
    }

    val chatModel = ChatModel(emptyList())  // TODO: CAMBIA
}

class ChatModel(userList: List<User>) {
    private val _chats: MutableStateFlow<MutableMap<User, MutableList<ChatMessage>>> = MutableStateFlow(mutableStateMapOf())

    val chats: StateFlow<MutableMap<User, MutableList<ChatMessage>>> = _chats
    fun newChat(user: User) {
        _chats.value.put(user, mutableStateListOf())
    }

    fun sendMessage(destUser: User, message: ChatMessage) {
        _chats.value[destUser]?.add(message)
    }

    fun editMessage(destUser: User, messageId: Long, newText: String) {
        _chats.value[destUser]?.find { it.id == messageId }?.text = newText
    }

    fun deleteMessage(user: User, messageId: Long) {
        _chats.value[user]?.removeIf { it.id == messageId }
    }

    private val _groupChat: MutableStateFlow<MutableList<ChatMessage>> = MutableStateFlow(mutableStateListOf())
    val groupChat: StateFlow<MutableList<ChatMessage>> = _groupChat
    fun sendGroupMessage(message: ChatMessage) {
        _groupChat.value.add(message)
    }

    fun editGroupMessage(messageId: Long, newText: String) {
        _groupChat.value[messageId.toInt()].text = newText
    }

    fun deleteGroupMessage(messageId: Long) {
        _groupChat.value.removeIf { it.id == messageId }
    }
}

// FILTERS VARIABLES
class FilterParams {
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
}
