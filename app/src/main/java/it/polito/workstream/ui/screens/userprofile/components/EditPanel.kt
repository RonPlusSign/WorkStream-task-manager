package it.polito.workstream.ui.screens.userprofile.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.polito.workstream.ui.shared.ProfilePicture

@Composable
fun EditPanel(
    firstNameValue: String,
    firstNameError: String,
    setFirstName: (String) -> Unit,
    lastNameValue: String,
    lastNameError: String,
    setLastName: (String) -> Unit,
    emailValue: String,
    emailError: String,
    setEmail: (String) -> Unit,
    locationValue: String,
    setLocation: (String) -> Unit,
    profilePictureValue: String,
    setProfilePicture: (String) -> Unit,
    photoBitmapValue: Bitmap?,
    setPhotoBitmap: (Bitmap?) -> Unit,
    validate: () -> Unit,
    save: () -> Unit,
) {

    // Responsive layout: 1 column with 2 rows for vertical screens, 2 columns with 1 row for horizontal screens
    val configuration = LocalConfiguration.current
    if (configuration.screenWidthDp > configuration.screenHeightDp) { // landscape (horizontal)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ProfilePicture(
                    profilePictureValue,
                    setProfilePicture,
                    isEditing = true,
                    photoBitmapValue,
                    setPhotoBitmap,
                    "$firstNameValue $lastNameValue"
                )
            }

            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserForm(
                    firstNameValue, firstNameError, setFirstName,
                    lastNameValue, lastNameError, setLastName,
                    emailValue, emailError, setEmail,
                    locationValue, setLocation
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = { validate() }
                ) {
                    Text("Done", color = MaterialTheme.colorScheme.onPrimary)
                    Icon(
                        Icons.Default.Save, contentDescription = "Save changes", modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                    )
                }
            }
        }
    } else {  // portrait (vertical)
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                ProfilePicture(
                    profilePictureValue,
                    setProfilePicture,
                    isEditing = true,
                    photoBitmapValue,
                    setPhotoBitmap,
                    "$firstNameValue $lastNameValue"
                )
            }
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserForm(
                    firstNameValue, firstNameError, setFirstName,
                    lastNameValue, lastNameError, setLastName,
                    emailValue, emailError, setEmail,
                    locationValue, setLocation
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = { save() }
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary)
                    Icon(
                        Icons.Default.Save, contentDescription = "Save changes", modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UserForm(
    firstNameValue: String, firstNameError: String, setFirstName: (String) -> Unit,
    lastNameValue: String, lastNameError: String, setLastName: (String) -> Unit,
    emailValue: String, emailError: String, setEmail: (String) -> Unit,
    locationValue: String, setLocation: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 4 text fields: first name, last name, email, location
        OutlinedTextField(
            value = firstNameValue,
            onValueChange = setFirstName,
            label = { Text("First name") },
            isError = firstNameError.isNotBlank(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "first name") },
            modifier = Modifier.fillMaxWidth()
        )
        if (firstNameError.isNotBlank()) {
            // Small text
            Text(
                text = firstNameError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = lastNameValue,
            onValueChange = setLastName,
            label = { Text("Last name") },
            isError = lastNameError.isNotBlank(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "last name") },
            modifier = Modifier.fillMaxWidth()
        )
        if (lastNameError.isNotBlank()) {
            Text(
                text = lastNameError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = emailValue,
            onValueChange = setEmail,
            label = { Text("Email") },
            isError = emailError.isNotBlank(),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        if (emailError.isNotBlank()) {
            Text(
                text = emailError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = locationValue, onValueChange = setLocation, label = { Text("Location") },
            leadingIcon = { Icon(Icons.Default.Place, contentDescription = "location") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}