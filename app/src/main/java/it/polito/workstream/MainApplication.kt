package it.polito.workstream

import android.annotation.SuppressLint
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import it.polito.workstream.ui.models.Chat
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.models.Comment
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import java.sql.Timestamp
import java.text.SimpleDateFormat
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
        chatModel = ChatModel(users, teams.value, _user, _activeTeam, db)
    }

    fun fetchUsers(): Flow<List<User>> = callbackFlow {
        val listener = db.collection("users").addSnapshotListener { r, e ->
            if (r != null) {
                val users = r.toObjects(User::class.java)
                trySend(users)
                for (document in r) {
                    Log.d("User", "${document.id} => ${document.data}")
                }
            } else {
                Log.d("User", "Error getting documents: ", e)
            }
        }

        awaitClose { listener.remove() }
    }

    val users: Flow<List<User>> = fetchUsers()  // TODO: ricordati di chiamarla sennò non vedi nulla!

    // Insert initial data here, to be fetched in the view model factory
    var _userList = MutableStateFlow(
        mutableListOf(
            User(
                firstName = "Cristoforo",
                lastName = "Colombo",
                email = "Cristoforo.Colombo@gmail.com",
                location = "Genova"
            ),
            User(
                firstName = "Sergio",
                lastName = "Mattarella",
                email = "Sergio.Mattarella@gmail.com",
                location = "Roma"
            ),
            User(
                firstName = "Antonio",
                lastName = "Giovanni",
                email = "Antonio.Giovanni@gmail.com",
                location = "Torino"
            ),
            User(
                firstName = "Giovanni",
                lastName = "Malnati",
                email = "Giovanni.Malnati@gmail.com",
                location = "Milano",
                teams = mutableStateListOf()
            ),
        )
    )
        private set

    val userList: StateFlow<List<User>> = _userList

    private fun getUserByName(name: String): User {
        return _userList.value.filter { (it.firstName + " " + it.lastName) == name }[0]
    }

    @SuppressLint("SimpleDateFormat")
    var _tasksList = MutableStateFlow(
        mutableListOf(
            // Task list from tasksList
            Task(
                "Test this app",
                assignee = getUserByName("Cristoforo Colombo"),
                attachments = mutableStateListOf("attachment1", "attachment2", "attachment3"),
                comments = mutableStateListOf(Comment(text = "prova", author = "chris")),
                history = mutableStateMapOf(
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "Created",
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "To do",
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "In progress"
                ),
                status = "To do",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-06-01")
                    ?.let { Timestamp(it.time) },
            ),
            Task(
                "Finish the project ASAP please",
                assignee = getUserByName("Cristoforo Colombo"),
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                history = mutableStateMapOf(
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "Created",
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "In progress"
                ),
                status = "In progress",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-06-10")
                    ?.let { Timestamp(it.time) }
            ),
            Task(
                "Submit the project",
                assignee = getUserByName("Giovanni Malnati"),
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                history = mutableStateMapOf(),
                status = "To do",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-06-20")
                    ?.let { Timestamp(it.time) },
            ),
            Task(
                "Buy apples",
                assignee = getUserByName("Cristoforo Colombo"),
                section = "Personal",
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                history = mutableStateMapOf(),
                status = "To do",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-05-15")
                    ?.let { Timestamp(it.time) },
            ),
            Task(
                "Clean the house",
                assignee = getUserByName("Sergio Mattarella"),
                section = "Personal",
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                history = mutableStateMapOf(),
                status = "In progress",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")
                    ?.let { Timestamp(it.time) },
            ),
            Task(
                "Organize meeting",
                assignee = getUserByName("Cristoforo Colombo"),
                section = "Work",
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                history = mutableStateMapOf(),
                status = "Paused",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-05-21")
                    ?.let { Timestamp(it.time) },
            ),
            Task(
                "End of April recap meeting",
                assignee = getUserByName("Cristoforo Colombo"),
                section = "Work",
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                status = "Completed",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-04-27")
                    ?.let { Timestamp(it.time) },
                completed = true,
            ),
            // Task list from tasksList1
            Task(
                "Test this app111",
                assignee = getUserByName("Cristoforo Colombo"),
                attachments = mutableStateListOf("attachment1", "attachment2", "attachment3"),
                comments = mutableStateListOf(Comment(text = "prova", author = "chris")),
                history = mutableStateMapOf(
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "Created",
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "To do",
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "In progress"
                ),
                status = "To do",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-06-01")
                    ?.let { Timestamp(it.time) },
            ),
            Task(
                "Finish the project1",
                assignee = getUserByName("Cristoforo Colombo"),
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                history = mutableStateMapOf(
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "Created",
                    Timestamp(SimpleDateFormat("yyyy-MM-dd").parse("2024-05-10")!!.time) to "In progress"
                ),
                status = "In progress",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-06-10")
                    ?.let { Timestamp(it.time) },
            ),
            Task(
                "Submit the project1",
                assignee = getUserByName("Giovanni Malnati"),
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                history = mutableStateMapOf(),
                status = "To do",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-06-20")
                    ?.let { Timestamp(it.time) },
            ),
            Task(
                "Buy apples1",
                assignee = getUserByName("Cristoforo Colombo"),
                section = "Funny ideas",
                attachments = mutableStateListOf(),
                comments = mutableStateListOf(),
                history = mutableStateMapOf(),
                status = "To do",
                dueDate = SimpleDateFormat("yyyy-MM-dd").parse("2024-05-15")
                    ?.let { Timestamp(it.time) },
            ),
        )
    )
        private set

    var tasksList: MutableStateFlow<MutableList<Task>> = _tasksList

    fun getTaskById(id: Long): Task {
        return tasksList.value.find { it.id == id }!!
    }

    fun getTasksListOfTeam(teamId: Int): MutableStateFlow<MutableList<Task>> {
        return tasksList   // TODO: Filter tasks by team
    }

    @SuppressLint("MutableCollectionMutableState")
    var _teams = MutableStateFlow(
        mutableListOf(
            Team(
                name = "Dream Team",
                members = mutableListOf(
                    getUserByName("Cristoforo Colombo"),
                    getUserByName("Sergio Mattarella"),
                    getUserByName("Antonio Giovanni"),
                    getUserByName("Giovanni Malnati")
                ),
                tasks = mutableListOf(
                    getTaskById(1),
                    getTaskById(2),
                    getTaskById(3),
                    getTaskById(4),
                    getTaskById(5),
                    getTaskById(6),
                    getTaskById(7)
                ),
                sections = mutableListOf("General", "Work", "Personal")
            ),
            Team(
                name = "Another Team",
                members = mutableListOf(
                    getUserByName("Sergio Mattarella"),
                    getUserByName("Giovanni Malnati"),
                    getUserByName("Cristoforo Colombo")
                ),
                tasks = mutableListOf(
                    getTaskById(8),
                    getTaskById(9),
                    getTaskById(10),
                ),
                sections = mutableListOf("General", "Funny ideas")
            )
        )
    )

    val teams: StateFlow<List<Team>> = _teams
    fun getSectionsOfTeam(teamId: Long): SnapshotStateList<String> {
        val s = _teams.value[teamId.toInt()].sections
        return mutableStateListOf(*s.toTypedArray())
    }

    fun addTeam(team: Team) {
        _teams.value.add(team)
    }

    val _activeTeam: MutableStateFlow<Team> = MutableStateFlow(_teams.value[0])
    val activeTeam: StateFlow<Team> = _activeTeam


    fun createEmptyTeam(name: String) {
        val newTeam = Team(name, sections = mutableListOf("General"))
        newTeam.admin = user.value

        user.value?.let { newTeam.addMember(it) }
        user.value?.teams?.add(newTeam)

        _teams.value.add(newTeam)
    }

    fun leaveTeam(team: Team, user: User) {
        // Rimuovi il team dalla lista di team dell'utente
        _userList.value.find { it.id == user.id }?.teams?.remove(team)

        // Rimuovi i task dell'utente associati al team
        _userList.value.find { it.id == user.id }?.tasks?.removeAll(user.tasks.filter { it.team?.id == team.id })

        _tasksList.value.forEach() {
            if (it.team?.id == team.id && it.assignee?.id == user.id) {
                it.assignee = null
            }
        }


        // Rimuovi l'utente dalla lista di membri del team
        _teams.value.find { it.id == team.id }?.members?.remove(user)

        // Cambia il team attivo se necessario
        if (user.teams.isNotEmpty()) {
            changeActiveTeamId(
                _userList.value.find { it.id == user.id }?.teams?.get(0)?.id ?: 0
            )
        } else {
            changeActiveTeamId(0)
        }
    }


    fun removeTeam(teamId: Long) {
        // Rimuovi il team dalla lista di team di ogni membro e rimuovi i task associati al team cancellato
        _teams.value.forEach { team -> Log.d("Team", "teamid: ${team.id}") }
        Log.d("Team", "teamId da rimuovere: $teamId")

        _teams.value.find { it.id == teamId }?.let { team ->
            //val roba =tasksList.value.filter { task -> team.tasks.map { it.id }.contains(task.id) }
            //roba.forEach { Log.d("taskdarimuovere", "taskid: ${it.id}")  }

            team.members.forEach { member ->
                member.teams.remove(team)
                val tasksToRemove = member.tasks.filter { task -> task.team?.id == team.id }
                member.tasks.removeAll(tasksToRemove)
            }
        }
        user.value?.teams?.get(0)?.let { changeActiveTeamId(it.id) }
        _teams.value.remove(_teams.value.find { it.id == teamId }!!)

    }

    fun changeActiveTeamId(teamId: Long) {
        Log.d("Team", "teamId da cambiare: $teamId")
        _activeTeam.value = _teams.value.find { it.id == teamId } ?: _teams.value[0]
        tasksList = MutableStateFlow(activeTeam.value.tasks)
    }

    var activePageValue =
        MutableStateFlow(Route.TeamTasks.name) //by mutableStateOf(Route.TeamTasks.name)
        private set

    fun setActivePage(page: String) {
        activePageValue.value = page
    }

    val _user = MutableStateFlow(User())
    val user: StateFlow<User?> = _user

    fun updateUserInFirestore(user: User) {
        db.collection("users").document(user.email).set(user)
            .addOnSuccessListener {
                Log.d("UserProfile", "User profile updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("UserProfile", "Error updating user profile", e)
            }
    }
    fun editUser(firstName : String, lastName : String, email : String, location : String) {
        _user.value.firstName = firstName
        _user.value.lastName = lastName
        _user.value.location = location
        updateUserInFirestore(_user.value)
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

    fun teamIdsetProfileBitmap(teamId: Long, b: Bitmap?) {
        teams.value.find { it.id == teamId }?.profileBitmap?.value = b
    }

    fun teamIdsetProfilePicture(teamId: Long, n: String) {
        teams.value.find { it.id == teamId }?.profilePicture?.value = n
    }

    fun removeMemberFromTeam(teamId: Long, userId: Long) {
        _teams.value.find { it.id == teamId }?.members?.remove(teams.value.find { it.id == teamId }?.members?.find { it.id == userId })
    }

}


class ChatModel(
    val userList: Flow<List<User>>,
    val teamList: List<Team>,
    val currentUser: MutableStateFlow<User>,
    val currentTeam: MutableStateFlow<Team>,
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

    private fun fetchChats(): Flow<List<Chat>> = callbackFlow {
        val listener = db.collection("chats").addSnapshotListener { r, e ->
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
                Log.d("Chat", "Error getting documents: ", e)
            }
        }

        awaitClose { listener.remove() }
    }
    val chats: Flow<List<Chat>> = fetchChats()

    fun addExampleChatToFirebase() {
        Log.d("aaa", currentUser.value?.firstName + " " + currentUser.value?.lastName + " " + currentUser.value?.email)

        val chatId = currentUser.value?.email + "_" + "" + "_" + "teamIDdiprova"
        val chatData = hashMapOf(
            "teamId" to "teamIDdiprova",
            "user1Id" to currentUser.value?.email,
            "user2Id" to "emaildiprova@gmail.com"
        )

        db.collection("chats")
            .document(chatId)
            .set(chatData)
            .addOnSuccessListener {
                Log.d("chat", "Chat creata con successo")
            }
            .addOnFailureListener { e ->
                Log.d("chat","Errore nella creazione della chat: $e")
            }
    }

    fun newChat(destUser: User) {
        //_chats.value[currentTeam.value]?.put(Pair(currentUser.value, destUser), mutableListOf())
        val chatId = currentUser.value.email + "_" + destUser.email + "_" + currentTeam.value.name
        val chatData = hashMapOf(
            "teamId" to currentTeam.value.name,
            "user1Id" to currentUser.value.email,
            "user2Id" to destUser.email
        )

        db.collection("chats")
            .document(chatId)
            .set(chatData)
            .addOnSuccessListener {
                Log.d("chat", "Chat creata con successo")
            }
            .addOnFailureListener { e ->
                Log.d("chat","Errore nella creazione della chat: $e")
            }
    }

    fun sendMessage(destUser: User, message: ChatMessage) {
//        _chats.value[currentTeam.value]?.entries?.find {
//            it.key.first == currentUser.value && it.key.second == destUser || it.key.first == destUser && it.key.second == currentUser.value
//        }?.value?.add(message)
        //val mychats = chats.filter { it.find { it.user1Id == currentUser.value.email && it.user2Id == destUser.email } != null } } }
    }

    fun editMessage(destUser: User, messageId: Long, newText: String) {
        //_chats.value[destUser]?.find { it.id == messageId }?.text = newText
    }

    fun deleteMessage(destUser: User, messageId: Long) {
//        _chats.value[currentTeam.value]?.entries?.find {
//            it.key.first == currentUser.value && it.key.second == destUser || it.key.first == destUser && it.key.second == currentUser.value
//        }?.value?.removeIf { it.id == messageId }
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

    private val _groupChats: MutableStateFlow<MutableMap<Team, MutableList<ChatMessage>>> = MutableStateFlow(
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
    val groupChats: StateFlow<MutableMap<Team, MutableList<ChatMessage>>> = _groupChats

    fun getGroupChatOfTeam(): MutableStateFlow<MutableList<ChatMessage>?> {
        return MutableStateFlow(_groupChats.value[currentTeam.value])
    }
    fun sendGroupMessage(message: ChatMessage) {
        _groupChats.value[currentTeam.value]?.add(message)
    }

    fun editGroupMessage(messageId: Long, newText: String) {
        //_groupChat.value[messageId.toInt()].text = newText
    }

    fun deleteGroupMessage(messageId: Long) {
        _groupChats.value[currentTeam.value]?.removeIf { it.id == messageId }
    }

    fun setGroupMessageAsSeen(messageId: Long) {
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

    val chatsSearchQuery = mutableStateOf("")
    fun setChatsSearchQuery(newQuery: String) {
        chatsSearchQuery.value = newQuery
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
