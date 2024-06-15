package it.polito.workstream.ui.screens.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import it.polito.workstream.ui.shared.JoinOrCreateTeam

@Composable
fun NoTeamsScreen(onJoinTeam: (String) -> Unit, addNewTeam: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Bro you have no teams", style = MaterialTheme.typography.displaySmall)
        JoinOrCreateTeam(onJoinTeam = onJoinTeam, addNewTeam = addNewTeam)
    }
}