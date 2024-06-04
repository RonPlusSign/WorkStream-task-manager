package it.polito.workstream.ui.shared

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import it.polito.workstream.Route
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
    onJoinTeam: (Long) -> Unit,
    addNewTeam: (String) -> Unit,
    activeTeamId: String,
    activeUser: StateFlow<User>,
    myProfile: () -> Unit,
) {


    var active by rememberSaveable { mutableStateOf(false) }
    // New Team Dialog variables
    var showNewTeamDialog by remember { mutableStateOf(false) }
    var newTeamName by remember { mutableStateOf("") }
    var newTeamNameError by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    fun setSearchQuery(newQuery: String) {
        searchQuery = newQuery
    }

    fun saveNewTeam() {
        if (newTeamName.isBlank()) {
            newTeamNameError = "Team name cannot be empty"
        } else { // Save the new team
            addNewTeam(newTeamName)
            showNewTeamDialog = false
        }
    }

    fun scanInviteLinkQR(context: Context, onJoinTeam: (teamId: Long) -> Unit, onCancel: () -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
        val scanner = GmsBarcodeScanning.getClient(context, options)
        scanner.startScan()
            .addOnSuccessListener { barcode: Barcode -> // QR Scanned successfully: get the parameters from the barcode and join the team
                val qrResult = barcode.rawValue
                if (!qrResult.isNullOrEmpty()) {
                    // The string should have the format "https://www.workstream.it/{teamId}"
                    if (!qrResult.startsWith("https://www.workstream.it/")) {
                        Toast.makeText(context, "Invalid QR code", Toast.LENGTH_SHORT).show()
                        onCancel()
                    } else {
                        val teamId = qrResult.split("/").last()
                        onJoinTeam(teamId.toLong())
                    }
                } else Toast.makeText(context, "Invalid QR code", Toast.LENGTH_SHORT).show()
            }
            .addOnCanceledListener { onCancel() }   // User canceled the operation
            .addOnFailureListener { _ -> // Task failed with an exception
                Toast.makeText(context, "Error scanning QR code", Toast.LENGTH_SHORT).show()
                onCancel()
            }
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

        // New Team Dialog
        if (showNewTeamDialog) {
            AlertDialog(onDismissRequest = { showNewTeamDialog = false },
                title = { Text("Choose the team name") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = newTeamName,
                            onValueChange = { newTeamName = it; newTeamNameError = "" },
                            label = { Text("Team Name") },
                            isError = newTeamNameError.isNotBlank(),
                            leadingIcon = { Icon(Icons.Default.PeopleAlt, contentDescription = "Team Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (newTeamNameError.isNotBlank()) { // Small text with error
                            Text(text = newTeamNameError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { saveNewTeam() }) { Text("Save") } },
                dismissButton = { TextButton(onClick = { showNewTeamDialog = false }) { Text("Cancel") } }
            )
        }

        vm.teamsToDrawerMenu(activeUser).filter { it.title.contains(searchQuery) }.forEach {
            val team = vm.teams.value.find { team -> team.id.toString() == it.route }!!
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

        // Join Team and Create Team buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            val context = LocalContext.current
            OutlinedButton(modifier = Modifier
                .padding(5.dp)
                .weight(1f),
                onClick = { scanInviteLinkQR(context = context, onJoinTeam = onJoinTeam, onCancel = { showNewTeamDialog = false }) }
            ) {
                Text(text = "Join a team")
                Icon(
                    Icons.Default.PersonAdd, contentDescription = "Save changes", modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp)
                )
            }
            OutlinedButton(modifier = Modifier
                .padding(5.dp)
                .weight(1f), onClick = { showNewTeamDialog = true; newTeamName = ""; newTeamNameError = "" }) {
                Text(text = "Create Team")
                Icon(
                    Icons.Outlined.AddReaction, contentDescription = "Save changes", modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp)
                )
            }
        }

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
    activeUser: StateFlow<User>,
    content: @Composable () -> Unit = {},
) {
    val activeTeamId: String = vm.activeTeam.collectAsState().value.id.toString()
    val teams = vm.teams.collectAsState().value
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

                    onJoinTeam = {
                        scope.launch { drawerState.close() }
                        navigateTo("profile?id=$it")
                    },

                    myProfile = {
                        scope.launch { drawerState.close() }
                        navigateTo(Route.UserView.name)
                    },
                    addNewTeam = { teamName -> vm.createEmptyTeam(teamName) },
                    activeUser = activeUser
                )
            }
        },
    ) {
        content()
    }
}
