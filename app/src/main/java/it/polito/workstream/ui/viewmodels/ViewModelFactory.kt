package it.polito.workstream.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.polito.workstream.MainApplication

class ViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val app = (context.applicationContext as? MainApplication) ?: throw IllegalArgumentException("Bad Application class")

    private val  activeTeam  = app.activeTeam
    private val  user = app.user
    private val  fetchUsers = app::fetchUsers
    private val  getTeams = app::getTeams
    private val  getTasks = app::getTasks
    private val  createTask = app::createTask
    private val  createTeam = app::createTeam

    private val activePageValue = app.activePageValue
    private val setActivePage = app::setActivePage
    private val changeActiveTeamId = app::changeActiveTeamId

    /*private val tasksList: MutableStateFlow<MutableList<Task>> = app.tasksList
    private val userList: StateFlow<List<User>> = app.userList
    private val teams = app.teams
    private val addTeam = app::addTeam
    private val removeTeam = app::removeTeam
    private val leaveTeam = app::leaveTeam
    private val activeTeam = app.activeTeam
    private val sections: SnapshotStateList<String> = app.getSectionsOfTeam(activeTeam.value.id)
    private val activePageValue = app.activePageValue
    private val setActivePage = app::setActivePage
    private val changeActiveTeamId = app::changeActiveTeamId
    private val user = app.user
    private val currentSortOrder = app.currentSortOrder
    private val setSortOrder = app::setSortOrder
    private val filterParams = app.filterParams
    private val searchQuery = app.searchQuery
    private val setSearchQuery = app::setSearchQuery
    private val createEmptyTeam = app::createEmptyTeam
    private val teamIdsetProfileBitmap = app::teamIdsetProfileBitmap
    private val teamIdsetProfilePicture = app::teamIdsetProfilePicture
    private val chatModel = app.chatModel
    private val removeMemberFromTeam = app::removeMemberFromTeam
    private val editUser = app::editUser

    private val teamsasdasd = app.teamsasdasd*/

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TeamViewModel::class.java) -> user.value?.let {
                TeamViewModel(
                    activeTeam,
                    it,
                    teamIdsetProfileBitmap,
                    teamIdsetProfilePicture,
                    removeMemberFromTeam
                )
            } as T

            modelClass.isAssignableFrom(UserViewModel::class.java) -> user.value?.let {
                UserViewModel(
                    it,
                    activeTeam,
                    userList,
                    chatModel,
                    editUser,
                    fetchUsers
                )
            } as T

            modelClass.isAssignableFrom(TaskListViewModel::class.java) -> TaskListViewModel(
                tasksList,
                sections,
                activePageValue,
                setActivePage,
                currentSortOrder,
                setSortOrder,
                filterParams,
                searchQuery,
                setSearchQuery
            ) as T

            modelClass.isAssignableFrom(TeamListViewModel::class.java) ->
                TeamListViewModel(
                    activeTeam,
                    getTasks,
                    activePageValue,
                    setActivePage,
                    changeActiveTeamId
                ) as T
                /*TeamListViewModel(
                teams,
                addTeam,
                removeTeam,
                activeTeam,
                activePageValue,
                setActivePage,
                changeActiveTeamId,
                leaveTeam,
                searchQuery,
                setSearchQuery,
                createEmptyTeam,
                app::getTeams,
                getTasks
            ) as T*/

            modelClass.isAssignableFrom(TaskViewModel::class.java) -> TaskViewModel(
                sections,
                activeTeam
            ) as T

            else -> throw IllegalArgumentException("ViewModel class not found")
        }
    }
}
