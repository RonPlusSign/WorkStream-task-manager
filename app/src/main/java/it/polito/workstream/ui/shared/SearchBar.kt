package it.polito.workstream.ui.shared

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.FilterParams
import it.polito.workstream.Route
import it.polito.workstream.ui.viewmodels.TaskListViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSearchBar(
    searchQuery: State<String>,
    changeSearchQuery: (String) -> Unit,
    showFilterDialog: Boolean,
    toggleShowFilterDialog: () -> Unit,
    showSortDialog: Boolean,
    toggleShowSortDialog: () -> Unit,
    activePage: String,
    allSortOrders: List<String>,
    currentSortOption: MutableStateFlow<String>,
    changeSortOption: (String) -> Unit,
    filterParams: FilterParams,
    areThereActiveFilters: () -> Boolean,
    sections: List<String>,
    statusList: List<String>,
    recurrentList: List<String>,
    assignee: List<String>
) {
    var active by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.semantics { isTraversalGroup = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            SearchBar(
                modifier = Modifier
                    .weight(1f)
                    .semantics { traversalIndex = -1f }
                    .padding(bottom = 8.dp, start = 12.dp),
                colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                query = searchQuery.value,
                onQueryChange = { changeSearchQuery(it) },
                onSearch = { active = false },
                active = false,
                onActiveChange = { active = false },
                placeholder = { Text("Search task...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.value.isNotEmpty()) {
                        Icon(Icons.Default.Close,
                            contentDescription = "Close search bar",
                            modifier = Modifier.clickable { changeSearchQuery(""); })
                    }
                },
                content = {}
            )

            val iconColor = if (areThereActiveFilters()) Color.Red else LocalContentColor.current
            IconButton(onClick = toggleShowFilterDialog) { Icon(Icons.Default.FilterAlt, contentDescription = "filter", tint = iconColor) }
            IconButton(onClick = toggleShowSortDialog) { Icon(Icons.AutoMirrored.Default.Sort, contentDescription = "sort") }

            FilterTasksDialog(
                showFilterDialog = showFilterDialog, closeDialog = toggleShowFilterDialog, filterParams, sections,
                assignee, statusList, recurrentList
            )
            SortTasksDialog(showSortDialog = showSortDialog, closeDialog = toggleShowSortDialog, allSortOrders, currentSortOption, changeSortOption, activePage = activePage)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarSurface(
    modifier: Modifier,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val colorTransitionFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
    val fraction = if (colorTransitionFraction > 0.01f) 1f else 0f
    val appBarContainerColor by animateColorAsState(
        targetValue = lerp(
            colors.containerColor,
            colors.scrolledContainerColor,
            FastOutLinearInEasing.transform(fraction),
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TopBarSurfaceContainerColorAnimation",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = appBarContainerColor,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTasksDialog(
    showFilterDialog: Boolean,
    closeDialog: () -> Unit,
    filterParams: FilterParams,
    sections: List<String>,
    assignee: List<String>,
    statusList: List<String>,
    recurrentList: List<String>,
    vm: TaskListViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {


    val activeTeamId by vm.activeTeamId.collectAsState(initial = "")
    val assignee = vm.fetchUsers(activeTeamId).collectAsState(initial = listOf()).value.map { it.email }
    val sections by vm.fetchSections(activeTeamId).collectAsState(initial = listOf())


    if (!showFilterDialog) return
    val sheetState = rememberModalBottomSheetState()
    var sectionExpanded by remember { mutableStateOf(false) }
    var userExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var recurrentExpanded by remember { mutableStateOf(false) }
    var switchChecked by remember { mutableStateOf(filterParams.completed) }

    ModalBottomSheet(
        onDismissRequest = closeDialog,
        sheetState = sheetState
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Filter by", fontSize = 27.sp, modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(
                Modifier.padding(bottom = 16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = userExpanded,
                            onExpandedChange = { userExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = if (filterParams.assignee != "") filterParams.assignee else "--- No assignee filter ---",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Assignee") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = userExpanded,
                                onDismissRequest = { userExpanded = false }) {
                                assignee.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = {
                                            filterParams.assignee = it; userExpanded = false
                                        })
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "--- No assignee filter ---",
                                            fontStyle = FontStyle.Italic
                                        )
                                    },
                                    onClick = { filterParams.assignee = ""; userExpanded = false }
                                )
                            }
                        }
                    }
                    // Section filter
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = sectionExpanded,
                            onExpandedChange = { sectionExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = if (filterParams.section != "") filterParams.section else "--- No section filter ---",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Section") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = sectionExpanded,
                                onDismissRequest = { sectionExpanded = false }) {
                                sections.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = {
                                            filterParams.section = it; sectionExpanded = false
                                        })
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "--- No section filter ---",
                                            fontStyle = FontStyle.Italic
                                        )
                                    },
                                    onClick = { filterParams.section = ""; sectionExpanded = false }
                                )
                            }
                        }
                    }
                    // Status filter
                    Box(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = it },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = if (filterParams.status != "") filterParams.status else "--- No status filter ---",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }) {
                                statusList.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = {
                                            filterParams.status = it; statusExpanded = false
                                        })
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "--- No status filter ---",
                                            fontStyle = FontStyle.Italic
                                        )
                                    },
                                    onClick = { filterParams.status = ""; statusExpanded = false }
                                )
                            }
                        }
                    }
                    // Recurrent filter
                    Box(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = recurrentExpanded,
                            onExpandedChange = { recurrentExpanded = it },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = if (filterParams.recurrent != "") filterParams.recurrent else "--- No recurrent filter ---",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Recurrent") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrentExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = recurrentExpanded,
                                onDismissRequest = { recurrentExpanded = false }) {
                                recurrentList.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = {
                                            filterParams.recurrent = it; statusExpanded = false
                                        })
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "--- No recurrent filter ---",
                                            fontStyle = FontStyle.Italic
                                        )
                                    },
                                    onClick = {
                                        filterParams.recurrent = ""; statusExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    // Completed filter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = "Show only completed tasks: ",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 1.dp)
                        )
                        Spacer(modifier = Modifier.weight(0.5f))
                        Switch(
                            checked = switchChecked,
                            onCheckedChange = {
                                switchChecked = it
                                filterParams.completed = it
                            }
                        )
                    }
                    // Clear filters and apply
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(bottom = 32.dp)
                    ) {
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = { closeDialog(); filterParams.clear() }) { Text("Clear filters") }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(modifier = Modifier.weight(1f), onClick = closeDialog) { Text("Apply") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortTasksDialog(showSortDialog: Boolean, closeDialog: () -> Unit, sortOptions: List<String>, currentSortOption: MutableStateFlow<String>, changeSortOption: (String) -> Unit, activePage: String) {
    if (!showSortDialog) return
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = closeDialog,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Sort by", fontSize = 27.sp, modifier = Modifier.padding(bottom = 16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                sortOptions.forEach { option ->
                    if ((activePage == Route.MyTasks.title && option != "Assignee") || (activePage == Route.TeamTasks.title && option != "Section")) {//inutile
                        OutlinedButton(
                            onClick = { changeSortOption(option) },
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp)
                                .fillMaxWidth()
                        ) {
                            if (option == currentSortOption.collectAsState().value)
                                Icon(imageVector = Icons.Default.Check, contentDescription = option, modifier = Modifier.padding(end = 8.dp))
                            Text(text = option)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = { changeSortOption("Due date"); closeDialog(); }) { Text("Reset") }
                Spacer(modifier = Modifier.width(10.dp))
                Button(modifier = Modifier.weight(1f), onClick = closeDialog) { Text("Apply") }
            }
        }
    }


}