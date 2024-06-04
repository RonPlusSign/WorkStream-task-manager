package it.polito.workstream.ui.screens.userprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ChatModel
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.userprofile.components.EditPanel
import it.polito.workstream.ui.screens.userprofile.components.PresentationPanel
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

@Composable
fun UserScreen(user: User, personalInfo: Boolean) {
    val vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
    vm.setUser(user)

    WorkStreamTheme {
        Column(modifier = Modifier.fillMaxSize()) {

            if (vm.isEditing) vm.profilePictureValue?.let {
                EditPanel(
                    vm.firstNameValue, vm.firstNameError, vm::setFirstName,
                    vm.lastNameValue, vm.lastNameError, vm::setLastName,
                    vm.emailValue, vm.emailError, vm::setEmail,
                    vm.locationValue ?: "", vm::setLocation,
                    it, vm::setProfilePicture,
                    vm.photoBitmapValue, vm::setPhotoBitmap,
                    vm::validate
                )
            }
            else vm.profilePictureValue?.let {
                PresentationPanel(
                    vm.firstNameValue,
                    vm.lastNameValue,
                    vm.emailValue,
                    vm.locationValue,
                    it,
                    vm::setProfilePicture,
                    vm.numberOfTeams,
                    vm.tasksCompleted,
                    vm.tasksToComplete,
                    vm::edit,
                    { println("Changing user password") },
                    { println("Logging out") },
                    vm.photoBitmapValue,
                    vm::setPhotoBitmap,
                    personalInfo = personalInfo
                )
            }
        }
    }
}