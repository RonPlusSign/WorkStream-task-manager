package it.polito.workstream.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.polito.workstream.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.stateIn

class ViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val app = (context.applicationContext as? MainApplication) ?: throw IllegalArgumentException("Bad Application class")

    private val activeTeam = app.activeTeam
    private val user = app.user
    private val fetchUsers = app::fetchUsers
    private val getTeams = app::getTeams
    private val getTasks = app::getTasks
    private val createTask = app::createTask
    private val createTeam = app::createTeam


    private val activePageValue = app.activePageValue
    private val setActivePage = app::setActivePage
    private val changeActiveTeamId = app::changeActiveTeamId

    private val removeTeam = app::removeTeam
    private val leaveTeam = app::leaveTeam
    private val joinTeam = app::joinTeam
    private val createEmptyTeam = app::createEmptyTeam

    private val onTaskUpdated = app::onTaskUpdated
    private val deleteTask = app::deleteTask
    private val onTaskCreated = app::onTaskCreated
    private val currentSortOrder = app.currentSortOrder
    private val setSortOrder = app::setSortOrder
    private val filterParams = app.filterParams
    private val searchQuery = app.searchQuery
    private val setSearchQuery = app::setSearchQuery

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TeamViewModel::class.java) -> TeamViewModel(
                activeTeam,
                user.value,
                updateTeam = app::updateTeam,
                teamIdsetProfileBitmap = app::setTeamProfileBitmap,
                teamIdsetProfilePicture = app::setTeamProfilePicture,
                removeMemberFromTeam = app::leaveTeam,
            ) as T

            modelClass.isAssignableFrom(UserViewModel::class.java) -> UserViewModel(
                user.value,
                activeTeam,
                chatModel = app.chatModel,
                updateUser = app::updateUser
            ) as T

            modelClass.isAssignableFrom(TaskListViewModel::class.java) ->
                TaskListViewModel(
                    activeTeam,
                    activePageValue,
                    setActivePage,
                    onTaskUpdated,
                    deleteTask,
                    onTaskCreated,
                    getTasks,
                    currentSortOrder,
                    setSortOrder,
                    filterParams,
                    searchQuery,
                    setSearchQuery
                ) as T

            modelClass.isAssignableFrom(TeamListViewModel::class.java) ->
                TeamListViewModel(
                    activeTeam,
                    getTeams,
                    getTasks,
                    activePageValue,
                    setActivePage,
                    changeActiveTeamId,
                    removeTeam,
                    leaveTeam,
                    joinTeam,
                    createEmptyTeam
                ) as T

            modelClass.isAssignableFrom(TaskViewModel::class.java) -> TaskViewModel(activeTeam) as T

            else -> throw IllegalArgumentException("ViewModel class not found")
        }
    }
}
