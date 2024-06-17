package it.polito.workstream.ui.screens.userprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.userprofile.components.EditPanel
import it.polito.workstream.ui.screens.userprofile.components.PresentationPanel
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory

@Composable
fun UserScreen(user: User, personalInfo: Boolean, onLogout: () -> Unit) {
    val vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
    vm.setUser(user)
    val tasks = vm.getTasks(vm.activeTeamId.collectAsState().value).collectAsState(initial = listOf()).value.filter { it.assignee == user.email }

    WorkStreamTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            if (vm.isEditing) EditPanel(
                vm.firstNameValue, vm.firstNameError, vm::setFirstName,
                vm.lastNameValue, vm.lastNameError, vm::setLastName,
                vm.emailValue, vm.emailError, vm::setEmail,
                vm.locationValue ?: "", vm::setLocation,
                vm.profilePictureValue, vm::setProfilePicture,
                vm.photoBitmapValue, vm::setPhotoBitmap,
                vm::validate,
                vm::save,
            )
            else PresentationPanel(
                vm.firstNameValue,
                vm.lastNameValue,
                vm.emailValue,
                vm.locationValue,
                vm.profilePictureValue,
                vm::setProfilePicture,
                vm.numberOfTeams,
                tasks.filter { it.completed }.size,
                tasks.filter { !it.completed }.size,
                vm::edit,
                { println("Changing user password") },
                onLogout,
                vm.photoBitmapValue,
                vm::setPhotoBitmap,
                personalInfo = personalInfo
            )
        }
    }
}