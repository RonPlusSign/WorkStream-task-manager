package it.polito.workstream.ui.viewmodels

import androidx.lifecycle.ViewModel
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/*

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
    val teamsasdasd: Flow<List<Team>>,
) : ViewModel() {

    fun teamsToDrawerMenu(user:  StateFlow<User?>): List<DrawerMenu> {
        return teams.value.filter { u-> u.members.contains(user.value) }.map { DrawerMenu(Icons.Filled.Face, it.name , it.id.toString(), it.members.size) }
    }

    fun joinTeam(team: Team, user: StateFlow<User?>) {
        user.value?.let { currentUser ->
            if (currentUser != null) {
                team.addMember(currentUser)
            }
            currentUser.teams.add(team)
        }
    }
}*/

class TeamListViewModel(
    val activeTeam: Flow<Team?>,
    val getTask: (teamId: String) -> Flow<List<Task>>,
    val activePageValue: MutableStateFlow<String>,
    val setActivePage: (page: String) -> Unit,
    val changeActiveTeamId: (teamId: Long) -> Unit,
):ViewModel(){

}