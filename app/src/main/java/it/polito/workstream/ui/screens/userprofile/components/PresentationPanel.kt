package it.polito.workstream.ui.screens.userprofile.components

import android.graphics.Bitmap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.style.TextAlign
import it.polito.workstream.ui.shared.ProfilePicture


@Composable
fun PresentationPanel(
    firstName: String,
    lastName: String,
    email: String,
    location: String?,
    profilePicture: MutableState<String>,
    setProfilePicture: (String) -> Unit,
    numberOfTeams: Int,
    tasksCompleted: Int,
    tasksToComplete: Int,
    edit: () -> Unit,
    changePassword: () -> Unit,
    logout: () -> Unit,
    photoBitmapValue: MutableState<Bitmap?>,
    setPhotoBitmap: (Bitmap?) -> Unit,
    personalInfo: Boolean
) {
    val configuration = LocalConfiguration.current
    if (configuration.screenWidthDp > configuration.screenHeightDp) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ProfilePicture(profilePicture, setProfilePicture, isEditing = false, photoBitmapValue, setPhotoBitmap, "$firstName $lastName")
            }
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UserInfoWithButtons("$firstName $lastName", email, location, edit, changePassword, logout, numberOfTeams, tasksCompleted, tasksToComplete, personalInfo)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                ProfilePicture(profilePicture, setProfilePicture, isEditing = false, photoBitmapValue, setPhotoBitmap, "$firstName $lastName")
            }
            Row(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxSize()
            ) {
                UserInfoWithButtons("$firstName $lastName", email, location, edit, changePassword, logout, numberOfTeams, tasksCompleted, tasksToComplete, personalInfo)
            }
        }
    }
}

@Composable
fun UserInfoWithButtons(
    fullName: String,
    email: String,
    location: String?,
    edit: () -> Unit,
    changePassword: () -> Unit,
    logout: () -> Unit,
    numberOfTeams: Int,
    tasksCompleted: Int,
    tasksToComplete: Int,
    personalInfo: Boolean
) {
    val showLogoutDialog = remember { mutableStateOf(false) }

    if (showLogoutDialog.value) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog.value = false },
            confirmButton = {},
            dismissButton = {},
            title = {
                Text(
                    "Logout Confirmation",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Are you sure you want to logout?", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                showLogoutDialog.value = false
                                logout()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor =  Color.Red)
                        ) {
                            Text("Confirm")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showLogoutDialog.value = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            fullName,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(email, style = MaterialTheme.typography.headlineSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "location",
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(20.dp)
            )
            Text(location ?: "Location not set", style = MaterialTheme.typography.bodyLarge)
        }

        if (numberOfTeams > 0) {
            Row {
                Text("Member of ", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "$numberOfTeams",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(" teams", style = MaterialTheme.typography.bodyLarge)
            }
        } else Text("You're not part of any team")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (tasksToComplete == 0 && tasksCompleted == 0) Text("No tasks assigned yet!", style = MaterialTheme.typography.bodyLarge)
            else {
                val progress = if (tasksToComplete > 0) tasksCompleted.toFloat() / (tasksToComplete + tasksCompleted) else 1f
                val progressPercentage = (progress * 100).toInt()
                if (progressPercentage == 100) Text("All tasks completed!", style = MaterialTheme.typography.bodyLarge)
                else Text(text = "Tasks completed: $tasksCompleted/${tasksToComplete + tasksCompleted} ($progressPercentage%)", style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(6.dp),
                    color = Color(0xFF43A047),
                    trackColor = Color(0xFFE53935),
                    strokeCap = StrokeCap.Round,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        if (personalInfo) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = changePassword, modifier = Modifier.weight(1f)) {
                        Text("Edit password")
                        Icon(
                            Icons.Default.Lock, contentDescription = "edit", modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp)
                        )
                    }
                    Button(onClick = edit, modifier = Modifier.weight(1f)) {
                        Text("Edit profile", color = MaterialTheme.colorScheme.onPrimary)
                        Icon(
                            Icons.Default.Edit, contentDescription = "edit", modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp)
                        )
                    }
                }
                OutlinedButton(onClick = { showLogoutDialog.value = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Logout")
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "logout",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}