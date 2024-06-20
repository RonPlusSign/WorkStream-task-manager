package it.polito.workstream.ui.shared


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toFile
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.util.DebugLogger
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.Instant


@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ProfilePicture(
    profilePicture: String,
    edit: (String) -> Unit = {},
    isEditing: Boolean = false,
    photoBitmapValue: Bitmap?,
    setPhotoBitmap: (Bitmap?) -> Unit,
    name: String,
    photo: MutableState<String> = mutableStateOf(""),
    basepath:String = "",
    setPhoto : (String) -> Unit
) {

    var showDialog by remember { mutableStateOf(false) }



    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            var nomefile = basepath+Instant.now().toString().replace(".","-")
            nomefile = nomefile.replace(".","-")
            nomefile = nomefile.replace(":","-")
            println(uri.toString())
            val a =  context.contentResolver.openInputStream(uri)?.use { it.readBytes()  }
            context.openFileOutput(nomefile, Context.MODE_PRIVATE).use{
                it.write(a)
            }
            Toast.makeText(context, "Image selected from gallery: $uri", Toast.LENGTH_SHORT).show()

            photo.value = nomefile
            setPhoto(nomefile)
            //edit("")
        }
    }

    val takePicture = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) { result ->
        // Handle the captured Bitmap
        if (result != null) {

            var nomefile = basepath+Instant.now().toString()
            nomefile = nomefile.replace(".","-")
            nomefile = nomefile.replace(":","-")
            nomefile = nomefile.replace("@","-")
            Log.d("nomefile", nomefile)

            Toast.makeText(context, "Image captured from camera", Toast.LENGTH_SHORT).show()
            val stream = ByteArrayOutputStream()
            result.compress(Bitmap.CompressFormat.PNG,100, stream)
            context.openFileOutput(nomefile, Context.MODE_PRIVATE).use{
                it.write(stream.toByteArray())
            }
            photo.value = nomefile
            setPhoto(nomefile)
            //edit("")

        }
    }

    // Round Profile picture with small "edit" button (circle with a pencil in bottom right corner)
    // Default value for the profile picture is the initials of the user in a circle
    Box(contentAlignment = Alignment.Center) {
        if (photo.value.isEmpty()) {

            // Show a monogram with the initials of his first and last name
            // The monogram is a circle with the first letter of the first name and the first letter of the last name
            val initials = if (name.trim().isNotBlank())
                name.trim().split(" ").map { it.first().uppercaseChar() }.joinToString("").take(2)
            else ""

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (initials.isNotBlank()) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 80.sp
                    )
                } else {
                    Icon(
                        Icons.Default.NoPhotography,
                        contentDescription = "no profile picture",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

        } else  {

                AsyncImage(
                    model =
                    ImageRequest.Builder(LocalContext.current)
                        .data(context.getFileStreamPath(photo.value).absolutePath)
                        .crossfade(true)
                        .build(), //minchia ci siamo
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // Edit button, positioned in the bottom right corner
        if (isEditing) {
            SmallFloatingActionButton(
                onClick = {
                    //pick.launch(PickVisualMediaRequest()) // pickImage.launch("image/*")
                    showDialog = true
                },
                modifier = Modifier
                    .padding(end = 12.dp, bottom = 12.dp)
                    .size(35.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape),
                containerColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "change profile picture",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()


    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = {
                showDialog = false
            },
            sheetState = sheetState,
            modifier = Modifier.height(180.dp)
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 0.dp, start = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Image profile",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Sheet content
                Row(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    Button(modifier = Modifier.weight(1f),
                        onClick = {
                            takePicture.launch()
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showDialog = false
                                }
                            }
                        }) {
                        Icon(
                            Icons.Rounded.AddAPhoto,
                            contentDescription = "take a photo",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = "Take a photo", modifier = Modifier.padding(start = 4.dp))
                    }
                    Button(modifier = Modifier.weight(1f),
                        onClick = {
                            pickImage.launch("image/*")
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showDialog = false
                                }
                            }
                        }) {
                        Icon(
                            Icons.Rounded.Image,
                            contentDescription = "open gallery",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = "Open gallery", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

