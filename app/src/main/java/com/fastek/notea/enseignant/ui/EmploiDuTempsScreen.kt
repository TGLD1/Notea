package com.fastek.notea.enseignant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Creneau
import com.fastek.notea.enseignant.data.entity.Rappel
import com.fastek.notea.enseignant.domain.EmploiDuTemps
import com.fastek.notea.enseignant.rappel.RappelScheduler

@Composable
fun EmploiDuTempsScreen(vm: EmploiViewModel, onRetour: () -> Unit) {
    val creneaux by vm.creneaux.collectAsState()
    val rappels by vm.rappels.collectAsState()
    val classes by vm.classes.collectAsState()
    val contexte = LocalContext.current

    var ajoutCours by remember { mutableStateOf(false) }
    var coursOuvert by remember { mutableStateOf<Creneau?>(null) }
    var rappelPour by remember { mutableStateOf<Creneau?>(null) }
    var erreurCours by remember { mutableStateOf<String?>(null) }
    var erreurRappel by remember { mutableStateOf<String?>(null) }

    val nomsClasses = remember(classes) { classes.associate { it.id to it.nom } }
    val rappelsParCours = remember(rappels) { rappels.groupBy { it.creneauId } }
    val alarmesPrecises = RappelScheduler.alarmesPrecisesAutorisees(contexte)

    val lanceurNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(rappelPour) {
        if (rappelPour != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexte, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            lanceurNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    EcranAvecEntete(
        titre = "Emploi du temps",
        onRetour = onRetour,
        sousTitre = if (creneaux.isEmpty()) "Tes cours de la semaine" else pluriel(creneaux.size, "cours") + " par semaine"
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BoutonPlein(
                    "＋  Ajouter un cours",
                    onClick = { erreurCours = null; ajoutCours = true },
                    enabled = classes.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (classes.isEmpty()) {
                    Text(
                        "Crée d'abord une classe sur l'accueil : chaque cours est lié à une classe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (rappels.isNotEmpty() && !alarmesPrecises) {
                    CarteDouce(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Rappels à la minute près", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Android peut retarder tes rappels de quelques minutes. Autorise « Alarmes et rappels » " +
                                    "pour qu'ils sonnent à l'heure exacte.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = {
                                RappelScheduler.intentAutorisation(contexte)?.let { contexte.startActivity(it) }
                            }) { Text("Autoriser") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (creneaux.isEmpty()) {
                EtatVide(
                    "📅", "Aucun cours",
                    "Ajoute tes cours : jour, heures et classe. Tu pourras y attacher des rappels, silencieux ou sonores."
                )
            } else {
                GrilleHoraire(creneaux, rappelsParCours, nomsClasses, onOuvrir = { erreurCours = null; coursOuvert = it })
            }
        }
    }

    if (ajoutCours) {
        CoursDialog(
            initial = null, classes = classes, rappels = emptyList(), erreur = erreurCours,
            onValider = { classeId, jour, debut, fin, salle ->
                vm.enregistrerCours(null, classeId, jour, debut, fin, salle) { erreur ->
                    erreurCours = erreur
                    if (erreur == null) ajoutCours = false
                }
            },
            onSupprimer = null, onAjouterRappel = null,
            onSupprimerRappel = {}, onBasculerRappel = {},
            onFermer = { ajoutCours = false }
        )
    }
    coursOuvert?.let { cours ->
        CoursDialog(
            initial = cours, classes = classes, rappels = rappelsParCours[cours.id].orEmpty(), erreur = erreurCours,
            onValider = { classeId, jour, debut, fin, salle ->
                vm.enregistrerCours(cours, classeId, jour, debut, fin, salle) { erreur ->
                    erreurCours = erreur
                    if (erreur == null) coursOuvert = null
                }
            },
            onSupprimer = { vm.supprimerCours(cours); coursOuvert = null },
            onAjouterRappel = { erreurRappel = null; rappelPour = cours },
            onSupprimerRappel = vm::supprimerRappel,
            onBasculerRappel = vm::basculerRappel,
            onFermer = { coursOuvert = null }
        )
    }
    rappelPour?.let { cours ->
        RappelDialog(
            creneau = cours, erreur = erreurRappel,
            onValider = { texte, minutes, sonore, hebdomadaire ->
                vm.ajouterRappel(cours, texte, minutes, sonore, hebdomadaire) { erreur ->
                    erreurRappel = erreur
                    if (erreur == null) rappelPour = null
                }
            },
            onFermer = { rappelPour = null }
        )
    }
}

// --- Tableau de la semaine --------------------------------------------------------------------------

@Composable
private fun GrilleHoraire(
    creneaux: List<Creneau>,
    rappels: Map<Long, List<Rappel>>,
    noms: Map<Long, String>,
    onOuvrir: (Creneau) -> Unit
) {
    val nbJours = if (creneaux.any { it.jour == 7 }) 7 else 6
    val premiereHeure = minOf(7, (creneaux.minOfOrNull { it.debut } ?: 420) / 60)
    val derniereHeure = maxOf(18, ((creneaux.maxOfOrNull { it.fin } ?: 1080) + 59) / 60)
    val nbHeures = derniereHeure - premiereHeure
    val hauteurHeure = 64.dp
    val hauteurEnTete = 34.dp

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            Column(Modifier.width(40.dp)) {
                Spacer(Modifier.height(hauteurEnTete))
                for (h in premiereHeure until derniereHeure) {
                    Box(Modifier.height(hauteurHeure), contentAlignment = Alignment.TopCenter) {
                        Text("${h}h", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            for (jour in 1..nbJours) {
                Column(Modifier.width(108.dp)) {
                    Box(Modifier.height(hauteurEnTete).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(EmploiDuTemps.JOURS_COURTS[jour - 1], style = MaterialTheme.typography.labelLarge)
                    }
                    Box(
                        Modifier
                            .height(hauteurHeure * nbHeures)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        for (h in 1 until nbHeures) {
                            Box(
                                Modifier.offset(y = hauteurHeure * h).fillMaxWidth().height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                        creneaux.filter { it.jour == jour }.forEach { cours ->
                            BlocCours(
                                cours = cours,
                                nomClasse = noms[cours.classeId] ?: "?",
                                rappels = rappels[cours.id].orEmpty(),
                                decalageMinutes = premiereHeure * 60,
                                hauteurHeure = hauteurHeure,
                                onClick = { onOuvrir(cours) }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
private fun BlocCours(
    cours: Creneau,
    nomClasse: String,
    rappels: List<Rappel>,
    decalageMinutes: Int,
    hauteurHeure: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val couleur = Couleurs.pourTexte(nomClasse)
    val haut = hauteurHeure * ((cours.debut - decalageMinutes) / 60f)
    val hauteur = hauteurHeure * ((cours.fin - cours.debut) / 60f)
    val sonores = rappels.count { it.sonore }
    val silencieux = rappels.size - sonores
    Box(
        Modifier
            .offset(y = haut)
            .fillMaxWidth()
            .height(hauteur)
            .padding(horizontal = 3.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(couleur.copy(alpha = 0.18f))
            .border(1.dp, couleur.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Column {
            Text(
                nomClasse, style = MaterialTheme.typography.titleSmall, color = couleur,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${EmploiDuTemps.heure(cours.debut)}–${EmploiDuTemps.heure(cours.fin)}",
                style = MaterialTheme.typography.labelMedium, maxLines = 1
            )
            if (cours.salle.isNotBlank()) {
                Text(
                    cours.salle, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (rappels.isNotEmpty()) {
                Text(
                    listOfNotNull(
                        if (sonores > 0) "🔔$sonores" else null,
                        if (silencieux > 0) "🔕$silencieux" else null
                    ).joinToString("  "),
                    style = MaterialTheme.typography.labelMedium, maxLines = 1
                )
            }
        }
    }
}

// --- Cours et rappels ------------------------------------------------------------------------------

@Composable
private fun CoursDialog(
    initial: Creneau?,
    classes: List<Classe>,
    rappels: List<Rappel>,
    erreur: String?,
    onValider: (classeId: Long, jour: Int, debut: Int, fin: Int, salle: String) -> Unit,
    onSupprimer: (() -> Unit)?,
    onAjouterRappel: (() -> Unit)?,
    onSupprimerRappel: (Rappel) -> Unit,
    onBasculerRappel: (Rappel) -> Unit,
    onFermer: () -> Unit
) {
    var classeId by remember { mutableStateOf(initial?.classeId ?: classes.firstOrNull()?.id ?: 0L) }
    var jour by remember { mutableStateOf(initial?.jour ?: 1) }
    var debut by remember { mutableStateOf(initial?.debut ?: 480) }
    var fin by remember { mutableStateOf(initial?.fin ?: 600) }
    var salle by remember { mutableStateOf(initial?.salle.orEmpty()) }

    Dialogue(
        titre = if (initial == null) "Nouveau cours" else "Modifier le cours",
        onFermer = onFermer,
        confirmer = {
            TextButton(enabled = classeId != 0L, onClick = { onValider(classeId, jour, debut, fin, salle) }) {
                Text("Enregistrer")
            }
        },
        annuler = {
            Row {
                if (onSupprimer != null) {
                    TextButton(onClick = onSupprimer) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onFermer) { Text("Fermer") }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TitreSection("CLASSE")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                classes.forEach { classe ->
                    PuceChoix(classe.nom, classeId == classe.id) { classeId = classe.id }
                }
            }
            TitreSection("JOUR")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmploiDuTemps.JOURS_COURTS.forEachIndexed { i, nom ->
                    PuceChoix(nom, jour == i + 1) { jour = i + 1 }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SaisieHeure("Début", debut, { debut = it })
                SaisieHeure("Fin", fin, { fin = it })
            }
            Champ(salle, { salle = it }, "Salle (facultatif)")
            erreur?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            if (initial != null) {
                TitreSection("RAPPELS PENDANT CE COURS")
                if (rappels.isEmpty()) {
                    Text(
                        "Aucun rappel. Ajoutes-en un pour te souvenir de quelque chose pendant le cours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                rappels.forEach { rappel ->
                    LigneRappel(rappel, onSupprimer = { onSupprimerRappel(rappel) }, onBasculer = { onBasculerRappel(rappel) })
                }
                if (onAjouterRappel != null) {
                    BoutonContour("＋  Ajouter un rappel", onClick = onAjouterRappel, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun LigneRappel(rappel: Rappel, onSupprimer: () -> Unit, onBasculer: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            (if (rappel.sonore) "🔔 " else "🔕 ") + EmploiDuTemps.heure(rappel.minutes) +
                (if (rappel.hebdomadaire) " · chaque semaine" else " · une seule fois") +
                (if (rappel.actif) "" else " · inactif"),
            style = MaterialTheme.typography.labelLarge
        )
        Text(rappel.texte, style = MaterialTheme.typography.bodyMedium)
        Row {
            TextButton(onClick = onBasculer) { Text(if (rappel.actif) "Désactiver" else "Réactiver") }
            TextButton(onClick = onSupprimer) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun RappelDialog(
    creneau: Creneau,
    erreur: String?,
    onValider: (texte: String, minutes: Int, sonore: Boolean, hebdomadaire: Boolean) -> Unit,
    onFermer: () -> Unit
) {
    var texte by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf(creneau.debut) }
    var sonore by remember { mutableStateOf(false) }
    var hebdomadaire by remember { mutableStateOf(true) }

    Dialogue(
        titre = "Nouveau rappel",
        onFermer = onFermer,
        confirmer = {
            TextButton(onClick = { onValider(texte, minutes, sonore, hebdomadaire) }) { Text("Créer") }
        },
        annuler = { TextButton(onClick = onFermer) { Text("Annuler") } }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Pendant le cours du ${EmploiDuTemps.JOURS[creneau.jour - 1].lowercase()}, de " +
                    "${EmploiDuTemps.heure(creneau.debut)} à ${EmploiDuTemps.heure(creneau.fin)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Champ(texte, { texte = it }, "Ce dont je veux me souvenir", singleLine = false, minLines = 2, maxLines = 4)
            SaisieHeure("Heure du rappel", minutes, { minutes = it })
            TitreSection("MODE")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PuceChoix("🔕 Silencieux", !sonore) { sonore = false }
                PuceChoix("🔔 Sonore", sonore) { sonore = true }
            }
            Text(
                if (sonore) "Une notification avec son et vibration." else "Une notification discrète, sans son ni vibration.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TitreSection("RÉPÉTITION")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PuceChoix("Chaque semaine", hebdomadaire) { hebdomadaire = true }
                PuceChoix("Une seule fois", !hebdomadaire) { hebdomadaire = false }
            }
            erreur?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
