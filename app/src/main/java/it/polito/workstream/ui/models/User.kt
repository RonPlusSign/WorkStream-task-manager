package it.polito.workstream.ui.models

import android.graphics.Bitmap
import com.google.firebase.firestore.DocumentReference
import java.time.LocalDateTime

class User(
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var location: String? = null,
    var profilePicture: String = "",
    var BitmapValue: Bitmap? = null,
    var activeTeam :  DocumentReference? = null,
    var userId : String = "",
    var teams: MutableList<Team> = mutableListOf(), // List of teams to which the user belongs
    var tasks: MutableList<Task> = mutableListOf(), // List of tasks assigned to the user
    var chats: MutableMap<String, List<ChatMessage>> = mutableMapOf() // Map of chat messages
) {

    fun addTeam(team: Team) {
        teams.add(team)
    }

    fun addTask(task: Task) {
        tasks.add(task)
    }

    fun getFirstAndLastName(): String {
        return "$firstName $lastName"
    }

    //secondary constructor to create a user without specifying the id
    constructor(
        firstName: String,
        lastName: String,
        email: String,
        location: String? = null,
        profilePicture: String = "",
        BitmapValue: Bitmap? = null,
        chats: MutableMap<String, List<ChatMessage>>
    ) : this(firstName, lastName, email, location, profilePicture, BitmapValue)

    companion object {  // To generate unique identifiers for teams
        private var idCounter: Long = 0
        fun getNewId() = idCounter++
    }
}


