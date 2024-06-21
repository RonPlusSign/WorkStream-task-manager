package it.polito.workstream.ui.viewmodels

import android.graphics.Bitmap
import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.polito.workstream.ChatModel
import it.polito.workstream.ui.models.Chat
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class UserViewModel(
    val user: User,
    userTasksFlow: Flow<List<Task>>,
    val teamMembersFlow: Flow<List<User>>,
    activeTeamFlow: Flow<Team?>,
    val chatModel: ChatModel,
    val updateUser: (firstName: String, lastName: String, email: String, location: String) -> Unit,
    fetchActiveTeam: (String) -> Flow<Team?>,
    val activeTeamId: MutableStateFlow<String>,
    fetchUsers: (String) -> Flow<List<User>>,
    val getTasks: (String) -> Flow<List<Task>>,
    var firstNameValue: MutableState<String>,
    var lastNameValue: MutableState<String>,
    var locationValue: MutableState<String?>,
    val uploaUserdPhoto: (User) -> Unit
) : ViewModel() {

    val activeTeam = fetchActiveTeam(activeTeamId.value)
    val teamMembers = fetchUsers(activeTeamId.value)
    val userTasks = userTasksFlow.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList())


    var isEditing by mutableStateOf(false)
        private set

    fun edit() {
        isEditing = true

        // Save current values before editing
        firstNameBeforeEdit = firstNameValue.value
        lastNameBeforeEdit = lastNameValue.value
        emailBeforeEdit = emailValue
        locationBeforeEdit = locationValue.value
    }

    fun save(firstname:String,lastName: String, location: String) {
        firstNameValue.value = firstname
        lastNameValue.value = lastName
        locationValue.value = location
        validate()
        if (firstNameError.isBlank() && lastNameError.isBlank() && emailError.isBlank()) {
            updateUser(firstNameValue.value, lastNameValue.value, emailValue, locationValue.value ?: "")
        }
    }

    /* Check if all fields are valid, and if so, stop editing */
    fun validate() {
        // Trim all fields
        firstNameValue.value = firstNameValue.value.trim()
        lastNameValue.value = lastNameValue.value.trim()
        emailValue = emailValue.trim()
        locationValue.value = locationValue.value?.trim()

        // Check if all fields are valid
        checkFirstName()
        checkLastName()
        checkEmail()
        Log.d("User", "$user")
        // if all fields are valid, stop editing
        if (firstNameError.isBlank() && lastNameError.isBlank() && emailError.isBlank()) {
            isEditing = false
        }
    }

    fun discard() {
        firstNameValue.value = firstNameBeforeEdit
        lastNameValue.value = lastNameBeforeEdit
        emailValue = emailBeforeEdit
        locationValue.value = locationBeforeEdit
        isEditing = false
    }

    /* First name */

    var firstNameError by mutableStateOf("")
        private set

    private var firstNameBeforeEdit by mutableStateOf("")

    fun setFirstName(n: String) {
        firstNameValue.value = n
    }

    private fun checkFirstName() {
        firstNameError = if (firstNameValue.value.isBlank()) "First name cannot be blank" else ""
    }

    /* Last name */

    var lastNameError by mutableStateOf("")
        private set

    private var lastNameBeforeEdit by mutableStateOf("")

    fun setLastName(n: String) {
        lastNameValue.value = n
    }

    private fun checkLastName() {
        lastNameError = if (lastNameValue.value.isBlank()) "Last name cannot be blank" else ""
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


    private var locationBeforeEdit: String? by mutableStateOf(null)

    fun setLocation(n: String) {
        locationValue.value = n
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

//    var currentDestUserId by mutableStateOf("")
//        private set
//    fun setCurrDestUser(destUserId: String) {
//        currentDestUserId = destUserId
//    }

    // Chats
    val chats = fetchChats(activeTeamId.value, user.email)
    fun fetchChats(teamId: String, userId: String): Flow<List<Chat>> = chatModel.fetchChats(teamId, userId)
    //val currentChat = fetchChat(currentDestUserId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    fun fetchChat(destUserId: String) = chatModel.fetchChat(activeTeamId.value, destUserId)
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
        firstNameValue.value = user.firstName
        lastNameValue.value = user.lastName
        emailValue = user.email
        locationValue.value = user.location
        profilePictureValue = user.profilePicture
        photoBitmapValue = user.BitmapValue
        numberOfTeams = user.teams.size
        tasksCompleted = userTasks.value.filter { it.completed }.size
        tasksToComplete = userTasks.value.filter { !it.completed }.size
    }
}