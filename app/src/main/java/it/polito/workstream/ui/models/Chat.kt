package it.polito.workstream.ui.models

import com.google.firebase.Timestamp
import java.time.LocalDateTime

class Chat (
    val teamId: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val messages: MutableList<ChatMessage> = mutableListOf()
) {
    //constructor(teamId: String, user1Id: String, user2Id: String) : this(teamId, user1Id, user2Id, mutableListOf())
}

data class ChatMessage(
    val id: Long = getNewId(),
    var text: String = "",
    val author: User = User(),
    val isFromMe: Boolean = false,
    val timestamp: Timestamp = Timestamp.now(),
) {
    constructor(text: String, author: User, isFromMe: Boolean, timestamp: Timestamp) : this(getNewId(), text, author, isFromMe, timestamp)

    companion object {  // To generate unique identifiers for comments
        private var idCounter: Long = 0
        private fun getNewId() = idCounter++
    }
}