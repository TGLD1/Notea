package com.fastek.notea.ui.parametres

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.diagnostic.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAILLE_MAX = 400

/** Photo de l'élève pour le cadre « Photo » du bulletin PDF. Stockée uniquement sur le téléphone. */
@Composable
fun PhotoScreen(repository: NoteaRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profil by repository.observerProfil().collectAsState(initial = null)
    var message by remember { mutableStateOf<String?>(null) }
    var apercu by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(profil?.photo) {
        val chemin = profil?.photo.orEmpty()
        apercu = if (chemin.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) { try { BitmapFactory.decodeFile(chemin) } catch (e: Exception) { null } }
        }
    }

    val lanceur = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val eleve = profil
        if (uri != null && eleve != null) {
            scope.launch {
                try {
                    val fichier = withContext(Dispatchers.IO) { enregistrerPhoto(context, uri) }
                    repository.mettreAJourPhoto(eleve.id, fichier.absolutePath)
                    Journal.ok("photo.enregistrement")
                    message = "Photo enregistrée."
                } catch (e: Exception) {
                    Journal.erreur("photo.enregistrement", e)
                    message = "Impossible d'utiliser cette image."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Photo de l'élève", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Elle apparaît dans le cadre « Photo » du bulletin PDF. Elle reste sur ton téléphone et n'est jamais envoyée.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        apercu?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Photo de l'élève",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 120.dp, height = 150.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { lanceur.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        ) { Text(if (apercu == null) "Choisir une photo" else "Changer la photo") }
        if (apercu != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val eleve = profil ?: return@OutlinedButton
                    scope.launch {
                        withContext(Dispatchers.IO) { File(context.filesDir, "photo_eleve.jpg").delete() }
                        repository.mettreAJourPhoto(eleve.id, "")
                        message = "Photo supprimée."
                    }
                }
            ) { Text("Supprimer la photo") }
        }
        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Réduit l'image (400 px max), corrige son orientation et l'enregistre dans le dossier privé. */
private fun enregistrerPhoto(context: android.content.Context, uri: Uri): File {
    val resolver = context.contentResolver
    val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, dimensions) }
    var echantillon = 1
    while (dimensions.outWidth / echantillon > TAILLE_MAX * 2 || dimensions.outHeight / echantillon > TAILLE_MAX * 2) {
        echantillon *= 2
    }
    var bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = echantillon })
    } ?: throw IllegalStateException("Image illisible")

    val rotation = resolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    if (rotation != 0f) {
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
    }
    val plusGrand = maxOf(bitmap.width, bitmap.height)
    if (plusGrand > TAILLE_MAX) {
        val echelle = TAILLE_MAX.toFloat() / plusGrand
        bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * echelle).toInt(), (bitmap.height * echelle).toInt(), true)
    }
    val fichier = File(context.filesDir, "photo_eleve.jpg")
    fichier.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    return fichier
}
