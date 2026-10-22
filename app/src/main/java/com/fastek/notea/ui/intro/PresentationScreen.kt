package com.fastek.notea.ui.intro

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Mémorise localement que la présentation du premier démarrage a été vue. */
object PresentationVue {
    private const val FICHIER = "notea_reglages"
    private const val CLE = "presentation_vue"

    fun dejaVue(context: Context): Boolean =
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).getBoolean(CLE, false)

    fun marquerVue(context: Context) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit().putBoolean(CLE, true).apply()
    }
}

private class Diapositive(val symbole: String, val titre: String, val texte: String)

private val DIAPOSITIVES = listOf(
    Diapositive(
        "🧮", "Tes moyennes, calculées pour toi",
        "Saisis tes devoirs et tes interrogations : Notea calcule automatiquement la moyenne de " +
            "chaque matière, ta moyenne générale et ta moyenne annuelle, selon la méthode officielle."
    ),
    Diapositive(
        "🎯", "Fixe ton objectif",
        "Définis la moyenne à atteindre pour chaque période et pour l'année. Le tableau de bord " +
            "mesure ta progression, et le simulateur t'indique la note à obtenir au prochain devoir."
    ),
    Diapositive(
        "📅", "Planning et rappels",
        "Ajoute tes devoirs et interrogations à venir. Notea te prévient deux jours avant, la veille " +
            "et le jour même, à l'heure que tu choisis."
    ),
    Diapositive(
        "📄", "Ton bulletin en PDF",
        "Génère le bulletin de la période au format PDF, avec photo et appréciations, puis partage-le " +
            "par WhatsApp ou par e-mail."
    ),
    Diapositive(
        "🔒", "Gratuite, hors ligne, à toi",
        "Tes données restent sur ton téléphone, sans compte ni connexion. Pense à créer une " +
            "sauvegarde pour les retrouver si tu changes de téléphone."
    )
)

/** Présentation des fonctionnalités principales (premier démarrage, ou depuis les paramètres). */
@Composable
fun PresentationScreen(onTerminer: () -> Unit) {
    val etat = rememberPagerState(pageCount = { DIAPOSITIVES.size })
    val scope = rememberCoroutineScope()
    val derniere = etat.currentPage == DIAPOSITIVES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "NOTEA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.sp
            )
            if (!derniere) {
                TextButton(onClick = onTerminer) { Text("Passer") }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }

        HorizontalPager(state = etat, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val d = DIAPOSITIVES[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(d.symbole, fontSize = 64.sp)
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    d.titre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    d.texte,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(DIAPOSITIVES.size) { index ->
                val actif = index == etat.currentPage
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (actif) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (actif) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }

        Button(
            onClick = {
                if (derniere) onTerminer() else scope.launch { etat.animateScrollToPage(etat.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (derniere) "Commencer" else "Suivant")
        }
    }
}
