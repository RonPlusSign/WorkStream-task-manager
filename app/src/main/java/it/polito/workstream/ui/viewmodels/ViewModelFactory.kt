package it.polito.workstream.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import it.polito.workstream.MainApplication
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val app = (context.applicationContext as? MainApplication) ?: throw IllegalArgumentException("Bad Application class")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TeamViewModel::class.java) -> TeamViewModel(
                app.activeTeam,
                app.activeTeamMembers,
                app.user.value,
                updateTeam = app::updateTeam,
                teamIdsetProfileBitmap = app::setTeamProfileBitmap,
                teamIdsetProfilePicture = app::setTeamProfilePicture,
                removeMemberFromTeam = app::leaveTeam,
                app::fetchActiveTeam,
                app.activeTeamId,
                app::fetchUsers
            ) as T

            modelClass.isAssignableFrom(UserViewModel::class.java) -> UserViewModel(
                app.user.value,
                app.teamTasks.map { it.filter { task -> task.assignee == app.user.value.email } },
                app.activeTeamMembers,
                app.activeTeam,
                chatModel = app.chatModel,
                updateUser = app::updateUser
            ) as T

            modelClass.isAssignableFrom(TaskListViewModel::class.java) ->
                TaskListViewModel(
                    app.activeTeam,
                    app.activeTeamMembers,
                    app.activePageValue,
                    app::setActivePage,
                    app::deleteTask,
                    app::onTaskCreated,
                    app::onTaskUpdated,
                    app::addSectionToTeam,
                    app::removeSectionFromTeam,
                    app.teamTasks,
                    app.currentSortOrder,
                    app::setSortOrder,
                    app.filterParams,
                    app.searchQuery,
                    app::setSearchQuery,
                    app.activeTeamId,
                    app::getTasks
                ) as T

            modelClass.isAssignableFrom(TeamListViewModel::class.java) ->
                TeamListViewModel(
                    app.activeTeam,
                    app.userTeams,
                    app.teamTasks,
                    app.activeTeamMembers,
                    app.activePageValue,
                    app::setActivePage,
                    app::changeActiveTeamId,
                    app::removeTeam,
                    app::leaveTeam,
                    app::joinTeam,
                    app::createEmptyTeam,
                    app::fetchActiveTeam,
                    app.user,
                    app.activeTeamId,
                    app::getTeams
                ) as T

            modelClass.isAssignableFrom(TaskViewModel::class.java) -> TaskViewModel(app.activeTeam) as T

            else -> throw IllegalArgumentException("ViewModel class not found")
        }
    }
}
