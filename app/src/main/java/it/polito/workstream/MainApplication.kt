package it.polito.workstream

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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
import kotlinx.coroutines.flow.update
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime

class MainApplication : Application() {
    lateinit var db: FirebaseFirestore

    lateinit var context: Context
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        db = Firebase.firestore
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

    val users = fetchUsers()    // TODO: ricordati di chiamarla sennò non vedi nulla!

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

        newTeam.addMember(user.value)
        user.value.teams.add(newTeam)

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
        changeActiveTeamId(user.value.teams[0].id)
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

    val _user = MutableStateFlow(_userList.value[1])
    val user: StateFlow<User> = _user

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

    val chatModel = ChatModel(_userList.value)

}

class ChatModel(userList: List<User>) {
    private val _chats: MutableStateFlow<MutableMap<User, MutableList<ChatMessage>>> =
        MutableStateFlow(
            mutableStateMapOf(
                (userList[1] to mutableStateListOf(
                    ChatMessage(
                        "Buongiorno! Quando possiamo organizzare un meeting?",
                        userList[1],
                        false,
                        LocalDateTime.now()
                    ),
                    ChatMessage(
                        "Buondì, io sono sempre disponibile!",
                        userList[0],
                        true,
                        LocalDateTime.now()
                    ),
                    ChatMessage(
                        "Perfetto, allora contatto il manager e cerco di programmarlo per la prossima settimana",
                        userList[1],
                        false,
                        LocalDateTime.now()
                    )
                )),
                userList[2] to mutableStateListOf(
                    ChatMessage(
                        "Mi sono stancato della democrazia",
                        userList[2],
                        false,
                        LocalDateTime.now()
                    ),
                    ChatMessage(
                        "Dovresti andare un po' in vacanza",
                        userList[0],
                        true,
                        LocalDateTime.now()
                    ),
                    ChatMessage(
                        "Oppure fare una dittatura",
                        userList[2],
                        false,
                        LocalDateTime.now()
                    )
                )
            )
        )
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

    private val _groupChat: MutableStateFlow<MutableList<ChatMessage>> = MutableStateFlow(
        mutableStateListOf(
            ChatMessage(
                "Benvenuti a tutti nella chat di gruppo!",
                userList[0],
                true,
                LocalDateTime.now()
            ),
            ChatMessage("Ciao ragazzi!", userList[1], false, LocalDateTime.now()),
            ChatMessage(
                "Non scrivete troppi messaggi",
                userList[2],
                false,
                LocalDateTime.now()
            ),
            ChatMessage(
                "Sennimondoesistesseunpodibene",
                userList[3],
                false,
                LocalDateTime.now()
            ),
        )
    )
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
