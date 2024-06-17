package it.polito.workstream

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.workstream.ui.login.LoginActivity
import it.polito.workstream.ui.models.Task
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.chats.*
import it.polito.workstream.ui.screens.tasks.*
import it.polito.workstream.ui.screens.tasks.components.ShowTaskDetails
import it.polito.workstream.ui.screens.team.ConfirmJoinTeamPage
import it.polito.workstream.ui.screens.team.NoTeamsScreen
import it.polito.workstream.ui.screens.team.TeamScreen
import it.polito.workstream.ui.screens.userprofile.UserScreen
import it.polito.workstream.ui.shared.*
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.TaskViewModel
import it.polito.workstream.ui.viewmodels.TeamListViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WorkStreamTheme {
                val currentUser by rememberUpdatedState(newValue = Firebase.auth.currentUser)
                val context = LocalContext.current
                val app = context.applicationContext as MainApplication
                val scope = rememberCoroutineScope()

                var user by remember { mutableStateOf<User?>(null) }

                if (currentUser == null) {
                    // Redirect to LoginActivity if the user is not authenticated
                    LaunchedEffect(Unit) {
                        val loginIntent = Intent(context, LoginActivity::class.java)
                        context.startActivity(loginIntent)
                        Log.d("Merda", "sono finito")
                        finish() // Finish MainActivity so the user cannot go back to it
                    }
                } else {
                    LaunchedEffect(currentUser) {
                        checkOrCreateUserInFirestore(currentUser!!) { retrievedUser ->
                            user = retrievedUser
                            app._user.value = retrievedUser
                            Log.d("user", retrievedUser.toString())
                            if (! retrievedUser.activeTeam.isNullOrEmpty() )
                                app.activeTeamId.value = retrievedUser.activeTeam!!

                        }
                    }
                }

                if (user != null) {
                    ContentView {
                        // Pass logout callback
                        scope.launch {
                            performLogout(context, app)
                        }
                    }
                } else {
                    // Show a loading screen or similar while the user is being initialized
                    LoadingScreen()
                }
            }
        }
    }

    private fun checkOrCreateUserInFirestore(firebaseUser: FirebaseUser, onComplete: (User) -> Unit) {
        Log.d("firebaseUser.email", "${firebaseUser.email}")
        val userRef = firebaseUser.email?.let { db.collection("users").document(it) }
        userRef?.get()?.addOnSuccessListener { document ->
            if (document.exists()) {
                // Ottieni i dati dal documento
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val email = document.getString("email") ?: ""
                val location = document.getString("location")
                var activeTeam = document.getString("activeTeam")
                val teams = document.get("teams") as? MutableList<String> ?: mutableListOf()
                if (activeTeam.isNullOrEmpty() && teams.isNotEmpty() ) {
                    activeTeam = teams[0]
                }
                else if (activeTeam.isNullOrEmpty() && teams.isEmpty()) {
                    activeTeam = "no_team"
                }

                Log.d("UserData", "First Name: $firstName")
                Log.d("UserData", "Last Name: $lastName")
                Log.d("UserData", "Email: $email")
                Log.d("UserData", "Location: $location")
                Log.d("UserData", "Active Team: $activeTeam")
                Log.d("UserData", "Teams: $teams")

                // Crea l'oggetto User
                val user = User(firstName = firstName, lastName = lastName, email = email, location = location, profilePicture = firebaseUser.photoUrl.toString(), activeTeam = activeTeam, teams = teams)

                // Completa l'operazione con il callback
                onComplete(user)
            } else {
                Log.d("ERROR", "User not signed in yet")
                onComplete(User()) // Return a default user object if not found
            }
        }?.addOnFailureListener { e ->
            Log.e("ERROR", "Error fetching user document", e)
            onComplete(User()) // Return a default user object on error
        }
    }

    private fun performLogout(context: android.content.Context, app: MainApplication) {
        Firebase.auth.signOut()

        app._user.value = User()
        //delay(1000)
        val loginIntent = Intent(context, MainActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(loginIntent)
        Log.d("Merda", "sono finito")
        finish()
    }
}

