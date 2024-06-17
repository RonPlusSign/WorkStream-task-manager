package it.polito.workstream.ui.viewmodels

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.withContext


class TeamViewModel(
    val _team: Flow<Team?>,
    val _teamMembers: Flow<List<User>>,
    val currentUser: User,
    val updateTeam: (team: Team) -> Unit,
    private val teamIdsetProfileBitmap: (teamId: String, b: Bitmap?) -> Unit,
    private val teamIdsetProfilePicture: (teamId: String, n: String) -> Unit,
    private val removeMemberFromTeam: (teamId: String, userId: String) -> Unit,
    fetchTeam: (String) -> Flow<Team?>,
    activeTeamId: MutableStateFlow<String>,
    fetchUsers: (String) -> Flow<List<User>>,

) : ViewModel() {
    val team = fetchTeam(activeTeamId.value)
    val teamMembers =fetchUsers(activeTeamId.value)

    suspend fun setProfilePicture(n: String) {
        withContext(Dispatchers.IO) { team.firstOrNull()?.let { teamIdsetProfilePicture(it.id, n) } }
    }


    suspend fun setProfileBitmap(b: Bitmap? ) {
        withContext(Dispatchers.IO) { team.firstOrNull()?.let { teamIdsetProfileBitmap(it.id, b) } }
    }

    suspend fun removeMember(memberId: String, teamId: String) {
        withContext(Dispatchers.IO) { removeMemberFromTeam(teamId, memberId) }
    }

    var showEditDialog by mutableStateOf(false)
        private set

    fun edit(nameValue: String) {
        showEditDialog = true
        nameBeforeEdit = nameValue // Save current values before editing
    }

    /* Check if all fields are valid, and if so, stop editing */
    suspend fun save(name: String) {
        val nameValue = name.trim()
        nameError = if (nameValue.isBlank()) "Team name cannot be blank" else ""

        if (nameError.isBlank()) { // if all fields are valid, stop editing
            val t = team.lastOrNull()
            if (t != null) {
                t.name = nameValue
                updateTeam(t)
            }
            showEditDialog = false
        }
    }

    fun discard() {
        showEditDialog = false
    }

    var nameError by mutableStateOf("")
        private set

    private var nameBeforeEdit by mutableStateOf("")

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
}