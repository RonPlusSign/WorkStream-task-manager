package it.polito.workstream.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import it.polito.workstream.Route
import it.polito.workstream.ui.models.Team
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.viewmodels.TeamListViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DrawerMenu(val icon: ImageVector, val title: String, val route: String, val num: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerContent(
    vm: TeamListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    onMenuClick: (String) -> Unit,
    activeTeamId: String,
    activeUser: StateFlow<User?>,
    myProfile: () -> Unit,
    drawerState: DrawerState,
    navigateTo: (String) -> Any,
) {
    val teams = vm.getTeams().collectAsState(initial = emptyList()).value
    var active by rememberSaveable { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    fun setSearchQuery(newQuery: String) {
        searchQuery = newQuery
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "My Teams", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))

        SearchBar(
            modifier = Modifier
                .semantics { traversalIndex = -1f }
                .padding(bottom = 8.dp, start = 12.dp),
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            query = searchQuery,
            onQueryChange = { setSearchQuery(it) },
            onSearch = { active = false },
            active = false,
            onActiveChange = { active = false },
            placeholder = { Text("Search team...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Default.Close,
                        contentDescription = "Close search bar",
                        modifier = Modifier.clickable { setSearchQuery(""); })
                }
            },
            content = {}
        )


        teamsToDrawerMenu(teams, activeUser).filter { it.title.contains(searchQuery) }.forEach {
            val team = teams.find { team -> team.id.toString() == it.route }!!
            OutlinedCard(
                colors = CardDefaults.cardColors(
                    containerColor = if (activeTeamId == it.route) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                ),
                border = if (activeTeamId == it.route) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .padding(6.dp)
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable { onMenuClick(it.route) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                    if (team.profileBitmap.value == null && team.profilePicture.value.isBlank()) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = it.title.trim().split(" ").map { it.first().uppercaseChar() }.joinToString("").take(2),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp
                            )
                        }
                    } else if (team.profileBitmap.value != null && team.profilePicture.value.isBlank()) {
                        Image(
                            bitmap = team.profileBitmap.value!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        AsyncImage(
                            model = team.profilePicture.value,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Column(modifier = Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp)) {
                        Text(
                            text = it.title,
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activeTeamId == it.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            Text("${it.num}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(text = " members")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        val scope = rememberCoroutineScope()
        JoinOrCreateTeam(
            onJoinTeam = {
                scope.launch { drawerState.close() }
                navigateTo("profile?id=$it")
            },

            addNewTeam = { teamName -> vm.createEmptyTeam(teamName) },
        )


        // My Account button
        Button(onClick = { myProfile() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Person, contentDescription = "My account")
            Text(text = "My account")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Composable
fun NavDrawer(
    vm: TeamListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    navigateTo: (String) -> Any,
    drawerState: DrawerState,
    activeUser: StateFlow<User?>,
    content: @Composable () -> Unit = {},
) {
    val activeTeamId: String = vm.activeTeam.collectAsState(initial = null).value?.id.toString()
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    activeTeamId = activeTeamId,
                    onMenuClick = {
                        scope.launch { drawerState.close() }
                        navigateTo("/${it}/${Route.TeamTasks.name}")
                    },

                    myProfile = {
                        scope.launch { drawerState.close() }
                        navigateTo(Route.UserView.name)
                    },
                    activeUser = activeUser,
                    drawerState = drawerState,
                    navigateTo = navigateTo
                )
            }
        },
    ) {
        content()
    }
}


fun teamsToDrawerMenu(teams: List<Team>, user: StateFlow<User?>): List<DrawerMenu> {
    return teams.filter { u -> u.members.contains(user.value) }.map { DrawerMenu(Icons.Filled.Face, it.name, it.id.toString(), it.members.size) }
}