@Composable
fun LoadingScreen() {
    // You can customize this with a proper loading indicator
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // For example, a CircularProgressIndicator in the center of the screen
        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ContentView(
    vm: TeamListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    taskVM: TaskViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    onLogout: () -> Unit
) {

    val activeTeamId = vm.activeTeamId.collectAsState().value //activeTeam.id //vm.activeTeam.collectAsState().value.id
    val activeTeam = vm.fetchActiveTeam(activeTeamId).collectAsState(null).value ?: Team(id = "no_team", name = "", admin = "")
    val tasksList = vm.getTasks(activeTeamId).collectAsState(initial = listOf())//vm.teamTasks.collectAsState(initial = emptyList())
    val sections = activeTeam.sections
    val user by vm.user.collectAsState()
    Log.d("activeTeam", activeTeam.name)
    Log.d("activeTeamId", activeTeamId)

    val navController = rememberNavController()

    var canNavigateBack: Boolean by remember { mutableStateOf(false) }
    navController.addOnDestinationChangedListener { controller, _, _ ->
        canNavigateBack = controller.previousBackStackEntry != null
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val onItemSelect: (route: Int, taskId: String?, taskName: String?, userId: Long?) -> Unit = { route: Int, taskId: String?, taskName: String?, userId: Long? ->

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

    if (activeTeam.id == "no_team") {
        NoTeamsScreen(activeUser = app.user, onJoinTeam = { /* no action needed */ }, addNewTeam = app::createEmptyTeam, navigateToTeam = { navigateTo("profile?id=$it") })
        return
    }

    NavDrawer(navigateTo = navigateTo, drawerState = drawerState, activeUser = app.user) {
        Scaffold(
            topBar = { Column { TopBarWrapper(drawerState = drawerState, navigateTo = navigateTo) } },
            bottomBar = { BottomNavbarWrapper(navigateTo = navigateTo, teamId = activeTeamId) },
        ) { padding ->
            Surface( // A surface container using the 'background' color from the theme
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), color = MaterialTheme.colorScheme.background
            ) {
                NavHost(navController = navController, startDestination = "/${activeTeamId.ifBlank { "no_team" }}/${Route.TeamTasks.name}") {

                    composable(
                        route = "/{teamId}/${Route.TeamTasks.name}",
                        arguments = listOf(navArgument("teamId") { type = NavType.StringType; nullable = false; defaultValue = "" })
                    ) {
                        //TODO: questo manda tutto a puttane
                        vm.changeActiveTeamId(it.arguments?.getString("teamId") ?: "")
                        vm.setActivePage(Route.TeamTasks.title)
                        TeamTaskScreenWrapper(onItemSelect = onItemSelect)
                    }

                    composable(route = Route.MyTasks.name) {
                        vm.setActivePage(Route.MyTasks.title)
                        PersonalTasksScreenWrapper(onItemSelect = onItemSelect, activeUser = app.user.value.email)
                    }

                    composable(route = Route.ChatScreen.name) {
                        vm.setActivePage(Route.ChatScreen.title)
                        ChatList(onChatClick = onItemSelect)
                    }

                    composable(
                        route = "${Route.ChatScreen.name}/{index}",
                        arguments = listOf(
                            navArgument("index") {
                                type = NavType.StringType
                                nullable = false
                                defaultValue = ""
                            }
                        )
                    ) { entry ->
                        val userId = entry.arguments?.getString("index")
                        val destUser = userId?.let { app.activeTeamMembers.collectAsState(initial = emptyList()).value.find { it.email == userId } }

                        if (destUser != null) {
                            vm.setActivePage(Route.ChatScreen.title + "/" + "${destUser.firstName} ${destUser.lastName}")
                            Chat(destUser)
                        }
                    }

                    composable(route = "${Route.ChatScreen.name}/group") {
                        vm.setActivePage(Route.ChatScreen.title + "/" + activeTeam.name)
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
                            navigateTo = navigateTo,
                            user= vm.user
                        )
                    }

                    composable(route = Route.NewTask.name) {
                        vm.setActivePage(Route.NewTask.title)
                        if (taskVM.task.value.title != "New Task")
                            taskVM.setTask(Task(title = "New Task", section = sections[0]))
                        NewTaskScreen(changeRoute = onItemSelect, vm = taskVM )//app
                    }

                    composable(
                        route = "${Route.TeamTasks.name}/{index}",
                        arguments = listOf(
                            navArgument("index") {
                                type = NavType.StringType
                                nullable = false
                                defaultValue = ""
                            }
                        )
                    ) { entry ->
                        val taskId = entry.arguments?.getString("index")

                        /*vm.tasksList.find { it.id.toInt() == index }?.let {
                            vm.setActivePage(it.title)
                        }*/
                        tasksList.value.find { it.id == taskId }?.let {
                            vm.setActivePage(it.title)
                            val assignee = app.activeTeamMembers.collectAsState(initial = emptyList()).value.find { u -> u.email == it.assignee }
                            ShowTaskDetails(it, assignee, onComplete = { task ->
                                task.complete()
                                taskVM.onTaskUpdated(task)
                                onItemSelect(1, null, null, null)
                            })
                        }
                    }

                    composable(
                        route = "${Route.EditTask.name}/{index}",
                        arguments = listOf(
                            navArgument("index") {
                                type = NavType.StringType
                                nullable = false
                                defaultValue = ""
                            }
                        )
                    ) { entry ->
                        val index = entry.arguments?.getString("index")
                        val taskEditing = tasksList.value.find { it.id == index }

                        tasksList.value.find { it.id == index }?.let {
                            vm.setActivePage(it.title)
                            if (taskVM.task.value.id != it.id)
                                taskVM.setTask(it)
                        }

                        if (taskEditing != null) {
                            EditTaskScreen(changeRoute = onItemSelect, taskVM = taskVM)
                        }
                    }

                    composable(
                        route = "${Route.UserView.name}/{index}",
                        arguments = listOf(
                            navArgument("index") {
                                type = NavType.StringType
                                nullable = false
                                defaultValue = ""
                            }
                        )
                    ) { entry ->
                        vm.setActivePage(Route.UserView.title)
                        val userId = entry.arguments?.getString("index")
                        var user = User()
                        if (userId != null) {
                            user = vm.fetchUsers(activeTeamId).collectAsState(initial = emptyList()).value.find { it.email == userId } ?: User()
                        }
                        UserScreen(user = user, personalInfo = false, onLogout = onLogout)
                    }

                    composable(route = Route.UserView.name) {
                        vm.setActivePage(Route.UserView.title)
                        UserScreen(user = app.user.value, personalInfo = true, onLogout = onLogout)
                    }

                    composable(
                        "profile?id={teamId}",
                        deepLinks = listOf(navDeepLink { uriPattern = "https://www.workstream.it/{teamId}" }),
                    ) { entry ->
                        val teamId = entry.arguments?.getString("teamId")!!
                        ConfirmJoinTeamPage(
                            navController = navController,
                            teamId = teamId,
                            onConfirm = { team ->
                                vm.joinTeam(teamId, user.email)
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
