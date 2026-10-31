package com.fastek.notea.enseignant.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.domain.reference.MatieresReference
import java.time.LocalDate

/** Année scolaire en cours : de septembre à décembre « 2026-2027 », de janvier à août « 2025-2026 ». */
internal fun anneeScolaireCourante(aujourdhui: LocalDate = LocalDate.now()): String {
    val debut = if (aujourdhui.monthValue >= 9) aujourdhui.year else aujourdhui.year - 1
    return "$debut-${debut + 1}"
}

@Composable
fun PremierDemarrageScreen(
    erreurRestauration: String?,
    onRestaurer: (Uri) -> Unit,
    onValider: (nom: String, prenom: String, matiere: String, type: TypeEtablissement, annee: String, etablissement: String) -> Unit
) {
    var nom by rememberSaveable { mutableStateOf("") }
    var prenom by rememberSaveable { mutableStateOf("") }
    var matiere by rememberSaveable { mutableStateOf("") }
    var etablissement by rememberSaveable { mutableStateOf("") }
    var annee by rememberSaveable { mutableStateOf(anneeScolaireCourante()) }
    var type by rememberSaveable { mutableStateOf(TypeEtablissement.PUBLIC) }
    var choixMatiere by rememberSaveable { mutableStateOf(false) }
    val lanceurImport = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onRestaurer(uri) }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        EnTete(titre = "Bienvenue", sousTitre = "Ton cahier de notes, classe par classe") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text("N", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Notea Enseignant", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Moyennes, rangs et classeur Excel calculés pour toi.",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TitreSection("TOI")
                    Champ(nom, { nom = it }, "Nom", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    Champ(prenom, { prenom = it }, "Prénoms", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                }
            }
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TitreSection("TON ENSEIGNEMENT")
                    ChampChoix("Matière enseignée", matiere, onClick = { choixMatiere = true })
                    Champ(etablissement, { etablissement = it }, "Établissement (facultatif)", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    Champ(annee, { annee = it }, "Année scolaire", keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                }
            }
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TitreSection("TYPE D'ÉTABLISSEMENT")
                    ChoixType(
                        titre = "Public", detail = "2 semestres · moyenne annuelle (2×S2 + S1) ÷ 3",
                        choisi = type == TypeEtablissement.PUBLIC, onClick = { type = TypeEtablissement.PUBLIC }
                    )
                    ChoixType(
                        titre = "Privé", detail = "3 trimestres · moyenne des trois trimestres",
                        choisi = type == TypeEtablissement.PRIVE, onClick = { type = TypeEtablissement.PRIVE }
                    )
                }
            }

            BoutonPlein(
                "Commencer",
                onClick = { onValider(nom, prenom, matiere, type, annee, etablissement) },
                enabled = nom.isNotBlank() && matiere.isNotBlank() && annee.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            BoutonContour(
                "Restaurer une sauvegarde",
                onClick = { lanceurImport.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            )
            erreurRestauration?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }
    if (choixMatiere) {
        ListeChoix(
            titre = "Matière enseignée",
            options = MatieresReference.LISTE,
            selection = matiere,
            onChoisir = { matiere = it; choixMatiere = false },
            onFermer = { choixMatiere = false }
        )
    }
    }
}

@Composable
internal fun ChoixType(titre: String, detail: String, choisi: Boolean, onClick: () -> Unit) {
    val primaire = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (choisi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (choisi) 2.dp else 1.dp, if (choisi) primaire else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (choisi) primaire else MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) { if (choisi) Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Column(Modifier.padding(start = 12.dp)) {
                Text(titre, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
