package it.polito.workstream.ui.models

import android.graphics.Bitmap
import java.time.LocalDateTime

class User(
    val id: Long = getNewId(),
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var location: String? = null,
    var profilePicture: String = "",
    var BitmapValue: Bitmap? = null,
    // lista dei team a cui l'utente appartiene
    var teams: MutableList<Team> = mutableListOf(),
    var tasks: MutableList<Task> = mutableListOf(),
    var chats: MutableMap<String, List<ChatMessage>> = mutableMapOf() // lista delle chat create dall'utente
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
// costruttore secondario per creare un utente senza specificare l'id
    constructor(
        firstName: String,
        lastName: String,
        email: String,
        location: String? = null,
        profilePicture: String = "",
        BitmapValue: Bitmap? = null,
        chats: MutableMap<String, List<ChatMessage>>
    ) : this(getNewId(), firstName, lastName, email, location, profilePicture, BitmapValue)

    companion object {  // To generate unique identifiers for teams
        private var idCounter: Long = 0
        fun getNewId() = idCounter++
    }
}


