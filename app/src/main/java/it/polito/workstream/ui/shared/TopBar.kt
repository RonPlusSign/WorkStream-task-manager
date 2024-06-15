package it.polito.workstream.ui.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.Route
import it.polito.workstream.ui.viewmodels.TaskListViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    drawerState: DrawerState? = null,
    navigateTo: (String) -> Any,
    content: @Composable () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    @Composable
    fun title() {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
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
        IconButton(onClick = { navigateTo(Route.ChatScreen.name) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.Message,
                contentDescription = "Localized description"
            )
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
            navigationIcon = { if (title == Route.TeamTasks.title || title == Route.TeamMembers.title || title == Route.MyTasks.title) navIcon() else navIconBack() },
            actions = { actions() }
        )
    } else {   // Vertical (portrait)
        TopAppBar( // Two rows
            title = { title() },
            colors = colors,
            navigationIcon = { if (title == Route.TeamTasks.title || title == Route.TeamMembers.title || title == Route.MyTasks.title) navIcon() else navIconBack() },
            actions = { actions() }
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWrapper(vm: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current)), drawerState: DrawerState? = null, navigateTo: (String) -> Any) {
    val activepage = vm.activePageValue.collectAsState().value
    // Serve per la gestione della activepage nella chat, e per togliere la bottombar
    val title =
        if (activepage.contains(Route.ChatScreen.title + "/"))
            activepage.removePrefix(Route.ChatScreen.title + "/")
        else if (activepage == Route.ChatScreen.title) "Chats"
        else activepage

    TopBar(title = title, drawerState = drawerState, navigateTo, content = {
        TopAppBarSurface(
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            modifier = Modifier
        ) {
            //  Show searchbar only if the active page is TeamTasks or MyTasks
            if (activepage == Route.TeamTasks.title || activepage == Route.MyTasks.title) {
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
                        sections = vm.activeTeam.collectAsState().value?.sections ?: emptyList(),
                        statusList = vm.statusList,
                        recurrentList = vm.recurrentList,
                        assignee = vm.getAssignees()
                    )
                }
            }
        }
    })
}