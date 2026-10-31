package com.fastek.notea.enseignant.importation

import android.content.Context
import android.net.Uri
import com.fastek.notea.diagnostic.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

sealed interface ResultatImport {
    data class Texte(val contenu: String) : ResultatImport
    data class Erreur(val message: String) : ResultatImport
}

private const val TAILLE_MAX = 5_000_000

/** Lit un fichier de liste d'élèves (CSV, texte ou Excel .xlsx) et le convertit en lignes « colonne ; colonne ; … ». */
suspend fun lireFichierListe(context: Context, uri: Uri): ResultatImport = withContext(Dispatchers.IO) {
    try {
        val octets = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext ResultatImport.Erreur("Impossible d'ouvrir le fichier.")
        when {
            octets.size > TAILLE_MAX -> ResultatImport.Erreur("Ce fichier est trop volumineux pour une liste d'élèves.")
            // « PK » : un fichier .xlsx est une archive zip.
            octets.size >= 4 && octets[0] == 0x50.toByte() && octets[1] == 0x4B.toByte() -> {
                val lignes = LecteurXlsx.lireLignes(ByteArrayInputStream(octets))
                ResultatImport.Texte(lignes.joinToString("\n") { it.joinToString(";") })
            }
            // Ancien format binaire d'Excel (.xls).
            octets.size >= 4 && octets[0] == 0xD0.toByte() && octets[1] == 0xCF.toByte() ->
                ResultatImport.Erreur("Les anciens fichiers Excel (.xls) ne sont pas pris en charge. Enregistre-le en .xlsx ou en .csv.")
            else -> ResultatImport.Texte(decoderTexte(octets))
        }
    } catch (e: Exception) {
        Journal.erreur("import.liste", e)
        ResultatImport.Erreur("Fichier illisible : ${e.message ?: e.javaClass.simpleName}")
    }
}

/** UTF-8 (avec ou sans BOM), sinon ISO-8859-1 : les CSV enregistrés par un vieux Excel ne sont pas en UTF-8. */
internal fun decoderTexte(octets: ByteArray): String {
    val utf8 = String(octets, Charsets.UTF_8).removePrefix("\uFEFF")
    return if (utf8.contains('\uFFFD')) String(octets, Charsets.ISO_8859_1) else utf8
}
