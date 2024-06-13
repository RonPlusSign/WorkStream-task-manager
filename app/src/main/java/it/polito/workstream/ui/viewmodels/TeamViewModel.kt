package it.polito.workstream.ui.viewmodels

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.flow.Flow


class TeamViewModel(
    val team: Flow<Team?>,
    val currentUser: User,
    private val teamIdsetProfileBitmap: (teamId: Long, b: Bitmap?) -> Unit,
    private val teamIdsetProfilePicture: (teamId: Long, n: String) -> Unit,
    private val removeMemberFromTeam: (teamId: Long, userId: Long) -> Unit
) : ViewModel() {

    fun setProfilePicture(n: String) {
        teamIdsetProfilePicture(team.id, n)
    }

    fun setProfileBitmap(b: Bitmap?) {
        teamIdsetProfileBitmap(team.id, b)
    }

    fun removeMember(memberId: Long, teamId: Long) {
        removeMemberFromTeam(teamId, memberId)
    }

    var showEditDialog by mutableStateOf(false)
        private set

    fun edit() {
        showEditDialog = true
        nameBeforeEdit = nameValue // Save current values before editing
    }

    /* Check if all fields are valid, and if so, stop editing */
    fun save() {
        nameValue = nameValue.trim()
        nameError = if (nameValue.isBlank()) "Team name cannot be blank" else ""

        if (nameError.isBlank()) { // if all fields are valid, stop editing
            team.name = nameValue
            showEditDialog = false
        }
    }

    fun discard() {
        nameValue = nameBeforeEdit
        showEditDialog = false
    }

    /* First name */
    var nameValue by mutableStateOf(team.name)
        private set
    var nameError by mutableStateOf("")
        private set

    private var nameBeforeEdit by mutableStateOf("")

    fun setName(n: String) {
        nameValue = n
    }

    /* Group picture */
    /*var profilePictureValue by mutableStateOf(team.profilePicture)
        private set

    var photoBitmapValue by mutableStateOf(team.profileBitmap)
        private set

    fun setPhotoBitmap(b: Bitmap?) {
        photoBitmapValue = b
        team.profileBitmap = b
    }

    fun setProfilePicture(n: String) {
        profilePictureValue = n
        team.profilePicture = n
    }*/


    /* Number of members */
    var numMembers by mutableIntStateOf(team.members.size)

    /* Total number of tasks completed */
    var tasksCompleted by mutableIntStateOf(team.tasks.filter { it.completed }.size)
        private set

    /* Number of tasks to complete */
    var tasksToComplete by mutableIntStateOf(team.tasks.size - tasksCompleted)
        private set

    var top3Users by mutableStateOf(team.members.sortedByDescending { it.tasks.filter { it.completed }.size }.take(3))
}