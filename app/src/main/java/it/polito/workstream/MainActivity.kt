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
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.chats.*
import it.polito.workstream.ui.screens.tasks.*
import it.polito.workstream.ui.screens.tasks.components.ShowTaskDetails
import it.polito.workstream.ui.screens.team.ConfirmJoinTeamPage
import it.polito.workstream.ui.screens.team.TeamScreen
import it.polito.workstream.ui.screens.userprofile.UserScreen
import it.polito.workstream.ui.shared.*
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.TaskViewModel
import it.polito.workstream.ui.viewmodels.TeamListViewModel
import it.polito.workstream.ui.viewmodels.UserViewModel
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
                        finish() // Finish MainActivity so the user cannot go back to it
                    }
                } else {
                    LaunchedEffect(currentUser) {
                        checkOrCreateUserInFirestore(currentUser!!) { retrievedUser ->
                            user = retrievedUser
                            app._user.value = retrievedUser
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
        val userRef = firebaseUser.email?.let { db.collection("users").document(it) }
        if (userRef != null) {
            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Ottieni i dati dal documento
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
                    val id = document.getLong("id") ?: User.getNewId()
                    val location = document.getString("location")

                    // Crea l'oggetto User
                    val user = User(
                        id = id,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        location = location,
                        profilePicture = firebaseUser.photoUrl.toString()
                    )

                    // Completa l'operazione con il callback
                    onComplete(user)
                } else {
                    Log.d("ERROR", "User not signed in yet")
                    onComplete(User()) // Return a default user object if not found
                }
            }.addOnFailureListener { e ->
                Log.e("ERROR", "Error fetching user document", e)
                onComplete(User()) // Return a default user object on error
            }
        }
    }

    private fun performLogout(context: android.content.Context, app: MainApplication) {
        Firebase.auth.signOut()
        app._user.value = User()
        //delay(1000)
        val loginIntent = Intent(context, MainActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(loginIntent)
        finish()
    }
}

@Composable
fun LoadingScreen() {
    // You can customize this with a proper loading indicator
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // For example, a CircularProgressIndicator in the center of the screen
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ContentView(
    vm: TeamListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    taskVM: TaskViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    userVM: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    onLogout: () -> Unit
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

    val onItemSelect: (
        route: Int,
        taskId: Int?,
        taskName: String?,
        userId: Long?,
        userMail: String?
    ) -> Unit = { route: Int, taskId: Int?, taskName: String?, userId: Long?, userMail: String? ->

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
            8 -> when {
                userId == null && userMail == null -> Route.ChatScreen.name
                userId == (-1).toLong() && userMail == null -> "${Route.ChatScreen.name}/group"
                userId == null && userMail != null -> "${Route.ChatScreen.name}/${userMail}"
                else -> Route.ChatScreen.name
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
                        app.user.value?.getFirstAndLastName()
                            ?.let { it1 -> PersonalTasksScreenWrapper(onItemSelect = onItemSelect, activeUser = it1) }
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
                        val index = entry.arguments?.getString("index")
                        val destUser = index?.let {
                            userVM.getUsers().collectAsState(initial = listOf()).value.find { it.email==index }
                        }
                        Log.d("Chat", destUser?.email?:"destuser not found")

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
                            navigateTo = navigateTo
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
                            onItemSelect(1, null, null, null, null)
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
                        UserScreen(user = user, personalInfo = false, onLogout = onLogout)
                    }

                    composable(route = Route.UserView.name) {
                        vm.setActivePage(Route.UserView.title)
                        app.user.value?.let { it1 -> UserScreen(user = it1, personalInfo = true, onLogout = onLogout) }
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
