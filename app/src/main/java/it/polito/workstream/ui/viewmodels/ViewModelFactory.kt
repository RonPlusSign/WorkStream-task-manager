package it.polito.workstream.ui.viewmodels

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.polito.workstream.MainApplication
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val app = (context.applicationContext as? MainApplication) ?: throw IllegalArgumentException("Bad Application class")

    private val tasksList: MutableStateFlow<MutableList<Task>> = app.tasksList
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

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TeamViewModel::class.java) -> user.value?.let {
                TeamViewModel(
                    activeTeam.value,
                    it,
                    teamIdsetProfileBitmap,
                    teamIdsetProfilePicture,
                    removeMemberFromTeam
                )
            } as T

            modelClass.isAssignableFrom(UserViewModel::class.java) -> user.value?.let {
                UserViewModel(
                    it,
                    activeTeam.value.id,
                    userList,
                    chatModel,
                    editUser
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

            modelClass.isAssignableFrom(TeamListViewModel::class.java) -> TeamListViewModel(
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
                createEmptyTeam
            ) as T

            modelClass.isAssignableFrom(TaskViewModel::class.java) -> TaskViewModel(
                sections,
                activeTeam
            ) as T

            else -> throw IllegalArgumentException("ViewModel class not found")
        }
    }
}
