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

class GroupChat(
    val teamId: String = "",
    val messages: MutableList<ChatMessage> = mutableListOf()
) {

}

data class ChatMessage(
    val id: String,
    var text: String = "",
    val authorId: String = "",
    val timestamp: Timestamp = Timestamp.now()
) {
    //constructor(messageId: String, text: String, author: String, timestamp: Timestamp) : this(messageId, text, author, timestamp)

//    companion object {  // To generate unique identifiers for comments
//        private var idCounter: Long = 0
//        private fun getNewId() = idCounter++
//    }
}