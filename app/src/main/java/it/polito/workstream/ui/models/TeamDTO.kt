package it.polito.workstream.ui.models

import android.util.Log
import androidx.compose.runtime.collectAsState
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TeamDTO {


    lateinit var teamName: String
    lateinit var teamPhoto: String
    lateinit var sections: MutableList<String>
    lateinit var tasksInfos: List<TaskInfo>
    lateinit var membership: List<Membership>
     var members: CollectionReference? = null
    lateinit var user :DocumentReference
    lateinit var teamId: String

override fun toString(): String {
        return "TeamEntity(teamName='$teamName', teamPhoto='$teamPhoto', sections=$sections, tasksInfos=$tasksInfos, membership=$membership, user : $user)"
    }
    private fun fetchAdmin() : Flow<User> = callbackFlow {
        val listener = user.addSnapshotListener { r, e ->
            if (r != null) {
                val admin = r.toObject(User::class.java)!!

                trySend(admin)

            } else {
                Log.d("Admin", "Error getting documents: ", e)
            }
        }

        awaitClose { listener.remove() }
    }
    private fun fetchMembers() : Flow<List<User>> = callbackFlow {
        val listener = members?.addSnapshotListener { r, e ->
            if (r != null) {
                val members = r.toObjects(User::class.java)

                trySend(members)

            } else {
                Log.d("Admin", "Error getting documents: ", e)
            }
        }

        awaitClose {
            listener?.remove()
        }
    }


    fun toTeam() : Team {
        return Team(
            name= teamName,
            teamId = teamId,
            adminFlow = fetchAdmin(),


        )

    }
    fun HashMap<String, String>.toUser(): User  = User(firstName = this["firstName"] as String, lastName = this["lastName"] as String, email = this["email"] as String, location = this["location"] as String)
}

class TaskInfo{

    var TaskInfosId : Long = 0
    lateinit var taskName: String
    lateinit var deadline: String
    lateinit var status: String

    override fun toString(): String {
        return "TaskInfo(TaskInfosId=$TaskInfosId, taskName='$taskName', deadline='$deadline', status='$status')"
    }
}


class Membership{
     var MemberId : Long=0
    lateinit var email: String
    lateinit var firstName: String
    lateinit var lastName: String
    lateinit var location : String

    override fun toString(): String {
        return "Membership(MemberId=$MemberId, email='$email', firstName='$firstName', lastName='$lastName', location='$location')"
    }
}

class UserDTO {

//    bitmapValue null
//
//    chats
//    (mappa)
//    email "christian.dellisanti9@gmail.com"
//    (stringa)
//    firstAndLastName "Christian Dellisanti"
//    (stringa)
//    firstName "Christian"
//    (stringa)
//    id 5
//    (numero)
//    lastName "Dellisanti"
//    (stringa)
//    location ""
//    (stringa)
//    profilePicture "https://lh3.googleusercontent.com/a/ACg8ocJEoAuazNiNxx0_SHSGXHM48-NI-myfWpY3paJxTeVRKonR6w=s96-c"
//    (stringa)
//    tasks
//    (array)
//    teams
}
