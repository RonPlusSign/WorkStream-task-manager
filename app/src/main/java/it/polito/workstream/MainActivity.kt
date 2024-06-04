package it.polito.workstream

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.chats.Chat
import it.polito.workstream.ui.screens.chats.ChatList
import it.polito.workstream.ui.screens.chats.GroupChat
import it.polito.workstream.ui.screens.chats.NewChat
import it.polito.workstream.ui.screens.tasks.EditTaskScreen
import it.polito.workstream.ui.screens.tasks.NewTaskScreen
import it.polito.workstream.ui.screens.tasks.PersonalTasksScreenWrapper
import it.polito.workstream.ui.screens.tasks.TeamTaskScreenWrapper
import it.polito.workstream.ui.screens.tasks.components.ShowTaskDetails
import it.polito.workstream.ui.screens.team.ConfirmJoinTeamPage
import it.polito.workstream.ui.screens.team.TeamScreen
import it.polito.workstream.ui.screens.userprofile.UserScreen
import it.polito.workstream.ui.shared.BottomNavbarWrapper
import it.polito.workstream.ui.shared.NavDrawer
import it.polito.workstream.ui.shared.TopBarWrapper
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.TaskViewModel
import it.polito.workstream.ui.viewmodels.TeamListViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkStreamTheme {
                ContentView()
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ContentView(
    vm: TeamListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    taskVM: TaskViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {
    val tasksList = vm.activeTeam.collectAsState().value.tasks
    val sections = vm.activeTeam.collectAsState().value.sections

    val navController = rememberNavController()
    val activeTeamId = vm.activeTeam.collectAsState().value.id
    var canNavigateBack: Boolean by remember { mutableStateOf(false) }
    navController.addOnDestinationChangedListener { controller, _, _ ->
        canNavigateBack = controller.previousBackStackEntry != null
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val onItemSelect: (route: Int, taskId: Int?, taskName: String?, userId: Long?) -> Unit = { route: Int, taskId: Int?, taskName: String?, userId: Long? ->

        val routeName = when (route) {
            1 -> when (taskId) {
                null -> "/${activeTeamId}/${Route.TeamTasks.name}"
                else -> "${Route.TeamTasks.name}/${taskId}"
            }

            2 -> Route.MyTasks.name
            3 -> Route.NewTask.name
            4 -> when (taskId) {
                null -> Route.TeamTasks.name
                else -> "${Route.EditTask.name}/${taskId}"
            }

            5 -> Route.TeamScreen.name
            6 -> when (taskId) { //riutilizzo taskId per passare 'id dello user da visualizzare
                null -> Route.TeamScreen.name
                else -> "${Route.UserView.name}/${taskId}"
            }

            7 -> Route.Back.name
            8 -> when (userId) {
                null -> Route.ChatScreen.name
                (-1).toLong() -> "${Route.ChatScreen.name}/group"
                else -> "${Route.ChatScreen.name}/${userId}"
            }

            9 -> Route.NewChat.name

            10 -> Route.UserView.name

            else -> Route.TeamScreen.name
        }

        //vm.setActivePage( taskName ?: routeName)
        if (routeName != Route.Back.name)
            navController.navigate(route = routeName)
        else
            navController.popBackStack()
    }

    val app = (LocalContext.current.applicationContext as? MainApplication) ?: throw IllegalArgumentException("Bad Application class")

    val navigateTo = { s: String ->
        if (s != Route.Back.name) navController.navigate(s) else navController.popBackStack()
    }
    NavDrawer(navigateTo = navigateTo, drawerState = drawerState, activeUser = app.user)
    {
        Scaffold(
            topBar = { Column { TopBarWrapper(drawerState = drawerState, navigateTo = navigateTo) } },
            bottomBar = { BottomNavbarWrapper(navigateTo = navigateTo, teamId = activeTeamId) },
        ) { padding ->
            Surface( // A surface container using the 'background' color from the theme
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), color = MaterialTheme.colorScheme.background
            ) {
                NavHost(navController = navController, startDestination = "/0/${Route.TeamTasks.name}") {

                    composable(
                        route = "/{teamId}/${Route.TeamTasks.name}",
                        arguments = listOf(
                            navArgument("teamId") {
                                type = NavType.LongType
                                nullable = false
                                defaultValue = 0
                            }
                        )
                    ) {
                        vm.changeActiveTeamId(it.arguments?.getLong("teamId") ?: 0)
                        vm.setActivePage(Route.TeamTasks.title)//portare setActivePage in TeamListViewModel
                        TeamTaskScreenWrapper(onItemSelect = onItemSelect)
                    }

                    composable(route = Route.MyTasks.name) {
                        vm.setActivePage(Route.MyTasks.title)
                        PersonalTasksScreenWrapper(onItemSelect = onItemSelect, activeUser = app.user.value.getFirstAndLastName())
                    }

                    composable(route = Route.ChatScreen.name) {
                        vm.setActivePage(Route.ChatScreen.title)
                        ChatList(onChatClick = onItemSelect)
                    }

                    composable(
                        route = "${Route.ChatScreen.name}/{index}",
                        arguments = listOf(
                            navArgument("index") {
                                type = NavType.LongType
                                nullable = false
                                defaultValue = 0
                            }
                        )
                    ) { entry ->
                        val index = entry.arguments?.getLong("index")
                        val destUser = index?.let { vm.activeTeam.value.members.find { it.id == index } }

                        if (destUser != null) {
                            vm.setActivePage(Route.ChatScreen.title + "/" + "${destUser.firstName} ${destUser.lastName}")
                            Chat(destUser)
                        }
                    }

                    composable(route = "${Route.ChatScreen.name}/group") {
                        vm.setActivePage(Route.ChatScreen.title + "/" + vm.activeTeam.collectAsState().value.name)
                        GroupChat()
                    }

                    composable(route = Route.NewChat.name) {
                        vm.setActivePage(Route.NewChat.title)
                        NewChat(onChatClick = onItemSelect)
                    }

                    composable(route = Route.TeamScreen.name) {
                        vm.setActivePage(Route.TeamScreen.title)
                        TeamScreen(
                            onTaskClick = onItemSelect,
                            removeTeam = vm.removeTeam,
                            leaveTeam = vm.leaveTeam,
                            context = LocalContext.current,
                            navController = navController
                        )
                    }

                    composable(route = Route.NewTask.name) {
                        vm.setActivePage(Route.NewTask.title)
                        if (taskVM.task.title != "New Task")
                            taskVM.setTask(Task(title = "New Task", section = sections[0]))
                        NewTaskScreen(changeRoute = onItemSelect, vm = taskVM)
                    }

                    composable(
                        route = "${Route.TeamTasks.name}/{index}",
                        arguments = listOf(
                            navArgument("index") {
                                type = NavType.IntType
                                nullable = false
                                defaultValue = 0
                            }
                        )
                    ) { entry ->
                        val index = entry.arguments?.getInt("index")

                        /*vm.tasksList.find { it.id.toInt() == index }?.let {
                            vm.setActivePage(it.title)
                        }*/
                        tasksList.find { it.id.toInt() == index }?.let {
                            vm.setActivePage(it.title)
                        }

                        ShowTaskDetails(tasksList, index = index ?: 1, onComplete = {
                            it.complete()
                            onItemSelect(1, null, null, null)
                        })
                    }

                    composable(
                        route = "${Route.EditTask.name}/{index}",
                        arguments = listOf(
                            navArgument("index") {
                                type = NavType.IntType
                                nullable = false
                                defaultValue = 0
                            }
                        )
                    ) { entry ->
                        val index = entry.arguments?.getInt("index")
                        val taskEditing = tasksList.find { it.id.toInt() == index }

                        tasksList.find { it.id.toInt() == index }?.let {
                            vm.setActivePage(it.title)
                            if (taskVM.task.id != it.id)
                                taskVM.setTask(it)
                        }

                        if (taskEditing != null) {
                            EditTaskScreen(changeRoute = onItemSelect, vm = taskVM)
                        }
                    }

                    composable(
                        route = "${Route.UserView.name}/{index}",
                        arguments = listOf(
                            navArgument("index") {
                                type = NavType.IntType
                                nullable = false
                                defaultValue = 0
                            }
                        )
                    ) { entry ->
                        vm.setActivePage(Route.UserView.title)
                        val index = entry.arguments?.getInt("index")
                        var user = User()
                        if (index != null) {
                            user = vm.activeTeam.collectAsState().value.members.find { it.id.toInt() == index }!!
                        }
                        UserScreen(user = user, personalInfo = false)
                    }

                    composable(route = Route.UserView.name) {
                        vm.setActivePage(Route.UserView.title)
                        UserScreen(user = app.user.value, personalInfo = true)
                    }

                    composable(
                        "profile?id={teamId}",
                        deepLinks = listOf(navDeepLink { uriPattern = "https://www.workstream.it/{teamId}" }),
                    ) { entry ->
                        val teamId = entry.arguments?.getString("teamId")
                        ConfirmJoinTeamPage(
                            navController = navController,
                            teamId = teamId,
                            onConfirm = { team ->
                                vm.joinTeam(team, app.user)
                                navController.navigate("/${team.id}/${Route.TeamTasks.name}")
                            },
                            onCancel = {
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
        }
    }
}

enum class Route(val title: String) {
    Back(title = "back"),
    MyTasks(title = "My Tasks"),
    TeamTasks(title = "Team Tasks"),
    TeamMembers(title = "Team Members"),
    NewTask(title = "New task"),
    EditTask(title = "Edit task"),
    TeamScreen(title = "Team Members"),
    UserView(title = "User View"),
    ChatScreen(title = "Chat Screen"),
    NewChat(title = "New Chat"),
}
