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
    //list of teams the user is part of
    var teams: MutableList<Team> = mutableListOf(),
    var tasks: MutableList<Task> = mutableListOf(),
    var chats: MutableMap<User, List<ChatMessage>> = mutableMapOf() //list of chats the user has created
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
        chats: MutableMap<User, List<ChatMessage>>
    ) : this(getNewId(), firstName, lastName, email, location, profilePicture, BitmapValue)

    companion object {  // To generate unique identifiers for teams
        private var idCounter: Long = 0
        fun getNewId() = idCounter++
    }
}

class ChatMessage(
    val id: Long = getNewId(),
    var text: String,
    val author: User,
    val isFromMe: Boolean,
    val timestamp: LocalDateTime? = LocalDateTime.now().withSecond(0),
) {
    // Secondary constructor, which allows to create a comment without specifying the id
    constructor(text: String, author: User, isFromMe: Boolean, timestamp: LocalDateTime?) : this(getNewId(), text, author, isFromMe, timestamp)

    companion object {  // To generate unique identifiers for comments
        private var idCounter: Long = 0
        private fun getNewId() = idCounter++
    }
}


