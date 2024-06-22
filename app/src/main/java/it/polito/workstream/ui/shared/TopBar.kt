package it.polito.workstream.ui.shared

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.Route
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.viewmodels.TaskListViewModel
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    drawerState: DrawerState? = null,
    navigateTo: (String) -> Any,
    content: @Composable () -> Unit = {},
    unseenMessagesCount: Int,
    activePage: String,
    destUser: User?
) {
    val scope = rememberCoroutineScope()
    Log.d("topbar", "TOOOOPPPPBAAAAR: ${destUser?.email}")

    @Composable
    fun title() {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (activePage.contains(Route.ChatScreen.title) && destUser != null) {
                        Log.d("chat", "Navigate to ${Route.UserView.title + "/${destUser.email}"}")
                        navigateTo("${Route.UserView.name}/${destUser.email}")
                    }
                }
        ) {
            Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }

    val colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)

    @Composable
    fun navIcon() {
        Icon(
            Icons.Filled.Menu,
            "change team",
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .clickable {
                    scope.launch {
                        drawerState?.apply {
                            if (isClosed) open() else close()
                        }
                    }
                }
        )
    }

    @Composable
    fun navIconBack() {
        IconButton(onClick = { navigateTo(Route.Back.name) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Localized description"
            )
        }
    }

    @Composable
    fun actions() {
        Box {
            if (activePage.contains(Route.ChatScreen.title))
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Localized description"
                    )
                }
            else
                IconButton(onClick = { navigateTo(Route.ChatScreen.name) }) {
                    //val tintColor = if (unseenMessagesCount > 0) Color.Red else MaterialTheme.colorScheme.onSurface
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Message,
                        contentDescription = "Localized description",
                    )
                }
            if (unseenMessagesCount > 0)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                ) { Text(text = unseenMessagesCount.toString(), fontSize = 20.sp, color = Color.White) }
        }
    }


    val configuration = LocalConfiguration.current
    if (configuration.screenWidthDp > configuration.screenHeightDp) {   // Horizontal (landscape)
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) { title() }
                    Column(modifier = Modifier.weight(1f)) { content() }
                }
            },
            colors = colors,
            navigationIcon = { if (activePage == Route.TeamTasks.title || activePage == Route.TeamMembers.title || activePage == Route.MyTasks.title) navIcon() else navIconBack() },
            actions = { actions() }
        )
    } else {   // Vertical (portrait)
        TopAppBar( // Two rows
            title = { title() },
            colors = colors,
            navigationIcon = { if (activePage == Route.TeamTasks.title || activePage == Route.TeamMembers.title || activePage == Route.MyTasks.title) navIcon() else navIconBack() },
            actions = { actions() }
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWrapper(
    vm: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    userVM: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)),
    drawerState: DrawerState? = null,
    navigateTo: (String) -> Any
) {
    val activePage = vm.activePageValue.collectAsState().value
    if (activePage.contains("no_team"))
        return

    val activeTeamId = userVM.activeTeamId.collectAsState().value
    // PROBLEMA
    val activeTeam = vm.activeTeam.collectAsState(initial = null).value
    val teamMembers = userVM.fetchUsers(activeTeamId).collectAsState(initial = listOf()).value
    val destUser = teamMembers.find{ it.email == userVM.currentDestUserId }

    // Serve per la gestione della activepage nella chat, e per togliere la bottombar
    val title =
        if (activePage.contains(Route.ChatScreen.title + "/")) activePage.removePrefix(Route.ChatScreen.title + "/")
        else if (activePage == Route.ChatScreen.title) "Chats"
        else if (activePage.contains(Route.TeamTasks.title)) activeTeam?.name ?: Route.TeamTasks.title
        else activePage

    var unseenMessagesCount = 0;
    for (m in teamMembers){
        unseenMessagesCount += userVM.countUnseenChatMessages(m.email).collectAsState(initial = 0).value
    }
    unseenMessagesCount += userVM.unseenGroupMessages.collectAsState(initial = 0).value ?: 0


    TopBar(
        title = title,
        drawerState = drawerState,
        navigateTo,
        unseenMessagesCount = unseenMessagesCount,
        activePage = activePage,
        destUser = destUser,
        content = {
            TopAppBarSurface(
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                modifier = Modifier
            ) {
                //  Show searchbar only if the active page is TeamTasks or MyTasks
                if (activePage == Route.TeamTasks.title || activePage == Route.MyTasks.title) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        TopSearchBar(
                            vm.searchQuery,
                            vm::setSearchQuery.get(),
                            vm.showFilterDialogValue,
                            vm::toggleShowFilterDialog,
                            vm.showSortDialogValue,
                            vm::toggleShowSortDialog,
                            activePage = vm.activePageValue.collectAsState().value,
                            allSortOrders = vm.allSortOrders,
                            currentSortOption = vm.currentSortOrder,
                            changeSortOption = vm::setSortOrder.get(),
                            filterParams = vm.filterParams,
                            vm::areThereActiveFilters,
                            sections = vm.activeTeam.value?.sections ?: emptyList(),
                            statusList = vm.statusList,
                            recurrentList = vm.recurrentList,
                            assignee = vm.getAssignees()
                        )
                    }
                }
            }
        })
}