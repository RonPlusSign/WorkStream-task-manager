package it.polito.workstream.ui.screens.tasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.viewmodels.TaskListViewModel
import it.polito.workstream.ui.viewmodels.TaskViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

/**
 * Composable function to create a new task.
 * @param saveTask callback function when a new task is created
 * @param changeRoute callback function to change the route
 * @param vm view model for the task
 */
@Composable
fun NewTaskScreen(
    changeRoute: (route: Int, taskId: String?, taskName: String?, userId: Long?, userMail: String?) -> Unit,
    vm: TaskViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    taskListVM: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    saveTask: (Task) -> Unit = taskListVM::onTaskCreated,
) {
    EditTaskScreen(saveTask = saveTask, changeRoute = changeRoute, vm = vm)
}