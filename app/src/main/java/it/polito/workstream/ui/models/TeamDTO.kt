package it.polito.workstream.ui.models

import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream
import java.io.File

data class TeamDTO(
    val admin : String,
    val id: String,
    val members: MutableList<String>,
    val name: String,
    val sections: MutableList<String>,
    val profilePicture: String,

)
fun TeamDTO.toTeam() = Team(admin = admin, id = id, members = members, name = name , sections = sections, profilePicture = profilePicture, )

fun Team.toDTO() : TeamDTO {

//    val baos = ByteArrayOutputStream()
//    this.profileBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
//    val data = baos.toByteArray()

    return TeamDTO(admin = admin, id = id, members = members, name = name , sections = sections, profilePicture = profilePicture, )
}

fun TeamDTO.uploadFile(storageRef: StorageReference): UploadTask {
    var file = Uri.parse(this.profilePicture)

    val riversRef = storageRef.child("images/${file.lastPathSegment}")
    val uploadTask = riversRef.putFile(file)

    return uploadTask
}