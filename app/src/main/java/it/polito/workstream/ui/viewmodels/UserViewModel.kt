package it.polito.workstream.ui.viewmodels

import android.graphics.Bitmap
import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.polito.workstream.ChatModel
import it.polito.workstream.ui.models.Chat
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.models.GroupChat
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserViewModel(
    val user: User,
    userTasksFlow: Flow<List<Task>>,
    val teamMembersFlow: Flow<List<User>>,
    activeTeamFlow: Flow<Team?>,
    val chatModel: ChatModel,
    val updateUser: (firstName: String, lastName: String, email: String, location: String) -> Unit,
    fetchActiveTeam: (String) -> Flow<Team?>,
    activeTeamId: MutableStateFlow<String>,
    fetchUsers: (String) -> Flow<List<User>>
) : ViewModel() {

    val activeTeam = fetchActiveTeam(activeTeamId.value)
    val teamMembers = fetchUsers(activeTeamId.value)
    val userTasks = userTasksFlow.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList())


    var isEditing by mutableStateOf(false)
        private set

    fun edit() {
        isEditing = true

        // Save current values before editing
        firstNameBeforeEdit = firstNameValue
        lastNameBeforeEdit = lastNameValue
        emailBeforeEdit = emailValue
        locationBeforeEdit = locationValue
    }

    fun save() {
        validate()
        if (firstNameError.isBlank() && lastNameError.isBlank() && emailError.isBlank()) {
            updateUser(firstNameValue, lastNameValue, emailValue, locationValue ?: "")
        }
    }

    /* Check if all fields are valid, and if so, stop editing */
    fun validate() {
        // Trim all fields
        firstNameValue = firstNameValue.trim()
        lastNameValue = lastNameValue.trim()
        emailValue = emailValue.trim()
        locationValue = locationValue?.trim()

        // Check if all fields are valid
        checkFirstName()
        checkLastName()
        checkEmail()

        // if all fields are valid, stop editing
        if (firstNameError.isBlank() && lastNameError.isBlank() && emailError.isBlank()) {
            isEditing = false
        }
    }

    fun discard() {
        firstNameValue = firstNameBeforeEdit
        lastNameValue = lastNameBeforeEdit
        emailValue = emailBeforeEdit
        locationValue = locationBeforeEdit
        isEditing = false
    }

    /* First name */
    var firstNameValue by mutableStateOf(user.firstName)
        private set
    var firstNameError by mutableStateOf("")
        private set

    private var firstNameBeforeEdit by mutableStateOf("")

    fun setFirstName(n: String) {
        firstNameValue = n
    }

    private fun checkFirstName() {
        firstNameError = if (firstNameValue.isBlank()) "First name cannot be blank" else ""
    }

    /* Last name */
    var lastNameValue by mutableStateOf(user.lastName)
        private set
    var lastNameError by mutableStateOf("")
        private set

    private var lastNameBeforeEdit by mutableStateOf("")

    fun setLastName(n: String) {
        lastNameValue = n
    }

    private fun checkLastName() {
        lastNameError = if (lastNameValue.isBlank()) "Last name cannot be blank" else ""
    }

    /* Email */
    var emailValue by mutableStateOf(user.email)
        private set
    var emailError by mutableStateOf("")
        private set

    private var emailBeforeEdit by mutableStateOf("")

    fun setEmail(n: String) {
        emailValue = n
    }

    private fun checkEmail() {
        emailError =
            if (emailValue.isBlank())
                "Email cannot be blank"
            // Use regex to check if email is valid
            else if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches())
                "Invalid email"
            else
                ""
    }

    /* Location, nullable */
    var locationValue: String? by mutableStateOf(user.location)
        private set

    private var locationBeforeEdit: String? by mutableStateOf(null)

    fun setLocation(n: String) {
        locationValue = n
    }

    /* Profile picture */
    var profilePictureValue = user.profilePicture
        private set

    var photoBitmapValue = user.BitmapValue
        private set

    fun setPhotoBitmap(b: Bitmap?) {
        photoBitmapValue = b
    }

    fun setProfilePicture(n: String) {
        profilePictureValue = n
    }

    // Chats
    val chats = fetchChats(activeTeamId.value, user.email).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    fun fetchChats(teamId: String, userId: String): Flow<List<Chat>> = chatModel.fetchChats(teamId, userId)
    fun newChat(destUserId: String) = chatModel.newChat(destUserId)
    fun sendMessage(destUserId: String, message: ChatMessage) = chatModel.sendMessage(destUserId, message)
    fun editMessage(destUserId: String, messageId: String, newText: String) = chatModel.editMessage(destUserId, messageId, newText)
    fun deleteMessage(destUserId: String, messageId: String) = chatModel.deleteMessage(destUserId, messageId)
    fun setMessageAsSeen(destUser: String, messageId: String) = chatModel.setMessageAsSeen(destUser, messageId)
    fun countUnseenChatMessages(destUserId: String) = chatModel.countUnseenChatMessages(destUserId)
