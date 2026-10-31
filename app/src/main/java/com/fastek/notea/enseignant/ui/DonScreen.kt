package com.fastek.notea.enseignant.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fastek.notea.BuildConfig
import com.fastek.notea.diagnostic.Journal

private const val MONTANT_MINIMUM = 100
private const val MONTANT_MAXIMUM = 1_000_000

/** Origine factice du document local : le paiement lui-même est servi par FedaPay (https). */
private const val ORIGINE_PAGE = "https://notea-enseignant.fastek.local/"

private val MONTANTS_PROPOSES = listOf(500, 1000, 2000, 5000)

/** Environnement déduit de la clé publique : « pk_live_… » est le vrai paiement, le reste est le bac à sable. */
internal fun environnementFedaPay(clePublique: String): String =
    if (clePublique.startsWith("pk_live")) "live" else "sandbox"

/**
 * Page locale qui ouvre le formulaire Feda Checkout (clé publique uniquement : aucune clé secrète
 * n'est nécessaire pour un don) et renvoie le résultat à l'application.
 */
internal fun pageFedaPay(clePublique: String, montant: Int): String = """
<!DOCTYPE html>
<html lang="fr"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  html, body { margin: 0; height: 100%; background: #F4F2FC; color: #4A4660;
    font-family: sans-serif; display: flex; align-items: center; justify-content: center; text-align: center; }
</style>
<script src="https://cdn.fedapay.com/checkout.js?v=1.1.7"></script>
</head>
<body>
<div id="message">Ouverture du paiement…</div>
<script>
  function ouvrir() {
    try {
      var widget = FedaPay.init({
        public_key: '$clePublique',
        environment: '${environnementFedaPay(clePublique)}',
        locale: 'fr',
        transaction: {
          amount: $montant,
          description: 'Don à FASTEK pour Notea Enseignant',
          custom_metadata: { app: 'notea-enseignant', type: 'don' }
        },
        onComplete: function (resp) {
          var reussi = resp.reason === FedaPay.CHECKOUT_COMPLETED;
          var statut = (resp.transaction && resp.transaction.status) ? String(resp.transaction.status) : '';
          Android.surFin(reussi, statut);
        }
      });
      widget.open();
    } catch (e) {
      Android.surErreur('Impossible d\'ouvrir le paiement. Vérifie ta connexion internet.');
    }
  }
  window.onload = ouvrir;
</script>
</body></html>
"""

/** Pont entre la page FedaPay et l'application (les appels arrivent sur un autre fil : on revient sur le fil principal). */
internal class PontFedaPay(
    private val onFin: (reussi: Boolean, statut: String) -> Unit,
    private val onErreur: (String) -> Unit
) {
    private val principal = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun surFin(reussi: Boolean, statut: String) {
        principal.post { onFin(reussi, statut) }
    }

    @JavascriptInterface
    fun surErreur(message: String) {
        principal.post { onErreur(message) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PaiementFedaPay(
    montant: Int,
    onFin: (reussi: Boolean, statut: String) -> Unit,
    onErreur: (String) -> Unit,
    onFermer: () -> Unit
) {
    BackHandler(onBack = onFermer)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        EnTete(
            titre = "Paiement sécurisé",
            sousTitre = "FedaPay · $montant FCFA",
            droite = { BoutonEntete("Fermer", onFermer) }
        )
        AndroidView(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            factory = { contexte ->
                WebView(contexte).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    addJavascriptInterface(PontFedaPay(onFin, onErreur), "Android")
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(
                        ORIGINE_PAGE, pageFedaPay(BuildConfig.FEDAPAY_PUBLIC_KEY, montant),
                        "text/html", "utf-8", null
                    )
                }
            },
            onRelease = { vue -> vue.destroy() }
        )
    }
}

@Composable
fun DonScreen(onRetour: () -> Unit) {
    var montantPropose by remember { mutableStateOf(1000) }
    var montantLibre by remember { mutableStateOf("") }
    var paiement by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var reussi by remember { mutableStateOf(false) }

    val montant = montantLibre.toIntOrNull() ?: montantPropose
    val montantValide = montant in MONTANT_MINIMUM..MONTANT_MAXIMUM
    val configure = BuildConfig.FEDAPAY_PUBLIC_KEY.isNotBlank()

    EcranAvecEntete("Soutenir Notea", onRetour, sousTitre = "Un don volontaire, quand tu veux") {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💜  Notea reste gratuit", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notea Enseignant est offert aux professeurs. Si l'application t'aide, un don volontaire " +
                            "finance la suite du développement : nouvelles fonctions, serveur, assistance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TitreSection("MONTANT (FCFA)")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MONTANTS_PROPOSES.forEach { valeur ->
                            PuceChoix("$valeur", montantLibre.isBlank() && montantPropose == valeur) {
                                montantLibre = ""
                                montantPropose = valeur
                            }
                        }
                    }
                    Champ(
                        montantLibre, { montantLibre = it.filter(Char::isDigit).take(7) }, "Autre montant",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (!montantValide) {
                        Text(
                            "Le montant doit être compris entre $MONTANT_MINIMUM et $MONTANT_MAXIMUM FCFA.",
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            message?.let {
                Text(it, color = if (reussi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            if (!configure) {
                Text(
                    "Le don n'est pas encore disponible dans cette version de l'application.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium
                )
            }
            BoutonPlein(
                "Donner $montant FCFA",
                onClick = { message = null; paiement = true },
                enabled = configure && montantValide,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Le paiement est traité par FedaPay (Mobile Money et carte). Notea ne voit et ne garde aucune information de paiement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }

    if (paiement) {
        PaiementFedaPay(
            montant = montant,
            onFin = { succes, statut ->
                paiement = false
                reussi = succes
                Journal.ok(if (succes) "don.fedapay.reussi" else "don.fedapay.annule", statut)
                message = if (succes) {
                    "Merci pour ton don de $montant FCFA ! 💜"
                } else {
                    "Le paiement n'a pas abouti. Aucun montant n'a été débité."
                }
            },
            onErreur = { texte ->
                paiement = false
                reussi = false
                Journal.ok("don.fedapay.erreur", texte)
                message = texte
            },
            onFermer = { paiement = false }
        )
    }
}
