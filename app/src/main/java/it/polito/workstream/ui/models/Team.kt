package it.polito.workstream.ui.models

import android.graphics.Bitmap



data class Team(

     var id: String = "",
    var name: String = "",
    var members: MutableList<String> = mutableListOf(),
    var tasks: MutableList<Task> = mutableListOf(),
    var sections: MutableList<String> = mutableListOf("General"),
    var admin: String = "",
    var profilePicture: String = "",
    var profileBitmap: Bitmap? = null
)