//    fun countUnseenChatMessages(destUserId: String): Int {
//        var count = 0
//        Log.d("chat", "Chats è lungo: ${chats.value?.size}")
//        chats.value
//            ?.find { (it.user1Id == user.email && it.user2Id == destUserId) ||  (it.user1Id == destUserId && it.user2Id == user.email) }
//            ?.messages
//            ?.forEach {
//                Log.d("chat", "Ho trovato il messaggio: $it")
//                if (!it.seenBy.contains(user.email))
//                    count++
//            }
//        return count
//    }
    fun sendTestMessage() = chatModel.sendTestMessage()

    var showEditDialog by mutableStateOf(false)
    fun toggleShowEditDialog() {
        showEditDialog = !showEditDialog
    }

    // Group chat
    val groupChat = fetchGroupChat(activeTeamId.value).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    fun fetchGroupChat(activeTeamId: String) = chatModel.fetchGroupChat(activeTeamId)
    fun sendGroupMessage(message: ChatMessage) = chatModel.sendGroupMessage(message)
    fun editGroupMessage(messageId: String, newText: String) = chatModel.editGroupMessage(messageId, newText)
    fun deleteGroupMessage(messageId: String) = chatModel.deleteGroupMessage(messageId)
    fun setGroupMessageAsSeen(messageId: String) = chatModel.setGroupMessageAsSeen(messageId)
    fun countUnseenGroupMessages() = chatModel.countUnseenGroupMessages()
    val unseenGroupMessages = countUnseenGroupMessages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
//    fun countUnseenGroupMessages(): Int {
//        var count = 0
//        groupChat.value?.messages
//            ?.forEach {
//                if (!it.seenBy.contains(user.email))
//                    count++
//            }
//        return count
//    }
//    fun countAllUnseenMessages(members: List<User>): Int {
//        Log.d("chat", "Numero di membri: ${members.size}")
//        var count = 0
//        for (m in members) {
//            count += countUnseenChatMessages(m.email)
//        }
//        val counterone = countUnseenGroupMessages()
//        count += counterone
//        Log.d("chat", "Counterone: $counterone")
//        return count
//    }

    /* Number of teams */
    var numberOfTeams by mutableIntStateOf(user.teams.size)
        private set

    /* Number of tasks completed */
    var tasksCompleted by mutableIntStateOf(userTasks.value.filter { it.completed }.size)
        private set

    /* Number of tasks to complete */
    var tasksToComplete by mutableIntStateOf(userTasks.value.filter { !it.completed }.size)
        private set

    fun setUser(user: User) {
        firstNameValue = user.firstName
        lastNameValue = user.lastName
        emailValue = user.email
        locationValue = user.location
        profilePictureValue = user.profilePicture
        photoBitmapValue = user.BitmapValue
        numberOfTeams = user.teams.size
        tasksCompleted = userTasks.value.filter { it.completed }.size
        tasksToComplete = userTasks.value.filter { !it.completed }.size
    }
}