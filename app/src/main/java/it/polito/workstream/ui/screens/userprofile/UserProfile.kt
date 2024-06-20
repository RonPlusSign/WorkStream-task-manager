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
    val firstname = vm.firstNameValue
    val lastname = vm.lastNameValue
    val location = vm.locationValue


    WorkStreamTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            if (vm.isEditing) EditPanel(
                firstname.value, vm.firstNameError, vm::setFirstName,
                lastname.value, vm.lastNameError, vm::setLastName,
                vm.emailValue, vm.emailError, vm::setEmail,
                location.value ?: "", vm::setLocation,
                vm.profilePictureValue, vm::setProfilePicture,
                vm.photoBitmapValue, vm::setPhotoBitmap,
                vm::validate,

                )
            else PresentationPanel(
                firstname.value,
                lastname.value,
                vm.emailValue,
                location.value,
                vm.profilePictureValue,
                vm::setProfilePicture,
                vm.numberOfTeams,
                tasks.filter { it.completed }.size,
                tasks.filter { !it.completed }.size,
                vm::edit,
                onLogout,
                vm.photoBitmapValue,
                vm::setPhotoBitmap,
                personalInfo = personalInfo
            )
        }
    }
}