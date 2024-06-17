package it.polito.workstream

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import it.polito.workstream.ui.models.Chat
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.models.GroupChat
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.models.toDTO
import it.polito.workstream.ui.screens.chats.GroupChat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import kotlin.random.Random


class MainApplication : Application() {
    lateinit var db: FirebaseFirestore
    lateinit var chatModel: ChatModel

    lateinit var context: Context
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        db = Firebase.firestore
        chatModel = ChatModel(_user, activeTeamId, db)
    }

    val _user = MutableStateFlow(User())
    val user: StateFlow<User> = _user

    var activeTeamId = MutableStateFlow("")
     fun fetchActiveTeam(activeTeamId: String): Flow<Team?> =  callbackFlow {
        //val listener = db.collection("Teams").whereEqualTo("id", activeTeamId.value).limit(1)

            Log.d("Firestore1", "Active team ID: ${activeTeamId}")
         if (activeTeamId.isNotEmpty()) {
             db.collection("Teams").document(activeTeamId)
                 .addSnapshotListener { value, error ->
                     if (value != null) {
                         val team = value.toObject(Team::class.java)

                         //activeTeamId.value = team?.id!!
                         //Log.d("Firestore", "Active team ID: ${team.id}")
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

        Log.d("Firestore1", "Active team ID: ${activeTeamId}")
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
                        trySend(emptyList() )
                    }
                }
        }

        awaitClose { }


    }

    val activeTeam = fetchActiveTeam(activeTeamId.value)

    fun getTeams(): Flow<List<Team>> = callbackFlow {
        val userId: String = user.value.email
        Log.d("Firestore email", "User ID: $userId")
        val listener = db.collection("Teams").whereArrayContains("members", userId ).addSnapshotListener { r, e ->

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

    fun getTasks(teamId: String): Flow<List<Task>> = callbackFlow {
        val listener = db.collection("Tasks").whereEqualTo("teamId", teamId).addSnapshotListener { r, e ->
            if (r != null) {
                val tasks = r.toObjects(Task::class.java)
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


        // Create the team in Firestore
        Log.d("Firestore", "newTeam ID: ${newTeam.id} email ${user.value.email }")
        val newTeamRef = db.collection("Teams").document()
        val newTeamId = newTeamRef.id
        newTeam.id = newTeamRef.id // se lo tocchi ti taglio le mani
        val userRef = db.collection("users").document(user.value.email)
        db.runTransaction {
            it.set(newTeamRef, newTeam)
            it.update(userRef, "teams", FieldValue.arrayUnion(newTeamRef.id))
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

}


class ChatModel(
    val currentUser: MutableStateFlow<User>,
    val currentTeamId: MutableStateFlow<String>,
    val db: FirebaseFirestore
) {
//    private val _chats: MutableStateFlow<MutableMap<Team, MutableMap<Pair<User, User>, MutableList<ChatMessage>>>> = MutableStateFlow(
//        mutableStateMapOf(
//            teamList[0] to
//                    mutableStateMapOf(
//                        Pair(userList[0], userList[1]) to mutableStateListOf(
//                            ChatMessage(
//                                "Buongiorno! Quando possiamo organizzare un meeting?",
//                                userList[1],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[0], userList[1])
//                            ),
//                            ChatMessage(
//                                "Buondì, io sono sempre disponibile!",
//                                userList[0],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[0], userList[1])
//                            ),
//                            ChatMessage(
//                                "Perfetto, allora contatto il manager e cerco di programmarlo per la prossima settimana",
//                                userList[1],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[1])
//                            )
//                        ),
//                        Pair(userList[2], userList[3]) to mutableStateListOf(
//                            ChatMessage(
//                                "Hey, non dovresti poter vedere questa chat tu!",
//                                userList[2],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[2], userList[3])
//                            )
//                        ),
//                        Pair(userList[0], userList[2]) to mutableStateListOf(
//                            ChatMessage(
//                                "Mi sono stancato della democrazia",
//                                userList[2],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[0], userList[2])
//                            ),
//                            ChatMessage(
//                                "Dovresti andare un po' in vacanza",
//                                userList[0],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[0], userList[2])
//                            ),
//                            ChatMessage(
//                                "Oppure fare una dittatura",
//                                userList[2],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[0], userList[2])
//                            )
//                        ),
//                        Pair(userList[0], userList[3]) to mutableStateListOf(
//                            ChatMessage(
//                                "Che noia queste chat di prova",
//                                userList[2],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[0], userList[3])
//                            ),
//                            ChatMessage(
//                                "Non me ne parlare",
//                                userList[0],
//                                LocalDateTime.now(),
//                                mutableListOf(userList[0], userList[3])
//                            ),
//                        )
//                    )
//        )
//    )

    //val chats: StateFlow<MutableMap<Team, MutableMap<Pair<User, User>, MutableList<ChatMessage>>>> = _chats

    // First get chats of team, second get my chats
    fun fetchChats(teamId: String, userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = db.collection("chats")
            .whereEqualTo("teamId", teamId)
            .where(Filter.or(
                Filter.equalTo("user1Id", userId),
                Filter.equalTo("user2Id", userId)
            ))
            .addSnapshotListener { r, e ->
                if (r != null) {
                    val chats = mutableListOf<Chat>()

                    for (document in r) {
                        val chat = document.toObject(Chat::class.java)

                        Log.d("Chat", "${document.id} => ${document.data}")
                        document.reference.collection("messages").get().addOnSuccessListener {
                            val messages = it.toObjects(ChatMessage::class.java)
                            chat.messages.addAll(messages)
                        }

                        chats.add(chat)
                    }

                    trySend(chats)
                } else {
                    Log.d("Chat", "Error getting private chats: ", e)
                }
            }

        awaitClose { listener.remove() }
    }

    val chats: Flow<List<Chat>> = fetchChats(currentTeamId.value, currentUser.value.email)

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
            "senderId" to destUserId,
            "text" to message,
            "timestamp" to Timestamp.now()
        )

        db.collection("chats")
            .whereIn("user1Id", chatUsers)
            .whereIn("user2Id", chatUsers)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.isEmpty){
                    Log.d("chat", "No collection messages inside chat!!!")
                } else {
                    val chatDocId = doc.documents[0].id

                    db.collection("chats").document(chatDocId)
                        .collection("messages")
                        .add(newMessage)
                        .addOnSuccessListener { doc ->
                            // Assegna al messaggio lo stesso id del documento
                            doc.update("id", doc.id)
                            Log.d("chat","Message added successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.d("chat","Error adding message: $e")
                        }
                }
            }

    }

    fun editMessage(destUserId: String, messageId: String, newText: String) {
        //_chats.value[destUser]?.find { it.id == messageId }?.text = newText
    }

    fun deleteMessage(destUserId: String, messageId: String) {
        val chatUsers = listOf(currentUser.value.email, destUserId)

        db.collection("chats")
            .whereIn("user1Id", chatUsers)
            .whereIn("user2Id", chatUsers)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.isEmpty){
                    Log.d("chat", "No collection messages inside chat!!!")
                } else {
                    val chatDocId = doc.documents[0].id

                    db.collection("chats")
                        .document(chatDocId)
                        .collection("messages")
                        .document(messageId)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("chat","Message deleted successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.d("chat","Error deleting message: $e")
                        }
                }
            }
    }

    fun setMessageAsSeen(destUser: User, messageId: Long) {
//        _chats.value[currentTeam.value]?.entries?.find {
//            it.key.first == currentUser.value && it.key.second == destUser || it.key.first == destUser && it.key.second == currentUser.value
//        }?.value?.find { it.id == messageId }?.seenBy?.add(currentUser.value)

    }

    fun countUnseenChatMessages(destUser: User): Int {
//        var count = 0
//        _chats.value[currentTeam.value]?.entries?.find {
//            it.key.first == currentUser.value && it.key.second == destUser || it.key.first == destUser && it.key.second == currentUser.value
//        }?.value?.forEach {
//            if(!it.seenBy.contains(currentUser.value))
//                count++
//        }
//        return count
        return 0
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

    private val _groupChats: MutableStateFlow<MutableMap<Team, MutableList<ChatMessage>>> =
        MutableStateFlow(
            mutableStateMapOf(
//            teamList[0] to
//                    mutableStateListOf(
//                        ChatMessage(
//                            "Benvenuti a tutti nella chat di gruppo!",
//                            userList[0],
//                            LocalDateTime.now(),
//                            mutableListOf(userList[0], userList[1], userList[2], userList[3])
//                        ),
//                        ChatMessage(
//                            "Ciao ragazzi!",
//                            userList[1],
//                            LocalDateTime.now(),
//                            mutableListOf(userList[0], userList[1], userList[2], userList[3])
//                        ),
//                        ChatMessage(
//                            "Non scrivete troppi messaggi",
//                            userList[2],
//                            LocalDateTime.now(),
//                            mutableListOf(userList[0], userList[1], userList[2], userList[3])
//                        ),
//                        ChatMessage(
//                            "Sennimondoesistesseunpodibene",
//                            userList[3],
//                            LocalDateTime.now(),
//                            mutableListOf(userList[1], userList[2], userList[3])
//                        ),
//                    ),
//            teamList[1] to
//                    mutableStateListOf(
//                        ChatMessage(
//                            "Un'altra chat di gruppo",
//                            userList[0],
//                            LocalDateTime.now(),
//                            mutableListOf(userList[0], userList[1], userList[2])
//                        ),
//                        ChatMessage(
//                            "Di nuovo",
//                            userList[2],
//                            LocalDateTime.now(),
//                            mutableListOf(userList[1], userList[2])
//                        ),
//                    )
            )
        )
    //val groupChats: StateFlow<MutableMap<Team, MutableList<ChatMessage>>> = _groupChats

    fun fetchGroupChat(teamId: String): Flow<GroupChat> = callbackFlow {
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
    val groupChat = fetchGroupChat("9vJ0F8M8CowyiMiq2Qdc")

    fun sendGroupMessage(newMessage: ChatMessage) {
        val messageToAdd = hashMapOf(
            "authorId" to currentUser.value.email,
            "text" to newMessage.text,
            "timestamp" to Timestamp.now()
        )

        db.collection("groupChats")
            .whereEqualTo("teamId", currentTeamId.value)
            .get()
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
                        }
                        .addOnFailureListener { e ->
                            Log.d("Chat", "Error adding message: ", e)
                        }
                } else {
                    Log.d("Chat", "No chat found with teamId: ${currentTeamId.value}")
                    // Puoi anche gestire il caso in cui non esiste una chat per il teamId
                    // Ad esempio, puoi creare una nuova chat qui se lo desideri
                }
            }
            .addOnFailureListener { e ->
                Log.d("Chat", "Error getting chats: ", e)
            }


    }

    fun editGroupMessage(messageId: String, newText: String) {
        //_groupChat.value[messageId.toInt()].text = newText
    }

    fun deleteGroupMessage(messageId: String) {
        db.collection("groupChats")
            .document(currentTeamId.value)
            .collection("messages")
            .document(messageId)
            .delete()
            .addOnSuccessListener {
                Log.d("chat","Chat message deleted successfully!")
            }
            .addOnFailureListener { e ->
                Log.d("chat","Error deleting chat message: $e")
            }
    }

    fun setGroupMessageAsSeen(messageId: String) {
        //_groupChats.value[currentTeam.value]?.find { it.id == messageId }?.seenBy?.add(currentUser.value)
    }

    fun countUnseenGroupMessages(): Int {
//        Log.d("unseen", currentTeam.value.name)
//        var count = 0
//        _groupChats.value[currentTeam.value]?.forEach {
//            if (!it.seenBy.contains(currentUser.value)) {
//                count++
//            }
//        }
//        return count
        return 0
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
