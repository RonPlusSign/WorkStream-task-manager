package it.polito.workstream.ui.screens.team

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.polito.workstream.R
import it.polito.workstream.ui.models.User
import it.polito.workstream.ui.shared.JoinOrCreateTeam
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NoTeamsScreen(activeUser: StateFlow<User>, onJoinTeam: (String) -> Unit, addNewTeam: (String) -> Result<String>, navigateToTeam: (String) -> Unit, logout : () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_round),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(30.dp))
        Text("Welcome, ${activeUser.collectAsState().value.firstName}!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(5.dp))
        Text("You are not part of any team yet")
        Spacer(modifier = Modifier.height(15.dp))
        JoinOrCreateTeam(joinTeam = onJoinTeam, addNewTeam = addNewTeam, navigateToTeam = navigateToTeam, logout = logout, logoutButton = true)
    }
}