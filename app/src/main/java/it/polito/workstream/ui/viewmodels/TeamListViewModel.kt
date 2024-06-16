package it.polito.workstream.ui.viewmodels

import androidx.lifecycle.ViewModel
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KFunction1

class TeamListViewModel(
    val activeTeam: Flow<Team?>,
    val teams: Flow<List<Team>>,
    val teamTasks: Flow<List<Task>>,
    val teamMembers: Flow<List<User>>,
    val activePageValue: MutableStateFlow<String>,
    val setActivePage: (page: String) -> Unit,
    val changeActiveTeamId: (teamId: String) -> Unit,
    val removeTeam: (teamId: String) -> Unit,
    val leaveTeam: (teamId: String, userId: String) -> Unit,
    val joinTeam: (teamId: String, userId: String) -> Unit,
    val createEmptyTeam: (nameTeam: String) -> Result<String>,
    val fetchActiveTeam: (String) -> Flow<Team?>,
    val user: StateFlow<User>,
    val activeTeamId: MutableStateFlow<String>,

    ) : ViewModel()