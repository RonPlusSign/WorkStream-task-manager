package it.polito.workstream.ui.viewmodels

import android.graphics.Bitmap
import android.util.Patterns
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.polito.workstream.ChatModel
import it.polito.workstream.ui.models.ChatMessage
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class UserViewModel(val user: User, activeTeamFlow: Flow<Team?>, val chatModel: ChatModel, val updateUser: (firstName: String, lastName: String, email: String, location: String) -> Unit) : ViewModel() {

    var isEditing by mutableStateOf(false)
        private set

    val activeTeam = activeTeamFlow.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = null)
    val usersList = activeTeam.value?.members ?: emptyList()


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
    var profilePictureValue = mutableStateOf(user.profilePicture)
        private set

    var photoBitmapValue = mutableStateOf<Bitmap?>(user.BitmapValue)
        private set

    fun setPhotoBitmap(b: Bitmap?) {
        photoBitmapValue.value = b
    }

    fun setProfilePicture(n: String) {
        profilePictureValue.value = n
    }

    // Chats
    val chats = chatModel.chats
    fun getChatsOfTeamAndUser(): MutableMap<Pair<User, User>, MutableList<ChatMessage>>? {
//        return chats[activeTeam]?.filter {
//            it.key.first == user || it.key.second == user
//        }?.filter {
//            val firstFullName = it.key.first.firstName + " " + it.key.first.lastName
//            val secondFullName = it.key.second.firstName + " " + it.key.second.lastName
//            firstFullName.contains(chatModel.chatsSearchQuery.value, ignoreCase = true) || secondFullName.contains(chatModel.chatsSearchQuery.value, ignoreCase = true)
//        }?.toMutableMap()
        return null
    }

    val chatty = chatModel.chats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun newChat(user: User) = chatModel.newChat(user)
    fun sendMessage(user: User, message: ChatMessage) = chatModel.sendMessage(user, message)
    fun editMessage(user: User, messageId: Long, newText: String) = chatModel.editMessage(user, messageId, newText)
    fun deleteMessage(user: User, messageId: Long) = chatModel.deleteMessage(user, messageId)
    fun setMessageAsSeen(destUser: User, messageId: Long) = chatModel.setMessageAsSeen(destUser, messageId)
    fun countUnseenChatMessages(destUser: User) = chatModel.countUnseenChatMessages(destUser)
    fun sendTestMessage() = chatModel.sendTestMessage()

    var showEditDialog by mutableStateOf(false)
    fun toggleShowEditDialog() {
        showEditDialog = !showEditDialog
    }

    // Group chat
    val groupChats = chatModel.groupChats.value
    fun getGroupChatsOfTeam(): MutableList<ChatMessage>? {
//        return groupChats[activeTeam]?.filter {
//            val fullName = it.author.firstName + " " + it.author.lastName
//            fullName.contains(chatModel.chatsSearchQuery.value, ignoreCase = true)
//        }?.toMutableList()
        return null
    }

    fun sendGroupMessage(message: ChatMessage) = chatModel.sendGroupMessage(message)
    fun editGroupMessage(messageId: Long, newText: String) = chatModel.editGroupMessage(messageId, newText)
    fun deleteGroupMessage(messageId: Long) = chatModel.deleteGroupMessage(messageId)
    fun setGroupMessageAsSeen(messageId: Long) = chatModel.setGroupMessageAsSeen(messageId)
    fun countUnseenGroupMessages() = chatModel.countUnseenGroupMessages()
    fun countAllUnseenMessages() = chatModel.countAllUnseenMessages()

    /* Number of teams */
    var numberOfTeams by mutableIntStateOf(user.teams.size)
        private set

    /* Number of tasks completed */
    var tasksCompleted by mutableIntStateOf(user.tasks.filter { it.completed && (it.team?.id ?: -1) == activeTeamFlow }.size)
        private set

    /* Number of tasks to complete */
    var tasksToComplete by mutableIntStateOf(user.tasks.filter { !it.completed && (it.team?.id ?: -1) == activeTeamFlow }.size)
        private set

    fun getUsers(): List<User> {
        return usersList
    }


    fun setUser(user: User) {
        firstNameValue = user.firstName
        lastNameValue = user.lastName
        emailValue = user.email
        locationValue = user.location
        profilePictureValue = mutableStateOf(user.profilePicture)
        photoBitmapValue = mutableStateOf(user.BitmapValue)
        numberOfTeams = user.teams.size
        tasksCompleted = user.tasks.filter { it.completed }.size
        tasksToComplete = user.tasks.filter { !it.completed }.size
    }
}