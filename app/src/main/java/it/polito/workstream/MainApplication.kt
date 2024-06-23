package it.polito.workstream

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import it.polito.workstream.ui.models.Chat
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.models.Comment
import it.polito.workstream.ui.models.GroupChat
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.TaskDTO
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.models.toDTO
import it.polito.workstream.ui.models.toTask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import java.io.File


class MainApplication : Application(), ImageLoaderFactory {
    lateinit var db: FirebaseFirestore
    lateinit var chatModel: ChatModel
    lateinit var storage: FirebaseStorage

    lateinit var context: Context
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        db = Firebase.firestore
        chatModel = ChatModel(_user, activeTeamId, db)
        storage = Firebase.storage
        context = this.applicationContext


    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true).logger(DebugLogger())
            .build()
    }

    val _user = MutableStateFlow(User())
    val user: StateFlow<User> = _user

    val LocalPhotos = mutableStateListOf<String>()
    val LocalDocuments = mutableStateListOf<String>()

    var activeTeamId = MutableStateFlow("")
    fun fetchActiveTeam(activeTeamId: String): Flow<Team?> = callbackFlow {
        Log.d("Firestore", "Active team ID: ${activeTeamId}")
        if (activeTeamId.isNotEmpty()) {
            db.collection("Teams").document(activeTeamId)
                .addSnapshotListener { value, error ->
                    if (value != null) {
                        val team = value.toObject(Team::class.java)
                        if (team != null) {
                            downloadPhoto(team.photo, team.photo)
                        }

                        Log.d("Firestore", "Active team ID: ${team}")
                        trySend(team)
                    } else {
                        Log.d("ERRORE", "ERRORE GRAVE")
                        trySend(null)
                    }
                }
        }

        awaitClose()
    }

    fun fetchSections(activeTeamId: String): Flow<List<String>> = callbackFlow {

        Log.d("Firestore", "Active team ID: ${activeTeamId}")
        if (activeTeamId.isNotEmpty()) {
            db.collection("Teams").document(activeTeamId)
                .addSnapshotListener { value, error ->
                    if (value != null) {
                        val sections = value.toObject(Team::class.java)?.sections
                        if (sections != null) {
                            trySend(sections)
                        }
                    } else {
                        Log.d("ERRORE", "ERRORE GRAVE $error")
                        trySend(emptyList())
                    }
                }
        }

        awaitClose { }
    }

    val activeTeam = fetchActiveTeam(activeTeamId.value)

    fun getTeams(): Flow<List<Team>> = callbackFlow {
        val userId: String = user.value.email
        val listener = db.collection("Teams").whereArrayContains("members", userId).addSnapshotListener { r, e ->
            if (r != null) {
                val teams = r.toObjects(Team::class.java)
                teams.forEach {
                    downloadPhoto(it.photo, it.photo)
                }

                // Check if the user is still a member of the team, otherwise change active team
                if (activeTeamId.value.isNotEmpty() && !teams.any { it.id == activeTeamId.value }) {
                    activeTeamId.value = teams.firstOrNull()?.id ?: ""
                }

                trySend(teams)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    fun getTasks(teamId: String): Flow<List<Task>> = callbackFlow {
        val listener = db.collection("Tasks").whereEqualTo("teamId", teamId).addSnapshotListener { r, e ->
            if (r != null) {
                val tasks = r.toObjects(TaskDTO::class.java).map { it.toTask() }
                for (t in tasks)
                    for (a in t.attachments)
                        downloadDocument(a)

                trySend(tasks)
            }
        }
        awaitClose { listener.remove() }
    }

    val teamTasks = getTasks(activeTeamId.value)


    fun fetchUsers(teamId: String): Flow<List<User>> = callbackFlow {
        val listener = db.collection("users").whereArrayContains("teams", teamId)
            .addSnapshotListener { r, e ->
                if (r != null) {
                    val users = r.toObjects(User::class.java)
                    for (u in users) {
                        downloadPhoto(u.photo, u.photo)
                    }
                    trySend(users)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    val activeTeamMembers = fetchUsers(activeTeamId.value)

    fun createTask(task: Task) {
        db.collection("Tasks").add(task)
            .addOnSuccessListener { documentReference -> Log.d("Firestore", "Task created with ID: ${documentReference.id}") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error creating a task", e) }
    }

    fun createTeam(team: Team) {
        db.collection("Teams").add(team)
            .addOnSuccessListener { documentReference -> Log.d("Firestore", "Team created with ID: ${documentReference.id}") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error creating a team", e) }
    }

    fun updateTeam(team: Team) {
        val t = team.toDTO()


        db.collection("Teams").document(activeTeamId.value).set(team.toDTO())
            .addOnSuccessListener { Log.d("Firestore", "Team successfully updated!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error updating team", e) }
        if (team.photo.isNotEmpty())
            uploadPhoto(team)
    }

    fun updateTeamName(newName: String, teamId: String) {
        db.collection("Teams").document(teamId).update("name", newName)
    }

    var activePageValue = MutableStateFlow(Route.TeamTasks.name)
        private set

    fun setActivePage(page: String) {
        activePageValue.value = page
    }

    fun changeActiveTeamId(teamId: String) {
        if (teamId != "no_team" && !teamId.isBlank())
            activeTeamId.value = teamId
    }

    fun leaveTeam(teamId: String, userId: String) {

        if (teamId.isEmpty() || userId.isEmpty()) {
            Log.w("Firestore", "team: $teamId or user:$userId is empty error")
            return
        }


        val teamRef = db.collection("Teams").document(teamId)
        val userRef = db.collection("users").document(userId)
        db.runTransaction {
            teamRef.update("members", FieldValue.arrayRemove(userId))
            userRef.update("teams", FieldValue.arrayRemove(teamId))
        }
            .addOnSuccessListener {
                Log.d("Firestore", "Team removed from user")
                if (user.value.email == userId) {   // I'm removing myself from the team
                    val nextTeam = user.value.teams.firstOrNull { it != teamId }
                    activeTeamId.value = nextTeam ?: "no_team"
                }
            }
            .addOnFailureListener { e -> Log.w("Firestore", "Error removing team from user", e) }
    }

    fun removeTeam(teamId: String, team: Team) {
        val usersRefs = team.members.map { db.collection("users").document(it) }
        val teamRef = db.collection("Teams").document(teamId)
        db.runTransaction {
            for (u in usersRefs) {
                it.update(u, "teams", FieldValue.arrayRemove(teamId))
            }
            it.delete(teamRef)
        }.addOnSuccessListener {
            Log.d("Firestore", "Team $teamId deleted")
            val nextTeam = user.value.teams.firstOrNull { it != teamId }
            activeTeamId.value = nextTeam ?: ""
        }
            .addOnFailureListener { e -> Log.w("Firestore", "Error deleting team $teamId", e) }

        //db.collection("Teams").document(teamId).delete()

    }

    fun joinTeam(teamId: String, userId: String) {
        // Add the user to the team's members list
        val teamRef = db.collection("Teams").document(teamId)
        val userRef = db.collection("users").document(userId)
        db.runTransaction {
            it.update(teamRef, "members", FieldValue.arrayUnion(userId))
            it.update(userRef, "teams", FieldValue.arrayUnion(teamId))
        }
            .addOnSuccessListener { Log.d("Firestore", "Team $teamId joined") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error join team $teamId", e) }
//        activeTeamId.value = teamId
    }

    fun fetchTeam(teamId: String): Flow<Team> = callbackFlow {
        val l = db.collection("Teams").document(teamId).addSnapshotListener { r, e ->
            if (r != null) {
                r.toObject<Team>()?.let { trySend(it) }
            }

        }
        awaitClose { l.remove() }
    }


    fun createEmptyTeam(nameTeam: String): Result<String> {
        val newTeam = Team(name = nameTeam, admin = user.value.email, members = mutableListOf(user.value.email), photo = "")


        // Create the team in Firestore
        Log.d("Firestore", "newTeam ID: ${newTeam.id} email ${user.value.email}")
        val newTeamRef = db.collection("Teams").document()
        val newTeamId = newTeamRef.id
        newTeam.id = newTeamRef.id // se lo tocchi ti taglio le mani
        val userRef = db.collection("users").document(user.value.email)
        db.runTransaction {
            it.set(newTeamRef, newTeam)
            it.update(userRef, "teams", FieldValue.arrayUnion(newTeamRef.id))
        }
            .addOnSuccessListener { activeTeamId.value = newTeamId; Log.d("Firestore", "User added to team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding user to team", e) }



        if (newTeamId == "") return Result.failure(Exception("Error creating team"))

        return Result.success(newTeamId)
    }

    fun addSectionToTeam(section: String) {
        db.collection("Teams").document(activeTeamId.value).update("sections", FieldValue.arrayUnion(section))
            .addOnSuccessListener { Log.d("Firestore", "Section added to team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding section to team", e) }
    }

    fun removeSectionFromTeam(section: String) {
        db.collection("Teams").document(activeTeamId.value).update("sections", FieldValue.arrayRemove(section))
            .addOnSuccessListener { Log.d("Firestore", "Section removed from team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error removing section from team", e) }
    }

    //TaskListViewModel

    //update task
    fun onTaskUpdated(updatedTask: Task) {

        Log.d("Firestore", "Task updated: $updatedTask")
        updatedTask.teamId = activeTeamId.value
        //uploadComments(updatedTask.comments.filter { it.id.isEmpty() })
        if (updatedTask.id.isEmpty())
            return

        db.collection("Tasks").document(updatedTask.id).set(updatedTask.toDTO())
            .addOnSuccessListener { Log.d("Firestore", "Transaction success!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Transaction failure.", e) }
    }

    fun fetchComments(taskId: String): Flow<List<Comment>> = callbackFlow {
        val listener = db.collection("comments").whereEqualTo("taskId", taskId).addSnapshotListener { r, e ->
            if (r != null) {
                val comments = r.toObjects(Comment::class.java)
                trySend(comments)
            } else trySend(emptyList())
            if (e != null) {
                Log.w("Firestore", "Error fetching comments", e)
            }
        }
        awaitClose { listener.remove() }

    }

    fun uploadComment(comment: Comment) {
        val ref = db.collection("comments").document()
        comment.id = ref.id
        ref.set(comment)
            .addOnSuccessListener { Log.d("Firestore", "Comments created ") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error creating a comment", e) }

    }

    private fun uploadComments(comments: List<Comment>) {
        val commentsRef = mutableListOf<DocumentReference>()
        for (comment in comments) {
            val ref = db.collection("comments").document()
            commentsRef.add(ref)
            comment.id = ref.id
        }
        db.runTransaction {
            for (i in commentsRef.indices) {
                it.set(commentsRef[i], comments[i])
            }
        }
            .addOnSuccessListener { documentReference -> Log.d("Firestore", "Comments created ") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error creating a comment", e) }

    }

    fun deleteTask(task: Task) {
        if (task.id.isEmpty())
            return
        // Remove the task from the user's tasks list
        val taskRef = db.collection("Tasks").document(task.id)
        val userRef = task.assignee?.let { db.collection("users").document(it) }
        val teamRef = db.collection("Teams").document(activeTeamId.value)
        db.runTransaction { t ->
            t.delete(taskRef)
            task.assignee?.let {
                if (userRef != null) {
                    t.update(userRef, "tasks", FieldValue.arrayRemove(task.id))
                    t.update(teamRef, "tasks", FieldValue.arrayRemove(task.id))
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
        task.teamId = activeTeamId.value
        val userRef = t.assignee?.let { db.collection("users").document(it) }
        val taskRef = db.collection("Tasks").document()
        task.id = taskRef.id
        db.runTransaction {
            if (userRef != null) {
                it.update(userRef, "tasks", FieldValue.arrayUnion(taskRef.id))
            }
            it.set(taskRef, task.toDTO())
        }
            .addOnSuccessListener { Log.d("Firestore", "Transaction success!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Transaction failure.", e) }

        // Add the task to the team's tasks list questo rompe tutto per ora

        /*db.collection("Teams").document(activeTeamId.value).update("tasks", FieldValue.arrayUnion(taskRef.id))
            .addOnSuccessListener { Log.d("Firestore", "Task added to team") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding task to team", e) }*/
    }

    fun onTaskDeleted(t: Task) {
        // Remove the task from the user's tasks list
        val taskRef = db.collection("task").document(t.id)
        val userRef = t.assignee?.let { db.collection("users").document(it) }
        val teamRef = db.collection("Teams").document(activeTeamId.value)
        db.runTransaction { tr ->
            tr.delete(taskRef)
            tr.update(userRef!!, "tasks", FieldValue.arrayRemove(t.id))
            tr.update(teamRef, "tasks", FieldValue.arrayRemove(t.id))
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

        //<<<<updateTeam>>>>()
    }

    fun downloadPhoto(pathNameDB: String, pathNameLocal: String) {
        val fileList: Array<String> = context.fileList()

        Log.d("fileList", "fileList: ${fileList.joinToString(separator = ", ")}")
        if (pathNameDB.isEmpty() || context.getFileStreamPath(pathNameLocal).exists() || LocalPhotos.contains(pathNameDB))
            return
        Log.d("pathNameDB", "pathNameDB: $pathNameDB")
        val dbRef = storage.reference.child("images").child(pathNameDB)
        val file = try {
            context.getFileStreamPath(pathNameLocal)
        } catch (_: Exception) {
            context.openFileOutput(pathNameLocal, Context.MODE_PRIVATE).write(1)
            context.getFileStreamPath(pathNameLocal)
        }
        dbRef.getFile(file)
            .addOnSuccessListener { Log.d("FireStorage", "file scaricato file: $file ") }
            .addOnFailureListener { e -> Log.w("FireStorage", "errore $e file: $file") }
    }

    fun uploadDocument(documentPath: String, taskId: String) {
        val file = Uri.parse(documentPath)
        val dbRef = file.lastPathSegment?.let { storage.reference.child("documents").child(it) }
        //val byteArray = context.openFileInput(documentPath).readBytes()
        //dbRef.putBytes(byteArray)
        file.lastPathSegment?.let { LocalDocuments.add(it) }
        dbRef?.putFile(file)?.addOnSuccessListener {
            Log.d("FireStorage", "documento caricato")
            db.collection("Tasks").document(taskId).update("attachments", FieldValue.arrayUnion(file.lastPathSegment))
                .addOnSuccessListener { Log.d("Firestore", "attachments updated") }
                .addOnFailureListener { Log.w("Firestore", "documento errore") }
        }?.addOnFailureListener { Log.w("FireStorage", "documento errore $it") }
    }

    fun deleteDocument(documentPath: String, taskId: String) {
        val dbRef = storage.reference.child("documents").child(documentPath)
        val refTask = db.collection("Tasks").document(taskId)
        refTask.update("attachments", FieldValue.arrayRemove(documentPath))
            .addOnSuccessListener {
                Log.d("Firestore", "attachments updated")
                dbRef.delete()
            }
            .addOnFailureListener { Log.w("Firestore", "documento errore") }

    }

    fun downloadDocument(documentPath: String) {
        if (documentPath.isEmpty() || LocalDocuments.contains(documentPath))
            return
        val destinationFile = File(context.getExternalFilesDir(null), documentPath)
        val dbRef = storage.reference.child("documents").child(documentPath)
        dbRef.getFile(destinationFile)
            .addOnSuccessListener { Log.d("FireStorage", "file scaricato") }
            .addOnFailureListener { e -> Log.w("FireStorage", "errore $e") }
    }

    fun uploadPhoto(team: Team) {
        if (team.photo.isNotEmpty()) {
            //ogni volta che si esegue upload photo si carica un nuovo file
            val imRef = db.collection("Images").document(team.photo)
            val teamRef = db.collection("Teams").document(team.id)
            team.photo = imRef.id


            val dbRef = storage.reference.child("images/${imRef.id}")

            val byteArray = context.openFileInput(team.photo).readBytes() //prima LocalImage

            dbRef.putBytes(byteArray)
                .addOnSuccessListener {
                    Log.d("FireStorage", "file caricato")

                    db.runTransaction {
                        LocalPhotos.add(team.photo)
                        LocalPhotos.add(imRef.id)
                        val newImage = mapOf("path" to team.photo)
                        it.set(imRef, newImage)
                        it.update(teamRef, "photo", imRef.id)
                    }
                        .addOnSuccessListener { Log.d("Firebase", "photo upload ") }
                        .addOnFailureListener { e -> Log.d("Firebase", "errore photo upload exceptio $e") }

                }
                .addOnFailureListener {
                    Log.d("FireStorage", "errore ")
                }

        }
    }

    fun uploaUserdPhoto(user: User) {
        if (user.photo.isNotEmpty()) {
            //ogni volta che si esegue upload photo si carica un nuovo file
            val imRef = db.collection("Images").document(user.photo)
            val teamRef = db.collection("Teams").document(user.email)
            user.photo = imRef.id


            val dbRef = storage.reference.child("images/${imRef.id}")

            val byteArray = context.openFileInput(user.photo).readBytes() //prima LocalImage

            dbRef.putBytes(byteArray)
                .addOnSuccessListener {
                    Log.d("FireStorage", "file caricato")

                    db.runTransaction {
                        LocalPhotos.add(user.photo)
                        LocalPhotos.add(imRef.id)
                        val newImage = mapOf("path" to user.photo)
                        it.set(imRef, newImage)
                        it.update(teamRef, "photo", imRef.id)
                    }
                        .addOnSuccessListener { Log.d("Firebase", "photo upload ") }
                        .addOnFailureListener { e -> Log.d("Firebase", "errore photo upload exceptio $e") }

                }
                .addOnFailureListener {
                    Log.d("FireStorage", "errore ")
                }

        }
    }


    fun setTeamProfilePicture(s: String, s1: String) {
        TODO("Not yet implemented")
        /*val t = team.toDTO()
        t.uploadFile(storage.reference)// Register observers to listen for when the download is done or if it fails
            .addOnFailureListener {
                Log.d("FireStorage", "errore ")
            }.addOnSuccessListener { taskSnapshot ->
                Log.d("FireStorage", "file caricato ")
            }*/
    }

    fun updateUser(firstName: String, lastName: String, email: String, location: String) {
        _user.value.firstName = firstName
        _user.value.lastName = lastName
        _user.value.email = email
        _user.value.location = location
        Log.d("UserProfile", "$_user")

        db.collection("users").document(email).set(_user.value)
            .addOnSuccessListener { Log.d("UserProfile", "User profile updated successfully") }
            .addOnFailureListener { e -> Log.e("UserProfile", "Error updating user profile", e) }
    }


    /*USERVIEW mutable state*/
    var firstNameValue = mutableStateOf(user.value.firstName)
    var lastNameValue = mutableStateOf(user.value.lastName)
    var locationValue = mutableStateOf(user.value.location)


}


class ChatModel(
    val currentUser: MutableStateFlow<User>,
    val currentTeamId: MutableStateFlow<String>,
    val db: FirebaseFirestore
) {
    // First get chats of team, second get my chats
    fun fetchChats(teamId: String, userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = db.collection("chats")  //TODO: Attento ai fetch concatenati usa una transiction è più efficiente e semplice
            .whereEqualTo("teamId", teamId)
            .where(
                Filter.or(
                    Filter.equalTo("user1Id", currentUser.value.email),
                    Filter.equalTo("user2Id", currentUser.value.email)
                )
            )
            .addSnapshotListener { r, e ->
                if (r != null) {
                    val chats = mutableListOf<Chat>()

                    for (document in r) {
                        val chat = document.toObject(Chat::class.java)

                        Log.d("Chat", "Fetching chat ${document.id} => ${document.data}")
                        document.reference.collection("messages").get().addOnSuccessListener {
                            val messages = it.toObjects(ChatMessage::class.java)
                            chat.messages = messages.toMutableList()

                            chats.add(chat)
                            trySend(chats)
                        }
                    }

                } else {
                    Log.d("Chat", "Error getting private chats: ", e)
                }
            }

        awaitClose { listener.remove() }
    }

    fun fetchChat(teamId: String, destUserId: String): Flow<Chat> = callbackFlow {
        Log.d("chat", "Fetching single chat of $destUserId")
        val listener = db.collection("chats")  //TODO: Attento ai fetch concatenati usa una transiction è più efficiente e semplice
            .whereEqualTo("teamId", teamId)
            .where(
                Filter.or(
                    Filter.equalTo("user1Id", currentUser.value.email),
                    Filter.equalTo("user2Id", currentUser.value.email)
                )
            )
            .where(
                Filter.or(
                    Filter.equalTo("user1Id", destUserId),
                    Filter.equalTo("user2Id", destUserId)
                )
            )
            .addSnapshotListener { r, e ->
                if (r != null && r.size() > 0) {
                    val document = r.first()
                    val chat = document.toObject(Chat::class.java)

                    Log.d("Chat", "Fetching chat ${document.id} => ${document.data}")
                    document.reference.collection("messages").get().addOnSuccessListener {
                        val messages = it.toObjects(ChatMessage::class.java)
                        chat.messages = messages.toMutableList()

                        trySend(chat)
                    }


                } else {
                    Log.d("Chat", "Error getting private chats: ", e)
                }
            }

        awaitClose { listener.remove() }
    }

    fun newChat(destUserId: String) {
        val chatData = hashMapOf(
            "teamId" to currentTeamId.value,
            "user1Id" to currentUser.value.email,
            "user2Id" to destUserId
        )

        db.collection("chats")
            .document()
            .set(chatData)
            .addOnSuccessListener {
                Log.d("chat", "Chat creata con successo")
            }
            .addOnFailureListener { e ->
                Log.d("chat", "Errore nella creazione della chat: $e")
            }
    }


    fun sendMessage(destUserId: String, message: ChatMessage) {
        val chatUsers = listOf(currentUser.value.email, destUserId)
        val newMessage = hashMapOf(
            "id" to "",
            "authorId" to currentUser.value.email,
            "text" to message.text,
            "seenBy" to mutableListOf<String>(),
            "timestamp" to message.timestamp
        )

        db.collection("chats")       //TODO: anche qui fetch concatenate da controllare
            .whereEqualTo("teamId", currentTeamId.value)
            .whereIn("user1Id", chatUsers)
            .whereIn("user2Id", chatUsers)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.isEmpty) {
                    Log.d("chat", "No collection messages inside chat!!!")
                } else {
                    val chatDocId = doc.documents[0].id

                    val docRef = db.collection("chats")
                        .document(chatDocId)
                        .collection("messages")
                        .document()

                    newMessage["id"] = docRef.id

                    docRef.set(newMessage)
                        .addOnSuccessListener {
                            Log.d("chat", "Message added successfully with ID: ${docRef.id}")
                        }.addOnFailureListener { e ->
                            Log.d("chat", "Error adding message: $e")
                        }
                }
            }

    }

    fun editMessage(destUserId: String, messageId: String, newText: String) {
        //_chats.value[destUser]?.find { it.id == messageId }?.text = newText
    }

    fun deleteMessage(destUserId: String, messageId: String) {   //TODO: anche qui fetch concatenate da controllare se quella sopra viene eseguita ma sotto no non viene eseguito il rollback
        val chatUsers = listOf(currentUser.value.email, destUserId)

        db.collection("chats")
            .whereEqualTo("teamId", currentTeamId.value)
            .whereIn("user1Id", chatUsers)
            .whereIn("user2Id", chatUsers)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.isEmpty) {
                    Log.d("chat", "No collection messages inside chat!!!")
                } else {
                    val chatDocId = doc.documents[0].id

                    db.collection("chats")
                        .document(chatDocId)
                        .collection("messages")
                        .document(messageId)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("chat", "Message deleted successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.d("chat", "Error deleting message: $e")
                        }
                }
            }
    }

    fun setMessageAsSeen(destUserId: String, messageId: String) { //TODO: anche qui fetch concatenate da controllare
        val chatUsers = listOf(currentUser.value.email, destUserId)

        db.collection("chats")
            .whereEqualTo("teamId", currentTeamId.value)
            .whereIn("user1Id", chatUsers)
            .whereIn("user2Id", chatUsers)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.isEmpty) {
                    Log.d("chat", "No collection messages inside chat!!!")
                } else {
                    val chatDocId = doc.documents[0].id

                    db.collection("chats")
                        .document(chatDocId)
                        .collection("messages")
                        .document(messageId)
                        .update("seenBy", FieldValue.arrayUnion(currentUser.value.email))
                        .addOnSuccessListener {
                            Log.d("chat", "Private message seen successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.d("chat", "Error seeing private message: $e")
                        }
                }
            }
    }

    fun countUnseenChatMessages(destUserId: String): Flow<Int> = callbackFlow {//TODO: anche qui fetch concatenate da controllare
        var count = 0
        val chatUsers = listOf(currentUser.value.email, destUserId)

        val listener = db.collection("chats")
            .whereEqualTo("teamId", currentTeamId.value)
            .whereIn("user1Id", chatUsers)
            .whereIn("user2Id", chatUsers)
            .addSnapshotListener { r, e ->
                if (r != null) {
                    val chats = mutableListOf<Chat>()

                    for (document in r) {
                        val chat = document.toObject(Chat::class.java)

                        Log.d("Chat", "${document.id} => ${document.data}")
                        document.reference.collection("messages").get().addOnSuccessListener {
                            val messages = it.toObjects(ChatMessage::class.java)
                            for (m in messages)
                                if (!m.seenBy.contains(currentUser.value.email))
                                    count++
                            trySend(count)
                        }
                    }

                } else {
                    Log.d("Chat", "Error getting private chats: ", e)
                }
            }

        awaitClose { listener.remove() }
    }

    fun sendTestMessage() {
//        val list = listOf("Ciao bel maschione", "Smettila di drogarti", "Mandami il tuo numero di conto corrente, serve per salvare il paese", "Aiuto sono stato rapito.......dal tuo sguardo pupa", "Mattarella non esiste, sono il suo sostituto robotico, MatTechRella", "Fi falve buonafera, fono proprio io Fergione")
//        val randomIndex = Random.nextInt(list.size)
//        val randomElement = list[randomIndex]
//
//        _chats.value[currentTeam.value]?.entries?.find {
//            it.key.first == currentUser.value && it.key.second == userList[1] || it.key.first == userList[1] && it.key.second == currentUser.value
//        }?.value?.add(ChatMessage(randomElement, userList[1], LocalDateTime.now(), mutableListOf(userList[1])))
    }

    fun fetchGroupChat(teamId: String): Flow<GroupChat> = callbackFlow {//TODO: anche qui fetch concatenate da controllare
        val listener = db.collection("groupChats")
            .whereEqualTo("teamId", teamId)
            .addSnapshotListener { querySnapshot, exception ->
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    // Group chat exists, retrieve the document
                    val chatDocument = querySnapshot.documents.first()
                    val groupChat = chatDocument.toObject(GroupChat::class.java)

                    if (groupChat != null) {
                        // Aggiungiamo un listener alla collezione "messages"
                        val secondListener = chatDocument.reference.collection("messages")
                            .addSnapshotListener { messagesSnapshot, messagesException ->
                                if (messagesException != null) {
                                    Log.d("Chat", "Error fetching chat message: $exception")
                                }

                                val messages = messagesSnapshot?.toObjects(ChatMessage::class.java)
                                    ?: mutableListOf()
                                groupChat.messages = messages.toMutableList()
                                Log.d("chat", "messages: ${groupChat.messages.size}")
                                trySend(groupChat)
                            }
                    } else {
                        Log.d("Chat", "Error fetching chat message: $exception")
                    }
                } else {
                    Log.d("Chat", "Error fetching chat message: $exception")
                }
            }
        awaitClose { listener.remove() }
    }

    fun sendGroupMessage(newMessage: ChatMessage) {//TODO: anche qui fetch concatenate da controllare
        val messageToAdd = hashMapOf(
            "id" to "",
            "authorId" to currentUser.value.email,
            "text" to newMessage.text,
            "seenBy" to mutableListOf<String>(),
            "timestamp" to Timestamp.now()
        )

        // Add the new message to the chat
        fun addMsg() {
            db.collection("groupChats").whereEqualTo("teamId", currentTeamId.value).get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        // Supponiamo che ci sia solo una chat per teamId
                        val chatDocument = querySnapshot.documents.first()

                        // Aggiungi il nuovo messaggio alla collezione "messages" di questo documento
                        chatDocument.reference.collection("messages")
                            .add(messageToAdd)
                            .addOnSuccessListener {
                                it.update("id", it.id)
                                Log.d("Chat", "Message added successfully!")
                            }.addOnFailureListener { e -> Log.d("Chat", "Error adding message: ", e) }
                    }
                }.addOnFailureListener { e -> Log.d("Chat", "Error getting chats: ", e) }
        }

        // If no chat exists for this team, create it
        db.collection("groupChats").whereEqualTo("teamId", currentTeamId.value).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) { // Create the chat
                    val chatData = hashMapOf("teamId" to currentTeamId.value)

                    db.collection("groupChats").document().set(chatData)
                        .addOnSuccessListener {
                            Log.d("Chat", "Group chat created successfully!")
                            addMsg()
                        }.addOnFailureListener { e -> Log.d("Chat", "Error creating group chat: ", e) }
                } else addMsg()
            }.addOnFailureListener { e -> Log.d("Chat", "Error getting chats: ", e) }
    }

    fun editGroupMessage(messageId: String, newText: String) {
        //_groupChat.value[messageId.toInt()].text = newText
    }

    fun deleteGroupMessage(messageId: String) {//TODO: anche qui fetch concatenate da controllare
        db.collection("groupChats")
            .whereEqualTo("teamId", currentTeamId.value)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.isEmpty) {
                    Log.d("chat", "No collection messages inside chat!!!")
                } else {
                    val chatDocId = doc.documents[0].id

                    db.collection("groupChats")
                        .document(chatDocId)
                        .collection("messages")
                        .document(messageId)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("chat", "Group message deleted successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.d("chat", "Error deleting group message: $e")
                        }
                }
            }
    }

    fun setGroupMessageAsSeen(messageId: String) {//TODO: anche qui fetch concatenate da controllare
        Log.d("chat", "Provo a visualizzare il group message $messageId")
        db.collection("groupChats")
            .whereEqualTo("teamId", currentTeamId.value)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.isEmpty) {
                    Log.d("chat", "No collection messages inside chat!!!")
                } else {
                    val chatDocId = doc.documents[0].id

                    db.collection("groupChats")
                        .document(chatDocId)
                        .collection("messages")
                        .whereEqualTo("id", messageId)
                        .get()
                        .addOnSuccessListener {
                            if (!it.isEmpty) {
                                db.collection("groupChats")
                                    .document(chatDocId)
                                    .collection("messages")
                                    .document(messageId)
                                    .update("seenBy", FieldValue.arrayUnion(currentUser.value.email))
                                    .addOnSuccessListener {
                                        Log.d("chat", "Group message seen successfully!")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d("chat", "Error seeing group message: $e")
                                    }
                            } else {
                                Log.d("chat", "Error looking for message to see $it")
                            }
                        }.addOnFailureListener {
                            Log.d("chat", "Error looking for message to see $it")
                        }
                }
            }
    }

    fun countUnseenGroupMessages(): Flow<Int> = callbackFlow {//TODO: anche qui fetch concatenate da controllare
        var count = 0
        val listener = db.collection("groupChats")
            .whereEqualTo("teamId", currentTeamId.value)
            .addSnapshotListener { querySnapshot, exception ->
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    // Group chat exists, retrieve the document
                    val chatDocument = querySnapshot.documents.first()

                    // Aggiungiamo un listener alla collezione "messages"
                    val secondListener = chatDocument.reference.collection("messages")
                        .addSnapshotListener { messagesSnapshot, messagesException ->
                            if (messagesException != null) {
                                Log.d("Chat", "Error fetching chat message: $exception")
                            }

                            val messages = messagesSnapshot?.toObjects(ChatMessage::class.java)
                                ?: mutableListOf()

                            for (m in messages)
                                if (!m.seenBy.contains(currentUser.value.email))
                                    count++

                            trySend(count)
                        }
                } else {
                    Log.d("Chat", "Error fetching chat message: $exception")
                }
            }
        awaitClose { listener.remove() }// TODO : PERChE' qui awaitClose deve stare solo dentro callbackflow
        Log.d("chat", "Model found $count unseen group messages")
    }

    fun countAllUnseenMessages(): Int {
//        var count = 0;
//        for (chat in _chats.value[currentTeam.value]?.entries?.filter { it.key.first == currentUser.value || it.key.second == currentUser.value }!!) {
//            val destUser = if (chat.key.first == currentUser.value) chat.key.second else chat.key.first
//            count += countUnseenChatMessages(destUser)
//        }
//        count += countUnseenGroupMessages()
//        return count
        return 0
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
