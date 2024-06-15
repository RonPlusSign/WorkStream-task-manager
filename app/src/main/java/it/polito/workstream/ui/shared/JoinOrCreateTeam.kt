package it.polito.workstream.ui.shared

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun JoinOrCreateTeam(onJoinTeam: (String) -> Unit, addNewTeam: (String) -> Unit) {

    // New Team Dialog variables
    var showNewTeamDialog by remember { mutableStateOf(false) }
    var newTeamName by remember { mutableStateOf("") }
    var newTeamNameError by remember { mutableStateOf("") }

    fun saveNewTeam() {
        if (newTeamName.isBlank()) {
            newTeamNameError = "Team name cannot be empty"
        } else { // Save the new team
            addNewTeam(newTeamName)
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
                    }
                } else Toast.makeText(context, "Invalid QR code", Toast.LENGTH_SHORT).show()
            }
            .addOnCanceledListener { onCancel() }   // User canceled the operation
            .addOnFailureListener { _ -> // Task failed with an exception
                Toast.makeText(context, "Error scanning QR code", Toast.LENGTH_SHORT).show()
                onCancel()
            }
    }

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