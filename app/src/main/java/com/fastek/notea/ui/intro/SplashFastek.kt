package com.fastek.notea.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastek.notea.R
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private val VIOLET_NUIT = Color(0xFF1B1040)
private val VIOLET = Color(0xFF4C1D95)
private val CYAN = Color(0xFF22D3EE)
private val BLANC = Color(0xFFF3F6FC)
private const val LETTRES = "FASTEK"
private const val DUREE_TOTALE_MS = 3000L

/**
 * Écran de lancement FASTEK, d'une durée de 3 secondes : le symbole apparaît, les lettres de
 * FASTEK s'écrivent l'une après l'autre, un trait cyan se déploie puis « Notea » s'affiche.
 * Un toucher permet de passer l'animation.
 */
@Composable
fun SplashFastek(onTermine: () -> Unit) {
    val symbole = remember { Animatable(0f) }
    val lettres = remember { List(LETTRES.length) { Animatable(0f) } }
    val trait = remember { Animatable(0f) }
    val produit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // 0 – 600 ms : le symbole apparaît
            animer(symbole, 600, 0L)
            // 500 – 2000 ms : FASTEK s'écrit lettre par lettre (250 ms d'écart, 400 ms par lettre)
            lettres.forEachIndexed { i, lettre -> animer(lettre, 400, 500L + i * 250L) }
            // 1900 – 2500 ms : le trait cyan se déploie
            animer(trait, 600, 1900L)
            // 2300 – 2800 ms : « Notea » apparaît
            animer(produit, 500, 2300L)
        }
        delay(DUREE_TOTALE_MS - 2800L)
        onTermine()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VIOLET_NUIT, VIOLET)))
            .clickable { onTermine() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .scale(0.6f + 0.4f * symbole.value)
                    .alpha(symbole.value)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LETTRES.forEachIndexed { i, lettre ->
                    val progression = lettres[i].value
                    Text(
                        text = lettre.toString(),
                        color = BLANC,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .alpha(progression)
                            .offset(y = (18f * (1f - progression)).dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width((200f * trait.value).dp)
                    .height(3.dp)
                    .background(CYAN)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Notea",
                color = BLANC,
                fontSize = 18.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(produit.value)
            )
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.animer(anim: Animatable<Float, *>, duree: Int, attente: Long) {
    async {
        delay(attente)
        anim.animateTo(1f, tween(duree, easing = if (duree > 500) FastOutSlowInEasing else LinearEasing))
    }
}
