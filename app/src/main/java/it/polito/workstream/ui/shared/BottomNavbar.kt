package it.polito.workstream.ui.shared

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attribution
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.Route
import it.polito.workstream.ui.viewmodels.TaskListViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

@Composable
fun BottomNavbar(active: String, onRouteChange: (route: String) -> Any, teamId: Long) {
    // 3 routes: my_tasks, team_tasks, team_members
    val routes = listOf(
        R("Team Tasks", Route.TeamTasks.name, Icons.Default.Checklist, 1),
        R("My Tasks", Route.MyTasks.name, Icons.Default.Attribution, 2),
        R("Team Members", Route.TeamScreen.name, Icons.Filled.Groups, 5)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.height(66.dp)
    ) {
        routes.forEach { route ->
            NavigationBarItem(
                icon = { Icon(imageVector = route.icon, contentDescription = route.name) },
                label = { Text(route.name) },
                selected = route.name == active,
                onClick = {
                    if (route.route == Route.TeamTasks.name)
                        onRouteChange("/$teamId/${route.route}")
                    else
                        onRouteChange(route.route)
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
fun BottomNavbarWrapper(
    vm: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    navigateTo: (route: String) -> Any,
    teamId: Long
) {
    val activePage = vm.activePageValue.collectAsState().value

    if (activePage != Route.NewChat.title && activePage != Route.ChatScreen.title && !activePage.contains(Route.ChatScreen.title)) {
        BottomNavbar(active = vm.activePageValue.collectAsState().value, onRouteChange = navigateTo, teamId = teamId)
    }


}

private class R(val name: String, val route: String, val icon: ImageVector, val idnav: Int)