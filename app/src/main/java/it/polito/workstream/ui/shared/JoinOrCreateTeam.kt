package it.polito.workstream.ui.shared

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun JoinOrCreateTeam(joinTeam: (String) -> Unit, addNewTeam: (teamName: String) -> Result<String>, navigateToTeam: (String) -> Unit, logout: () -> Unit= {}, logoutButton: Boolean = false)  {

    // New Team Dialog variables
    var showNewTeamDialog by remember { mutableStateOf(false) }
    var newTeamName by remember { mutableStateOf("") }
    var newTeamNameError by remember { mutableStateOf("") }
    val showLogoutDialog = remember { mutableStateOf(false) }

    fun saveNewTeam() {
        Log.d("newTeamName", "newTeamName: $newTeamName")
        if (newTeamName.isBlank()) {
            newTeamNameError = "Team name cannot be empty"
        } else { // Save the new team
            val result = addNewTeam(newTeamName)
            if (result.isSuccess) navigateToTeam(result.getOrNull()!!)
            else newTeamNameError = result.exceptionOrNull()?.message ?: "Error creating team"
            showNewTeamDialog = false
        }
    }

    fun scanInviteLinkQR(context: Context, onJoinTeam: (teamId: String) -> Unit, onCancel: () -> Unit) {
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
                        onJoinTeam(teamId)
                        navigateToTeam(teamId)
                    }
                } else Toast.makeText(context, "Invalid QR code", Toast.LENGTH_SHORT).show()
            }
            .addOnCanceledListener { onCancel() }   // User canceled the operation
            .addOnFailureListener { _ -> // Task failed with an exception
                Toast.makeText(context, "Error scanning QR code", Toast.LENGTH_SHORT).show()
                onCancel()
            }
    }

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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor = Color.Red)
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

    // Join Team and Create Team buttons

    Row(modifier = Modifier.fillMaxWidth()) {
        val context = LocalContext.current
        OutlinedButton(modifier = Modifier
            .padding(5.dp)
            .weight(1f),
            onClick = { scanInviteLinkQR(context = context, onJoinTeam = joinTeam, onCancel = { showNewTeamDialog = false }) }
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
    if (logoutButton) {
        OutlinedButton(
            onClick = { showLogoutDialog.value = true },
        ) {
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
}