package com.fastek.notea.enseignant.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.fastek.notea.enseignant.export.ClasseurEnseignant
import com.fastek.notea.enseignant.export.ClasseurExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/** Écrit le classeur Excel dans le cache de l'application (dossier « exports », déjà déclaré au FileProvider). */
suspend fun creerFichierExcel(context: Context, export: ClasseurExport): File = withContext(Dispatchers.IO) {
    val dossier = File(context.cacheDir, "exports").apply { mkdirs() }
    val base = "${export.matiere}_${export.anneeScolaire}".replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
    File(dossier, "Notea_$base.xlsx").also { fichier ->
        FileOutputStream(fichier).use { ClasseurEnseignant.ecrire(export, it) }
    }
}

fun partagerExcel(context: Context, fichier: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = TYPE_XLSX
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Partager le classeur Excel"))
}
