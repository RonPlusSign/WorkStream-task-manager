package it.polito.workstream.ui.models

import android.graphics.Bitmap
import com.google.firebase.firestore.DocumentReference
import java.time.LocalDateTime

data class User(
    var email: String = "", // Unique identifier
    var firstName: String = "",
    var lastName: String = "",
    var location: String? = null,
    var profilePicture: String = "",
    var BitmapValue: Bitmap? = null,
    var activeTeam: DocumentReference? = null,
    var teams: MutableList<String> = mutableListOf(), // List of teams to which the user belongs
    var chats: MutableMap<String, List<ChatMessage>> = mutableMapOf() // Map of chat messages
) {

    fun getFirstAndLastName(): String {
        return "$firstName $lastName"
    }

    //secondary constructor to create a user without specifying the id
    constructor(
        email: String,
        firstName: String,
        lastName: String,
        location: String? = null,
        profilePicture: String = "",
        BitmapValue: Bitmap? = null,
        chats: MutableMap<String, List<ChatMessage>>
    ) : this(firstName, lastName, email, location, profilePicture, BitmapValue)
}

data class ChatMessage(
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


