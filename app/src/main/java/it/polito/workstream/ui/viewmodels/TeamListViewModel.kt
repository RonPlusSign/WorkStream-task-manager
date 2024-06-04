package it.polito.workstream.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.shared.DrawerMenu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KFunction1


class TeamListViewModel(
    val teams: StateFlow<List<Team>>,
    val addTeam: (Team) -> Unit,
    val removeTeam: (teamId: Long) -> Unit,
    val activeTeam: StateFlow<Team>,
    val activePageValue: MutableStateFlow<String>,
    val setActivePage: (page: String) -> Unit,
    val changeActiveTeamId: KFunction1<Long, Unit>,
    val leaveTeam: (team: Team, user: User) -> Unit,
    val searchQuery: MutableState<String>,
    val setSearchQuery: (newQuery: String) -> Unit,
    val createEmptyTeam: (name: String) -> Unit,
) : ViewModel() {

    fun teamsToDrawerMenu(user:  StateFlow<User>): List<DrawerMenu> {
        return teams.value.filter { u-> u.members.contains(user.value) }.map { DrawerMenu(Icons.Filled.Face, it.name , it.id.toString(), it.members.size) }
    }

    fun joinTeam(team: Team, user: StateFlow<User>) {
        user.value.let { currentUser ->
            team.addMember(currentUser)
            currentUser.teams.add(team)
        }
    }
}