package it.polito.workstream.ui.screens.userprofile

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.workstream.MainApplication
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.screens.userprofile.components.EditPanel
import it.polito.workstream.ui.screens.userprofile.components.PresentationPanel
import it.polito.workstream.ui.theme.WorkStreamTheme
import it.polito.workstream.ui.viewmodels.UserViewModel
import it.polito.workstream.ui.viewmodels.ViewModelFactory
import it.polito.workstream.ui.Login.LoginActivity

@Composable
fun UserScreen(user: User, personalInfo: Boolean) {
    val vm: UserViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
    vm.setUser(user)
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication

    WorkStreamTheme {
        Column(modifier = Modifier.fillMaxSize()) {

            if (vm.isEditing) EditPanel(
                vm.firstNameValue, vm.firstNameError, vm::setFirstName,
                vm.lastNameValue, vm.lastNameError, vm::setLastName,
                vm.emailValue, vm.emailError, vm::setEmail,
                vm.locationValue ?: "", vm::setLocation,
                vm.profilePictureValue, vm::setProfilePicture,
                vm.photoBitmapValue, vm::setPhotoBitmap,
                vm::validate
            )
            else PresentationPanel(
                vm.firstNameValue,
                vm.lastNameValue,
                vm.emailValue,
                vm.locationValue,
                vm.profilePictureValue,
                vm::setProfilePicture,
                vm.numberOfTeams,
                vm.tasksCompleted,
                vm.tasksToComplete,
                vm::edit,
                { println("Changing user password") },
                {
                    if (personalInfo) {
                        Firebase.auth.signOut()
                        app._user.value = User();
                        val loginIntent = Intent(context, LoginActivity::class.java)
                        context.startActivity(loginIntent)
                    } else {
                        println("Logging out")
                    }
                },
                vm.photoBitmapValue,
                vm::setPhotoBitmap,
                personalInfo = personalInfo
            )
        }
    }
}
